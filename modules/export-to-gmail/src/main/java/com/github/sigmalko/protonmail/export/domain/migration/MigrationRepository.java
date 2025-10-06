package com.github.sigmalko.protonmail.export.domain.migration;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrationRepository extends JpaRepository<MigrationEntity, Long> {

    Optional<MigrationStatus> findByMessageId(String messageId);

    List<MigrationStatus> findAllByMessageAlreadyExistsFalse();

    List<MigrationStatus> findAllByMessageExportedFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MigrationEntity m set m.messageInFile = :value where m.messageId = :messageId")
    int updateMessageInFileByMessageId(
            @Param("messageId") String messageId, @Param("value") boolean value);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update MigrationEntity m set m.messageAlreadyExists = :value where m.messageId = :messageId")
    int updateMessageAlreadyExistsByMessageId(
            @Param("messageId") String messageId, @Param("value") boolean value);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MigrationEntity m set m.messageExported = :value where m.messageId = :messageId")
    int updateMessageExportedByMessageId(
            @Param("messageId") String messageId, @Param("value") boolean value);

    record MigrationStatus(
            String messageId,
            OffsetDateTime messageDate,
            boolean messageInFile,
            boolean messageAlreadyExists,
            boolean messageExported) {}
}
