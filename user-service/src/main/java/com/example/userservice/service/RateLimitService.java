package com.example.userservice.service;

import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int FORGOT_PASSWORD_LIMIT = 5; // 5 requests
    private static final int FORGOT_PASSWORD_WINDOW_MINUTES = 15; // per 15 minutes
    private static final int VERIFY_EMAIL_LIMIT = 10; // 10 requests
    private static final int VERIFY_EMAIL_WINDOW_MINUTES = 60; // per hour
    private static final int RESEND_VERIFICATION_LIMIT = 3; // 3 requests
    private static final int RESEND_VERIFICATION_WINDOW_MINUTES = 60; // per hour

    /**
     * Check rate limit for forgot password requests
     */
    public void checkForgotPasswordRateLimit(String email) {
        String key = "rate_limit:forgot_password:" + email;
        checkRateLimit(key, FORGOT_PASSWORD_LIMIT, FORGOT_PASSWORD_WINDOW_MINUTES, "forgot password");
    }

    /**
     * Check rate limit for email verification attempts
     */
    public void checkVerifyEmailRateLimit(String token) {
        String key = "rate_limit:verify_email:" + token;
        checkRateLimit(key, VERIFY_EMAIL_LIMIT, VERIFY_EMAIL_WINDOW_MINUTES, "email verification");
    }

    /**
     * Check rate limit for resend verification email
     */
    public void checkResendVerificationRateLimit(String email) {
        String key = "rate_limit:resend_verification:" + email;
        checkRateLimit(key, RESEND_VERIFICATION_LIMIT, RESEND_VERIFICATION_WINDOW_MINUTES, "resend verification");
    }

    /**
     * Generic rate limit check
     * If Redis is unavailable, fails open (allows request) to prevent service disruption
     */
    private void checkRateLimit(String key, int limit, int windowMinutes, String operation) {
        try {
            Integer currentCount = (Integer) redisTemplate.opsForValue().get(key);
            
            if (currentCount == null) {
                // First request in this window
                redisTemplate.opsForValue().set(key, 1, windowMinutes, TimeUnit.MINUTES);
                return;
            }
            
            if (currentCount >= limit) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                log.warn("Rate limit exceeded for {}: key={}, count={}, limit={}, ttl={}s", 
                        operation, key, currentCount, limit, ttl);
                throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
        } catch (AppException e) {
            // Re-throw rate limit exceptions
            throw e;
        } catch (Exception e) {
            // If Redis is unavailable, log warning but allow request (fail open)
            // This prevents service disruption if Redis is down
            log.error("Redis unavailable for rate limiting (operation: {}): {}. Allowing request.", 
                    operation, e.getMessage());
            // Fail open: allow request if Redis is down
            // Alternative: fail closed (more secure) - uncomment below
            // throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Reset rate limit for a key (useful for testing or manual reset)
     */
    public void resetRateLimit(String key) {
        redisTemplate.delete(key);
    }
}



