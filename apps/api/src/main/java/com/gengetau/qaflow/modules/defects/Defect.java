package com.gengetau.qaflow.modules.defects;

import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.test_runs.TestRunItem;
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
import jakarta.persistence.OrderBy;
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
@Table(name = "defects")
public class Defect {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "test_run_item_id")
  private TestRunItem testRunItem;

  @Column(nullable = false, length = 240)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DefectSeverity severity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DefectPriority priority;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DefectStatus status = DefectStatus.OPEN;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reported_by", nullable = false)
  private User reportedBy;

  @OneToMany(mappedBy = "defect", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("createdAt ASC")
  private List<DefectComment> comments = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Defect() {}

  public Defect(
      Project project,
      TestRunItem testRunItem,
      String title,
      String description,
      DefectSeverity severity,
      DefectPriority priority,
      User assignee,
      User reportedBy) {
    this.project = project;
    this.testRunItem = testRunItem;
    this.title = title;
    this.description = description;
    this.severity = severity;
    this.priority = priority;
    this.assignee = assignee;
    this.reportedBy = reportedBy;
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

  public void update(
      String title,
      String description,
      DefectSeverity severity,
      DefectPriority priority,
      User assignee) {
    this.title = title;
    this.description = description;
    this.severity = severity;
    this.priority = priority;
    this.assignee = assignee;
  }

  public void transitionTo(DefectStatus status) {
    this.status = status;
  }

  public void addComment(DefectComment comment) {
    comment.attachTo(this);
    comments.add(comment);
  }

  public UUID getId() {
    return id;
  }

  public Project getProject() {
    return project;
  }

  public TestRunItem getTestRunItem() {
    return testRunItem;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public DefectSeverity getSeverity() {
    return severity;
  }

  public DefectPriority getPriority() {
    return priority;
  }

  public DefectStatus getStatus() {
    return status;
  }

  public User getAssignee() {
    return assignee;
  }

  public User getReportedBy() {
    return reportedBy;
  }

  public List<DefectComment> getComments() {
    return comments;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
