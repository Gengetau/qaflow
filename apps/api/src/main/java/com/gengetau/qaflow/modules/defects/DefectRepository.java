package com.gengetau.qaflow.modules.defects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DefectRepository extends JpaRepository<Defect, UUID> {

  @EntityGraph(
      attributePaths = {
        "project",
        "testRunItem",
        "testRunItem.testCase",
        "assignee",
        "reportedBy",
        "comments",
        "comments.author"
      })
  @Query(
      """
      select distinct defect
      from Defect defect
      where defect.project.id = :projectId
      order by defect.createdAt desc
      """)
  List<Defect> findDetailedByProjectIdOrderByCreatedAtDesc(@Param("projectId") UUID projectId);

  @EntityGraph(
      attributePaths = {
        "project",
        "testRunItem",
        "testRunItem.testCase",
        "assignee",
        "reportedBy",
        "comments",
        "comments.author"
      })
  @Query("select distinct defect from Defect defect where defect.id = :id")
  Optional<Defect> findDetailedById(@Param("id") UUID id);
}
