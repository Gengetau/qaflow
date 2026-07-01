import { defineStore } from "pinia";

export type Role = "OWNER" | "TESTER" | "VIEWER";

export interface CurrentUser {
  email: string;
  displayName: string;
  role: Role;
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: "",
    user: null as CurrentUser | null
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    canWrite: (state) => state.user?.role === "OWNER" || state.user?.role === "TESTER"
  },
  actions: {
    setSession(token: string, user: CurrentUser) {
      this.token = token;
      this.user = user;
    },
    clearSession() {
      this.token = "";
      this.user = null;
    }
  }
});
