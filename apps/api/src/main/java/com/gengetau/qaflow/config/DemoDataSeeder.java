package com.gengetau.qaflow.config;

import com.gengetau.qaflow.modules.defects.Defect;
import com.gengetau.qaflow.modules.defects.DefectPriority;
import com.gengetau.qaflow.modules.defects.DefectRepository;
import com.gengetau.qaflow.modules.defects.DefectSeverity;
import com.gengetau.qaflow.modules.defects.DefectStatus;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectRepository;
import com.gengetau.qaflow.modules.test_cases.TestCase;
import com.gengetau.qaflow.modules.test_cases.TestCasePriority;
import com.gengetau.qaflow.modules.test_cases.TestCaseRepository;
import com.gengetau.qaflow.modules.test_cases.TestCaseStatus;
import com.gengetau.qaflow.modules.test_cases.TestCaseStep;
import com.gengetau.qaflow.modules.test_cases.TestCaseType;
import com.gengetau.qaflow.modules.test_runs.TestRun;
import com.gengetau.qaflow.modules.test_runs.TestRunItem;
import com.gengetau.qaflow.modules.test_runs.TestRunItemResult;
import com.gengetau.qaflow.modules.test_runs.TestRunRepository;
import com.gengetau.qaflow.modules.test_suites.TestSuite;
import com.gengetau.qaflow.modules.test_suites.TestSuiteRepository;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.modules.workspaces.Workspace;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMember;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMemberRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String TESTER_EMAIL = "tester@example.com";
  private static final String VIEWER_EMAIL = "viewer@example.com";
  private static final String DEMO_PASSWORD = "password123";

  private final UserRepository userRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final ProjectRepository projectRepository;
  private final TestSuiteRepository testSuiteRepository;
  private final TestCaseRepository testCaseRepository;
  private final TestRunRepository testRunRepository;
  private final DefectRepository defectRepository;
  private final PasswordEncoder passwordEncoder;

  public DemoDataSeeder(
      UserRepository userRepository,
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository workspaceMemberRepository,
      ProjectRepository projectRepository,
      TestSuiteRepository testSuiteRepository,
      TestCaseRepository testCaseRepository,
      TestRunRepository testRunRepository,
      DefectRepository defectRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.projectRepository = projectRepository;
    this.testSuiteRepository = testSuiteRepository;
    this.testCaseRepository = testCaseRepository;
    this.testRunRepository = testRunRepository;
    this.defectRepository = defectRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (userRepository.existsByEmail(OWNER_EMAIL)) {
      return;
    }

    User owner = createUser(OWNER_EMAIL, "Demo Owner");
    User tester = createUser(TESTER_EMAIL, "Demo Tester");
    User viewer = createUser(VIEWER_EMAIL, "Demo Viewer");

    Workspace workspace = workspaceRepository.save(new Workspace("QAFlow Demo Workspace", "qaflow-demo", owner));
    workspaceMemberRepository.save(new WorkspaceMember(workspace, owner, WorkspaceRole.OWNER));
    workspaceMemberRepository.save(new WorkspaceMember(workspace, tester, WorkspaceRole.TESTER));
    workspaceMemberRepository.save(new WorkspaceMember(workspace, viewer, WorkspaceRole.VIEWER));

    Project project =
        projectRepository.save(
            new Project(
                workspace,
                "Storefront Release QA",
                "SHOP",
                "Demo release project with checkout, account, and reporting coverage.",
                owner));

    TestSuite checkoutSuite =
        testSuiteRepository.save(new TestSuite(project, "Checkout Regression", "Guest and signed-in checkout flows.", 10));
    TestSuite accountSuite =
        testSuiteRepository.save(new TestSuite(project, "Account Smoke", "Login and account management checks.", 20));

    List<TestCase> cases =
        testCaseRepository.saveAll(
            List.of(
                testCase(
                    project,
                    checkoutSuite,
                    "SHOP-1",
                    "Guest checkout completes with credit card",
                    TestCasePriority.CRITICAL,
                    TestCaseType.REGRESSION,
                    TestCaseStatus.READY,
                    owner,
                    "Add a physical item to cart",
                    "Order confirmation is displayed"),
                testCase(
                    project,
                    checkoutSuite,
                    "SHOP-2",
                    "Coupon checkout applies campaign discount",
                    TestCasePriority.HIGH,
                    TestCaseType.REGRESSION,
                    TestCaseStatus.READY,
                    owner,
                    "Apply SAVE10 during checkout",
                    "Discounted total is charged"),
                testCase(
                    project,
                    checkoutSuite,
                    "SHOP-3",
                    "Saved card checkout completes for returning buyer",
                    TestCasePriority.HIGH,
                    TestCaseType.REGRESSION,
                    TestCaseStatus.READY,
                    owner,
                    "Sign in with a saved payment method",
                    "Order is placed with saved card"),
                testCase(
                    project,
                    accountSuite,
                    "SHOP-4",
                    "Customer can reset password from login",
                    TestCasePriority.MEDIUM,
                    TestCaseType.FUNCTIONAL,
                    TestCaseStatus.READY,
                    owner,
                    "Request a password reset link",
                    "Reset email is sent"),
                testCase(
                    project,
                    accountSuite,
                    "SHOP-5",
                    "Order history shows latest purchase",
                    TestCasePriority.MEDIUM,
                    TestCaseType.SMOKE,
                    TestCaseStatus.READY,
                    owner,
                    "Open account order history",
                    "Latest order appears first"),
                testCase(
                    project,
                    accountSuite,
                    "SHOP-6",
                    "Gift card checkout exploratory notes",
                    TestCasePriority.LOW,
                    TestCaseType.EXPLORATORY,
                    TestCaseStatus.DRAFT,
                    owner,
                    "Explore split gift card payment",
                    "Risks and follow-ups are captured")));

    TestRun activeRun = new TestRun(project, "Storefront RC active sweep", "In-progress exploratory pass.", tester);
    activeRun.start();
    activeRun.addItem(new TestRunItem(cases.get(0)));
    activeRun.addItem(new TestRunItem(cases.get(1)));
    activeRun.addItem(new TestRunItem(cases.get(4)));
    testRunRepository.save(activeRun);

    TestRun completedRun = new TestRun(project, "Storefront RC regression", "Completed release candidate regression.", tester);
    for (TestCase testCase : cases) {
      if (testCase.getStatus() == TestCaseStatus.READY) {
        completedRun.addItem(new TestRunItem(testCase));
      }
    }
    completedRun.start();
    completedRun.getItems().get(0).execute(TestRunItemResult.PASSED, "Order confirmation displayed.", tester);
    completedRun.getItems().get(1).execute(TestRunItemResult.FAILED, "Coupon service returned HTTP 500.", tester);
    completedRun.getItems().get(2).execute(TestRunItemResult.PASSED, "Saved card order completed.", tester);
    completedRun.getItems().get(3).execute(TestRunItemResult.BLOCKED, "Email sandbox unavailable.", tester);
    completedRun.getItems().get(4).execute(TestRunItemResult.PASSED, "Order history sorted correctly.", tester);
    completedRun.complete();
    completedRun = testRunRepository.save(completedRun);

    Defect couponDefect =
        new Defect(
            project,
            completedRun.getItems().get(1),
            "Coupon checkout returns 500",
            "The SAVE10 campaign fails during payment review and blocks discounted checkout.",
            DefectSeverity.CRITICAL,
            DefectPriority.URGENT,
            tester,
            tester);
    Defect resetEmailDefect =
        new Defect(
            project,
            completedRun.getItems().get(3),
            "Password reset email sandbox unavailable",
            "Reset email cannot be verified until the mail sandbox is restored.",
            DefectSeverity.HIGH,
            DefectPriority.HIGH,
            tester,
            tester);
    resetEmailDefect.transitionTo(DefectStatus.IN_PROGRESS);
    defectRepository.saveAll(List.of(couponDefect, resetEmailDefect));
  }

  private User createUser(String email, String displayName) {
    return userRepository.save(new User(email, passwordEncoder.encode(DEMO_PASSWORD), displayName));
  }

  private TestCase testCase(
      Project project,
      TestSuite suite,
      String caseKey,
      String title,
      TestCasePriority priority,
      TestCaseType type,
      TestCaseStatus status,
      User actor,
      String action,
      String expectedResult) {
    TestCase testCase =
        new TestCase(
            project,
            suite,
            caseKey,
            title,
            "Demo coverage for " + title.toLowerCase() + ".",
            "Demo storefront build is deployed.",
            priority,
            type,
            status,
            actor);
    testCase.addStep(new TestCaseStep(1, action, expectedResult));
    testCase.addStep(new TestCaseStep(2, "Record evidence and result", "Result notes are captured in QAFlow"));
    return testCase;
  }
}
