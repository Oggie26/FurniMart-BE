package com.example.orderservice.controller;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.service.inteface.WarrantyService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/warranties")
@RequiredArgsConstructor
public class InternalWarrantyController {

    private final ApplicationContext applicationContext;

    @PostMapping("/generate")
    public ApiResponse<Void> generate(@RequestParam Long orderId) {
        WarrantyService warrantyService = applicationContext.getBean(WarrantyService.class);
        warrantyService.createWarrantiesForOrder(orderId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Warranties generated")
                .build();
    }
}


