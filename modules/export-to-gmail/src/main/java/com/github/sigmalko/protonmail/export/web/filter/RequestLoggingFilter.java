package com.github.sigmalko.protonmail.export.web.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.sigmalko.protonmail.export.web.support.RequestUtils;

@Slf4j(topic = "REQUEST_LOGGING_FILTER")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final var actionName = buildActionName(request);
        RequestUtils.logHeadersAndParams(request, actionName);
        final var clientIp = RequestUtils.getClientIp(request);
        final var android = RequestUtils.isAndroid(request);
        final var windows = RequestUtils.isWindows(request);
        final var bot = RequestUtils.isBot(request);

        log.debug(
                "{}|ClientIp={} android={} windows={} bot={}",
                actionName,
                clientIp,
                android,
                windows,
                bot
        );

        filterChain.doFilter(request, response);
    }

    private String buildActionName(HttpServletRequest request) {
        final var method = request.getMethod();
        final var uri = request.getRequestURI();
        final var query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return method + " " + uri;
        }
        return method + " " + uri + '?' + query;
    }
}
