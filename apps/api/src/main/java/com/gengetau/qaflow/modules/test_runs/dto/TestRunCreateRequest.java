package com.gengetau.qaflow.modules.test_runs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record TestRunCreateRequest(
    @NotBlank @Size(max = 180) String name,
    @Size(max = 4000) String description,
    @NotEmpty List<UUID> testCaseIds) {}
