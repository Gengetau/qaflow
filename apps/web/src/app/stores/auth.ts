import { defineStore } from "pinia";
import * as authApi from "../api/auth";
import type { AuthUser, LoginPayload, RegisterPayload, Role, WorkspaceMembership } from "../api/auth";

const STORAGE_KEY = "qaflow.auth";

interface AuthState {
  accessToken: string;
  refreshToken: string;
  user: AuthUser | null;
  workspaces: WorkspaceMembership[];
  activeWorkspaceId: string;
  hydrated: boolean;
}

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
  workspaces: WorkspaceMembership[];
  activeWorkspaceId: string;
}

export type { AuthUser, Role, WorkspaceMembership };

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    accessToken: "",
    refreshToken: "",
    user: null,
    workspaces: [],
    activeWorkspaceId: "",
    hydrated: false
  }),
  getters: {
    token: (state) => state.accessToken,
    isAuthenticated: (state) => Boolean(state.accessToken),
    activeWorkspace: (state) =>
      state.workspaces.find((workspace) => workspace.id === state.activeWorkspaceId) ?? state.workspaces[0] ?? null,
    role(): Role | null {
      return this.activeWorkspace?.role ?? null;
    },
    canWrite(): boolean {
      return this.role === "OWNER" || this.role === "TESTER";
    }
  },
  actions: {
    async login(payload: LoginPayload) {
      this.setSession(await authApi.login(payload));
    },
    async register(payload: RegisterPayload) {
      this.setSession(await authApi.register(payload));
    },
    async logout() {
      const accessToken = this.accessToken;
      const refreshToken = this.refreshToken;
      this.clearSession();

      if (accessToken && refreshToken) {
        try {
          await authApi.logout(accessToken, refreshToken);
        } catch {
          // Local logout should still complete if token revocation is unavailable.
        }
      }
    },
    setSession(response: authApi.AuthResponse) {
      this.accessToken = response.accessToken;
      this.refreshToken = response.refreshToken;
      this.user = response.user;
      this.workspaces = response.workspaces;
      this.activeWorkspaceId = response.workspaces[0]?.id ?? "";
      this.hydrated = true;
      this.persistSession();
    },
    restoreSession() {
      if (this.hydrated) {
        return;
      }

      const rawSession = localStorage.getItem(STORAGE_KEY);
      if (!rawSession) {
        this.hydrated = true;
        return;
      }

      try {
        const session = JSON.parse(rawSession) as StoredSession;
        this.accessToken = session.accessToken;
        this.refreshToken = session.refreshToken;
        this.user = session.user;
        this.workspaces = session.workspaces;
        this.activeWorkspaceId = session.activeWorkspaceId;
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      } finally {
        this.hydrated = true;
      }
    },
    clearSession() {
      this.accessToken = "";
      this.refreshToken = "";
      this.user = null;
      this.workspaces = [];
      this.activeWorkspaceId = "";
      this.hydrated = true;
      localStorage.removeItem(STORAGE_KEY);
    },
    persistSession() {
      if (!this.user) {
        return;
      }

      const session: StoredSession = {
        accessToken: this.accessToken,
        refreshToken: this.refreshToken,
        user: this.user,
        workspaces: this.workspaces,
        activeWorkspaceId: this.activeWorkspaceId
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    }
  }
});