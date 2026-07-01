package com.gengetau.qaflow.modules.auth.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
    UUID id,
    String email,
    String displayName,
    String avatarUrl,
    List<WorkspaceMembershipResponse> workspaces) {}