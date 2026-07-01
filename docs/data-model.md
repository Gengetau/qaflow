# Data Model

The target model follows the supplied design document.

Core entities:

- User
- Workspace
- WorkspaceMember
- Project
- TestSuite
- TestCase
- TestCaseStep
- TestRun
- TestRunItem
- Defect
- DefectComment
- Attachment
- ActivityLog

Key constraints:

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
