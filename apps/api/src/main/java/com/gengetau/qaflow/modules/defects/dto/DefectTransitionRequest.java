package com.gengetau.qaflow.modules.defects.dto;

import com.gengetau.qaflow.modules.defects.DefectStatus;
import jakarta.validation.constraints.NotNull;

public record DefectTransitionRequest(@NotNull DefectStatus status) {}
