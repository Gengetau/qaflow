package com.gengetau.qaflow.modules.workspaces.dto;

import java.util.UUID;

public record WorkspaceUserResponse(UUID id, String email, String displayName, String avatarUrl) {}
