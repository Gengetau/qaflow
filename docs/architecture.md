# Architecture

QAFlow is a monorepo with a Spring Boot API, Vue SPA, PostgreSQL database, local file storage, and Docker Compose local runtime.

```text
Browser
  -> Vue 3 SPA
  -> REST API
  -> Spring Boot service layer
  -> PostgreSQL via JPA/Flyway
  -> local file storage for evidence attachments
```

## Why This Is Not Toy CRUD

QAFlow models a real QA operating workflow rather than independent create/read/update/delete screens:

- Test runs have lifecycle rules: `PLANNED -> IN_PROGRESS -> COMPLETED`.
- Test run items can only be executed while the parent run is in progress.
- Defects can be created from failed run items and then move through a constrained lifecycle.
- Reports aggregate multiple domains: cases, runs, item results, and linked defects.
- Every protected resource is scoped through workspace membership and role permissions.
- Attachments are authorized through project/workspace ownership and served through the API rather than direct file paths.

## Backend Layers

- Controller layer: request mapping, validation entrypoint, DTOs, HTTP semantics.
- Service layer: permission checks, workflow transitions, report aggregation, attachment authorization.
- Persistence layer: JPA entities, repositories, database constraints, query composition.
- Infrastructure layer: security, OpenAPI, file storage, exception mapping, configuration.

## Backend Modules

The API uses module-oriented packages under `com.gengetau.qaflow.modules`:

| Module | Responsibility |
| --- | --- |
| `auth` | Registration, login, refresh token rotation, logout, current user response. |
| `workspaces` | Workspace creation, member management, role checks. |
| `projects` | Project lifecycle and workspace-scoped project keys. |
| `test_suites` | Ordered grouping for test cases. |
| `test_cases` | Case metadata, status, priority/type, ordered steps. |
| `test_runs` | Run planning, execution state, item result updates. |
| `defects` | Defect creation, failed item linkage, comments, transitions. |
| `attachments` | Evidence upload metadata, file validation, authorized downloads. |
| `reports` | Dashboard aggregates, report summary, run report, HTML export. |
| `health` | Public health endpoint for compose and smoke checks. |

Controllers keep HTTP mapping thin. Services own permissions and workflow rules. Repositories stay focused on persistence queries.

## Frontend Layers

- Pages: route-level views.
- Components: reusable UI surfaces such as badges, tables, editors, uploaders, and dialogs.
- Stores: Pinia state for auth, workspaces, projects, test cases, test runs, defects, and notifications.
- API client: typed REST boundary with generated OpenAPI client coverage for reporting APIs.

The frontend is a working internal tool surface: project lists, editors, execution controls, kanban-style defect state, dashboard panels, and report preview are available without a marketing landing page.

## Runtime Services

- `postgres`: PostgreSQL database.
- `api`: Spring Boot backend on port 8080, with `/api/health` health checks.
- `web`: Vite-built frontend served by nginx on port 5173.

`docker-compose.yml` starts `postgres`, waits for database health before starting `api`, waits for API health before starting `web`, and seeds demo data through the `dev,demo` Spring profiles.

## Security Model

- JWT access tokens are sent with `Authorization: Bearer <token>`.
- Refresh tokens are persisted server-side as SHA-256 hashes and rotated through the refresh endpoint.
- Workspace roles are `OWNER`, `TESTER`, and `VIEWER`.
- Backend services verify workspace membership and role permissions before protected reads or writes.
- CORS origins are configurable through `QAFLOW_CORS_ALLOWED_ORIGINS` and default to local web origins.

## Data Boundaries

- PostgreSQL owns durable relational state and Flyway owns schema evolution.
- Attachment binary files live under `qaflow.upload-root`; the database stores sanitized metadata and relative storage paths.
- The frontend never receives raw storage paths. Evidence downloads go through authorized API endpoints.
- Demo data is seeded idempotently and only when the `demo` Spring profile is active.
