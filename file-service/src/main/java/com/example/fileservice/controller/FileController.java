package com.example.fileservice.controller;

import com.example.fileservice.response.ApiResponse;
import com.example.fileservice.service.CloudinaryService;
import com.example.fileservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class    FileController {

    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = cloudinaryService.uploadFile(file);
        return ApiResponse.<String>builder()
                .status(201)
                .message("File uploaded successfully")
                .data(fileUrl)
                .build();
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Get file", description = "Serve uploaded file by filename")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource resource = fileStorageService.loadFileAsResource(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
