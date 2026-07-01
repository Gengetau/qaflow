import { mount } from "@vue/test-utils";
import { createPinia, setActivePinia } from "pinia";
import { describe, expect, it } from "vitest";
import { createMemoryHistory } from "vue-router";
import { createQaflowRouter } from "./app/router";
import App from "./App.vue";

function storeSession() {
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
}

describe("App", () => {
  it("renders the authenticated QAFlow product shell", async () => {
    localStorage.clear();
    storeSession();
    const pinia = createPinia();
    setActivePinia(pinia);
    const router = createQaflowRouter(createMemoryHistory());
    await router.push("/app/dashboard");
    await router.isReady();

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router]
      }
    });

    expect(wrapper.text()).toContain("QAFlow");
    expect(wrapper.text()).toContain("test runs");
    expect(wrapper.text()).toContain("defects");
    expect(wrapper.text()).toContain("Acme Quality Lab");
  });
});