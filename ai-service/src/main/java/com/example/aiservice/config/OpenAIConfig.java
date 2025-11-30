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

    // ==============================================================================
    // 🔴 KHU VỰC CẤU HÌNH API KEY (BẠN SẼ ĐIỀN VÀO ĐÂY SAU)
    // ==============================================================================
    // API Key được lấy từ biến môi trường OPENAI_API_KEY hoặc GOOGLE_API_KEY
    // Cấu hình trong: application.yml hoặc Docker environment variable
    // 
    // Spring AI sẽ tự động tạo OpenAiChatModel bean nếu có API key hợp lệ
    // Nếu không có key, bean sẽ không được tạo và service sẽ chạy ở mock mode
    // ==============================================================================

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
