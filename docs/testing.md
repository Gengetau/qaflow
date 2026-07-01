# Testing

QAFlow uses layered automated checks.

## Backend

- JUnit 5 unit tests for services.
- MockMvc controller tests.
- Testcontainers PostgreSQL tests for persistence and Flyway.
- Security/RBAC tests for role permissions.
- Workflow tests for register/login, project creation, test execution, defect creation, and dashboard updates.

## Frontend

- Vitest tests for stores and components.
- Vue Test Utils for page/component smoke checks.
- TypeScript type checks.
- Production build checks.

## E2E

Playwright covers browser-level user flows with the Vue dev server and deterministic API route fixtures:

- Login -> create project -> create test case.
- Create test run -> mark failed -> create defect.
- Transition defect -> verify dashboard/report.

Install the Chromium browser once, then run the E2E suite:

```powershell
cd apps/web
pnpm e2e:install
pnpm e2e
```

CI currently runs frontend lint, typecheck, Vitest, and build. Playwright remains a manual local browser gate. For Docker-backed full-stack smoke testing, start compose and run:

```powershell
.\scripts\dev-up.ps1 -Detached
.\scripts\dev-smoke.ps1
```
