package com.gengetau.qaflow.modules.test_suites;

import com.gengetau.qaflow.modules.activity.ActivityLog;
import com.gengetau.qaflow.modules.activity.ActivityLogRepository;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectService;
import com.gengetau.qaflow.modules.test_suites.dto.TestSuiteCreateRequest;
import com.gengetau.qaflow.modules.test_suites.dto.TestSuiteResponse;
import com.gengetau.qaflow.modules.test_suites.dto.TestSuiteUpdateRequest;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TestSuiteService {

  private final TestSuiteRepository testSuiteRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;
  private final ActivityLogRepository activityLogRepository;

  public TestSuiteService(
      TestSuiteRepository testSuiteRepository,
      ProjectService projectService,
      UserRepository userRepository,
      ActivityLogRepository activityLogRepository) {
    this.testSuiteRepository = testSuiteRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
    this.activityLogRepository = activityLogRepository;
  }

  @Transactional(readOnly = true)
  public List<TestSuiteResponse> list(CurrentUser currentUser, UUID projectId) {
    projectService.requireProjectForRead(projectId, currentUser);
    return testSuiteRepository.findByProject_IdOrderBySortOrderAscNameAsc(projectId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public TestSuiteResponse create(CurrentUser currentUser, UUID projectId, TestSuiteCreateRequest request) {
    Project project = projectService.requireProjectForTestArtifactWrite(projectId, currentUser);
    TestSuite suite =
        testSuiteRepository.save(
            new TestSuite(project, request.name().trim(), trimToNull(request.description()), request.sortOrder()));
    log(project, requireUser(currentUser.id()), "TEST_SUITE", suite.getId(), "TEST_SUITE_CREATED");
    return toResponse(suite);
  }

  @Transactional
  public TestSuiteResponse update(CurrentUser currentUser, UUID suiteId, TestSuiteUpdateRequest request) {
    TestSuite suite = requireSuite(suiteId);
    Project project = projectService.requireProjectForTestArtifactWrite(suite.getProject().getId(), currentUser);
    suite.update(request.name().trim(), trimToNull(request.description()), request.sortOrder());
    log(project, requireUser(currentUser.id()), "TEST_SUITE", suite.getId(), "TEST_SUITE_UPDATED");
    return toResponse(suite);
  }

  @Transactional
  public void delete(CurrentUser currentUser, UUID suiteId) {
    TestSuite suite = requireSuite(suiteId);
    Project project = projectService.requireProjectForTestArtifactWrite(suite.getProject().getId(), currentUser);
    UUID deletedId = suite.getId();
    testSuiteRepository.delete(suite);
    log(project, requireUser(currentUser.id()), "TEST_SUITE", deletedId, "TEST_SUITE_DELETED");
  }

  private TestSuite requireSuite(UUID suiteId) {
    return testSuiteRepository
        .findById(suiteId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test suite was not found"));
  }

  private TestSuiteResponse toResponse(TestSuite suite) {
    return new TestSuiteResponse(
        suite.getId(),
        suite.getProject().getId(),
        suite.getName(),
        suite.getDescription(),
        suite.getSortOrder(),
        suite.getCreatedAt(),
        suite.getUpdatedAt());
  }

  private User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
  }

  private void log(Project project, User actor, String entityType, UUID entityId, String action) {
    activityLogRepository.save(
        new ActivityLog(project.getWorkspace(), project, actor, entityType, entityId, action));
  }

  private String trimToNull(String value) {
    if (value == null || value.trim().isBlank()) {
      return null;
    }
    return value.trim();
  }
}
