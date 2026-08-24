package com.vicinia.couponservice.web;

import com.vicinia.couponservice.exception.CouponAlreadyUsedByUserException;
import com.vicinia.couponservice.exception.CouponCodeAlreadyExistsException;
import com.vicinia.couponservice.exception.CouponNotActiveException;
import com.vicinia.couponservice.exception.CouponNotFoundException;
import com.vicinia.couponservice.exception.CouponUsageLimitExceededException;
import com.vicinia.couponservice.exception.ForbiddenException;
import com.vicinia.couponservice.exception.MinOrderValueNotMetException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CouponCodeAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CouponUsageLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUsageLimit(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CouponAlreadyUsedByUserException.class)
    public ResponseEntity<Map<String, Object>> handlePerUserLimit(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(CouponNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleNotActive(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MinOrderValueNotMetException.class)
    public ResponseEntity<Map<String, Object>> handleMinOrderValue(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return body(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
