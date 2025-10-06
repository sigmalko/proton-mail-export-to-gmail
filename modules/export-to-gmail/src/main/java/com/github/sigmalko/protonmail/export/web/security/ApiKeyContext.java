package com.github.sigmalko.protonmail.export.web.security;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ApiKeyContext {

    public static final String HEADER_NAME = "X-Api-Key";

    private String apiKey;

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Optional<String> apiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(apiKey);
    }
}
