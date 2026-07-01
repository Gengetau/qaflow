package com.gengetau.qaflow.modules.attachments;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileSystemUtils;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:qaflow_attachments;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "qaflow.upload-root=target/test-uploads",
      "qaflow.security.jwt-secret=test-secret-test-secret-test-secret-test-secret-test-secret",
      "qaflow.security.access-token-minutes=15",
      "qaflow.security.refresh-token-days=7"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttachmentControllerIntegrationTest {

  private static final Path TEST_UPLOAD_ROOT = Path.of("target/test-uploads");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @AfterEach
  void cleanUploads() {
    FileSystemUtils.deleteRecursively(TEST_UPLOAD_ROOT.toFile());
  }

  @Test
  void uploadsEvidenceValidatesFilesAndBlocksUnauthorizedDownload() throws Exception {
    RegisteredUser owner = register("owner-attachments@example.com", "Attachment Owner", "Attachment Workspace");
    RegisteredUser tester = register("tester-attachments@example.com", "Attachment Tester", "Tester Workspace");
    RegisteredUser viewer = register("viewer-attachments@example.com", "Attachment Viewer", "Viewer Workspace");
    RegisteredUser outsider = register("outsider-attachments@example.com", "Attachment Outsider", "Outsider Workspace");

    addMember(owner, owner.workspaceId(), tester.email(), "TESTER").andExpect(status().isCreated());
    addMember(owner, owner.workspaceId(), viewer.email(), "VIEWER").andExpect(status().isCreated());

    UUID projectId =
        createProject(owner, owner.workspaceId(), "Checkout Evidence", "ATT")
            .andExpect(status().isCreated())
            .andReturnId();
    UUID defectId = createDefect(tester, projectId).andExpect(status().isCreated()).andReturnId();

    uploadDefectAttachment(viewer, projectId, defectId, "viewer-note.txt", "text/plain", "viewer")
        .andExpect(status().isForbidden());
    uploadDefectAttachment(tester, projectId, defectId, "script.exe", "application/x-msdownload", "not allowed")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("File type")));

    MvcResult uploaded =
        uploadDefectAttachment(
                tester, projectId, defectId, "../payment failure.txt", "text/plain", "gateway timeout")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", not(blankOrNullString())))
            .andExpect(jsonPath("$.projectId").value(projectId.toString()))
            .andExpect(jsonPath("$.defectId").value(defectId.toString()))
            .andExpect(jsonPath("$.fileName").value("payment failure.txt"))
            .andExpect(jsonPath("$.contentType").value("text/plain"))
            .andExpect(jsonPath("$.fileSize").value(15))
            .andReturn();
    UUID attachmentId =
        UUID.fromString(objectMapper.readTree(uploaded.getResponse().getContentAsString()).get("id").asText());

    mockMvc
        .perform(get("/api/defects/{defectId}/attachments", defectId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(attachmentId.toString()))
        .andExpect(jsonPath("$[0].fileName").value("payment failure.txt"));

    mockMvc
        .perform(get("/api/attachments/{attachmentId}/download", attachmentId).header("Authorization", bearer(outsider)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/attachments/{attachmentId}/download", attachmentId).header("Authorization", bearer(viewer)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("payment failure.txt")))
        .andExpect(content().string("gateway timeout"));
  }

  private ResultActions uploadDefectAttachment(
      RegisteredUser actor, UUID projectId, UUID defectId, String fileName, String contentType, String body)
      throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", fileName, contentType, body.getBytes(StandardCharsets.UTF_8));
    return mockMvc.perform(
        multipart("/api/attachments")
            .file(file)
            .param("projectId", projectId.toString())
            .param("defectId", defectId.toString())
            .header("Authorization", bearer(actor)));
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

  private ResultWithId createDefect(RegisteredUser actor, UUID projectId) throws Exception {
    return new ResultWithId(
        mockMvc.perform(
            post("/api/projects/{projectId}/defects", projectId)
                .header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title": "Checkout payment failure",
                      "description": "Payment gateway timeout",
                      "severity": "HIGH",
                      "priority": "URGENT"
                    }
                    """)));
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
