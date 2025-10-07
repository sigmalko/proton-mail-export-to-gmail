package com.github.sigmalko.protonmail.export.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.sigmalko.protonmail.export.web.security.ApiKeyInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;
    private final SpringMvcCorsProperties corsProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsProperties.getMappings().isEmpty()) {
            corsProperties.getMappings().forEach((path, configuration) ->
                    registry.addMapping(path).combine(configuration)
            );
        } else {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("POST", "PUT", "PATCH", "GET", "OPTIONS", "DELETE")
                    .allowedHeaders(
                            "Content-Type",
                            "Access-Control-Allow-Headers",
                            "Access-Control-Allow-Origin",
                            "Authorization",
                            "X-Requested-With",
                            "token",
                            "asnu",
                            "rsnu"
                    )
                    .maxAge(3600);
        }
    }
}
