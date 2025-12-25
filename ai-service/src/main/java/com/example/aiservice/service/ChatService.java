package com.example.aiservice.service;

import com.example.aiservice.request.ChatRequest;
import com.example.aiservice.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý AI thân thiện của FurniMart - cửa hàng nội thất hàng đầu.
            Nhiệm vụ: Hỗ trợ khách hàng tư vấn về sản phẩm nội thất, trả lời câu hỏi, và hướng dẫn mua hàng.
            Trả lời bằng Tiếng Việt, thân thiện, chuyên nghiệp và hữu ích.
            Nếu khách hàng cần hỗ trợ từ nhân viên, hãy gợi ý họ sử dụng nút "Gặp nhân viên".
            """;

    public ChatResponse chat(ChatRequest request) {
        try {
            log.info("Processing chat request for chatId: {}", request.getChatId());

            // Build chat client with system prompt
            ChatClient chatClient = chatClientBuilder
                    .defaultSystem(SYSTEM_PROMPT)
                    .build();

            // Build conversation context from message history
            String conversationContext = buildConversationContext(request);

            // Call AI with conversation context and current message
            String aiResponse = chatClient.prompt()
                    .user(conversationContext + "\n\nKhách hàng: " + request.getMessage())
                    .call()
                    .content();

            log.info("AI response generated for chatId: {}", request.getChatId());

            return ChatResponse.builder()
                    .response(aiResponse)
                    .build();

        } catch (Exception e) {
            log.error("Error processing chat request for chatId: {}", request.getChatId(), e);
            // Return a friendly error message
            return ChatResponse.builder()
                    .response("Xin lỗi, tôi đang gặp sự cố kỹ thuật. Vui lòng thử lại sau hoặc liên hệ nhân viên hỗ trợ.")
                    .build();
        }
    }

    private String buildConversationContext(ChatRequest request) {
        if (request.getMessageHistory() == null || request.getMessageHistory().isEmpty()) {
            return "Đây là cuộc trò chuyện mới.";
        }

        // Build context from message history (limit to last 10 messages for context)
        return request.getMessageHistory().stream()
                .limit(10)
                .map(item -> {
                    String roleLabel = "user".equals(item.getRole()) ? "Khách hàng" : "Trợ lý AI";
                    return roleLabel + ": " + item.getContent();
                })
                .collect(Collectors.joining("\n"));
    }
}

