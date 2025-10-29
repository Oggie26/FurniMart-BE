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
                    .append(" (Giá: ").append(p.getPrice()).append("₫)\n")
                    .append(" (Màu: ").append(p.getColor()).append("\n")
            ;
        }

        String prompt = """
            Bạn là trợ lý nội thất thông minh FurniAI.
            Hãy đọc câu hỏi của người dùng và tư vấn thật chuyên nghiệp.
            
            Thông tin người dùng hỏi:
            "%s"
            
            Danh sách sản phẩm hiện có trong cửa hàng:
            %s
            
            Nhiệm vụ của bạn:
            - Nếu người dùng hỏi về tư vấn nội thất, hãy phân tích nhu cầu (phòng, diện tích, phong cách, màu sắc, ngân sách).
            - Đề xuất 2–3 sản phẩm phù hợp trong danh sách kèm lý do chọn.
            - Gợi ý thêm vật liệu hoặc màu sắc để phối hợp đẹp mắt.
            - Nếu câu hỏi không liên quan sản phẩm, hãy trả lời thân thiện, ngắn gọn, và gợi ý cách hỏi khác.
            
            Hãy trả lời bằng tiếng Việt tự nhiên, giọng thân thiện như đang tư vấn khách hàng.
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
