package com.gengetau.qaflow.modules.projects.dto;

import com.gengetau.qaflow.modules.projects.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
    @NotBlank @Size(max = 180) String name,
    @Size(max = 4000) String description,
    @NotNull ProjectStatus status) {}
