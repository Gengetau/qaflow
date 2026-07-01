package com.gengetau.qaflow.modules.reports;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:qaflow_reports;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "qaflow.security.jwt-secret=test-secret-test-secret-test-secret-test-secret-test-secret",
      "qaflow.security.access-token-minutes=15",
      "qaflow.security.refresh-token-days=7"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void dashboardAndReportsAggregateProjectQualityData() throws Exception {
    RegisteredUser owner = register("owner-reports@example.com", "Report Owner", "Report Workspace");
    RegisteredUser tester = register("tester-reports@example.com", "Report Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-reports@example.com", "Report Viewer", "Viewer Workspace");
    RegisteredUser outsider = register("outsider-reports@example.com", "Report Outsider", "Outsider Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER").andExpect(status().isCreated());
    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER").andExpect(status().isCreated());

    UUID projectId =
        createProject(owner, owner.workspaceId(), "Checkout Reports", "REP")
            .andExpect(status().isCreated())
            .andReturnId();
    UUID firstCaseId = createTestCase(tester, projectId, "REP-1", "Guest checkout").andExpect(status().isCreated()).andReturnId();
    UUID secondCaseId = createTestCase(tester, projectId, "REP-2", "Coupon checkout").andExpect(status().isCreated()).andReturnId();

    MvcResult runResult = createRun(tester, projectId, firstCaseId, secondCaseId).andExpect(status().isCreated()).andReturn();
    JsonNode runJson = objectMapper.readTree(runResult.getResponse().getContentAsString());
    UUID testRunId = UUID.fromString(runJson.get("id").asText());
    UUID passedItemId = itemIdByCaseKey(runJson, "REP-1");
    UUID failedItemId = itemIdByCaseKey(runJson, "REP-2");

    mockMvc.perform(post("/api/test-runs/{testRunId}/start", testRunId).header("Authorization", bearer(tester)))
        .andExpect(status().isOk());
    updateItemResult(tester, passedItemId, "PASSED", "Order completed").andExpect(status().isOk());
    updateItemResult(tester, failedItemId, "FAILED", "Coupon rejected").andExpect(status().isOk());
    mockMvc.perform(post("/api/test-runs/{testRunId}/complete", testRunId).header("Authorization", bearer(tester)))
        .andExpect(status().isOk());
    createDefectFromItem(tester, failedItemId).andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/projects/{projectId}/dashboard", projectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalTestCases").value(2))
        .andExpect(jsonPath("$.readyTestCases").value(2))
        .andExpect(jsonPath("$.activeTestRuns").value(0))
        .andExpect(jsonPath("$.latestPassRate").value(50))
        .andExpect(jsonPath("$.openDefects").value(1))
        .andExpect(jsonPath("$.criticalDefects").value(1))
        .andExpect(jsonPath("$.defectsByStatus.OPEN").value(1))
        .andExpect(jsonPath("$.testResults.PASSED").value(1))
        .andExpect(jsonPath("$.testResults.FAILED").value(1));

    mockMvc
        .perform(get("/api/projects/{projectId}/reports/summary", projectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectName").value("Checkout Reports"))
        .andExpect(jsonPath("$.latestRun.name").value("Checkout release report"))
        .andExpect(jsonPath("$.latestRun.passRate").value(50))
        .andExpect(jsonPath("$.defectsByStatus.OPEN").value(1));

    mockMvc
        .perform(
            get("/api/projects/{projectId}/reports/test-run/{testRunId}", projectId, testRunId)
                .header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.testRunName").value("Checkout release report"))
        .andExpect(jsonPath("$.totalCases").value(2))
        .andExpect(jsonPath("$.passed").value(1))
        .andExpect(jsonPath("$.failed").value(1))
        .andExpect(jsonPath("$.passRate").value(50))
        .andExpect(jsonPath("$.failedCases[0].caseKey").value("REP-2"))
        .andExpect(jsonPath("$.linkedDefects[0].title").value("Coupon checkout failure"));

    mockMvc
        .perform(post("/api/projects/{projectId}/reports/export", projectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("Checkout Reports")))
        .andExpect(content().string(containsString("50% pass rate")))
        .andExpect(content().string(containsString("Coupon checkout failure")));

    mockMvc
        .perform(get("/api/projects/{projectId}/dashboard", projectId).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden());
  }

  private ResultWithId createProject(RegisteredUser actor, UUID workspaceId, String name, String key)
      throws Exception {
    return new ResultWithId(
        mockMvc.perform(
            post("/api/projects")
                .header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "workspaceId": "%s",
                      "name": "%s",
                      "key": "%s",
                      "description": "Project quality scope"
                    }
                    """
                        .formatted(workspaceId, name, key))));
  }

  private ResultWithId createTestCase(RegisteredUser actor, UUID projectId, String caseKey, String title)
      throws Exception {
    return new ResultWithId(
        mockMvc.perform(
            post("/api/projects/{projectId}/test-cases", projectId)
                .header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "caseKey": "%s",
                      "title": "%s",
                      "description": "Checkout coverage",
                      "preconditions": "Cart has one item",
                      "priority": "HIGH",
                      "type": "REGRESSION",
                      "status": "READY",
                      "steps": [
                        {
                          "stepOrder": 1,
                          "action": "Open checkout",
                          "expectedResult": "Checkout form is shown"
                        }
                      ]
                    }
                    """
                        .formatted(caseKey, title))));
  }

  private ResultActions createRun(RegisteredUser actor, UUID projectId, UUID firstCaseId, UUID secondCaseId)
      throws Exception {
    return mockMvc.perform(
        post("/api/projects/{projectId}/test-runs", projectId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "name": "Checkout release report",
                  "description": "Release candidate report",
                  "testCaseIds": ["%s", "%s"]
                }
                """
                    .formatted(firstCaseId, secondCaseId)));
  }

  private ResultActions updateItemResult(
      RegisteredUser actor, UUID itemId, String result, String actualResult) throws Exception {
    return mockMvc.perform(
        patch("/api/test-run-items/{itemId}/result", itemId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "result": "%s",
                  "actualResult": "%s"
                }
                """
                    .formatted(result, actualResult)));
  }

  private ResultActions createDefectFromItem(RegisteredUser actor, UUID itemId) throws Exception {
    return mockMvc.perform(
        post("/api/test-run-items/{itemId}/defects", itemId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "title": "Coupon checkout failure",
                  "description": "Coupon was rejected during checkout",
                  "severity": "CRITICAL",
                  "priority": "URGENT"
                }
                """));
  }

  private ResultActions addMember(RegisteredUser actor, UUID workspaceId, String email, String role)
      throws Exception {
    return mockMvc.perform(
        post("/api/workspaces/{workspaceId}/members", workspaceId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "email": "%s",
                  "role": "%s"
                }
                """
                    .formatted(email, role)));
  }

  private RegisteredUser register(String email, String displayName, String workspaceName) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "%s",
                          "password": "password123",
                          "displayName": "%s",
                          "workspaceName": "%s"
                        }
                        """
                            .formatted(email, displayName, workspaceName)))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
    return new RegisteredUser(
        email, json.get("accessToken").asText(), UUID.fromString(json.get("workspaces").get(0).get("id").asText()));
  }

  private UUID itemIdByCaseKey(JsonNode runJson, String caseKey) {
    for (JsonNode item : runJson.get("items")) {
      if (caseKey.equals(item.get("caseKey").asText())) {
        return UUID.fromString(item.get("id").asText());
      }
    }
    throw new IllegalArgumentException("Run item was not found for case key " + caseKey);
  }

  private String bearer(RegisteredUser user) {
    return "Bearer " + user.accessToken();
  }

  private record RegisteredUser(String email, String accessToken, UUID workspaceId) {}

  private class ResultWithId {
    private final ResultActions resultActions;

    private ResultWithId(ResultActions resultActions) {
      this.resultActions = resultActions;
    }

    ResultWithId andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
      resultActions.andExpect(matcher);
      return this;
    }

    MvcResult andReturn() throws Exception {
      return resultActions.andReturn();
    }

    UUID andReturnId() throws Exception {
      MvcResult result = resultActions.andReturn();
      JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
      return UUID.fromString(json.get("id").asText());
    }
  }
}
