package com.github.sigmalko.pmetg.gmail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GmailImapFetcher")
@Component
@RequiredArgsConstructor
public class GmailImapFetcher {

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

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
                        log.info("Connecting to Gmail IMAP server {}:{} using SSL: {}", properties.host(), properties.port(),
                                        properties.sslEnabled());
                        store.connect(properties.host(), properties.port(), properties.username(), properties.password());

                        inbox = store.getFolder("INBOX");
                        inbox.open(Folder.READ_ONLY);

                        final int messageCount = inbox.getMessageCount();
                        if (messageCount == 0) {
                                log.info("No messages found in the INBOX.");
                                return List.of();
                        }

                        final Message[] messages = selectMessages(inbox, messageCount);
                        final FetchProfile fetchProfile = new FetchProfile();
                        fetchProfile.add(FetchProfile.Item.ENVELOPE);
                        fetchProfile.add("Message-ID");
                        inbox.fetch(messages, fetchProfile);

                        final var headers = Arrays.stream(messages)
                                        .map(this::toHeader)
                                        .toList();

                        headers.forEach(header -> log.info(
                                        "Message header - from: {}, to: {}, sentAt: {}, messageId: {}",
                                        header.from(),
                                        header.to(),
                                        header.sentAt() == null ? "N/A"
                                                        : DATE_FORMATTER.format(header.sentAt().atZone(ZoneId.systemDefault())),
                                        header.messageId()));

                        log.info("Fetched {} message headers from Gmail.", headers.size());
                        return headers;
                } catch (MessagingException exception) {
                        log.error("Failed to fetch Gmail message headers.", exception);
                        return List.of();
                } finally {
                        closeFolder(inbox);
                        closeStore(store);
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

        private Message[] selectMessages(Folder inbox, int messageCount) throws MessagingException {
                final int limit = properties.messageLimit();
                if (limit <= 0 || limit >= messageCount) {
                        log.info("Fetching all {} messages from INBOX.", messageCount);
                        return inbox.getMessages();
                }

                final int start = Math.max(1, messageCount - limit + 1);
                log.info("Fetching the latest {} messages from INBOX (messages {} to {}).", limit, start, messageCount);
                return inbox.getMessages(start, messageCount);
        }

        private EmailHeader toHeader(Message message) {
                try {
                        final var from = formatAddresses(message.getFrom());
                        final var to = formatAddresses(message.getRecipients(Message.RecipientType.TO));
                        final Instant sentAt = Optional.ofNullable(message.getSentDate())
                                        .map(date -> date.toInstant())
                                        .orElse(null);
                        String messageId = firstHeader(message, "Message-ID");
                        if (!StringUtils.hasText(messageId)) {
                                messageId = firstHeader(message, "Message-Id");
                        }
                        if (!StringUtils.hasText(messageId)) {
                                messageId = "<unknown>";
                        }
                        return new EmailHeader(from, to, sentAt, messageId);
                } catch (MessagingException exception) {
                        log.error("Failed to read message header information.", exception);
                        return new EmailHeader("<error>", "<error>", null, "<error>");
                }
        }

        private String formatAddresses(Address[] addresses) {
                if (addresses == null || addresses.length == 0) {
                        return "<empty>";
                }
                final var joined = Arrays.stream(addresses)
                                .map(Address::toString)
                                .collect(Collectors.joining(", "));
                return StringUtils.hasText(joined) ? joined : "<empty>";
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

        private String firstHeader(Message message, String headerName) throws MessagingException {
                final String[] values = message.getHeader(headerName);
                if (values == null || values.length == 0) {
                        return null;
                }
                return values[0];
        }
}
