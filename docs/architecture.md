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

## Backend Layers

- Controller layer: request mapping, validation entrypoint, DTOs, HTTP semantics.
- Service layer: permission checks, workflow transitions, report aggregation, attachment authorization.
- Persistence layer: JPA entities, repositories, database constraints, query composition.
- Infrastructure layer: security, OpenAPI, file storage, exception mapping, configuration.

## Frontend Layers

- Pages: route-level views.
- Components: reusable UI surfaces such as badges, tables, editors, uploaders, and dialogs.
- Stores: Pinia state for auth, workspaces, projects, test cases, test runs, defects, and notifications.
- API client: typed REST boundary, eventually generated from OpenAPI.

## Runtime Services

- `postgres`: PostgreSQL database.
- `api`: Spring Boot backend on port 8080.
- `web`: Vite-built frontend on port 5173 in development.
