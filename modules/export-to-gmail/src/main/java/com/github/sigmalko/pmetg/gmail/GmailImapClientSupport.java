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

        public void closeFolder(Folder folder) {
                if (folder != null && folder.isOpen()) {
                        try {
                                folder.close(false);
                        } catch (MessagingException exception) {
                                log.warn("Failed to close IMAP folder cleanly.", exception);
                        }
                }
        }

        public void closeStore(Store store) {
                if (store != null && store.isConnected()) {
                        try {
                                store.close();
                        } catch (MessagingException exception) {
                                log.warn("Failed to close IMAP store cleanly.", exception);
                        }
                }
        }
}
