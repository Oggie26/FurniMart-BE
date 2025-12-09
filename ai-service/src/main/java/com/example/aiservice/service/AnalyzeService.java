package com.example.aiservice.service;

import com.example.aiservice.response.InteriorDesignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalyzeService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a senior interior architect with 20 years of experience.
            Your goal is to provide practical, aesthetic, and budget-friendly advice.
            Always focus on lighting, color harmony, and space optimization.
            """;

    public AnalyzeService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    public InteriorDesignResponse analyzeRoom(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File image cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Please upload an image.");
        }

        Resource resource = file.getResource();
        var mimeType = MimeTypeUtils.parseMimeType(contentType);

        String userPromptText = """
                Analyze this room image deeply based on:
                1. Architectural Style (e.g., Minimalist, Industrial).
                2. Color Analysis (Suggest a color palette).
                3. Suggest 3 specific furniture items to add/replace to elevate the look.
                4. For each item, suggest the best placement in the room.
                """;

        // 3. Gọi AI và map thẳng vào DTO
        return chatClient.prompt()
                .user(u -> u.text(userPromptText)
                        .media(mimeType, resource))
                .call()
                // Phép thuật ở đây: Tự động ép kiểu JSON từ AI thành Java Object
                .entity(InteriorDesignResponse.class);
    }
}
