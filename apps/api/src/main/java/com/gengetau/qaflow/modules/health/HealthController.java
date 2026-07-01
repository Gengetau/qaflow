package com.gengetau.qaflow.modules.health;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

  @GetMapping
  HealthResponse health() {
    return new HealthResponse("UP", "qaflow-api", OffsetDateTime.now());
  }

  record HealthResponse(String status, String service, OffsetDateTime checkedAt) {}
}
