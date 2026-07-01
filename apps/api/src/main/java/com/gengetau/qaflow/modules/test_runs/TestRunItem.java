package com.gengetau.qaflow.modules.test_runs;

import com.gengetau.qaflow.modules.test_cases.TestCase;
import com.gengetau.qaflow.modules.users.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_run_items")
public class TestRunItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_run_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private TestRun testRun;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_case_id", nullable = false)
  private TestCase testCase;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestRunItemResult result = TestRunItemResult.UNTESTED;

  @Column(name = "actual_result", columnDefinition = "TEXT")
  private String actualResult;

  @Column(name = "executed_at")
  private OffsetDateTime executedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "executed_by")
  private User executedBy;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected TestRunItem() {}

  public TestRunItem(TestCase testCase) {
    this.testCase = testCase;
  }

  void attachTo(TestRun testRun) {
    this.testRun = testRun;
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

  public void execute(TestRunItemResult result, String actualResult, User actor) {
    this.result = result;
    this.actualResult = actualResult;
    this.executedBy = actor;
    this.executedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public TestRun getTestRun() {
    return testRun;
  }

  public TestCase getTestCase() {
    return testCase;
  }

  public User getAssignee() {
    return assignee;
  }

  public TestRunItemResult getResult() {
    return result;
  }

  public String getActualResult() {
    return actualResult;
  }

  public OffsetDateTime getExecutedAt() {
    return executedAt;
  }

  public User getExecutedBy() {
    return executedBy;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
