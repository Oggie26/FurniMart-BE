package com.example.userservice.service;

import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Tests")
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should allow first request (no existing count)")
    void testCheckForgotPasswordRateLimit_FirstRequest() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(null);
        lenient().when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(900L);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkForgotPasswordRateLimit(email));
        verify(valueOperations).set(anyString(), eq(1), eq(15L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should allow request when count is below limit")
    void testCheckForgotPasswordRateLimit_BelowLimit() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(3); // Below limit of 5
        lenient().when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(900L);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkForgotPasswordRateLimit(email));
        verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("Should throw exception when rate limit exceeded")
    void testCheckForgotPasswordRateLimit_Exceeded() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(5); // At limit
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(900L);

        // When & Then
        AppException exception = assertThrows(AppException.class, 
            () -> rateLimitService.checkForgotPasswordRateLimit(email));
        
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(valueOperations, never()).increment(anyString());
    }

    @Test
    @DisplayName("Should allow request when Redis is unavailable (fail open)")
    void testCheckForgotPasswordRateLimit_RedisUnavailable() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - Should not throw exception (fail open)
        assertDoesNotThrow(() -> rateLimitService.checkForgotPasswordRateLimit(email));
    }

    @Test
    @DisplayName("Should allow request when Redis set operation fails")
    void testCheckForgotPasswordRateLimit_RedisSetFails() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(null);
        doThrow(new RuntimeException("Redis set failed")).when(valueOperations)
            .set(anyString(), anyInt(), anyLong(), any(TimeUnit.class));

        // When & Then - Should not throw exception (fail open)
        assertDoesNotThrow(() -> rateLimitService.checkForgotPasswordRateLimit(email));
    }

    @Test
    @DisplayName("Should check email verification rate limit")
    void testCheckVerifyEmailRateLimit() {
        // Given
        String token = "verification-token-123";
        when(valueOperations.get(anyString())).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkVerifyEmailRateLimit(token));
        verify(valueOperations).set(anyString(), eq(1), eq(60L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should check resend verification rate limit")
    void testCheckResendVerificationRateLimit() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkResendVerificationRateLimit(email));
        verify(valueOperations).set(anyString(), eq(1), eq(60L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should reset rate limit")
    void testResetRateLimit() {
        // Given
        String key = "rate_limit:forgot_password:test@example.com";
        when(redisTemplate.delete(key)).thenReturn(true);

        // When
        rateLimitService.resetRateLimit(key);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    @DisplayName("Should handle increment failure gracefully")
    void testCheckRateLimit_IncrementFails() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(2);
        doThrow(new RuntimeException("Redis increment failed")).when(valueOperations).increment(anyString());

        // When & Then - Should not throw exception (fail open)
        assertDoesNotThrow(() -> rateLimitService.checkForgotPasswordRateLimit(email));
    }

    @Test
    @DisplayName("Should re-throw AppException (rate limit exceeded)")
    void testCheckRateLimit_ReThrowAppException() {
        // Given
        String email = "test@example.com";
        when(valueOperations.get(anyString())).thenReturn(5);
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(900L);

        // When & Then
        AppException exception = assertThrows(AppException.class, 
            () -> rateLimitService.checkForgotPasswordRateLimit(email));
        
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
    }
}

