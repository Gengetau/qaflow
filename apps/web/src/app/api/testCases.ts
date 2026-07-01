export type TestCasePriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type TestCaseType = "FUNCTIONAL" | "REGRESSION" | "SMOKE" | "EXPLORATORY";
export type TestCaseStatus = "DRAFT" | "READY" | "DEPRECATED";

export interface PageResponse<T> {
  items: T[];
  totalItems: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface TestCaseStep {
  id: string | null;
  stepOrder: number;
  action: string;
  expectedResult: string;
}

export interface TestCase {
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
  steps: TestCaseStep[];
}

export interface TestCasePayload {
  suiteId: string | null;
  caseKey: string;
  title: string;
  description: string;
  preconditions: string;
  priority: TestCasePriority;
  type: TestCaseType;
  status: TestCaseStatus;
  steps: Array<{
    stepOrder: number;
    action: string;
    expectedResult: string;
  }>;
}

export interface TestCaseFilters {
  query: string;
  status: "" | TestCaseStatus;
  priority: "" | TestCasePriority;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function listTestCases(
  accessToken: string,
  projectId: string,
  filters: TestCaseFilters,
  page = 0,
  size = 20
): Promise<PageResponse<TestCase>> {
  const params = new URLSearchParams();
  if (filters.query.trim()) {
    params.set("query", filters.query.trim());
  }
  if (filters.status) {
    params.set("status", filters.status);
  }
  if (filters.priority) {
    params.set("priority", filters.priority);
  }
  params.set("page", String(page));
  params.set("size", String(size));

  return requestJson(`/api/projects/${projectId}/test-cases?${params.toString()}`, accessToken);
}

export function createTestCase(
  accessToken: string,
  projectId: string,
  payload: TestCasePayload
): Promise<TestCase> {
  return requestJson(`/api/projects/${projectId}/test-cases`, accessToken, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getTestCase(accessToken: string, testCaseId: string): Promise<TestCase> {
  return requestJson(`/api/test-cases/${testCaseId}`, accessToken);
}

export function updateTestCase(
  accessToken: string,
  testCaseId: string,
  payload: TestCasePayload
): Promise<TestCase> {
  return requestJson(`/api/test-cases/${testCaseId}`, accessToken, {
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
