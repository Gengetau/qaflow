export type DefectSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type DefectPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type DefectStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | "REOPENED";

export interface DefectComment {
  id: string;
  defectId: string;
  authorId: string;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface Defect {
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
  comments: DefectComment[];
}

export interface DefectPayload {
  title: string;
  description: string;
  severity: DefectSeverity;
  priority: DefectPriority;
  assigneeId: string | null;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function listDefects(accessToken: string, projectId: string): Promise<Defect[]> {
  return requestJson(`/api/projects/${projectId}/defects`, accessToken);
}

export function createProjectDefect(
  accessToken: string,
  projectId: string,
  payload: DefectPayload
): Promise<Defect> {
  return requestJson(`/api/projects/${projectId}/defects`, accessToken, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function createDefectFromRunItem(
  accessToken: string,
  itemId: string,
  payload: DefectPayload
): Promise<Defect> {
  return requestJson(`/api/test-run-items/${itemId}/defects`, accessToken, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getDefect(accessToken: string, defectId: string): Promise<Defect> {
  return requestJson(`/api/defects/${defectId}`, accessToken);
}

export function updateDefect(accessToken: string, defectId: string, payload: DefectPayload): Promise<Defect> {
  return requestJson(`/api/defects/${defectId}`, accessToken, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}

export function transitionDefect(accessToken: string, defectId: string, status: DefectStatus): Promise<Defect> {
  return requestJson(`/api/defects/${defectId}/transition`, accessToken, {
    method: "POST",
    body: JSON.stringify({ status })
  });
}

export function addDefectComment(accessToken: string, defectId: string, body: string): Promise<DefectComment> {
  return requestJson(`/api/defects/${defectId}/comments`, accessToken, {
    method: "POST",
    body: JSON.stringify({ body })
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
