package com.carddemo.batch.online;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class OnlineExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handle(BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("errorMessage", exception.getMessage()));
    }
}
