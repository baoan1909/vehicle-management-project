import { expect, test, type Page } from "@playwright/test";

const envelope = (data: unknown) => ({ success: true, data, message: "Thành công", timestamp: "2026-11-27T09:00:00+07:00" });
const paged = (content: unknown[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true });
const account = {
  account: { accountId: "account-1", accountStatus: "ACTIVE", createdAt: "2026-11-27T02:00:00Z", updatedAt: "2026-11-27T02:00:00Z", email: "review@example.test", username: "quantri", keycloakUserId: "external-1" },
  role: { roleId: "role-1", roleCode: "SYSTEM_ADMIN", roleName: "SYSTEM_ADMIN", permissionCodes: [] },
  lifecycle: { approvalState: "APPROVED", effectivePermissionCount: 0, operationalAccessState: "ORGANIZATION_ACCESS_REQUIRED", profileState: "PROFILE_COMPLETED", requiredAction: "CONTACT_ORGANIZATION_ADMIN", scopeState: "ORGANIZATION_REQUIRED", workforceState: "NOT_APPLICABLE" },
};

async function fixtures(page: Page) {
  const requests: Array<{ path: string; method: string; body: unknown }> = [];
  const unexpected: string[] = [];
  await page.route("**/*", async (route) => {
    const request = route.request();
    if (!["fetch", "xhr"].includes(request.resourceType())) return route.continue();
    const url = new URL(request.url()), path = url.pathname;
    requests.push({ path: path + url.search, method: request.method(), body: request.postDataJSON() });
    let data: unknown;
    if (path.endsWith("/iam/accounts/provisioned")) data = [account];
    else if (path.endsWith("/account-1/status")) data = { ...account, account: { ...account.account, accountStatus: request.postDataJSON().status } };
    else if (path.endsWith("/account-1/role")) data = { ...account, role: { ...account.role, roleCode: request.postDataJSON().roleCode } };
    else if (path.endsWith("/parking-lots")) data = [];
    else if (path.endsWith("/lost-card-reports/summary")) data = { openCount: 1, unpaidInvoiceCount: 1, resolvedCount: 0, lostCardCount: 1 };
    else if (path.endsWith("/lost-card-reports")) data = [{ lostCardReportId: "lost-1", reportCode: "BCMT-001", context: "VISITOR_IN_PARKING", status: "OPEN", invoiceStatus: "UNPAID", reporterName: "Nguyễn Văn An", licensePlate: "51A-12345", reporterPhone: "0900000000", timeOfLost: "2026-11-27T02:00:00Z", notificationTime: "2026-11-27T02:00:00Z", createdAt: "2026-11-27T02:00:00Z", updatedAt: "2026-11-27T02:00:00Z", totalAmount: 100000 }];
    else if (path.endsWith("/user-guide-features")) data = paged([{ featureId: "feature-1", code: "PARKING", name: "Bãi xe", enabled: true }]);
    else if (path.endsWith("/user-guides/counts")) data = { all: 1, DRAFT: 0, PUBLISHED: 1, INACTIVE: 0, ARCHIVED: 0 };
    else if (path.endsWith("/notifications/user-guides")) data = paged([{ guideId: "guide-1", featureId: "feature-1", code: "HUONG_DAN_01", status: "PUBLISHED", displayType: "BOTTOM_SHEET", replayPolicy: "ONCE_PER_VERSION", contextKey: "parking", priority: 1, displayOrder: 0, currentVersion: 1, lockVersion: 0 }]);
    else if (path.endsWith("/ai/models/configurations")) data = [{ configurationId: "model-1", provider: "GEMINI", modelId: "gemini-test", apiVersion: "v1", useCase: "INTENT_CLASSIFICATION", status: "FALLBACK", rolloutPercentage: 0, priority: 1, requiresFunctionCalling: false, requiresStructuredOutput: true, freeTierApproved: true }];
    else if (path.endsWith("/ai/models/catalog")) data = [{ provider: "GEMINI", modelId: "gemini-test", supportedActions: ["generateContent"] }];
    else if (path.endsWith("/ai/models/warnings")) data = [];
    else { unexpected.push(path); return route.fulfill({ status: 400, json: { message: "Unexpected fixture request" } }); }
    return route.fulfill({ json: envelope(data) });
  });
  return { requests, unexpected };
}

async function expectNoPageOverflow(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
}

for (const width of [390, 768, 1440]) {
  test(`Vietnamese labels wrap inside badges and dropdowns at ${width}px`, async ({ page }, testInfo) => {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/tests/enum-localization-harness.html");
    await expect(page.getByText("Chưa xác định", { exact: true })).toBeVisible();
    await expect(page.getByText("FUTURE_STATUS", { exact: true })).toHaveCount(0);
    await expectNoPageOverflow(page);
    const clipped = await page.getByTestId("translated-badge").evaluateAll((badges) => badges.filter((badge) => {
      const element = badge as HTMLElement, parent = element.parentElement!;
      return element.scrollWidth > element.clientWidth + 1 || element.getBoundingClientRect().right > parent.getBoundingClientRect().right + 1;
    }).map((badge) => badge.textContent));
    expect(clipped).toEqual([]);
    await page.getByRole("button", { name: "Đối tượng", exact: true }).click();
    const option = page.getByRole("button", { name: "Mọi tài khoản đăng nhập", exact: true });
    await expect(option).toBeVisible();
    await option.click();
    const form = page.getByRole("form", { name: "Biểu mẫu nhãn" });
    expect(await form.evaluate((element) => new FormData(element as HTMLFormElement).get("audience"))).toBe("ALL_AUTHENTICATED");
    const narrowTrigger = page.getByRole("button", { name: "Dữ liệu cần dọn dẹp", exact: true });
    const triggerSize = await narrowTrigger.boundingBox();
    expect(triggerSize!.height).toBeGreaterThan(42);
    await narrowTrigger.click();
    const longOption = page.getByRole("button", { name: "Tài khoản tại nhà cung cấp đăng nhập", exact: true });
    await expect(longOption).toBeVisible();
    expect(await longOption.locator("span").last().evaluate((node) => node.scrollWidth <= node.clientWidth + 1 && node.scrollHeight <= node.clientHeight + 1)).toBe(true);
    await longOption.click();
    await page.screenshot({ path: testInfo.outputPath(`labels-${width}.png`), animations: "disabled" });
  });
}

test("account filters and status mutation display Vietnamese while keeping API enums", async ({ page }, testInfo) => {
  const state = await fixtures(page);
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.goto("/tests/enum-localization-harness.html?page=accounts");
  await expect(page.getByText("Quản trị hệ thống", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("SYSTEM_ADMIN", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Vai trò tài khoản", exact: true }).click();
  await page.getByRole("button", { name: "Quản trị hệ thống", exact: true }).click();
  await expect.poll(() => state.requests.some((request) => request.path.includes("roleCode=SYSTEM_ADMIN"))).toBe(true);
  await page.getByRole("button", { name: /Đổi trạng thái$/ }).click();
  const dialog = page.getByRole("dialog", { name: "Đổi trạng thái", exact: true });
  await dialog.getByRole("button", { name: "Đã khóa", exact: true }).click();
  await dialog.getByPlaceholder("Nhập lý do cập nhật trạng thái...").fill("Kiểm tra nhãn giao diện");
  await dialog.getByRole("button", { name: /Lưu trạng thái$/ }).click();
  await expect.poll(() => state.requests.find((request) => request.method === "PATCH")?.body).toEqual({ status: "LOCKED", reason: "Kiểm tra nhãn giao diện" });
  await expect(page.getByText("Đã khóa", { exact: true }).first()).toBeVisible();
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("accounts-1440.png"), animations: "disabled" });
  for (const width of [768, 390]) {
    await page.setViewportSize({ width, height: 900 });
    await expectNoPageOverflow(page);
    await page.screenshot({ path: testInfo.outputPath(`accounts-${width}.png`), animations: "disabled" });
  }
  expect(state.unexpected).toEqual([]);
});

test("lost-card report and invoice statuses are readable and filter values stay intact", async ({ page }, testInfo) => {
  const state = await fixtures(page);
  await page.goto("/tests/enum-localization-harness.html?page=lost-cards");
  await expect(page.getByText("Đang mở", { exact: true })).toBeVisible();
  await expect(page.getByText("Chưa thanh toán", { exact: true })).toBeVisible();
  await expect(page.getByText(/^(OPEN|UNPAID|RESOLVED)$/)).toHaveCount(0);
  await page.getByRole("button", { name: "Trạng thái", exact: true }).click();
  await page.getByRole("button", { name: "Đang mở", exact: true }).click();
  await expect.poll(() => state.requests.some((request) => request.path.includes("status=OPEN"))).toBe(true);
  await page.screenshot({ path: testInfo.outputPath("lost-cards.png"), animations: "disabled" });
  expect(state.unexpected).toEqual([]);
});

test("guide type filters and published labels use Vietnamese", async ({ page }, testInfo) => {
  const state = await fixtures(page);
  await page.goto("/tests/enum-localization-harness.html?page=guides");
  await expect(page.getByText("Bảng trượt từ dưới", { exact: true })).toBeVisible();
  await expect(page.getByText(/^(BOTTOM_SHEET|PUBLISHED)$/)).toHaveCount(0);
  await page.getByRole("button", { name: "Lọc theo loại hiển thị", exact: true }).click();
  await page.getByRole("button", { name: "Bảng trượt từ dưới", exact: true }).click();
  await expect.poll(() => state.requests.some((request) => request.path.includes("displayType=BOTTOM_SHEET"))).toBe(true);
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("guides.png"), animations: "disabled" });
  expect(state.unexpected).toEqual([]);
});

test("AI purpose and fallback status use Vietnamese and table stays scrollable on mobile", async ({ page }, testInfo) => {
  const state = await fixtures(page);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/tests/enum-localization-harness.html?page=models");
  await expect(page.getByRole("cell", { name: "Phân loại ý định", exact: true })).toBeVisible();
  await expect(page.getByRole("cell", { name: "Dự phòng", exact: true })).toBeVisible();
  await expect(page.getByText(/^(INTENT_CLASSIFICATION|FALLBACK)$/)).toHaveCount(0);
  await expectNoPageOverflow(page);
  await page.screenshot({ path: testInfo.outputPath("models-390.png"), animations: "disabled" });
  expect(state.unexpected).toEqual([]);
});
