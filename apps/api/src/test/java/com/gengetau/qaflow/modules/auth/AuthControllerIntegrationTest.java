package com.gengetau.qaflow.modules.auth;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:qaflow_auth;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void meRequiresBearerToken() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void registerLoginRefreshAndMeWorkflow() throws Exception {
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "owner@example.com",
                          "password": "password123",
                          "displayName": "QA Owner",
                          "workspaceName": "Acme Quality Lab"
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
            .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
            .andExpect(jsonPath("$.user.email").value("owner@example.com"))
            .andExpect(jsonPath("$.user.displayName").value("QA Owner"))
            .andExpect(jsonPath("$.workspaces[0].role").value("OWNER"))
            .andReturn();

    JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
    String registerAccessToken = registerJson.get("accessToken").asText();
    String registerRefreshToken = registerJson.get("refreshToken").asText();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + registerAccessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("owner@example.com"))
        .andExpect(jsonPath("$.displayName").value("QA Owner"))
        .andExpect(jsonPath("$.workspaces[0].role").value("OWNER"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "OWNER@example.com",
                      "password": "password123"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
        .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
        .andExpect(jsonPath("$.user.email").value("owner@example.com"));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + registerRefreshToken + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
        .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
        .andExpect(jsonPath("$.user.email").value("owner@example.com"));
  }
}