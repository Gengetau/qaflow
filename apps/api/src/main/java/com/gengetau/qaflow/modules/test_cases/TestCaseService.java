package com.gengetau.qaflow.modules.test_cases;

import com.gengetau.qaflow.common.pagination.PageResponse;
import com.gengetau.qaflow.modules.activity.ActivityLog;
import com.gengetau.qaflow.modules.activity.ActivityLogRepository;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectService;
import com.gengetau.qaflow.modules.test_cases.dto.TestCaseRequest;
import com.gengetau.qaflow.modules.test_cases.dto.TestCaseResponse;
import com.gengetau.qaflow.modules.test_cases.dto.TestCaseStepRequest;
import com.gengetau.qaflow.modules.test_cases.dto.TestCaseStepResponse;
import com.gengetau.qaflow.modules.test_suites.TestSuite;
import com.gengetau.qaflow.modules.test_suites.TestSuiteRepository;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TestCaseService {

  private final TestCaseRepository testCaseRepository;
  private final TestSuiteRepository testSuiteRepository;
  private final ProjectService projectService;
  private final UserRepository userRepository;
  private final ActivityLogRepository activityLogRepository;

  public TestCaseService(
      TestCaseRepository testCaseRepository,
      TestSuiteRepository testSuiteRepository,
      ProjectService projectService,
      UserRepository userRepository,
      ActivityLogRepository activityLogRepository) {
    this.testCaseRepository = testCaseRepository;
    this.testSuiteRepository = testSuiteRepository;
    this.projectService = projectService;
    this.userRepository = userRepository;
    this.activityLogRepository = activityLogRepository;
  }

  @Transactional(readOnly = true)
  public PageResponse<TestCaseResponse> list(
      CurrentUser currentUser,
      UUID projectId,
      String query,
      TestCaseStatus status,
      TestCasePriority priority,
      UUID suiteId,
      int page,
      int size) {
    projectService.requireProjectForRead(projectId, currentUser);
    PageRequest pageRequest =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "updatedAt"));

    return PageResponse.from(
        testCaseRepository
            .findAll(filter(projectId, query, status, priority, suiteId), pageRequest)
            .map(this::toResponse));
  }

  @Transactional
  public TestCaseResponse create(CurrentUser currentUser, UUID projectId, TestCaseRequest request) {
    Project project = projectService.requireProjectForTestArtifactWrite(projectId, currentUser);
    User actor = requireUser(currentUser.id());
    TestSuite suite = requireSuite(projectId, request.suiteId());
    String caseKey = normalizeCaseKey(request.caseKey());
    if (testCaseRepository.existsByProject_IdAndCaseKey(projectId, caseKey)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Test case key already exists in this project");
    }

    TestCase testCase =
        new TestCase(
            project,
            suite,
            caseKey,
            request.title().trim(),
            trimToNull(request.description()),
            trimToNull(request.preconditions()),
            request.priority(),
            request.type(),
            request.status(),
            actor);
    testCase.replaceSteps(toSteps(request.steps()));
    TestCase saved = testCaseRepository.save(testCase);
    log(project, actor, saved.getId(), "TEST_CASE_CREATED");
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public TestCaseResponse get(CurrentUser currentUser, UUID testCaseId) {
    TestCase testCase = requireTestCase(testCaseId);
    projectService.requireProjectForRead(testCase.getProject().getId(), currentUser);
    return toResponse(testCase);
  }

  @Transactional
  public TestCaseResponse update(CurrentUser currentUser, UUID testCaseId, TestCaseRequest request) {
    TestCase testCase = requireTestCase(testCaseId);
    Project project = projectService.requireProjectForTestArtifactWrite(testCase.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    String caseKey = normalizeCaseKey(request.caseKey());

    if (testCaseRepository.existsByProject_IdAndCaseKeyAndIdNot(project.getId(), caseKey, testCaseId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Test case key already exists in this project");
    }

    testCase.update(
        requireSuite(project.getId(), request.suiteId()),
        caseKey,
        request.title().trim(),
        trimToNull(request.description()),
        trimToNull(request.preconditions()),
        request.priority(),
        request.type(),
        request.status(),
        actor);
    testCase.replaceSteps(toSteps(request.steps()));
    log(project, actor, testCase.getId(), "TEST_CASE_UPDATED");
    return toResponse(testCase);
  }

  @Transactional
  public void delete(CurrentUser currentUser, UUID testCaseId) {
    TestCase testCase = requireTestCase(testCaseId);
    Project project = projectService.requireProjectForTestArtifactWrite(testCase.getProject().getId(), currentUser);
    User actor = requireUser(currentUser.id());
    UUID deletedId = testCase.getId();
    testCaseRepository.delete(testCase);
    log(project, actor, deletedId, "TEST_CASE_DELETED");
  }

  private Specification<TestCase> filter(
      UUID projectId, String query, TestCaseStatus status, TestCasePriority priority, UUID suiteId) {
    return (root, criteriaQuery, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));

      if (query != null && !query.trim().isBlank()) {
        String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("caseKey")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)));
      }

      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }

      if (priority != null) {
        predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
      }

      if (suiteId != null) {
        predicates.add(criteriaBuilder.equal(root.get("suite").get("id"), suiteId));
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private TestCase requireTestCase(UUID testCaseId) {
    return testCaseRepository
        .findDetailedById(testCaseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test case was not found"));
  }

  private TestSuite requireSuite(UUID projectId, UUID suiteId) {
    if (suiteId == null) {
      return null;
    }

    return testSuiteRepository
        .findByIdAndProject_Id(suiteId, projectId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Suite does not belong to project"));
  }

  private User requireUser(UUID userId) {
    return userRepository
        .findById(userId)
        .filter(User::isActive)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not active"));
  }

  private List<TestCaseStep> toSteps(List<TestCaseStepRequest> requests) {
    return requests.stream()
        .sorted(Comparator.comparingInt(TestCaseStepRequest::stepOrder))
        .map(
            request ->
                new TestCaseStep(
                    request.stepOrder(), request.action().trim(), request.expectedResult().trim()))
        .toList();
  }

  private TestCaseResponse toResponse(TestCase testCase) {
    return new TestCaseResponse(
        testCase.getId(),
        testCase.getProject().getId(),
        testCase.getSuite() == null ? null : testCase.getSuite().getId(),
        testCase.getCaseKey(),
        testCase.getTitle(),
        testCase.getDescription(),
        testCase.getPreconditions(),
        testCase.getPriority(),
        testCase.getType(),
        testCase.getStatus(),
        testCase.getCreatedBy().getId(),
        testCase.getUpdatedBy().getId(),
        testCase.getCreatedAt(),
        testCase.getUpdatedAt(),
        testCase.getSteps().stream()
            .map(
                step ->
                    new TestCaseStepResponse(
                        step.getId(), step.getStepOrder(), step.getAction(), step.getExpectedResult()))
            .toList());
  }

  private void log(Project project, User actor, UUID entityId, String action) {
    activityLogRepository.save(
        new ActivityLog(project.getWorkspace(), project, actor, "TEST_CASE", entityId, action));
  }

  private String normalizeCaseKey(String caseKey) {
    return caseKey.trim().toUpperCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (value == null || value.trim().isBlank()) {
      return null;
    }
    return value.trim();
  }
}
