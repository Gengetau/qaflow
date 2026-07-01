package com.gengetau.qaflow.modules.workspaces.dto;

import java.util.List;
import java.util.UUID;

public record WorkspaceDetailResponse(
    UUID id, String name, String slug, String role, List<WorkspaceMemberResponse> members) {}
