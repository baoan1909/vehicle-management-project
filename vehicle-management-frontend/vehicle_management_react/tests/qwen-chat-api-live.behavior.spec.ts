import { expect, test } from "@playwright/test";
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";

// Opt-in only. Responses come from the disposable real backend, never answer fixtures.
const readyFile = process.env.QWEN_API_BROWSER_READY_FILE;
test.skip(!readyFile, "Requires a running isolated Qwen API cohort with a browser hold window");
test.setTimeout(180_000);

test("guest sends real API questions, sees sources and keeps follow-up context", async ({ page }, testInfo) => {
  const ready = JSON.parse(readFileSync(readyFile!, "utf8")) as { baseUrl: string; expiresAt: string };
  expect(Date.parse(ready.expiresAt)).toBeGreaterThan(Date.now());
  const backend = new URL(ready.baseUrl);
  expect(["localhost", "127.0.0.1"]).toContain(backend.hostname);
  const published: { question: string; answer: string; citations: unknown[]; status: number }[] = [];
  const violations: string[] = [];
  page.on("request", request => {
    if (/generativelanguage\.googleapis\.com|aiplatform\.googleapis\.com/.test(request.url())) violations.push(request.url());
  });
  await page.route("**/vehicle-management/api/**", async route => {
    const incoming = new URL(route.request().url());
    const api = incoming.pathname.slice(incoming.pathname.indexOf("/api/"));
    // Transport proxy only: actual status/body and authorization checks remain in the backend.
    const response = await route.fetch({ url: ready.baseUrl + api + incoming.search, timeout: 120_000 });
    if (api === "/api/public/support/assistant/messages") {
      const payload = await response.json();
      const request = route.request().postDataJSON() as { message: string; scope?: unknown; tools?: unknown };
      expect(request.scope).toBeUndefined();
      expect(request.tools).toBeUndefined();
      published.push({ question: request.message, answer: String(payload.data?.answer ?? ""),
        citations: payload.data?.citations ?? [], status: response.status() });
    }
    await route.fulfill({ response });
  });
  // The external challenge widget is a fixture; proof issuance and validation use real APIs/Redis.
  await page.route("https://challenges.cloudflare.com/turnstile/v0/api.js**", route => route.fulfill({
    contentType: "text/javascript", body: `window.turnstile={remove:function(){},render:function(element,options){setTimeout(function(){options.callback('fixture-browser-'+crypto.randomUUID());},0);return 'fixture';}};`,
  }));
  try {
    await page.goto("/help");
    const panel = page.getByRole("dialog", { name: "Trợ lý hỗ trợ CoParking", exact: true });
    await expect(panel).toBeVisible();
    await expect(panel.getByText("CAPTCHA đã xác minh.", { exact: true })).toBeVisible({ timeout: 20_000 });
    await panel.getByLabel("Nhập tin nhắn hỗ trợ").fill("Hotline hỗ trợ CoParking là gì?");
    await panel.getByRole("button", { name: "Gửi tin nhắn", exact: true }).click();
    await expect.poll(() => published.length, { timeout: 120_000 }).toBe(1);
    expect(published[0].status).toBe(200);
    expect(published[0].answer).toContain("0912345678");
    expect(published[0].citations.length).toBeGreaterThan(0);
    await expect(panel.getByText("Nguồn tham khảo", { exact: true })).toBeVisible();
    await expect(panel.getByText("CAPTCHA đã xác minh.", { exact: true })).toBeVisible({ timeout: 20_000 });
    await panel.getByLabel("Nhập tin nhắn hỗ trợ").fill("Số đó là gì?");
    await panel.getByRole("button", { name: "Gửi tin nhắn", exact: true }).click();
    await expect.poll(() => published.length, { timeout: 120_000 }).toBe(2);
    expect(published[1].status).toBe(200);
    expect(published[1].answer).toContain("0912345678");
    expect(violations).toEqual([]);
    await page.screenshot({ path: testInfo.outputPath("qwen-real-api-guest-follow-up.png") });
    await testInfo.attach("actual-api-transcript", { body: JSON.stringify(published, null, 2), contentType: "application/json" });
  } finally {
    writeFileSync(path.join(path.dirname(readyFile!), "browser-complete.json"), JSON.stringify({
      kind: "REAL_GUEST_API_BROWSER_FAQ_AND_FOLLOW_UP", recorded: published.length,
      semanticAcceptance: false, memberUiAndModelFailureUiVerified: false,
    }));
  }
});
