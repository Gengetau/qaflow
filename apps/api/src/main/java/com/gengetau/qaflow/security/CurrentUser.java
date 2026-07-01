package com.gengetau.qaflow.security;

import java.util.UUID;

public record CurrentUser(UUID id, String email, String displayName) {}