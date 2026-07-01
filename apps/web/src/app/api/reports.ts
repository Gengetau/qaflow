export type DefectStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | "REOPENED";
export type TestRunItemResult = "UNTESTED" | "PASSED" | "FAILED" | "BLOCKED" | "SKIPPED";
export type TestRunStatus = "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export interface DashboardResponse {
  totalTestCases: number;
  readyTestCases: number;
  activeTestRuns: number;
  latestPassRate: number;
  openDefects: number;
  criticalDefects: number;
  defectsByStatus: Record<DefectStatus, number>;
  testResults: Record<TestRunItemResult, number>;
}

export interface ReportSummaryResponse {
  projectId: string;
  projectName: string;
  totalTestCases: number;
  readyTestCases: number;
  latestRun: RunSummary | null;
  openDefects: number;
  criticalDefects: number;
  defectsByStatus: Record<DefectStatus, number>;
}

export interface RunSummary {
  id: string;
  name: string;
  status: TestRunStatus;
  totalCases: number;
  passed: number;
  failed: number;
  blocked: number;
  skipped: number;
  passRate: number;
  startedAt: string | null;
  completedAt: string | null;
}

export interface TestRunReportResponse {
  projectId: string;
  projectName: string;
  testRunId: string;
  testRunName: string;
  startedAt: string | null;
  completedAt: string | null;
  totalCases: number;
  passed: number;
  failed: number;
  blocked: number;
  skipped: number;
  passRate: number;
  failedCases: FailedCase[];
  linkedDefects: LinkedDefect[];
  generatedAt: string;
}

export interface FailedCase {
  testRunItemId: string;
  testCaseId: string;
  caseKey: string;
  title: string;
  actualResult: string | null;
}

export interface LinkedDefect {
  id: string;
  testRunItemId: string;
  caseKey: string;
  title: string;
  severity: string;
  priority: string;
  status: DefectStatus;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function getDashboard(accessToken: string, projectId: string): Promise<DashboardResponse> {
  return requestJson(`/api/projects/${projectId}/dashboard`, accessToken);
}

export function getReportSummary(accessToken: string, projectId: string): Promise<ReportSummaryResponse> {
  return requestJson(`/api/projects/${projectId}/reports/summary`, accessToken);
}

export function getTestRunReport(
  accessToken: string,
  projectId: string,
  testRunId: string
): Promise<TestRunReportResponse> {
  return requestJson(`/api/projects/${projectId}/reports/test-run/${testRunId}`, accessToken);
}

export function exportHtmlReport(accessToken: string, projectId: string): Promise<string> {
  return requestText(`/api/projects/${projectId}/reports/export`, accessToken, { method: "POST" });
}

async function requestJson<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
  const response = await request(path, accessToken, init);
  return response.json() as Promise<T>;
}

async function requestText(path: string, accessToken: string, init: RequestInit = {}): Promise<string> {
  const response = await request(path, accessToken, init);
  return response.text();
}

async function request(path: string, accessToken: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      Authorization: `Bearer ${accessToken}`,
      ...init.headers
    }
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  return response;
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}
