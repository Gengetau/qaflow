import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it } from "vitest";
import { createQaflowRouter } from ".";
import { createMemoryHistory } from "vue-router";

describe("router guards", () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it("redirects unauthenticated app routes to login", async () => {
    const router = createQaflowRouter(createMemoryHistory());

    await router.push("/app/dashboard");
    await router.isReady();

    expect(router.currentRoute.value.path).toBe("/auth/login");
    expect(router.currentRoute.value.query.redirect).toBe("/app/dashboard");
  });

  it("allows authenticated users into the app shell", async () => {
    localStorage.setItem(
      "qaflow.auth",
      JSON.stringify({
        accessToken: "stored-access",
        refreshToken: "stored-refresh",
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
        ],
        activeWorkspaceId: "a5155989-4ef6-4b0a-8c5c-7386905e1814"
      })
    );
    const router = createQaflowRouter(createMemoryHistory());

    await router.push("/app/dashboard");
    await router.isReady();

    expect(router.currentRoute.value.path).toBe("/app/dashboard");
  });
});
