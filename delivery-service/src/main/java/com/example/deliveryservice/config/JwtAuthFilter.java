package com.example.deliveryservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info("=== JwtAuthFilter.shouldNotFilter: {} {} ===", method, path);
        if (path == null) {
            log.debug("Path is null, not filtering.");
            return false;
        }
        // Skip JWT filter for public endpoints
        boolean shouldSkip = path.startsWith("/api/auth/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/swagger-ui.html") ||
                path.contains("/branch-info") ||
                path.startsWith("/static/") ||
                path.endsWith(".js") ||
                path.endsWith(".css");
        
        log.info("JwtAuthFilter.shouldNotFilter: {} {} -> shouldSkip: {}", method, path, shouldSkip);
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.info("=== JwtAuthFilter.doFilterInternal: {} {} ===", method, path);

        final String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header: {}", authHeader != null ? "present" : "missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid Authorization header, proceeding without authentication");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            log.error("JWT authentication failed for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\"Invalid or expired JWT token\"}");
            response.getWriter().flush();
            return;
        }
        log.debug("JWT authentication successful, proceeding with filter chain");
        filterChain.doFilter(request, response);

    }
}
