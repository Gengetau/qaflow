package com.gengetau.qaflow.modules.attachments;

import com.gengetau.qaflow.modules.defects.Defect;
import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.test_runs.TestRunItem;
import com.gengetau.qaflow.modules.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "attachments")
public class Attachment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "defect_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Defect defect;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "test_run_item_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private TestRunItem testRunItem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "uploaded_by", nullable = false)
  private User uploadedBy;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 120)
  private String contentType;

  @Column(name = "storage_path", nullable = false, length = 1024)
  private String storagePath;

  @Column(name = "file_size", nullable = false)
  private long fileSize;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  protected Attachment() {}

  public Attachment(
      Project project,
      Defect defect,
      TestRunItem testRunItem,
      User uploadedBy,
      String fileName,
      String contentType,
      String storagePath,
      long fileSize) {
    this.project = project;
    this.defect = defect;
    this.testRunItem = testRunItem;
    this.uploadedBy = uploadedBy;
    this.fileName = fileName;
    this.contentType = contentType;
    this.storagePath = storagePath;
    this.fileSize = fileSize;
  }

  @PrePersist
  void onCreate() {
    createdAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public Project getProject() {
    return project;
  }

  public Defect getDefect() {
    return defect;
  }

  public TestRunItem getTestRunItem() {
    return testRunItem;
  }

  public User getUploadedBy() {
    return uploadedBy;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public long getFileSize() {
    return fileSize;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
