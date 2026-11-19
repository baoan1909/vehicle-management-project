import { expect, test, type Page } from "@playwright/test";

// The first Vite module graph includes the shared UI barrel and OIDC runtime.
test.setTimeout(60_000);

const api = "**/api/operations/support-ticket-categories/available";
const catalog = [
  { categoryId: "second", code: "CUSTOMER_CARD_QR", name: "Thẻ và mã QR", description: "Thẻ hoặc QR hợp lệ nhưng không sử dụng được.", priority: "HIGH", displayOrder: 40, featureCode: null },
  { categoryId: "first", code: "CUSTOMER_ACCOUNT_ACCESS", name: "Tài khoản và đăng nhập", description: "Không đăng nhập được hoặc phiên đăng nhập lỗi.", priority: "HIGH", displayOrder: 10, featureCode: null },
];
const envelope = (data: unknown) => ({ data, success: true, message: "ok", timestamp: new Date().toISOString() });
async function open(page: Page) { await page.goto("/tests/support-categories-harness.html"); }

test("loads API names in display order, searches, shows description and new categories on reopen", async ({ page }) => {
  let list = [...catalog];
  await page.route(api, route => route.fulfill({ json: envelope(list) }));
  await open(page);
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await expect(page.locator(".vm-select-menu-panel").getByRole("button").nth(1)).toHaveText("Tài khoản và đăng nhập");
  await page.getByPlaceholder("Tìm danh mục hỗ trợ...").fill("Thẻ");
  await expect(page.getByRole("button", { name: "Tài khoản và đăng nhập" })).toHaveCount(0);
  await page.getByRole("button", { name: "Thẻ và mã QR" }).click();
  await expect(page.getByText(catalog[0].description, { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Lỗi xử lý báo mất thẻ" })).toHaveCount(0);
  await page.getByRole("button", { name: "Hủy", exact: true }).click();
  list = [...catalog, { ...catalog[0], categoryId: "new", code: "ADMIN_CUSTOM", name: "Danh mục mới từ quản trị", displayOrder: 50 }];
  await page.getByRole("button", { name: "Mở form hỗ trợ" }).click();
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Danh mục mới từ quản trị" })).toBeVisible();
});

test("loading, empty and recoverable error states disable submission", async ({ page }) => {
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  await page.route(api, async route => { await gate; await route.fulfill({ json: envelope([]) }); });
  await open(page);
  await expect(page.getByText("Đang tải danh mục hỗ trợ...", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true })).toBeDisabled();
  release();
  await expect(page.getByText("Chưa có danh mục hỗ trợ khả dụng cho tài khoản của bạn.")).toBeVisible();
  await page.unroute(api);
  await page.route(api, route => route.fulfill({ status: 503, json: { success: false, message: "Không thể tải danh mục lúc này." } }));
  await page.getByRole("button", { name: "Làm mới danh mục hỗ trợ" }).click();
  await expect(page.getByRole("alert")).toContainText("Không thể tải danh mục lúc này.");
  await page.unroute(api);
  await page.route(api, route => route.fulfill({ json: envelope(catalog) }));
  await page.getByRole("button", { name: "Làm mới danh mục hỗ trợ" }).click();
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Thẻ và mã QR" })).toBeVisible();
});

for (const status of [403, 409]) test(`category unavailable (${status}) during editing refreshes selection`, async ({ page }) => {
  const message = status === 409 ? "Danh mục hỗ trợ đã bị vô hiệu hóa. Vui lòng tải lại và chọn danh mục khác." : "Quyền truy cập đã thay đổi. Vui lòng chọn lại.";
  let list = [...catalog];
  await page.route(api, route => route.fulfill({ json: envelope(list) }));
  await page.route("**/api/operations/support-tickets", route => route.fulfill({ status,
    json: { success: false, message, data: { code: status === 403 ? "SUPPORT_CATEGORY_NOT_AVAILABLE" : "CONFLICT" } } }));
  await open(page);
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await page.getByRole("button", { name: "Thẻ và mã QR" }).click();
  await page.getByLabel("Tiêu đề", { exact: true }).fill("Lỗi quét thẻ");
  await page.getByLabel("Nội dung chi tiết").fill("Thẻ hợp lệ không quét được.");
  list = [catalog[1]];
  await page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText(message);
  await expect(page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Thẻ và mã QR" })).toHaveCount(0);
  await expect(page.getByLabel("Tiêu đề", { exact: true })).toHaveValue("Lỗi quét thẻ");
});

test("mobile dialog and searchable selector fit viewport", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 });
  await page.route(api, route => route.fulfill({ json: envelope(catalog) }));
  await open(page);
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Thẻ và mã QR" })).toBeVisible();
  const dialog = await page.getByRole("dialog").boundingBox();
  expect(dialog!.width).toBeLessThanOrEqual(375);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
});


test("manual refresh retains selection and draft without periodic requests", async ({ page }) => {
  await page.clock.install();
  let requests = 0;
  await page.route(api, route => { requests++; return route.fulfill({ json: envelope(catalog) }); });
  await open(page);
  await page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await page.getByRole("button", { name: "Thẻ và mã QR", exact: true }).click();
  await page.getByLabel("Tiêu đề", { exact: true }).fill("Lỗi quét thẻ");
  await page.getByLabel("Nội dung chi tiết").fill("Thẻ hợp lệ không quét được.");
  const initial = requests;
  await page.clock.fastForward(35_000);
  expect(requests).toBe(initial);
  await page.getByRole("button", { name: "Làm mới danh mục hỗ trợ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Làm mới danh mục hỗ trợ", exact: true })).toBeEnabled();
  expect(requests).toBe(initial + 1);
  await expect(page.getByRole("button", { name: "Danh mục hỗ trợ", exact: true })).toContainText("Thẻ và mã QR");
  await expect(page.getByLabel("Tiêu đề", { exact: true })).toHaveValue("Lỗi quét thẻ");
  await expect(page.getByLabel("Nội dung chi tiết")).toHaveValue("Thẻ hợp lệ không quét được.");
});
