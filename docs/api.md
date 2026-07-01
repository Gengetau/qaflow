# API

All API routes use the `/api` prefix.

## Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`

## Workspaces

- `GET /api/workspaces`
- `POST /api/workspaces`
- `GET /api/workspaces/{workspaceId}`
- `POST /api/workspaces/{workspaceId}/members`
- `PATCH /api/workspaces/{workspaceId}/members/{memberId}`
- `DELETE /api/workspaces/{workspaceId}/members/{memberId}`

## Projects And Test Assets

- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}`
- `DELETE /api/projects/{projectId}`
- `GET /api/projects/{projectId}/suites`
- `POST /api/projects/{projectId}/suites`
- `GET /api/projects/{projectId}/test-cases`
- `POST /api/projects/{projectId}/test-cases`

## Execution, Defects, Attachments, Reports

- `GET /api/projects/{projectId}/test-runs`
- `POST /api/projects/{projectId}/test-runs`
- `PATCH /api/test-run-items/{itemId}/result`
- `POST /api/test-run-items/{itemId}/defects`
- `GET /api/projects/{projectId}/defects`
- `POST /api/attachments`
- `GET /api/projects/{projectId}/dashboard`
- `POST /api/projects/{projectId}/reports/export`
