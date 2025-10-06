package com.github.sigmalko.protonmail.export.config.web;

import java.time.Clock;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.github.sigmalko.protonmail.export.web.filter.CORSFilter;

@Configuration
public class WebInfrastructureConfig {

    @Bean
    FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistration() {
        final var filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        filter.setForceRequestEncoding(true);
        filter.setForceResponseEncoding(false);

        final var registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

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
