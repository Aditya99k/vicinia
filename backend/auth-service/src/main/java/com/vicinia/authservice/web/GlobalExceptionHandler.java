package com.vicinia.authservice.web;

import com.vicinia.authservice.exception.EmailAlreadyExistsException;
import com.vicinia.authservice.exception.InvalidCredentialsException;
import com.vicinia.authservice.exception.InvalidRoleException;
import com.vicinia.authservice.exception.InvalidTokenException;
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

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<Map<String, Object>> handleUnauthorized(RuntimeException e) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
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
