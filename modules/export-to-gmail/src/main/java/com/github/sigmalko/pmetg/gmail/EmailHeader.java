package com.github.sigmalko.pmetg.gmail;

import java.time.Instant;

public record EmailHeader(
        String from,
        String to,
        Instant sentAt,
        String messageId
) {
}
