package com.github.sigmalko.protonmail.export.web.controller;

import com.github.sigmalko.protonmail.export.domain.migration.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migrations/messages")
@RequiredArgsConstructor
@Tag(
        name = "Migration message lookup",
        description = "Custom GPT helper: endpoints supporting targeted follow-up actions for Proton Mail exports that are not yet present in Gmail."
)
public class MigrationMessageQueryController {

    private final MigrationService migrationService;

    @GetMapping("/missing-in-gmail")
    @Operation(
            summary = "List message identifiers missing in Gmail but present in export files",
            description = "Custom GPT: Invoke this to retrieve the MESSAGE_ID values for Proton Mail exports awaiting Gmail import (messageInGmail=false AND messageInFile=true)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of message identifiers still absent from Gmail",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MissingMessageIdsResponse.class)
            )
    )
    public MissingMessageIdsResponse listMessageIdsMissingInGmailButInFiles() {
        final var messageIds = migrationService.findMessageIdsMissingInGmailButInFiles();
        return new MissingMessageIdsResponse(messageIds);
    }

    @Schema(description = "Collection of message identifiers that require Gmail import.")
    public record MissingMessageIdsResponse(
            @ArraySchema(schema = @Schema(description = "A single Proton Mail message identifier.")) List<String> messageIds) {}
}
