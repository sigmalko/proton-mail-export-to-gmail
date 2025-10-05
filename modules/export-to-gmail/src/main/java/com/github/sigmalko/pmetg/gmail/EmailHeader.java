package com.github.sigmalko.pmetg.gmail;

import java.time.Instant;

public record EmailHeader(
        int messageNumber,
        String messageId,
        Instant sentAt,
        String from
) {
}
