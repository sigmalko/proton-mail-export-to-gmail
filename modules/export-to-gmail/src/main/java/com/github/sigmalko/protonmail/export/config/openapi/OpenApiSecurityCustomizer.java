package com.github.sigmalko.protonmail.export.config.openapi;

import java.util.List;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.sigmalko.protonmail.export.web.security.ApiKeyContext;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

@Configuration
public class OpenApiSecurityCustomizer {

    @Bean
    public org.springdoc.core.customizers.OpenApiCustomizer emptySecurityForModelPost() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, item) -> {
                    if (path == null || item == null) {
                        return;
                    }
                    if (item.getPost() != null) {
                        item.getPost().addExtension("x-openai-isConsequential", Boolean.TRUE);
                        ensureApiKeyHeaderParameter(item.getPost());
                    }
                    if (item.getGet() != null) {
                        item.getGet().addExtension("x-openai-isConsequential", Boolean.FALSE);
                        ensureApiKeyHeaderParameter(item.getGet());
                    }
                });

                clearSecurity(openApi.getPaths().get("/mail"), PathItem::getPost);
                clearSecurity(openApi.getPaths().get("/mail"), PathItem::getGet);
                clearSecurity(openApi.getPaths().get("/mail/bulk"), PathItem::getPost);
                clearSecurity(openApi.getPaths().get("/mail/{source}/last"), PathItem::getGet);
                clearSecurity(openApi.getPaths().get("/mail/all"), PathItem::getGet);

                ignoreForOpenAi(openApi.getPaths().get("/mail/bulk"), PathItem::getPost, "receiveBulkMail");
                ignoreForOpenAi(openApi.getPaths().get("/mail"), PathItem::getPost, "receiveMail");
                ignoreForOpenAi(openApi.getPaths().get("/hello"), PathItem::getGet, null);
            }
        };
    }

    private void ensureApiKeyHeaderParameter(Operation operation) {
        if (operation == null) {
            return;
        }

        if (operation.getParameters() != null
                && operation.getParameters().stream()
                        .anyMatch(parameter -> parameter != null
                                && ApiKeyContext.HEADER_NAME.equalsIgnoreCase(parameter.getName()))) {
            return;
        }

        Parameter parameter = new Parameter()
                .in("header")
                .name(ApiKeyContext.HEADER_NAME)
                .description("API key used by ChatGPT Custom GPT integrations")
                .required(false)
                .schema(new StringSchema());

        operation.addParametersItem(parameter);
    }

    private void clearSecurity(PathItem pathItem, Function<PathItem, Operation> operationExtractor) {
        if (pathItem == null) {
            return;
        }
        Operation operation = operationExtractor.apply(pathItem);
        if (operation != null) {
            operation.setSecurity(List.of());
        }
    }

    private void ignoreForOpenAi(
            PathItem pathItem,
            Function<PathItem, Operation> operationExtractor,
            String expectedOperationId
    ) {
        if (pathItem == null) {
            return;
        }
        Operation operation = operationExtractor.apply(pathItem);
        if (operation == null) {
            return;
        }

        if (expectedOperationId != null) {
            String operationId = operation.getOperationId();
            if (operationId != null && !operationId.equals(expectedOperationId)) {
                return;
            }
        }

        operation.addExtension("x-openai-ignore", Boolean.TRUE);
    }
}
