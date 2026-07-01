package com.gengetau.qaflow.modules.test_cases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TestCaseStepRequest(
    @Positive int stepOrder,
    @NotBlank @Size(max = 4000) String action,
    @NotBlank @Size(max = 4000) String expectedResult) {}
