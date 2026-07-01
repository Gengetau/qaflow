package com.gengetau.qaflow.modules.attachments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

  @EntityGraph(attributePaths = {"project", "project.workspace", "defect", "testRunItem", "uploadedBy"})
  Optional<Attachment> findDetailedById(UUID id);

  @EntityGraph(attributePaths = {"project", "project.workspace", "defect", "testRunItem", "uploadedBy"})
  List<Attachment> findByDefect_IdOrderByCreatedAtAsc(UUID defectId);

  @EntityGraph(attributePaths = {"project", "project.workspace", "defect", "testRunItem", "uploadedBy"})
  List<Attachment> findByTestRunItem_IdOrderByCreatedAtAsc(UUID testRunItemId);
}
