package com.gengetau.qaflow.modules.test_cases.dto;

import com.gengetau.qaflow.modules.test_cases.TestCasePriority;
import com.gengetau.qaflow.modules.test_cases.TestCaseStatus;
import com.gengetau.qaflow.modules.test_cases.TestCaseType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TestCaseResponse(
    UUID id,
    UUID projectId,
    UUID suiteId,
    String caseKey,
    String title,
    String description,
    String preconditions,
    TestCasePriority priority,
    TestCaseType type,
    TestCaseStatus status,
    UUID createdBy,
    UUID updatedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<TestCaseStepResponse> steps) {}
