package com.github.sigmalko.pmetg.migrations;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrationRepository extends JpaRepository<MigrationEntity, Long> {

    Optional<MigrationEntity> findByMessageId(String messageId);

    List<MigrationEntity> findAllByMessageAlreadyExistsFalse();

    List<MigrationEntity> findAllByMessageExportedFalse();
}
