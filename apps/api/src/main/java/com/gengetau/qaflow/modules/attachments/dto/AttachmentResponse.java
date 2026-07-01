package com.gengetau.qaflow.modules.attachments.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    UUID projectId,
    UUID defectId,
    UUID testRunItemId,
    UUID uploadedBy,
    String fileName,
    String contentType,
    long fileSize,
    OffsetDateTime createdAt) {}
