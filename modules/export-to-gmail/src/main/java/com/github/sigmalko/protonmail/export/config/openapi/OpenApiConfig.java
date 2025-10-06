package com.github.sigmalko.protonmail.export.config.openapi;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${swagger.server.public.url}")
    private String swaggerPublicUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .openapi("3.1.0")
                .servers(Arrays.asList(
                    new Server()
                            .url(swaggerPublicUrl)
                            .description("Primary server exposing backend for Custom Models")
                ))
                .info(new Info()
                        .title("Proton-mail-export-to-gmail API")
                        .description("REST API that powers the proton-mail-export-to-gmail integration for ChatGPT Custom Models. The OpenAPI document is available at " + resolveOpenApiDocumentUrl())
                        .version("0.1.0"))
                .externalDocs(new ExternalDocumentation()
                        .description("Machine-readable OpenAPI specification")
                        .url(resolveOpenApiDocumentUrl()));
    }

    private String resolveOpenApiDocumentUrl() {
        if (swaggerPublicUrl == null || swaggerPublicUrl.isBlank()) {
            return "/openapi.json";
        }
        return swaggerPublicUrl.endsWith("/")
                ? swaggerPublicUrl + "openapi.json"
                : swaggerPublicUrl + "/openapi.json";
    }
}
