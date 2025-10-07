package com.github.sigmalko.protonmail.export.integration.eml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "eml.reader")
public record EmlReaderProperties(
        @DefaultValue("false") boolean enabled,
        String directory) {
}
