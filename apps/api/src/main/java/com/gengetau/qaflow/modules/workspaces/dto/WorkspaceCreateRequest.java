package com.gengetau.qaflow.modules.workspaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceCreateRequest(
    @NotBlank @Size(max = 160) String name) {}
