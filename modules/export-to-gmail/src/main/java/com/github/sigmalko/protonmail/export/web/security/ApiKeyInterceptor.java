package com.github.sigmalko.protonmail.export.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Slf4j(topic = "API_KEY_INTERCEPTOR")
@Component
@RequiredArgsConstructor
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyContext apiKeyContext;
    private final OpenAiHeadersContext openAiHeadersContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            Method method = handlerMethod.getMethod();
            if (method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)) {
                String apiKey = request.getHeader(ApiKeyContext.HEADER_NAME);
                apiKeyContext.setApiKey(apiKey);
                openAiHeadersContext.setEphemeralUserId(
                        request.getHeader(OpenAiHeadersContext.EPHEMERAL_USER_ID_HEADER)
                );
                openAiHeadersContext.setConversationId(
                        request.getHeader(OpenAiHeadersContext.CONVERSATION_ID_HEADER)
                );
                openAiHeadersContext.setGptId(request.getHeader(OpenAiHeadersContext.GPT_ID_HEADER));
                log.debug(
                        "Captured metadata for {} {} apiKeyPresent={} ephemeralUserIdPresent={} conversationIdPresent={} gptIdPresent={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        apiKey != null && !apiKey.isBlank(),
                        openAiHeadersContext.ephemeralUserId().isPresent(),
                        openAiHeadersContext.conversationId().isPresent(),
                        openAiHeadersContext.gptId().isPresent()
                );
            }
        }
        return true;
    }
}
