package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;

import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapClientSupport;
import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapProperties;

class GmailFakeMessageSeederTest {

        private final GmailImapProperties properties = new GmailImapProperties(
                        true,
                        "imap.gmail.com",
                        993,
                        true,
                        "user@example.com",
                        "secret",
                        50);
        private final GmailImapClientSupport clientSupport = mock(GmailImapClientSupport.class);
        private final FakeGmailMessageFactory fakeGmailMessageFactory = mock(FakeGmailMessageFactory.class);
        private final GmailMessageAppender gmailMessageAppender = mock(GmailMessageAppender.class);
        private final GmailFakeMessageSeeder seeder = new GmailFakeMessageSeeder(
                        properties,
                        clientSupport,
                        fakeGmailMessageFactory,
                        gmailMessageAppender);

        @Test
        void shouldSkipWhenCredentialsMissing() throws Exception {
                doReturn(false).when(clientSupport).hasCredentials();

                seeder.appendFakeMessage();

                verify(clientSupport, never()).openFolder("exported", Folder.READ_WRITE);
        }

        @Test
        void shouldDelegateMessageCreationAndAppend() throws Exception {
                doReturn(true).when(clientSupport).hasCredentials();
                final var session = Session.getDefaultInstance(new java.util.Properties());
                doReturn(session).when(clientSupport).createSession();

                final var store = mock(jakarta.mail.Store.class);
                final var folder = mock(Folder.class);
                final var folderSession = new GmailImapClientSupport.FolderSession(store, folder);
                doReturn(folderSession).when(clientSupport).openFolder("exported", Folder.READ_WRITE);
                doReturn("exported").when(folder).getFullName();

                final var message = new MimeMessage(session);
                doReturn(message).when(fakeGmailMessageFactory).create(session, properties);

                seeder.appendFakeMessage();

                verify(fakeGmailMessageFactory).create(session, properties);
                verify(gmailMessageAppender).appendToFolder(folder, message);
        }
}
