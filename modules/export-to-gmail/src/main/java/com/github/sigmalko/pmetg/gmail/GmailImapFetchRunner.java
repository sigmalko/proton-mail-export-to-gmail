package com.github.sigmalko.pmetg.gmail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j(topic = "GmailImapFetchRunner")
@Component
@RequiredArgsConstructor
public class GmailImapFetchRunner implements CommandLineRunner {

        private final GmailImapFetcher gmailImapFetcher;
        private final ApplicationContext applicationContext;

        @Override
        public void run(String... args) {
                log.info("##################################################");
                log.info("Fetching latest email headers from Gmail via IMAP...");
                log.info("##################################################");

                gmailImapFetcher.fetchLatestHeaders();
                log.info("IMAP header fetch complete. Shutting down the application.");
                // final int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                // System.exit(exitCode);
        }
}
