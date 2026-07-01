package com.gengetau.qaflow.modules.workspaces;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gengetau.qaflow.security.CurrentUser;
import com.gengetau.qaflow.security.PermissionService;
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
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:qaflow_rbac;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class RbacSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PermissionService permissionService;

  @Test
  void workspaceRolesControlReadWriteAndTestArtifactPermissions() throws Exception {
    RegisteredUser owner = register("owner-rbac@example.com", "QA Owner", "Owner Workspace");
    RegisteredUser tester = register("tester-rbac@example.com", "QA Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-rbac@example.com", "QA Viewer", "Viewer Workspace");
    RegisteredUser outsider = register("outsider-rbac@example.com", "QA Outsider", "Outsider Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", not(blankOrNullString())))
        .andExpect(jsonPath("$.user.email").value(tester.email()))
        .andExpect(jsonPath("$.role").value("TESTER"));

    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.user.email").value(viewer.email()))
        .andExpect(jsonPath("$.role").value("VIEWER"));

    mockMvc
        .perform(get("/api/workspaces/{workspaceId}", owner.workspaceId()).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(owner.workspaceId().toString()))
        .andExpect(jsonPath("$.role").value("VIEWER"));

    addMember(viewer, owner.workspaceId(), outsider.email(), "VIEWER").andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/workspaces/{workspaceId}", owner.workspaceId()).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden());

    assertDoesNotThrow(
        () ->
            permissionService.requireTestArtifactWrite(
                owner.workspaceId(), new CurrentUser(tester.userId(), tester.email(), tester.displayName())));
    assertThrows(
        ResponseStatusException.class,
        () ->
            permissionService.requireTestArtifactWrite(
                owner.workspaceId(), new CurrentUser(viewer.userId(), viewer.email(), viewer.displayName())));
  }

  private ResultActions addMember(
      RegisteredUser actor, UUID workspaceId, String email, String role) throws Exception {
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
        UUID.fromString(json.get("user").get("id").asText()),
        email,
        displayName,
        json.get("accessToken").asText(),
        UUID.fromString(json.get("workspaces").get(0).get("id").asText()));
  }

  private String bearer(RegisteredUser user) {
    return "Bearer " + user.accessToken();
  }

  private record RegisteredUser(
      UUID userId, String email, String displayName, String accessToken, UUID workspaceId) {}
}
