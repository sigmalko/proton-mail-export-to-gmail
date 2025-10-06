package com.github.sigmalko.protonmail.export.web.support;

import java.util.Enumeration;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RequestUtils {

    private RequestUtils() {
    }

    public static void logHeadersAndParams(HttpServletRequest request, String label) {
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            final String name = headerNames.nextElement();
            final String value = request.getHeader(name);
            log.info("{}|Header|{}={}", label, name, value);
        }

        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            final String name = paramNames.nextElement();
            final String value = request.getParameter(name);
            log.info("{}|Parameter|{}={}", label, name, value);
        }
    }

    public static String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-real-ip");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Forwarded-For");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
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

        final String headerValue = request.getHeader(header);
        if (headerValue == null || headerValue.isEmpty()) {
            return false;
        }

        final String headerLowerCase = headerValue.toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (fragment != null && !fragment.isEmpty()) {
                if (headerLowerCase.contains(fragment.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}
