# Codex Continuous Loop Engineering Prompt: QAFlow

You are Codex working inside this repository.

Your mission is to build **QAFlow**, a full-stack QA management web application.

QAFlow is a portfolio-grade full-stack project, not a toy CRUD app.

It should demonstrate:

- Java 21 + Spring Boot backend engineering
- Vue 3 + TypeScript frontend engineering
- PostgreSQL relational data modeling
- Flyway migrations
- JWT authentication
- role-based access control
- test case management
- test run execution workflow
- defect tracking
- evidence attachment handling
- dashboard and report surfaces
- Docker Compose local development
- CI quality gates
- automated tests
- clean documentation

The project should feel like a small but real internal QA platform, similar to a lightweight TestRail + defect board.

---

## Operating Mode: Continuous Engineering Loop

Work in repeated implementation loops.

Do **not** stop after one loop unless blocked.

After each loop:

1. summarize the loop briefly
2. run relevant checks
3. fix failures
4. commit the work if git is available
5. update docs or backlog when needed
6. immediately proceed to the next smallest valuable loop

Only stop when:

- all v1.0 milestone acceptance criteria are complete
- you hit a real blocker that requires user input
- the environment cannot perform required actions
- the repository enters a state that risks data loss
- tests cannot be made green after reasonable debugging

If you are blocked, produce a clear blocked report with evidence.

Otherwise, keep moving.

---

## Loop Discipline

Each loop must follow this cycle:

```text
Inspect -> Plan -> Implement -> Test -> Fix -> Document -> Commit -> Continue
```

For every loop:

- inspect existing files first
- avoid overwriting user changes
- make a small, reviewable implementation plan
- implement only the next coherent slice
- add or update tests
- run the smallest useful checks first
- run broader checks before committing
- update README/docs if user-facing behavior changed
- use Conventional Commits
- continue to the next loop

Do not attempt to finish the whole project in one giant pass.

------

## Required Loop Summary Format

At the end of each loop, append a short summary to `docs/engineering-log.md`.

Use this format:

```
## Loop <N>: <short name>

Implemented:
- ...

Checks run:
- ...

Result:
- pass / partial / blocked

Notes:
- ...

Next loop:
- ...
```

Also print the same summary in your final response for that loop.

Then continue with the next loop unless blocked.

------

## Git Rules

Use trunk-based development.

If git is available:

- keep `main` runnable
- create commits after coherent loops
- use Conventional Commits
- do not rewrite history
- do not force push
- do not commit secrets
- do not commit local database files
- do not commit uploaded attachments
- do not commit `node_modules`, `target`, or build artifacts

Commit examples:

```
chore: scaffold qaflow monorepo
feat(api): add auth and workspace models
feat(web): add authenticated app shell
test(api): cover test run execution workflow
docs: document local development setup
ci: add backend and frontend quality gates
```

Use release tags only when a milestone is clearly complete:

```
v0.1.0 scaffold + auth + RBAC
v0.2.0 projects + test suites + test cases
v0.3.0 test runs + execution workflow
v0.4.0 defects + attachments
v0.5.0 dashboard + reports
v1.0.0 Docker + CI + E2E + README polish
```

------

## Core Tech Stack

Use this stack unless the repository already contains a deliberate alternative.

### Backend

- Java 21
- Spring Boot 3.x stable line
- Maven Wrapper
- Spring Web
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Bean Validation
- springdoc-openapi
- MapStruct
- JUnit 5
- Mockito
- Testcontainers

### Frontend

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Tailwind CSS
- shadcn-vue or equivalent component primitives
- TanStack Table
- ECharts
- Vitest
- Playwright

### Infrastructure

- Docker Compose
- GitHub Actions
- pnpm
- OpenAPI-generated TypeScript API client when backend API stabilizes

------

## Repository Shape

Target this monorepo structure:

```
qaflow/
 apps/
 api/
 pom.xml
 mvnw
 mvnw.cmd
 src/
 main/
 java/com/gengetau/qaflow/
 QaflowApiApplication.java
 common/
 error/
 pagination/
 validation/
 config/
 OpenApiConfig.java
 WebConfig.java
 security/
 JwtAuthenticationFilter.java
 SecurityConfig.java
 CurrentUser.java
 modules/
 auth/
 users/
 workspaces/
 projects/
 test_suites/
 test_cases/
 test_runs/
 defects/
 attachments/
 reports/
 activity/
 resources/
 application.yml
 application-dev.yml
 application-test.yml
 db/migration/
 test/
 java/com/gengetau/qaflow/
 web/
 package.json
 pnpm-lock.yaml
 vite.config.ts
 src/
 main.ts
 app/
 router/
 stores/
 pages/
 components/
 features/
 api/
 types/
 packages/
 shared/
 openapi/
 types/
 docker/
 docs/
 architecture.md
 api.md
 data-model.md
 testing.md
 engineering-log.md
 scripts/
 .github/
 workflows/
 ci.yml
 docker-compose.yml
 README.md
```

Keep the structure simple.

Do not introduce Nx, Turborepo, Kubernetes, Kafka, Redis, or distributed workers in v1.

------

## Product Scope

QAFlow v1 should support:

- user registration
- login
- JWT access token
- refresh token
- workspace membership
- OWNER / TESTER / VIEWER roles
- project management
- test suite management
- test case management
- test steps
- test run creation
- test run execution
- result statuses:
 - UNTESTED
 - PASSED
 - FAILED
 - BLOCKED
 - SKIPPED
- defect creation from failed test run items
- defect board
- defect comments
- attachment upload and download
- dashboard metrics
- report summary
- HTML report export
- seed demo data
- Docker Compose local startup
- CI
- README and docs

Out of scope for v1:

- billing
- OAuth / SSO
- email notifications
- WebSocket collaboration
- AI generation
- cloud object storage
- Redis
- mobile app
- Kubernetes
- advanced enterprise audit compliance

------

## Core Business Workflow

The main user workflow is:

```
register / login
 -> create workspace
 -> create project
 -> create test suite
 -> create test cases and steps
 -> create test run
 -> execute test run items
 -> mark cases as passed / failed / blocked / skipped
 -> create defects from failed items
 -> upload evidence
 -> transition defect status
 -> view dashboard
 -> export report
```

This workflow must be visible in the backend API, frontend UI, tests, seed data, and README.

------

## Roles and Permissions

Implement these roles:

```
OWNER
TESTER
VIEWER
```

Rules:

- OWNER can manage workspace, projects, members, destructive actions, and reports.
- TESTER can create and update test cases, execute test runs, create defects, comment, and upload evidence.
- VIEWER can only read projects, test cases, runs, defects, dashboard, and reports.
- Every protected resource must verify workspace membership.
- Every write operation must verify role permission.
- Do not rely only on frontend permission checks.

------

## Backend Design Rules

Use module-oriented Spring Boot design.

Prefer:

```
modules/projects/
 Project.java
 ProjectStatus.java
 ProjectRepository.java
 ProjectService.java
 ProjectController.java
 ProjectMapper.java
 dto/
```

Avoid:

```
one giant controller
one giant service
global model dump
business logic in controllers
status updates with no validation
```

Service layer owns:

- permission checks
- workflow transitions
- report aggregation
- attachment authorization
- cross-entity business rules

Controller layer owns:

- request mapping
- validation entrypoint
- response DTOs
- HTTP semantics

Repository layer owns:

- persistence access
- query composition
- database-specific reads

------

## Backend Entity Model

Implement the following core entities.

### User

```
id
email
passwordHash
displayName
avatarUrl
isActive
createdAt
updatedAt
```

### Workspace

```
id
name
slug
createdBy
createdAt
updatedAt
```

### WorkspaceMember

```
id
workspaceId
userId
role: OWNER | TESTER | VIEWER
joinedAt
```

### Project

```
id
workspaceId
name
key
description
status: ACTIVE | ARCHIVED
createdBy
createdAt
updatedAt
```

### TestSuite

```
id
projectId
name
description
sortOrder
createdAt
updatedAt
```

### TestCase

```
id
projectId
suiteId
caseKey
title
description
preconditions
priority: LOW | MEDIUM | HIGH | CRITICAL
type: FUNCTIONAL | REGRESSION | SMOKE | EXPLORATORY
status: DRAFT | READY | DEPRECATED
createdBy
updatedBy
createdAt
updatedAt
```

### TestCaseStep

```
id
testCaseId
stepOrder
action
expectedResult
```

### TestRun

```
id
projectId
name
description
status: PLANNED | IN_PROGRESS | COMPLETED | CANCELLED
startedAt
completedAt
createdBy
createdAt
updatedAt
```

### TestRunItem

```
id
testRunId
testCaseId
assigneeId
result: UNTESTED | PASSED | FAILED | BLOCKED | SKIPPED
actualResult
executedAt
executedBy
createdAt
updatedAt
```

### Defect

```
id
projectId
testRunItemId
title
description
severity: LOW | MEDIUM | HIGH | CRITICAL
priority: LOW | MEDIUM | HIGH | URGENT
status: OPEN | IN_PROGRESS | RESOLVED | CLOSED | REOPENED
assigneeId
reportedBy
createdAt
updatedAt
```

### DefectComment

```
id
defectId
authorId
body
createdAt
updatedAt
```

### Attachment

```
id
projectId
defectId
testRunItemId
uploadedBy
fileName
contentType
storagePath
fileSize
createdAt
```

### ActivityLog

```
id
workspaceId
projectId
actorId
entityType
entityId
action
metadataJson
createdAt
```

------

## State Machine Rules

Do not allow arbitrary status mutation.

### TestRun

Allowed transitions:

```
PLANNED -> IN_PROGRESS
IN_PROGRESS -> COMPLETED
IN_PROGRESS -> CANCELLED
PLANNED -> CANCELLED
```

### TestRunItem

Allowed transitions:

```
UNTESTED -> PASSED
UNTESTED -> FAILED
UNTESTED -> BLOCKED
UNTESTED -> SKIPPED
FAILED -> PASSED
FAILED -> BLOCKED
BLOCKED -> PASSED
BLOCKED -> FAILED
```

### Defect

Allowed transitions:

```
OPEN -> IN_PROGRESS
IN_PROGRESS -> RESOLVED
RESOLVED -> CLOSED
RESOLVED -> REOPENED
REOPENED -> IN_PROGRESS
```

If an invalid transition is requested, return a clear 400 response.

------

## API Surface

Implement REST APIs.

Use `/api` prefix.

### Auth

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET /api/auth/me
```

### Workspaces

```
GET /api/workspaces
POST /api/workspaces
GET /api/workspaces/{workspaceId}
POST /api/workspaces/{workspaceId}/members
PATCH /api/workspaces/{workspaceId}/members/{memberId}
DELETE /api/workspaces/{workspaceId}/members/{memberId}
```

### Projects

```
GET /api/projects
POST /api/projects
GET /api/projects/{projectId}
PATCH /api/projects/{projectId}
DELETE /api/projects/{projectId}
```

### Test Suites

```
GET /api/projects/{projectId}/suites
POST /api/projects/{projectId}/suites
PATCH /api/suites/{suiteId}
DELETE /api/suites/{suiteId}
```

### Test Cases

```
GET /api/projects/{projectId}/test-cases
POST /api/projects/{projectId}/test-cases
GET /api/test-cases/{testCaseId}
PATCH /api/test-cases/{testCaseId}
DELETE /api/test-cases/{testCaseId}
POST /api/test-cases/{testCaseId}/steps
PATCH /api/test-cases/{testCaseId}/steps/{stepId}
DELETE /api/test-cases/{testCaseId}/steps/{stepId}
```

### Test Runs

```
GET /api/projects/{projectId}/test-runs
POST /api/projects/{projectId}/test-runs
GET /api/test-runs/{testRunId}
PATCH /api/test-runs/{testRunId}
POST /api/test-runs/{testRunId}/start
POST /api/test-runs/{testRunId}/complete
GET /api/test-runs/{testRunId}/items
PATCH /api/test-run-items/{itemId}/result
POST /api/test-run-items/{itemId}/defects
```

### Defects

```
GET /api/projects/{projectId}/defects
POST /api/projects/{projectId}/defects
GET /api/defects/{defectId}
PATCH /api/defects/{defectId}
POST /api/defects/{defectId}/comments
POST /api/defects/{defectId}/transition
```

### Attachments

```
POST /api/attachments
GET /api/attachments/{attachmentId}
GET /api/attachments/{attachmentId}/download
DELETE /api/attachments/{attachmentId}
```

### Dashboard and Reports

```
GET /api/projects/{projectId}/dashboard
GET /api/projects/{projectId}/reports/summary
GET /api/projects/{projectId}/reports/test-run/{testRunId}
POST /api/projects/{projectId}/reports/export
```

------

## Frontend Design

Build a modern SaaS-style SPA.

Pages:

```
/auth/login
/auth/register
/app/dashboard
/app/projects
/app/projects/:projectId
/app/projects/:projectId/suites
/app/projects/:projectId/test-cases
/app/test-cases/:testCaseId
/app/projects/:projectId/test-runs
/app/test-runs/:testRunId/execute
/app/projects/:projectId/defects
/app/defects/:defectId
/app/projects/:projectId/reports
/app/settings
```

Core components:

```
AppLayout
SidebarNavigation
TopBar
ProjectSwitcher
DataTable
StatusBadge
PriorityBadge
RoleBadge
TestCaseEditor
TestStepEditor
TestRunExecutionPanel
DefectKanbanBoard
AttachmentUploader
DashboardMetricCard
ReportPreview
ConfirmDialog
```

Pinia stores:

```
authStore
workspaceStore
projectStore
testCaseStore
testRunStore
defectStore
notificationStore
```

Use OpenAPI-generated TypeScript types once backend OpenAPI is stable.

------

## File Upload Rules

v1 uses local filesystem storage.

Use a data directory such as:

```
data/uploads/
```

Store only metadata in PostgreSQL.

Allowed types:

```
image/png
image/jpeg
image/webp
application/pdf
text/plain
```

Max size:

```
10MB
```

Security:

- never expose raw storage paths directly
- validate file ownership through workspace/project membership
- sanitize original file names
- prevent path traversal
- serve downloads through authorized API endpoint

------

## Report Rules

Dashboard metrics:

```
total_test_cases
ready_test_cases
active_test_runs
latest_pass_rate
open_defects
critical_defects
defects_by_status
test_results_by_day
```

Report content:

```
project name
test run name
execution period
total cases
passed / failed / blocked / skipped
pass rate
failed cases list
linked defects
critical defect summary
generated_at
generated_by
```

v1 report export:

- HTML report first
- PDF optional if time remains

------

## Database and Migration Rules

Use Flyway.

Do not rely on Hibernate `ddl-auto=update`.

Use:

```
spring:
 jpa:
 hibernate:
 ddl-auto: validate
 flyway:
 enabled: true
```

Migration naming:

```
V1__init_schema.sql
V2__test_management.sql
V3__test_runs.sql
V4__defects_and_attachments.sql
V5__reports_and_activity.sql
```

Add database constraints:

- unique workspace slug
- unique project key within workspace
- unique test case key within project
- foreign keys
- not null constraints
- indexes for list queries
- indexes for dashboard/report queries

------

## Local Development

Docker Compose should support:

```
postgres
api
web
```

Default ports:

```
frontend: http://localhost:5173
backend: http://localhost:8080
openapi: http://localhost:8080/swagger-ui.html
postgres: localhost:5432
```

Expected commands:

```
docker compose up --build
```

Backend local:

```
cd apps/api
./mvnw spring-boot:run
```

Frontend local:

```
cd apps/web
pnpm install
pnpm dev
```

------

## Testing Requirements

Backend:

- unit tests for services
- controller tests with MockMvc
- repository/integration tests with Testcontainers PostgreSQL
- security/RBAC tests
- workflow tests

Frontend:

- component tests
- store tests
- page smoke tests
- build check
- typecheck

E2E:

Use Playwright for core flows.

Minimum E2E flows:

```
login -> create project -> create test case
create test run -> mark failed -> create defect
transition defect -> verify dashboard/report
```

Default early CI may skip E2E until UI stabilizes.

------

## CI Requirements

Create GitHub Actions.

Backend job:

```
setup Java 21
./mvnw test
./mvnw verify
```

Frontend job:

```
setup Node 24
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test
pnpm build
```

Docker job:

```
docker compose config
```

Later add:

```
playwright e2e
docker compose integration smoke test
```

------

## Demo Data

Seed demo data should include:

```
1 workspace
3 users
2 projects
4 test suites
20 test cases
2 test runs
mixed passed / failed / blocked results
5 defects across different statuses
several comments
sample attachment metadata
```

Demo accounts:

```
owner@example.com / password123
tester@example.com / password123
viewer@example.com / password123
```

Do not seed production profile automatically.

------

## Milestone Backlog

Work through these milestones in order.

### Loop 0: Repository Discovery

Goal:

- inspect current repository
- identify existing files
- determine whether this is blank or partial
- create or update implementation plan
- create `docs/engineering-log.md`

Acceptance:

- repository state understood
- no unnecessary changes
- next loop identified

------

### Loop 1: Monorepo Scaffold

Goal:

- create base monorepo structure
- create root README
- create `.gitignore`
- create basic docs folder
- create root Docker Compose skeleton
- create backend and frontend app directories

Acceptance:

- repository has intended structure
- README explains QAFlow
- engineering log updated

------

### Loop 2: Spring Boot API Scaffold

Goal:

- create Java 21 Spring Boot app under `apps/api`
- use Maven Wrapper
- add health endpoint
- add base package structure
- add application profiles
- add basic error response shape

Acceptance:

- `./mvnw test` passes
- health endpoint exists
- app can start locally

------

### Loop 3: PostgreSQL and Flyway Foundation

Goal:

- add PostgreSQL Docker service
- configure Spring datasource
- add Flyway
- create first migration
- configure JPA validate
- add Testcontainers setup

Acceptance:

- migration runs
- test database starts through Testcontainers
- schema validation passes

------

### Loop 4: Vue Frontend Scaffold

Goal:

- create Vue 3 + TypeScript + Vite app under `apps/web`
- configure pnpm
- add router
- add Pinia
- add Tailwind
- add basic layout
- add health or landing page

Acceptance:

- `pnpm install`
- `pnpm build`
- landing page renders

------

### Loop 5: CI Foundation

Goal:

- add GitHub Actions for backend and frontend
- run Maven tests
- run frontend typecheck/build
- run docker compose config
- add CI badge placeholder in README

Acceptance:

- CI workflow files exist
- local equivalent commands pass

------

### Loop 6: Auth Backend

Goal:

- add User entity
- add Workspace and WorkspaceMember entities
- add registration
- add login
- add BCrypt password hashing
- add JWT access token
- add refresh token storage
- add `/api/auth/me`

Acceptance:

- register/login tests pass
- protected endpoint requires token
- refresh token works

------

### Loop 7: RBAC and Security

Goal:

- implement OWNER / TESTER / VIEWER
- add workspace membership checks
- add reusable permission service
- add security tests

Acceptance:

- viewer cannot write
- tester can create test artifacts
- owner can manage workspace/project data

------

### Loop 8: Auth Frontend

Goal:

- add login page
- add register page
- add auth store
- add token persistence
- add route guards
- add app shell after login

Acceptance:

- user can log in through UI
- protected routes redirect unauthenticated users
- authenticated app shell renders

------

### Loop 9: Projects and Test Suites Backend

Goal:

- add Project entity/API
- add TestSuite entity/API
- add pagination
- add project membership authorization
- add activity log basics

Acceptance:

- project CRUD tests pass
- suite CRUD tests pass
- unauthorized access blocked

------

### Loop 10: Projects and Test Suites Frontend

Goal:

- add project list page
- add project detail page
- add project create/edit dialogs
- add suite management UI
- add empty states

Acceptance:

- user can create and view projects
- user can manage suites in UI

------

### Loop 11: Test Cases Backend

Goal:

- add TestCase entity
- add TestCaseStep entity
- add create/update/list/detail APIs
- add filtering and pagination
- enforce unique test case key per project

Acceptance:

- test case workflow tests pass
- steps persist correctly
- duplicate key returns clear error

------

### Loop 12: Test Cases Frontend

Goal:

- add test case table
- add filters
- add test case detail
- add test case editor
- add steps editor

Acceptance:

- user can create and edit test cases with steps
- table supports useful filtering

------

### Loop 13: Test Runs Backend

Goal:

- add TestRun entity
- add TestRunItem entity
- create test run from selected cases
- start/complete run
- execute run item
- enforce allowed transitions

Acceptance:

- valid transitions pass
- invalid transitions return 400
- execution updates persisted results

------

### Loop 14: Test Run Execution Frontend

Goal:

- add test run list
- add create run flow
- add execution screen
- allow marking pass/fail/blocked/skipped
- show progress

Acceptance:

- user can execute a run through UI
- progress updates visually

------

### Loop 15: Defects Backend

Goal:

- add Defect entity
- add DefectComment entity
- create defect from failed run item
- defect CRUD
- defect transition endpoint
- enforce defect state machine

Acceptance:

- failed run item can create defect
- invalid defect transitions fail
- comments persist

------

### Loop 16: Defects Frontend

Goal:

- add defect list
- add defect board
- add defect detail page
- add comment UI
- add status transition UI

Acceptance:

- user can create and manage defects
- defect board shows statuses

------

### Loop 17: Attachments

Goal:

- add attachment entity
- add local file storage
- add upload endpoint
- add download endpoint
- add attachment UI
- add file validation and authorization

Acceptance:

- evidence can be uploaded
- unauthorized download blocked
- allowed file validation works

------

### Loop 18: Dashboard and Reports Backend

Goal:

- add dashboard aggregate endpoint
- add report summary endpoint
- add test run report endpoint
- add HTML report export

Acceptance:

- dashboard metrics are correct
- report endpoint returns useful data
- HTML report generated

------

### Loop 19: Dashboard and Reports Frontend

Goal:

- add project dashboard
- add charts
- add report page
- add report preview/export UI

Acceptance:

- dashboard shows pass rate, defect status, run progress
- report page works with demo data

------

### Loop 20: OpenAPI TypeScript Client

Goal:

- expose OpenAPI docs
- generate TypeScript API client
- replace hand-written API calls where practical
- document API contract workflow

Acceptance:

- generated client works
- frontend still builds
- README explains generation

------

### Loop 21: Demo Data and Seed Workflow

Goal:

- add demo data seed command/profile
- create owner/tester/viewer users
- create realistic QA data
- document demo login

Acceptance:

- demo data loads locally
- README has demo accounts
- dashboard has meaningful data

------

### Loop 22: E2E Tests

Goal:

- add Playwright
- add login/create project/create test case flow
- add test run failed to defect flow
- add defect transition dashboard/report flow

Acceptance:

- E2E tests pass locally
- CI can optionally run E2E or document manual command

------

### Loop 23: Docker and Local DX Polish

Goal:

- finalize Dockerfiles
- finalize docker-compose
- add Makefile or scripts
- add `.env.example`
- ensure one-command startup

Acceptance:

- `docker compose up --build` works
- API and web communicate
- README quick start works

------

### Loop 24: Documentation Polish

Goal:

- complete README
- add screenshots placeholders or real screenshots if possible
- add architecture docs
- add data model docs
- add API docs
- add testing docs
- add roadmap

Acceptance:

- reviewer can understand and run project from README
- docs explain why project is not a toy CRUD app

------

### Loop 25: Quality Pass and v1.0

Goal:

- run full backend tests
- run full frontend checks
- run docker compose smoke test
- remove dead code
- improve errors
- check no secrets
- verify docs match behavior
- tag v1.0.0 if complete

Acceptance:

- all checks pass
- project is portfolio-ready
- README has CI badge
- release tag created if appropriate

------

## Quality Bar

Do not mark a loop complete unless:

- code compiles
- relevant tests pass
- new behavior has tests where practical
- user-facing behavior is documented
- no secrets are committed
- errors are understandable
- code follows module boundaries

------

## Implementation Guardrails

Avoid overengineering.

Do not add:

- Kubernetes
- microservices
- Redis
- Kafka
- billing
- OAuth
- AI features
- complex workflow engines
- generated code that is not documented
- large binary files

Prefer:

- simple readable code
- strong domain modeling
- meaningful tests
- useful UI
- clear docs
- working local demo

------

## First Action

Start with Loop 0.

Inspect the repository, list what exists, determine whether this is a blank repo or partial implementation, and create the first engineering log entry.

Then immediately continue to Loop 1 unless blocked.
