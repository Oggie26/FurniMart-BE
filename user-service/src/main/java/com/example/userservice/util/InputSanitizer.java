package com.example.userservice.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing and validating user inputs to prevent injection attacks
 */
@Component
public class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    // Pattern to detect SQL injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|onerror|onload)"
    );

    // Pattern to detect XSS attempts
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script>|javascript:|onerror=|onload=|eval\\(|alert\\()"
    );

    // Pattern for safe search keywords (alphanumeric, spaces, and common punctuation)
    // Note: Currently using manual sanitization, but keeping pattern for future use
    @SuppressWarnings("unused")
    private static final Pattern SAFE_SEARCH_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,!?@#$%&*()]+$");

    /**
     * Sanitize search keyword to prevent SQL injection
     * @param keyword The search keyword to sanitize
     * @return Sanitized keyword or empty string if dangerous
     */
    public static String sanitizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "";
        }

        String trimmed = keyword.trim();

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential SQL injection attempt detected: {}", trimmed);
            return "";
        }

        // Check for XSS patterns
        if (XSS_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential XSS attempt detected: {}", trimmed);
            return "";
        }

        // Remove any remaining dangerous characters
        String sanitized = trimmed.replaceAll("[<>\"'%;()&+]", "");

        // Limit length to prevent DoS
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }

        return sanitized;
    }

    /**
     * Validate and sanitize email input
     * @param email The email to validate
     * @return Sanitized email or null if invalid
     */
    public static String sanitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        String trimmed = email.trim().toLowerCase();

        // Basic email validation
        if (!trimmed.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return null;
        }

        // Check for SQL injection
        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential SQL injection in email: {}", trimmed);
            return null;
        }

        return trimmed;
    }

    /**
     * Sanitize path parameter (ID, UUID, etc.)
     * @param param The path parameter to sanitize
     * @return Sanitized parameter or null if invalid
     */
    public static String sanitizePathParameter(String param) {
        if (param == null || param.trim().isEmpty()) {
            return null;
        }

        String trimmed = param.trim();

        // Check for SQL injection
        if (SQL_INJECTION_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential SQL injection in path parameter: {}", trimmed);
            return null;
        }

        // Check for XSS
        if (XSS_PATTERN.matcher(trimmed).find()) {
            log.warn("Potential XSS in path parameter: {}", trimmed);
            return null;
        }

        // Remove dangerous characters
        String sanitized = trimmed.replaceAll("[<>\"'%;()&+\\s]", "");

        return sanitized;
    }

    /**
     * Validate UUID format
     * @param uuid The UUID string to validate
     * @return true if valid UUID format
     */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }

        return uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    /**
     * Sanitize general text input
     * @param input The text to sanitize
     * @return Sanitized text
     */
    public static String sanitizeText(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String trimmed = input.trim();

        // Remove script tags and dangerous patterns
        trimmed = trimmed.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        trimmed = trimmed.replaceAll("(?i)javascript:", "");
        trimmed = trimmed.replaceAll("(?i)onerror=", "");
        trimmed = trimmed.replaceAll("(?i)onload=", "");

        // Limit length
        if (trimmed.length() > 1000) {
            trimmed = trimmed.substring(0, 1000);
        }

        return trimmed;
    }
}

