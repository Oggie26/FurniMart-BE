package com.example.remotetests.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class TestConfig {

    @Value("${test.base-url:${TEST_BASE_URL:http://localhost:8080}}")
    private String baseUrl;

    @Value("${test.user-service-url:${TEST_USER_SERVICE_URL:http://localhost:8086}}")
    private String userServiceUrl;

    @Value("${test.product-service-url:${TEST_PRODUCT_SERVICE_URL:http://localhost:8081}}")
    private String productServiceUrl;

    @Value("${test.order-service-url:${TEST_ORDER_SERVICE_URL:http://localhost:8082}}")
    private String orderServiceUrl;

    @Value("${test.delivery-service-url:${TEST_DELIVERY_SERVICE_URL:http://localhost:8083}}")
    private String deliveryServiceUrl;

    @Value("${test.inventory-service-url:${TEST_INVENTORY_SERVICE_URL:http://localhost:8084}}")
    private String inventoryServiceUrl;

    @Value("${test.ai-service-url:${TEST_AI_SERVICE_URL:http://localhost:9000}}")
    private String aiServiceUrl;

    @Value("${test.use-gateway:${TEST_USE_GATEWAY:true}}")
    private boolean useGateway;

    @Bean
    public RestTemplate restTemplate() {
        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(10000);
        ((SimpleClientHttpRequestFactory) factory).setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    public String getBaseUrl() {
        return useGateway ? baseUrl : "";
    }

    public String getUserServiceUrl() {
        return useGateway ? baseUrl : userServiceUrl;
    }

    public String getProductServiceUrl() {
        return useGateway ? baseUrl : productServiceUrl;
    }

    public String getOrderServiceUrl() {
        return useGateway ? baseUrl : orderServiceUrl;
    }

    public String getDeliveryServiceUrl() {
        return useGateway ? baseUrl : deliveryServiceUrl;
    }

    public String getInventoryServiceUrl() {
        return useGateway ? baseUrl : inventoryServiceUrl;
    }

    public String getAiServiceUrl() {
        return useGateway ? baseUrl : aiServiceUrl;
    }
}
