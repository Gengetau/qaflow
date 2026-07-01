package com.gengetau.qaflow.modules.activity;

import com.gengetau.qaflow.modules.projects.Project;
import com.gengetau.qaflow.modules.users.User;
import com.gengetau.qaflow.modules.workspaces.Workspace;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Column(name = "entity_type", nullable = false, length = 80)
  private String entityType;

  @Column(name = "entity_id")
  private UUID entityId;

  @Column(nullable = false, length = 80)
  private String action;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
  private String metadataJson = "{}";

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  protected ActivityLog() {}

  public ActivityLog(
      Workspace workspace, Project project, User actor, String entityType, UUID entityId, String action) {
    this.workspace = workspace;
    this.project = project;
    this.actor = actor;
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
  }

  @PrePersist
  void onCreate() {
    createdAt = OffsetDateTime.now();
  }
}
