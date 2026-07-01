package com.gengetau.qaflow.modules.auth.dto;

import java.util.List;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds,
    UserSummaryResponse user,
    List<WorkspaceMembershipResponse> workspaces) {}