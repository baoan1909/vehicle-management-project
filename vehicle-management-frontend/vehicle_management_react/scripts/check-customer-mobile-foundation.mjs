import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = process.cwd();
function read(file) {
  return readFileSync(resolve(root, file), "utf8");
}

const failures = [];
function check(name, condition, hint = "") {
  if (!condition) failures.push(`${name}${hint ? ` (${hint})` : ""}`);
}

// 1. Mobile foundation CSS exists and is desktop-safe (all layout overrides in max-width queries).
const mobileCss = read("src/app/styles/customer-mobile.css");
check("customer-mobile.css present", mobileCss.length > 1000);
check("mobile CSS imports in globals", read("src/app/styles/globals.css").includes("customer-mobile.css"));
const mediaBlocks = mobileCss.match(/@media[^{]*max-width[^{]*\{/g) ?? [];
check("mobile CSS uses max-width queries", mediaBlocks.length >= 4, `found ${mediaBlocks.length}`);
check("no bare min-width desktop override", !/^[^@]*\.vm-customer-content\s*\{[^}]*grid-template-columns/m.test(mobileCss.split("@media")[0] ?? ""));
for (const token of [
  "env(safe-area-inset-bottom",
  "100dvh",
  "prefers-reduced-motion",
  ":focus-visible",
  "vm-customer-mobile-nav",
  "vm-support-mobile-cards",
  "vm-customer-more-sheet",
  "vm-client-mobile-drawer",
  "vm-client-drawer-backdrop",
]) {
  check(`mobile CSS contains ${token}`, mobileCss.includes(token));
}
// Tap target + iOS zoom guards.
check("44px tap target guard", mobileCss.includes("44px"));
check("16px input guard", mobileCss.includes("font-size: 16px"));

// 2. Bottom nav is permission-aware and hidden on desktop by default.
const mobileNav = read("src/features/customer-portal/components/CustomerMobileNav.tsx");
check("bottom nav uses permission gate", mobileNav.includes("canAccessCustomerRoute"));
check("bottom nav has safe-area styling via CSS class", mobileNav.includes("vm-customer-mobile-nav"));
check("bottom nav exposes logout", mobileNav.includes("logoutCurrentUser"));
check("bottom nav exposes find parking as a primary destination", mobileNav.includes('label: "Tìm bãi"') && mobileNav.includes('to: "/parking-lots/nearby"'));
check("bottom nav more sheet has dialog semantics", mobileNav.includes('aria-modal="true"') && mobileNav.includes('role="dialog"'));
const portalShared = read("src/features/customer-portal/pages/PortalShared.tsx");
check("portal layout mounts mobile nav", portalShared.includes("CustomerMobileNav"));
check("portal layout scopes the active customer route", portalShared.includes("data-customer-route"));

const clientNavbar = read("src/shared/components/layout/ClientNavbar.tsx");
for (const publicLabel of ["Giới thiệu", "Tìm bãi", "Bảng giá", "Hướng dẫn", "Liên hệ"]) {
  check(`mobile header exposes ${publicLabel}`, clientNavbar.includes(`label: "${publicLabel}"`));
}
check("mobile header drawer has dialog semantics", clientNavbar.includes('aria-modal="true"') && clientNavbar.includes("vm-client-mobile-drawer"));
check("mobile header drawer traps keyboard focus", clientNavbar.includes('event.key !== "Tab"') && clientNavbar.includes('event.key === "Escape"'));
check("customer sidebar is removed from mobile layout", /@media\s*\(max-width:\s*768px\)[\s\S]*?\.vm-customer-sidebar\s*\{[\s\S]*?display:\s*none\s*!important/.test(mobileCss));
check("no global horizontal clipping workaround", !/html\s*,\s*body\s*\{[^}]*overflow-x\s*:\s*(?:hidden|clip)/s.test(mobileCss));
check("responsive layer does not hide horizontal overflow", !/overflow-x\s*:\s*(?:hidden|clip)/.test(mobileCss));

// 3. Support page uses /mine, permission-gated create, mobile cards, idempotency.
const supportPage = read("src/features/customer-portal/pages/SupportPage.tsx");
check("support uses getMySupportTickets", supportPage.includes("getMySupportTickets"));
check("support no longer queries admin list by customerId", !supportPage.includes("getSupportTickets({ customerId })"));
check("support create gated by permission", supportPage.includes("SUPPORT_TICKET_CREATE_OWN"));
check("support renders mobile cards", supportPage.includes("vm-support-mobile-cards"));
check("support keeps desktop table", supportPage.includes("vm-support-desktop-table"));
check("support create uses idempotency key", supportPage.includes("Idempotency-Key") || supportPage.includes("idempotencyKey"));
check("support create guards double submit", supportPage.includes("if (saving)"));

// 4. Vehicle actions are permission-gated (no hard role check).
const vehiclePage = read("src/features/customer-portal/pages/VehiclePage.tsx");
check("vehicle create gated", vehiclePage.includes("CUSTOMER_VEHICLE_CREATE_OWN"));
check("vehicle update gated", vehiclePage.includes("CUSTOMER_VEHICLE_UPDATE_OWN"));
check("vehicle uses permission helper", vehiclePage.includes("hasAnyPermission"));
check("vehicle has no role-code branch", !/user\?\.role\s*===\s*"(CUSTOMER|EMPLOYEE|PARKING_MANAGER|SYSTEM_ADMIN)"/.test(vehiclePage));

// 5. Subscription register/pay gated + no double-submit regression on register CTA.
const subscriptionPage = read("src/features/customer-portal/pages/SubscriptionPage.tsx");
check("subscription register gated", subscriptionPage.includes("SUBSCRIPTION_CREATE_OWN"));
check("subscription pay gated", subscriptionPage.includes("WALLET_PAY_INVOICE_OWN"));
check("subscription register disables while saving", subscriptionPage.includes("disabled={!profile || saving}"));

// 6. Wallet keeps the desktop ledger table and adds a mobile card list.
const walletPage = read("src/features/billing/pages/CustomerWalletPage.tsx");
check("wallet keeps desktop table", walletPage.includes("vm-wallet-desktop-table"));
check("wallet renders mobile cards", walletPage.includes("vm-wallet-mobile-cards"));
check("wallet top-up uses numeric keyboard", read("src/features/billing/pages/WalletTopUpPage.tsx").includes('inputMode="numeric"'));
check("wallet CSS covers mobile cards", mobileCss.includes("vm-wallet-mobile-cards"));

// 7. Support center chat collapses on mobile with keyboard-safe composer.
const chatPage = read("src/features/support/pages/OperationsSupportCenterPage.tsx");
check("chat shell uses dvh-safe mobile class", chatPage.includes("vm-chat-shell-mobile"));
check("chat has mobile list/chat view state", chatPage.includes("mobileChatView"));
check("chat selection opens workspace on mobile", chatPage.includes('setMobileChatView("chat")'));
check("chat workspace has mobile back control", chatPage.includes("vm-chat-back") && chatPage.includes("onBackToList"));
check("chat syncs shell height with visualViewport", chatPage.includes("visualViewport"));
check("chat composer has mobile class", chatPage.includes("vm-chat-composer"));
check("chat input targets 16px on mobile", chatPage.includes("vm-chat-input") && mobileCss.includes(".vm-chat-composer .vm-chat-input"));
check("chat shows sending progress", chatPage.includes("vm-chat-sending") && chatPage.includes('role="status"'));
check("chat shows send error with retry", chatPage.includes("vm-chat-send-error") && chatPage.includes("handleRetrySendMessage"));
check("chat send guards double submit", chatPage.includes("if (isSending) return;"));
check("chat CSS collapses panes under breakpoint", mobileCss.includes('[data-mobile-view="chat"]') && mobileCss.includes('[data-mobile-view="list"]'));
check("chat composer has safe-area padding", mobileCss.includes(".vm-chat-composer {") || mobileCss.includes(".vm-chat-composer{"));
check("chat send control compacts on small phones", chatPage.includes("vm-chat-send-label") && mobileCss.includes(".vm-chat-send-label"));

// 8. Customer "own" flows use /me endpoints and never send customerId from the browser.
const portalApi = read("src/features/customer-portal/api/customerPortalApi.ts");
check("vehicles listed via /me", portalApi.includes("`${apiEndpoints.customers.customerVehicles}/me`"));
check("vehicles created via /me", portalApi.includes("`${apiEndpoints.customers.customerVehicles}/me`"));
check("subscriptions listed via /me", portalApi.includes("`${apiEndpoints.accessControl.subscriptions}/me`"));
check("subscription create sends idempotency key", portalApi.includes('"Idempotency-Key"'));
check("invoices batched via /me", portalApi.includes("`${apiEndpoints.billing.invoices}/me"));
check("no customerId query from browser", !portalApi.includes("customerId: requireCustomerId") && !portalApi.includes("{ customerId }"));
check("dashboard batches invoices in one request", read("src/features/customer-portal/pages/CustomerDashboardPage.tsx").includes("getMySubscriptionInvoices("));
check("subscription page batches invoices in one request", read("src/features/customer-portal/pages/SubscriptionPage.tsx").includes("getMySubscriptionInvoices("));
check("support detail does not fall back to ticket.customerId", !supportPage.includes("selectedTicket.customerId"));

// 9. Remaining customer/public screens collapse without touching desktop.
check("guide/contact grids collapse on mobile", mobileCss.includes(".vm-guide-step-grid") && mobileCss.includes(".vm-contact-layout"));
check("vehicle hero no longer overlaps on mobile", read("src/features/customer-portal/pages/VehiclePage.tsx").includes("max-[640px]:tw-max-w-full"));
check("vehicle filter collapses on small phones", read("src/features/customer-portal/pages/VehiclePage.tsx").includes("max-[480px]:tw-grid-cols-1"));
check("history keeps desktop timeline and adds mobile cards", read("src/features/customer-portal/pages/CustomerHistoryPage.tsx").includes("vm-history-mobile-cards"));
check("history CSS toggles timeline/cards", mobileCss.includes("vm-history-desktop-timeline") && mobileCss.includes("vm-history-mobile-cards"));
check("history detail events stack on small phones", read("src/features/customer-portal/pages/CustomerHistoryPage.tsx").includes("vm-history-event-row") && mobileCss.includes(".vm-history-event-row"));
check("profile hero stacks on small phones", read("src/features/customer-portal/pages/ProfilePage.tsx").includes("vm-profile-hero-grid") && mobileCss.includes(".vm-profile-hero-grid"));
check("top-up result stays inside customer layout", read("src/features/billing/pages/WalletTopupResultPage.tsx").includes("CustomerPortalLayout"));
check("pricing stacks cards on small phones", read("src/features/customer-portal/pages/PricingPage.tsx").includes("max-[480px]:tw-grid-cols-1"));
check("auth shell targets 16px inputs", mobileCss.includes(".vm-auth-shell input"));
check("header fits 320px viewports", mobileCss.includes('a[href="/become-a-partner"]'));
check("mobile footer watermark cannot enlarge the canvas", portalShared.includes("vm-public-footer-watermark") && mobileCss.includes(".vm-public-footer-watermark"));

if (failures.length > 0) {
  throw new Error(`Customer mobile foundation checks failed:\n- ${failures.join("\n- ")}`);
}

console.log("Customer mobile foundation checks passed.");
