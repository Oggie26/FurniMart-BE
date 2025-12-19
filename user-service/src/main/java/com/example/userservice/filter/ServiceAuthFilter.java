package com.example.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter to protect internal service endpoints (like /api/auth/{email})
 * Only allows requests from internal services with valid service token
 * 
 * This prevents unauthorized access to sensitive user information (password hash, role)
 */
@Component
@Order(0) // Run before other filters
@Slf4j
public class ServiceAuthFilter extends OncePerRequestFilter {

    @Value("${app.service-token:internal-service-token-12345}")
    private String serviceToken;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Protect GET /api/auth/{email} endpoint - only allow internal services
        if ("GET".equals(method) && path.matches("/api/auth/[^/]+")) {
            String providedToken = request.getHeader("X-Service-Token");
            
            if (providedToken == null || !providedToken.equals(serviceToken)) {
                log.warn("Unauthorized service access attempt to {} from IP: {}", path, getClientIp(request));
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"message\":\"Access denied. Service token required.\"}");
                return;
            }
            
            // Mark request as authenticated for internal service
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "internal-service",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Service token validated for internal service request to: {}", path);
        }

        filterChain.doFilter(request, response);
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
