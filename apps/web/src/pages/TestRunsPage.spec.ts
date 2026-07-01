import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";
import TestRunsPage from "./TestRunsPage.vue";

const run = {
  id: "run-1",
  projectId: "project-1",
  name: "Checkout regression",
  description: "Release candidate run",
  status: "IN_PROGRESS",
  startedAt: "2026-07-01T00:10:00Z",
  completedAt: null,
  createdBy: "user-1",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:10:00Z",
  items: [
    {
      id: "item-1",
      testRunId: "run-1",
      testCaseId: "case-1",
      caseKey: "CHK-1",
      title: "Guest checkout succeeds",
      assigneeId: null,
      result: "PASSED",
      actualResult: "Order completed",
      executedAt: "2026-07-01T00:11:00Z",
      executedBy: "user-1",
      createdAt: "2026-07-01T00:00:00Z",
      updatedAt: "2026-07-01T00:11:00Z"
    },
    {
      id: "item-2",
      testRunId: "run-1",
      testCaseId: "case-2",
      caseKey: "CHK-2",
      title: "Coupon checkout succeeds",
      assigneeId: null,
      result: "UNTESTED",
      actualResult: null,
      executedAt: null,
      executedBy: null,
      createdAt: "2026-07-01T00:00:00Z",
      updatedAt: "2026-07-01T00:00:00Z"
    }
  ]
};

const readyCase = {
  id: "case-3",
  projectId: "project-1",
  suiteId: null,
  caseKey: "CHK-3",
  title: "Saved card checkout succeeds",
  description: "Card on file happy path",
  preconditions: null,
  priority: "HIGH",
  type: "REGRESSION",
  status: "READY",
  createdBy: "user-1",
  updatedBy: "user-1",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z",
  steps: []
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
    routes: [{ path: "/app/projects/:projectId/test-runs", component: TestRunsPage }]
  });
  await router.push("/app/projects/project-1/test-runs");
  await router.isReady();

  const wrapper = mount(TestRunsPage, {
    global: {
      plugins: [pinia, router]
    }
  });
  await flushPromises();
  return wrapper;
}

describe("TestRunsPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads test runs and shows execution progress", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0]) => {
      if (String(input) === "/api/projects/project-1/test-runs") {
        return response([run]);
      }
      throw new Error(`Unexpected request: ${String(input)}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();

    expect(wrapper.text()).toContain("Checkout regression");
    expect(wrapper.text()).toContain("Guest checkout succeeds");
    expect(wrapper.get('[data-test="run-progress"]').text()).toContain("50%");
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/test-runs",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
  });

  it("creates a test run from selected ready cases", async () => {
    const createdRun = {
      ...run,
      id: "run-2",
      name: "Payment regression",
      status: "PLANNED",
      startedAt: null,
      items: [{ ...run.items[1], id: "item-3", testRunId: "run-2", testCaseId: "case-3", caseKey: "CHK-3" }]
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/test-runs" && init?.method !== "POST") {
        return response([]);
      }
      if (url === "/api/projects/project-1/test-cases?status=READY&page=0&size=100") {
        return response({ items: [readyCase], totalItems: 1, totalPages: 1, page: 0, size: 100 });
      }
      if (url === "/api/projects/project-1/test-runs" && init?.method === "POST") {
        return response(createdRun);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    await wrapper.get('[data-test="new-run"]').trigger("click");
    await flushPromises();
    await wrapper.get('[data-test="run-name-input"]').setValue("Payment regression");
    await wrapper.get('[data-test="case-checkbox-case-3"]').setValue(true);
    await wrapper.get('[data-test="create-run-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/test-runs",
      expect.objectContaining({
        method: "POST",
        body: expect.stringContaining('"testCaseIds":["case-3"]')
      })
    );
    expect(wrapper.text()).toContain("Payment regression");
    expect(wrapper.text()).toContain("CHK-3");
  });

  it("starts a planned run, marks an item result, and updates progress", async () => {
    const plannedRun = {
      ...run,
      status: "PLANNED",
      startedAt: null,
      items: [run.items[1]]
    };
    const startedRun = {
      ...plannedRun,
      status: "IN_PROGRESS",
      startedAt: "2026-07-01T00:12:00Z"
    };
    const passedItem = {
      ...plannedRun.items[0],
      result: "PASSED",
      actualResult: "Coupon accepted",
      executedAt: "2026-07-01T00:13:00Z"
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/test-runs") {
        return response([plannedRun]);
      }
      if (url === "/api/test-runs/run-1/start" && init?.method === "POST") {
        return response(startedRun);
      }
      if (url === "/api/test-run-items/item-2/result" && init?.method === "PATCH") {
        return response(passedItem);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    expect(wrapper.get('[data-test="run-progress"]').text()).toContain("0%");

    await wrapper.get('[data-test="start-run"]').trigger("click");
    await flushPromises();
    await wrapper.get('[data-test="item-result-note-item-2"]').setValue("Coupon accepted");
    await wrapper.get('[data-test="mark-item-2-PASSED"]').trigger("click");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/test-run-items/item-2/result",
      expect.objectContaining({
        method: "PATCH",
        body: expect.stringContaining('"actualResult":"Coupon accepted"')
      })
    );
    expect(wrapper.get('[data-test="run-progress"]').text()).toContain("100%");
    expect((wrapper.get('[data-test="item-result-note-item-2"]').element as { value: string }).value).toBe(
      "Coupon accepted"
    );
  });
});
