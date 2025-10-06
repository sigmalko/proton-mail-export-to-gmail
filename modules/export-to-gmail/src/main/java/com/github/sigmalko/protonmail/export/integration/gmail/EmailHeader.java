package com.github.sigmalko.protonmail.export.integration.gmail;

import java.time.Instant;

public record EmailHeader(
        int messageNumber,
        String messageId,
        Instant sentAt,
        String from
) {
}
