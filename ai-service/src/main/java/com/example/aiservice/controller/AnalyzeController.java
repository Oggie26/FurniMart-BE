package com.example.aiservice.controller;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.InteriorDesignResponse;
import com.example.aiservice.service.AiInteriorDesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/ai/analyze")
@RequiredArgsConstructor
@Slf4j
public class AnalyzeController {

    private final AiInteriorDesignService aiInteriorDesignService;

    @PostMapping(value = "/analyze-room", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InteriorDesignResponse>> analyzeRoom(
            @RequestParam("image") MultipartFile image) {

        InteriorDesignResponse result = aiInteriorDesignService.analyzeRoom(image);

        return ResponseEntity.ok(
                ApiResponse.<InteriorDesignResponse>builder()
                        .status(200)
                        .message("Room analyzed successfully")
                        .data(result)
                        .build()
        );
    }

}
