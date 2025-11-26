package com.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveToken(String username, String token, long jwtExpiration) {
        try {
            String key = "TOKEN:" + username;
            redisTemplate.opsForValue().set(key, token, jwtExpiration, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to save token to Redis for user: {}. Error: {}", username, e.getMessage());
            // Fail gracefully - token will still be returned to user, just not stored in Redis
            // This allows login to succeed even if Redis is down
        }
    }

    public void saveRefreshToken(String username, String refreshToken, long refreshExpiration) {
        try {
            String key = "REFRESH_TOKEN:" + username;
            redisTemplate.opsForValue().set(key, refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to save refresh token to Redis for user: {}. Error: {}", username, e.getMessage());
            // Fail gracefully - refresh token will still be returned to user
        }
    }

    public String getToken(String username) {
        try {
            String key = "TOKEN:" + username;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get token from Redis for user: {}. Error: {}", username, e.getMessage());
            return null; // Return null if Redis is unavailable
        }
    }

    public String getRefreshToken(String username) {
        try {
            String key = "REFRESH_TOKEN:" + username;
            return (String) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get refresh token from Redis for user: {}. Error: {}", username, e.getMessage());
            return null; // Return null if Redis is unavailable
        }
    }

    public void deleteToken(String username) {
        try {
            redisTemplate.delete("TOKEN:" + username);
            redisTemplate.delete("REFRESH_TOKEN:" + username);
        } catch (Exception e) {
            log.error("Failed to delete token from Redis for user: {}. Error: {}", username, e.getMessage());
            // Fail gracefully
        }
    }

    public void blacklistToken(String token, long remainingTime) {
        try {
            // Chỉ blacklist nếu token còn thời gian
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(
                        "JWT_BLACKLIST:" + token,
                        "true",
                        remainingTime,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis. Error: {}", e.getMessage());
            // Fail gracefully - token blacklisting won't work if Redis is down
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("JWT_BLACKLIST:" + token));
        } catch (Exception e) {
            log.error("Failed to check token blacklist in Redis. Error: {}", e.getMessage());
            return false; // Fail open - allow token if Redis is unavailable
        }
    }

    public Long getTokenTTL(String username) {
        try {
            String key = "TOKEN:" + username;
            return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Failed to get token TTL from Redis for user: {}. Error: {}", username, e.getMessage());
            return null;
        }
    }

    public boolean extendTokenExpiration(String username, long additionalTime) {
        try {
            String key = "TOKEN:" + username;
            if (redisTemplate.hasKey(key)) {
                Long currentTTL = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
                if (currentTTL != null && currentTTL > 0) {
                    return redisTemplate.expire(key, currentTTL + additionalTime, TimeUnit.MILLISECONDS);
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to extend token expiration in Redis for user: {}. Error: {}", username, e.getMessage());
            return false;
        }
    }

    public void clearAllUserTokens(String username) {
        try {
            redisTemplate.delete("TOKEN:" + username);
            redisTemplate.delete("REFRESH_TOKEN:" + username);
            // Có thể thêm logic để blacklist current token nếu cần
        } catch (Exception e) {
            log.error("Failed to clear tokens from Redis for user: {}. Error: {}", username, e.getMessage());
            // Fail gracefully
        }
    }
}