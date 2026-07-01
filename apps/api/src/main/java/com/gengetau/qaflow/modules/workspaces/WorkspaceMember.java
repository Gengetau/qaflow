package com.gengetau.qaflow.modules.workspaces;

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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspace_members")
public class WorkspaceMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private Workspace workspace;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkspaceRole role;

  @Column(name = "joined_at", nullable = false)
  private OffsetDateTime joinedAt;

  protected WorkspaceMember() {}

  public WorkspaceMember(Workspace workspace, User user, WorkspaceRole role) {
    this.workspace = workspace;
    this.user = user;
    this.role = role;
  }

  @PrePersist
  void onCreate() {
    joinedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public WorkspaceRole getRole() {
    return role;
  }
}