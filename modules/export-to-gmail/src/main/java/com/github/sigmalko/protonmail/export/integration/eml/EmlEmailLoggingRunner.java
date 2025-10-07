package com.github.sigmalko.protonmail.export.integration.eml;

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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.sigmalko.protonmail.export.domain.migration.MigrationRepository.MigrationStatus;
import com.github.sigmalko.protonmail.export.domain.migration.MigrationService;
import com.github.sigmalko.protonmail.export.domain.migration.MigrationService.MigrationFlag;
import com.github.sigmalko.protonmail.export.domain.problem.ProblemService;

@Slf4j(topic = "EmlEmailLoggingRunner")
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eml.reader", name = "enabled", havingValue = "true")
public class EmlEmailLoggingRunner {

    private static final Session MAIL_SESSION = Session.getInstance(new Properties());
    private static final int BATCH_SIZE = 1_000;

    private final EmlReaderProperties properties;
    private final MigrationService migrationService;
    private final ProblemService problemService;

    public void run() {
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
        try (final var files = Files.newDirectoryStream(directory, "*.eml")) {
            final var batch = new ArrayList<Path>(BATCH_SIZE);
            for (final var file : files) {
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
        for (final var file : batch) {
            processFile(file);
        }
    }

    private void processFile(Path file) {
        try (final var inputStream = Files.newInputStream(file)) {
            final var message = new MimeMessage(MAIL_SESSION, inputStream);
            final var messageId = readHeader(message, "Message-ID");
            final var from = readHeader(message, "From");
            final var date = readHeader(message, "Date");
            final var messageDate = extractMessageDate(message);

            log.info("Message-ID={}, From={}, Date={}", messageId, from, date);
            if (!StringUtils.hasText(messageId)) {
                log.debug("Skipping EML file {} because it does not contain Message-ID header.", file);
                problemService.logFileProblem(
                        file.getFileName().toString(),
                        messageDate,
                        from,
                        "Missing Message-ID header in EML file " + file);
                return;
            }

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

    private void markMessageAsStoredInFile(MigrationStatus status) {
        if (status.messageInFile()) {
            return;
        }

        migrationService.updateFlagByMessageId(
                status.messageId(), MigrationFlag.MESSAGE_IN_FILE, true);
    }
}
