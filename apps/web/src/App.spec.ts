import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import App from "./App.vue";

describe("App", () => {
  it("renders the QAFlow product shell", () => {
    const wrapper = mount(App);

    expect(wrapper.text()).toContain("QAFlow");
    expect(wrapper.text()).toContain("test runs");
    expect(wrapper.text()).toContain("defects");
  });
});
