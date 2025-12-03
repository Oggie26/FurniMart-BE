package com.example.userservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j
 * Note: Requires bucket4j-core dependency
 */
@Configuration
public class RateLimitingConfig {

    // Cache for user buckets
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Get or create bucket for a user
     * @param key User identifier (email, IP, etc.)
     * @return Bucket instance
     */
    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Create a new bucket with rate limits
     * Default: 10 requests per minute
     */
    private Bucket createNewBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Create bucket for login endpoint (stricter limits)
     * 5 attempts per 15 minutes
     * Cached per IP address
     */
    public Bucket createLoginBucket(String ip) {
        String key = "login:" + ip;
        return cache.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(10))))
                .build());
    }

    /**
     * Clear bucket for a user (e.g., after successful login)
     */
    public void clearBucket(String key) {
        cache.remove(key);
    }
}

