package com.gengetau.qaflow.modules.workspaces.dto;

import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkspaceMemberCreateRequest(
    @Email @NotBlank String email, @NotNull WorkspaceRole role) {}
