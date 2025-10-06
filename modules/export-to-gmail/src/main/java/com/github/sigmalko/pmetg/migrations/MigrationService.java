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
    public MigrationEntity updateFlagByMessageId(String messageId, MigrationFlag flag, boolean value) {
        MigrationEntity entity = migrationRepository.findByMessageId(messageId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Migration entry not found for messageId=" + messageId));
        applyFlag(entity, flag, value);
        MigrationEntity saved = migrationRepository.save(entity);
        log.debug(
                "Updated {} flag to {} for messageId={} (id={})",
                flag,
                value,
                saved.getMessageId(),
                saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<MigrationEntity> findByMessageId(String messageId) {
        return migrationRepository.findByMessageId(messageId);
    }

    @Transactional(readOnly = true)
    public List<MigrationEntity> findMessagesNotExistingInGmail() {
        return migrationRepository.findAllByMessageAlreadyExistsFalse();
    }

    @Transactional(readOnly = true)
    public List<MigrationEntity> findMessagesNotExported() {
        return migrationRepository.findAllByMessageExportedFalse();
    }

    private void applyFlag(MigrationEntity entity, MigrationFlag flag, boolean value) {
        switch (flag) {
            case MESSAGE_IN_FILE -> entity.setMessageInFile(value);
            case MESSAGE_ALREADY_EXISTS -> entity.setMessageAlreadyExists(value);
            case MESSAGE_EXPORTED -> entity.setMessageExported(value);
        }
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
