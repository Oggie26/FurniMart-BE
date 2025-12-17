package com.example.aiservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feign Client Interceptor to add service token for internal service calls
 * 
 * This automatically adds the X-Service-Token header when ai-service
 * calls user-service endpoints that require service authentication
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Value("${app.service-token:internal-service-token-12345}")
    private String serviceToken;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // Add service token header for internal service calls to user-service
        // Specifically for /api/auth/{email} endpoint
        if (requestTemplate.url().contains("/api/auth/")) {
            requestTemplate.header("X-Service-Token", serviceToken);
        }
    }
}
