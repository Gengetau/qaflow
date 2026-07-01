# API

All API routes use the `/api` prefix.

## Auth

- `POST /api/auth/register`: create a user, default workspace, OWNER membership, access token, and refresh token.
- `POST /api/auth/login`: authenticate with email/password and issue fresh tokens.
- `POST /api/auth/refresh`: rotate a valid refresh token and issue a new access token.
- `POST /api/auth/logout`: revoke a refresh token for an authenticated user.
- `GET /api/auth/me`: return the current authenticated user and workspace memberships.

Auth tokens use `Authorization: Bearer <accessToken>`. Refresh tokens are stored server-side as SHA-256 hashes.

## Workspaces

- `GET /api/workspaces`: list workspaces where the current user is a member.
- `POST /api/workspaces`: create a workspace and assign the current user as `OWNER`.
- `GET /api/workspaces/{workspaceId}`: return workspace details and members; requires membership.
- `POST /api/workspaces/{workspaceId}/members`: add an existing user by email; requires `OWNER`.
- `PATCH /api/workspaces/{workspaceId}/members/{memberId}`: change a member role; requires `OWNER` and keeps at least one owner.
- `DELETE /api/workspaces/{workspaceId}/members/{memberId}`: remove a member; requires `OWNER` and keeps at least one owner.

Workspace roles are `OWNER`, `TESTER`, and `VIEWER`. Backend permission checks enforce membership on protected workspace resources. `OWNER` can manage workspace members, `OWNER` and `TESTER` can pass test-artifact write checks, and `VIEWER` is read-only.

## Projects And Test Assets

- `GET /api/projects?workspaceId={workspaceId}&page=0&size=20`: list projects for a workspace using `{ items, totalItems, totalPages, page, size }`; requires workspace membership.
- `POST /api/projects`: create a project with `workspaceId`, `name`, `key`, and optional `description`; requires `OWNER`.
- `GET /api/projects/{projectId}`: return project details; requires workspace membership.
- `PATCH /api/projects/{projectId}`: update `name`, `description`, and `status`; requires `OWNER`.
- `DELETE /api/projects/{projectId}`: delete a project; requires `OWNER`.
- `GET /api/projects/{projectId}/suites`: list suites by `sortOrder` and `name`; requires workspace membership.
- `POST /api/projects/{projectId}/suites`: create a suite with `name`, optional `description`, and `sortOrder`; requires `OWNER` or `TESTER`.
- `PATCH /api/suites/{suiteId}`: update suite details; requires `OWNER` or `TESTER`.
- `DELETE /api/suites/{suiteId}`: delete a suite; requires `OWNER` or `TESTER`.
- `GET /api/projects/{projectId}/test-cases?query=&status=&priority=&suiteId=&page=0&size=20`: list test cases using `{ items, totalItems, totalPages, page, size }`; requires workspace membership.
- `POST /api/projects/{projectId}/test-cases`: create a test case with optional `suiteId`, `caseKey`, content fields, enum fields, and ordered `steps`; requires `OWNER` or `TESTER`.
- `GET /api/test-cases/{testCaseId}`: return test case detail with steps; requires workspace membership.
- `PATCH /api/test-cases/{testCaseId}`: update test case fields and replace its ordered steps; requires `OWNER` or `TESTER`.
- `DELETE /api/test-cases/{testCaseId}`: delete a test case and its steps; requires `OWNER` or `TESTER`.

## Test Run Execution

- `GET /api/projects/{projectId}/test-runs`: list test runs for a project; requires workspace membership.
- `POST /api/projects/{projectId}/test-runs`: create a planned run from selected `testCaseIds`; requires `OWNER` or `TESTER`.
- `GET /api/test-runs/{testRunId}`: return run detail with items; requires workspace membership.
- `PATCH /api/test-runs/{testRunId}`: update planned run `name` and `description`; requires `OWNER` or `TESTER`.
- `POST /api/test-runs/{testRunId}/start`: transition a `PLANNED` run to `IN_PROGRESS`; requires `OWNER` or `TESTER`.
- `POST /api/test-runs/{testRunId}/complete`: transition an `IN_PROGRESS` run to `COMPLETED`; requires `OWNER` or `TESTER`.
- `GET /api/test-runs/{testRunId}/items`: list execution items and persisted results; requires workspace membership.
- `PATCH /api/test-run-items/{itemId}/result`: set `PASSED`, `FAILED`, `BLOCKED`, or `SKIPPED` while the run is in progress; requires `OWNER` or `TESTER`.

Invalid test run transitions return `400`. `VIEWER` users can read run state but cannot create, update, start, complete, or execute runs.

## Defects

- `GET /api/projects/{projectId}/defects`: list defects for a project; requires workspace membership.
- `POST /api/projects/{projectId}/defects`: create a project defect not linked to a run item; requires `OWNER` or `TESTER`.
- `POST /api/test-run-items/{itemId}/defects`: create a defect linked to a failed run item; requires `OWNER` or `TESTER` and the run item result must be `FAILED`.
- `GET /api/defects/{defectId}`: return defect detail with comments; requires workspace membership.
- `PATCH /api/defects/{defectId}`: update title, description, severity, priority, and optional assignee; requires `OWNER` or `TESTER`.
- `POST /api/defects/{defectId}/comments`: add a defect comment; requires `OWNER` or `TESTER`.
- `POST /api/defects/{defectId}/transition`: transition defect state; requires `OWNER` or `TESTER`.

Defect severities are `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL`. Priorities are `LOW`, `MEDIUM`, `HIGH`, and `URGENT`. State transitions are enforced in the service layer: `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`, with `RESOLVED` or `CLOSED` allowed to move to `REOPENED`, and `REOPENED -> IN_PROGRESS`.

## Attachments

- `POST /api/attachments`: upload multipart evidence for exactly one defect or test run item. Parameters are `projectId`, optional `defectId`, optional `testRunItemId`, and `file`; requires `OWNER` or `TESTER`.
- `GET /api/defects/{defectId}/attachments`: list evidence attached to a defect; requires workspace membership.
- `GET /api/test-run-items/{itemId}/attachments`: list evidence attached to a run item; requires workspace membership.
- `GET /api/attachments/{attachmentId}/download`: download evidence through the authorized API; requires workspace membership.

Allowed attachment content types are `image/png`, `image/jpeg`, `image/webp`, `application/pdf`, and `text/plain`. Maximum file size is 10 MB. Downloads are served through the API; storage paths are never exposed directly.

## Reports

- `GET /api/projects/{projectId}/dashboard`
- `GET /api/projects/{projectId}/reports/summary`
- `GET /api/projects/{projectId}/reports/test-run/{testRunId}`
- `POST /api/projects/{projectId}/reports/export`

Dashboard returns project-level case counts, active run count, latest pass rate, open/critical defect counts, defect status distribution, and latest run result counts. Report summary returns the same project summary with latest run details. Test run report returns execution totals, pass rate, failed cases, and linked defects. HTML export returns a print-friendly `text/html` report.
