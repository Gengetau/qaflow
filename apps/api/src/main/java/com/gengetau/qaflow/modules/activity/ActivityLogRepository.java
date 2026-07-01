package com.gengetau.qaflow.modules.activity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {}
