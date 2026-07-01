package com.gengetau.qaflow.modules.test_cases;

import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.test_suites.TestSuite;
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
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "test_cases",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_test_cases_project_key", columnNames = {"project_id", "case_key"})
    })
public class TestCase {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suite_id")
  private TestSuite suite;

  @Column(name = "case_key", nullable = false, length = 80)
  private String caseKey;

  @Column(nullable = false, length = 240)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(columnDefinition = "TEXT")
  private String preconditions;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestCasePriority priority;

  @Enumerated(EnumType.STRING)
  @Column(name = "case_type", nullable = false, length = 20)
  private TestCaseType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestCaseStatus status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "updated_by", nullable = false)
  private User updatedBy;

  @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("stepOrder ASC")
  private List<TestCaseStep> steps = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected TestCase() {}

  public TestCase(
      Project project,
      TestSuite suite,
      String caseKey,
      String title,
      String description,
      String preconditions,
      TestCasePriority priority,
      TestCaseType type,
      TestCaseStatus status,
      User actor) {
    this.project = project;
    this.suite = suite;
    this.caseKey = caseKey;
    this.title = title;
    this.description = description;
    this.preconditions = preconditions;
    this.priority = priority;
    this.type = type;
    this.status = status;
    this.createdBy = actor;
    this.updatedBy = actor;
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
      TestSuite suite,
      String caseKey,
      String title,
      String description,
      String preconditions,
      TestCasePriority priority,
      TestCaseType type,
      TestCaseStatus status,
      User actor) {
    this.suite = suite;
    this.caseKey = caseKey;
    this.title = title;
    this.description = description;
    this.preconditions = preconditions;
    this.priority = priority;
    this.type = type;
    this.status = status;
    this.updatedBy = actor;
  }

  public void replaceSteps(List<TestCaseStep> nextSteps) {
    steps.clear();
    nextSteps.forEach(this::addStep);
  }

  public void addStep(TestCaseStep step) {
    step.attachTo(this);
    steps.add(step);
  }

  public UUID getId() {
    return id;
  }

  public Project getProject() {
    return project;
  }

  public TestSuite getSuite() {
    return suite;
  }

  public String getCaseKey() {
    return caseKey;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getPreconditions() {
    return preconditions;
  }

  public TestCasePriority getPriority() {
    return priority;
  }

  public TestCaseType getType() {
    return type;
  }

  public TestCaseStatus getStatus() {
    return status;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public User getUpdatedBy() {
    return updatedBy;
  }

  public List<TestCaseStep> getSteps() {
    return steps;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
