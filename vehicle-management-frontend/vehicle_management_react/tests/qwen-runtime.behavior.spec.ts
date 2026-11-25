import { expect, test } from "@playwright/test";
test.setTimeout(60_000);
const envelope = (data: unknown) => ({ success: true, data, message: "ok", timestamp: new Date().toISOString() });
const initial = { mode: "GOOGLE_API", targetMode: "GOOGLE_API", epoch: 1, status: "READY", changedBy: null, changedAt: new Date().toISOString() };

test("two exclusive choices persist server state and do not announce success during switching", async ({ page }) => {
  let state = { ...initial };
  const writes: unknown[] = [];
  let release!: () => void;
  let gate = new Promise<void>(resolve => { release = resolve; });
  await page.route("**/ai/runtime", async route => {
    if (route.request().method() === "PUT") {
      const body = route.request().postDataJSON(); writes.push(body);
      if (body.expectedEpoch !== state.epoch) return route.fulfill({ status: 400, json: { success: false, message: "Cấu hình đã thay đổi" } });
      state = { ...state, targetMode: body.mode, epoch: state.epoch + 1, status: "SWITCHING" };
      await gate;
      return route.fulfill({ json: envelope(state) });
    }
    return route.fulfill({ json: envelope(state) });
  });
  await page.goto("/tests/qwen-runtime-harness.html");
  const panel = page.getByRole("region", { name: "Chế độ chatbot AI" });
  await expect(panel.getByRole("button")).toHaveCount(2);
  await expect(panel.getByRole("button", { name: "Google API", exact: true })).toHaveAttribute("aria-pressed", "true");
  await panel.getByRole("button", { name: "Qwen local", exact: true }).click();
  await expect(panel.getByRole("button", { name: "Qwen local", exact: true })).toBeDisabled();
  release();
  await expect(panel.getByRole("status")).toContainText("Đang chuyển sang Qwen local");
  await expect(panel.getByRole("button", { name: "Google API", exact: true })).toBeDisabled();
  state = { ...state, mode: "QWEN_LOCAL", status: "PREPARING" };
  await expect(panel.getByRole("status")).toContainText("FAQ đã duyệt vẫn có thể trả lời mà không gọi model");
  state = { ...state, status: "READY" };
  await expect(panel.getByRole("status")).toHaveText("Đã áp dụng: Qwen local.");
  await page.reload();
  await expect(panel.getByRole("button", { name: "Qwen local", exact: true })).toHaveAttribute("aria-pressed", "true");
  gate = Promise.resolve();
  await panel.getByRole("button", { name: "Google API", exact: true }).click();
  await expect(panel.getByRole("status")).toContainText("Đang chuyển sang Google API");
  state = { ...state, mode: "GOOGLE_API", status: "READY" };
  await expect(panel.getByRole("status")).toHaveText("Đã áp dụng: Google API.");
  expect(writes).toEqual([{ mode: "QWEN_LOCAL", expectedEpoch: 1 }, { mode: "GOOGLE_API", expectedEpoch: 2 }]);
});

test("read-only user cannot change provider", async ({ page }) => {
  let writes = 0;
  await page.route("**/ai/runtime", route => {
    if (route.request().method() !== "GET") writes++;
    return route.fulfill({ json: envelope(initial) });
  });
  await page.goto("/tests/qwen-runtime-harness.html?readonly");
  const panel = page.getByRole("region", { name: "Chế độ chatbot AI" });
  await expect(panel.getByText("Bạn không có quyền đổi chế độ AI.")).toBeVisible();
  await expect(panel.getByRole("button", { name: "Qwen local", exact: true })).toBeDisabled();
  await expect(panel.getByRole("button", { name: "Google API", exact: true })).toBeDisabled();
  expect(writes).toBe(0);
});
