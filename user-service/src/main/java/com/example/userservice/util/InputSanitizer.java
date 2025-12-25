package com.example.userservice.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|onerror|onload)"
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script>|javascript:|onerror=|onload=|eval\\(|alert\\()"
    );

    @SuppressWarnings("unused")
    private static final Pattern SAFE_SEARCH_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,!?@#$%&*()]+$");

    public static String sanitizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "";
        }

        String trimmed = keyword.trim();

        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential SQL injection attempt detected: {}", trimmed);
            return "";
        }

        if (XSS_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential XSS attempt detected: {}", trimmed);
            return "";
        }

        String sanitized = trimmed.replaceAll("[<>\"'%;()&+]", "");

        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    public static String sanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String trimmed = email.trim().toLowerCase();

        if (!trimmed.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return null;
        }

        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential SQL injection in email: {}", trimmed);
            return null;
        }

        return trimmed;
    }

    public static String sanitizePathParameter(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }

        String trimmed = param.trim();

        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential SQL injection in path parameter: {}", trimmed);
            return null;
        }

        if (XSS_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential XSS in path parameter: {}", trimmed);
            return null;
        }

        String sanitized = trimmed.replaceAll("[<>\"'%;()&+\\s]", "");

        return sanitized;
    }

    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }

        return uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    public static String sanitizeText(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String trimmed = input.trim();

        trimmed = trimmed.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        trimmed = trimmed.replaceAll("(?i)javascript:", "");
        trimmed = trimmed.replaceAll("(?i)onerror=", "");
        trimmed = trimmed.replaceAll("(?i)onload=", "");

        if (trimmed.length() > 1000) {
            trimmed = trimmed.substring(0, 1000);
        }

        return trimmed;
    }
}

