package com.gengetau.qaflow.modules.reports;

import com.gengetau.qaflow.modules.defects.Defect;
import com.gengetau.qaflow.modules.defects.DefectRepository;
import com.gengetau.qaflow.modules.defects.DefectSeverity;
import com.gengetau.qaflow.modules.defects.DefectStatus;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectService;
import com.gengetau.qaflow.modules.reports.dto.DashboardResponse;
import com.gengetau.qaflow.modules.reports.dto.ReportSummaryResponse;
import com.gengetau.qaflow.modules.reports.dto.ReportSummaryResponse.RunSummary;
import com.gengetau.qaflow.modules.reports.dto.TestRunReportResponse;
import com.gengetau.qaflow.modules.reports.dto.TestRunReportResponse.FailedCase;
import com.gengetau.qaflow.modules.reports.dto.TestRunReportResponse.LinkedDefect;
import com.gengetau.qaflow.modules.test_cases.TestCaseRepository;
import com.gengetau.qaflow.modules.test_cases.TestCaseStatus;
import com.gengetau.qaflow.modules.test_runs.TestRun;
import com.gengetau.qaflow.modules.test_runs.TestRunItem;
import com.gengetau.qaflow.modules.test_runs.TestRunItemResult;
import com.gengetau.qaflow.modules.test_runs.TestRunRepository;
import com.gengetau.qaflow.modules.test_runs.TestRunStatus;
import com.gengetau.qaflow.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {

  private final ProjectService projectService;
  private final TestCaseRepository testCaseRepository;
  private final TestRunRepository testRunRepository;
  private final DefectRepository defectRepository;

  public ReportService(
      ProjectService projectService,
      TestCaseRepository testCaseRepository,
      TestRunRepository testRunRepository,
      DefectRepository defectRepository) {
    this.projectService = projectService;
    this.testCaseRepository = testCaseRepository;
    this.testRunRepository = testRunRepository;
    this.defectRepository = defectRepository;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard(CurrentUser currentUser, UUID projectId) {
    projectService.requireProjectForRead(projectId, currentUser);
    List<TestRun> runs = testRunRepository.findByProject_IdOrderByCreatedAtDesc(projectId);
    List<Defect> defects = defectRepository.findDetailedByProjectIdOrderByCreatedAtDesc(projectId);
    TestRun latestRun = runs.isEmpty() ? null : runs.get(0);

    return new DashboardResponse(
        testCaseRepository.countByProject_Id(projectId),
        testCaseRepository.countByProject_IdAndStatus(projectId, TestCaseStatus.READY),
        runs.stream().filter((run) -> run.getStatus() == TestRunStatus.IN_PROGRESS).count(),
        latestRun == null ? 0 : passRate(latestRun),
        defects.stream().filter(this::isOpenDefect).count(),
        defects.stream().filter(this::isCriticalOpenDefect).count(),
        defectsByStatus(defects),
        latestRun == null ? emptyResultCounts() : resultCounts(latestRun));
  }

  @Transactional(readOnly = true)
  public ReportSummaryResponse summary(CurrentUser currentUser, UUID projectId) {
    Project project = projectService.requireProjectForRead(projectId, currentUser);
    List<TestRun> runs = testRunRepository.findByProject_IdOrderByCreatedAtDesc(projectId);
    List<Defect> defects = defectRepository.findDetailedByProjectIdOrderByCreatedAtDesc(projectId);
    TestRun latestRun = runs.isEmpty() ? null : runs.get(0);

    return new ReportSummaryResponse(
        project.getId(),
        project.getName(),
        testCaseRepository.countByProject_Id(projectId),
        testCaseRepository.countByProject_IdAndStatus(projectId, TestCaseStatus.READY),
        latestRun == null ? null : toRunSummary(latestRun),
        defects.stream().filter(this::isOpenDefect).count(),
        defects.stream().filter(this::isCriticalOpenDefect).count(),
        defectsByStatus(defects));
  }

  @Transactional(readOnly = true)
  public TestRunReportResponse testRunReport(CurrentUser currentUser, UUID projectId, UUID testRunId) {
    Project project = projectService.requireProjectForRead(projectId, currentUser);
    TestRun testRun =
        testRunRepository
            .findDetailedById(testRunId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run was not found"));
    if (!testRun.getProject().getId().equals(projectId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test run was not found for this project");
    }

    List<Defect> linkedDefects = linkedDefects(projectId, testRun);
    Map<TestRunItemResult, Long> counts = resultCounts(testRun);
    return new TestRunReportResponse(
        project.getId(),
        project.getName(),
        testRun.getId(),
        testRun.getName(),
        testRun.getStartedAt(),
        testRun.getCompletedAt(),
        testRun.getItems().size(),
        counts.get(TestRunItemResult.PASSED).intValue(),
        counts.get(TestRunItemResult.FAILED).intValue(),
        counts.get(TestRunItemResult.BLOCKED).intValue(),
        counts.get(TestRunItemResult.SKIPPED).intValue(),
        passRate(testRun),
        failedCases(testRun),
        linkedDefects.stream().map(this::toLinkedDefect).toList(),
        OffsetDateTime.now());
  }

  @Transactional(readOnly = true)
  public String exportHtml(CurrentUser currentUser, UUID projectId) {
    ReportSummaryResponse summary = summary(currentUser, projectId);
    List<TestRun> runs = testRunRepository.findByProject_IdOrderByCreatedAtDesc(projectId);
    TestRunReportResponse runReport = runs.isEmpty() ? null : testRunReport(currentUser, projectId, runs.get(0).getId());

    StringBuilder html = new StringBuilder();
    html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>QAFlow Report</title>");
    html.append("<style>body{font-family:Arial,sans-serif;color:#17202a;margin:32px;}");
    html.append("table{border-collapse:collapse;width:100%;}td,th{border:1px solid #d1d5db;padding:8px;}");
    html.append(".metric{display:inline-block;margin-right:24px;}</style></head><body>");
    html.append("<h1>").append(escape(summary.projectName())).append("</h1>");
    html.append("<p class=\"metric\">").append(summary.latestRun() == null ? 0 : summary.latestRun().passRate());
    html.append("% pass rate</p>");
    html.append("<p class=\"metric\">Open defects: ").append(summary.openDefects()).append("</p>");
    html.append("<p class=\"metric\">Critical defects: ").append(summary.criticalDefects()).append("</p>");

    if (runReport != null) {
      html.append("<h2>").append(escape(runReport.testRunName())).append("</h2>");
      html.append("<p>Total cases: ").append(runReport.totalCases()).append("</p>");
      html.append("<h3>Failed cases</h3><table><thead><tr><th>Case</th><th>Title</th><th>Actual result</th></tr></thead><tbody>");
      for (FailedCase failedCase : runReport.failedCases()) {
        html.append("<tr><td>").append(escape(failedCase.caseKey())).append("</td><td>");
        html.append(escape(failedCase.title())).append("</td><td>");
        html.append(escape(failedCase.actualResult())).append("</td></tr>");
      }
      html.append("</tbody></table><h3>Linked defects</h3><ul>");
      for (LinkedDefect defect : runReport.linkedDefects()) {
        html.append("<li>").append(escape(defect.title())).append(" - ").append(defect.status()).append("</li>");
      }
      html.append("</ul>");
    }
    html.append("<p>Generated at ").append(OffsetDateTime.now()).append("</p></body></html>");
    return html.toString();
  }

  private RunSummary toRunSummary(TestRun testRun) {
    Map<TestRunItemResult, Long> counts = resultCounts(testRun);
    return new RunSummary(
        testRun.getId(),
        testRun.getName(),
        testRun.getStatus(),
        testRun.getItems().size(),
        counts.get(TestRunItemResult.PASSED).intValue(),
        counts.get(TestRunItemResult.FAILED).intValue(),
        counts.get(TestRunItemResult.BLOCKED).intValue(),
        counts.get(TestRunItemResult.SKIPPED).intValue(),
        passRate(testRun),
        testRun.getStartedAt(),
        testRun.getCompletedAt());
  }

  private Map<DefectStatus, Long> defectsByStatus(List<Defect> defects) {
    Map<DefectStatus, Long> counts = new EnumMap<>(DefectStatus.class);
    for (DefectStatus status : DefectStatus.values()) {
      counts.put(status, 0L);
    }
    defects.forEach((defect) -> counts.compute(defect.getStatus(), (status, count) -> count == null ? 1L : count + 1));
    return counts;
  }

  private Map<TestRunItemResult, Long> resultCounts(TestRun testRun) {
    Map<TestRunItemResult, Long> counts = emptyResultCounts();
    testRun.getItems()
        .forEach((item) -> counts.compute(item.getResult(), (result, count) -> count == null ? 1L : count + 1));
    return counts;
  }

  private Map<TestRunItemResult, Long> emptyResultCounts() {
    Map<TestRunItemResult, Long> counts = new EnumMap<>(TestRunItemResult.class);
    for (TestRunItemResult result : TestRunItemResult.values()) {
      counts.put(result, 0L);
    }
    return counts;
  }

  private int passRate(TestRun testRun) {
    long executed =
        testRun.getItems().stream().filter((item) -> item.getResult() != TestRunItemResult.UNTESTED).count();
    if (executed == 0) {
      return 0;
    }
    long passed = testRun.getItems().stream().filter((item) -> item.getResult() == TestRunItemResult.PASSED).count();
    return Math.round((passed * 100.0f) / executed);
  }

  private List<FailedCase> failedCases(TestRun testRun) {
    return testRun.getItems().stream()
        .filter((item) -> item.getResult() == TestRunItemResult.FAILED)
        .map(
            (item) ->
                new FailedCase(
                    item.getId(),
                    item.getTestCase().getId(),
                    item.getTestCase().getCaseKey(),
                    item.getTestCase().getTitle(),
                    item.getActualResult()))
        .toList();
  }

  private List<Defect> linkedDefects(UUID projectId, TestRun testRun) {
    Set<UUID> runItemIds = testRun.getItems().stream().map(TestRunItem::getId).collect(Collectors.toSet());
    return defectRepository.findDetailedByProjectIdOrderByCreatedAtDesc(projectId).stream()
        .filter((defect) -> defect.getTestRunItem() != null)
        .filter((defect) -> runItemIds.contains(defect.getTestRunItem().getId()))
        .toList();
  }

  private LinkedDefect toLinkedDefect(Defect defect) {
    return new LinkedDefect(
        defect.getId(),
        defect.getTestRunItem() == null ? null : defect.getTestRunItem().getId(),
        defect.getTestRunItem() == null ? null : defect.getTestRunItem().getTestCase().getCaseKey(),
        defect.getTitle(),
        defect.getSeverity(),
        defect.getPriority(),
        defect.getStatus());
  }

  private boolean isOpenDefect(Defect defect) {
    return defect.getStatus() == DefectStatus.OPEN
        || defect.getStatus() == DefectStatus.IN_PROGRESS
        || defect.getStatus() == DefectStatus.REOPENED;
  }

  private boolean isCriticalOpenDefect(Defect defect) {
    return defect.getSeverity() == DefectSeverity.CRITICAL && isOpenDefect(defect);
  }

  private String escape(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
