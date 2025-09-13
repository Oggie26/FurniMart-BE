package com.example.productservice.controller;

import com.example.productservice.request.ColorRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.ColorResponse;
import com.example.productservice.response.PageResponse;
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
    @Operation(summary = "Tạo màu mới")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ColorResponse> createColor(@Valid @RequestBody ColorRequest request) {
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo màu thành công")
                .data(colorService.addColor(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật màu")
    public ApiResponse<ColorResponse> updateColor(@PathVariable String id,
                                                  @Valid @RequestBody ColorRequest request) {
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật màu thành công")
                .data(colorService.updateColor(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm màu (isDeleted)")
    public ApiResponse<Void> deleteColor(@PathVariable String id) {
        colorService.deleteColor(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá màu thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả màu")
    public ApiResponse<List<ColorResponse>> getColors() {
        return ApiResponse.<List<ColorResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách màu thành công")
                .data(colorService.getAllColors())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết màu theo ID")
    public ApiResponse<ColorResponse> getColorById(@PathVariable String id) {
        return ApiResponse.<ColorResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy màu thành công")
                .data(colorService.getColorById(id))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm màu theo nhiều tiêu chí")
    public ApiResponse<PageResponse<ColorResponse>> searchColors(
            @RequestParam(required = false) String request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<ColorResponse> colors = colorService.searchColors(request, page, size);

        return ApiResponse.<PageResponse<ColorResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm màu thành công")
                .data(colors)
                .build();
    }
}
