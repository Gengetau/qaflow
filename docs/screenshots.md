# Screenshots

This project is ready for screenshots, but image assets are intentionally not committed yet.

Recommended captures after running `docker compose up --build`:

| View | URL | What to show |
| --- | --- | --- |
| Login | `http://localhost:5173/auth/login` | Demo account login entrypoint. |
| Dashboard | `http://localhost:5173/app/projects/demo/dashboard` | Ready case count, pass rate, open defects, and execution progress. |
| Projects | `http://localhost:5173/app/projects` | Workspace project list and suite management. |
| Test cases | `http://localhost:5173/app/projects/demo/test-cases` | Case editor with priority, type, status, and ordered steps. |
| Test runs | `http://localhost:5173/app/projects/demo/test-runs` | Execution board, result buttons, and failed-item defect creation. |
| Defects | `http://localhost:5173/app/projects/demo/defects` | Kanban-style defect lifecycle and detail panel. |
| Reports | `http://localhost:5173/app/projects/demo/reports` | Pass rate, failed cases, linked defects, and HTML export preview. |

Suggested output paths:

```text
docs/assets/screenshots/login.png
docs/assets/screenshots/dashboard.png
docs/assets/screenshots/test-runs.png
docs/assets/screenshots/defects.png
docs/assets/screenshots/reports.png
```

The screenshot set should be captured from the seeded demo profile so reviewers see realistic data instead of empty CRUD screens.
