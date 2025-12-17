package com.example.aiservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Recommendation Controller")
@RequiredArgsConstructor
@Slf4j
public class AIRecommendationController {

    /**
     * AI recommend store tốt nhất cho order
     * Tiêu chí: CÓ ĐỦ HÀNG + GẦN NHẤT
     */

}
