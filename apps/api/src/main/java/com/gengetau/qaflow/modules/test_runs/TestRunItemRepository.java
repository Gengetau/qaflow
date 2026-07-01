package com.gengetau.qaflow.modules.test_runs;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestRunItemRepository extends JpaRepository<TestRunItem, UUID> {

  @EntityGraph(attributePaths = {"testRun", "testRun.project", "testCase", "executedBy"})
  @Query("select item from TestRunItem item where item.id = :id")
  Optional<TestRunItem> findDetailedById(@Param("id") UUID id);

  @EntityGraph(attributePaths = {"testCase", "executedBy"})
  List<TestRunItem> findByTestRun_IdOrderByCreatedAtAsc(UUID testRunId);
}
