package com.github.sigmalko.protonmail.export.domain.problem;

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
@Table(name = "PROBLEMS")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemEntity {

    @Id
    @SequenceGenerator(name = "problems_seq", sequenceName = "PROBLEMS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "problems_seq")
    private Long id;

    @Column(name = "MESSAGE_DATE")
    private OffsetDateTime messageDate;

    @Column(name = "MESSAGE_FILE", length = 1024)
    private String messageFile;

    @Builder.Default
    @Column(name = "MESSAGE_IS_FILE", nullable = false)
    private boolean messageIsFile = false;

    @Builder.Default
    @Column(name = "MESSAGE_IS_REMOTE", nullable = false)
    private boolean messageIsRemote = false;

    @Column(name = "MESSAGE_FROM", length = 512)
    private String messageFrom;

    @Column(name = "MESSAGE_DIAGNOSTICS", length = 2048)
    private String messageDiagnostics;
}
