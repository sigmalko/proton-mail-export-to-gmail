package com.github.sigmalko.pmetg.migrations;

import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "pmetg.migration-service")
@Service
@RequiredArgsConstructor
public class MigrationService {

    private final MigrationRepository migrationRepository;

    @Transactional
    public MigrationEntity createFileMigration(String messageId, OffsetDateTime messageDate) {
        return createMigration(messageId, messageDate, builder -> builder.messageInFile(true));
    }

    @Transactional
    public MigrationEntity createGmailMigration(String messageId, OffsetDateTime messageDate) {
        return createMigration(messageId, messageDate, builder -> builder.messageAlreadyExists(true));
    }

    @Transactional
    public void updateFlagByMessageId(String messageId, MigrationFlag flag, boolean value) {
        int updatedRows = switch (flag) {
            case MESSAGE_IN_FILE -> migrationRepository.updateMessageInFileByMessageId(messageId, value);
            case MESSAGE_ALREADY_EXISTS ->
                    migrationRepository.updateMessageAlreadyExistsByMessageId(messageId, value);
            case MESSAGE_EXPORTED -> migrationRepository.updateMessageExportedByMessageId(messageId, value);
        };

        if (updatedRows == 0) {
            throw new EntityNotFoundException("Migration entry not found for messageId=" + messageId);
        }

        log.debug("Updated {} flag to {} for messageId={} (affectedRows={})", flag, value, messageId, updatedRows);
    }

    @Transactional(readOnly = true)
    public Optional<MigrationRepository.MigrationStatus> findByMessageId(String messageId) {
        return migrationRepository.findByMessageId(messageId);
    }

    @Transactional(readOnly = true)
    public List<MigrationRepository.MigrationStatus> findMessagesNotExistingInGmail() {
        return migrationRepository.findAllByMessageAlreadyExistsFalse();
    }

    @Transactional(readOnly = true)
    public List<MigrationRepository.MigrationStatus> findMessagesNotExported() {
        return migrationRepository.findAllByMessageExportedFalse();
    }

    private MigrationEntity createMigration(
            String messageId,
            OffsetDateTime messageDate,
            Consumer<MigrationEntity.MigrationEntityBuilder> builderCustomizer) {
        MigrationEntity.MigrationEntityBuilder builder = MigrationEntity.builder()
                .messageId(messageId)
                .messageDate(messageDate);
        builderCustomizer.accept(builder);
        MigrationEntity saved = migrationRepository.save(builder.build());
        log.debug(
                "Created migration entry with id={} for messageId={} (messageInFile={}, messageAlreadyExists={})",
                saved.getId(),
                messageId,
                saved.isMessageInFile(),
                saved.isMessageAlreadyExists());
        return saved;
    }

    public enum MigrationFlag {
        MESSAGE_IN_FILE,
        MESSAGE_ALREADY_EXISTS,
        MESSAGE_EXPORTED
    }
}
