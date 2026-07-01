package com.gengetau.qaflow.modules.test_cases;

import com.gengetau.qaflow.common.pagination.PageResponse;
import com.gengetau.qaflow.modules.test_cases.dto.TestCaseRequest;
import com.gengetau.qaflow.modules.test_cases.dto.TestCaseResponse;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestCaseController {

  private final TestCaseService testCaseService;

  public TestCaseController(TestCaseService testCaseService) {
    this.testCaseService = testCaseService;
  }

  @GetMapping("/projects/{projectId}/test-cases")
  PageResponse<TestCaseResponse> list(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) TestCaseStatus status,
      @RequestParam(required = false) TestCasePriority priority,
      @RequestParam(required = false) UUID suiteId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return testCaseService.list(currentUser, projectId, query, status, priority, suiteId, page, size);
  }

  @PostMapping("/projects/{projectId}/test-cases")
  @ResponseStatus(HttpStatus.CREATED)
  TestCaseResponse create(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @Valid @RequestBody TestCaseRequest request) {
    return testCaseService.create(currentUser, projectId, request);
  }

  @GetMapping("/test-cases/{testCaseId}")
  TestCaseResponse get(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID testCaseId) {
    return testCaseService.get(currentUser, testCaseId);
  }

  @PatchMapping("/test-cases/{testCaseId}")
  TestCaseResponse update(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID testCaseId,
      @Valid @RequestBody TestCaseRequest request) {
    return testCaseService.update(currentUser, testCaseId, request);
  }

  @DeleteMapping("/test-cases/{testCaseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID testCaseId) {
    testCaseService.delete(currentUser, testCaseId);
  }
}
