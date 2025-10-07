package com.github.sigmalko.protonmail.export.integration.gmail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GmailFakeMessageGenerator")
@Component
@RequiredArgsConstructor
public class GmailFakeMessageGenerator {

        private static final String TARGET_FOLDER = "exported";
        private static final DateTimeFormatter SUBJECT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

        private final GmailImapProperties properties;
        private final GmailImapClientSupport clientSupport;

        public void appendFakeMessage() {
                if (!clientSupport.hasCredentials()) {
                        log.warn("Gmail IMAP credentials are not configured; skipping fake message append.");
                        return;
                }

                final var session = clientSupport.createSession();

                try (final var folderSession = clientSupport.openFolder(TARGET_FOLDER, Folder.READ_WRITE)) {
                        final var exportedFolder = folderSession.folder();
                        final var message = buildFakeMessage(session);

                        appendToFolder(exportedFolder, message);
                        log.info("Fake message appended to folder '{}' successfully.", exportedFolder.getFullName());
                } catch (MessagingException exception) {
                        log.error("Failed to append fake Gmail message.", exception);
                }
        }

        private MimeMessage buildFakeMessage(Session session) throws MessagingException {
                final var message = new MimeMessage(session);

                final var username = properties.username();
                final var displayAddress = StringUtils.hasText(username) ? username : "no-reply@example.com";
                message.setFrom(new InternetAddress(displayAddress, false));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(displayAddress));

                final var sentAt = Date.from(Instant.now());
                message.setSentDate(sentAt);
                final var subjectTimestamp = SUBJECT_TIMESTAMP.format(sentAt.toInstant());
                message.setSubject("Exported test message - " + subjectTimestamp, "UTF-8");

                final var messageId = "<fake-" + UUID.randomUUID() + "@example.com>";
                message.setHeader("Message-ID", messageId);

                final var htmlBody = "<html><body><h1>Proton Mail Export</h1><p>This is a synthetic message created "
                                + "for testing the Proton Mail export workflow.</p></body></html>";
                message.setContent(htmlBody, "text/html; charset=UTF-8");

                message.saveChanges();
                return message;
        }

        private void appendToFolder(Folder folder, MimeMessage message) throws MessagingException {
                if (tryAppendUsingImapFolder(folder, message)) {
                        return;
                }

                folder.appendMessages(new Message[] { message });
        }

        private boolean tryAppendUsingImapFolder(Folder folder, MimeMessage message) {
                try {
                        final var imapFolderClass = Class.forName("com.sun.mail.imap.IMAPFolder");
                        if (!imapFolderClass.isInstance(folder)) {
                                return false;
                        }

                        final var appendMethod = imapFolderClass.getMethod("appendMessage", Message.class, Flags.class, Date.class);
                        final var flags = new Flags(Flags.Flag.SEEN);
                        Date internalDate;
                        try {
                                internalDate = message.getSentDate();
                        } catch (MessagingException exception) {
                                log.debug("Failed to resolve message sent date. Using null for INTERNALDATE.", exception);
                                internalDate = null;
                        }

                        appendMethod.invoke(folder, message, flags, internalDate);
                        return true;
                } catch (ClassNotFoundException exception) {
                        log.debug("IMAPFolder class is not available. Falling back to default append.", exception);
                        return false;
                } catch (ReflectiveOperationException exception) {
                        log.warn("Failed to use IMAPFolder-specific append API. Falling back to default append.", exception);
                        return false;
                }
        }

}
