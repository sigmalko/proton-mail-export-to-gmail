package com.github.sigmalko.protonmail.export.integration.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.junit.jupiter.api.Test;

class GmailImapClientSupportTest {

        private static final GmailImapProperties DEFAULT_PROPERTIES =
                        new GmailImapProperties("imap.gmail.com", 993, true, "INBOX", "user", "pass", 10);

        private final GmailImapClientSupport support = new GmailImapClientSupport(DEFAULT_PROPERTIES);

        @Test
        void hasCredentialsReturnsTrueWhenUsernameAndPasswordPresent() {
                assertThat(support.hasCredentials()).isTrue();
        }

        @Test
        void hasCredentialsReturnsFalseWhenUsernameMissing() {
                final var properties = new GmailImapProperties("imap.gmail.com", 993, true, "INBOX", null, "pass", 10);
                final var clientSupport = new GmailImapClientSupport(properties);

                assertThat(clientSupport.hasCredentials()).isFalse();
        }

        @Test
        void buildMailPropertiesReflectsConfiguration() {
                final var properties = support.buildMailProperties();

                assertThat(properties).containsEntry("mail.store.protocol", "imaps")
                                .containsEntry("mail.imap.host", DEFAULT_PROPERTIES.host())
                                .containsEntry("mail.imap.port", Integer.toString(DEFAULT_PROPERTIES.port()))
                                .containsEntry("mail.imap.ssl.enable", Boolean.toString(DEFAULT_PROPERTIES.sslEnabled()))
                                .containsEntry("mail.imaps.host", DEFAULT_PROPERTIES.host())
                                .containsEntry("mail.imaps.port", Integer.toString(DEFAULT_PROPERTIES.port()))
                                .containsEntry("mail.imaps.ssl.enable", Boolean.toString(DEFAULT_PROPERTIES.sslEnabled()));
        }

        @Test
        void createSessionUsesConfiguredProperties() {
                final Session session = support.createSession();

                assertThat(session.getProperty("mail.store.protocol")).isEqualTo("imaps");
        }

        @Test
        void resolveProtocolReturnsImapWhenSslDisabled() {
                final var properties = new GmailImapProperties("imap.gmail.com", 143, false, "INBOX", "user", "pass", 10);
                final var clientSupport = new GmailImapClientSupport(properties);

                assertThat(clientSupport.resolveProtocol()).isEqualTo("imap");
        }

        @Test
        void folderSessionCloseClosesOpenResources() throws Exception {
                final Folder folder = mock(Folder.class);
                when(folder.isOpen()).thenReturn(true);
                final Store store = mock(Store.class);
                when(store.isConnected()).thenReturn(true);

                try (var session = new GmailImapClientSupport.FolderSession(store, folder)) {
                        // no-op
                }

                verify(folder).close(false);
                verify(store).close();
        }

        @Test
        void folderSessionCloseSwallowsExceptions() throws Exception {
                final Store store = mock(Store.class);
                when(store.isConnected()).thenReturn(true);
                final Folder folder = mock(Folder.class);
                when(folder.isOpen()).thenReturn(true);
                doThrow(new MessagingException("boom")).when(folder).close(false);
                doThrow(new MessagingException("boom"))
                                .when(store)
                                .close();

                try (var session = new GmailImapClientSupport.FolderSession(store, folder)) {
                        // no-op
                }

                verify(folder).close(false);
                verify(store).close();
        }
}
