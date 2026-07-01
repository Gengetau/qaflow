package com.gengetau.qaflow.modules.test_runs.dto;

import com.gengetau.qaflow.modules.test_runs.TestRunItemResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestRunItemResultRequest(
    @NotNull TestRunItemResult result, @Size(max = 4000) String actualResult) {}
