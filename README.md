# QAFlow

QAFlow is a full-stack QA management platform for organizing test cases, executing test runs, tracking defects, attaching evidence, and generating project quality reports.

It is built as a portfolio-grade internal QA tool, not a toy CRUD app. The target stack is Java 21 + Spring Boot, Vue 3 + TypeScript, PostgreSQL, Flyway, JWT/RBAC, Docker Compose, and automated quality gates.

## Current Status

This repository is being built from the supplied `qaflow-design` and `qaflow-loop` documents. The first implementation slice establishes the monorepo, backend health endpoint, PostgreSQL/Flyway baseline, frontend shell, Docker Compose, CI, and documentation scaffolding.

## Quick Start

```powershell
docker compose up --build
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

Seed data will provide these accounts once the auth and seed loops are complete:

- `owner@example.com` / `password123`
- `tester@example.com` / `password123`
- `viewer@example.com` / `password123`

## Project Structure

```text
apps/
  api/      Spring Boot API
  web/      Vue 3 SPA
docs/       Architecture, API, data model, testing, engineering log
docker/     Runtime configuration
scripts/    Local automation helpers
```

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

The implementation follows the loop backlog in the supplied prompt:

- Scaffold, backend health, PostgreSQL/Flyway, frontend shell, CI
- Auth, refresh tokens, workspace membership, RBAC
- Projects, suites, test cases, and steps
- Test runs, execution workflow, defects, comments, attachments
- Dashboard, reports, OpenAPI client, seed data, E2E, Docker polish

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

cd ../..
docker compose config
```

## Testcontainers Note

On this machine, Docker Desktop is reachable from the Docker CLI, but Testcontainers 1.21.0 cannot use the Windows named-pipe endpoint from Java and the migration test skips via `@Testcontainers(disabledWithoutDocker = true)`. On a compatible Docker environment, the migration test verifies the Flyway core schema tables.
