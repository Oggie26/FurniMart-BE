package com.example.orderservice.controller;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.service.inteface.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/warranties")
@RequiredArgsConstructor
public class InternalWarrantyController {

    private final WarrantyService warrantyService;

    @PostMapping("/generate")
    public ApiResponse<Void> generate(@RequestParam Long orderId) {
        warrantyService.createWarrantiesForOrder(orderId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Warranties generated")
                .build();
    }
}


