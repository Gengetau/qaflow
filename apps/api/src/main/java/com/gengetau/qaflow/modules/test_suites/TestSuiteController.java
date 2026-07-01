package com.gengetau.qaflow.modules.test_suites;

import com.gengetau.qaflow.modules.test_suites.dto.TestSuiteCreateRequest;
import com.gengetau.qaflow.modules.test_suites.dto.TestSuiteResponse;
import com.gengetau.qaflow.modules.test_suites.dto.TestSuiteUpdateRequest;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestSuiteController {

  private final TestSuiteService testSuiteService;

  public TestSuiteController(TestSuiteService testSuiteService) {
    this.testSuiteService = testSuiteService;
  }

  @GetMapping("/projects/{projectId}/suites")
  List<TestSuiteResponse> list(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID projectId) {
    return testSuiteService.list(currentUser, projectId);
  }

  @PostMapping("/projects/{projectId}/suites")
  @ResponseStatus(HttpStatus.CREATED)
  TestSuiteResponse create(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID projectId,
      @Valid @RequestBody TestSuiteCreateRequest request) {
    return testSuiteService.create(currentUser, projectId, request);
  }

  @PatchMapping("/suites/{suiteId}")
  TestSuiteResponse update(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID suiteId,
      @Valid @RequestBody TestSuiteUpdateRequest request) {
    return testSuiteService.update(currentUser, suiteId, request);
  }

  @DeleteMapping("/suites/{suiteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID suiteId) {
    testSuiteService.delete(currentUser, suiteId);
  }
}
