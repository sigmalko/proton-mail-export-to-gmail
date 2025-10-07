package com.github.sigmalko.protonmail.export.integration.gmail;

import com.github.sigmalko.protonmail.export.domain.migration.MigrationRepository.MigrationStatus;
import com.github.sigmalko.protonmail.export.domain.migration.MigrationService;
import com.github.sigmalko.protonmail.export.domain.migration.MigrationService.MigrationFlag;
import com.github.sigmalko.protonmail.export.domain.problem.ProblemService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j(topic = "GmailHeaderSynchronizer")
@Component
@RequiredArgsConstructor
public class GmailHeaderSynchronizer {

        private final MigrationService migrationService;
        private final ProblemService problemService;

        public void synchronize(List<EmailHeader> headers) {
                headers.forEach(this::synchronizeHeader);
        }

        private void synchronizeHeader(EmailHeader header) {
                final var messageDate = header.sentAt() != null
                                ? OffsetDateTime.ofInstant(header.sentAt(), ZoneOffset.UTC)
                                : null;

                try {
                        if (!StringUtils.hasText(header.messageId())) {
                                log.debug(
                                                "Skipping Gmail message {} because it does not contain Message-ID header.",
                                                header.messageNumber());
                                problemService.logRemoteProblem(
                                                messageDate,
                                                header.from(),
                                                "Missing Message-ID header for Gmail message number "
                                                                + header.messageNumber());
                                return;
                        }

                        final var existing = migrationService.findByMessageId(header.messageId());
                        if (existing.isEmpty()) {
                                log.debug(
                                                "Skipping Gmail message {} because it was not discovered in local files.",
                                                header.messageId());
                                return;
                        }

                        markMessageAsExisting(existing.orElseThrow());
                } catch (Exception exception) {
                        if (!StringUtils.hasText(header.messageId())) {
                                log.warn(
                                                "Failed to log missing Message-ID problem for Gmail message {}.",
                                                header.messageNumber(),
                                                exception);
                                return;
                        }

                        log.warn(
                                        "Failed to persist Gmail message {} (messageId={}).",
                                        header.messageNumber(),
                                        header.messageId(),
                                        exception);
                }
        }

        private void markMessageAsExisting(MigrationStatus status) {
                if (status.messageInGmail()) {
                        return;
                }

                migrationService.updateFlagByMessageId(
                                status.messageId(), MigrationFlag.MESSAGE_IN_GMAIL, true);
        }
}
