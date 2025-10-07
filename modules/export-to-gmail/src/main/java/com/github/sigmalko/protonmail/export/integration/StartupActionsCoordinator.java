package com.github.sigmalko.protonmail.export.integration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.sigmalko.protonmail.export.integration.eml.EmlEmailLoggingRunner;
import com.github.sigmalko.protonmail.export.integration.gmail.GmailImapFetchRunner;

@Slf4j(topic = "StartupActionsCoordinator")
@Component
@RequiredArgsConstructor
public class StartupActionsCoordinator {

    private final ObjectProvider<EmlEmailLoggingRunner> emlEmailLoggingRunnerProvider;
    private final ObjectProvider<GmailImapFetchRunner> gmailImapFetchRunnerProvider;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void executeStartupActions() {
        final var emlRunner = emlEmailLoggingRunnerProvider.getIfAvailable();
        final var gmailRunner = gmailImapFetchRunnerProvider.getIfAvailable();

        if (emlRunner == null) {
            log.debug("EmlEmailLoggingRunner bean is not available. Proceeding directly to GmailImapFetchRunner.");
            runGmailRunner(gmailRunner);
            return;
        }

        if (executeEmlRunner(emlRunner)) {
            runGmailRunner(gmailRunner);
        }
    }

    private boolean executeEmlRunner(EmlEmailLoggingRunner emlRunner) {
        try {
            emlRunner.run();
            return true;
        } catch (RuntimeException exception) {
            log.error("EmlEmailLoggingRunner failed. GmailImapFetchRunner will not run.", exception);
            return false;
        }
    }

    private void runGmailRunner(GmailImapFetchRunner gmailRunner) {
        if (gmailRunner == null) {
            log.debug("GmailImapFetchRunner bean is not available. Skipping Gmail IMAP fetch.");
            return;
        }

        try {
            gmailRunner.run();
        } catch (RuntimeException exception) {
            log.error("GmailImapFetchRunner failed.", exception);
        }
    }
}
