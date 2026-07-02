import { chromium, request } from "@playwright/test";
import { mkdir } from "node:fs/promises";
import path from "node:path";

const webBaseUrl = process.env.QAFLOW_WEB_URL ?? "http://localhost:5173";
const apiBaseUrl = process.env.QAFLOW_API_URL ?? "http://localhost:8080";
const outputDir =
  process.env.QAFLOW_SCREENSHOT_DIR ?? path.resolve(process.cwd(), "../../docs/assets/screenshots");

async function main() {
  await mkdir(outputDir, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({
    viewport: { width: 1440, height: 1000 },
    deviceScaleFactor: 1
  });

  try {
    await page.goto(`${webBaseUrl}/auth/login`, { waitUntil: "domcontentloaded" });
    await page.getByRole("button", { name: "Log in" }).click();
    await page.waitForURL("**/app/dashboard", { timeout: 15000 });

    const session = await page.evaluate(() => {
      const raw = localStorage.getItem("qaflow.auth");
      return raw ? JSON.parse(raw) : null;
    });

    if (!session?.accessToken || !session?.activeWorkspaceId) {
      throw new Error("Demo login did not create an authenticated session.");
    }

    const api = await request.newContext({
      baseURL: apiBaseUrl,
      extraHTTPHeaders: {
        Authorization: `Bearer ${session.accessToken}`
      }
    });

    const projectsResponse = await api.get(
      `/api/projects?workspaceId=${encodeURIComponent(session.activeWorkspaceId)}&page=0&size=20`
    );

    if (!projectsResponse.ok()) {
      throw new Error(`Project lookup failed with HTTP ${projectsResponse.status()}.`);
    }

    const projects = await projectsResponse.json();
    const project = projects.items?.find((item) => item.key === "SHOP") ?? projects.items?.[0];

    if (!project?.id) {
      throw new Error("No demo project was available for screenshots.");
    }

    await api.dispose();

    const targets = [
      {
        file: "dashboard.png",
        url: `${webBaseUrl}/app/projects/${project.id}/dashboard`,
        heading: "Release quality snapshot"
      },
      {
        file: "test-runs.png",
        url: `${webBaseUrl}/app/projects/${project.id}/test-runs`,
        heading: "Execution board"
      },
      {
        file: "defects.png",
        url: `${webBaseUrl}/app/projects/${project.id}/defects`,
        heading: "Risk and fix tracking"
      },
      {
        file: "reports.png",
        url: `${webBaseUrl}/app/projects/${project.id}/reports`,
        heading: "Quality report preview"
      }
    ];

    for (const target of targets) {
      await page.goto(target.url, { waitUntil: "domcontentloaded" });
      await page.getByRole("heading", { name: target.heading }).waitFor({ timeout: 15000 });
      await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => undefined);
      await page.screenshot({
        path: path.join(outputDir, target.file),
        fullPage: true
      });
      console.log(`Captured ${target.file}`);
    }
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
