import { expect, test, type Page } from "@playwright/test";

const apiBase = "**/vehicle-management/api";
const actorId = "00000000-0000-0000-0000-0000000000c1";
const envelope = (data: unknown) => ({ data, message: "ok", success: true, timestamp: new Date().toISOString() });

const parkingLot = {
  parkingLotId: "lot-1", code: "LOT-1", name: "Bãi xe Nông Lâm",
  addressDisplay: "Thành phố Hồ Chí Minh", latitude: 10.87, longitude: 106.8,
  status: "ACTIVE", totalCapacity: 100, visitorAvailable: 30, hasAvailability: true,
  minimumPrice: 4000, maximumPrice: 80000, completedSessionCount: 72,
  averageRating: 4.5, reviewCount: 2, snapshotAt: "2026-11-04T00:00:00Z", vehicleTypes: [],
};

const reviewPage = {
  summary: { averageRating: 4.5, reviewCount: 2, distribution: { 1: 0, 2: 0, 3: 0, 4: 1, 5: 1 } },
  content: [{
    parkingLotName: "Bãi xe Nông Lâm", reviewerDisplayName: "N*** A***",
    reviewerCustomerId: "00000000-0000-0000-0000-0000000000c2", rating: 5,
    comment: "Dịch vụ tốt", verified: true, createdAt: "2026-11-03T10:00:00Z",
    updatedAt: null, edited: false,
  }],
  page: 0, size: 4, totalElements: 2, totalPages: 1, first: true, last: true,
};

async function mockPricing(page: Page, onReviewRequest?: (rating: string) => void) {
  await page.route(`${apiBase}/**`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([])),
  }));
  await page.route(`${apiBase}/public/parking-lots/directory`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([parkingLot])),
  }));
  await page.route(`${apiBase}/public/parking-lots/lot-1/reviews?**`, (route) => {
    onReviewRequest?.(new URL(route.request().url()).searchParams.get("rating") ?? "all");
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(reviewPage)) });
  });
}

async function seedSession(page: Page, roleCode = "CUSTOMER", permissionCodes: string[] = []) {
  const username = roleCode === "CUSTOMER" ? "customer-reviewer" : "partner-review-manager";
  await page.addInitScript(({ accountId, seededUsername }) => {
    const authority = "http://localhost:8081/realms/vehicle-management";
    const clientId = "vehicle-management-frontend";
    const encode = (value: object) => btoa(JSON.stringify(value)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
    const accessToken = `${encode({ alg: "none" })}.${encode({
      account_id: accountId,
      exp: Math.floor(Date.now() / 1000) + 3600,
      preferred_username: seededUsername,
    })}.signature`;
    sessionStorage.setItem(`oidc.user:${authority}:${clientId}`, JSON.stringify({
      access_token: accessToken,
      expires_at: Math.floor(Date.now() / 1000) + 3600,
      profile: {
        aud: clientId,
        exp: Math.floor(Date.now() / 1000) + 3600,
        iat: Math.floor(Date.now() / 1000),
        iss: authority,
        preferred_username: seededUsername,
        sub: accountId,
      },
      scope: "openid profile email roles",
      token_type: "Bearer",
    }));
  }, { accountId: actorId, seededUsername: username });

  await page.route(`${apiBase}/iam/accounts/current-access`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope({
      accountId: actorId,
      accountStatus: "ACTIVE",
      accessRevision: "review-revision-1",
      accessState: "ACTIVE",
      customerApprovalStatus: "APPROVED",
      customerStatus: "ACTIVE",
      onboardingRequired: false,
      permissionCodes,
      requiredAction: null,
      roleCode,
      username,
    })),
  }));
  await page.route(`${apiBase}/iam/accounts/onboarding`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope({ onboardingRequired: false, profile: { fullName: "Khách đánh giá" } })),
  }));
}

test("pricing detail shows aggregate, verified review and rating filter", async ({ page }) => {
  const requestedRatings: string[] = [];
  await mockPricing(page, (rating) => requestedRatings.push(rating));

  await page.goto("/pricing/lot-1");
  const reviewSection = page.locator("#parking-reviews");
  await expect(reviewSection.getByRole("heading", { name: "Đánh giá Bãi xe Nông Lâm" })).toBeVisible();
  await expect(reviewSection.getByText("4.5", { exact: true })).toBeVisible();
  await expect(reviewSection.getByText("N*** A***")).toBeVisible();
  await expect(reviewSection.getByText("Dịch vụ tốt")).toBeVisible();
  await reviewSection.getByRole("button", { name: "5 sao, 1 đánh giá" }).click();
  expect(requestedRatings[0]).toBe("all");
  await expect.poll(() => requestedRatings.at(-1)).toBe("5");
});

test("pricing hero action scrolls to the embedded review section", async ({ page }) => {
  await mockPricing(page);

  await page.goto("/pricing/lot-1");
  const reviewSection = page.locator("#parking-reviews");
  await expect(reviewSection).toBeVisible();
  await page.evaluate(() => window.scrollTo({ top: 0 }));
  await page.getByRole("link", { name: "Xem đánh giá" }).click();
  await expect(page).toHaveURL(/#parking-reviews$/);
  await expect.poll(async () => {
    const box = await reviewSection.boundingBox();
    return box ? Math.abs(box.y) < 140 : false;
  }).toBe(true);
});

test("customer creates a review in a modal on the pricing page", async ({ page }) => {
  await mockPricing(page);
  await seedSession(page);
  let submitted: unknown;
  await page.route(`${apiBase}/parking/parking-lots/lot-1/reviews/me`, async (route) => {
    if (route.request().method() === "PUT") {
      submitted = route.request().postDataJSON();
      return route.fulfill({
        contentType: "application/json",
        body: JSON.stringify(envelope({
          rating: 4, comment: "Bãi sạch và nhân viên hỗ trợ tốt.", createdAt: "2026-11-04T01:00:00Z",
          updatedAt: "2026-11-04T01:00:00Z", edited: false,
        })),
      });
    }
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(envelope({
        eligible: true, reasonCode: "ELIGIBLE_TRANSACTION", parkingLotId: "lot-1",
        parkingLotName: "Bãi xe Nông Lâm", currentReview: null,
      })),
    });
  });

  await page.goto("/pricing/lot-1#write-review");
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByRole("heading", { name: "Viết đánh giá bãi xe" })).toBeVisible();
  await dialog.getByRole("button", { name: "4 sao" }).click();
  await dialog.getByLabel(/Nhận xét/).fill("Bãi sạch và nhân viên hỗ trợ tốt.");
  await dialog.getByRole("button", { name: "Gửi đánh giá" }).click();
  await expect(dialog).toBeHidden();
  await expect(page).toHaveURL(/#parking-reviews$/);
  const editButton = page.locator("#parking-reviews").getByRole("button", { name: "Sửa đánh giá" });
  await expect(editButton).toBeVisible();
  await editButton.click();
  await expect(dialog.getByRole("heading", { name: "Sửa đánh giá bãi xe" })).toBeVisible();
  await expect(dialog.getByRole("button", { name: "4 sao" })).toHaveAttribute("aria-pressed", "true");
  await expect(dialog.getByLabel(/Nhận xét/)).toHaveValue("Bãi sạch và nhân viên hỗ trợ tốt.");
  expect(submitted).toEqual({ rating: 4, comment: "Bãi sạch và nhân viên hỗ trợ tốt." });
});

test("partner review management uses project dropdowns and renders the dashboard", async ({ page }) => {
  const requestedRatings: string[] = [];
  let contactedCustomerId = "";
  await page.route(`${apiBase}/**`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([])),
  }));
  await seedSession(page, "PARTNER_ADMIN", [
    "PARKING_LOT_REVIEW_READ_ALL",
    "CHAT_CONVERSATION_CREATE_CUSTOMER_DIRECT",
    "CHAT_CONVERSATION_READ_OWN",
  ]);
  await page.route(`${apiBase}/parking/parking-lots`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([parkingLot])),
  }));
  await page.route(`${apiBase}/parking/parking-lot-reviews?**`, (route) => {
    requestedRatings.push(new URL(route.request().url()).searchParams.get("rating") ?? "all");
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(envelope({
        ...reviewPage,
        summary: { averageRating: 3.7, reviewCount: 3, distribution: { 1: 0, 2: 1, 3: 0, 4: 1, 5: 1 } },
        totalElements: 3,
      })),
    });
  });
  await page.route(`${apiBase}/operations/chat/conversations/customer-support`, (route) => {
    contactedCustomerId = String(route.request().postDataJSON()?.customerId ?? "");
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify(envelope({
        conversationId: "conversation-review-1",
        conversationType: "CUSTOMER_DIRECT",
        participants: [],
        status: "ACTIVE",
      })),
    });
  });

  await page.goto("/admin/parking-reviews");
  await expect(page.getByRole("heading", { name: "Đánh giá bãi xe" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Cơ cấu phản hồi" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "Hành động đề xuất" })).toBeVisible();
  await expect(page.getByText("Dịch vụ tốt")).toBeVisible();
  const popupPromise = page.waitForEvent("popup");
  await page.getByRole("button", { name: "Chat với N*** A***" }).click();
  const chatPage = await popupPromise;
  await expect.poll(() => contactedCustomerId).toBe("00000000-0000-0000-0000-0000000000c2");
  await expect(chatPage).toHaveURL(/\/admin\/support-center\?conversationId=conversation-review-1/);
  await chatPage.close();
  await page.getByRole("button", { name: "Lọc theo số sao" }).click();
  await page.getByRole("button", { name: "2 sao", exact: true }).click();
  await expect.poll(() => requestedRatings.at(-1)).toBe("2");
});
