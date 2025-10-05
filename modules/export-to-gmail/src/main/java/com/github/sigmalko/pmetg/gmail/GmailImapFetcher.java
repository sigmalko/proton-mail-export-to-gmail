package com.github.sigmalko.pmetg.gmail;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import jakarta.mail.Folder;
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

                        log.info("store.isConnected(): {}", store.isConnected());
                        Arrays.asList(store.getPersonalNamespaces()).forEach(ns -> {
                                try {
                                        log.info("Found personal namespace: {}", ns);
                                        log.info("  -> full name: {}", ns.getFullName());
                                        log.info("  -> name: {}", ns.getName());
                                        log.info("  -> count: {}", ns.getMessageCount());
                                } catch (MessagingException e) {
                                        log.error("Failed to get namespace details", e);
                                }
                        });
                        Arrays.asList(store.getSharedNamespaces()).forEach(ns -> log.info("Found shared namespace: {}", ns));
                        log.info("store.getDefaultFolder().getName(): ", store.getDefaultFolder().getName());
                        
                        inbox = store.getFolder("Proton");
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

        
                        return null;
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
