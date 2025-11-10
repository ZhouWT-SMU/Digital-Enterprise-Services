package com.example.difyfilter.controller;

import com.example.difyfilter.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof MethodArgumentNotValidException validationException) {
            status = HttpStatus.BAD_REQUEST;
            ex = new IllegalArgumentException(validationException.getBindingResult().toString());
        } else if (ex instanceof ResponseStatusException responseStatusException) {
            status = responseStatusException.getStatusCode();
        }

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                UUID.randomUUID().toString(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
