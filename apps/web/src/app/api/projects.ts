export type ProjectStatus = "ACTIVE" | "ARCHIVED";

export interface PageResponse<T> {
  items: T[];
  totalItems: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface Project {
  id: string;
  workspaceId: string;
  name: string;
  key: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectCreatePayload {
  workspaceId: string;
  name: string;
  key: string;
  description: string;
}

export interface ProjectUpdatePayload {
  name: string;
  description: string;
  status: ProjectStatus;
}

export interface TestSuite {
  id: string;
  projectId: string;
  name: string;
  description: string | null;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface TestSuitePayload {
  name: string;
  description: string;
  sortOrder: number;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function listProjects(
  accessToken: string,
  workspaceId: string,
  page = 0,
  size = 20
): Promise<PageResponse<Project>> {
  const params = new URLSearchParams({
    workspaceId,
    page: String(page),
    size: String(size)
  });
  return requestJson(`/api/projects?${params.toString()}`, accessToken);
}

export function createProject(accessToken: string, payload: ProjectCreatePayload): Promise<Project> {
  return requestJson("/api/projects", accessToken, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateProject(
  accessToken: string,
  projectId: string,
  payload: ProjectUpdatePayload
): Promise<Project> {
  return requestJson(`/api/projects/${projectId}`, accessToken, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function deleteProject(accessToken: string, projectId: string): Promise<void> {
  return requestVoid(`/api/projects/${projectId}`, accessToken, { method: "DELETE" });
}

export function listSuites(accessToken: string, projectId: string): Promise<TestSuite[]> {
  return requestJson(`/api/projects/${projectId}/suites`, accessToken);
}

export function createSuite(
  accessToken: string,
  projectId: string,
  payload: TestSuitePayload
): Promise<TestSuite> {
  return requestJson(`/api/projects/${projectId}/suites`, accessToken, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateSuite(accessToken: string, suiteId: string, payload: TestSuitePayload): Promise<TestSuite> {
  return requestJson(`/api/suites/${suiteId}`, accessToken, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function deleteSuite(accessToken: string, suiteId: string): Promise<void> {
  return requestVoid(`/api/suites/${suiteId}`, accessToken, { method: "DELETE" });
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

async function requestVoid(path: string, accessToken: string, init: RequestInit): Promise<void> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...init.headers
    }
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const payload = (await response.json()) as { message?: string };
    return payload.message ?? `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}
