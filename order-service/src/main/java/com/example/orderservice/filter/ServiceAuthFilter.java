package com.example.orderservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Order(0)
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

        if (providedServiceToken != null && providedServiceToken.equals(serviceToken)) {
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

        filterChain.doFilter(request, response);
    }
}
