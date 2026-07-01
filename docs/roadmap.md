# Roadmap

QAFlow is built in small verified loops. The current project has completed the v1.0 quality pass and is portfolio-ready.

## Completed

| Area | Status | Notes |
| --- | --- | --- |
| Foundation | Done | Monorepo, Java 21 backend, Vue frontend, PostgreSQL, Flyway, Docker Compose, CI. |
| Auth and RBAC | Done | Register/login/refresh/logout/me, hashed refresh tokens, workspace memberships, role checks. |
| Project assets | Done | Projects, suites, test cases, ordered steps, filters, and role-aware UI controls. |
| Execution | Done | Planned runs, run start/complete, item result updates, progress metrics. |
| Defects | Done | Project defects, failed-run-item defects, comments, lifecycle transitions. |
| Attachments | Done | Evidence uploads, metadata, content-type limits, authorized downloads. |
| Dashboard and reports | Done | Dashboard aggregates, report summary, run report, HTML export. |
| OpenAPI workflow | Done | Committed OpenAPI snapshot and generated TypeScript client coverage. |
| Demo data | Done | Idempotent `demo` profile with users, workspace, project, cases, runs, and defects. |
| E2E | Done | Playwright browser tests for core user flows with deterministic API fixtures. |
| Docker/local DX | Done | One-command compose stack, nginx web image, health checks, `.env.example`, smoke scripts. |
| Documentation | Done | README, architecture, API, data model, testing, screenshots placeholders, and roadmap. |
| v1.0 quality pass | Done | Full backend, frontend, Playwright, Docker smoke, secrets, dead-code, and docs checks completed. |
| Release readiness | Done | README CI badge is present and `v1.0.0` is ready to tag after the loop commit passes CI. |

## Later Ideas

- CI Playwright job backed by a full-stack service container setup.
- Role management UI for workspace members.
- Audit log timeline in the frontend.
- Report download artifacts instead of source preview only.
- More granular dashboards by suite, assignee, severity, and release window.
