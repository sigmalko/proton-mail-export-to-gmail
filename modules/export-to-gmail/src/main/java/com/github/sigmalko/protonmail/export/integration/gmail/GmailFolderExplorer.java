package com.github.sigmalko.protonmail.export.integration.gmail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GMAIL")
@Component
public class GmailFolderExplorer {

        public void logFolderTopology(Store store) {
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

        public List<String> collectReadableFolders(Store store) throws MessagingException {
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

        private void visitFolderRecursively(Folder folder, int depth, Consumer<FolderDescriptor> visitor)
                        throws MessagingException {
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

        String resolveFolderDisplayName(Folder folder) {
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
