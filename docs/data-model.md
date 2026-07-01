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
- Test case keys are unique within a project.
- Protected resources must be scoped through workspace membership.
- Status transitions are enforced in the service layer and supported by database constraints where practical.
