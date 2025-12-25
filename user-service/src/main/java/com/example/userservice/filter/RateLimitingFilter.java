package com.example.userservice.filter;

import com.example.userservice.config.RateLimitingConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate limiting filter for authentication and chat polling endpoints
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingConfig rateLimitingConfig;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Apply rate limiting to login endpoint
        if ("POST".equals(method) && path.contains("/api/auth/login")) {
            String clientIp = getClientIp(request);

            io.github.bucket4j.Bucket bucket = rateLimitingConfig.createLoginBucket(clientIp);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for login attempt from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Too many login attempts. Please try again later.\"}");
                return;
            }
        }

        // Apply rate limiting to register endpoint
        if ("POST".equals(method) && path.contains("/api/auth/register")) {
            String clientIp = getClientIp(request);
            String key = "register:" + clientIp;

            io.github.bucket4j.Bucket bucket = rateLimitingConfig.resolveBucket(key);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for registration attempt from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Too many registration attempts. Please try again later.\"}");
                return;
            }
        }

        // Apply rate limiting to chat polling endpoints
        if ("GET".equals(method) && (path.contains("/api/chats/waiting-staff") || 
                                      path.equals("/api/chats") || 
                                      path.matches("/api/chats/[a-f0-9\\-]+"))) {
            String identifier = getIdentifier(request);
            io.github.bucket4j.Bucket bucket = rateLimitingConfig.createChatPollingBucket(identifier);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for chat polling from: {} - path: {}", identifier, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please use WebSocket for real-time updates or increase polling interval to at least 2 seconds.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get identifier for rate limiting (email if authenticated, otherwise IP)
     */
    private String getIdentifier(HttpServletRequest request) {
        // Try to get user email from request attribute (set by JWT filter)
        String email = (String) request.getAttribute("userEmail");
        if (email != null && !email.isEmpty()) {
            return email;
        }
        // Fallback to IP address
        return getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

