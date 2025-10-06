package com.github.sigmalko.protonmail.export.domain.migration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MIGRATIONS")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MigrationEntity {

    @Id
    @SequenceGenerator(name = "migration_seq", sequenceName = "MIGRATIONS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "migration_seq")
    private Long id;

    @Column(name = "MESSAGE_ID", nullable = false, unique = true, length = 512)
    private String messageId;

    @Column(name = "MESSAGE_DATE")
    private OffsetDateTime messageDate;

    @Column(name = "MESSAGE_IN_FILE", nullable = false)
    @Builder.Default
    private boolean messageInFile = false;

    @Column(name = "MESSAGE_ALREADY_EXISTS", nullable = false)
    @Builder.Default
    private boolean messageAlreadyExists = false;

    @Column(name = "MESSAGE_EXPORTED", nullable = false)
    @Builder.Default
    private boolean messageExported = false;
}
