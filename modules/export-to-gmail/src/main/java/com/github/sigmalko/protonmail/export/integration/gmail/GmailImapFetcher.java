package com.github.sigmalko.protonmail.export.integration.gmail;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GmailImapFetcher")
@Component
@RequiredArgsConstructor
public class GmailImapFetcher {

        private static final DateTimeFormatter HEADER_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

        private final GmailImapProperties properties;
        private final GmailImapClientSupport clientSupport;
        private final GmailEmailHeaderMapper mapper;
        private final GmailHeaderSynchronizer headerSynchronizer;
        private final GmailFolderExplorer folderExplorer;

        public List<String> fetchReadableFolders() {
                if (!clientSupport.hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping readable folder discovery.");
                        return List.of();
                }

                try (final var storeSession = clientSupport.openStore()) {
                        final var store = storeSession.store();
                        final var readableFolders = folderExplorer.collectReadableFolders(store);

                        logDiscoveredFolders(readableFolders);

                        return List.copyOf(readableFolders);
                } catch (MessagingException exception) {
                        log.error("Failed to discover readable Gmail folders.", exception);
                        return List.of();
                }
        }

        public List<EmailHeader> fetchLatestHeaders() {
                if (!clientSupport.hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping header fetch.");
                        return List.of();
                }

                try (final var storeSession = clientSupport.openStore()) {
                        final var store = storeSession.store();
                        folderExplorer.logFolderTopology(store);

                        final var readableFolders = folderExplorer.collectReadableFolders(store);
                        logDiscoveredFolders(readableFolders);

                        final var headers = new ArrayList<EmailHeader>();
                        final var messageLimit = properties.messageLimit();

                        for (final var folderName : readableFolders) {
                                fetchHeadersFromFolder(store, folderName, messageLimit, headers);
                        }

                        return List.copyOf(headers);
                } catch (MessagingException exception) {
                        log.error("Failed to fetch Gmail message headers.", exception);
                        return List.of();
                }
        }

        private void logFolderDetails(Folder folder) throws MessagingException {
                log.info("Opened Gmail folder '{}' in read-only mode.", folder.getFullName());
                log.info("Messages found in folder {}: {}", folder.getFullName(), folder.getMessageCount());

                log.info("folder.getFullName(): {}", folder.getFullName());
                log.info("folder.getMode(): {}", folder.getMode());
                log.info("folder.getName(): {}", folder.getName());
                log.info("folder.getSeparator(): {}", folder.getSeparator());
                log.info("folder.getType(): {}", folder.getType());
                log.info("folder.getUnreadMessageCount(): {}", folder.getUnreadMessageCount());
                log.info("folder.getNewMessageCount(): {}", folder.getNewMessageCount());
        }

        private Optional<MessageWindow> determineWindow(Folder folder, int limit) throws MessagingException {
                if (limit <= 0) {
                        log.info("Configured message limit is {}. Skipping header retrieval.", limit);
                        return Optional.empty();
                }

                final var messageCount = folder.getMessageCount();
                if (messageCount == 0) {
                        log.info("Folder {} is empty. Skipping header retrieval.", folder.getFullName());
                        return Optional.empty();
                }

                final var start = Math.max(1, messageCount - limit + 1);
                final var end = messageCount;

                log.info("Getting messages {} - {}", start, end);
                return Optional.of(new MessageWindow(start, end));
        }

        @SneakyThrows(MessagingException.class)
        private List<EmailHeader> fetchHeaders(Folder folder, MessageWindow window) {
                final var messages = folder.getMessages(window.start(), window.end());
                fetchEnvelopeOnly(folder, messages);

                final var headers = Arrays.stream(messages)
                                .<EmailHeader>mapMulti(mapper::map)
                                .sorted(Comparator.comparingInt(EmailHeader::messageNumber).reversed())
                                .toList();
                headerSynchronizer.synchronize(headers);
                headers.forEach(this::logHeader);
                return headers;
        }

        private void fetchEnvelopeOnly(Folder folder, Message[] messages) throws MessagingException {
                final var fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                folder.fetch(messages, fetchProfile);
        }

        private record MessageWindow(int start, int end) {
        }

        private void logHeader(EmailHeader header) {
                final var messageId = StringUtils.hasText(header.messageId()) ? header.messageId() : "N/A";
                final var formattedDate = header.sentAt() != null
                                ? HEADER_DATE_FORMATTER.format(header.sentAt().atZone(ZoneId.systemDefault()))
                                : "N/A";
                final var from = StringUtils.hasText(header.from()) ? header.from() : "N/A";

                log.info("{};;{};{};{}", header.messageNumber(), messageId, formattedDate, from);
        }

        private void fetchHeadersFromFolder(Store store, String folderName, int messageLimit, List<EmailHeader> accumulator) {
                Folder folder = null;
                try {
                        folder = store.getFolder(folderName);
                        if (folder == null) {
                                log.warn("Gmail folder '{}' could not be resolved; skipping header retrieval.", folderName);
                                return;
                        }

                        if (!folder.exists()) {
                                log.warn("Gmail folder '{}' does not exist or is not accessible; skipping header retrieval.", folderName);
                                return;
                        }

                        folder.open(Folder.READ_ONLY);
                        logFolderDetails(folder);

                        final var window = determineWindow(folder, messageLimit);
                        if (window.isPresent()) {
                                accumulator.addAll(fetchHeaders(folder, window.orElseThrow()));
                        }
                } catch (MessagingException exception) {
                        log.warn("Failed to fetch Gmail message headers from folder '{}'.", folderName, exception);
                } finally {
                        closeQuietly(folder);
                }
        }

        private void closeQuietly(Folder folder) {
                if (folder == null) {
                        return;
                }

                try {
                        if (folder.isOpen()) {
                                folder.close(false);
                        }
                } catch (MessagingException exception) {
                        log.warn("Failed to close Gmail folder '{}' cleanly.", folderExplorer.resolveFolderDisplayName(folder), exception);
                }
        }

        private void logDiscoveredFolders(List<String> readableFolders) {
                if (readableFolders.isEmpty()) {
                        log.info("No readable Gmail folders were discovered.");
                } else {
                        log.info("Readable Gmail folders ({}): {}", readableFolders.size(), readableFolders);
                }
        }

}
