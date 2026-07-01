# Engineering Log

## Loop 0: Repository Discovery

Implemented:
- Inspected the workspace and found only the two supplied markdown design/prompt files.
- Confirmed this directory is not currently a git repository.
- Identified local tooling: Maven 3.9.11, Docker 29.2.0, default Java 8, available JDK 24 at `D:\jdk24\jdk-24.0.2`, Node 22, pnpm 11.

Checks run:
- `Get-ChildItem -Force`
- `git status --short`
- `java -version`
- `mvn -version`
- `node -v`
- `pnpm -v`
- `docker --version`

Result:
- pass

Notes:
- Backend checks must set `JAVA_HOME=D:\jdk24\jdk-24.0.2` because default `java` is Java 8.
- No git commits can be created until the workspace is initialized as a repository.

Next loop:
- Create monorepo scaffold and the first runnable backend/frontend slices.

## Loop 1: Monorepo Scaffold

Implemented:
- Added root README, `.gitignore`, docs skeleton, Docker Compose, CI workflow, and app directories.
- Documented the target architecture, API surface, data model, testing strategy, and loop log.

Checks run:
- `docker compose config`

Result:
- pass

Notes:
- Docker Compose validates the `postgres`, `api`, and `web` service definitions.

Next loop:
- Add Spring Boot API scaffold and health endpoint.

## Loop 2: Spring Boot API Scaffold

Implemented:
- Added Spring Boot 3.5 API under `apps/api` with Java 21 target.
- Added Maven Wrapper pinned to Maven 3.9.11.
- Added `/api/health`, OpenAPI metadata, CORS config, stateless security baseline, and API error shape.
- Added a MockMvc health endpoint test.

Checks run:
- Red: `mvn -Dtest=HealthEndpointTest test` failed with 404 before the endpoint existed.
- Green: `mvn -Dtest=HealthEndpointTest test` passed after adding the endpoint.
- `./mvnw.cmd -version`
- `./mvnw.cmd test`

Result:
- pass

Notes:
- Default Java remains Java 8, so local Maven commands need `JAVA_HOME=D:\jdk24\jdk-24.0.2`.

Next loop:
- Add PostgreSQL and Flyway foundation.

## Loop 3: PostgreSQL and Flyway Foundation

Implemented:
- Added PostgreSQL/Flyway/JPA/Testcontainers dependencies.
- Added application datasource and JPA validate configuration.
- Added `V1__init_schema.sql` for users, workspaces, workspace members, projects, and activity logs.
- Added a Testcontainers-backed migration test that verifies core tables when Docker is available to Java.

Checks run:
- `docker info`
- `docker context ls`
- `mvn -Dtest=DatabaseMigrationTest test`
- `./mvnw.cmd test`

Result:
- partial

Notes:
- Docker Desktop starts and the Docker CLI can reach server `29.2.0`.
- Testcontainers 1.21.0 cannot use this Windows Docker Desktop named-pipe environment from Java; it receives a 400 response with empty Docker info fields.
- The migration test uses `@Testcontainers(disabledWithoutDocker = true)`, so it runs in compatible CI/local Docker environments and skips in this one.

Next loop:
- Add Vue frontend scaffold.

## Loop 4: Vue Frontend Scaffold

Implemented:
- Added Vue 3 + TypeScript + Vite app under `apps/web`.
- Added Pinia auth store, Vue Router routes, app layout, dashboard, projects, test cases, test runs, defects, reports, settings, login, and register pages.
- Added a Vitest smoke test for the product shell.
- Added responsive CSS for the SaaS-style QA management surface.

Checks run:
- Red: `pnpm test` failed because `App.vue` did not exist.
- Config red: `pnpm test` failed until `vite.config.ts` loaded `@vitejs/plugin-vue`.
- `pnpm lint`
- `pnpm typecheck`
- `pnpm test`
- `pnpm build`

Result:
- pass

Notes:
- Local Node is 22, while CI targets Node 24. The frontend package allows Node `>=22 <25` so local checks can run while remaining compatible with Node 24.

Next loop:
- Add CI foundation and continue toward auth/RBAC.

## Loop 5: CI Foundation

Implemented:
- Added GitHub Actions workflow for backend tests, frontend lint/typecheck/test/build, and Docker Compose config validation.
- Added Dockerfiles for API and web services.
- Added Docker Compose service wiring for PostgreSQL, API, and web.

Checks run:
- `./mvnw.cmd test`
- `pnpm lint`
- `pnpm test`
- `pnpm build`
- `docker compose config`

Result:
- partial

Notes:
- Local checks pass, with the Testcontainers migration test skipped because of the Java/Docker Desktop named-pipe incompatibility described in Loop 3.
- Git commits were skipped because this workspace is not a git repository.

Next loop:
- Loop 6: Auth Backend.
## Loop 6: Auth Backend

Implemented:
- Installed Eclipse Temurin 21 JDK and verified `java -version` / `javac -version` with Java 21.
- Added Java 21 setup notes to README and `docs/development.md`.
- Added User, Workspace, WorkspaceMember, and RefreshToken JPA models and repositories.
- Added registration, login, refresh token rotation, logout token revocation, and `/api/auth/me`.
- Added JWT access-token issuing and a Bearer-token authentication filter.
- Added `V2__auth_refresh_tokens.sql` for refresh token persistence.
- Added Auth integration tests covering unauthenticated `/me`, register, authenticated `/me`, login, and refresh.

Checks run:
- Red: `./mvnw.cmd -Dtest=AuthControllerIntegrationTest test` failed before implementation with 403 responses.
- Green: `./mvnw.cmd -Dtest=AuthControllerIntegrationTest test` passed after implementation.
- `java -version`
- `javac -version`
- `./mvnw.cmd test`
- `./mvnw.cmd verify`

Result:
- pass

Notes:
- Commands were run with `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`.
- Testcontainers still cannot access Docker Desktop from Java in this Windows named-pipe environment, so `DatabaseMigrationTest` skips via `disabledWithoutDocker`.

Next loop:
- Loop 7: RBAC and Security.

## Loop 7: RBAC and Security

Implemented:
- Added reusable `PermissionService` for workspace membership checks, owner-only workspace management, and owner/tester test-artifact write authorization.
- Added workspace listing, creation, detail, member add, member role update, and member removal APIs.
- Enforced OWNER / TESTER / VIEWER backend role rules for workspace member management and read access.
- Added uniform `ResponseStatusException` mapping to the existing `ApiError` response shape.
- Added RBAC integration coverage for owner member management, viewer write denial, non-member denial, and tester test-artifact permission.

Checks run:
- Red: `./mvnw.cmd -Dtest=RbacSecurityIntegrationTest test` failed before implementation because `PermissionService` and workspace APIs did not exist.
- Green: `./mvnw.cmd -Dtest=RbacSecurityIntegrationTest test`
- `./mvnw.cmd test`
- `./mvnw.cmd verify`

Result:
- pass

Notes:
- Backend checks were run with Java 21 via `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`.
- Testcontainers still cannot access Docker Desktop from Java in this Windows named-pipe environment, so `DatabaseMigrationTest` remains skipped via `disabledWithoutDocker`.
- Local untracked diagnostic log files were left untouched and are not part of this loop.

Next loop:
- Loop 8: Auth Frontend.

## Loop 8: Auth Frontend

Implemented:
- Added a frontend auth API client for login, registration, and logout token revocation.
- Reworked the Pinia auth store to persist access token, refresh token, current user, active workspace, and role in `localStorage`.
- Added route guards so protected `/app/*` routes redirect unauthenticated users to `/auth/login` and authenticated users see the app shell.
- Converted login and registration pages from static forms into real submit flows.
- Updated the app shell to display authenticated workspace, user, and role state.
- Added a Vite `/api` dev proxy to the Spring Boot backend and documented frontend auth development.

Checks run:
- Red: `pnpm test` failed before implementation because `login`, `restoreSession`, and `createQaflowRouter` did not exist.
- Green: `pnpm test`
- `pnpm lint`
- `pnpm typecheck`
- `pnpm build`

Result:
- pass

Notes:
- Frontend auth uses `/api` by default. In local Vite development, requests are proxied to `http://localhost:8080`.

Next loop:
- Loop 9: Projects and Test Suites Backend.

## Loop 9: Projects and Test Suites Backend

Implemented:
- Added Project and TestSuite JPA models, repositories, DTOs, services, and REST controllers.
- Added paginated project listing with a shared `PageResponse` shape.
- Added project membership authorization: workspace members can read, OWNER can manage projects, and OWNER/TESTER can manage suites.
- Added activity log creation for project and suite create/update/delete actions.
- Added `V3__test_management.sql` for test suites and kept project-related cascade delete behavior aligned between Flyway and Hibernate-generated test schemas.

Checks run:
- Red: `./mvnw.cmd -Dtest=ProjectAndSuiteControllerIntegrationTest test` failed before project APIs existed.
- Red: project list pagination assertion failed while the API still returned a bare array.
- Green: `./mvnw.cmd -Dtest=ProjectAndSuiteControllerIntegrationTest test`
- `./mvnw.cmd test`
- `./mvnw.cmd verify`

Result:
- pass

Notes:
- Backend checks were run with Java 21 via `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`.
- Testcontainers still cannot access Docker Desktop from Java in this Windows named-pipe environment, so `DatabaseMigrationTest` remains skipped locally via `disabledWithoutDocker`.

Next loop:
- Loop 10: Projects and Test Suites Frontend.

## Loop 10: Projects and Test Suites Frontend

Implemented:
- Added a project and suite API client for authenticated `/api/projects` and `/api/suites` calls.
- Replaced the static projects page with a workspace-scoped project list, project detail panel, project create/edit/delete dialog flow, and suite create/edit/delete management UI.
- Added loading, error, and empty states for projects and suites.
- Added frontend tests covering project/suite loading and creating a project from the page dialog.

Checks run:
- Red: `pnpm exec vitest run src/pages/ProjectsPage.spec.ts` failed while the page still rendered static sample data and never called the API.
- Green: `pnpm exec vitest run src/pages/ProjectsPage.spec.ts`
- `pnpm test`
- `pnpm lint`
- `pnpm typecheck`
- `pnpm build`

Result:
- pass

Notes:
- Frontend checks were run under the local Windows Node 22 environment. The GitHub workflow still validates the same app under Node 24.

Next loop:
- Loop 11: Test Cases Backend.

## Loop 11: Test Cases Backend

Implemented:
- Added TestCase and TestCaseStep JPA models with priority, type, and status enums.
- Added test case create, list, detail, update, and delete APIs.
- Added ordered step persistence through create/update payloads.
- Added filtering and pagination for project-scoped test case lists.
- Enforced unique test case keys per project and project membership/write authorization.
- Added `V4__test_cases.sql` for test cases and steps.

Checks run:
- Red: `./mvnw.cmd -Dtest=TestCaseControllerIntegrationTest test` failed with 404 before the test case APIs existed.
- Green: `./mvnw.cmd -Dtest=TestCaseControllerIntegrationTest test`
- `./mvnw.cmd test`
- `./mvnw.cmd verify`

Result:
- pass

Notes:
- Backend checks were run with Java 21 via `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`.
- Testcontainers still cannot access Docker Desktop from Java in this Windows named-pipe environment, so `DatabaseMigrationTest` remains skipped locally via `disabledWithoutDocker`.

Next loop:
- Loop 12: Test Cases Frontend.
