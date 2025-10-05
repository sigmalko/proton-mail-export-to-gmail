package com.github.sigmalko.pmetg;

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
        final String actionName = buildActionName(request);
        RequestUtils.logHeadersAndParams(request, actionName);
        final String clientIp = RequestUtils.getClientIp(request);
        final boolean android = RequestUtils.isAndroid(request);
        final boolean windows = RequestUtils.isWindows(request);
        final boolean bot = RequestUtils.isBot(request);

        log.info(
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
        final String method = request.getMethod();
        final String uri = request.getRequestURI();
        final String query = request.getQueryString();
        if (query == null || query.isEmpty()) {
            return method + " " + uri;
        }
        return method + " " + uri + '?' + query;
    }
}
