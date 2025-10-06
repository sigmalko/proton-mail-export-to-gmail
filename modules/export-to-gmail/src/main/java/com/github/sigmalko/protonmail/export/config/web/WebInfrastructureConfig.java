package com.github.sigmalko.protonmail.export.config.web;

import java.time.Clock;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.github.sigmalko.protonmail.export.web.filter.CORSFilter;

@Configuration
public class WebInfrastructureConfig {

    @Bean
    FilterRegistrationBean<CORSFilter> corsFilterRegistration(CORSFilter corsFilter) {
        final var registrationBean = new FilterRegistrationBean<>(corsFilter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
