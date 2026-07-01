import { afterEach, describe, expect, it, vi } from "vitest";
import { exportHtmlReport, getDashboard } from "./qaflowClient";

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

describe("generated qaflow client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("calls generated JSON and HTML report operations with authorization", async () => {
    const fetchMock = vi.fn((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = String(input);
      if (url === "/api/projects/project-1/dashboard") {
        return jsonResponse({
          totalTestCases: 2,
          readyTestCases: 2,
          activeTestRuns: 0,
          latestPassRate: 50,
          openDefects: 1,
          criticalDefects: 1,
          defectsByStatus: { OPEN: 1 },
          testResults: { PASSED: 1, FAILED: 1 }
        });
      }
      if (url === "/api/projects/project-1/reports/export" && init?.method === "POST") {
        return textResponse("<html>QAFlow report</html>");
      }
      throw new Error(`Unexpected request: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    const dashboard = await getDashboard("access-token", "project-1");
    const html = await exportHtmlReport("access-token", "project-1");

    expect(dashboard.latestPassRate).toBe(50);
    expect(html).toContain("QAFlow report");
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/dashboard",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/projects/project-1/reports/export",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
  });
});
