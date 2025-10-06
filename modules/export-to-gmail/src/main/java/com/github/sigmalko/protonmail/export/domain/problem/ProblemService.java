package com.github.sigmalko.protonmail.export.domain.problem;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j(topic = "protonmail-export.problem-service")
@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    @Transactional
    public ProblemEntity logFileProblem(
            String messageFile, OffsetDateTime messageDate, String messageFrom, String diagnostics) {
        ProblemEntity saved = problemRepository.save(buildProblem(
                messageDate,
                StringUtils.hasText(messageFile) ? messageFile : null,
                true,
                false,
                normalizeSender(messageFrom),
                diagnostics));
        log.debug("Stored file problem entry with id={} for file={}", saved.getId(), saved.getMessageFile());
        return saved;
    }

    @Transactional
    public ProblemEntity logRemoteProblem(
            OffsetDateTime messageDate, String messageFrom, String diagnostics) {
        ProblemEntity saved = problemRepository.save(buildProblem(
                messageDate, null, false, true, normalizeSender(messageFrom), diagnostics));
        log.debug("Stored remote problem entry with id={}", saved.getId());
        return saved;
    }

    private ProblemEntity buildProblem(
            OffsetDateTime messageDate,
            String messageFile,
            boolean isFile,
            boolean isRemote,
            String messageFrom,
            String diagnostics) {
        return ProblemEntity.builder()
                .messageDate(messageDate)
                .messageFile(messageFile)
                .messageIsFile(isFile)
                .messageIsRemote(isRemote)
                .messageFrom(messageFrom)
                .messageDiagnostics(diagnostics)
                .build();
    }

    private String normalizeSender(String messageFrom) {
        return StringUtils.hasText(messageFrom) ? messageFrom : null;
    }
}
