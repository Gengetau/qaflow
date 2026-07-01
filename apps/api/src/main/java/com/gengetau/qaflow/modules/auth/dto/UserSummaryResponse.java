package com.gengetau.qaflow.modules.auth.dto;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String email, String displayName, String avatarUrl) {}