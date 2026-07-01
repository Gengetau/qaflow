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