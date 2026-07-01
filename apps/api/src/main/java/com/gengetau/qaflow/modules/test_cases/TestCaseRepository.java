package com.gengetau.qaflow.modules.test_cases;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository
    extends JpaRepository<TestCase, UUID>, JpaSpecificationExecutor<TestCase> {

  boolean existsByProject_IdAndCaseKey(UUID projectId, String caseKey);

  boolean existsByProject_IdAndCaseKeyAndIdNot(UUID projectId, String caseKey, UUID id);

  @EntityGraph(attributePaths = {"steps", "suite", "project", "createdBy", "updatedBy"})
  @Query("select testCase from TestCase testCase where testCase.id = :id")
  Optional<TestCase> findDetailedById(@Param("id") UUID id);
}
