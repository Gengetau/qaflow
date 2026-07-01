package com.gengetau.qaflow.modules.test_runs;

import com.gengetau.qaflow.modules.test_runs.dto.TestRunCreateRequest;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunItemResponse;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunItemResultRequest;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunResponse;
import com.gengetau.qaflow.modules.test_runs.dto.TestRunUpdateRequest;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestRunController {

  private final TestRunService testRunService;

  public TestRunController(TestRunService testRunService) {
    this.testRunService = testRunService;
  }

  @GetMapping("/projects/{projectId}/test-runs")
  List<TestRunResponse> list(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return testRunService.list(currentUser, projectId);
  }

  @PostMapping("/projects/{projectId}/test-runs")
  @ResponseStatus(HttpStatus.CREATED)
  TestRunResponse create(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @Valid @RequestBody TestRunCreateRequest request) {
    return testRunService.create(currentUser, projectId, request);
  }

  @GetMapping("/test-runs/{testRunId}")
  TestRunResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID testRunId) {
    return testRunService.get(currentUser, testRunId);
  }

  @PatchMapping("/test-runs/{testRunId}")
  TestRunResponse update(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID testRunId,
      @Valid @RequestBody TestRunUpdateRequest request) {
    return testRunService.update(currentUser, testRunId, request);
  }

  @PostMapping("/test-runs/{testRunId}/start")
  TestRunResponse start(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID testRunId) {
    return testRunService.start(currentUser, testRunId);
  }

  @PostMapping("/test-runs/{testRunId}/complete")
  TestRunResponse complete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID testRunId) {
    return testRunService.complete(currentUser, testRunId);
  }

  @GetMapping("/test-runs/{testRunId}/items")
  List<TestRunItemResponse> listItems(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID testRunId) {
    return testRunService.listItems(currentUser, testRunId);
  }

  @PatchMapping("/test-run-items/{itemId}/result")
  TestRunItemResponse updateResult(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID itemId,
      @Valid @RequestBody TestRunItemResultRequest request) {
    return testRunService.updateResult(currentUser, itemId, request);
  }
}
