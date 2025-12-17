package com.example.remotetests.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isEmpty()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    public static HttpHeaders createHeaders() {
        return createHeaders(null);
    }

    public static <T> ResponseEntity<T> postRequest(RestTemplate restTemplate, String url, Object body, 
                                                   Class<T> responseType, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Object> entity = new HttpEntity<>(body, Objects.requireNonNull(headers));
        return restTemplate.exchange(Objects.requireNonNull(url), Objects.requireNonNull(HttpMethod.POST), entity, Objects.requireNonNull(responseType));
    }

    public static <T> ResponseEntity<T> getRequest(RestTemplate restTemplate, String url, 
                                                   Class<T> responseType, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(Objects.requireNonNull(headers));
        return restTemplate.exchange(Objects.requireNonNull(url), Objects.requireNonNull(HttpMethod.GET), entity, Objects.requireNonNull(responseType));
    }

    public static <T> ResponseEntity<T> putRequest(RestTemplate restTemplate, String url, Object body, 
                                                   Class<T> responseType, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Object> entity = new HttpEntity<>(body, Objects.requireNonNull(headers));
        return restTemplate.exchange(Objects.requireNonNull(url), Objects.requireNonNull(HttpMethod.PUT), entity, Objects.requireNonNull(responseType));
    }

    public static <T> ResponseEntity<T> patchRequest(RestTemplate restTemplate, String url, Object body, 
                                                     Class<T> responseType, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Object> entity = new HttpEntity<>(body, Objects.requireNonNull(headers));
        return restTemplate.exchange(Objects.requireNonNull(url), Objects.requireNonNull(HttpMethod.PATCH), entity, Objects.requireNonNull(responseType));
    }

    public static <T> ResponseEntity<T> deleteRequest(RestTemplate restTemplate, String url, 
                                                       Class<T> responseType, String token) {
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(Objects.requireNonNull(headers));
        return restTemplate.exchange(Objects.requireNonNull(url), Objects.requireNonNull(HttpMethod.DELETE), entity, Objects.requireNonNull(responseType));
    }

    public static Map<String, Object> createLoginRequest(String email, String password) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);
        return request;
    }

    public static Map<String, Object> createRegisterRequest(String email, String password, 
                                                              String fullName, String phone) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", password);
        request.put("fullName", fullName);
        request.put("phone", phone);  // Changed from phoneNumber to phone
        request.put("gender", true);
        request.put("birthDay", "1990-01-01");
        return request;
    }

    public static String extractTokenFromResponse(Object response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.convertValue(response, Map.class);
            if (responseMap.containsKey("data")) {
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    // Try both token and accessToken
                    if (dataMap.containsKey("token")) {
                        return (String) dataMap.get("token");
                    }
                    if (dataMap.containsKey("accessToken")) {
                        return (String) dataMap.get("accessToken");
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Get token from environment variable or extract from login response
     */
    public static String getTokenFromEnvOrResponse(String envVarName, Object loginResponse) {
        String token = System.getenv(envVarName);
        if (token != null && !token.isEmpty()) {
            return token;
        }
        return extractTokenFromResponse(loginResponse);
    }
}

