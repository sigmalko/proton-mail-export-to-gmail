package com.github.sigmalko.pmetg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j(topic = "HELLO_CONTROLLER")
@RequiredArgsConstructor
public class HelloController {

    private final ApiKeyContext apiKeyContext;
    private final OpenAiHeadersContext openAiHeadersContext;

    @GetMapping(value = "/hello", produces = "application/json")
    public ResponseEntity<String> hello() {
        apiKeyContext.apiKey()
                .ifPresentOrElse(
                        apiKey -> log.info("hello endpoint invoked with API key"),
                        () -> log.warn("hello endpoint invoked without API key")
                );
        logOpenAiHeaders();

        final var out = new StringBuffer();
        out.append("sms-phone-web, GITHUB_RUN_NUMBER=");
        out.append(System.getenv("GITHUB_RUN_NUMBER"));
        out.append(", DOCKER_TAG_VERSION=");
        out.append(System.getenv("DOCKER_TAG_VERSION")).append("");
        return ResponseEntity.ok(out.toString());
    }

    private void logOpenAiHeaders() {
        openAiHeadersContext.ephemeralUserId()
                .ifPresentOrElse(
                        value -> log.info(
                                "hello endpoint header {}={}",
                                OpenAiHeadersContext.EPHEMERAL_USER_ID_HEADER,
                                value
                        ),
                        () -> log.warn(
                                "hello endpoint header {} not provided",
                                OpenAiHeadersContext.EPHEMERAL_USER_ID_HEADER
                        )
                );
        openAiHeadersContext.conversationId()
                .ifPresentOrElse(
                        value -> log.info(
                                "hello endpoint header {}={}",
                                OpenAiHeadersContext.CONVERSATION_ID_HEADER,
                                value
                        ),
                        () -> log.warn(
                                "hello endpoint header {} not provided",
                                OpenAiHeadersContext.CONVERSATION_ID_HEADER
                        )
                );
        openAiHeadersContext.gptId()
                .ifPresentOrElse(
                        value -> log.info(
                                "hello endpoint header {}={}",
                                OpenAiHeadersContext.GPT_ID_HEADER,
                                value
                        ),
                        () -> log.warn(
                                "hello endpoint header {} not provided",
                                OpenAiHeadersContext.GPT_ID_HEADER
                        )
                );
    }
}
