package com.gengetau.qaflow.modules.test_runs;

import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.users.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_runs")
public class TestRun {

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestRunStatus status = TestRunStatus.PLANNED;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<TestRunItem> items = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected TestRun() {}

  public TestRun(Project project, String name, String description, User createdBy) {
    this.project = project;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
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

  public void addItem(TestRunItem item) {
    item.attachTo(this);
    items.add(item);
  }

  public void updateDetails(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public void start() {
    status = TestRunStatus.IN_PROGRESS;
    startedAt = OffsetDateTime.now();
  }

  public void complete() {
    status = TestRunStatus.COMPLETED;
    completedAt = OffsetDateTime.now();
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

  public TestRunStatus getStatus() {
    return status;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public List<TestRunItem> getItems() {
    return items;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
