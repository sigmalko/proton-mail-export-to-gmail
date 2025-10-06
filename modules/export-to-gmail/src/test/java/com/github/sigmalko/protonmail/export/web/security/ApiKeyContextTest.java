package com.github.sigmalko.protonmail.export.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiKeyContextTest {

    private ApiKeyContext apiKeyContext;

    @BeforeEach
    void setUp() {
        apiKeyContext = new ApiKeyContext();
    }

    @Test
    void apiKeyReturnsEmptyOptionalWhenKeyIsNotSet() {
        assertThat(apiKeyContext.apiKey()).isEmpty();
    }

    @Test
    void apiKeyReturnsEmptyOptionalWhenKeyIsBlank() {
        apiKeyContext.setApiKey("   ");

        assertThat(apiKeyContext.apiKey()).isEmpty();
    }

    @Test
    void apiKeyReturnsOptionalWithKeyWhenKeyIsProvided() {
        apiKeyContext.setApiKey("secret-key");

        assertThat(apiKeyContext.apiKey()).contains("secret-key");
    }
}
