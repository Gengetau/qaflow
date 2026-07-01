package com.gengetau.qaflow.modules.defects.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DefectCommentResponse(
    UUID id, UUID defectId, UUID authorId, String body, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
