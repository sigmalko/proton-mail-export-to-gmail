package com.github.sigmalko.protonmail.export.integration.gmail;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j(topic = "GmailEmailHeaderMapper")
@Component
public class GmailEmailHeaderMapper {

        public void map(Message message, Consumer<? super EmailHeader> consumer) {
                map(message).ifPresent(consumer);
        }

        public Optional<EmailHeader> map(Message message) {
                final var messageNumber = message.getMessageNumber();

                try {
                        return Optional.of(new EmailHeader(
                                        messageNumber,
                                        firstHeaderValue(message, "Message-ID"),
                                        resolveSentAt(message),
                                        formatAddresses(message.getFrom())));
                } catch (MessagingException exception) {
                        log.warn("Failed to extract headers for message {}.", messageNumber, exception);
                        return Optional.empty();
                }
        }

        private Instant resolveSentAt(Message message) throws MessagingException {
                return message.getSentDate() != null ? message.getSentDate().toInstant() : null;
        }

        private String firstHeaderValue(Message message, String headerName) throws MessagingException {
                final var headerValues = message.getHeader(headerName);
                if (headerValues == null || headerValues.length == 0) {
                        return null;
                }

                return headerValues[0];
        }

        private String formatAddresses(Address[] addresses) {
                if (addresses == null || addresses.length == 0) {
                        return "";
                }

                return Arrays.stream(addresses)
                                .map(this::formatAddress)
                                .filter(address -> !address.isEmpty())
                                .collect(Collectors.joining(", "));
        }

        private String formatAddress(Address address) {
                return switch (address) {
                case InternetAddress internetAddress -> internetAddress.toUnicodeString();
                case null -> "";
                default -> address.toString();
                };
        }
}
