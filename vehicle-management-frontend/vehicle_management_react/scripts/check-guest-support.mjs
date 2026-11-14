import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = process.cwd();
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), "utf8");
const widget = read("src/features/support/components/GuestSupportFloatingWidget.tsx");
const conversation = read("src/features/support/context/GuestSupportConversationProvider.tsx");
const state = read("src/features/support/guestSupportState.ts");
const provider = read("src/app/providers/AppProviders.tsx");
const host = read("src/features/support/components/ClientSupportWidget.tsx");
const api = read("src/features/support/api/guestSupportApi.ts");
const routes = read("src/app/routes.tsx");
const navbar = read("src/shared/components/layout/ClientNavbar.tsx");

const checks = [
  [routes.includes('<Navigate to="/?support=open" replace />') && !routes.includes("GuestSupportPage"), "Legacy help must redirect to the widget instead of rendering a page"],
  [!navbar.includes('label: "Trợ giúp"') && !navbar.includes('href: "/help"') && widget.includes("data-support-floating-button"), "Support must remain available from the floating widget without a header navigation entry"],
  [api.includes("skipAuth: true"), "Every guest API boundary must explicitly skip OIDC authentication"],
  [state.includes("sessionStorage") && !state.includes("localStorage"), "Guest capability must use sessionStorage, not durable auth storage"],
  [state.includes("rate-limited") && state.includes("offline") && state.includes("expired"), "Guest UI must expose rate-limit, offline and expired states"],
  [widget.includes("không thể xem hay thay đổi dữ liệu tài khoản"), "Guest UI must explain the public AI boundary"],
  [provider.includes("<GuestSupportConversationProvider>") && !conversation.includes("sessionStorage") && !conversation.includes("localStorage"), "Conversation history must stay in app-level React memory"],
  [host.includes('authenticationStatus === "unauthenticated" && !user'), "Guest support must not be an authentication-error or missing-permission fallback"],
];

const failed = checks.filter(([passed]) => !passed);
if (failed.length) {
  for (const [, message] of failed) console.error(message);
  process.exit(1);
}

console.log("Guest support widget, memory lifecycle, public API boundary, and capability checks passed.");
