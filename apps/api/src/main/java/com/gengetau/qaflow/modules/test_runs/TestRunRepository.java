package com.gengetau.qaflow.modules.test_runs;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestRunRepository extends JpaRepository<TestRun, UUID> {

  List<TestRun> findByProject_IdOrderByCreatedAtDesc(UUID projectId);

  @EntityGraph(attributePaths = {"project", "createdBy", "items", "items.testCase", "items.executedBy"})
  @Query("select distinct testRun from TestRun testRun where testRun.id = :id")
  Optional<TestRun> findDetailedById(@Param("id") UUID id);
}
