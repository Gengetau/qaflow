package com.gengetau.qaflow.modules.test_runs.dto;

import com.gengetau.qaflow.modules.test_runs.TestRunStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TestRunResponse(
    UUID id,
    UUID projectId,
    String name,
    String description,
    TestRunStatus status,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    UUID createdBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<TestRunItemResponse> items) {}
