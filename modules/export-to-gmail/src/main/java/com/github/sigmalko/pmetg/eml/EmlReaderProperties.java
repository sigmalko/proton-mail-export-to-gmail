package com.github.sigmalko.pmetg.eml;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eml.reader")
public record EmlReaderProperties(String directory) {
}
