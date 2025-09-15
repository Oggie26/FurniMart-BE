package com.example.fileservice.service;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads");

    public String saveFile(MultipartFile file) {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = root.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            return "http://localhost:8081/files/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path file = root.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }
}
