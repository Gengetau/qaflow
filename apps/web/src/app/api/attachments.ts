export interface Attachment {
  id: string;
  projectId: string;
  defectId: string | null;
  testRunItemId: string | null;
  uploadedBy: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

export type UploadableFile = Blob & { name?: string };

export interface AttachmentUploadPayload {
  projectId: string;
  defectId?: string;
  testRunItemId?: string;
  file: UploadableFile;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export function listDefectAttachments(accessToken: string, defectId: string): Promise<Attachment[]> {
  return requestJson(`/api/defects/${defectId}/attachments`, accessToken);
}

export function listTestRunItemAttachments(accessToken: string, itemId: string): Promise<Attachment[]> {
  return requestJson(`/api/test-run-items/${itemId}/attachments`, accessToken);
}

export function uploadAttachment(
  accessToken: string,
  payload: AttachmentUploadPayload
): Promise<Attachment> {
  const formData = new FormData();
  formData.set("projectId", payload.projectId);
  if (payload.defectId) {
    formData.set("defectId", payload.defectId);
  }
  if (payload.testRunItemId) {
    formData.set("testRunItemId", payload.testRunItemId);
  }
  formData.append("file", payload.file, payload.file.name ?? "evidence");

  return requestJson("/api/attachments", accessToken, {
    method: "POST",
    body: formData
  });
}

async function requestJson<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
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
