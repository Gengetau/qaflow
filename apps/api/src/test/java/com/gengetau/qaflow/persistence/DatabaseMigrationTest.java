package com.gengetau.qaflow.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class DatabaseMigrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.enabled", () -> true);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private DataSource dataSource;

  @Test
  void flywayCreatesCoreSchemaTables() throws Exception {
    assertThat(tableNames())
        .contains(
            "users",
            "workspaces",
            "workspace_members",
            "projects",
            "test_suites",
            "test_cases",
            "test_case_steps",
            "test_runs",
            "test_run_items",
            "defects",
            "defect_comments",
            "activity_logs");
  }

  private Set<String> tableNames() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metadata = connection.getMetaData();
      try (ResultSet tables = metadata.getTables(null, "public", "%", new String[] {"TABLE"})) {
        Set<String> names = new TreeSet<>();
        while (tables.next()) {
          names.add(tables.getString("TABLE_NAME"));
        }
        return names;
      }
    }
  }
}


