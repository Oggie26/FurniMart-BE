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
 * This prevents unauthorized access to sensitive user information (password
 * hash, role)
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
        String providedServiceToken = request.getHeader("X-Service-Token");
        String authHeader = request.getHeader("Authorization");

        // 1. Check Service Token
        if (providedServiceToken != null && providedServiceToken.equals(serviceToken)) {
            // Only set internal-service authentication if no user JWT is present.
            // If a JWT is present, we skip setting context here to allow JwtAuthFilter
            // to populate the SecurityContext with the actual user's details.
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "internal-service",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Service token validated (no user context) for request to: {}", path);
            } else {
                log.debug("Service token validated (user context present) for request to: {}", path);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // 2. If no valid service token, let JwtAuthFilter handle user authentication
        // OR if it's already authenticated by JwtAuthFilter (if JwtAuthFilter runs
        // before this)
        // In SecurityConfig, serviceAuthFilter is added BEFORE JwtAuthFilter.
        // So we just continue the chain. If JwtAuthFilter finds a valid token, it will
        // set context.

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
