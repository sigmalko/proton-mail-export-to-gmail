package com.github.sigmalko.pmetg.eml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "EmlEmailLoggingRunner")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class EmlEmailLoggingRunner implements CommandLineRunner {

    private static final Session MAIL_SESSION = Session.getInstance(new Properties());

    private final EmlReaderProperties properties;

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
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isEmlFile)
                    .sorted()
                    .forEach(this::logHeadersFromFile);
        } catch (IOException exception) {
            log.error("Failed to read EML files from directory: {}", directory, exception);
        }
    }

    private boolean isEmlFile(Path file) {
        final var filename = Objects.toString(file.getFileName(), "");
        return filename.toLowerCase(Locale.ROOT).endsWith(".eml");
    }

    private void logHeadersFromFile(Path file) {
        try (var inputStream = Files.newInputStream(file)) {
            final var message = new MimeMessage(MAIL_SESSION, inputStream);
            final var messageId = readHeader(message, "Message-ID");
            final var from = readHeader(message, "From");
            final var date = readHeader(message, "Date");

            log.info("Message-ID={}, From={}, Date={}", messageId, from, date);
        } catch (MessagingException | IOException exception) {
            log.error("Failed to process EML file: {}", file, exception);
        }
    }

    private String readHeader(MimeMessage message, String headerName) throws MessagingException {
        final var header = message.getHeader(headerName, ", ");
        return header != null ? header.strip() : "";
    }
}
