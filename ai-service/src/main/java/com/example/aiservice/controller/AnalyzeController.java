package com.example.aiservice.controller;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.InteriorDesignResponse;
import com.example.aiservice.service.AnalyzeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/ai/analyze")
@Slf4j
@RestController
@RequiredArgsConstructor
public class AnalyzeController {

    private final AnalyzeService analyzeService;
    @PostMapping(value = "/analyze-room", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InteriorDesignResponse>> analyzeRoom(
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(
                ApiResponse.<InteriorDesignResponse>builder()
                        .status(200)
                        .message("Room analyzed successfully")
                        .data(analyzeService.analyzeRoom(image))
                        .build());
    }

}
