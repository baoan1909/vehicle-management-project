import { spawn } from "node:child_process";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { chromium } from "@playwright/test";

const PORT = 4173;
const BASE_URL = `http://127.0.0.1:${PORT}`;
const SCREENSHOT_DIR = mkdtempSync(join(tmpdir(), "coparking-customer-visual-"));

const allViewports = [
  { height: 568, name: "320x568", width: 320 },
  { height: 800, name: "360x800", width: 360 },
  { height: 844, name: "390x844", width: 390 },
  { height: 896, name: "414x896", width: 414 },
  { height: 1024, name: "768x1024", width: 768 },
  { height: 720, name: "1280x720", width: 1280 },
  { height: 900, name: "1440x900", width: 1440 },
];

const allRoutes = [
  { path: "/", tag: "about" },
  { path: "/pricing", tag: "pricing" },
  { path: "/guide", tag: "guide" },
  { path: "/contact", tag: "contact" },
  { path: "/login", tag: "login" },
  { path: "/become-a-partner", tag: "partner" },
  { path: "/parking-lots/nearby", tag: "nearby" },
  { path: "/customer/dashboard", tag: "dashboard", protected: true },
  { path: "/customer/profile", tag: "profile", protected: true },
  { path: "/customer/vehicles", tag: "vehicles", protected: true },
  { path: "/customer/subscriptions", tag: "subscriptions", protected: true },
  { path: "/customer/wallet", tag: "wallet", protected: true },
  { path: "/customer/wallet/topup", tag: "wallet-topup", protected: true },
  { path: "/customer/wallet/topup-result", tag: "wallet-result", protected: true },
  { path: "/customer/parking-history", tag: "history", protected: true },
  { path: "/customer/support", tag: "support", protected: true },
  { path: "/customer/support/chat", tag: "support-chat", protected: true },
];
const routeFilter = process.argv[2];
const viewportFilter = process.argv[3];
const requestedRoutes = routeFilter && routeFilter !== "all" ? new Set(routeFilter.split(",")) : null;
const ROUTES = requestedRoutes ? allRoutes.filter((route) => requestedRoutes.has(route.tag)) : allRoutes;
const VIEWPORTS = viewportFilter ? allViewports.filter((viewport) => viewport.name === viewportFilter) : allViewports;

const failures = [];

function slug(text) {
  return text.replace(/[^a-z0-9]+/gi, "-").toLowerCase();
}

const customerPermissions = [
  "USER_PROFILE_READ_OWN",
  "CUSTOMER_VEHICLE_READ_OWN",
  "CUSTOMER_VEHICLE_CREATE_OWN",
  "CUSTOMER_VEHICLE_UPDATE_OWN",
  "SUBSCRIPTION_READ_OWN",
  "SUBSCRIPTION_CREATE_OWN",
  "WALLET_READ_OWN",
  "WALLET_TOP_UP_OWN",
  "WALLET_PAY_INVOICE_OWN",
  "PARKING_SESSION_READ_OWN",
  "SUPPORT_TICKET_READ_OWN",
  "SUPPORT_TICKET_CREATE_OWN",
  "CHAT_CONVERSATION_READ_OWN",
  "CHAT_MESSAGE_CREATE_OWN",
  "CHAT_MESSAGE_SEND_OWN",
  "SUPPORT_WIDGET_ACCESS_OWN",
];

const profile = {
  account: {
    accountId: "account-mobile-qa",
    accountStatus: "ACTIVE",
    email: "mobile.qa@coparking.vn",
    permissionCodes: customerPermissions,
    roleCode: "CUSTOMER",
    username: "mobile.qa",
  },
  customer: {
    customerApprovalStatus: "APPROVED",
    customerCode: "CUS-MOBILE-QA",
    customerId: "customer-mobile-qa",
    customerStatus: "ACTIVE",
    customerType: "VIP",
  },
  onboardingRequired: false,
  profile: {
    addressDisplay: "Quận 1, Thành phố Hồ Chí Minh",
    fullName: "Nguyễn Khách Hàng Mobile",
    phoneNumber: "0901234567",
    userProfileStatus: "ACTIVE",
  },
};

const sampleVehicle = {
  customerVehicleId: "vehicle-mobile-qa",
  licensePlate: "59A-123.45",
  vehicleTypeId: "vehicle-type-car",
  vehicleTypeCode: "CAR",
  vehicleTypeName: "Ô tô",
  brand: "VinFast",
  model: "VF 8",
  color: "Trắng",
  status: "ACTIVE",
  isDefault: true,
};

const sampleSubscription = {
  subscriptionId: "subscription-mobile-qa",
  customerId: "customer-mobile-qa",
  customerVehicleId: sampleVehicle.customerVehicleId,
  ticketTypeId: "ticket-monthly",
  parkingLotId: "parking-lot-qa",
  effectiveFrom: "2026-10-01",
  effectiveTo: "2026-10-31",
  price: 1200000,
  status: "ACTIVE",
  createdAt: "2026-10-01T08:00:00Z",
};

const sampleSession = {
  parkingSessionId: "parking-session-mobile-qa",
  checkInTime: "2026-10-15T01:00:00Z",
  checkOutTime: "2026-10-15T03:30:00Z",
  licensePlateIn: sampleVehicle.licensePlate,
  licensePlateOut: sampleVehicle.licensePlate,
  parkingLotName: "CoParking Trung tâm",
  zoneName: "Khu A",
  cardNumber: "CARD-001",
  vehicleTypeName: "Ô tô",
  events: [
    { parkingEventId: "event-in", eventType: "CHECK_IN", eventTime: "2026-10-15T01:00:00Z", laneName: "Làn vào 1", licensePlateDetected: sampleVehicle.licensePlate },
    { parkingEventId: "event-out", eventType: "CHECK_OUT", eventTime: "2026-10-15T03:30:00Z", laneName: "Làn ra 1", licensePlateDetected: sampleVehicle.licensePlate },
  ],
};

const sampleChatConversation = {
  assignedTo: "support-agent-mobile-qa",
  conversationId: "chat-mobile-qa",
  conversationType: "CUSTOMER_DIRECT",
  customerId: profile.customer.customerId,
  lastMessageAt: "2026-10-15T03:00:00Z",
  lastMessageId: "message-mobile-qa",
  ownerAccountId: profile.account.accountId,
  participants: [
    { accountId: profile.account.accountId, avatarUrl: null, email: profile.account.email, fullName: profile.profile.fullName, memberRole: "CUSTOMER", accountRoleCode: "CUSTOMER", accountRoleName: "Khách hàng", username: profile.account.username },
    { accountId: "support-agent-mobile-qa", avatarUrl: null, email: "hotro@coparking.vn", fullName: "Hỗ trợ CoParking", memberRole: "ASSIGNEE", accountRoleCode: "EMPLOYEE", accountRoleName: "Nhân viên hỗ trợ", username: "support.mobile" },
  ],
  relatedId: null,
  relatedSchema: null,
  relatedTable: null,
  status: "OPEN",
  supportTicketId: null,
  title: "Hỗ trợ khách hàng",
};

const sampleChatMessage = {
  attachments: [],
  content: "Xin chào, CoParking có thể hỗ trợ bạn vấn đề gì?",
  conversationId: sampleChatConversation.conversationId,
  createdAt: "2026-10-15T03:00:00Z",
  deleted: false,
  deletedAt: null,
  editedAt: null,
  messageId: "message-mobile-qa",
  messageType: "TEXT",
  relatedId: null,
  relatedSchema: null,
  relatedTable: null,
  contextTicketId: null,
  replyToMessageId: null,
  senderAccountId: "support-agent-mobile-qa",
};

function apiData(pathname) {
  if (pathname.endsWith("/notifications/broadcast-announcements/active")) {
    return [{
      broadcastId: "broadcast-mobile-qa",
      createdAt: "2026-10-15T00:00:00Z",
      displayOrder: 1,
      enabled: true,
      endAt: "2099-12-31T23:59:59Z",
      message: "Ưu đãi và thông tin dịch vụ mới dành cho khách hàng CoParking",
      publishedAt: "2026-10-15T00:00:00Z",
      startAt: "2026-01-01T00:00:00Z",
      status: "PUBLISHED",
      title: "Điều chỉnh giá vé",
    }];
  }
  if (pathname.endsWith("/operations/chat/conversations")) {
    return [{ conversation: sampleChatConversation, lastMessage: sampleChatMessage, unreadCount: 1 }];
  }
  if (pathname.endsWith(`/operations/chat/conversations/${sampleChatConversation.conversationId}`)) return sampleChatConversation;
  if (pathname.endsWith(`/operations/chat/conversations/${sampleChatConversation.conversationId}/messages`)) return [sampleChatMessage];
  if (pathname.endsWith(`/operations/chat/conversations/${sampleChatConversation.conversationId}/read`)) return null;
  if (pathname.endsWith("/iam/accounts/current-access")) {
    return { ...profile.account, customerApprovalStatus: "APPROVED", customerStatus: "ACTIVE" };
  }
  if (pathname.endsWith("/iam/accounts/onboarding")) return profile;
  if (pathname.endsWith("/people/customer-vehicles/me")) return [sampleVehicle];
  if (pathname.endsWith("/access-control/subscriptions/me")) return [sampleSubscription];
  if (pathname.includes("/billing/invoices/me")) {
    return [{ amount: 1200000, discountAmount: 0, finalAmount: 1200000, invoiceId: "invoice-mobile-qa", invoiceNo: "INV-MOBILE-QA", status: "PAID", subscriptionId: sampleSubscription.subscriptionId }];
  }
  if (pathname.endsWith("/parking/parking-sessions/me")) return [sampleSession];
  if (pathname.endsWith("/billing/wallets/me")) {
    return { walletId: "wallet-mobile-qa", ownerType: "CUSTOMER", currency: "VND", availableBalance: 2500000, pendingBalance: 0, heldBalance: 0, status: "ACTIVE" };
  }
  if (pathname.includes("/billing/wallets/me/transactions")) {
    return [{ financialTransactionId: "tx-mobile-qa", transactionCode: "TX-MOBILE-QA", transactionType: "TOP_UP", status: "SUCCESS", currency: "VND", occurredAt: "2026-10-15T02:00:00Z" }];
  }
  if (pathname.includes("/billing/wallets/me/topups")) return [];
  if (pathname.endsWith("/public/parking-lots")) {
    return [{ parkingLotId: "parking-lot-qa", name: "CoParking Trung tâm", addressDisplay: "Quận 1, Thành phố Hồ Chí Minh" }];
  }
  if (pathname.includes("/public/pricing/vehicle-types")) {
    return [{ vehicleTypeId: "vehicle-type-car", code: "CAR", name: "Ô tô", status: "ACTIVE" }];
  }
  if (pathname.includes("/public/pricing/ticket-types")) {
    return [{ ticketTypeId: "ticket-monthly", code: "MONTHLY", name: "Vé tháng", status: "ACTIVE" }];
  }
  if (pathname.includes("/public/pricing/price-rules")) {
    return [{ priceRuleId: "price-mobile-qa", ticketTypeId: "ticket-monthly", vehicleTypeId: "vehicle-type-car", amount: 1200000, status: "ACTIVE" }];
  }
  if (pathname.includes("/notifications/unread/count")) return { unreadCount: 0 };
  return [];
}

async function installCustomerSession(context) {
  const payload = {
    account_id: profile.account.accountId,
    email: profile.account.email,
    exp: Math.floor(Date.now() / 1000) + 3600,
    name: profile.profile.fullName,
    preferred_username: profile.account.username,
    role: "CUSTOMER",
  };
  const token = `qa.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.signature`;
  await context.addInitScript((accessToken) => localStorage.setItem("vm_access_token", accessToken), token);
  await context.route("**/api/**", async (route) => {
    const requestUrl = new URL(route.request().url());
    await route.fulfill({
      body: JSON.stringify({ data: apiData(requestUrl.pathname), message: "OK", success: true, timestamp: new Date().toISOString() }),
      contentType: "application/json",
      status: 200,
    });
  });
}

async function waitForServer() {
  const deadline = Date.now() + 30000;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(BASE_URL);
      if (response.ok) return;
    } catch {
      // retry until the preview server is up
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error("Preview server did not start in time");
}

const viteCli = join(process.cwd(), "node_modules", "vite", "bin", "vite.js");
const server = spawn(process.execPath, [viteCli, "preview", "--host", "127.0.0.1", "--port", String(PORT)], {
  cwd: process.cwd(),
  stdio: "ignore",
});

try {
  await waitForServer();
  const browser = await chromium.launch();

  for (const viewport of VIEWPORTS) {
    const context = await browser.newContext({ viewport: { height: viewport.height, width: viewport.width } });
    await installCustomerSession(context);
    const page = await context.newPage();
    let activeLabel = `unknown@${viewport.name}`;
    page.on("pageerror", (error) => {
      const message = error.message || String(error);
      const googleMapsFrameError = activeLabel.startsWith("contact@")
        && message === "Unexpected token ':'"
        && page.frames().some((frame) => frame.url().includes("google.com/maps"));
      if (googleMapsFrameError) return;
      failures.push(`${activeLabel}: runtime error: ${error.name || "Error"}: ${message}${error.stack ? `\n${error.stack}` : ""}`);
    });

    for (const route of ROUTES) {
      const label = `${route.tag}@${viewport.name}`;
      activeLabel = label;
      await page.goto(`${BASE_URL}${route.path}`, { waitUntil: "networkidle" });
      await page.waitForTimeout(400);

      if (route.protected && new URL(page.url()).pathname === "/login") {
        failures.push(`${label}: protected route redirected to login`);
      }

      const overflow = await page.evaluate(() => ({
        docWidth: document.documentElement.scrollWidth,
        innerWidth: window.innerWidth,
        scrollX: window.scrollX,
      }));
      if (overflow.docWidth > overflow.innerWidth + 1) {
        const overflowSources = await page.evaluate(() => Array.from(document.querySelectorAll("body *"))
          .map((element) => ({ element, bounds: element.getBoundingClientRect(), style: window.getComputedStyle(element) }))
          .filter(({ bounds, style }) => {
            return style.display !== "none"
              && style.visibility !== "hidden"
              && bounds.width > 0
              && (bounds.right > window.innerWidth + 1 || bounds.left < -1);
          })
          .sort((a, b) => Math.max(b.bounds.right - window.innerWidth, -b.bounds.left) - Math.max(a.bounds.right - window.innerWidth, -a.bounds.left))
          .slice(0, 10)
          .map(({ element, bounds }) => {
            return `${element.tagName.toLowerCase()}.${String(element.className).slice(0, 90)} [${Math.round(bounds.left)}..${Math.round(bounds.right)}]`;
          }));
        failures.push(`${label}: horizontal overflow (scrollWidth=${overflow.docWidth} > innerWidth=${overflow.innerWidth}); sources: ${overflowSources.join(" | ")}`);
      }

      // Mobile-only chrome must stay hidden on desktop viewports.
      if (viewport.width >= 1280) {
        const mobileNavVisible = await page.evaluate(() => {
          const nav = document.querySelector(".vm-customer-mobile-nav");
          if (!nav) return false;
          return window.getComputedStyle(nav).display !== "none";
        });
        if (mobileNavVisible) {
          failures.push(`${label}: mobile bottom nav visible on desktop`);
        }
      }

      // iOS zoom guard: customer/auth inputs must be >= 16px on mobile.
      if (viewport.width <= 480) {
        const smallInputs = await page.evaluate(() => Array.from(
          document.querySelectorAll(".vm-customer-shell input:not([type=checkbox]):not([type=radio]), .vm-auth-shell input:not([type=checkbox]):not([type=radio]), .vm-chat-composer textarea"),
        )
          .map((input) => parseFloat(window.getComputedStyle(input).fontSize))
          .filter((size) => Number.isFinite(size) && size < 16).length);
        if (smallInputs > 0) {
          failures.push(`${label}: ${smallInputs} input(s) below 16px`);
        }

        const clippedElements = await page.evaluate(() => Array.from(document.querySelectorAll("main *, .vm-customer-content *"))
          .filter((element) => {
            const node = element;
            const style = window.getComputedStyle(node);
            const bounds = node.getBoundingClientRect();
            if (style.position === "fixed" || style.display === "none" || style.visibility === "hidden") return false;
            if (["auto", "scroll", "hidden", "clip"].includes(style.overflowX)) return false;
            return (bounds.right > window.innerWidth + 2 || bounds.left < -2) && node.scrollWidth > node.clientWidth + 2;
          })
          .slice(0, 8)
          .map((element) => `${element.tagName.toLowerCase()}.${element.className}`));
        if (clippedElements.length > 0) {
          failures.push(`${label}: content clipping candidates: ${clippedElements.join(", ")}`);
        }
      }

      if (viewport.width <= 768) {
        const assertMobileElement = async (locator, elementName, edgePadding = 7) => {
          const bounds = await locator.evaluate((element) => {
            const rect = element.getBoundingClientRect();
            return { bottom: rect.bottom, left: rect.left, right: rect.right, top: rect.top };
          });
          if (bounds.left < edgePadding || bounds.right > viewport.width - edgePadding || bounds.top < 0 || bounds.bottom > viewport.height) {
            failures.push(`${label}: ${elementName} outside viewport (${JSON.stringify(bounds)})`);
          }
        };

        const auditSelectMenus = async (scopeSelector = "body") => {
          const triggers = page.locator(`${scopeSelector} .vm-select-menu-trigger:visible`);
          const labels = await triggers.evaluateAll((elements) => elements.map((element, index) => element.getAttribute("aria-label") || `select-${index + 1}`));
          for (const triggerLabel of labels) {
            const trigger = page.locator(scopeSelector).getByRole("button", { name: triggerLabel, exact: true }).first();
            await trigger.evaluate((element) => element.scrollIntoView({ behavior: "instant", block: "center", inline: "center" }));
            try {
              await trigger.click({ timeout: 5000 });
            } catch (error) {
              failures.push(`${label}: select trigger ${triggerLabel} is obstructed (${error instanceof Error ? error.message.split("\n")[0] : "click failed"})`);
              continue;
            }
            const panel = page.locator(".vm-select-menu-panel:visible").last();
            try {
              await panel.waitFor({ state: "visible", timeout: 3000 });
            } catch {
              failures.push(`${label}: select menu ${triggerLabel} did not open`);
              continue;
            }
            await assertMobileElement(panel, `select menu ${triggerLabel}`);
            await page.keyboard.press("Escape");
            await panel.waitFor({ state: "detached", timeout: 5000 });
          }
        };

        const tickerGap = await page.evaluate(() => {
          const header = document.querySelector(".vm-client-shell > header");
          const ticker = document.querySelector(".vm-announcement-ticker");
          if (!header || !ticker) return null;
          return Math.abs(ticker.getBoundingClientRect().top - header.getBoundingClientRect().bottom);
        });
        if (tickerGap !== null && tickerGap > 1) {
          failures.push(`${label}: announcement ticker gap is ${tickerGap.toFixed(1)}px`);
        }

        if (route.tag === "about") {
          const watermark = await page.evaluate(() => {
            const element = document.querySelector(".vm-public-footer-watermark");
            if (!element) return null;
            const visibleWords = Array.from(element.querySelectorAll("span")).filter((span) => window.getComputedStyle(span).display !== "none").length;
            const firstRow = element.querySelector(".vm-public-footer-watermark-row");
            return { fontSize: firstRow ? parseFloat(window.getComputedStyle(firstRow).fontSize) : 0, transform: window.getComputedStyle(element).transform, visibleWords };
          });
          if (watermark?.transform === "none") failures.push(`${label}: footer watermark is not angled on mobile`);
          if (!watermark || watermark.visibleWords < 20) failures.push(`${label}: footer watermark does not show repeated word bands`);
          if (watermark && watermark.fontSize > 44) failures.push(`${label}: footer watermark text remains too large (${watermark.fontSize}px)`);
        }

        await auditSelectMenus(".vm-client-shell");

        if (route.tag === "history") {
          const rangeTrigger = page.getByRole("button", { name: "Khoảng ngày lọc lịch sử gửi xe" });
          await rangeTrigger.evaluate((element) => element.scrollIntoView({ behavior: "instant", block: "center", inline: "center" }));
          await rangeTrigger.click();
          const rangePanel = page.locator(".vm-date-range-panel");
          await rangePanel.waitFor({ state: "visible" });
          await page.waitForTimeout(250);
          await assertMobileElement(rangePanel, "date range panel");
          await page.keyboard.press("Escape");
        }

        if (route.tag === "dashboard") {
          const dockClear = await page.evaluate(() => {
            const button = document.querySelector("[data-support-floating-button]");
            const navigation = document.querySelector(".vm-customer-mobile-nav");
            if (!button || !navigation) return null;
            return button.getBoundingClientRect().bottom <= navigation.getBoundingClientRect().top + 1;
          });
          if (dockClear === false) failures.push(`${label}: support widget overlaps bottom navigation`);

          await page.getByRole("button", { name: "Thông báo" }).first().click();
          await page.locator(".vm-notification-popover").waitFor({ state: "visible" });
          await assertMobileElement(page.locator(".vm-notification-popover"), "notification panel");
          await page.locator(".vm-header-popover-backdrop").click({ position: { x: 2, y: 2 } });

          await page.locator('button[aria-label^="Mở hồ sơ"]').click();
          await page.locator(".vm-profile-popover").waitFor({ state: "visible" });
          await assertMobileElement(page.locator(".vm-profile-popover"), "profile panel");
          await page.locator(".vm-header-popover-backdrop").click({ position: { x: 2, y: 2 } });

          await page.getByRole("button", { name: "Mở menu điều hướng" }).click();
          await page.locator('#vm-client-mobile-drawer[data-open="true"]').waitFor({ state: "visible" });
          const drawerLabels = await page.locator("#vm-client-mobile-drawer").textContent() ?? "";
          for (const expected of ["Giới thiệu", "Tìm bãi", "Bảng giá", "Hướng dẫn", "Liên hệ"]) {
            if (!drawerLabels.includes(expected)) failures.push(`${label}: mobile drawer missing ${expected}`);
          }
          await page.getByRole("button", { name: "Đóng menu", exact: true }).click();

          await page.getByRole("button", { name: "Mở thêm chức năng khách hàng" }).click();
          const moreSheet = page.locator(".vm-customer-more-sheet");
          await moreSheet.waitFor({ state: "visible" });
          await page.waitForTimeout(250);
          await assertMobileElement(moreSheet, "customer more sheet", 0);
          await page.getByRole("button", { name: "Đóng", exact: true }).click();
        }

        if (viewport.name === "390x844" && route.tag === "subscriptions") {
          const createButton = page.getByRole("button", { name: "Đăng ký vé mới" });
          if (await createButton.isVisible()) {
            await createButton.click();
            const modal = page.locator(".vm-shared-modal-panel");
            await modal.waitFor({ state: "visible" });
            await page.waitForTimeout(250);
            await assertMobileElement(modal, "subscription registration modal", 0);
            await auditSelectMenus(".vm-shared-modal-panel");
            if (await modal.isVisible()) {
              const dateTrigger = modal.getByRole("button", { name: "Ngày bắt đầu" });
              await dateTrigger.scrollIntoViewIfNeeded();
              await dateTrigger.click();
              const datePanel = page.locator(".vm-date-picker-panel");
              await datePanel.waitFor({ state: "visible" });
              await page.waitForTimeout(250);
              await assertMobileElement(datePanel, "subscription start date panel");
              await page.keyboard.press("Escape");
              if (!(await modal.isVisible())) failures.push(`${label}: closing the date picker also closed its parent modal`);
              await page.keyboard.press("Escape");
            } else {
              failures.push(`${label}: testing a nested select unexpectedly closed the registration modal`);
            }
          }
        }

        if (viewport.name === "390x844" && route.tag === "support") {
          const createButton = page.getByRole("button", { name: /Tạo yêu cầu mới/ });
          if (await createButton.isVisible()) {
            await createButton.click();
            const modal = page.locator(".vm-shared-modal-panel");
            await modal.waitFor({ state: "visible" });
            await page.waitForTimeout(250);
            await assertMobileElement(modal, "support request modal", 0);
            await auditSelectMenus(".vm-shared-modal-panel");
            await page.keyboard.press("Escape");
          }
        }

        if (route.tag === "support-chat") {
          const conversation = page.locator(".vm-chat-conversation-item").first();
          await conversation.waitFor({ state: "visible" });
          await conversation.click();
          const chatVisible = await page.locator(".vm-chat-workspace").evaluate((element) => window.getComputedStyle(element).display !== "none");
          if (!chatVisible) failures.push(`${label}: chat workspace did not open from the mobile inbox`);
        }

        await page.evaluate(() => window.scrollTo({ left: 0, top: 0 }));
      }

      await page.screenshot({ fullPage: false, path: join(SCREENSHOT_DIR, `${slug(label)}.png`) });
    }

    await context.close();
  }

  await browser.close();
} finally {
  server.kill();
  rmSync(SCREENSHOT_DIR, { force: true, recursive: true });
}

if (failures.length > 0) {
  throw new Error(`Visual regression checks failed:\n- ${failures.join("\n- ")}`);
}

console.log(`Visual regression checks passed (${VIEWPORTS.length} viewports x ${ROUTES.length} routes).`);
