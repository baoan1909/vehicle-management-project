import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = process.cwd();
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), "utf8");
const page = read("src/features/support/pages/GuestSupportPage.tsx");
const api = read("src/features/support/api/guestSupportApi.ts");
const routes = read("src/app/routes.tsx");
const navbar = read("src/shared/components/layout/ClientNavbar.tsx");

const checks = [
  [routes.includes('path: "/help"') && navbar.includes('href: "/help"'), "Guest help route must have a public navigation entry"],
  [api.includes("skipAuth: true"), "Every guest API boundary must explicitly skip OIDC authentication"],
  [page.includes("sessionStorage") && !page.includes("localStorage"), "Guest capability must use sessionStorage, not durable auth storage"],
  [page.includes("rate-limited") && page.includes("offline") && page.includes("expired"), "Guest UI must expose rate-limit, offline and expired states"],
  [page.includes("không thể xem hay thay đổi dữ liệu tài khoản"), "Guest UI must explain the public AI boundary"],
];

const failed = checks.filter(([passed]) => !passed);
if (failed.length) {
  for (const [, message] of failed) console.error(message);
  process.exit(1);
}

console.log("Guest public support route, API boundary, capability storage, and failure-state checks passed.");
