package com.gengetau.qaflow.modules.test_cases.dto;

import java.util.UUID;

public record TestCaseStepResponse(UUID id, int stepOrder, String action, String expectedResult) {}
