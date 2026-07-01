package com.gengetau.qaflow.modules.projects.dto;

import com.gengetau.qaflow.modules.projects.ProjectStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    UUID workspaceId,
    String name,
    String key,
    String description,
    ProjectStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
