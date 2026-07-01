export type Role = "OWNER" | "TESTER" | "VIEWER";

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  avatarUrl: string | null;
}

export interface WorkspaceMembership {
  id: string;
  name: string;
  slug: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  user: AuthUser;
  workspaces: WorkspaceMembership[];
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  displayName: string;
  workspaceName: string;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  return postJson("/api/auth/login", payload);
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  return postJson("/api/auth/register", payload);
}

export async function logout(accessToken: string, refreshToken: string): Promise<void> {
  await postJson(
    "/api/auth/logout",
    { refreshToken },
    {
      Authorization: `Bearer ${accessToken}`
    }
  );
}

async function postJson<T>(path: string, body: unknown, headers: Record<string, string> = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers
    },
    body: JSON.stringify(body)
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