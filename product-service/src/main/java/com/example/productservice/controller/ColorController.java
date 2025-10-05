package com.example.productservice.controller;

import com.example.productservice.request.ColorRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.ColorResponse;
import com.example.productservice.service.inteface.ColorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
@Tag(name = "Color Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @PostMapping
    @Operation(summary = "Thêm màu mới")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ColorResponse> addColor(@Valid @RequestBody ColorRequest colorRequest) {
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Thêm màu thành công")
                .data(colorService.addColor(colorRequest))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin màu")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ColorResponse> updateColor(
            @PathVariable String id,
            @Valid @RequestBody ColorRequest colorRequest) {
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật màu thành công")
                .data(colorService.updateColor(colorRequest, id))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm màu (isDeleted = true)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteColor(@PathVariable String id) {
        colorService.deleteColor(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa màu thành công")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết màu theo ID")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ColorResponse> getColor(@PathVariable String id) {
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy màu thành công")
                .data(colorService.getColor(id))
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả màu (chưa bị xóa)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ColorResponse>> getAllColors() {
        return ApiResponse.<List<ColorResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách màu thành công")
                .data(colorService.getAllColors())
                .build();
    }
}
