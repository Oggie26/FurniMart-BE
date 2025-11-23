package com.example.deliveryservice.exception;

import com.example.deliveryservice.enums.ErrorCode;
import com.example.deliveryservice.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError() != null
                ? exception.getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_KEY.name();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);

            if (!exception.getBindingResult().getAllErrors().isEmpty()) {
                var constraintViolation = exception.getBindingResult()
                        .getAllErrors()
                        .get(0)
                        .unwrap(jakarta.validation.ConstraintViolation.class);

                attributes = constraintViolation.getConstraintDescriptor().getAttributes();
                log.info("Validation attributes: {}", attributes);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Không tìm thấy ErrorCode tương ứng với key: {}", enumKey);
        } catch (Exception ex) {
            log.error("Lỗi khi phân tích validation exception", ex);
        }

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setStatus(errorCode.getCode());  // Lấy mã lỗi từ ErrorCode
        apiResponse.setMessage(
                (attributes != null)
                        ? mapAttribute(errorCode.getMessage(), attributes)  // Xử lý message nếu có attributes
                        : errorCode.getMessage()  // Dùng thông điệp mặc định từ ErrorCode
        );

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        // Tạo đối tượng lỗi mặc định
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setStatus(ErrorCode.INVALID_JSON.getCode());

        // Cung cấp thông điệp lỗi chi tiết
        String errorMessage = "JSON data invalid";

        // Log lỗi để kiểm tra chi tiết
        log.error("Error processing JSON: " + exception.getMessage());

        // Nếu lỗi JSON liên quan đến cấu trúc không hợp lệ hoặc định dạng, thông báo thêm
        if (exception.getMessage().contains("JSON parse error")) {
            errorMessage = "JSON data invalid. Please check again";
        }

        // Trả về thông điệp lỗi
        apiResponse.setMessage(errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        log.warn("Constraint validation failed: {}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("Access Denied: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message(ErrorCode.UNAUTHENTICATED.getMessage())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        log.warn("Data violation: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Data input invalid")
                        .build());
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        log.error("Application error: {} - ErrorCode: {}", exception.getMessage(), exception.getErrorCode(), exception);
        return ResponseEntity.status(exception.getErrorCode().getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .status(exception.getErrorCode().getCode())
                        .message(exception.getMessage())
                        .build());

    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(feign.FeignException exception) {
        log.error("Feign exception: status={}, message={}", exception.status(), exception.getMessage());
        
        // Map Feign 404 to appropriate error
        if (exception.status() == 404) {
            // Try to determine which resource was not found from the request
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("order") || message.contains("Order")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.<Void>builder()
                                    .status(ErrorCode.ORDER_NOT_FOUND.getCode())
                                    .message(ErrorCode.ORDER_NOT_FOUND.getMessage())
                                    .build());
                } else if (message.contains("store") || message.contains("Store")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.<Void>builder()
                                    .status(ErrorCode.STORE_NOT_FOUND.getCode())
                                    .message(ErrorCode.STORE_NOT_FOUND.getMessage())
                                    .build());
                } else if (message.contains("assignment") || message.contains("Assignment")) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.<Void>builder()
                                    .status(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND.getCode())
                                    .message(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND.getMessage())
                                    .build());
                }
            }
            // Generic 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .status(ErrorCode.DELIVERY_ASSIGNMENT_NOT_FOUND.getCode())
                            .message("Resource not found")
                            .build());
        }
        
        // Other Feign errors (4xx, 5xx)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message("External service error: " + exception.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncaughtException(Exception exception) {
        log.error("=== UNCAUGHT EXCEPTION ===", exception);
        log.error("Exception type: {}", exception.getClass().getName());
        log.error("Exception message: {}", exception.getMessage());
        log.error("Exception cause: {}", exception.getCause() != null ? exception.getCause().getClass().getName() : "null");
        if (exception.getCause() != null) {
            log.error("Cause message: {}", exception.getCause().getMessage());
        }
        log.error("Stack trace:", exception);
        log.error("========================");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                        .build());
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }

}


