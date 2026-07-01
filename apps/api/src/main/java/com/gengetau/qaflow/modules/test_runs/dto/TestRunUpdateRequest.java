package com.gengetau.qaflow.modules.test_runs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestRunUpdateRequest(@NotBlank @Size(max = 180) String name, @Size(max = 4000) String description) {}
