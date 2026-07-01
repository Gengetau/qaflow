CREATE TABLE test_runs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  name VARCHAR(180) NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_test_runs_project_status ON test_runs(project_id, status, created_at DESC);

CREATE TABLE test_run_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  test_run_id UUID NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
  test_case_id UUID NOT NULL REFERENCES test_cases(id),
  assignee_id UUID REFERENCES users(id),
  result VARCHAR(20) NOT NULL DEFAULT 'UNTESTED' CHECK (result IN ('UNTESTED', 'PASSED', 'FAILED', 'BLOCKED', 'SKIPPED')),
  actual_result TEXT,
  executed_at TIMESTAMPTZ,
  executed_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_test_run_items_run_created ON test_run_items(test_run_id, created_at);
CREATE INDEX idx_test_run_items_case ON test_run_items(test_case_id);
