# Testing

QAFlow uses layered automated checks.

## Command Matrix

| Scope | Command | Notes |
| --- | --- | --- |
| Backend unit/integration | `cd apps/api; .\mvnw.cmd test` | Uses Java 21. Testcontainers migration test skips locally when Docker is not available to Java. |
| Backend package | `cd apps/api; .\mvnw.cmd verify` | Runs tests and builds the Spring Boot jar. |
| Frontend unit/components | `cd apps/web; pnpm test` | Vitest and Vue Test Utils. Playwright specs are excluded from Vitest. |
| Frontend type/lint | `cd apps/web; pnpm lint; pnpm typecheck` | Both currently run `vue-tsc --noEmit`. |
| Frontend build | `cd apps/web; pnpm build` | Runs `vue-tsc --noEmit` and Vite build. |
| Browser E2E | `cd apps/web; pnpm e2e` | Playwright with deterministic API route fixtures. |
| Compose config | `docker compose config` | CI validates compose syntax and interpolation. |
| Full-stack smoke | `.\scripts\dev-up.ps1 -Detached; .\scripts\dev-smoke.ps1` | Requires Docker Desktop. Checks API health, web root, and demo login. |

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

## CI

GitHub Actions runs three jobs on push and pull request:

- `backend`: Temurin Java 21 and Maven tests.
- `frontend`: pnpm install, lint, typecheck, Vitest, and build.
- `docker`: `docker compose config`.

The frontend job installs pnpm before `actions/setup-node` enables `cache: pnpm`, because setup-node expects pnpm to be available when resolving the cache store.
