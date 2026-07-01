export type TestRunStatus = "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type TestRunItemResult = "UNTESTED" | "PASSED" | "FAILED" | "BLOCKED" | "SKIPPED";

export interface TestRunItem {
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

export interface TestRun {
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

export interface TestRunCreatePayload {
  name: string;
  description: string;
  testCaseIds: string[];
}

export interface TestRunItemResultPayload {
  result: TestRunItemResult;
  actualResult: string;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function listTestRuns(accessToken: string, projectId: string): Promise<TestRun[]> {
  return requestJson(`/api/projects/${projectId}/test-runs`, accessToken);
}

export function createTestRun(
  accessToken: string,
  projectId: string,
  payload: TestRunCreatePayload
): Promise<TestRun> {
  return requestJson(`/api/projects/${projectId}/test-runs`, accessToken, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function startTestRun(accessToken: string, testRunId: string): Promise<TestRun> {
  return requestJson(`/api/test-runs/${testRunId}/start`, accessToken, { method: "POST" });
}

export function completeTestRun(accessToken: string, testRunId: string): Promise<TestRun> {
  return requestJson(`/api/test-runs/${testRunId}/complete`, accessToken, { method: "POST" });
}

export function listTestRunItems(accessToken: string, testRunId: string): Promise<TestRunItem[]> {
  return requestJson(`/api/test-runs/${testRunId}/items`, accessToken);
}

export function updateTestRunItemResult(
  accessToken: string,
  itemId: string,
  payload: TestRunItemResultPayload
): Promise<TestRunItem> {
  return requestJson(`/api/test-run-items/${itemId}/result`, accessToken, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

async function requestJson<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
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

  return response.json() as Promise<T>;
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}
