# Testing

QAFlow uses layered automated checks.

## Backend

- JUnit 5 unit tests for services.
- MockMvc controller tests.
- Testcontainers PostgreSQL tests for persistence and Flyway.
- Security/RBAC tests for role permissions.
- Workflow tests for register/login, project creation, test execution, defect creation, and dashboard updates.

## Frontend

- Vitest tests for stores and components.
- Vue Test Utils for page/component smoke checks.
- TypeScript type checks.
- Production build checks.

## E2E

Playwright will cover:

- Login -> create project -> create test case.
- Create test run -> mark failed -> create defect.
- Transition defect -> verify dashboard/report.
