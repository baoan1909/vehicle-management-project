import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  testMatch: "ai-knowledge-workflow.behavior.spec.ts",
  timeout: 30_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: "http://127.0.0.1:5173",
    browserName: "chromium",
    headless: true,
    viewport: { width: 1440, height: 1000 },
    launchOptions: { args: ["--disable-gpu", "--renderer-process-limit=1", "--js-flags=--max-old-space-size=96"] },
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 5173 --strictPort",
    url: "http://127.0.0.1:5173/tests/ai-knowledge-workflow-harness.html",
    reuseExistingServer: true,
    timeout: 30_000,
  },
});
