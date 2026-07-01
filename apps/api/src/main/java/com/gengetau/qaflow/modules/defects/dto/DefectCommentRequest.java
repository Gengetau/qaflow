package com.gengetau.qaflow.modules.defects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DefectCommentRequest(@NotBlank @Size(max = 4000) String body) {}
