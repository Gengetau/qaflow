package com.gengetau.qaflow.modules.test_runs.dto;

import com.gengetau.qaflow.modules.test_runs.TestRunItemResult;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TestRunItemResponse(
    UUID id,
    UUID testRunId,
    UUID testCaseId,
    String caseKey,
    String title,
    UUID assigneeId,
    TestRunItemResult result,
    String actualResult,
    OffsetDateTime executedAt,
    UUID executedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
