package com.github.sigmalko.protonmail.export.web.security;

import java.util.Optional;

import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OpenAiHeadersContext {

    public static final String EPHEMERAL_USER_ID_HEADER = "openai-ephemeral-user-id";
    public static final String CONVERSATION_ID_HEADER = "openai-conversation-id";
    public static final String GPT_ID_HEADER = "openai-gpt-id";

    @Setter
    private String ephemeralUserId;
    @Setter
    private String conversationId;
    @Setter
    private String gptId;

    public Optional<String> ephemeralUserId() {
        return optionalOf(ephemeralUserId);
    }

    public Optional<String> conversationId() {
        return optionalOf(conversationId);
    }

    public Optional<String> gptId() {
        return optionalOf(gptId);
    }

    private Optional<String> optionalOf(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
