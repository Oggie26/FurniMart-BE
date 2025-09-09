package com.example.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveToken(String username, String token, long jwtExpiration) {
        String key = "TOKEN:" + username;
        redisTemplate.opsForValue().set(key, token, jwtExpiration, TimeUnit.MILLISECONDS);
    }

    public void saveRefreshToken(String username, String refreshToken, long refreshExpiration) {
        String key = "REFRESH_TOKEN:" + username;
        redisTemplate.opsForValue().set(key, refreshToken, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    public String getToken(String username) {
        String key = "TOKEN:" + username;
        return (String) redisTemplate.opsForValue().get(key);
    }

    public String getRefreshToken(String username) {
        String key = "REFRESH_TOKEN:" + username;
        return (String) redisTemplate.opsForValue().get(key);
    }

    public void deleteToken(String username) {
        redisTemplate.delete("TOKEN:" + username);
        redisTemplate.delete("REFRESH_TOKEN:" + username);
    }

    public void blacklistToken(String token, long remainingTime) {
        // Chỉ blacklist nếu token còn thời gian
        if (remainingTime > 0) {
            redisTemplate.opsForValue().set(
                    "JWT_BLACKLIST:" + token,
                    "true",
                    remainingTime,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("JWT_BLACKLIST:" + token));
    }

    public Long getTokenTTL(String username) {
        String key = "TOKEN:" + username;
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }

    public boolean extendTokenExpiration(String username, long additionalTime) {
        String key = "TOKEN:" + username;
        if (redisTemplate.hasKey(key)) {
            Long currentTTL = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            if (currentTTL != null && currentTTL > 0) {
                return redisTemplate.expire(key, currentTTL + additionalTime, TimeUnit.MILLISECONDS);
            }
        }
        return false;
    }

    public void clearAllUserTokens(String username) {
        redisTemplate.delete("TOKEN:" + username);
        redisTemplate.delete("REFRESH_TOKEN:" + username);
        // Có thể thêm logic để blacklist current token nếu cần
    }
}