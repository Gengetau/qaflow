package com.gengetau.qaflow.modules.test_cases.dto;

import com.gengetau.qaflow.modules.test_cases.TestCasePriority;
import com.gengetau.qaflow.modules.test_cases.TestCaseStatus;
import com.gengetau.qaflow.modules.test_cases.TestCaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record TestCaseRequest(
    UUID suiteId,
    @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]*") String caseKey,
    @NotBlank @Size(max = 240) String title,
    @Size(max = 4000) String description,
    @Size(max = 4000) String preconditions,
    @NotNull TestCasePriority priority,
    @NotNull TestCaseType type,
    @NotNull TestCaseStatus status,
    @NotEmpty @Size(max = 200) List<@Valid TestCaseStepRequest> steps) {}
