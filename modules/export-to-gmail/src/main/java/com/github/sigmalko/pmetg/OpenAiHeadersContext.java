package com.github.sigmalko.pmetg;

import java.util.Optional;

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

    private String ephemeralUserId;
    private String conversationId;
    private String gptId;

    public void setEphemeralUserId(String ephemeralUserId) {
        this.ephemeralUserId = ephemeralUserId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setGptId(String gptId) {
        this.gptId = gptId;
    }

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
