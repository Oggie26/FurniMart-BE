package com.example.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        
        // Configure date format for java.util.Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        objectMapper.setDateFormat(dateFormat);
        
        // Disable writing dates as timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Register JavaTimeModule for LocalDate, LocalDateTime, etc.
        objectMapper.registerModule(new JavaTimeModule());
        
        return objectMapper;
    }
}

