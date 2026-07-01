package com.gengetau.qaflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.projects.ProjectRepository;
import com.gengetau.qaflow.modules.reports.ReportService;
import com.gengetau.qaflow.modules.reports.dto.DashboardResponse;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.users.UserRepository;
import com.gengetau.qaflow.modules.workspaces.Workspace;
import com.gengetau.qaflow.modules.workspaces.WorkspaceMemberRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRepository;
import com.gengetau.qaflow.modules.workspaces.WorkspaceRole;
import com.gengetau.qaflow.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:qaflow_demo_seed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
@ActiveProfiles({"test", "demo"})
class DemoDataSeederTest {

  @Autowired private UserRepository userRepository;
  @Autowired private WorkspaceRepository workspaceRepository;
  @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ReportService reportService;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void seedsDemoAccountsAndMeaningfulDashboardData() {
    User owner = userRepository.findByEmail("owner@example.com").orElseThrow();
    User tester = userRepository.findByEmail("tester@example.com").orElseThrow();
    User viewer = userRepository.findByEmail("viewer@example.com").orElseThrow();

    assertThat(passwordEncoder.matches("password123", owner.getPasswordHash())).isTrue();
    assertThat(passwordEncoder.matches("password123", tester.getPasswordHash())).isTrue();
    assertThat(passwordEncoder.matches("password123", viewer.getPasswordHash())).isTrue();

    Workspace workspace =
        workspaceRepository.findAll().stream()
            .filter(candidate -> candidate.getSlug().equals("qaflow-demo"))
            .findFirst()
            .orElseThrow();
    assertThat(workspaceMemberRepository.countByWorkspace_IdAndRole(workspace.getId(), WorkspaceRole.OWNER)).isEqualTo(1);
    assertThat(workspaceMemberRepository.countByWorkspace_IdAndRole(workspace.getId(), WorkspaceRole.TESTER)).isEqualTo(1);
    assertThat(workspaceMemberRepository.countByWorkspace_IdAndRole(workspace.getId(), WorkspaceRole.VIEWER)).isEqualTo(1);

    Project project =
        projectRepository.findAll().stream()
            .filter(candidate -> candidate.getKey().equals("SHOP"))
            .findFirst()
            .orElseThrow();

    DashboardResponse dashboard =
        reportService.dashboard(new CurrentUser(viewer.getId(), viewer.getEmail(), viewer.getDisplayName()), project.getId());

    assertThat(dashboard.totalTestCases()).isGreaterThanOrEqualTo(6);
    assertThat(dashboard.readyTestCases()).isGreaterThanOrEqualTo(5);
    assertThat(dashboard.latestPassRate()).isGreaterThan(0);
    assertThat(dashboard.activeTestRuns()).isGreaterThan(0);
    assertThat(dashboard.openDefects()).isGreaterThan(0);
    assertThat(dashboard.testResults().values()).anyMatch(count -> count > 0);
  }
}
