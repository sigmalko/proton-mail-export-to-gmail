package com.github.sigmalko.protonmail.export.integration.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GmailEmailHeaderMapperTest {

        private final GmailEmailHeaderMapper mapper = new GmailEmailHeaderMapper();

        @Test
        void mapReturnsEmailHeaderWhenMessageReadable() throws Exception {
                final Message message = mock(Message.class);
                final Instant sentAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

                when(message.getMessageNumber()).thenReturn(42);
                when(message.getHeader("Message-ID")).thenReturn(new String[] {"<id-42>"});
                when(message.getSentDate()).thenReturn(Date.from(sentAt));
                when(message.getFrom()).thenReturn(new Address[] {new InternetAddress("John Doe <john@example.com>")});

                final Optional<EmailHeader> result = mapper.map(message);

                assertThat(result).isPresent();
                assertThat(result.get())
                                .extracting(EmailHeader::messageNumber, EmailHeader::messageId, EmailHeader::sentAt, EmailHeader::from)
                                .containsExactly(42, "<id-42>", sentAt, "John Doe <john@example.com>");
        }

        @Test
        void mapFormatsNonInternetAddressesUsingToString() throws Exception {
                final Message message = mock(Message.class);
                final Address customAddress = new CustomAddress("custom-address");
                when(message.getMessageNumber()).thenReturn(100);
                when(message.getHeader("Message-ID")).thenReturn(new String[] {"<id-100>"});
                when(message.getSentDate()).thenReturn(null);
                when(message.getFrom()).thenReturn(new Address[] {customAddress, null});

                final Optional<EmailHeader> result = mapper.map(message);

                assertThat(result).isPresent();
                assertThat(result.get().from()).isEqualTo("custom-address");
        }

        @Test
        void mapReturnsEmptyWhenMessagingExceptionThrown() throws Exception {
                final Message message = mock(Message.class);

                when(message.getMessageNumber()).thenReturn(7);
                when(message.getHeader("Message-ID")).thenThrow(new MessagingException("boom"));

                assertThat(mapper.map(message)).isEmpty();
        }
        private static final class CustomAddress extends Address {
                private final String value;

                private CustomAddress(String value) {
                        this.value = value;
                }

                @Override
                public String getType() {
                        return "custom";
                }

                @Override
                public String toString() {
                        return value;
                }

                @Override
                public boolean equals(Object obj) {
                        if (this == obj) {
                                return true;
                        }
                        if (!(obj instanceof CustomAddress other)) {
                                return false;
                        }
                        return value.equals(other.value);
                }

                @Override
                public int hashCode() {
                        return value.hashCode();
                }
        }
}
