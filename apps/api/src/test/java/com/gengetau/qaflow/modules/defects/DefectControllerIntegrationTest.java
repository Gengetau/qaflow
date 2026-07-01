package com.gengetau.qaflow.modules.defects;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
      "spring.datasource.url=jdbc:h2:mem:qaflow_defects;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class DefectControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void defectWorkflowLinksFailedRunItemsTransitionsStateAndPersistsComments() throws Exception {
    RegisteredUser owner = register("owner-defects@example.com", "Defect Owner", "Defect Workspace");
    RegisteredUser tester = register("tester-defects@example.com", "Defect Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-defects@example.com", "Defect Viewer", "Viewer Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER").andExpect(status().isCreated());
    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER").andExpect(status().isCreated());

    UUID projectId =
        createProject(owner, owner.workspaceId(), "Checkout Quality", "DEF")
            .andExpect(status().isCreated())
            .andReturnId();
    UUID failedCaseId = createTestCase(tester, projectId, "DEF-1").andExpect(status().isCreated()).andReturnId();
    UUID untestedCaseId = createTestCase(tester, projectId, "DEF-2").andExpect(status().isCreated()).andReturnId();

    MvcResult createdRun =
        createRun(tester, projectId, failedCaseId, untestedCaseId)
            .andExpect(status().isCreated())
            .andReturn();
    JsonNode runJson = objectMapper.readTree(createdRun.getResponse().getContentAsString());
    UUID testRunId = UUID.fromString(runJson.get("id").asText());
    UUID failedItemId = itemIdByCaseKey(runJson, "DEF-1");
    UUID untestedItemId = itemIdByCaseKey(runJson, "DEF-2");

    mockMvc
        .perform(post("/api/test-runs/{testRunId}/start", testRunId).header("Authorization", bearer(tester)))
        .andExpect(status().isOk());
    updateItemResult(tester, failedItemId, "FAILED", "Payment declined")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("FAILED"));

    createDefectFromItem(tester, untestedItemId).andExpect(status().isBadRequest());
    createDefectFromItem(viewer, failedItemId).andExpect(status().isForbidden());

    MvcResult createdDefect =
        createDefectFromItem(tester, failedItemId)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", not(blankOrNullString())))
            .andExpect(jsonPath("$.projectId").value(projectId.toString()))
            .andExpect(jsonPath("$.testRunItemId").value(failedItemId.toString()))
            .andExpect(jsonPath("$.caseKey").value("DEF-1"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.severity").value("HIGH"))
            .andExpect(jsonPath("$.priority").value("URGENT"))
            .andReturn();
    UUID defectId =
        UUID.fromString(objectMapper.readTree(createdDefect.getResponse().getContentAsString()).get("id").asText());

    mockMvc
        .perform(get("/api/projects/{projectId}/defects", projectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Checkout payment failure"));

    updateDefect(viewer, defectId).andExpect(status().isForbidden());
    updateDefect(tester, defectId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Checkout payment failure on Visa"))
        .andExpect(jsonPath("$.severity").value("CRITICAL"));

    transitionDefect(tester, defectId, "CLOSED")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("transition")));

    transitionDefect(tester, defectId, "IN_PROGRESS").andExpect(status().isOk());
    transitionDefect(tester, defectId, "RESOLVED").andExpect(status().isOk());
    transitionDefect(tester, defectId, "CLOSED")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));

    addComment(tester, defectId, "Fixed in payment service patch")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.comments[0].body").value("Fixed in payment service patch"));

    mockMvc
        .perform(get("/api/defects/{defectId}", defectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.comments[0].body").value("Fixed in payment service patch"));
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

  private ResultActions createRun(RegisteredUser actor, UUID projectId, UUID firstCaseId, UUID secondCaseId)
      throws Exception {
    return mockMvc.perform(
        post("/api/projects/{projectId}/test-runs", projectId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "name": "Checkout regression",
                  "description": "Release candidate run",
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
                  "title": "Checkout payment failure",
                  "description": "Payment declined during checkout",
                  "severity": "HIGH",
                  "priority": "URGENT"
                }
                """));
  }

  private ResultActions updateDefect(RegisteredUser actor, UUID defectId) throws Exception {
    return mockMvc.perform(
        patch("/api/defects/{defectId}", defectId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "title": "Checkout payment failure on Visa",
                  "description": "Visa authorization fails during checkout",
                  "severity": "CRITICAL",
                  "priority": "HIGH"
                }
                """));
  }

  private ResultActions transitionDefect(RegisteredUser actor, UUID defectId, String status) throws Exception {
    return mockMvc.perform(
        post("/api/defects/{defectId}/transition", defectId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "status": "%s"
                }
                """
                    .formatted(status)));
  }

  private ResultActions addComment(RegisteredUser actor, UUID defectId, String body) throws Exception {
    return mockMvc.perform(
        post("/api/defects/{defectId}/comments", defectId)
            .header("Authorization", bearer(actor))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "body": "%s"
                }
                """
                    .formatted(body)));
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
