package com.example.aiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAIConfig {

    @Value("${spring.ai.openai.api-key:dummy_key}")
    private String openaiApiKey;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", havingValue = "dummy_key", matchIfMissing = true)
    public WebClient openAiWebClient() {
        // Chỉ tạo WebClient nếu có API key thật (không phải dummy_key)
        if (isValidApiKey(openaiApiKey)) {
            return WebClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .exchangeStrategies(ExchangeStrategies.builder()
                            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                            .build())
                    .build();
        }
        // Trả về null nếu không có key hợp lệ - Spring sẽ xử lý
        return null;
    }

    private boolean isValidApiKey(String key) {
        return key != null 
            && !key.isBlank() 
            && !key.equals("dummy_key")
            && !key.startsWith("${"); // Không phải placeholder
    }
}
