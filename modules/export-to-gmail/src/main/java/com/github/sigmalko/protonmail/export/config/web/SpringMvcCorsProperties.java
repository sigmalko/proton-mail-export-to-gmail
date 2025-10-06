package com.github.sigmalko.protonmail.export.config.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(prefix = "spring.mvc.cors")
public class SpringMvcCorsProperties {

    private Map<String, CorsConfiguration> mappings = new LinkedHashMap<>();

    public Map<String, CorsConfiguration> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, CorsConfiguration> mappings) {
        this.mappings = mappings;
    }
}
