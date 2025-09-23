package com.example.productservice.controller;

import com.example.productservice.request.MaterialRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.MaterialResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.service.inteface.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@Tag(name = "Material Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping
    @Operation(summary = "Tạo chất liệu mới")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MaterialResponse> createMaterial(@Valid @RequestBody MaterialRequest request) {
        return ApiResponse.<MaterialResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo chất liệu thành công")
                .data(materialService.createMaterial(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật chất liệu")
    public ApiResponse<MaterialResponse> updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody MaterialRequest request) {
        return ApiResponse.<MaterialResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật chất liệu thành công")
                .data(materialService.updateMaterial(request, id))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm chất liệu (isDeleted)")
    public ApiResponse<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá chất liệu thành công")
                .build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Vô hiệu hoá chất liệu (set INACTIVE)")
    public ApiResponse<Void> disableMaterial(@PathVariable Long id) {
        materialService.disableMaterial(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Đã vô hiệu hoá chất liệu")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả chất liệu")
    public ApiResponse<List<MaterialResponse>> getAllMaterials() {
        return ApiResponse.<List<MaterialResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách chất liệu thành công")
                .data(materialService.getAllMaterials())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết chất liệu theo ID")
    public ApiResponse<MaterialResponse> getMaterialById(@PathVariable Long id) {
        return ApiResponse.<MaterialResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy chất liệu thành công")
                .data(materialService.getMaterialById(id))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm chất liệu theo nhiều tiêu chí")
    public ApiResponse<PageResponse<MaterialResponse>> searchMaterials(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<MaterialResponse> materials = materialService.searchMaterial(keyword, page, size);

        return ApiResponse.<PageResponse<MaterialResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm thành công")
                .data(materials)
                .build();
    }
}
