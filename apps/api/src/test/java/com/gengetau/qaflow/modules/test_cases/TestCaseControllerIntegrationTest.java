package com.gengetau.qaflow.modules.test_cases;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
      "spring.datasource.url=jdbc:h2:mem:qaflow_test_cases;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class TestCaseControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void testCaseWorkflowPersistsStepsAndEnforcesProjectRules() throws Exception {
    RegisteredUser owner = register("owner-cases@example.com", "Case Owner", "Case Workspace");
    RegisteredUser tester = register("tester-cases@example.com", "Case Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-cases@example.com", "Case Viewer", "Viewer Workspace");
    RegisteredUser outsider = register("outsider-cases@example.com", "Case Outsider", "Outsider Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER").andExpect(status().isCreated());
    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER").andExpect(status().isCreated());

    UUID projectId =
        createProject(owner, owner.workspaceId(), "Checkout Quality", "CHK")
            .andExpect(status().isCreated())
            .andReturnId();
    UUID suiteId =
        createSuite(tester, projectId, "Regression", 10).andExpect(status().isCreated()).andReturnId();

    UUID caseId =
        createTestCase(tester, projectId, suiteId, "CHK-1")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", not(blankOrNullString())))
            .andExpect(jsonPath("$.caseKey").value("CHK-1"))
            .andExpect(jsonPath("$.title").value("Guest checkout succeeds"))
            .andExpect(jsonPath("$.steps[0].stepOrder").value(1))
            .andExpect(jsonPath("$.steps[1].expectedResult").value("Order confirmation is shown"))
            .andReturnId();

    createTestCase(tester, projectId, suiteId, "CHK-1")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", containsString("Test case key already exists")));

    createTestCase(viewer, projectId, suiteId, "CHK-2").andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/projects/{projectId}/test-cases", projectId)
                .header("Authorization", bearer(owner))
                .param("query", "checkout")
                .param("status", "READY")
                .param("priority", "HIGH")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(caseId.toString()))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10));

    mockMvc
        .perform(get("/api/test-cases/{testCaseId}", caseId).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/test-cases/{testCaseId}", caseId)
                .header("Authorization", bearer(tester))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "suiteId": "%s",
                      "caseKey": "CHK-1",
                      "title": "Guest checkout with coupon succeeds",
                      "description": "Coupon-assisted checkout coverage",
                      "preconditions": "Cart has an eligible item",
                      "priority": "CRITICAL",
                      "type": "REGRESSION",
                      "status": "READY",
                      "steps": [
                        {
                          "stepOrder": 1,
                          "action": "Apply a coupon",
                          "expectedResult": "Discount is accepted"
                        }
                      ]
                    }
                    """
                        .formatted(suiteId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Guest checkout with coupon succeeds"))
        .andExpect(jsonPath("$.priority").value("CRITICAL"))
        .andExpect(jsonPath("$.steps.length()").value(1))
        .andExpect(jsonPath("$.steps[0].action").value("Apply a coupon"));

    mockMvc
        .perform(get("/api/test-cases/{testCaseId}", caseId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.steps[0].expectedResult").value("Discount is accepted"));

    mockMvc
        .perform(delete("/api/test-cases/{testCaseId}", caseId).header("Authorization", bearer(tester)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/test-cases/{testCaseId}", caseId).header("Authorization", bearer(owner)))
        .andExpect(status().isNotFound());
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

  private ResultWithId createSuite(RegisteredUser actor, UUID projectId, String name, int sortOrder)
      throws Exception {
    return new ResultWithId(
        mockMvc.perform(
            post("/api/projects/{projectId}/suites", projectId)
                .header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "%s",
                      "description": "Core quality coverage",
                      "sortOrder": %d
                    }
                    """
                        .formatted(name, sortOrder))));
  }

  private ResultWithId createTestCase(RegisteredUser actor, UUID projectId, UUID suiteId, String caseKey)
      throws Exception {
    return new ResultWithId(
        mockMvc.perform(
            post("/api/projects/{projectId}/test-cases", projectId)
                .header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "suiteId": "%s",
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
                        },
                        {
                          "stepOrder": 2,
                          "action": "Place the order",
                          "expectedResult": "Order confirmation is shown"
                        }
                      ]
                    }
                    """
                        .formatted(suiteId, caseKey))));
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

    UUID andReturnId() throws Exception {
      MvcResult result = resultActions.andReturn();
      JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
      return UUID.fromString(json.get("id").asText());
    }
  }
}
