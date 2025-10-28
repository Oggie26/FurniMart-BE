package com.example.aiservice.service;

import com.example.aiservice.feign.ProductClient;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.PageResponse;
import com.example.aiservice.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBoxService {

    private final OpenAiChatModel chatModel;
    private final ProductClient productClient;

    public String chatWithAI(String message) {
        // 🔹 B1: Rút gọn và làm sạch input
        String keyword = extractKeyword(message);
        log.info("User message: {}", message);
        log.info("Extracted keyword: {}", keyword);

        // 🔹 B2: Gọi Product Service để tìm sản phẩm
        ApiResponse<PageResponse<ProductResponse>> response;
        try {
            response = productClient.searchProducts(keyword, 0, 5);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Product Service qua Feign: {}", e.getMessage());
            return "Xin lỗi, hiện tôi không thể lấy dữ liệu sản phẩm. Bạn thử lại sau nhé!";
        }

        if (response == null || response.getData() == null || response.getData().getContent().isEmpty()) {
            return "Hiện chưa tìm thấy sản phẩm nào phù hợp với yêu cầu của bạn 😅.";
        }

        List<ProductResponse> products = response.getData().getContent();

        // 🔹 B3: Ghép danh sách sản phẩm thành văn bản
        StringBuilder productSummary = new StringBuilder("Một số sản phẩm bạn có thể quan tâm:\n");
        for (ProductResponse p : products) {
            productSummary.append("- ")
                    .append(p.getName())
                    .append(" (Giá: ")
                    .append(p.getPrice())
                    .append("₫)\n");
        }

        // 🔹 B4: Tạo prompt cho AI
        String prompt = """
                Bạn là trợ lý tư vấn nội thất thông minh FurniAI.
                Người dùng hỏi: %s
                Dưới đây là danh sách sản phẩm có thể phù hợp:
                %s
                Hãy phản hồi bằng giọng thân thiện, ngắn gọn và gợi ý thêm cách phối hợp nội thất hoặc vật liệu phù hợp.
                """.formatted(message, productSummary);

        // 🔹 B5: Gọi AI model (Spring AI)
        try {
            ChatResponse aiResponse = chatModel.call(new Prompt(prompt));
            aiResponse.getResult();
            aiResponse.getResult().getOutput();
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
