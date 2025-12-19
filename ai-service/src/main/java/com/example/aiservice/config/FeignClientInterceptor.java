package com.example.aiservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign Client Interceptor to:
 * 1. Forward Authorization header from incoming requests
 * 2. Add service token for internal service calls to user-service
 */
@Slf4j
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
        // Check URL and path for /api/auth/ endpoint
        String url = template.url();
        String path = template.path();
        
        boolean isAuthEndpoint = (url != null && url.contains("/api/auth/")) 
                                || (path != null && path.contains("/api/auth/"));
        
        if (isAuthEndpoint) {
            template.header("X-Service-Token", serviceToken);
            log.info("✅ Added X-Service-Token header for auth endpoint request (url: {}, path: {})", 
                     url, path);
        } else {
            log.debug("Skipping X-Service-Token for non-auth endpoint: url={}, path={}", url, path);
        }
    }
}
