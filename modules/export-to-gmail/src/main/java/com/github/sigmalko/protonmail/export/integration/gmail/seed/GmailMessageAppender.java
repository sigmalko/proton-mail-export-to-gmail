package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import java.util.Date;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j(topic = "GMAIL")
@Component
public class GmailMessageAppender {

        private static final String DEFAULT_IMAP_FOLDER_CLASS_NAME = "com.sun.mail.imap.IMAPFolder";

        private final String imapFolderClassName;

        public GmailMessageAppender() {
                this(DEFAULT_IMAP_FOLDER_CLASS_NAME);
        }

        GmailMessageAppender(String imapFolderClassName) {
                this.imapFolderClassName = imapFolderClassName;
        }

        public void appendToFolder(Folder folder, MimeMessage message) throws MessagingException {
                if (tryAppendUsingImapFolder(folder, message)) {
                        return;
                }

                folder.appendMessages(new Message[] { message });
        }

        boolean tryAppendUsingImapFolder(Folder folder, MimeMessage message) {
                try {
                        final var imapFolderClass = Class.forName(imapFolderClassName);
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
