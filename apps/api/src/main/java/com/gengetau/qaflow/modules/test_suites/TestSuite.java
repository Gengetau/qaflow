package com.gengetau.qaflow.modules.test_suites;

import com.gengetau.qaflow.modules.projects.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_suites")
public class TestSuite {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Project project;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected TestSuite() {}

  public TestSuite(Project project, String name, String description, int sortOrder) {
    this.project = project;
    this.name = name;
    this.description = description;
    this.sortOrder = sortOrder;
  }

  @PrePersist
  void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public void update(String name, String description, int sortOrder) {
    this.name = name;
    this.description = description;
    this.sortOrder = sortOrder;
  }

  public UUID getId() {
    return id;
  }

  public Project getProject() {
    return project;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
