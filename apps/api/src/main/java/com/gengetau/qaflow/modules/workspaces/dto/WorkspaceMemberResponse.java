package com.gengetau.qaflow.modules.workspaces.dto;

import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceMemberResponse(
    UUID id, WorkspaceUserResponse user, WorkspaceRole role, OffsetDateTime joinedAt) {}
