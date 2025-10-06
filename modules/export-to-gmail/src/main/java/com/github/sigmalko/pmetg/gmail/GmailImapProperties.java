package com.github.sigmalko.pmetg.gmail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "gmail.imap")
public record GmailImapProperties(
        @DefaultValue("imap.gmail.com") String host,
        @DefaultValue("993") int port,
        @DefaultValue("true") boolean sslEnabled,
        @DefaultValue("INBOX") String folder,
        String username,
        String password,
        @DefaultValue("50") int messageLimit
) {
}
