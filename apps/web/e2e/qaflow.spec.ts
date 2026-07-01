import { expect, test, type Page, type Route } from "@playwright/test";

type ProjectStatus = "ACTIVE" | "ARCHIVED";
type TestCaseStatus = "DRAFT" | "READY" | "DEPRECATED";
type TestCasePriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
type TestCaseType = "FUNCTIONAL" | "REGRESSION" | "SMOKE" | "EXPLORATORY";
type TestRunStatus = "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
type TestRunItemResult = "UNTESTED" | "PASSED" | "FAILED" | "BLOCKED" | "SKIPPED";
type DefectStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | "REOPENED";
type DefectSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
type DefectPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

interface Project {
  id: string;
  workspaceId: string;
  name: string;
  key: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}

interface TestCase {
  id: string;
  projectId: string;
  suiteId: string | null;
  caseKey: string;
  title: string;
  description: string | null;
  preconditions: string | null;
  priority: TestCasePriority;
  type: TestCaseType;
  status: TestCaseStatus;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
  steps: Array<{
    id: string | null;
    stepOrder: number;
    action: string;
    expectedResult: string;
  }>;
}

interface TestRunItem {
  id: string;
  testRunId: string;
  testCaseId: string;
  caseKey: string;
  title: string;
  assigneeId: string | null;
  result: TestRunItemResult;
  actualResult: string | null;
  executedAt: string | null;
  executedBy: string | null;
  createdAt: string;
  updatedAt: string;
}

interface TestRun {
  id: string;
  projectId: string;
  name: string;
  description: string | null;
  status: TestRunStatus;
  startedAt: string | null;
  completedAt: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  items: TestRunItem[];
}

interface Defect {
  id: string;
  projectId: string;
  testRunItemId: string | null;
  testCaseId: string | null;
  caseKey: string | null;
  title: string;
  description: string | null;
  severity: DefectSeverity;
  priority: DefectPriority;
  status: DefectStatus;
  assigneeId: string | null;
  reportedBy: string;
  createdAt: string;
  updatedAt: string;
  comments: unknown[];
}

const workspace = {
  id: "workspace-1",
  name: "QAFlow Demo Workspace",
  slug: "qaflow-demo",
  role: "OWNER"
};

test("login creates a project and a ready test case", async ({ page }) => {
  const api = await installApiMock(page);

  await login(page);
  await page.goto("/app/projects");
  await page.getByTestId("new-project").click();
  await page.getByTestId("project-name").fill("Storefront Release QA");
  await page.getByTestId("project-key").fill("SF");
  await page.getByTestId("project-description").fill("Release checkout coverage");
  await page.getByRole("button", { name: "Save project" }).click();

  await expect(page.getByRole("heading", { name: "Storefront Release QA" })).toBeVisible();

  const project = api.projects.find((item) => item.key === "SF");
  expect(project).toBeDefined();
  await page.goto(`/app/projects/${project!.id}/test-cases`);
  await page.getByTestId("new-case").click();
  await page.getByTestId("case-key-input").fill("SF-1");
  await page.getByTestId("case-title-input").fill("Guest checkout can place an order");
  await page.getByTestId("case-priority-input").selectOption("HIGH");
  await page.getByTestId("case-type-input").selectOption("REGRESSION");
  await page.getByTestId("case-status-input").selectOption("READY");
  await page.getByTestId("step-action-0").fill("Submit the checkout form");
  await page.getByTestId("step-expected-0").fill("The order confirmation is displayed");
  await page.getByRole("button", { name: "Save case" }).click();

  await expect(page.getByRole("button", { name: /SF-1/ })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Guest checkout can place an order" })).toBeVisible();
});

test("creates a defect from a failed test run item", async ({ page }) => {
  const api = await installApiMock(page);

  await login(page);
  await page.goto("/app/projects/project-1/test-runs");
  await page.getByTestId("new-run").click();
  await page.getByTestId("run-name-input").fill("Checkout smoke");
  await page.getByTestId("case-checkbox-case-1").check();
  await page.getByRole("button", { name: "Create run" }).click();

  await expect(page.getByRole("heading", { name: "Checkout smoke" })).toBeVisible();
  const run = api.testRuns.find((item) => item.name === "Checkout smoke");
  expect(run).toBeDefined();
  const itemId = run!.items[0].id;

  await page.getByTestId("start-run").click();
  await page.getByTestId(`item-result-note-${itemId}`).fill("Gateway returned 504 on payment submit.");
  await page.getByTestId(`mark-${itemId}-FAILED`).click();
  await page.getByTestId(`create-defect-${itemId}`).click();
  await page.getByTestId("run-item-defect-title-input").fill("Payment submit returns 504");
  await page.getByTestId("run-item-defect-form").getByRole("button", { name: "Save defect" }).click();

  await expect(page.getByTestId("run-defect-notice")).toContainText("Defect created");

  await page.goto("/app/projects/project-1/defects");
  await expect(page.getByTestId("defect-board-OPEN")).toContainText("Payment submit returns 504");
});

test("transitions a defect and reflects it in dashboard and reports", async ({ page }) => {
  await installApiMock(page);

  await login(page);
  await page.goto("/app/projects/project-1/defects");
  await expect(page.getByTestId("defect-board-OPEN")).toContainText("Payment timeout on retry");

  await page.getByTestId("defect-transition-input").selectOption("IN_PROGRESS");
  await expect(page.getByTestId("defect-detail")).toContainText("IN_PROGRESS");
  await page.getByTestId("defect-transition-input").selectOption("RESOLVED");
  await expect(page.getByTestId("defect-detail")).toContainText("RESOLVED");

  await page.goto("/app/projects/project-1/dashboard");
  await expect(page.getByTestId("defect-status-RESOLVED")).toContainText("1");

  await page.goto("/app/projects/project-1/reports");
  await expect(page.getByTestId("report-pass-rate")).toContainText("0%");
  await expect(page.getByText("Payment timeout on retry")).toBeVisible();
  await expect(page.getByText("RESOLVED")).toBeVisible();
  await page.getByTestId("export-html").click();
  await expect(page.getByTestId("html-preview")).toContainText("Payment timeout on retry");
  await expect(page.getByTestId("html-preview")).toContainText("RESOLVED");
});

async function login(page: Page) {
  await page.goto("/auth/login");
  await page.getByRole("button", { name: "Log in" }).click();
  await expect(page).toHaveURL(/\/app\/dashboard$/);
}

async function installApiMock(page: Page) {
  const state = createState();

  await page.route("http://127.0.0.1:5173/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();

    if (method === "POST" && path === "/api/auth/login") {
      return fulfillJson(route, {
        accessToken: "access-token",
        refreshToken: "refresh-token",
        tokenType: "Bearer",
        expiresInSeconds: 3600,
        user: {
          id: "user-1",
          email: "owner@example.com",
          displayName: "QA Owner",
          avatarUrl: null
        },
        workspaces: [workspace]
      });
    }

    if (method === "GET" && path === "/api/projects") {
      return fulfillJson(route, pageResponse(state.projects));
    }

    if (method === "POST" && path === "/api/projects") {
      const body = jsonBody<{ workspaceId: string; name: string; key: string; description: string }>(route);
      const project: Project = {
        id: `project-${state.nextProjectId++}`,
        workspaceId: body.workspaceId,
        name: body.name,
        key: body.key,
        description: body.description,
        status: "ACTIVE",
        createdAt: now(),
        updatedAt: now()
      };
      state.projects = [project, ...state.projects];
      return fulfillJson(route, project);
    }

    const suitesMatch = path.match(/^\/api\/projects\/([^/]+)\/suites$/);
    if (method === "GET" && suitesMatch) {
      return fulfillJson(route, []);
    }

    const dashboardMatch = path.match(/^\/api\/projects\/([^/]+)\/dashboard$/);
    if (method === "GET" && dashboardMatch) {
      return fulfillJson(route, dashboardResponse(state, effectiveProjectId(state, dashboardMatch[1])));
    }

    const testCasesMatch = path.match(/^\/api\/projects\/([^/]+)\/test-cases$/);
    if (method === "GET" && testCasesMatch) {
      const projectId = testCasesMatch[1];
      const status = url.searchParams.get("status") as TestCaseStatus | null;
      const cases = state.testCases.filter((item) => item.projectId === projectId && (!status || item.status === status));
      return fulfillJson(route, pageResponse(cases, Number(url.searchParams.get("page") ?? 0), Number(url.searchParams.get("size") ?? 20)));
    }

    if (method === "POST" && testCasesMatch) {
      const body = jsonBody<{
        suiteId: string | null;
        caseKey: string;
        title: string;
        description: string;
        preconditions: string;
        priority: TestCasePriority;
        type: TestCaseType;
        status: TestCaseStatus;
        steps: Array<{ stepOrder: number; action: string; expectedResult: string }>;
      }>(route);
      const testCase: TestCase = {
        id: `case-${state.nextCaseId++}`,
        projectId: testCasesMatch[1],
        suiteId: body.suiteId,
        caseKey: body.caseKey,
        title: body.title,
        description: body.description,
        preconditions: body.preconditions,
        priority: body.priority,
        type: body.type,
        status: body.status,
        createdBy: "user-1",
        updatedBy: "user-1",
        createdAt: now(),
        updatedAt: now(),
        steps: body.steps.map((step) => ({ ...step, id: `step-${step.stepOrder}` }))
      };
      state.testCases = [testCase, ...state.testCases];
      return fulfillJson(route, testCase);
    }

    const testCaseDetailMatch = path.match(/^\/api\/test-cases\/([^/]+)$/);
    if (method === "GET" && testCaseDetailMatch) {
      return fulfillJson(route, required(state.testCases.find((item) => item.id === testCaseDetailMatch[1])));
    }

    const testRunsMatch = path.match(/^\/api\/projects\/([^/]+)\/test-runs$/);
    if (method === "GET" && testRunsMatch) {
      return fulfillJson(route, state.testRuns.filter((item) => item.projectId === testRunsMatch[1]));
    }

    if (method === "POST" && testRunsMatch) {
      const body = jsonBody<{ name: string; description: string; testCaseIds: string[] }>(route);
      const runId = `run-${state.nextRunId++}`;
      const run: TestRun = {
        id: runId,
        projectId: testRunsMatch[1],
        name: body.name,
        description: body.description,
        status: "PLANNED",
        startedAt: null,
        completedAt: null,
        createdBy: "user-1",
        createdAt: now(),
        updatedAt: now(),
        items: body.testCaseIds.map((testCaseId) => {
          const testCase = required(state.testCases.find((item) => item.id === testCaseId));
          return {
            id: `item-${state.nextItemId++}`,
            testRunId: runId,
            testCaseId,
            caseKey: testCase.caseKey,
            title: testCase.title,
            assigneeId: null,
            result: "UNTESTED",
            actualResult: null,
            executedAt: null,
            executedBy: null,
            createdAt: now(),
            updatedAt: now()
          };
        })
      };
      state.testRuns = [run, ...state.testRuns];
      return fulfillJson(route, run);
    }

    const startRunMatch = path.match(/^\/api\/test-runs\/([^/]+)\/start$/);
    if (method === "POST" && startRunMatch) {
      const run = required(state.testRuns.find((item) => item.id === startRunMatch[1]));
      run.status = "IN_PROGRESS";
      run.startedAt = now();
      run.updatedAt = now();
      return fulfillJson(route, run);
    }

    const itemResultMatch = path.match(/^\/api\/test-run-items\/([^/]+)\/result$/);
    if (method === "PATCH" && itemResultMatch) {
      const body = jsonBody<{ result: TestRunItemResult; actualResult: string }>(route);
      const item = required(state.testRuns.flatMap((run) => run.items).find((runItem) => runItem.id === itemResultMatch[1]));
      item.result = body.result;
      item.actualResult = body.actualResult;
      item.executedAt = now();
      item.executedBy = "user-1";
      item.updatedAt = now();
      return fulfillJson(route, item);
    }

    const itemDefectMatch = path.match(/^\/api\/test-run-items\/([^/]+)\/defects$/);
    if (method === "POST" && itemDefectMatch) {
      const body = jsonBody<{
        title: string;
        description: string;
        severity: DefectSeverity;
        priority: DefectPriority;
        assigneeId: string | null;
      }>(route);
      const item = required(state.testRuns.flatMap((run) => run.items).find((runItem) => runItem.id === itemDefectMatch[1]));
      const run = required(state.testRuns.find((testRun) => testRun.id === item.testRunId));
      const defect: Defect = {
        id: `defect-${state.nextDefectId++}`,
        projectId: run.projectId,
        testRunItemId: item.id,
        testCaseId: item.testCaseId,
        caseKey: item.caseKey,
        title: body.title,
        description: body.description,
        severity: body.severity,
        priority: body.priority,
        status: "OPEN",
        assigneeId: body.assigneeId,
        reportedBy: "user-1",
        createdAt: now(),
        updatedAt: now(),
        comments: []
      };
      state.defects = [defect, ...state.defects];
      return fulfillJson(route, defect);
    }

    const defectsMatch = path.match(/^\/api\/projects\/([^/]+)\/defects$/);
    if (method === "GET" && defectsMatch) {
      return fulfillJson(route, state.defects.filter((item) => item.projectId === defectsMatch[1]));
    }

    const attachmentsMatch = path.match(/^\/api\/defects\/([^/]+)\/attachments$/);
    if (method === "GET" && attachmentsMatch) {
      return fulfillJson(route, []);
    }

    const transitionMatch = path.match(/^\/api\/defects\/([^/]+)\/transition$/);
    if (method === "POST" && transitionMatch) {
      const body = jsonBody<{ status: DefectStatus }>(route);
      const defect = required(state.defects.find((item) => item.id === transitionMatch[1]));
      defect.status = body.status;
      defect.updatedAt = now();
      return fulfillJson(route, defect);
    }

    const reportSummaryMatch = path.match(/^\/api\/projects\/([^/]+)\/reports\/summary$/);
    if (method === "GET" && reportSummaryMatch) {
      return fulfillJson(route, reportSummaryResponse(state, effectiveProjectId(state, reportSummaryMatch[1])));
    }

    const testRunReportMatch = path.match(/^\/api\/projects\/([^/]+)\/reports\/test-run\/([^/]+)$/);
    if (method === "GET" && testRunReportMatch) {
      return fulfillJson(route, testRunReportResponse(state, effectiveProjectId(state, testRunReportMatch[1]), testRunReportMatch[2]));
    }

    const exportMatch = path.match(/^\/api\/projects\/([^/]+)\/reports\/export$/);
    if (method === "POST" && exportMatch) {
      return route.fulfill({
        status: 200,
        contentType: "text/html",
        body: htmlReport(state, effectiveProjectId(state, exportMatch[1]))
      });
    }

    return fulfillJson(route, { message: `Unhandled ${method} ${path}` }, 404);
  });

  return state;
}

function createState() {
  const testCase: TestCase = {
    id: "case-1",
    projectId: "project-1",
    suiteId: null,
    caseKey: "SF-1",
    title: "Guest checkout can place an order",
    description: "Happy path checkout",
    preconditions: "Catalog contains an in-stock item.",
    priority: "HIGH",
    type: "REGRESSION",
    status: "READY",
    createdBy: "user-1",
    updatedBy: "user-1",
    createdAt: now(),
    updatedAt: now(),
    steps: [{ id: "step-1", stepOrder: 1, action: "Submit checkout", expectedResult: "Order confirmation appears" }]
  };
  const seedItem: TestRunItem = {
    id: "item-1",
    testRunId: "run-1",
    testCaseId: testCase.id,
    caseKey: testCase.caseKey,
    title: testCase.title,
    assigneeId: null,
    result: "FAILED",
    actualResult: "Payment retry times out.",
    executedAt: now(),
    executedBy: "user-1",
    createdAt: now(),
    updatedAt: now()
  };

  return {
    nextProjectId: 2,
    nextCaseId: 2,
    nextRunId: 2,
    nextItemId: 2,
    nextDefectId: 2,
    projects: [
      {
        id: "project-1",
        workspaceId: workspace.id,
        name: "Storefront Release QA",
        key: "SF",
        description: "Release checkout coverage",
        status: "ACTIVE" as const,
        createdAt: now(),
        updatedAt: now()
      }
    ] as Project[],
    testCases: [testCase],
    testRuns: [
      {
        id: "run-1",
        projectId: "project-1",
        name: "Nightly checkout regression",
        description: "Completed seeded report run",
        status: "COMPLETED" as const,
        startedAt: now(),
        completedAt: now(),
        createdBy: "user-1",
        createdAt: now(),
        updatedAt: now(),
        items: [seedItem]
      }
    ] as TestRun[],
    defects: [
      {
        id: "defect-1",
        projectId: "project-1",
        testRunItemId: seedItem.id,
        testCaseId: testCase.id,
        caseKey: testCase.caseKey,
        title: "Payment timeout on retry",
        description: "Retrying payment leaves the order pending.",
        severity: "HIGH" as const,
        priority: "URGENT" as const,
        status: "OPEN" as const,
        assigneeId: null,
        reportedBy: "user-1",
        createdAt: now(),
        updatedAt: now(),
        comments: []
      }
    ] as Defect[]
  };
}

function pageResponse<T>(items: T[], page = 0, size = 20) {
  return {
    items,
    totalItems: items.length,
    totalPages: items.length === 0 ? 0 : Math.ceil(items.length / size),
    page,
    size
  };
}

function dashboardResponse(state: ReturnType<typeof createState>, projectId: string) {
  const cases = state.testCases.filter((item) => item.projectId === projectId);
  const runs = state.testRuns.filter((item) => item.projectId === projectId);
  const defects = state.defects.filter((item) => item.projectId === projectId);
  const latestCompleted = runs.find((item) => item.status === "COMPLETED") ?? null;

  return {
    totalTestCases: cases.length,
    readyTestCases: cases.filter((item) => item.status === "READY").length,
    activeTestRuns: runs.filter((item) => item.status === "PLANNED" || item.status === "IN_PROGRESS").length,
    latestPassRate: latestCompleted ? passRate(latestCompleted) : 0,
    openDefects: defects.filter((item) => item.status === "OPEN" || item.status === "REOPENED").length,
    criticalDefects: defects.filter((item) => item.severity === "CRITICAL").length,
    defectsByStatus: countBy(["OPEN", "IN_PROGRESS", "REOPENED", "RESOLVED", "CLOSED"], defects.map((item) => item.status)),
    testResults: countBy(["PASSED", "FAILED", "BLOCKED", "SKIPPED", "UNTESTED"], runs.flatMap((run) => run.items.map((item) => item.result)))
  };
}

function reportSummaryResponse(state: ReturnType<typeof createState>, projectId: string) {
  const project = required(state.projects.find((item) => item.id === projectId));
  const cases = state.testCases.filter((item) => item.projectId === projectId);
  const defects = state.defects.filter((item) => item.projectId === projectId);
  const latestRun = state.testRuns.find((item) => item.projectId === projectId && item.status === "COMPLETED") ?? null;

  return {
    projectId,
    projectName: project.name,
    totalTestCases: cases.length,
    readyTestCases: cases.filter((item) => item.status === "READY").length,
    latestRun: latestRun ? runSummary(latestRun) : null,
    openDefects: defects.filter((item) => item.status === "OPEN" || item.status === "REOPENED").length,
    criticalDefects: defects.filter((item) => item.severity === "CRITICAL").length,
    defectsByStatus: countBy(["OPEN", "IN_PROGRESS", "REOPENED", "RESOLVED", "CLOSED"], defects.map((item) => item.status))
  };
}

function testRunReportResponse(state: ReturnType<typeof createState>, projectId: string, runId: string) {
  const project = required(state.projects.find((item) => item.id === projectId));
  const run = required(state.testRuns.find((item) => item.id === runId));
  const linkedDefects = state.defects.filter((defect) => run.items.some((item) => item.id === defect.testRunItemId));

  return {
    projectId,
    projectName: project.name,
    testRunId: run.id,
    testRunName: run.name,
    startedAt: run.startedAt,
    completedAt: run.completedAt,
    totalCases: run.items.length,
    passed: run.items.filter((item) => item.result === "PASSED").length,
    failed: run.items.filter((item) => item.result === "FAILED").length,
    blocked: run.items.filter((item) => item.result === "BLOCKED").length,
    skipped: run.items.filter((item) => item.result === "SKIPPED").length,
    passRate: passRate(run),
    failedCases: run.items
      .filter((item) => item.result === "FAILED")
      .map((item) => ({
        testRunItemId: item.id,
        testCaseId: item.testCaseId,
        caseKey: item.caseKey,
        title: item.title,
        actualResult: item.actualResult
      })),
    linkedDefects: linkedDefects.map((defect) => ({
      id: defect.id,
      testRunItemId: defect.testRunItemId,
      caseKey: defect.caseKey,
      title: defect.title,
      severity: defect.severity,
      priority: defect.priority,
      status: defect.status
    })),
    generatedAt: now()
  };
}

function htmlReport(state: ReturnType<typeof createState>, projectId: string) {
  const summary = reportSummaryResponse(state, projectId);
  if (!summary.latestRun) {
    return "<h1>No report data</h1>";
  }
  const report = testRunReportResponse(state, projectId, summary.latestRun.id);
  const defects = report.linkedDefects.map((defect) => `<li>${defect.caseKey}: ${defect.title} - ${defect.status}</li>`).join("");
  return `<h1>${report.projectName}</h1><h2>${report.testRunName}</h2><ul>${defects}</ul>`;
}

function runSummary(run: TestRun) {
  return {
    id: run.id,
    name: run.name,
    status: run.status,
    totalCases: run.items.length,
    passed: run.items.filter((item) => item.result === "PASSED").length,
    failed: run.items.filter((item) => item.result === "FAILED").length,
    blocked: run.items.filter((item) => item.result === "BLOCKED").length,
    skipped: run.items.filter((item) => item.result === "SKIPPED").length,
    passRate: passRate(run),
    startedAt: run.startedAt,
    completedAt: run.completedAt
  };
}

function countBy<T extends string>(keys: T[], values: T[]) {
  const counts = Object.fromEntries(keys.map((key) => [key, 0])) as Record<T, number>;
  values.forEach((value) => {
    counts[value] += 1;
  });
  return counts;
}

function passRate(run: TestRun) {
  return run.items.length === 0
    ? 0
    : Math.round((run.items.filter((item) => item.result === "PASSED").length / run.items.length) * 100);
}

function effectiveProjectId(state: ReturnType<typeof createState>, projectId: string) {
  return state.projects.some((item) => item.id === projectId) ? projectId : "project-1";
}

function jsonBody<T>(route: Route): T {
  const raw = route.request().postData();
  return raw ? (JSON.parse(raw) as T) : ({} as T);
}

function fulfillJson(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body)
  });
}

function required<T>(value: T | undefined | null): T {
  if (!value) {
    throw new Error("Expected seeded API entity to exist.");
  }
  return value;
}

function now() {
  return "2026-07-01T00:00:00Z";
}
