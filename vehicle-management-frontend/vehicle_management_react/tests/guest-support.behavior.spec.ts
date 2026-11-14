import { expect, test, type Page, type Request } from "@playwright/test";

const apiBase = "**/vehicle-management/api";
const categoryId = "00000000-0000-0000-0000-000000000101";
const referenceCode = "GS-0123456789ABCDEF01234567";
const capabilityToken = "guest-capability-token";

function envelope(data: unknown) {
  return { data, message: "ok", success: true, timestamp: new Date().toISOString() };
}

function supportRequest(token: string | null = capabilityToken) {
  return {
    capabilityExpiresAt: "2030-11-02T00:00:00Z", capabilityToken: token, categoryId,
    content: "Tôi cần hướng dẫn đăng ký.", createdAt: "2026-10-26T00:00:00Z",
    maskedContact: "ba***@example.com", referenceCode, status: "OPEN", title: "Hỗ trợ đăng ký",
  };
}

async function mockBootstrap(page: Page, assistantEnabled = true, captcha = false) {
  await page.route(`${apiBase}/notifications/broadcast-announcements/active`, (route) => route.fulfill({
    contentType: "application/json", body: JSON.stringify(envelope([])),
  }));
  await page.route(`${apiBase}/public/application-time`, (route) => route.fulfill({
    contentType: "application/json", body: JSON.stringify(envelope({ timeZone: "Asia/Ho_Chi_Minh" })),
  }));
  await page.route(`${apiBase}/public/support/categories`, (route) => route.fulfill({
    contentType: "application/json", body: JSON.stringify(envelope([{
      categoryId, code: "ONBOARDING", description: "Hỗ trợ đăng ký", name: "Đăng ký và onboarding", priority: "NORMAL",
    }])),
  }));
  await page.route(`${apiBase}/public/support/assistant/status`, (route) => route.fulfill({
    contentType: "application/json", body: JSON.stringify(envelope({
      enabled: assistantEnabled, captchaEnabled: captcha, captchaProvider: captcha ? "TURNSTILE" : null,
      captchaSiteKey: captcha ? "test-site-key" : null,
      captchaRequiredForAssistant: captcha, captchaRequiredForIntake: captcha,
    })),
  }));
}

function expectNoAuthorization(request: Request) {
  expect(request.headers().authorization).toBeUndefined();
}

async function openGuestWidget(page: Page) {
  await page.goto("/help");
  await expect(page.getByRole("dialog", { name: "Trợ lý hỗ trợ CoParking", exact: true })).toBeVisible();
  await expect(page.getByText("Đang kiểm tra trạng thái trợ lý...")).toHaveCount(0);
}

async function openAction(page: Page, action: "Tạo phiếu hỗ trợ" | "Tra cứu phiếu") {
  await page.getByRole("button", { name: "Mở thêm tác vụ" }).click();
  await page.getByRole("button", { name: action, exact: true }).click();
  const dialog = page.getByRole("dialog", { name: action === "Tạo phiếu hỗ trợ" ? action : "Tra cứu phiếu hỗ trợ", exact: true });
  await expect(dialog).toBeVisible();
  return dialog;
}

async function fillIntake(page: Page) {
  await page.getByRole("button", { name: "Danh mục", exact: true }).click();
  await page.getByRole("button", { name: "Đăng ký và onboarding", exact: true }).click();
  await page.getByRole("textbox", { name: "Tiêu đề", exact: true }).fill("Hỗ trợ đăng ký");
  await page.getByRole("textbox", { name: "Nội dung", exact: true }).fill("Tôi cần hướng dẫn đăng ký.");
  await page.getByRole("textbox", { name: "Email", exact: true }).fill("baoan@example.com");
}

async function ask(page: Page, question = "Tôi cần đăng ký như thế nào?") {
  await page.getByLabel("Nhập tin nhắn hỗ trợ").fill(question);
  await page.getByRole("button", { name: "Gửi tin nhắn", exact: true }).click();
}

async function mockAnswer(page: Page, answer = "Bạn cần xác minh email rồi hoàn thiện hồ sơ.", handoff = false) {
  await page.route(`${apiBase}/public/support/assistant/messages`, (route) => route.fulfill({
    contentType: "application/json", body: JSON.stringify(envelope({
      answer, citations: [{ label: "C1", page: null, section: "Đăng ký", title: "Hướng dẫn công khai" }],
      diagnosticCode: null, handoffRecommended: handoff,
    })),
  }));
}

test("legacy help opens the guest widget without account API requests", async ({ page }, testInfo) => {
  await mockBootstrap(page);
  const publicRequests: Request[] = [];
  const privateRequests: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/api/public/support/")) publicRequests.push(request);
    if (/\/api\/(operations|ai)\//.test(request.url())) privateRequests.push(request.url());
  });
  await openGuestWidget(page);
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByText("không thể xem hay thay đổi dữ liệu tài khoản", { exact: false })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Bạn cần hỗ trợ trước khi đăng nhập?" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Đính kèm ảnh" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Đóng trợ lý", exact: true })).toBeVisible();
  expect(publicRequests.length).toBeGreaterThanOrEqual(1);
  publicRequests.forEach(expectNoAuthorization);
  expect(privateRequests).toEqual([]);
  await expect(page.getByRole("heading", { name: "Mỗi lượt xe rõ ràng. Mỗi vận hành an tâm." })).toBeVisible();
  await expect(page.locator(".tw-pointer-events-none.tw-transition-opacity[aria-hidden]")).toHaveCSS("opacity", "0");
  await page.screenshot({ path: testInfo.outputPath("guest-widget-desktop.png") });
});

test("floating support opens in place and PUBLIC requests contain no client scope or tools", async ({ page }) => {
  await mockBootstrap(page);
  let assistantBody: Record<string, unknown> | null = null;
  await page.route(`${apiBase}/public/support/assistant/messages`, (route) => {
    expectNoAuthorization(route.request());
    assistantBody = route.request().postDataJSON();
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({
      answer: "Hướng dẫn công khai", citations: [], diagnosticCode: null, handoffRecommended: false,
    })) });
  });
  await page.goto("/guide");
  await expect(page.getByRole("button", { name: "Trợ giúp", exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Mở Trợ lý hỗ trợ CoParking", exact: true }).click();
  await ask(page);
  await expect(page.getByRole("log")).toContainText("Hướng dẫn công khai");
  await expect(page).toHaveURL(/\/guide$/);
  expect(assistantBody).toMatchObject({ message: "Tôi cần đăng ký như thế nào?" });
  expect(assistantBody).not.toHaveProperty("scope");
  expect(assistantBody).not.toHaveProperty("tools");
});

test("chat survives close, SPA navigation and browser back, but refresh starts a new conversation", async ({ page }) => {
  await mockBootstrap(page);
  await mockAnswer(page);
  await openGuestWidget(page);
  await ask(page);
  await expect(page.getByRole("log")).toContainText("Bạn cần xác minh email rồi hoàn thiện hồ sơ.");
  await page.getByRole("button", { name: "Đóng trợ lý", exact: true }).click();
  await page.getByRole("button", { name: "Mở Trợ lý hỗ trợ CoParking", exact: true }).click();
  await expect(page.getByRole("log")).toContainText("Bạn cần xác minh email rồi hoàn thiện hồ sơ.");
  await page.getByRole("link", { name: "Hướng dẫn", exact: true }).first().click();
  await expect(page).toHaveURL(/\/guide$/);
  await expect(page.getByRole("log")).toContainText("Bạn cần xác minh email rồi hoàn thiện hồ sơ.");
  await page.goBack();
  await expect(page.getByRole("log")).toContainText("Bạn cần xác minh email rồi hoàn thiện hồ sơ.");
  await page.reload();
  await page.getByRole("button", { name: "Mở Trợ lý hỗ trợ CoParking", exact: true }).click();
  await expect(page.getByRole("log")).toBeEmpty();
  await expect(page.getByLabel("Nhập tin nhắn hỗ trợ")).toHaveValue("");
});

test("handoff opens a prefilled form and never creates a ticket until confirmed", async ({ page }) => {
  await mockBootstrap(page);
  await mockAnswer(page, "Chưa có đủ thông tin công khai.", true);
  let creates = 0;
  await page.route(`${apiBase}/public/support/requests`, (route) => { creates++; return route.fulfill({ status: 500 }); });
  await openGuestWidget(page);
  await ask(page);
  await page.getByRole("log").getByRole("button", { name: "Tạo phiếu hỗ trợ" }).click();
  await expect(page.getByRole("textbox", { name: "Nội dung", exact: true })).toHaveValue("Tôi cần đăng ký như thế nào?");
  expect(creates).toBe(0);
});

test("guest creates an intake and can still look it up after refresh", async ({ page }) => {
  await mockBootstrap(page);
  let idempotencyKey = "";
  await page.route(`${apiBase}/public/support/requests`, (route) => {
    expectNoAuthorization(route.request());
    idempotencyKey = route.request().headers()["idempotency-key"] ?? "";
    return route.fulfill({ contentType: "application/json", status: 201, body: JSON.stringify(envelope(supportRequest())) });
  });
  await page.route(`${apiBase}/public/support/requests/access`, (route) => {
    expectNoAuthorization(route.request());
    expect(route.request().postDataJSON()).toEqual({ referenceCode, capabilityToken });
    expect(route.request().url()).not.toContain(capabilityToken);
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(supportRequest(null))) });
  });
  await openGuestWidget(page);
  await openAction(page, "Tạo phiếu hỗ trợ");
  await fillIntake(page);
  await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect(page.getByText(`Đã tiếp nhận: ${referenceCode}`)).toBeVisible();
  await expect(page.getByLabel("Tiêu đề", { exact: true })).toHaveValue("");
  expect(idempotencyKey).toMatch(/^[0-9a-f-]{36}$/i);
  const saved = await page.evaluate(() => sessionStorage.getItem("coparking.guest-support.capability"));
  expect(JSON.parse(saved ?? "{}")).toEqual({ referenceCode, capabilityToken });
  await page.reload();
  await page.getByRole("button", { name: "Mở Trợ lý hỗ trợ CoParking", exact: true }).click();
  await openAction(page, "Tra cứu phiếu");
  await expect(page.getByLabel("Mã yêu cầu", { exact: true })).toHaveValue(referenceCode);
  await page.getByRole("button", { name: "Tra cứu", exact: true }).click();
  await expect(page.getByText(`${referenceCode} · Đã tiếp nhận`)).toBeVisible();
  await expect(page.getByText("Liên hệ: ba***@example.com")).toBeVisible();
});

test("intake retry reuses its key and edited payload receives a new key", async ({ page }) => {
  await mockBootstrap(page);
  const keys: string[] = [];
  await page.route(`${apiBase}/public/support/requests`, (route) => {
    keys.push(route.request().headers()["idempotency-key"]);
    return route.fulfill({ status: 503, contentType: "application/json", body: JSON.stringify({ message: "Tạm thời không khả dụng" }) });
  });
  await openGuestWidget(page);
  await openAction(page, "Tạo phiếu hỗ trợ");
  await fillIntake(page);
  for (let index = 0; index < 2; index++) {
    await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
    await expect.poll(() => keys.length).toBe(index + 1);
    await expect(page.getByRole("button", { name: "Gửi yêu cầu", exact: true })).toBeEnabled();
  }
  await page.getByLabel("Tiêu đề", { exact: true }).fill("Hỗ trợ đăng ký mới");
  await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect.poll(() => keys.length).toBe(3);
  expect(keys[0]).toBe(keys[1]);
  expect(keys[2]).not.toBe(keys[0]);
});

test("disabled assistant still permits intake and expired credentials are cleared", async ({ page }) => {
  await mockBootstrap(page, false);
  await page.route(`${apiBase}/public/support/requests`, (route) => route.fulfill({
    contentType: "application/json", status: 429, body: JSON.stringify({ message: "Bạn đã gửi quá nhiều yêu cầu." }),
  }));
  await page.route(`${apiBase}/public/support/requests/access`, (route) => route.fulfill({
    contentType: "application/json", status: 404, body: JSON.stringify({ message: "Không tìm thấy yêu cầu" }),
  }));
  await page.addInitScript(({ referenceCode, capabilityToken }) => {
    sessionStorage.setItem("coparking.guest-support.capability", JSON.stringify({ referenceCode, capabilityToken }));
  }, { referenceCode, capabilityToken });
  await openGuestWidget(page);
  await expect(page.getByText("Trợ lý đang tạm ngưng.", { exact: false })).toBeVisible();
  await expect(page.getByRole("button", { name: "Gửi tin nhắn", exact: true })).toBeDisabled();
  const dialog = await openAction(page, "Tạo phiếu hỗ trợ");
  await fillIntake(page);
  await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect(dialog.getByRole("alert")).toContainText("Bạn đã gửi quá nhiều yêu cầu.");
  await dialog.getByRole("button", { name: "Đóng", exact: true }).last().click();
  const lookup = await openAction(page, "Tra cứu phiếu");
  await page.getByRole("button", { name: "Tra cứu", exact: true }).click();
  await expect(lookup.getByRole("alert")).toContainText("Mã truy cập không đúng hoặc đã hết hạn.");
  expect(await page.evaluate(() => sessionStorage.getItem("coparking.guest-support.capability"))).toBeNull();
});

test("offline AI keeps the failed question and intake available", async ({ page }) => {
  await mockBootstrap(page);
  await page.route(`${apiBase}/public/support/assistant/messages`, (route) => route.abort("internetdisconnected"));
  await openGuestWidget(page);
  await ask(page, "Tôi cần trợ giúp");
  await expect(page.getByRole("alert")).toContainText("Không thể kết nối máy chủ");
  await expect(page.getByLabel("Nhập tin nhắn hỗ trợ")).toHaveValue("Tôi cần trợ giúp");
  await openAction(page, "Tạo phiếu hỗ trợ");
  await expect(page.getByRole("button", { name: "Gửi yêu cầu", exact: true })).toBeEnabled();
});

test("an unavailable assistant status does not disable the existing intake form", async ({ page }) => {
  await mockBootstrap(page);
  await page.route(`${apiBase}/public/support/assistant/status`, (route) => route.fulfill({
    status: 503, contentType: "application/json", body: JSON.stringify({ message: "Tạm ngưng" }),
  }));
  await page.route(`${apiBase}/public/support/requests`, (route) => route.fulfill({
    status: 201, contentType: "application/json", body: JSON.stringify(envelope(supportRequest())),
  }));
  await openGuestWidget(page);
  await expect(page.getByRole("alert")).toContainText("Không thể kiểm tra dịch vụ hỗ trợ");
  await openAction(page, "Tạo phiếu hỗ trợ");
  await fillIntake(page);
  await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect(page.getByText(`Đã tiếp nhận: ${referenceCode}`)).toBeVisible();
});

test("CAPTCHA assistant and intake proofs remain separate and are reset after use", async ({ page }) => {
  await page.addInitScript(() => {
    window.turnstile = {
      render: (_element: HTMLElement, options: Record<string, unknown>) => {
        queueMicrotask(() => (options.callback as (token: string) => void)(`${String(options.action)}-provider-token`));
        return String(options.action);
      }, remove: () => undefined,
    };
  });
  await mockBootstrap(page, true, true);
  const issuedPurposes: string[] = [];
  await page.route(`${apiBase}/public/support/captcha/verify`, (route) => {
    const body = route.request().postDataJSON();
    issuedPurposes.push(body.purpose);
    expectNoAuthorization(route.request());
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({ proof: `${body.purpose}-proof`, expiresAt: "2030-01-01T00:00:00Z" })) });
  });
  await page.route(`${apiBase}/public/support/assistant/messages`, (route) => {
    expect(route.request().postDataJSON().captchaProof).toBe("ASSISTANT-proof");
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({ answer: "PUBLIC only", citations: [], handoffRecommended: false, diagnosticCode: null })) });
  });
  await page.route(`${apiBase}/public/support/requests`, (route) => {
    expect(route.request().postDataJSON().captchaProof).toBe("INTAKE-proof");
    return route.fulfill({ contentType: "application/json", status: 201, body: JSON.stringify(envelope(supportRequest())) });
  });
  await openGuestWidget(page);
  await expect(page.getByText("CAPTCHA đã xác minh.")).toBeVisible();
  await ask(page);
  await expect(page.getByRole("log")).toContainText("PUBLIC only");
  await expect.poll(() => issuedPurposes.filter((purpose) => purpose === "ASSISTANT").length).toBeGreaterThanOrEqual(2);
  const create = await openAction(page, "Tạo phiếu hỗ trợ");
  await expect(create.getByText("CAPTCHA đã xác minh.")).toBeVisible();
  await fillIntake(page);
  await page.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect(page.getByText(`Đã tiếp nhận: ${referenceCode}`)).toBeVisible();
  await expect.poll(() => issuedPurposes.filter((purpose) => purpose === "INTAKE").length).toBeGreaterThanOrEqual(2);
});

test("mobile header omits Help while floating support and intake stay available", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockBootstrap(page);
  await page.goto("/guide");
  await page.getByRole("button", { name: "Mở menu điều hướng" }).click();
  await expect(page.getByRole("button", { name: "Trợ giúp", exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Đóng menu", exact: true }).click();
  await page.getByRole("button", { name: "Mở Trợ lý hỗ trợ CoParking", exact: true }).click();
  const panel = page.getByRole("dialog", { name: "Trợ lý hỗ trợ CoParking", exact: true });
  await expect(panel).toBeVisible();
  const box = await panel.boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(390);
  const create = await openAction(page, "Tạo phiếu hỗ trợ");
  await fillIntake(page);
  await expect(create.getByRole("button", { name: "Gửi yêu cầu", exact: true })).toBeInViewport();
  await expect(page.locator(".tw-pointer-events-none.tw-transition-opacity[aria-hidden]")).toHaveCSS("opacity", "0");
  await create.evaluate(async (element) => {
    await Promise.all(element.getAnimations({ subtree: true })
      .filter((animation) => animation.effect?.getTiming().iterations !== Infinity)
      .map((animation) => animation.finished.catch(() => undefined)));
  });
  await page.screenshot({ path: testInfo.outputPath("guest-intake-mobile.png") });
});

test("an in-flight guest answer survives navigation without duplicate submission", async ({ page }) => {
  await mockBootstrap(page);
  let release!: () => void;
  const pending = new Promise<void>((resolve) => { release = resolve; });
  let calls = 0;
  await page.route(`${apiBase}/public/support/assistant/messages`, async (route) => {
    calls++;
    await pending;
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({
      answer: "Câu trả lời sau khi chuyển trang", citations: [], handoffRecommended: false, diagnosticCode: null,
    })) });
  });
  await openGuestWidget(page);
  await ask(page);
  await expect(page.getByText("Trợ lý đang xử lý...")).toBeVisible();
  await page.getByRole("link", { name: "Hướng dẫn", exact: true }).first().click();
  await page.getByRole("button", { name: "Đóng trợ lý", exact: true }).click();
  release();
  await page.getByRole("button", { name: "Mở Trợ lý hỗ trợ CoParking", exact: true }).click();
  await expect(page.getByRole("log")).toContainText("Câu trả lời sau khi chuyển trang");
  expect(calls).toBe(1);
});

test("authenticated widget keeps its account endpoints and ticket actions", async ({ page }) => {
  await mockBootstrap(page);
  const accountCalls: string[] = [];
  let publicCalls = 0;
  await page.route(`${apiBase}/operations/support-tickets/assistant-conversation`, (route) => {
    accountCalls.push("conversation");
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({ conversationId: "conversation-a", lastMessageId: null })) });
  });
  await page.route(`${apiBase}/ai/assistant/status`, (route) => {
    accountCalls.push("status");
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({ enabled: true })) });
  });
  await page.route(`${apiBase}/operations/chat/conversations/conversation-a/messages**`, (route) => {
    accountCalls.push("messages");
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope([])) });
  });
  page.on("request", (request) => { if (request.url().includes("/public/support/")) publicCalls++; });
  await page.goto("/tests/support-widget-harness.html");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await page.getByRole("button", { name: "Open support", exact: true }).click();
  await expect(page.getByLabel("Nhập tin nhắn hỗ trợ")).toBeEnabled();
  await page.getByRole("button", { name: "Mở thêm tác vụ" }).click();
  await expect(page.getByRole("button", { name: "Phiếu của tôi", exact: true })).toBeVisible();
  expect(accountCalls).toEqual(expect.arrayContaining(["conversation", "status", "messages"]));
  expect(publicCalls).toBe(0);
});

test("authentication failure and missing account permission never fall back to guest support", async ({ page }) => {
  await mockBootstrap(page);
  await page.goto("/tests/support-widget-harness.html");
  await page.getByRole("button", { name: "Authentication error", exact: true }).click();
  await expect(page.locator("[data-support-floating-button]")).toHaveCount(0);
  await page.getByRole("button", { name: "Sign in without permission", exact: true }).click();
  await expect(page.locator("[data-support-floating-button]")).toHaveCount(0);
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await expect(page.locator("[data-support-floating-button]")).toHaveCount(1);
});

test("sign-in discards an in-flight guest answer and sign-out starts a clean chat", async ({ page }) => {
  await mockBootstrap(page);
  let release!: () => void;
  const pending = new Promise<void>((resolve) => { release = resolve; });
  await page.route(`${apiBase}/public/support/assistant/messages`, async (route) => {
    await pending;
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({
      answer: "Late guest answer", citations: [], handoffRecommended: false, diagnosticCode: null,
    })) });
  });
  await page.goto("/tests/support-widget-harness.html");
  await page.getByRole("button", { name: "Open support", exact: true }).click();
  await ask(page);
  await expect(page.getByText("Trợ lý đang xử lý...")).toBeVisible();
  await page.getByRole("button", { name: "Sign in without permission", exact: true }).click();
  release();
  await page.getByRole("button", { name: "Sign out", exact: true }).click();
  await page.getByRole("button", { name: "Open support", exact: true }).click();
  await expect(page.getByRole("log")).toBeEmpty();
  await expect(page.getByText("Trợ lý đang xử lý...")).toHaveCount(0);
  await expect(page.getByLabel("Nhập tin nhắn hỗ trợ")).toHaveValue("");
});
