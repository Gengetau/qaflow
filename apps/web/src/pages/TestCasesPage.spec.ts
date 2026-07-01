import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import TestCasesPage from "./TestCasesPage.vue";
import { useAuthStore } from "../app/stores/auth";

const testCase = {
  id: "case-1",
  projectId: "project-1",
  suiteId: null,
  caseKey: "CHK-1",
  title: "Guest checkout succeeds",
  description: "Checkout happy path coverage",
  preconditions: "Cart has one item",
  priority: "HIGH",
  type: "REGRESSION",
  status: "READY",
  createdBy: "user-1",
  updatedBy: "user-1",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z",
  steps: [
    {
      id: "step-1",
      stepOrder: 1,
      action: "Open checkout",
      expectedResult: "Checkout form is shown"
    }
  ]
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
      email: "owner@example.com",
      displayName: "QA Owner",
      avatarUrl: null
    },
    workspaces: [
      {
        id: "workspace-1",
        name: "Acme Quality Lab",
        slug: "acme-quality-lab",
        role: "OWNER"
      }
    ],
    activeWorkspaceId: "workspace-1",
    hydrated: true
  });

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/app/projects/:projectId/test-cases", component: TestCasesPage }]
  });
  await router.push("/app/projects/project-1/test-cases");
  await router.isReady();

  const wrapper = mount(TestCasesPage, {
    global: {
      plugins: [pinia, router]
    }
  });
  await flushPromises();
  return wrapper;
}

describe("TestCasesPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads the test case table and selected case detail", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0]) => {
      const url = String(input);
      if (url.startsWith("/api/projects/project-1/test-cases")) {
        return response({ items: [testCase], totalItems: 1, totalPages: 1, page: 0, size: 20 });
      }
      if (url === "/api/test-cases/case-1") {
        return response(testCase);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();

    expect(wrapper.text()).toContain("Guest checkout succeeds");
    expect(wrapper.text()).toContain("Open checkout");
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/test-cases?page=0&size=20",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
  });

  it("applies useful table filters", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0]) => {
      const url = String(input);
      if (url.startsWith("/api/projects/project-1/test-cases")) {
        return response({ items: [testCase], totalItems: 1, totalPages: 1, page: 0, size: 20 });
      }
      if (url === "/api/test-cases/case-1") {
        return response(testCase);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    await wrapper.get('[data-test="case-query"]').setValue("checkout");
    await wrapper.get('[data-test="case-status"]').setValue("READY");
    await wrapper.get('[data-test="case-priority"]').setValue("HIGH");
    await wrapper.get('[data-test="case-filter-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/test-cases?query=checkout&status=READY&priority=HIGH&page=0&size=20",
      expect.any(Object)
    );
  });

  it("creates and edits test cases with steps", async () => {
    const createdCase = { ...testCase, id: "case-2", caseKey: "CHK-2", title: "Coupon checkout succeeds" };
    const editedCase = {
      ...createdCase,
      title: "Coupon checkout succeeds on mobile",
      steps: [{ ...createdCase.steps[0], action: "Apply mobile coupon" }]
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url.startsWith("/api/projects/project-1/test-cases") && init?.method !== "POST") {
        return response({ items: [], totalItems: 0, totalPages: 0, page: 0, size: 20 });
      }
      if (url === "/api/projects/project-1/test-cases" && init?.method === "POST") {
        return response(createdCase);
      }
      if (url === "/api/test-cases/case-2" && init?.method === "PATCH") {
        return response(editedCase);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    await wrapper.get('[data-test="new-case"]').trigger("click");
    await wrapper.get('[data-test="case-key-input"]').setValue("CHK-2");
    await wrapper.get('[data-test="case-title-input"]').setValue("Coupon checkout succeeds");
    await wrapper.get('[data-test="case-priority-input"]').setValue("HIGH");
    await wrapper.get('[data-test="case-type-input"]').setValue("REGRESSION");
    await wrapper.get('[data-test="case-status-input"]').setValue("READY");
    await wrapper.get('[data-test="step-action-0"]').setValue("Apply coupon");
    await wrapper.get('[data-test="step-expected-0"]').setValue("Discount is accepted");
    await wrapper.get('[data-test="case-editor-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/test-cases",
      expect.objectContaining({
        method: "POST",
        body: expect.stringContaining('"caseKey":"CHK-2"')
      })
    );
    expect(wrapper.text()).toContain("Coupon checkout succeeds");

    await wrapper.get('[data-test="edit-case"]').trigger("click");
    await wrapper.get('[data-test="case-title-input"]').setValue("Coupon checkout succeeds on mobile");
    await wrapper.get('[data-test="step-action-0"]').setValue("Apply mobile coupon");
    await wrapper.get('[data-test="case-editor-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/test-cases/case-2",
      expect.objectContaining({
        method: "PATCH",
        body: expect.stringContaining("Apply mobile coupon")
      })
    );
    expect(wrapper.text()).toContain("Coupon checkout succeeds on mobile");
  });
});
