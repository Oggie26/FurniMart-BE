package com.example.userservice.exception;

import com.example.userservice.enums.ErrorCode;
import com.example.userservice.response.ApiResponse;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handlingValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();
        
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage != null ? errorMessage : "Validation failed");
        });
        
        // Log all validation errors
        log.warn("Validation errors: {}", errors);
        
        // Get first error message for main message
        String mainMessage = errors.isEmpty() 
            ? "Validation failed" 
            : errors.values().iterator().next();
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message(mainMessage)
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        // Cung cấp thông điệp lỗi chi tiết
        String errorMessage = "JSON data invalid";

        // Log lỗi để kiểm tra chi tiết
        log.error("Error processing JSON: " + exception.getMessage());

        // Nếu lỗi JSON liên quan đến cấu trúc không hợp lệ hoặc định dạng, thông báo thêm
        if (exception.getMessage().contains("JSON parse error")) {
            errorMessage = "JSON data invalid. Please check again";
        }

        // Tạo đối tượng lỗi mặc định
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .status(ErrorCode.INVALID_JSON.getCode())
                .message(errorMessage)
                .build();

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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException exception) {
        log.warn("Bad Credentials: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message("Email hoặc mật khẩu không đúng")
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception) {
        log.warn("Authentication failed: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message("Xác thực thất bại")
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        log.warn("Data violation: {}", exception.getMessage());
        
        String errorMessage = "Data input invalid";
        String rootCause = "";
        
        try {
            Throwable rootCauseException = exception.getRootCause();
            if (rootCauseException != null && rootCauseException.getMessage() != null) {
                rootCause = rootCauseException.getMessage();
            }
        } catch (Exception e) {
            log.warn("Could not extract root cause: {}", e.getMessage());
        }
        
        // Provide more specific error messages based on constraint violations
        if (!rootCause.isEmpty()) {
            if (rootCause.contains("unique") || rootCause.contains("duplicate")) {
                if (rootCause.contains("email") || rootCause.contains("accounts_email_key")) {
                    errorMessage = "Email đã tồn tại";
                } else if (rootCause.contains("phone") || rootCause.contains("employees_phone_key")) {
                    errorMessage = "Số điện thoại đã tồn tại";
                } else if (rootCause.contains("cccd") || rootCause.contains("employees_cccd_key")) {
                    errorMessage = "CCCD đã tồn tại";
                } else {
                    errorMessage = "Dữ liệu đã tồn tại: " + rootCause;
                }
            } else if (rootCause.contains("not null") || rootCause.contains("null value")) {
                errorMessage = "Thiếu dữ liệu bắt buộc";
            } else {
                errorMessage = "Lỗi ràng buộc dữ liệu: " + rootCause;
            }
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message(errorMessage)
                        .build());
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        log.error("Application error: {} - Error Code: {} - Status: {}", 
                exception.getMessage(), 
                exception.getErrorCode().getCode(), 
                exception.getErrorCode().getStatusCode());
        
        // Log stack trace for debugging
        log.error("Stack trace for error code {}: ", exception.getErrorCode().getCode(), exception);
        
        return ResponseEntity.status(exception.getErrorCode().getStatusCode())
                .body(ApiResponse.<Void>builder()
                        .status(exception.getErrorCode().getCode())
                        .message(exception.getMessage())
                        .build());

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncaughtException(Exception exception) {
        log.error("Uncaught exception of type {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
        log.error("Full stack trace: ", exception);
        
        // Get root cause message for detailed error
        String detailedMessage = exception.getMessage();
        Throwable rootCause = exception.getCause();
        
        // Traverse to find the actual root cause
        while (rootCause != null && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        
        // Build detailed error message
        String errorMessage = ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage();
        
        // Add exception details to help debugging
        if (detailedMessage != null && !detailedMessage.isEmpty()) {
            errorMessage = ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage() + " | Error: " + detailedMessage;
        }
        
        // Add root cause if available
        if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().isEmpty()) {
            errorMessage += " | Root Cause: " + rootCause.getMessage();
        }
        
        // Provide more specific error message based on exception type
        if (exception instanceof DataIntegrityViolationException) {
            errorMessage = "Database constraint violation: " + (detailedMessage != null ? detailedMessage : "Please check your data.");
        } else if (exception instanceof IllegalArgumentException) {
            errorMessage = "Invalid argument: " + (detailedMessage != null ? detailedMessage : "Please check your input.");
        } else if (exception.getCause() != null) {
            // Include cause message for better debugging
            String causeMsg = exception.getCause().getMessage();
            if (causeMsg != null && !causeMsg.isEmpty()) {
                errorMessage = ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage() + " | Cause: " + causeMsg;
            }
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .status(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode())
                        .message(errorMessage)
                        .build());
    }

}


