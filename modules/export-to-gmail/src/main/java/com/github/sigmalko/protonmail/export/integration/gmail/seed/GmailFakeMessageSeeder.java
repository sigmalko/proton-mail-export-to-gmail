package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapClientSupport;
import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapProperties;

@Slf4j(topic = "GMAIL")
@Component
@RequiredArgsConstructor
public class GmailFakeMessageSeeder {

        private static final String TARGET_FOLDER = "exported";

        private final GmailImapProperties properties;
        private final GmailImapClientSupport clientSupport;
        private final FakeGmailMessageFactory fakeGmailMessageFactory;
        private final GmailMessageAppender gmailMessageAppender;

        public void appendFakeMessage() {
                if (!clientSupport.hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping fake message append.");
                        return;
                }

                final var session = clientSupport.createSession();

                try (final var folderSession = clientSupport.openFolder(TARGET_FOLDER, Folder.READ_WRITE)) {
                        final var exportedFolder = folderSession.folder();
                        final var message = fakeGmailMessageFactory.create(session, properties);

                        gmailMessageAppender.appendToFolder(exportedFolder, message);
                        log.info("Fake message appended to folder '{}' successfully.", exportedFolder.getFullName());
                } catch (MessagingException exception) {
                        log.error("Failed to append fake Gmail message.", exception);
                }
        }
}
