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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBoxService {

    private final OpenAiChatModel chatModel;
    private final ProductClient productClient;

    public String chatWithAI(String message) {
        log.info("💬 Tin nhắn từ người dùng: {}", message);

        // Gọi Product Service
        ApiResponse<List<ProductResponse>> response;
        try {
            response = productClient.getProducts();
            int count = (response != null && response.getData() != null) ? response.getData().size() : 0;
            log.info("✅ Gọi Product Service thành công, nhận được {} sản phẩm", count);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Product Service qua Feign: {}", e.getMessage());
            return "Xin lỗi, tôi chưa thể lấy dữ liệu sản phẩm từ hệ thống. Bạn thử lại sau nhé!";
        }

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return "Hiện chưa có sản phẩm nào trong hệ thống 😅.";
        }

        // Làm sạch keyword
        String keyword = extractKeyword(message);
        log.info("🔍 Keyword sau khi xử lý: {}", keyword);

        // Nếu câu hỏi chung chung (ví dụ: “có bao nhiêu sản phẩm”) → không lọc
        List<ProductResponse> matchedProducts = (keyword.isEmpty() || isGeneralQuestion(message))
                ? response.getData()
                : response.getData().stream()
                .filter(p -> p.getName() != null &&
                        p.getName().toLowerCase().contains(keyword.toLowerCase()))
                .limit(10)
                .toList();

        if (matchedProducts.isEmpty()) {
            return "Tôi không tìm thấy sản phẩm nào liên quan đến “" + keyword + "” 😅.";
        }

        // Tóm tắt danh sách sản phẩm gửi cho AI
        StringBuilder productSummary = new StringBuilder("Danh sách sản phẩm hiện có:\n");
        matchedProducts.forEach(p -> {
            productSummary.append("- ").append(p.getName());
            if (p.getColor() != null) productSummary.append(" (Màu: ").append(p.getColor()).append(")");
            if (p.getPrice() != null) productSummary.append(", Giá: ").append(p.getPrice()).append("₫");
            productSummary.append("\n");
        });

        // Prompt cải tiến
        String prompt = """
        Bạn là **FurniAI**, trợ lý AI chuyên nghiệp của cửa hàng nội thất FurniMart.
        Nhiệm vụ của bạn là hỗ trợ khách hàng về sản phẩm, giá cả, và gợi ý lựa chọn phù hợp.

        ## Câu hỏi của khách:
        "%s"

        ## Dữ liệu sản phẩm hiện có (JSON tóm tắt):
        %s

        ## Hướng dẫn trả lời:
        1. Nếu câu hỏi về số lượng, liệt kê, hay tìm sản phẩm → hãy dùng dữ liệu trên để trả lời chi tiết.
        2. Nếu hỏi tư vấn phong cách nội thất → phân tích nhu cầu và gợi ý sản phẩm phù hợp.
        3. Nếu không liên quan đến sản phẩm → trả lời thân thiện, gợi ý khách hỏi lại về sản phẩm.

        Yêu cầu:
        - Trả lời bằng tiếng Việt tự nhiên, dễ hiểu, giọng thân thiện như nhân viên tư vấn thật.
        - Không lặp lại câu hỏi của khách.
        - Nếu có thể, hãy gợi ý thêm 1–2 sản phẩm liên quan để tăng tương tác.
        """.formatted(message, productSummary);

        // Gọi OpenAI
        try {
            ChatResponse aiResponse = chatModel.call(new Prompt(prompt));
            String reply = aiResponse.getResult().getOutput().getText();
            log.info("🤖 AI Response: {}", reply);
            return reply != null && !reply.isBlank()
                    ? reply.trim()
                    : "Xin lỗi, tôi chưa hiểu rõ câu hỏi của bạn. Bạn có thể nói cụ thể hơn không?";
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi OpenAI API: {}", e.getMessage());
            return "Xin lỗi, hệ thống AI đang tạm gián đoạn. Vui lòng thử lại sau nhé!";
        }
    }

    private String extractKeyword(String message) {
        if (message == null || message.isBlank()) return "";
        return message.replaceAll("[^\\p{L}\\p{N}\\s]", "").trim();
    }

    private boolean isGeneralQuestion(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("bao nhiêu")
                || lower.contains("tổng số")
                || lower.contains("liệt kê")
                || lower.contains("sản phẩm nào")
                || lower.contains("có những gì");
    }
}
