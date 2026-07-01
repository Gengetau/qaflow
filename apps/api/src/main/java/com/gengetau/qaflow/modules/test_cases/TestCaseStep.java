package com.gengetau.qaflow.modules.test_cases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "test_case_steps")
public class TestCaseStep {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_case_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private TestCase testCase;

  @Column(name = "step_order", nullable = false)
  private int stepOrder;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String action;

  @Column(name = "expected_result", nullable = false, columnDefinition = "TEXT")
  private String expectedResult;

  protected TestCaseStep() {}

  public TestCaseStep(int stepOrder, String action, String expectedResult) {
    this.stepOrder = stepOrder;
    this.action = action;
    this.expectedResult = expectedResult;
  }

  void attachTo(TestCase testCase) {
    this.testCase = testCase;
  }

  public UUID getId() {
    return id;
  }

  public int getStepOrder() {
    return stepOrder;
  }

  public String getAction() {
    return action;
  }

  public String getExpectedResult() {
    return expectedResult;
  }
}
