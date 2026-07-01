import { beforeEach, describe, expect, it, vi } from "vitest";
import { createPinia, setActivePinia } from "pinia";
import { useAuthStore } from "./auth";

const authResponse = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  tokenType: "Bearer",
  expiresInSeconds: 900,
  user: {
    id: "2b49db7c-80f8-47cc-8e30-8d281f912ca2",
    email: "owner@example.com",
    displayName: "QA Owner",
    avatarUrl: null
  },
  workspaces: [
    {
      id: "a5155989-4ef6-4b0a-8c5c-7386905e1814",
      name: "Acme Quality Lab",
      slug: "acme-quality-lab",
      role: "OWNER"
    }
  ]
};

describe("auth store", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it("logs in through the API and persists the session", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(authResponse)
    });
    vi.stubGlobal("fetch", fetchMock);

    const auth = useAuthStore();
    await auth.login({ email: "owner@example.com", password: "password123" });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ email: "owner@example.com", password: "password123" })
      })
    );
    expect(auth.isAuthenticated).toBe(true);
    expect(auth.user?.email).toBe("owner@example.com");
    expect(auth.activeWorkspace?.role).toBe("OWNER");
    expect(JSON.parse(localStorage.getItem("qaflow.auth") ?? "{}")).toMatchObject({
      accessToken: "access-token",
      refreshToken: "refresh-token"
    });
  });

  it("restores and clears persisted sessions", () => {
    localStorage.setItem(
      "qaflow.auth",
      JSON.stringify({
        accessToken: "stored-access",
        refreshToken: "stored-refresh",
        user: authResponse.user,
        workspaces: authResponse.workspaces,
        activeWorkspaceId: authResponse.workspaces[0].id
      })
    );

    const auth = useAuthStore();
    auth.restoreSession();

    expect(auth.isAuthenticated).toBe(true);
    expect(auth.activeWorkspace?.name).toBe("Acme Quality Lab");

    auth.clearSession();

    expect(auth.isAuthenticated).toBe(false);
    expect(localStorage.getItem("qaflow.auth")).toBeNull();
  });
});
