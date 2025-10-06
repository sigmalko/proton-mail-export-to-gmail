package com.github.sigmalko.pmetg.gmail;

import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GmailImapClientSupport")
@Component
@RequiredArgsConstructor
public class GmailImapClientSupport {

        private final GmailImapProperties properties;

        public boolean hasCredentials() {
                return StringUtils.hasText(properties.username()) && StringUtils.hasText(properties.password());
        }

        public Session createSession() {
                return Session.getInstance(buildMailProperties());
        }

        public Properties buildMailProperties() {
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

        public String resolveProtocol() {
                return properties.sslEnabled() ? "imaps" : "imap";
        }

        public FolderSession openReadOnlyFolder(String folder) throws MessagingException {
                return openFolder(folder, Folder.READ_ONLY);
        }

        public FolderSession openFolder(String folderName, int mode) throws MessagingException {
                final var session = createSession();
                final var store = session.getStore(resolveProtocol());

                log.info(
                                "Connecting to Gmail IMAP server {}:{} using SSL: {}",
                                properties.host(),
                                properties.port(),
                                properties.sslEnabled());
                store.connect(properties.host(), properties.port(), properties.username(), properties.password());
                log.info("store.isConnected(): {}", store.isConnected());

                final var targetFolder = store.getFolder(folderName);
                if (targetFolder == null) {
                        throw new MessagingException("IMAP folder '%s' could not be resolved.".formatted(folderName));
                }

                if (!targetFolder.exists() && mode != Folder.READ_ONLY) {
                        log.info("IMAP folder '{}' does not exist. Creating it now...", folderName);
                        targetFolder.create(Folder.HOLDS_MESSAGES);
                }

                if (!targetFolder.exists()) {
                        throw new MessagingException("IMAP folder '%s' does not exist.".formatted(folderName));
                }

                targetFolder.open(mode);
                return new FolderSession(store, targetFolder);
        }

        public record FolderSession(Store store, Folder folder) implements AutoCloseable {

                @Override
                public void close() {
                        if (folder != null && folder.isOpen()) {
                                try {
                                        folder.close(false);
                                } catch (MessagingException exception) {
                                        log.warn("Failed to close IMAP folder cleanly.", exception);
                                }
                        }

                        if (store != null && store.isConnected()) {
                                try {
                                        store.close();
                                } catch (MessagingException exception) {
                                        log.warn("Failed to close IMAP store cleanly.", exception);
                                }
                        }
                }
        }
}
