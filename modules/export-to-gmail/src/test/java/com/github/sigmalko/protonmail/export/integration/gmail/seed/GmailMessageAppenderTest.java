package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Properties;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

class GmailMessageAppenderTest {

        @Test
        void shouldUseImapSpecificAppendWhenAvailable() throws Exception {
                final var session = Session.getInstance(new Properties());
                final var message = new MimeMessage(session);
                final var sentDate = new java.util.Date();
                message.setSentDate(sentDate);
                final var expectedInternalDate = message.getSentDate();

                final var folder = new TestImapFolder();
                final var appender = new GmailMessageAppender(TestImapFolder.class.getName());

                appender.appendToFolder(folder, message);

                assertThat(folder.appendedMessage).isEqualTo(message);
                assertThat(folder.appendedFlags).isNotNull().matches(flags -> flags.contains(Flags.Flag.SEEN));
                assertThat(folder.appendedDate).isEqualTo(expectedInternalDate);
                assertThat(folder.appendedMessages).isNull();
        }

        @Test
        void shouldFallbackToDefaultAppendWhenImapSpecificAppendFails() throws Exception {
                final var session = Session.getInstance(new Properties());
                final var message = new MimeMessage(session);
                final var folder = new TestImapFolder();
                folder.throwOnImapAppend = true;
                final var appender = new GmailMessageAppender(TestImapFolder.class.getName());

                appender.appendToFolder(folder, message);

                assertThat(folder.appendedMessages).containsExactly(message);
        }

        @Test
        void shouldAppendUsingFolderWhenImapFolderClassUnavailable() throws Exception {
                final var session = Session.getInstance(new Properties());
                final var message = new MimeMessage(session);
                final var folder = org.mockito.Mockito.mock(Folder.class);
                org.mockito.Mockito.doNothing().when(folder).appendMessages(org.mockito.ArgumentMatchers.any(Message[].class));

                final var appender = new GmailMessageAppender("non.existent.IMAPFolder");

                appender.appendToFolder(folder, message);

                org.mockito.Mockito.verify(folder).appendMessages(org.mockito.ArgumentMatchers.argThat((Message[] argument) -> {
                        assertThat(argument).hasSize(1);
                        return argument[0] == message;
                }));
        }
}
