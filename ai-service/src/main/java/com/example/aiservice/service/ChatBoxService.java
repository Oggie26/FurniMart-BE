package com.example.aiservice.service;

import com.example.aiservice.feign.ProductClient;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBoxService {

    private final OpenAiChatModel chatModel;
    private final ProductClient productClient;

    public String chatWithAI(String message) {
        String keyword = extractKeyword(message);
        log.info("💬 User message: {}", message);
        log.info("🔍 Extracted keyword: {}", keyword);

        ApiResponse<List<ProductResponse>> response;
        try {
            response = productClient.getProducts();
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Product Service qua Feign: {}", e.getMessage());
            return "Xin lỗi, tôi chưa thể lấy dữ liệu sản phẩm. Bạn thử lại sau nhé!";
        }

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return "Hiện chưa có sản phẩm nào trong hệ thống 😅.";
        }

        List<ProductResponse> filtered = response.getData().stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .limit(5)
                .toList();

        if (filtered.isEmpty()) {
            return "Tôi không tìm thấy sản phẩm nào có liên quan đến “" + keyword + "” 😅.";
        }

        StringBuilder productSummary = new StringBuilder("Một số sản phẩm bạn có thể quan tâm:\n");
        for (ProductResponse p : filtered) {
            productSummary.append("- ").append(p.getName())
                    .append(" (Giá: ").append(p.getPrice()).append("₫)\n");
        }

        String prompt = """
                Bạn là trợ lý nội thất FurniAI thân thiện.
                Người dùng hỏi: %s
                Dưới đây là danh sách sản phẩm có thể phù hợp:
                %s
                Hãy trả lời ngắn gọn, tự nhiên, tư vấn gợi ý thêm cách phối nội thất hoặc vật liệu phù hợp.
                """.formatted(message, productSummary);

        try {
            ChatResponse aiResponse = chatModel.call(new Prompt(prompt));
            return aiResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi OpenAI API: {}", e.getMessage());
            return "Xin lỗi, AI đang gặp sự cố tạm thời. Bạn thử lại sau nhé!";
        }
    }

    private String extractKeyword(String message) {
        if (message == null || message.isBlank()) return "";
        return message.replaceAll("[^\\p{L}\\p{N}\\s]", "").trim();
    }
}
