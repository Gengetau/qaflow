package com.gengetau.qaflow.modules.workspaces.dto;

import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record WorkspaceMemberUpdateRequest(@NotNull WorkspaceRole role) {}
