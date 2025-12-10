package com.example.aiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    // ==============================================================================
    // ✅ CẤU HÌNH GEMINI API KEY
    // ==============================================================================
    // API Key được lấy từ biến môi trường GOOGLE_API_KEY hoặc từ application.yml
    // Cấu hình trong: application.yml hoặc Docker environment variable
    // 
    // Spring AI sẽ tự động tạo GeminiChatModel bean nếu có API key hợp lệ
    // Nếu không có key, bean sẽ không được tạo và service sẽ chạy ở mock mode
    // ==============================================================================

    @Value("${spring.ai.google.gemini.api-key:AIzaSyCZbTJLhyCYBD5BVAsheK67FOSS7yTPiFs}")
    private String geminiApiKey;

    // Spring AI tự động tạo GeminiChatModel bean dựa trên cấu hình trong application.yml
    // Không cần tạo bean thủ công như trước
}
