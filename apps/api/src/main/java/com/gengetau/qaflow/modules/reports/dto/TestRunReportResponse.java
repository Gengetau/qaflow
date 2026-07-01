package com.gengetau.qaflow.modules.reports.dto;

import com.gengetau.qaflow.modules.defects.DefectPriority;
import com.gengetau.qaflow.modules.defects.DefectSeverity;
import com.gengetau.qaflow.modules.defects.DefectStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TestRunReportResponse(
    UUID projectId,
    String projectName,
    UUID testRunId,
    String testRunName,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    int totalCases,
    int passed,
    int failed,
    int blocked,
    int skipped,
    int passRate,
    List<FailedCase> failedCases,
    List<LinkedDefect> linkedDefects,
    OffsetDateTime generatedAt) {

  public record FailedCase(UUID testRunItemId, UUID testCaseId, String caseKey, String title, String actualResult) {}

  public record LinkedDefect(
      UUID id,
      UUID testRunItemId,
      String caseKey,
      String title,
      DefectSeverity severity,
      DefectPriority priority,
      DefectStatus status) {}
}
