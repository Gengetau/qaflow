import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";
import DashboardPage from "./DashboardPage.vue";

const dashboard = {
  totalTestCases: 16,
  readyTestCases: 12,
  activeTestRuns: 2,
  latestPassRate: 67,
  openDefects: 4,
  criticalDefects: 1,
  defectsByStatus: {
    OPEN: 3,
    IN_PROGRESS: 1,
    RESOLVED: 2,
    CLOSED: 5,
    REOPENED: 0
  },
  testResults: {
    UNTESTED: 4,
    PASSED: 8,
    FAILED: 2,
    BLOCKED: 1,
    SKIPPED: 1
  }
};

function response(body: unknown) {
  return Promise.resolve({
    ok: true,
    json: () => Promise.resolve(body)
  } as Response);
}

async function mountPage() {
  const pinia = createPinia();
  setActivePinia(pinia);
  const auth = useAuthStore();
  auth.$patch({
    accessToken: "access-token",
    user: {
      id: "user-1",
      email: "viewer@example.com",
      displayName: "QA Viewer",
      avatarUrl: null
    },
    workspaces: [
      {
        id: "workspace-1",
        name: "Acme Quality Lab",
        slug: "acme-quality-lab",
        role: "VIEWER"
      }
    ],
    activeWorkspaceId: "workspace-1",
    hydrated: true
  });

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/app/projects/:projectId/dashboard", component: DashboardPage }]
  });
  await router.push("/app/projects/project-1/dashboard");
  await router.isReady();

  const wrapper = mount(DashboardPage, {
    global: {
      plugins: [pinia, router]
    }
  });
  await flushPromises();
  return wrapper;
}

describe("DashboardPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads project dashboard metrics with defect status and run progress charts", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0]) => {
      if (String(input) === "/api/projects/project-1/dashboard") {
        return response(dashboard);
      }
      throw new Error(`Unexpected request: ${String(input)}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/dashboard",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
    expect(wrapper.get('[data-test="latest-pass-rate"]').text()).toContain("67%");
    expect(wrapper.get('[data-test="open-defects"]').text()).toContain("4");
    expect(wrapper.get('[data-test="defect-status-OPEN"]').text()).toContain("3");
    expect(wrapper.get('[data-test="test-result-PASSED"]').text()).toContain("8");
    expect(wrapper.get('[data-test="run-progress"]').text()).toContain("75%");
  });
});
