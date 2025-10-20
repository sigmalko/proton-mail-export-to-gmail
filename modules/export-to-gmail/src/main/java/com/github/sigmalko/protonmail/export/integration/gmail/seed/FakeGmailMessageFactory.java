package com.github.sigmalko.protonmail.export.integration.gmail.seed;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapProperties;

@Slf4j(topic = "GMAIL")
@Component
public class FakeGmailMessageFactory {

        private static final DateTimeFormatter SUBJECT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

        public MimeMessage create(Session session, GmailImapProperties properties) throws MessagingException {
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
                log.debug("Prepared fake Gmail message with subject '{}' and Message-ID {}.", message.getSubject(), messageId);
                return message;
        }
}
