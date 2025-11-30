//package com.example.aiservice.service;
//
//import com.example.aiservice.enums.EnumStatus;
//import com.example.aiservice.enums.ErrorCode;
//import com.example.aiservice.exception.AppException;
//import com.example.aiservice.feign.ProductClient;
//import com.example.aiservice.response.ApiResponse;
//import com.example.aiservice.response.BlogResponse;
//import com.example.aiservice.response.ProductCreatedEvent;
//import com.example.aiservice.response.ProductResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class GenerationBlogService {
//
//    private final ProductClient productClient;
//    private final WebClient webClient;
//
//    @Value("${spring.ai.openai.api-key}")
//    private String apiKey;
//
//    @Value("${spring.ai.openai.base-url}")
//    private String baseUrl;
//
//    @Value("${spring.ai.openai.chat.options.model}")
//    private String model;
//
//    public BlogResponse generationBlogByAi(ProductCreatedEvent event) {
//        try {
//            // Tạo prompt "xịn xò"
//            String prompt = String.format(
//                    "Bạn là một chuyên gia viết blog về %s. "
//                            + "Hãy viết một bài blog chi tiết, khoảng 500-700 từ, với cấu trúc rõ ràng gồm tiêu đề, mở bài, nội dung chính, và kết luận. "
//                            + "Phong cách viết thân thiện, dễ hiểu, thu hút người đọc. "
//                            + "Tên sản phẩm: %s. Mô tả: %s. "
//                            + "Hãy đảm bảo bài viết sáng tạo, hấp dẫn và phù hợp với đối tượng độc giả quan tâm tới %s.",
//                    event.getName(),
//                    event.getDescription() != null ? event.getDescription() : ""
//            );
//
//            Mono<String> resultMono = getWebClient()
//                    .post()
//                    .uri("/v1/chat/completions")
//                    .bodyValue(buildRequestBody(prompt))
//                    .retrieve()
//                    .bodyToMono(OpenAiResponse.class)
//                    .map(resp -> resp.getChoices()[0].getMessage().getContent());
//
//            String aiContent = resultMono.block();
//
//            ProductResponse response = getProductResponse(event.getProductId());
//
//            return BlogResponse.builder()
//                    .name(event.getName())
//                    .content(aiContent)
//                    .status(false)
//                    .userId("fd0c791a-b070-4ab6-9084-21845aba4f1d")
//                    .image(response.getThumbnailImage())
//                    .build();
//
//        } catch (WebClientResponseException e) {
//            log.error("OpenAI API error: {}", e.getResponseBodyAsString());
//            throw new RuntimeException("AI generation failed");
//        } catch (Exception e) {
//            log.error("Unexpected error: {}", e.getMessage(), e);
//            throw new RuntimeException("AI generation failed");
//        }
//    }
//
//    private WebClient getWebClient() {
//        return WebClient.builder()
//                .baseUrl(baseUrl)
//                .defaultHeader("Authorization", "Bearer " + apiKey)
//                .build();
//    }
//
//    private ChatCompletionRequest buildRequestBody(String prompt) {
//        return new ChatCompletionRequest(
//                model,
//                new Message[]{new Message("user", prompt)},
//                1,
//                1024
//        );
//    }
//
//    @lombok.Data
//    @lombok.AllArgsConstructor
//    @lombok.NoArgsConstructor
//    static class ChatCompletionRequest {
//        private String model;
//        private Message[] messages;
//        private int n;
//        private int max_tokens;
//    }
//
//    @lombok.Data
//    @lombok.AllArgsConstructor
//    @lombok.NoArgsConstructor
//    static class Message {
//        private String role;
//        private String content;
//    }
//
//    @lombok.Data
//    @lombok.AllArgsConstructor
//    @lombok.NoArgsConstructor
//    static class OpenAiResponse {
//        private Choice[] choices;
//
//        @lombok.Data
//        @lombok.AllArgsConstructor
//        @lombok.NoArgsConstructor
//        static class Choice {
//            private Message message;
//        }
//    }
//
//    private ProductResponse getProductResponse(String id){
//        ApiResponse<ProductResponse> response = productClient.getProductById(id);
//        if (response == null || response.getData() == null) {
//            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
//        }
//        return response.getData();
//    }
//}
