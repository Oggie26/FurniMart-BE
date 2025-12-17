package com.example.aiservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign Client Interceptor to:
 * 1. Forward Authorization header from incoming requests
 * 2. Add service token for internal service calls to user-service
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${app.service-token:internal-service-token-12345}")
    private String serviceToken;

    @Override
    public void apply(RequestTemplate template) {
        // Forward Authorization header from incoming request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                template.header("Authorization", authorization);
            }
        }

        // Add service token header for internal service calls to user-service
        // Specifically for /api/auth/{email} endpoint
        if (template.url().contains("/api/auth/")) {
            template.header("X-Service-Token", serviceToken);
        }
    }
}
