package com.gengetau.qaflow.modules.reports;

import com.gengetau.qaflow.modules.reports.dto.DashboardResponse;
import com.gengetau.qaflow.modules.reports.dto.ReportSummaryResponse;
import com.gengetau.qaflow.modules.reports.dto.TestRunReportResponse;
import com.gengetau.qaflow.security.CurrentUser;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/dashboard")
  DashboardResponse dashboard(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return reportService.dashboard(currentUser, projectId);
  }

  @GetMapping("/reports/summary")
  ReportSummaryResponse summary(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return reportService.summary(currentUser, projectId);
  }

  @GetMapping("/reports/test-run/{testRunId}")
  TestRunReportResponse testRunReport(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @PathVariable UUID testRunId) {
    return reportService.testRunReport(currentUser, projectId, testRunId);
  }

  @PostMapping("/reports/export")
  ResponseEntity<String> exportHtml(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(reportService.exportHtml(currentUser, projectId));
  }
}
