package com.github.sigmalko.protonmail.export.integration.gmail;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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

@Slf4j(topic = "GMAIL")
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

        public void fetchLatestHeaders() {
                if (!clientSupport.hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping header fetch.");
                        return;
                }

                try (final var storeSession = clientSupport.openStore()) {
                        final var store = storeSession.store();
                        folderExplorer.logFolderTopology(store);

                        final var readableFolders = folderExplorer.collectReadableFolders(store);
                        logDiscoveredFolders(readableFolders);

                        final var windowSize = properties.windowSize();

                        for (final var folderName : readableFolders) {
                                fetchHeadersFromFolder(store, folderName, windowSize);
                        }
                } catch (MessagingException exception) {
                        log.error("Failed to fetch Gmail message headers.", exception);
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

        private List<MessageWindow> determineWindows(Folder folder, int windowSize) throws MessagingException {
                if (windowSize <= 0) {
                        log.info("Configured window size is {}. Skipping header retrieval.", windowSize);
                        return List.of();
                }

                final var messageCount = folder.getMessageCount();
                if (messageCount == 0) {
                        log.info("Folder {} is empty. Skipping header retrieval.", folder.getFullName());
                        return List.of();
                }

                final var windows = new ArrayList<MessageWindow>();
                for (var end = messageCount; end >= 1;) {
                        final var start = Math.max(1, end - windowSize + 1);
                        log.info("Getting messages {} - {}", start, end);
                        windows.add(new MessageWindow(start, end));
                        end = start - 1;
                }

                return List.copyOf(windows);
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

        private void fetchHeadersFromFolder(Store store, String folderName, int windowSize) {
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

                        final var windows = determineWindows(folder, windowSize);
                        for (final var window : windows) {
                                final var headers = fetchHeaders(folder, window);
                                log.info(
                                                "Processed {} Gmail headers from folder '{}' window {}-{}.",
                                                headers.size(),
                                                folder.getFullName(),
                                                window.start(),
                                                window.end());
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
