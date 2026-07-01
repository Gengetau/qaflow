package com.gengetau.qaflow.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validationError(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<ApiError.FieldViolation> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
            .toList();

    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                "VALIDATION_FAILED",
                "Request validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                java.time.OffsetDateTime.now(),
                violations));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> responseStatus(ResponseStatusException exception, HttpServletRequest request) {
    HttpStatusCode statusCode = exception.getStatusCode();
    HttpStatus status = HttpStatus.resolve(statusCode.value());
    String code = status == null ? "REQUEST_FAILED" : status.name();
    String message = exception.getReason() == null ? "Request failed" : exception.getReason();
    return ResponseEntity.status(statusCode)
        .body(ApiError.of(code, message, statusCode.value(), request.getRequestURI()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> notFound(NoResourceFoundException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiError.of(
                "NOT_FOUND",
                "The requested resource was not found",
                HttpStatus.NOT_FOUND.value(),
                request.getRequestURI()));
  }
}