package com.gengetau.qaflow.modules.defects.dto;

import com.gengetau.qaflow.modules.defects.DefectPriority;
import com.gengetau.qaflow.modules.defects.DefectSeverity;
import com.gengetau.qaflow.modules.defects.DefectStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DefectResponse(
    UUID id,
    UUID projectId,
    UUID testRunItemId,
    UUID testCaseId,
    String caseKey,
    String title,
    String description,
    DefectSeverity severity,
    DefectPriority priority,
    DefectStatus status,
    UUID assigneeId,
    UUID reportedBy,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<DefectCommentResponse> comments) {}
