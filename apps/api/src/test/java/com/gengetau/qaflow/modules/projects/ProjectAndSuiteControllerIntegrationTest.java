package com.gengetau.qaflow.modules.projects;

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
      "spring.datasource.url=jdbc:h2:mem:qaflow_projects;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class ProjectAndSuiteControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void projectAndSuiteApisEnforceWorkspaceRoles() throws Exception {
    RegisteredUser owner = register("owner-projects@example.com", "Project Owner", "Project Workspace");
    RegisteredUser tester = register("tester-projects@example.com", "Project Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-projects@example.com", "Project Viewer", "Viewer Workspace");
    RegisteredUser outsider = register("outsider-projects@example.com", "Project Outsider", "Outsider Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER").andExpect(status().isCreated());
    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER").andExpect(status().isCreated());

    UUID projectId =
        createProject(owner, owner.workspaceId(), "Checkout Quality", "CHK")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", not(blankOrNullString())))
            .andExpect(jsonPath("$.name").value("Checkout Quality"))
            .andExpect(jsonPath("$.key").value("CHK"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturnId();

    createProject(viewer, owner.workspaceId(), "Viewer Project", "VIEW").andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/projects")
                .header("Authorization", bearer(owner))
                .param("workspaceId", owner.workspaceId().toString())
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(projectId.toString()))
        .andExpect(jsonPath("$.totalItems").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(10));

    mockMvc
        .perform(get("/api/projects/{projectId}", projectId).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            patch("/api/projects/{projectId}", projectId)
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Checkout Quality Updated",
                      "description": "Updated regression scope",
                      "status": "ACTIVE"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Checkout Quality Updated"))
        .andExpect(jsonPath("$.description").value("Updated regression scope"));

    UUID suiteId =
        createSuite(tester, projectId, "Regression", 10)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", not(blankOrNullString())))
            .andExpect(jsonPath("$.name").value("Regression"))
            .andReturnId();

    createSuite(viewer, projectId, "Read Only", 20).andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/projects/{projectId}/suites", projectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(suiteId.toString()))
        .andExpect(jsonPath("$[0].name").value("Regression"));

    mockMvc
        .perform(
            patch("/api/suites/{suiteId}", suiteId)
                .header("Authorization", bearer(tester))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Regression Updated",
                      "description": "Updated core quality coverage",
                      "sortOrder": 5
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Regression Updated"))
        .andExpect(jsonPath("$.sortOrder").value(5));

    mockMvc
        .perform(delete("/api/suites/{suiteId}", suiteId).header("Authorization", bearer(viewer)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(delete("/api/suites/{suiteId}", suiteId).header("Authorization", bearer(tester)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/projects/{projectId}/suites", projectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    UUID projectToDelete =
        createProject(owner, owner.workspaceId(), "Temporary Project", "TMP")
            .andExpect(status().isCreated())
            .andReturnId();

    mockMvc
        .perform(delete("/api/projects/{projectId}", projectToDelete).header("Authorization", bearer(owner)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/projects/{projectId}", projectToDelete).header("Authorization", bearer(owner)))
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
