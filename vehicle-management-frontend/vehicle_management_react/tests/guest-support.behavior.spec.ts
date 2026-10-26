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
    capabilityExpiresAt: "2026-11-02T00:00:00Z",
    capabilityToken: token,
    categoryId,
    content: "Tôi cần hướng dẫn đăng ký.",
    createdAt: "2026-10-26T00:00:00Z",
    maskedContact: "ba***@example.com",
    referenceCode,
    status: "OPEN",
    title: "Hỗ trợ đăng ký",
  };
}

async function mockBootstrap(page: Page, assistantEnabled = true, captcha = false) {
  await page.route(`${apiBase}/notifications/broadcast-announcements/active`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([])),
  }));
  await page.route(`${apiBase}/public/application-time`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope({ timeZone: "Asia/Ho_Chi_Minh" })),
  }));
  await page.route(`${apiBase}/public/support/categories`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([{
      categoryId,
      code: "ONBOARDING",
      description: "Hỗ trợ đăng ký",
      name: "Đăng ký và onboarding",
      priority: "NORMAL",
    }])),
  }));
  await page.route(`${apiBase}/public/support/assistant/status`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope({
      enabled: assistantEnabled,
      captchaEnabled: captcha,
      captchaProvider: captcha ? "TURNSTILE" : null,
      captchaSiteKey: captcha ? "test-site-key" : null,
      captchaRequiredForAssistant: captcha,
      captchaRequiredForIntake: captcha,
    })),
  }));
}

function expectNoAuthorization(request: Request) {
  expect(request.headers().authorization).toBeUndefined();
}

test("guest opens public help without account-only actions or authorization", async ({ page }) => {
  await mockBootstrap(page);
  const publicRequests: Request[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/api/public/support/")) publicRequests.push(request);
  });

  await page.goto("/help");

  await expect(page.getByRole("heading", { name: "Bạn cần hỗ trợ trước khi đăng nhập?" })).toBeVisible();
  await expect(page.getByRole("option", { name: "Đăng ký và onboarding" })).toBeAttached();
  await expect(page.getByRole("link", { name: "Đăng nhập", exact: true })).toBeVisible();
  await expect(page.getByText("không thể xem hay thay đổi dữ liệu tài khoản")).toBeVisible();
  await expect(page.getByRole("link", { name: /Ví của tôi|Xe của tôi|Vé tháng/ })).toHaveCount(0);
  expect(publicRequests.length).toBeGreaterThanOrEqual(2);
  publicRequests.forEach(expectNoAuthorization);
});

test("guest asks the PUBLIC assistant without client-controlled scope", async ({ page }) => {
  await mockBootstrap(page);
  let assistantBody: Record<string, unknown> | null = null;
  await page.route(`${apiBase}/public/support/assistant/messages`, async (route) => {
    expectNoAuthorization(route.request());
    assistantBody = route.request().postDataJSON() as Record<string, unknown>;
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(envelope({
        answer: "Bạn cần xác minh email rồi hoàn thiện hồ sơ.",
        citations: [{ label: "C1", page: null, section: "Đăng ký", title: "Hướng dẫn công khai" }],
        diagnosticCode: null,
        handoffRecommended: false,
      })),
    });
  });

  await page.goto("/help");
  await page.getByLabel("Câu hỏi").fill("Tôi cần đăng ký như thế nào?");
  await page.getByRole("button", { name: "Hỏi trợ lý" }).click();

  await expect(page.getByText("Bạn cần xác minh email rồi hoàn thiện hồ sơ.")).toBeVisible();
  expect(assistantBody).not.toBeNull();
  expect(assistantBody).toMatchObject({ message: "Tôi cần đăng ký như thế nào?" });
  expect(assistantBody).not.toHaveProperty("scope");
  expect(assistantBody).not.toHaveProperty("tools");
});

test("guest creates and reads an intake only with its capability", async ({ page }) => {
  await mockBootstrap(page);
  let idempotencyKey = "";
  await page.route(`${apiBase}/public/support/requests`, async (route) => {
    expectNoAuthorization(route.request());
    idempotencyKey = route.request().headers()["idempotency-key"] ?? "";
    await route.fulfill({
      contentType: "application/json",
      status: 201,
      body: JSON.stringify(envelope(supportRequest())),
    });
  });
  await page.route(`${apiBase}/public/support/requests/access`, async (route) => {
    expectNoAuthorization(route.request());
    expect(route.request().postDataJSON()).toEqual({ referenceCode, capabilityToken });
    expect(route.request().url()).not.toContain(capabilityToken);
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(supportRequest(null))) });
  });

  await page.goto("/help");
  await page.getByLabel("Danh mục").selectOption(categoryId);
  await page.getByLabel("Tiêu đề").fill("Hỗ trợ đăng ký");
  await page.getByLabel("Nội dung").fill("Tôi cần hướng dẫn đăng ký.");
  await page.getByLabel("Email").fill("baoan@example.com");
  await page.getByRole("button", { name: "Gửi yêu cầu" }).click();

  await expect(page.getByText(`Đã tiếp nhận: ${referenceCode}`)).toBeVisible();
  expect(idempotencyKey).toMatch(/^[0-9a-f-]{36}$/i);
  const saved = await page.evaluate(() => sessionStorage.getItem("coparking.guest-support.capability"));
  expect(JSON.parse(saved ?? "{}")).toEqual({ referenceCode, capabilityToken });

  await page.getByRole("button", { name: "Tra cứu" }).click();
  await expect(page.getByText(`${referenceCode} · OPEN`)).toBeVisible();
  await expect(page.getByText("Liên hệ: ba***@example.com")).toBeVisible();
});

test("guest sees rate-limit, disabled assistant, and expired capability states", async ({ page }) => {
  await mockBootstrap(page, false);
  await page.route(`${apiBase}/public/support/requests`, (route) => route.fulfill({
    contentType: "application/json",
    status: 429,
    body: JSON.stringify({ message: "Bạn đã gửi quá nhiều yêu cầu." }),
  }));
  await page.route(`${apiBase}/public/support/requests/access`, (route) => route.fulfill({
    contentType: "application/json",
    status: 404,
    body: JSON.stringify({ message: "Không tìm thấy yêu cầu hỗ trợ hoặc quyền truy cập đã hết hạn" }),
  }));

  await page.goto("/help");
  await expect(page.getByText("Trợ lý đang tạm ngưng.", { exact: false })).toBeVisible();
  await expect(page.getByRole("button", { name: "Hỏi trợ lý" })).toBeDisabled();

  await page.getByLabel("Danh mục").selectOption(categoryId);
  await page.getByLabel("Tiêu đề").fill("Hỗ trợ đăng ký");
  await page.getByLabel("Nội dung").fill("Tôi cần hướng dẫn đăng ký.");
  await page.getByLabel("Email").fill("baoan@example.com");
  await page.getByRole("button", { name: "Gửi yêu cầu" }).click();
  await expect(page.getByRole("alert")).toContainText("Bạn đã gửi quá nhiều yêu cầu.");

  await page.getByLabel("Mã yêu cầu").fill(referenceCode);
  await page.getByLabel("Mã truy cập").fill("expired-token");
  await page.getByRole("button", { name: "Tra cứu" }).click();
  await expect(page.getByRole("alert")).toContainText("Mã truy cập không đúng hoặc đã hết hạn.");
  await expect.poll(() => page.evaluate(() => sessionStorage.getItem("coparking.guest-support.capability"))).toBeNull();
});

test("guest sees an offline state while support intake remains available", async ({ page }) => {
  await mockBootstrap(page);
  await page.route(`${apiBase}/public/support/assistant/messages`, (route) => route.abort("internetdisconnected"));

  await page.goto("/help");
  await page.getByLabel("Câu hỏi").fill("Tôi cần trợ giúp");
  await page.getByRole("button", { name: "Hỏi trợ lý" }).click();

  await expect(page.getByRole("alert")).toContainText("Không thể kết nối máy chủ");
  await expect(page.getByRole("button", { name: "Gửi yêu cầu" })).toBeEnabled();
});

test("required CAPTCHA is verified for distinct assistant and intake purposes and reset after use", async ({ page }) => {
  await page.addInitScript(() => {
    (window as unknown as { turnstile: unknown }).turnstile = {
      render: (_element: HTMLElement, options: Record<string, unknown>) => {
        const callback = options.callback as (token: string) => void;
        queueMicrotask(() => callback(`${String(options.action)}-provider-token`));
        return String(options.action);
      },
      remove: () => undefined,
    };
  });
  await mockBootstrap(page, true, true);
  const issuedPurposes: string[] = [];
  await page.route(`${apiBase}/public/support/captcha/verify`, async (route) => {
    const body = route.request().postDataJSON() as { purpose: string; challengeToken: string };
    issuedPurposes.push(body.purpose);
    expect(body.challengeToken).toContain(body.purpose === "INTAKE" ? "support_intake" : "support_assistant");
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(envelope({ proof: `${body.purpose.toLowerCase()}-proof`, expiresAt: new Date(Date.now() + 60_000).toISOString() })),
    });
  });
  let assistantProof = "";
  await page.route(`${apiBase}/public/support/assistant/messages`, async (route) => {
    assistantProof = (route.request().postDataJSON() as { captchaProof: string }).captchaProof;
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope({
      answer: "PUBLIC only", citations: [], diagnosticCode: null, handoffRecommended: false,
    })) });
  });
  let intakeProof = "";
  await page.route(`${apiBase}/public/support/requests`, async (route) => {
    intakeProof = (route.request().postDataJSON() as { captchaProof: string }).captchaProof;
    await route.fulfill({ contentType: "application/json", status: 201, body: JSON.stringify(envelope(supportRequest())) });
  });

  await page.goto("/help");
  await expect(page.getByText("CAPTCHA đã xác minh.")).toHaveCount(2);
  await page.getByLabel("Câu hỏi").fill("Cách đăng ký?");
  await page.getByRole("button", { name: "Hỏi trợ lý" }).click();
  await expect(page.getByText("PUBLIC only")).toBeVisible();
  expect(assistantProof).toBe("assistant-proof");

  await page.getByLabel("Danh mục").selectOption(categoryId);
  await page.getByLabel("Tiêu đề").fill("Hỗ trợ đăng ký");
  await page.getByLabel("Nội dung").fill("Tôi cần hướng dẫn đăng ký.");
  await page.getByLabel("Email").fill("baoan@example.com");
  await page.getByRole("button", { name: "Gửi yêu cầu" }).click();
  await expect(page.getByText(`Đã tiếp nhận: ${referenceCode}`)).toBeVisible();
  expect(intakeProof).toBe("intake-proof");
  expect(issuedPurposes).toEqual(expect.arrayContaining(["ASSISTANT", "INTAKE"]));
});
