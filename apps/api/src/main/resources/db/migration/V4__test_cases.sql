CREATE TABLE test_cases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
  suite_id UUID REFERENCES test_suites(id) ON DELETE SET NULL,
  case_key VARCHAR(80) NOT NULL,
  title VARCHAR(240) NOT NULL,
  description TEXT,
  preconditions TEXT,
  priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
  case_type VARCHAR(20) NOT NULL CHECK (case_type IN ('FUNCTIONAL', 'REGRESSION', 'SMOKE', 'EXPLORATORY')),
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'READY', 'DEPRECATED')),
  created_by UUID NOT NULL REFERENCES users(id),
  updated_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uk_test_cases_project_key UNIQUE(project_id, case_key)
);

CREATE INDEX idx_test_cases_project_status ON test_cases(project_id, status, priority);
CREATE INDEX idx_test_cases_project_suite ON test_cases(project_id, suite_id);

CREATE TABLE test_case_steps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  test_case_id UUID NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
  step_order INTEGER NOT NULL,
  action TEXT NOT NULL,
  expected_result TEXT NOT NULL
);

CREATE INDEX idx_test_case_steps_case_order ON test_case_steps(test_case_id, step_order);
