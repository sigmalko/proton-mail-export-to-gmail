package com.github.sigmalko.pmetg.gmail;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GmailImapFetcher")
@Component
@RequiredArgsConstructor
public class GmailImapFetcher {

        private static final DateTimeFormatter HEADER_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

        private final GmailImapProperties properties;

        public List<EmailHeader> fetchLatestHeaders() {
                if (!hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping header fetch.");
                        return List.of();
                }

                final var session = Session.getInstance(buildMailProperties());
                Store store = null;
                Folder inbox = null;

                try {
                        store = session.getStore(resolveProtocol());
                        log.info("Connecting to Gmail IMAP server {}:{} using SSL: {}", properties.host(), properties.port(), properties.sslEnabled());
                        store.connect(properties.host(), properties.port(), properties.username(), properties.password());
                        log.info("store.isConnected(): {}", store.isConnected());
                        logFolderTopology(store);
                        
                        final var FOLDER_NAME = "Proton";
                        // final var FOLDER_NAME = "INBOX";
                        inbox = store.getFolder(FOLDER_NAME);
                        inbox.open(Folder.READ_ONLY);
                        final int messageCount = inbox.getMessageCount();
                        log.info("Messages found in the INBOX: {}", messageCount);

                        log.info("inbox.getFullName(): {}", inbox.getFullName());
                        log.info("inbox.getMode(): {}", inbox.getMode());
                        log.info("inbox.getName(): {}", inbox.getName());
                        log.info("inbox.getSeparator(): {}", inbox.getSeparator());
                        log.info("inbox.getType(): {}", inbox.getType());
                        log.info("inbox.getUnreadMessageCount(): {}", inbox.getUnreadMessageCount());
                        log.info("inbox.getNewMessageCount(): {}", inbox.getNewMessageCount());

                        final int limit = properties.messageLimit();
                        if (limit <= 0) {
                                log.info("Configured message limit is {}. Skipping header retrieval.", limit);
                                return List.of();
                        }

                        if (messageCount == 0) {
                                log.info("Folder {} is empty. Skipping header retrieval.", inbox.getFullName());
                                return List.of();
                        }

                        final int start = Math.max(1, messageCount - limit + 1);
                        final int end = messageCount;


                        log.info("Getting messages {} - {}", start, end);


                        final Message[] messages = inbox.getMessages(start, end);
                        fetchEnvelopeOnly(inbox, messages);

                        final var headers = extractHeaders(messages);
                        headers.forEach(this::logHeader);
                        return headers;
                } catch (MessagingException exception) {
                        log.error("Failed to fetch Gmail message headers.", exception);
                        return List.of();
                } finally {
                        closeFolder(inbox);
                        closeStore(store);
                }
        }

        private void fetchEnvelopeOnly(Folder inbox, Message[] messages) throws MessagingException {
                final var fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                inbox.fetch(messages, fetchProfile);
        }

        private List<EmailHeader> extractHeaders(Message[] messages) {
                final var headers = new ArrayList<EmailHeader>(messages.length);

                for (Message message : messages) {
                        final int messageNumber = message.getMessageNumber();
                        try {
                                headers.add(new EmailHeader(
                                                messageNumber,
                                                firstHeaderValue(message, "Message-ID"),
                                                message.getSentDate() != null ? message.getSentDate().toInstant() : null,
                                                formatAddresses(message.getFrom())));
                        } catch (MessagingException exception) {
                                log.warn("Failed to extract headers for message {}.", messageNumber, exception);
                        }
                }

                headers.sort(Comparator.comparingInt(EmailHeader::messageNumber).reversed());
                return headers;
        }

        private String formatAddresses(Address[] addresses) {
                if (addresses == null || addresses.length == 0) {
                        return "";
                }

                return Arrays.stream(addresses)
                                .map(address -> address instanceof InternetAddress internetAddress
                                                ? internetAddress.toUnicodeString()
                                                : address.toString())
                                .collect(Collectors.joining(", "));
        }

        private String firstHeaderValue(Message message, String headerName) throws MessagingException {
                final var headerValues = message.getHeader(headerName);
                if (headerValues == null || headerValues.length == 0) {
                        return null;
                }

                return headerValues[0];
        }

        private void logHeader(EmailHeader header) {
                final var messageId = StringUtils.hasText(header.messageId()) ? header.messageId() : "N/A";
                final var formattedDate = header.sentAt() != null
                                ? HEADER_DATE_FORMATTER.format(header.sentAt().atZone(ZoneId.systemDefault()))
                                : "N/A";
                final var from = StringUtils.hasText(header.from()) ? header.from() : "N/A";

                log.info("{};;{};{};{}", header.messageNumber(), messageId, formattedDate, from);
        }

        private void logFolderTopology(Store store) {
                try {
                        final var defaultFolder = store.getDefaultFolder();
                        if (defaultFolder == null) {
                                log.warn("Unable to log Gmail folder topology because the default folder is null.");
                                return;
                        }

                        log.info("Gmail folder topology:");
                        logFolderRecursively(defaultFolder, 0);
                } catch (MessagingException exception) {
                        log.warn("Failed to log Gmail folder topology.", exception);
                }
        }

        private void logFolderRecursively(Folder folder, int depth) throws MessagingException {
                final var indent = "  ".repeat(depth);
                int messageCount;
                try {
                        messageCount = folder.getMessageCount();
                } catch (MessagingException exception) {
                        log.warn("Failed to resolve message count for folder {}.", folder.getFullName(), exception);
                        messageCount = -1;
                }

                log.info("{}- {} (messages: {})", indent, folder.getFullName(), messageCount);

                if ((folder.getType() & Folder.HOLDS_FOLDERS) == 0) {
                        return;
                }

                for (Folder child : folder.list()) {
                        logFolderRecursively(child, depth + 1);
                }
        }

        private boolean hasCredentials() {
                return StringUtils.hasText(properties.username()) && StringUtils.hasText(properties.password());
        }

        private Properties buildMailProperties() {
                final var props = new Properties();
                final var protocol = resolveProtocol();
                props.put("mail.store.protocol", protocol);
                props.put("mail.imap.host", properties.host());
                props.put("mail.imap.port", Integer.toString(properties.port()));
                props.put("mail.imap.ssl.enable", Boolean.toString(properties.sslEnabled()));
                props.put("mail.imaps.host", properties.host());
                props.put("mail.imaps.port", Integer.toString(properties.port()));
                props.put("mail.imaps.ssl.enable", Boolean.toString(properties.sslEnabled()));
                return props;
        }

        private String resolveProtocol() {
                return properties.sslEnabled() ? "imaps" : "imap";
        }

        private void closeFolder(Folder folder) {
                if (folder != null && folder.isOpen()) {
                        try {
                                folder.close(false);
                        } catch (MessagingException exception) {
                                log.warn("Failed to close IMAP folder cleanly.", exception);
                        }
                }
        }

        private void closeStore(Store store) {
                if (store != null && store.isConnected()) {
                        try {
                                store.close();
                        } catch (MessagingException exception) {
                                log.warn("Failed to close IMAP store cleanly.", exception);
                        }
                }
        }
}
