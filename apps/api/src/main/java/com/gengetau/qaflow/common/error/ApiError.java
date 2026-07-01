package com.gengetau.qaflow.common.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
    String code,
    String message,
    int status,
    String path,
    OffsetDateTime timestamp,
    List<FieldViolation> fieldViolations) {

  public static ApiError of(String code, String message, int status, String path) {
    return new ApiError(code, message, status, path, OffsetDateTime.now(), List.of());
  }

  public record FieldViolation(String field, String message) {}
}
