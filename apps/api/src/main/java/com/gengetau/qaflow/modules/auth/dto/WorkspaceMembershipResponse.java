package com.gengetau.qaflow.modules.auth.dto;

import java.util.UUID;

public record WorkspaceMembershipResponse(UUID id, String name, String slug, String role) {}