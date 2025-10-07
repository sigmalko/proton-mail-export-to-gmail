package com.github.sigmalko.protonmail.export.web.support;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class RequestUtils {

    private static final List<String> CLIENT_IP_HEADERS = List.of("x-real-ip", "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP");

    public static void logHeadersAndParams(HttpServletRequest request, String label) {
        final var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final var name = headerNames.nextElement();
            final var value = request.getHeader(name);
            log.info("{}|Header|{}={}", label, name, value);
        }

        final var paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            final var name = paramNames.nextElement();
            final var value = request.getParameter(name);
            log.info("{}|Parameter|{}={}", label, name, value);
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        return Stream.concat(
                        CLIENT_IP_HEADERS.stream().map(request::getHeader),
                        Stream.of(request.getRemoteAddr()))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .filter(Predicate.not(ip -> "unknown".equalsIgnoreCase(ip)))
                .findFirst()
                .orElse(null);
    }

    public static boolean isAndroid(final HttpServletRequest request) {
        try {
            return headerContainsIgnoreCase(request, "User-Agent", "android")
                    || headerContainsIgnoreCase(request, "sec-ch-ua-platform", "android");
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage(), e);
            return false;
        }
    }

    public static boolean isWindows(final HttpServletRequest request) {
        try {
            return headerContainsIgnoreCase(request, "sec-ch-ua", "microsoft edge");
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage(), e);
            return false;
        }
    }

    public static boolean isBot(final HttpServletRequest request) {
        try {
            return headerContainsIgnoreCase(request, "User-Agent", "robot", "bot");
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage(), e);
            return false;
        }
    }

    private static boolean headerContainsIgnoreCase(HttpServletRequest request, String header, String... fragments) {
        if (request == null || header == null || fragments == null || fragments.length == 0) {
            return false;
        }

        final var headerValue = request.getHeader(header);
        if (headerValue == null || headerValue.isBlank()) {
            return false;
        }

        final var normalizedHeader = headerValue.toLowerCase(Locale.ROOT);
        return Arrays.stream(fragments)
                .filter(Objects::nonNull)
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedHeader::contains);
    }
}
