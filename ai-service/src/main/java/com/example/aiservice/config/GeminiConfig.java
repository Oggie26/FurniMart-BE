package com.example.aiservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${spring.ai.google.gemini.api-key:AIzaSyCZbTJLhyCYBD5BVAsheK67FOSS7yTPiFs}")
    private String geminiApiKey;

}
