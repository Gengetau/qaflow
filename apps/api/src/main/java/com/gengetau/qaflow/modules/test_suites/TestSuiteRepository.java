package com.gengetau.qaflow.modules.test_suites;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {
  List<TestSuite> findByProject_IdOrderBySortOrderAscNameAsc(UUID projectId);

  Optional<TestSuite> findByIdAndProject_Id(UUID suiteId, UUID projectId);
}
