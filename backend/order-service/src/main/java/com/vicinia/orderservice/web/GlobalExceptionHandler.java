package com.vicinia.orderservice.web;

import com.vicinia.orderservice.exception.CartHasUnavailableItemsException;
import com.vicinia.orderservice.exception.EmptyCartException;
import com.vicinia.orderservice.exception.ForbiddenException;
import com.vicinia.orderservice.exception.IllegalOrderStatusTransitionException;
import com.vicinia.orderservice.exception.OrderNotFoundException;
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

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(RuntimeException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyCart(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CartHasUnavailableItemsException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailableItems(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalOrderStatusTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalTransition(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
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
