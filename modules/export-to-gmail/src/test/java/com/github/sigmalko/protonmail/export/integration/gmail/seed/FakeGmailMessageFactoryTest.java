package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import jakarta.mail.Session;

import org.junit.jupiter.api.Test;

import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapProperties;

class FakeGmailMessageFactoryTest {

        private final FakeGmailMessageFactory factory = new FakeGmailMessageFactory();

        @Test
        void shouldCreateHtmlMessageWithDeterministicStructure() throws Exception {
                final var session = Session.getInstance(new Properties());
                final var properties = new GmailImapProperties(
                                true,
                                "imap.gmail.com",
                                993,
                                true,
                                "user@example.com",
                                "secret",
                                50);

                final var message = factory.create(session, properties);

                assertThat(message.getFrom()).singleElement().hasToString("user@example.com");
                assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.TO))
                                .singleElement()
                                .hasToString("user@example.com");
                assertThat(message.getSubject()).startsWith("Exported test message - ");
                assertThat(message.getHeader("Message-ID")).hasSize(1);
                assertThat(message.getContentType()).isEqualTo("text/html; charset=UTF-8");
                assertThat(message.getContent()).asString().contains("synthetic message created");
        }
}
