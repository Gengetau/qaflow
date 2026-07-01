package com.gengetau.qaflow.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 160) String displayName,
    @NotBlank @Size(max = 160) String workspaceName) {}