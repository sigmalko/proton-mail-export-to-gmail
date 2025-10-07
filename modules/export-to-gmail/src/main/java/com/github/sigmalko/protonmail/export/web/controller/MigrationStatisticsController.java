package com.github.sigmalko.protonmail.export.web.controller;

import com.github.sigmalko.protonmail.export.domain.migration.MigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migrations/statistics")
@RequiredArgsConstructor
@Tag(
        name = "Migration statistics",
        description = "Custom GPT helper: endpoints that provide aggregated counts summarising migration progress between exported files and Gmail."
)
public class MigrationStatisticsController {

    private final MigrationService migrationService;

    @GetMapping("/messages-present-in-gmail-and-files/count")
    @Operation(
            summary = "Count messages synchronised between export files and Gmail",
            description = "Custom GPT: Call this to learn how many Proton Mail messages are already present both in the exported files and in Gmail (messageInGmail=true AND messageInFile=true)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Count for messages present in both Gmail and files",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponse.class))
    )
    public CountResponse countMessagesPresentInGmailAndFiles() {
        final var count = migrationService.countMessagesPresentInGmailAndFiles();
        return new CountResponse(count);
    }

    @GetMapping("/messages-missing-in-gmail/count")
    @Operation(
            summary = "Count messages missing in Gmail but stored in export files",
            description = "Custom GPT: Use this when you need the number of Proton Mail messages still absent from Gmail while existing in the exported files (messageInGmail=false AND messageInFile=true)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Count for messages missing in Gmail and present in files",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponse.class))
    )
    public CountResponse countMessagesMissingInGmailButInFiles() {
        final var count = migrationService.countMessagesMissingInGmailButInFiles();
        return new CountResponse(count);
    }

    @Schema(description = "Wrapper for a single numeric statistic value.")
    public record CountResponse(@Schema(description = "Value of the requested migration statistic.") long count) {}
}
