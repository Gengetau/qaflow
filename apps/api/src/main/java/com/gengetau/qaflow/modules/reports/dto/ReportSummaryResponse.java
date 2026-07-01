package com.gengetau.qaflow.modules.reports.dto;

import com.gengetau.qaflow.modules.defects.DefectStatus;
import com.gengetau.qaflow.modules.test_runs.TestRunStatus;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ReportSummaryResponse(
    UUID projectId,
    String projectName,
    long totalTestCases,
    long readyTestCases,
    RunSummary latestRun,
    long openDefects,
    long criticalDefects,
    Map<DefectStatus, Long> defectsByStatus) {

  public record RunSummary(
      UUID id,
      String name,
      TestRunStatus status,
      int totalCases,
      int passed,
      int failed,
      int blocked,
      int skipped,
      int passRate,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt) {}
}
