# Data Model

The data model is workspace-scoped and workflow-oriented. It is designed to support authorization, test execution state, defect linkage, evidence files, dashboard aggregation, and report export.

## Core Entities

| Entity | Purpose |
| --- | --- |
| `User` | Login identity and display profile. |
| `Workspace` | Tenant boundary for projects and membership. |
| `WorkspaceMember` | User-to-workspace role assignment. |
| `Project` | QA initiative under a workspace. |
| `TestSuite` | Ordered grouping for test cases inside a project. |
| `TestCase` | Reusable verification asset. |
| `TestCaseStep` | Ordered action/expected-result step under a test case. |
| `TestRun` | Execution instance planned from selected test cases. |
| `TestRunItem` | Snapshot of a test case inside one run, with execution result. |
| `Defect` | Project risk item, optionally linked to a failed run item. |
| `DefectComment` | Discussion note on a defect. |
| `Attachment` | Evidence file metadata linked to a defect or run item. |
| `ActivityLog` | Project activity/audit event foundation. |

## Relationships

| Relationship | Cardinality |
| --- | --- |
| Workspace -> WorkspaceMember | One workspace has many members. |
| User -> WorkspaceMember | One user can belong to many workspaces. |
| Workspace -> Project | One workspace has many projects. |
| Project -> TestSuite | One project has many suites. |
| Project -> TestCase | One project has many test cases. |
| TestCase -> TestCaseStep | One test case has ordered steps. |
| Project -> TestRun | One project has many runs. |
| TestRun -> TestRunItem | One run has selected execution items. |
| TestRunItem -> TestCase | Each item references the source case. |
| Project -> Defect | One project has many defects. |
| Defect -> TestRunItem | Optional link from a defect to a failed run item. |
| Defect -> DefectComment | One defect has many comments. |
| Project -> Attachment | One project owns all evidence metadata. |
| Attachment -> Defect/TestRunItem | Exactly one evidence target is required. |

## Key Constraints

- Workspace slugs are unique.
- Project keys are unique within a workspace.
- Test suites belong to a project and are ordered by `sort_order` then name.
- Deleting a project cascades to its suites and project activity logs.
- Test case keys are unique within a project.
- Test case steps belong to one test case and are ordered by `step_order`.
- Deleting a test case cascades to its steps.
- Test runs belong to one project and contain selected test run items.
- Test run states are `PLANNED`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`.
- Test run details can be edited only while `PLANNED`.
- Test run execution items reference test cases and persist `UNTESTED`, `PASSED`, `FAILED`, `BLOCKED`, or `SKIPPED` results.
- Test run item results can be changed only while the parent run is `IN_PROGRESS`.
- Deleting a project cascades to its test runs and run items.
- Defects belong to one project and may reference one failed test run item.
- Defect severities are `LOW`, `MEDIUM`, `HIGH`, and `CRITICAL`; priorities are `LOW`, `MEDIUM`, `HIGH`, and `URGENT`.
- Defect states are `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, and `REOPENED`.
- Defect comments belong to one defect and cascade when the defect is deleted.
- Deleting a project cascades to its defects and comments.
- Attachments belong to one project and exactly one evidence target: either a defect or a test run item.
- Attachment files are stored under the configured local upload root; the database stores sanitized file metadata and a relative storage path.
- Attachment content types are limited to `image/png`, `image/jpeg`, `image/webp`, `application/pdf`, and `text/plain`.
- Protected resources must be scoped through workspace membership.
- Status transitions are enforced in the service layer and supported by database constraints where practical.

## State Machines

Test runs:

```text
PLANNED -> IN_PROGRESS -> COMPLETED
```

Only planned runs can have their details edited. Only in-progress runs can execute item results.

Test run item results:

```text
UNTESTED -> PASSED | FAILED | BLOCKED | SKIPPED
```

Defects can be created from run items only after the item result is `FAILED`.

Defects:

```text
OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
RESOLVED -> REOPENED -> IN_PROGRESS
CLOSED -> REOPENED -> IN_PROGRESS
```

## Reporting Data

Dashboard and report responses aggregate across:

- total and ready test cases
- active and latest completed test runs
- run item result counts
- latest pass rate
- open and critical defects
- defect status distribution
- failed cases and linked defects for a run report

The model intentionally preserves run item snapshots so a historical run report still has the case key/title that was executed, even if the source test case changes later.
