package com.teasub.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    // Belt-and-suspenders: if a unique-index race slips past the service-layer pre-check,
    // MongoDB itself refuses the duplicate write and we map it to the same business error.
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKey(DuplicateKeyException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        if (message.contains("jti")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "QR_ALREADY_USED", "message", "This QR code has already been used."));
        }
        if (message.contains("razorpayPaymentId")) {
            return ResponseEntity.ok(Map.of("code", "PAYMENT_ALREADY_PROCESSED", "message", "This payment has already been processed."));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "ALREADY_REDEEMED_TODAY", "message", "You've already redeemed your tea for today."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("code", "INTERNAL_ERROR", "message", "Something went wrong. Please try again."));
    }
}
