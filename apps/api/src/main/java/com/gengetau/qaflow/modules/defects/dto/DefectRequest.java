package com.gengetau.qaflow.modules.defects.dto;

import com.gengetau.qaflow.modules.defects.DefectPriority;
import com.gengetau.qaflow.modules.defects.DefectSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DefectRequest(
    @NotBlank @Size(max = 240) String title,
    @Size(max = 4000) String description,
    @NotNull DefectSeverity severity,
    @NotNull DefectPriority priority,
    UUID assigneeId) {}
