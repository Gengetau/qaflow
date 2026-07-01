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
