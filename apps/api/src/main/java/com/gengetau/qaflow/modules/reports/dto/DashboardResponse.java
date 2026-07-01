package com.gengetau.qaflow.modules.reports.dto;

import com.gengetau.qaflow.modules.defects.DefectStatus;
import com.gengetau.qaflow.modules.test_runs.TestRunItemResult;
import java.util.Map;

public record DashboardResponse(
    long totalTestCases,
    long readyTestCases,
    long activeTestRuns,
    int latestPassRate,
    long openDefects,
    long criticalDefects,
    Map<DefectStatus, Long> defectsByStatus,
    Map<TestRunItemResult, Long> testResults) {}
