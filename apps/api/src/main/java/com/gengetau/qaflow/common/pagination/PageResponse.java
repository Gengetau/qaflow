package com.gengetau.qaflow.common.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> items, long totalItems, int totalPages, int page, int size) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize());
  }
}
