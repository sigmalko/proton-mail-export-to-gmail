package com.github.sigmalko.protonmail.export.integration.gmail;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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

        public List<String> fetchReadableFolders() {
                if (!clientSupport.hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping readable folder discovery.");
                        return List.of();
                }

                try (final var session = clientSupport.openReadOnlyFolder(properties.folder())) {
                        final var store = session.store();
                        final var readableFolders = collectReadableFolders(store);

                        if (readableFolders.isEmpty()) {
                                log.info("No readable Gmail folders were discovered.");
                        } else {
                                log.info(
                                                "Readable Gmail folders ({}): {}",
                                                readableFolders.size(),
                                                String.join(", ", readableFolders));
                        }

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

                try (final var session = clientSupport.openReadOnlyFolder(properties.folder())) {
                        final var store = session.store();
                        logFolderTopology(store);

                        final var inbox = session.folder();
                        logFolderDetails(inbox);

                        return determineWindow(inbox, properties.messageLimit())
                                        .map(window -> fetchHeaders(inbox, window))
                                        .orElseGet(List::of);
                } catch (MessagingException exception) {
                        log.error("Failed to fetch Gmail message headers.", exception);
                        return List.of();
                }
        }

        private void logFolderDetails(Folder inbox) throws MessagingException {
                log.info("Opened Gmail folder '{}' in read-only mode.", inbox.getFullName());
                log.info("Messages found in folder {}: {}", inbox.getFullName(), inbox.getMessageCount());

                log.info("inbox.getFullName(): {}", inbox.getFullName());
                log.info("inbox.getMode(): {}", inbox.getMode());
                log.info("inbox.getName(): {}", inbox.getName());
                log.info("inbox.getSeparator(): {}", inbox.getSeparator());
                log.info("inbox.getType(): {}", inbox.getType());
                log.info("inbox.getUnreadMessageCount(): {}", inbox.getUnreadMessageCount());
                log.info("inbox.getNewMessageCount(): {}", inbox.getNewMessageCount());
        }

        private Optional<MessageWindow> determineWindow(Folder inbox, int limit) throws MessagingException {
                if (limit <= 0) {
                        log.info("Configured message limit is {}. Skipping header retrieval.", limit);
                        return Optional.empty();
                }

                final var messageCount = inbox.getMessageCount();
                if (messageCount == 0) {
                        log.info("Folder {} is empty. Skipping header retrieval.", inbox.getFullName());
                        return Optional.empty();
                }

                final var start = Math.max(1, messageCount - limit + 1);
                final var end = messageCount;

                log.info("Getting messages {} - {}", start, end);
                return Optional.of(new MessageWindow(start, end));
        }

        @SneakyThrows(MessagingException.class)
        private List<EmailHeader> fetchHeaders(Folder inbox, MessageWindow window) {
                final var messages = inbox.getMessages(window.start(), window.end());
                fetchEnvelopeOnly(inbox, messages);

                final var headers = Arrays.stream(messages)
                                .<EmailHeader>mapMulti(mapper::map)
                                .sorted(Comparator.comparingInt(EmailHeader::messageNumber).reversed())
                                .toList();
                headerSynchronizer.synchronize(headers);
                headers.forEach(this::logHeader);
                return headers;
        }

        private void fetchEnvelopeOnly(Folder inbox, Message[] messages) throws MessagingException {
                final var fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                inbox.fetch(messages, fetchProfile);
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

        private void logFolderTopology(Store store) {
                try {
                        final var defaultFolder = resolveDefaultFolder(store);
                        if (defaultFolder == null) {
                                return;
                        }

                        log.info("Gmail folder topology:");
                        visitFolderRecursively(defaultFolder, 0, descriptor -> log.info(
                                        "{}- {} (messages: {})",
                                        "  ".repeat(descriptor.depth()),
                                        descriptor.displayName(),
                                        descriptor.messageCountDescription()));
                } catch (MessagingException exception) {
                        log.warn("Failed to log Gmail folder topology.", exception);
                }
        }

        private List<String> collectReadableFolders(Store store) throws MessagingException {
                final var defaultFolder = resolveDefaultFolder(store);
                if (defaultFolder == null) {
                        return List.of();
                }

                final var readableFolders = new ArrayList<String>();
                visitFolderRecursively(defaultFolder, 0, descriptor -> {
                        if (descriptor.holdsMessages() && !"(root)".equals(descriptor.displayName())) {
                                readableFolders.add(descriptor.displayName());
                        }
                });
                return readableFolders;
        }

        private Folder resolveDefaultFolder(Store store) throws MessagingException {
                final var defaultFolder = store.getDefaultFolder();
                if (defaultFolder == null) {
                        log.warn("Unable to access Gmail folders because the default folder is null.");
                }
                return defaultFolder;
        }

        private void visitFolderRecursively(Folder folder, int depth, Consumer<FolderDescriptor> visitor) throws MessagingException {
                final var descriptor = buildFolderDescriptor(folder, depth);
                visitor.accept(descriptor);

                if (!descriptor.holdsFolders()) {
                        return;
                }

                try {
                        for (final var child : folder.list()) {
                                visitFolderRecursively(child, depth + 1, visitor);
                        }
                } catch (MessagingException exception) {
                        log.warn("Failed to enumerate children for folder {}.", descriptor.displayName(), exception);
                }
        }

        private FolderDescriptor buildFolderDescriptor(Folder folder, int depth) {
                final var folderName = resolveFolderDisplayName(folder);

                int folderType = Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES;
                try {
                        folderType = folder.getType();
                } catch (MessagingException exception) {
                        log.warn(
                                        "Failed to determine folder type for {}. Assuming it may contain sub-folders.",
                                        folderName,
                                        exception);
                }

                final var holdsMessages = (folderType & Folder.HOLDS_MESSAGES) != 0;
                final var holdsFolders = (folderType & Folder.HOLDS_FOLDERS) != 0;
                final var messageCountDescription = holdsMessages
                                ? resolveMessageCountDescription(folder, folderName)
                                : "n/a";

                return new FolderDescriptor(folderName, depth, holdsMessages, holdsFolders, messageCountDescription);
        }

        private String resolveMessageCountDescription(Folder folder, String folderName) {
                try {
                        return Integer.toString(folder.getMessageCount());
                } catch (MessagingException exception) {
                        log.warn("Failed to resolve message count for folder {}.", folderName, exception);
                        return "error";
                }
        }

        private String resolveFolderDisplayName(Folder folder) {
                final var fullName = folder.getFullName();
                if (StringUtils.hasText(fullName)) {
                        return fullName;
                }

                final var name = folder.getName();
                if (StringUtils.hasText(name)) {
                        return name;
                }

                return "(root)";
        }

        private record FolderDescriptor(
                String displayName,
                int depth,
                boolean holdsMessages,
                boolean holdsFolders,
                String messageCountDescription) {
        }

}
