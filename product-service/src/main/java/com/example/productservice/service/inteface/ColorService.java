package com.example.productservice.service.inteface;

import com.example.productservice.request.ColorRequest;
import com.example.productservice.response.ColorResponse;

import java.util.List;

public interface ColorService {
    ColorResponse addColor(ColorRequest colorRequest);
    ColorResponse updateColor(ColorRequest colorRequest,  String id);
    void deleteColor(String id);
    ColorResponse getColor(String id);
    List<ColorResponse> getAllColors();

}
