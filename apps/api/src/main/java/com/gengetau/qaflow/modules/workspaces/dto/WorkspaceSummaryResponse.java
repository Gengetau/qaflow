package com.gengetau.qaflow.modules.workspaces.dto;

import java.util.UUID;

public record WorkspaceSummaryResponse(UUID id, String name, String slug, String role) {}
