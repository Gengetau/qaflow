import { flushPromises, mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { afterEach, describe, expect, it, vi } from "vitest";
import ProjectsPage from "./ProjectsPage.vue";
import { useAuthStore } from "../app/stores/auth";

const workspace = {
  id: "workspace-1",
  name: "Acme Quality Lab",
  slug: "acme-quality-lab",
  role: "OWNER" as const
};

const project = {
  id: "project-1",
  workspaceId: workspace.id,
  name: "Checkout Quality",
  key: "CHK",
  description: "Regression coverage",
  status: "ACTIVE",
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z"
};

const suite = {
  id: "suite-1",
  projectId: project.id,
  name: "Regression",
  description: "Core checkout flow",
  sortOrder: 10,
  createdAt: "2026-07-01T00:00:00Z",
  updatedAt: "2026-07-01T00:00:00Z"
};

function response(body: unknown) {
  return Promise.resolve({
    ok: true,
    json: () => Promise.resolve(body)
  } as Response);
}

function mountWithOwner() {
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
    workspaces: [workspace],
    activeWorkspaceId: workspace.id,
    hydrated: true
  });

  return mount(ProjectsPage, {
    global: {
      plugins: [pinia]
    }
  });
}

describe("ProjectsPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads projects and suites for the active workspace", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0]) => {
      const url = String(input);
      if (url.startsWith("/api/projects?")) {
        return response({
          items: [project],
          totalItems: 1,
          totalPages: 1,
          page: 0,
          size: 20
        });
      }
      if (url === `/api/projects/${project.id}/suites`) {
        return response([suite]);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = mountWithOwner();
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects?workspaceId=workspace-1&page=0&size=20",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
    expect(wrapper.text()).toContain("Checkout Quality");
    expect(wrapper.text()).toContain("Regression");
  });

  it("creates a project from the page dialog", async () => {
    const createdProject = {
      ...project,
      id: "project-2",
      name: "Mobile App QA",
      key: "MOB",
      description: "Mobile release checks"
    };
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url.startsWith("/api/projects?")) {
        return response({
          items: [],
          totalItems: 0,
          totalPages: 0,
          page: 0,
          size: 20
        });
      }
      if (url === "/api/projects" && init?.method === "POST") {
        return response(createdProject);
      }
      if (url === `/api/projects/${createdProject.id}/suites`) {
        return response([]);
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const wrapper = mountWithOwner();
    await flushPromises();

    await wrapper.get('[data-test="new-project"]').trigger("click");
    await wrapper.get('[data-test="project-name"]').setValue("Mobile App QA");
    await wrapper.get('[data-test="project-key"]').setValue("MOB");
    await wrapper.get('[data-test="project-description"]').setValue("Mobile release checks");
    await wrapper.get('[data-test="project-form"]').trigger("submit");
    await flushPromises();

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          workspaceId: workspace.id,
          name: "Mobile App QA",
          key: "MOB",
          description: "Mobile release checks"
        })
      })
    );
    expect(wrapper.text()).toContain("Mobile App QA");
  });
});
