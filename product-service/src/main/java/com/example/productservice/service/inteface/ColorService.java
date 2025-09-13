package com.example.productservice.service.inteface;

import com.example.productservice.request.ColorRequest;
import com.example.productservice.response.ColorResponse;
import com.example.productservice.response.PageResponse;

import java.util.List;

public interface ColorService {
    ColorResponse addColor(ColorRequest colorRequest);
    ColorResponse updateColor(ColorRequest colorRequest, String colorId);
    ColorResponse getColorById(String colorId);
    List<ColorResponse> getAllColors();
    void deleteColor(String colorId);
    PageResponse<ColorResponse> searchColors(String request, int page, int size);

}
