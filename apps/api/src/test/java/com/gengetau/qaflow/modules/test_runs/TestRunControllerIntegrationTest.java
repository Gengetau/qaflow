package com.gengetau.qaflow.modules.test_runs;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
      "spring.datasource.url=jdbc:h2:mem:qaflow_test_runs;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class TestRunControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void testRunWorkflowEnforcesTransitionsAndPersistsExecutionResults() throws Exception {
    RegisteredUser owner = register("owner-runs@example.com", "Run Owner", "Run Workspace");
    RegisteredUser tester = register("tester-runs@example.com", "Run Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-runs@example.com", "Run Viewer", "Viewer Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER").andExpect(status().isCreated());
    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER").andExpect(status().isCreated());

    UUID projectId =
        createProject(owner, owner.workspaceId(), "Checkout Quality", "RUN")
            .andExpect(status().isCreated())
            .andReturnId();
    UUID testCaseId =
        createTestCase(tester, projectId, "RUN-1").andExpect(status().isCreated()).andReturnId();

    createRun(viewer, projectId, testCaseId).andExpect(status().isForbidden());

    MvcResult createdRun =
        createRun(tester, projectId, testCaseId)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", not(blankOrNullString())))
            .andExpect(jsonPath("$.status").value("PLANNED"))
            .andExpect(jsonPath("$.items[0].testCaseId").value(testCaseId.toString()))
            .andExpect(jsonPath("$.items[0].result").value("UNTESTED"))
            .andReturn();
    JsonNode runJson = objectMapper.readTree(createdRun.getResponse().getContentAsString());
    UUID testRunId = UUID.fromString(runJson.get("id").asText());
    UUID itemId = UUID.fromString(runJson.get("items").get(0).get("id").asText());

    updateRunDetails(viewer, testRunId, "Viewer update", "Viewer should not write")
        .andExpect(status().isForbidden());

    updateRunDetails(tester, testRunId, "Checkout regression RC", "Nightly run")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Checkout regression RC"))
        .andExpect(jsonPath("$.description").value("Nightly run"));

    updateItemResult(tester, itemId, "PASSED", "Looks good")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("in progress")));

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/start", testRunId).header("Authorization", bearer(tester)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.startedAt", not(blankOrNullString())));

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/start", testRunId).header("Authorization", bearer(tester)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("PLANNED")));

    updateRunDetails(tester, testRunId, "Locked run", "Already started")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("PLANNED")));

    updateItemResult(tester, itemId, "PASSED", "Order completed")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("PASSED"))
        .andExpect(jsonPath("$.actualResult").value("Order completed"))
        .andExpect(jsonPath("$.executedAt", not(blankOrNullString())));

    mockMvc
        .perform(get("/api/test-runs/{testRunId}/items", testRunId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].result").value("PASSED"));

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/complete", testRunId).header("Authorization", bearer(tester)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.completedAt", not(blankOrNullString())));

    updateItemResult(tester, itemId, "FAILED", "Regression")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("in progress")));
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

  private ResultWithId createTestCase(RegisteredUser actor, UUID projectId, String caseKey) throws Exception {
    return new ResultWithId(
        mockMvc.perform(
            post("/api/projects/{projectId}/test-cases", projectId)
                .header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "caseKey": "%s",
                      "title": "Guest checkout succeeds",
                      "description": "Checkout happy path coverage",
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
                        .formatted(caseKey))));
  }

  private ResultActions createRun(RegisteredUser actor, UUID projectId, UUID testCaseId) throws Exception {
    return mockMvc.perform(
        post("/api/projects/{projectId}/test-runs", projectId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "name": "Checkout regression",
                  "description": "Release candidate run",
                  "testCaseIds": ["%s"]
                }
                """
                    .formatted(testCaseId)));
  }

  private ResultActions updateRunDetails(
      RegisteredUser actor, UUID testRunId, String name, String description) throws Exception {
    return mockMvc.perform(
        patch("/api/test-runs/{testRunId}", testRunId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "name": "%s",
                  "description": "%s"
                }
                """
                    .formatted(name, description)));
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
