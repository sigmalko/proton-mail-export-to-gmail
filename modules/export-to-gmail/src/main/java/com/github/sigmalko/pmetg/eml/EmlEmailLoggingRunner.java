package com.github.sigmalko.pmetg.eml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.sigmalko.pmetg.migrations.MigrationEntity;
import com.github.sigmalko.pmetg.migrations.MigrationService;
import com.github.sigmalko.pmetg.migrations.MigrationService.MigrationFlag;

@Slf4j(topic = "EmlEmailLoggingRunner")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EmlEmailLoggingRunner implements CommandLineRunner {

    private static final Session MAIL_SESSION = Session.getInstance(new Properties());
    private static final int BATCH_SIZE = 1_000;

    private final EmlReaderProperties properties;
    private final MigrationService migrationService;

    @Override
    public void run(String... args) {
        log.info("##################################################");
        log.info("Logging headers from local EML files...");
        log.info("##################################################");

        final var directory = resolveDirectory();
        if (directory.isPresent()) {
            processDirectory(directory.get());
            log.info("Finished processing local EML files.");
        } else {
            log.info("Skipping local EML logging. No readable directory configured.");
        }
    }

    private Optional<Path> resolveDirectory() {
        final var directory = properties.directory();
        if (directory == null || directory.isBlank()) {
            log.debug("EML reader directory is not configured. Skipping scan.");
            return Optional.empty();
        }

        final Path path;
        try {
            path = Path.of(directory);
        } catch (InvalidPathException exception) {
            log.warn("Configured EML reader path is invalid: {}", directory, exception);
            return Optional.empty();
        }
        if (!Files.exists(path)) {
            log.warn("EML reader directory does not exist: {}", path);
            return Optional.empty();
        }

        if (!Files.isDirectory(path)) {
            log.warn("Configured EML reader path is not a directory: {}", path);
            return Optional.empty();
        }

        if (!Files.isReadable(path)) {
            log.warn("Configured EML reader directory is not readable: {}", path);
            return Optional.empty();
        }

        return Optional.of(path);
    }

    private void processDirectory(Path directory) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.eml")) {
            final List<Path> batch = new ArrayList<>(BATCH_SIZE);
            for (Path file : files) {
                if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                    continue;
                }

                batch.add(file);
                if (batch.size() >= BATCH_SIZE) {
                    processBatch(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        } catch (IOException exception) {
            log.error("Failed to read EML files from directory: {}", directory, exception);
        }
    }

    private void processBatch(List<Path> batch) {
        for (Path file : batch) {
            processFile(file);
        }
    }

    private void processFile(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            final MimeMessage message = new MimeMessage(MAIL_SESSION, inputStream);
            final String messageId = readHeader(message, "Message-ID");
            final String from = readHeader(message, "From");
            final String date = readHeader(message, "Date");

            log.info("Message-ID={}, From={}, Date={}", messageId, from, date);
            if (!StringUtils.hasText(messageId)) {
                log.debug("Skipping EML file {} because it does not contain Message-ID header.", file);
                return;
            }

            final OffsetDateTime messageDate = extractMessageDate(message);
            storeMigrationEntry(messageId, messageDate);
        } catch (MessagingException | IOException exception) {
            log.error("Failed to process EML file: {}", file, exception);
        }
    }

    private String readHeader(MimeMessage message, String headerName) throws MessagingException {
        final var header = message.getHeader(headerName, ", ");
        return header != null ? header.strip() : "";
    }

    private OffsetDateTime extractMessageDate(MimeMessage message) throws MessagingException {
        final var sentDate = message.getSentDate();
        if (sentDate == null) {
            return null;
        }

        return OffsetDateTime.ofInstant(sentDate.toInstant(), ZoneOffset.UTC);
    }

    private void storeMigrationEntry(String messageId, OffsetDateTime messageDate) {
        try {
            migrationService.findByMessageId(messageId)
                    .ifPresentOrElse(
                            this::markMessageAsStoredInFile,
                            () -> migrationService.createFileMigration(messageId, messageDate));
        } catch (Exception exception) {
            log.warn(
                    "Failed to persist migration entry for messageId={} originating from EML file.",
                    messageId,
                    exception);
        }
    }

    private void markMessageAsStoredInFile(MigrationEntity entity) {
        if (entity.isMessageInFile()) {
            return;
        }

        migrationService.updateFlagByMessageId(
                entity.getMessageId(), MigrationFlag.MESSAGE_IN_FILE, true);
    }
}
