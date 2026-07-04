package com.inkpulse.controllers;

import com.inkpulse.corehelpers.exceptions.AccountLockedException;
import com.inkpulse.corehelpers.exceptions.BaseException;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.TooManyRequestException;
import com.inkpulse.models.response.ResultRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ResultRes<Object>> handleBusinessValidation(BusinessValidationException ex) {
        log.warn("Business validation error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ResultRes.errorResult(ex.getMessage(), ex.getStatusCode(), List.of(ex.getErrorCode())));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ResultRes<Object>> handleAccountLocked(AccountLockedException ex) {
        log.warn("Account locked [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ResultRes.errorResult(ex.getMessage(), ex.getStatusCode(), List.of(ex.getErrorCode())));
    }

    @ExceptionHandler(TooManyRequestException.class)
    public ResponseEntity<ResultRes<Object>> handleTooManyRequests(TooManyRequestException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ResultRes.errorResult(ex.getMessage(), ex.getStatusCode(), List.of(ex.getErrorCode())));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ResultRes<Object>> handleBaseException(BaseException ex) {
        log.warn("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ResultRes.errorResult(ex.getMessage(), ex.getStatusCode(), List.of(ex.getErrorCode())));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ResultRes<Object>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(java.util.stream.Collectors.joining(", "));
        log.warn("Validation error: {}", errorMsg);
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(ResultRes.errorResult(errorMsg, 400, List.of("VALIDATION_ERROR")));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ResultRes<Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied error: {}", ex.getMessage());
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(ResultRes.errorResult("Bạn không có quyền thực hiện hành động này.", 403, List.of("ACCESS_DENIED")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultRes<Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .internalServerError()
                .body(ResultRes.errorResult("Internal server error", 500, List.of("INTERNAL_ERROR")));
    }
}
