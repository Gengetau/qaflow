package com.gengetau.qaflow.modules.projects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProjectCreateRequest(
    @NotNull UUID workspaceId,
    @NotBlank @Size(max = 180) String name,
    @NotBlank @Size(max = 24) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*") String key,
    @Size(max = 4000) String description) {}
