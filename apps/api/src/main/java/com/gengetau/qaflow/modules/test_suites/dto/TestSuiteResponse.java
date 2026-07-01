package com.gengetau.qaflow.modules.test_suites.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TestSuiteResponse(
    UUID id,
    UUID projectId,
    String name,
    String description,
    int sortOrder,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
