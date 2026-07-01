import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";
import ReportsPage from "./ReportsPage.vue";

const summary = {
  projectId: "project-1",
  projectName: "Checkout Quality",
  totalTestCases: 5,
  readyTestCases: 4,
  latestRun: {
    id: "run-1",
    name: "Checkout regression",
    status: "COMPLETED",
    totalCases: 5,
    passed: 4,
    failed: 1,
    blocked: 0,
    skipped: 0,
    passRate: 80,
    startedAt: "2026-07-01T00:10:00Z",
    completedAt: "2026-07-01T00:40:00Z"
  },
  openDefects: 2,
  criticalDefects: 1,
  defectsByStatus: {
    OPEN: 1,
    IN_PROGRESS: 1,
    RESOLVED: 0,
    CLOSED: 3,
    REOPENED: 0
  }
};

const testRunReport = {
  projectId: "project-1",
  projectName: "Checkout Quality",
  testRunId: "run-1",
  testRunName: "Checkout regression",
  startedAt: "2026-07-01T00:10:00Z",
  completedAt: "2026-07-01T00:40:00Z",
  totalCases: 5,
  passed: 4,
  failed: 1,
  blocked: 0,
  skipped: 0,
  passRate: 80,
  failedCases: [
    {
      testRunItemId: "item-1",
      testCaseId: "case-1",
      caseKey: "CHK-2",
      title: "Coupon checkout succeeds",
      actualResult: "Coupon API returned 500"
    }
  ],
  linkedDefects: [
    {
      id: "defect-1",
      testRunItemId: "item-1",
      caseKey: "CHK-2",
      title: "Coupon rejection blocks checkout",
      severity: "CRITICAL",
      priority: "URGENT",
      status: "OPEN"
    }
  ],
  generatedAt: "2026-07-01T00:45:00Z"
};

function jsonResponse(body: unknown) {
  return Promise.resolve({
    ok: true,
    json: () => Promise.resolve(body)
  } as Response);
}

function textResponse(body: string) {
  return Promise.resolve({
    ok: true,
    text: () => Promise.resolve(body)
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
      email: "tester@example.com",
      displayName: "QA Tester",
      avatarUrl: null
    },
    workspaces: [
      {
        id: "workspace-1",
        name: "Acme Quality Lab",
        slug: "acme-quality-lab",
        role: "TESTER"
      }
    ],
    activeWorkspaceId: "workspace-1",
    hydrated: true
  });

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/app/projects/:projectId/reports", component: ReportsPage }]
  });
  await router.push("/app/projects/project-1/reports");
  await router.isReady();

  const wrapper = mount(ReportsPage, {
    global: {
      plugins: [pinia, router]
    }
  });
  await flushPromises();
  return wrapper;
}

describe("ReportsPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads summary, previews the latest run report, and exports HTML", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/reports/summary") {
        return jsonResponse(summary);
      }
      if (url === "/api/projects/project-1/reports/test-run/run-1") {
        return jsonResponse(testRunReport);
      }
      if (url === "/api/projects/project-1/reports/export" && init?.method === "POST") {
        return textResponse("<html><body>Checkout Quality report HTML</body></html>");
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();

    expect(wrapper.get('[data-test="report-pass-rate"]').text()).toContain("80%");
    expect(wrapper.text()).toContain("Checkout regression");
    expect(wrapper.text()).toContain("Coupon checkout succeeds");
    expect(wrapper.text()).toContain("Coupon rejection blocks checkout");

    await wrapper.get('[data-test="export-html"]').trigger("click");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/reports/export",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
    expect(wrapper.get('[data-test="html-preview"]').text()).toContain("Checkout Quality report HTML");
  });
});
