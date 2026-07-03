# QAFlow

[![CI](https://github.com/Gengetau/qaflow/actions/workflows/ci.yml/badge.svg)](https://github.com/Gengetau/qaflow/actions/workflows/ci.yml)

## Project Overview

QAFlow is a full-stack QA management platform for organizing test cases, executing test runs, tracking defects, attaching evidence, and generating project quality reports.

## Technology Stack

It is built as a portfolio-grade internal QA tool, not a toy CRUD app. The target stack is Java 21 + Spring Boot, Vue 3 + TypeScript, PostgreSQL, Flyway, JWT/RBAC, Docker Compose, and automated quality gates.

## Current Status

The core workflow is implemented end to end: authentication, workspace roles, project and suite management, test cases with steps, test runs, failed-run defects, evidence attachments, dashboard/report APIs, generated OpenAPI client coverage, demo seed data, browser E2E tests, Docker Compose startup, CI, and the final v1.0 quality pass.

## What It Demonstrates

- Stateful QA workflows: planned/in-progress/completed test runs, run item results, and defect lifecycle transitions.
- Real authorization: `OWNER`, `TESTER`, and `VIEWER` roles are enforced in backend services, not only in the UI.
- Non-trivial persistence: workspace-scoped uniqueness, ordered suites/steps, cascades, attachment metadata, and Flyway migrations.
- Reporting: dashboard aggregates, latest run quality metrics, failed-case lists, linked defects, and HTML export.
- Operational polish: Java 21, Docker Compose, nginx-served frontend image, seeded demo profile, local smoke scripts, CI gates, and documented test strategy.
- Release readiness: the README includes the CI badge, all loop quality gates have passed locally, and the project is ready for the `v1.0.0` tag once the final CI run is green.

## Running Locally

Docker Compose uses built-in defaults, so copying `.env.example` is optional. Copy it when you want to change ports, credentials, CORS origins, or the frontend API URL.

```powershell
Copy-Item .env.example .env
docker compose up --build
```

The compose stack starts PostgreSQL, the Spring Boot API with the `dev,demo` profiles, and the built Vue app served by nginx.

Open the app at http://localhost:5173 and log in with `owner@example.com` / `password123`.

PowerShell helpers are also available:

```powershell
.\scripts\dev-up.ps1
.\scripts\dev-up.ps1 -Detached
.\scripts\dev-smoke.ps1
.\scripts\dev-down.ps1
```

For a quick reviewer pass:

```powershell
.\scripts\dev-up.ps1 -Detached
.\scripts\dev-smoke.ps1
```

Local backend:

```powershell
cd apps/api
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Local frontend:

```powershell
cd apps/web
pnpm install
pnpm dev
```

Default URLs:

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- OpenAPI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432

## Java 21 Setup

QAFlow targets Java 21. On Windows, install Eclipse Temurin 21 JDK and run backend commands with:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK --exact
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

Do not switch the project target to Java 24; `apps/api/pom.xml` stays on Java 21.

## Demo Accounts

Run the backend with the `demo` profile to seed a realistic demo workspace, storefront QA project, test cases, test runs, defects, and report/dashboard metrics. The seed is idempotent and skips if the demo owner already exists.

```powershell
cd apps/api
$env:SPRING_PROFILES_ACTIVE='dev,demo'
./mvnw.cmd spring-boot:run
```

Demo login accounts:

- `owner@example.com` / `password123`
- `tester@example.com` / `password123`
- `viewer@example.com` / `password123`

## Project Structure

```text
apps/
  api/      Spring Boot API
  web/      Vue 3 SPA
docs/       Architecture, API, data model, testing, engineering log
openapi/    Committed OpenAPI snapshot
scripts/    Local automation helpers
```

## Documentation

- [Architecture](docs/architecture.md)
- [API](docs/api.md)
- [Data model](docs/data-model.md)
- [Development](docs/development.md)
- [Testing](docs/testing.md)
- [Roadmap](docs/roadmap.md)
- [Screenshots](docs/screenshots.md)

## Main Workflows

- Register or log in.
- Create a workspace and project.
- Create suites, test cases, and test steps.
- Create a test run and execute run items.
- Convert failed run items into defects.
- Upload evidence attachments.
- Review dashboard metrics and export reports.

## API Contract Workflow

The backend exposes OpenAPI at `http://localhost:8080/v3/api-docs` and Swagger UI at `http://localhost:8080/swagger-ui.html`.

Refresh the committed OpenAPI snapshot after backend API changes:

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs -OutFile openapi/qaflow.openapi.json
```

Generate the TypeScript API client used by the frontend:

```powershell
cd apps/web
pnpm api:generate
pnpm typecheck
```

Generated client code lives in `apps/web/src/app/api/generated/`. Frontend service modules should prefer re-exporting or wrapping generated operations instead of duplicating request types.

## Roadmap

See [docs/roadmap.md](docs/roadmap.md) for the full project roadmap. Current completed slices include:

- Foundation: monorepo, health endpoint, PostgreSQL/Flyway, frontend shell, Docker Compose, CI.
- Identity and access: auth, refresh tokens, workspace membership, RBAC.
- QA assets: projects, suites, test cases, and ordered steps.
- Execution: test runs, item results, failed-item defect creation.
- Risk and reporting: defects, comments, attachments, dashboard, reports, OpenAPI client.
- Delivery polish: demo seed data, Playwright E2E, Docker/local DX, documentation.

## Quality Gates

```powershell
cd apps/api
./mvnw.cmd test
./mvnw.cmd verify

cd ../web
pnpm lint
pnpm typecheck
pnpm test
pnpm build

# Optional browser E2E gate
pnpm e2e:install
pnpm e2e

cd ../..
docker compose config
```

## Testcontainers Note

On this machine, Docker Desktop is reachable from the Docker CLI, but Testcontainers 1.21.0 cannot use the Windows named-pipe endpoint from Java and the migration test skips via `@Testcontainers(disabledWithoutDocker = true)`. On a compatible Docker environment, the migration test verifies the Flyway core schema tables.
