package com.example.aiservice.exception;

import com.example.aiservice.enums.ErrorCode;
import com.example.aiservice.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    /**
     * Xử lý lỗi validate trong @RequestBody (bean validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String enumKey = ex.getFieldError() != null
                ? ex.getFieldError().getDefaultMessage()
                : ErrorCode.INVALID_KEY.name();

        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            // Lấy ErrorCode tương ứng từ enum
            errorCode = ErrorCode.valueOf(enumKey);

            // Lấy attributes từ lỗi đầu tiên (nếu có)
            if (!ex.getBindingResult().getAllErrors().isEmpty()) {
                Object violation = ex.getBindingResult().getAllErrors().get(0)
                        .unwrap(ConstraintViolation.class);
                if (violation instanceof ConstraintViolation<?> constraintViolation) {
                    attributes = constraintViolation.getConstraintDescriptor().getAttributes();
                    log.debug("Validation attributes: {}", attributes);
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Không tìm thấy ErrorCode tương ứng với key: {}", enumKey);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý validation exception", e);
        }

        String message = (attributes != null)
                ? mapAttribute(errorCode.getMessage(), attributes)
                : errorCode.getMessage();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(errorCode.getCode())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Lỗi JSON không hợp lệ
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("Error parsing JSON: {}", ex.getMessage());
        String message = ex.getMessage() != null && ex.getMessage().contains("JSON parse error")
                ? "JSON data invalid. Please check again."
                : "Invalid JSON format.";

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status(ErrorCode.INVALID_JSON.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Xử lý lỗi validation trong @PathVariable, @RequestParam,...
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> errors.put(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ));

        log.warn("Constraint validation failed: {}", errors);

        return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, String>>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Validation failed")
                        .data(errors)
                        .build()
        );
    }

    /**
     * Lỗi truy cập không hợp lệ
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.<Void>builder()
                        .status(ErrorCode.UNAUTHENTICATED.getCode())
                        .message(ErrorCode.UNAUTHENTICATED.getMessage())
                        .build()
        );
    }

    /**
     * Lỗi dữ liệu vi phạm ràng buộc database
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiResponse.<Void>builder()
                        .status(ErrorCode.DATA_INVALID.getCode())
                        .message("Data input invalid")
                        .build()
        );
    }

    /**
     * Lỗi ứng dụng do developer ném ra
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        log.error("Application error: {}", ex.getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatusCode()).body(
                ApiResponse.<Void>builder()
                        .status(ex.getErrorCode().getCode())
                        .message(ex.getMessage())
                        .build()
        );
    }

    /**
     * Lỗi không xác định (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncaught(Exception ex) {
        log.error("Uncaught exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Void>builder()
                        .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage())
                        .build()
        );
    }

    /**
     * Helper method để map message có chứa {min}, {max},...
     */
    private String mapAttribute(String message, Map<String, Object> attributes) {
        if (attributes == null || !attributes.containsKey(MIN_ATTRIBUTE)) {
            return message;
        }
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
