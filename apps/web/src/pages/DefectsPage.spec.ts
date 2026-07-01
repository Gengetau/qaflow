import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import { useAuthStore } from "../app/stores/auth";
import DefectsPage from "./DefectsPage.vue";

const defect = {
  id: "defect-1",
  projectId: "project-1",
  testRunItemId: "item-1",
  testCaseId: "case-1",
  caseKey: "CHK-1",
  title: "Payment timeout on retry",
  description: "Retrying payment leaves the order pending.",
  severity: "HIGH",
  priority: "URGENT",
  status: "OPEN",
  assigneeId: null,
  reportedBy: "user-1",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z",
  comments: [
    {
      id: "comment-1",
      defectId: "defect-1",
      authorId: "user-1",
      body: "Reproduced against the release candidate.",
      createdAt: "2026-07-01T00:05:00Z",
      updatedAt: "2026-07-01T00:05:00Z"
    }
  ]
};

const resolvedDefect = {
  ...defect,
  id: "defect-2",
  testRunItemId: null,
  testCaseId: null,
  caseKey: null,
  title: "Dashboard pass rate rounding",
  description: "Resolved rounding issue.",
  severity: "MEDIUM",
  priority: "MEDIUM",
  status: "RESOLVED",
  comments: []
};

const attachment = {
  id: "attachment-1",
  projectId: "project-1",
  defectId: "defect-1",
  testRunItemId: null,
  uploadedBy: "user-1",
  fileName: "payment failure.txt",
  contentType: "text/plain",
  fileSize: 15,
  createdAt: "2026-07-01T00:30:00Z"
};

function response(body: unknown) {
  return Promise.resolve({
    ok: true,
    json: () => Promise.resolve(body)
  } as Response);
}

async function mountPage(role: "OWNER" | "TESTER" | "VIEWER" = "TESTER") {
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
        role
      }
    ],
    activeWorkspaceId: "workspace-1",
    hydrated: true
  });

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/app/projects/:projectId/defects", component: DefectsPage }]
  });
  await router.push("/app/projects/project-1/defects");
  await router.isReady();

  const wrapper = mount(DefectsPage, {
    global: {
      plugins: [pinia, router]
    }
  });
  await flushPromises();
  return wrapper;
}

describe("DefectsPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads defects and groups them by status on the board", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/defects") {
        return response([defect, resolvedDefect]);
      }
      if (url === "/api/defects/defect-1/attachments") {
        return response([attachment]);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/defects",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
    expect(wrapper.get('[data-test="defect-board-OPEN"]').text()).toContain("Payment timeout on retry");
    expect(wrapper.get('[data-test="defect-board-RESOLVED"]').text()).toContain("Dashboard pass rate rounding");
    expect(wrapper.get('[data-test="defect-detail"]').text()).toContain("Retrying payment leaves the order pending.");
    expect(wrapper.get('[data-test="defect-detail"]').text()).toContain("Reproduced against the release candidate.");
    expect(wrapper.get('[data-test="defect-attachments"]').text()).toContain("payment failure.txt");
  });

  it("creates and edits a project defect", async () => {
    const createdDefect = {
      ...defect,
      id: "defect-3",
      testRunItemId: null,
      testCaseId: null,
      caseKey: null,
      title: "Search filters drop owner",
      description: "Owner filter clears after refresh.",
      severity: "CRITICAL",
      priority: "URGENT",
      comments: []
    };
    const editedDefect = {
      ...createdDefect,
      title: "Search filters drop selected owner",
      priority: "HIGH"
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/defects" && init?.method !== "POST") {
        return response([]);
      }
      if (url === "/api/projects/project-1/defects" && init?.method === "POST") {
        return response(createdDefect);
      }
      if (url === "/api/defects/defect-3/attachments") {
        return response([]);
      }
      if (url === "/api/defects/defect-3" && init?.method === "PATCH") {
        return response(editedDefect);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    await wrapper.get('[data-test="new-defect"]').trigger("click");
    await wrapper.get('[data-test="defect-title-input"]').setValue("Search filters drop owner");
    await wrapper.get('[data-test="defect-description-input"]').setValue("Owner filter clears after refresh.");
    await wrapper.get('[data-test="defect-severity-input"]').setValue("CRITICAL");
    await wrapper.get('[data-test="defect-priority-input"]').setValue("URGENT");
    await wrapper.get('[data-test="defect-editor-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/defects",
      expect.objectContaining({
        method: "POST",
        body: expect.stringContaining('"title":"Search filters drop owner"')
      })
    );
    expect(wrapper.text()).toContain("Search filters drop owner");

    await wrapper.get('[data-test="edit-defect"]').trigger("click");
    await wrapper.get('[data-test="defect-title-input"]').setValue("Search filters drop selected owner");
    await wrapper.get('[data-test="defect-priority-input"]').setValue("HIGH");
    await wrapper.get('[data-test="defect-editor-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/defects/defect-3",
      expect.objectContaining({
        method: "PATCH",
        body: expect.stringContaining('"priority":"HIGH"')
      })
    );
    expect(wrapper.text()).toContain("Search filters drop selected owner");
  });

  it("transitions defects and persists comments", async () => {
    const inProgressDefect = { ...defect, status: "IN_PROGRESS", updatedAt: "2026-07-01T00:15:00Z" };
    const newComment = {
      id: "comment-2",
      defectId: "defect-1",
      authorId: "user-1",
      body: "Backend logs attached to the ticket.",
      createdAt: "2026-07-01T00:20:00Z",
      updatedAt: "2026-07-01T00:20:00Z"
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/defects") {
        return response([defect]);
      }
      if (url === "/api/defects/defect-1/attachments") {
        return response([]);
      }
      if (url === "/api/defects/defect-1/transition" && init?.method === "POST") {
        return response(inProgressDefect);
      }
      if (url === "/api/defects/defect-1/comments" && init?.method === "POST") {
        return response(newComment);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    await wrapper.get('[data-test="defect-transition-input"]').setValue("IN_PROGRESS");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/defects/defect-1/transition",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ status: "IN_PROGRESS" })
      })
    );
    expect(wrapper.get('[data-test="defect-detail"]').text()).toContain("IN_PROGRESS");

    await wrapper.get('[data-test="defect-comment-input"]').setValue("Backend logs attached to the ticket.");
    await wrapper.get('[data-test="defect-comment-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/defects/defect-1/comments",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ body: "Backend logs attached to the ticket." })
      })
    );
    expect(wrapper.get('[data-test="defect-detail"]').text()).toContain("Backend logs attached to the ticket.");
  });

  it("uploads evidence attachments for the selected defect", async () => {
    const uploadedAttachment = {
      ...attachment,
      id: "attachment-2",
      fileName: "checkout-log.txt"
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/defects") {
        return response([defect]);
      }
      if (url === "/api/defects/defect-1/attachments") {
        return response([]);
      }
      if (url === "/api/attachments" && init?.method === "POST") {
        return response(uploadedAttachment);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = await mountPage();
    const file = new Blob(["gateway timeout"], { type: "text/plain" }) as Blob & { name: string };
    Object.defineProperty(file, "name", { value: "checkout-log.txt" });
    const input = wrapper.get('[data-test="attachment-file-input"]');
    Object.defineProperty(input.element, "files", { value: [file], configurable: true });
    await input.trigger("change");
    await wrapper.get('[data-test="attachment-upload-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/attachments",
      expect.objectContaining({
        method: "POST",
        body: expect.any(FormData)
      })
    );
    expect(wrapper.get('[data-test="defect-attachments"]').text()).toContain("checkout-log.txt");
  });
});
