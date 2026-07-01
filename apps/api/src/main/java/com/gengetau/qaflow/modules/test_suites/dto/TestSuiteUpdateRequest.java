package com.gengetau.qaflow.modules.test_suites.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TestSuiteUpdateRequest(
    @NotBlank @Size(max = 180) String name,
    @Size(max = 4000) String description,
    @PositiveOrZero int sortOrder) {}
