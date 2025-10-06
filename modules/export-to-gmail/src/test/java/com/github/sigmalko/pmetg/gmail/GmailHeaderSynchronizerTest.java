package com.github.sigmalko.pmetg.gmail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.sigmalko.pmetg.migrations.MigrationService;
import com.github.sigmalko.pmetg.migrations.MigrationService.MigrationFlag;
import com.github.sigmalko.pmetg.migrations.MigrationRepository.MigrationStatus;
import com.github.sigmalko.pmetg.problems.ProblemService;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GmailHeaderSynchronizerTest {

        private MigrationService migrationService;
        private ProblemService problemService;
        private GmailHeaderSynchronizer synchronizer;

        @BeforeEach
        void setUp() {
                migrationService = mock(MigrationService.class);
                problemService = mock(ProblemService.class);
                synchronizer = new GmailHeaderSynchronizer(migrationService, problemService);
        }

        @Test
        void synchronizeLogsProblemWhenMessageIdMissing() {
                final Instant sentAt = Instant.parse("2024-01-01T10:15:30Z");
                final EmailHeader header = new EmailHeader(1, " ", sentAt, "Alice <alice@example.com>");
                final var expectedDiagnostics = "Missing Message-ID header for Gmail message number " + header.messageNumber();

                synchronizer.synchronize(List.of(header));

                final ArgumentCaptor<OffsetDateTime> dateCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
                verify(problemService).logRemoteProblem(
                                dateCaptor.capture(),
                                eq(header.from()),
                                eq(expectedDiagnostics));
                verifyOffsetDate(dateCaptor.getValue(), sentAt);
                verifyNoMoreInteractions(migrationService);
        }

        @Test
        void synchronizeCreatesMigrationWhenNotExisting() {
                final Instant sentAt = Instant.parse("2024-02-02T02:02:02Z");
                final EmailHeader header = new EmailHeader(2, "<id-2>", sentAt, "Bob <bob@example.com>");

                when(migrationService.findByMessageId("<id-2>")).thenReturn(Optional.empty());

                synchronizer.synchronize(List.of(header));

                verify(migrationService).findByMessageId("<id-2>");
                verify(migrationService).createGmailMigration("<id-2>", OffsetDateTime.ofInstant(sentAt, ZoneOffset.UTC));
                verifyNoMoreInteractions(problemService);
        }

        @Test
        void synchronizeUpdatesExistingFlagWhenNeeded() {
                final EmailHeader header = new EmailHeader(3, "<id-3>", null, "Carol");
                final MigrationStatus status = new MigrationStatus("<id-3>", null, false, false, false);

                when(migrationService.findByMessageId("<id-3>")).thenReturn(Optional.of(status));

                synchronizer.synchronize(List.of(header));

                verify(migrationService).findByMessageId("<id-3>");
                verify(migrationService).updateFlagByMessageId("<id-3>", MigrationFlag.MESSAGE_ALREADY_EXISTS, true);
                verifyNoMoreInteractions(problemService);
        }

        @Test
        void synchronizeSkipsUpdatingFlagWhenAlreadySet() {
                final EmailHeader header = new EmailHeader(4, "<id-4>", null, "Dana");
                final MigrationStatus status = new MigrationStatus("<id-4>", null, false, true, false);

                when(migrationService.findByMessageId("<id-4>")).thenReturn(Optional.of(status));

                synchronizer.synchronize(List.of(header));

                verify(migrationService).findByMessageId("<id-4>");
                verify(migrationService, never())
                                .updateFlagByMessageId("<id-4>", MigrationFlag.MESSAGE_ALREADY_EXISTS, true);
                verifyNoMoreInteractions(problemService);
        }

        private void verifyOffsetDate(OffsetDateTime value, Instant expected) {
                assertThat(value).isEqualTo(OffsetDateTime.ofInstant(expected, ZoneOffset.UTC));
        }

}
