package com.example.aiservice.response;

import java.util.List;

public record InteriorDesignResponse(
                String style,
                String analysis,
                List<String> colorPalette,
                List<Suggestion> suggestions) {
        public record Suggestion(
                        String id,
                        String itemName,
                        String reason,
                        String placementAdvice, // Lời khuyên đặt ở đâu
                        String thumbnailImage,
                        Double price,
                        String recommendedColor) {
        }
}