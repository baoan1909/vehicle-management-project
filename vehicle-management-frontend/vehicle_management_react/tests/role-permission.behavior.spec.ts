import { expect, test, type Page } from "@playwright/test";
import { readFileSync, mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

// Audit evidence only: role grants are a read-only snapshot of the local database.
// API fixtures exercise UI visibility; they do not assert live backend authorization.
const snapshotDir = fileURLToPath(new URL("../../../vehicle-management-backend/vehicle-management/docs/qa/role-permission-audit-2026-11-28/", import.meta.url));
const evidenceDir = resolve(snapshotDir, "after-fix");
mkdirSync(evidenceDir, { recursive: true });
const roles = JSON.parse(readFileSync(resolve(snapshotDir, "effective-permissions-after-fix.json"), "utf8").replace(/^\uFEFF/, "")) as Array<{ code: string; permissions: string[] }>;
const accountId = "00000000-0000-0000-0000-0000000000a1";
const envelope = (data: unknown) => ({ data, success: true, message: "ok", timestamp: new Date().toISOString() });

async function actor(page: Page, roleCode: string, permissions?: string[], accessState = "ACTIVE") {
  const permissionCodes = permissions ?? roles.find((role) => role.code === roleCode)!.permissions;
  await page.addInitScript(({ accountId }) => {
    const authority = "http://localhost:8081/realms/vehicle-management";
    const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
    const exp = Math.floor(Date.now() / 1000) + 3600;
    const profile = { iss: authority, aud: "vehicle-management-frontend", sub: accountId, exp, iat: exp - 3600, preferred_username: "audit-user" };
    sessionStorage.setItem(`oidc.user:${authority}:vehicle-management-frontend`, JSON.stringify({
      access_token: `${encode({ alg: "none" })}.${encode({ ...profile, account_id: accountId })}.signature`,
      expires_at: exp, profile, scope: "openid profile email roles", token_type: "Bearer",
    }));
  }, { accountId });
  const requests: string[] = [];
  await page.route("**/vehicle-management/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname.replace("/vehicle-management/api", "");
    requests.push(`${route.request().method()} ${path}`);
    if (route.request().method() !== "GET") return route.fulfill({ status: 403, json: { message: "Read-only audit" } });
    let data: unknown = [];
    if (path.endsWith("/accounts/current-access")) data = {
      accountId, accountStatus: "ACTIVE", accessRevision: "audit-revision", accessState, roleCode,
      username: "audit-user", onboardingRequired: false, permissionCodes,
      requiredAction: accessState === "ACTIVE" ? null : "CONTACT_ORGANIZATION_ADMIN",
      ...(roleCode === "CUSTOMER" ? { customerStatus: "ACTIVE", customerApprovalStatus: "APPROVED" } : {}),
    };
    else if (path.endsWith("/accounts/onboarding")) data = {
      account: { accountId, accountStatus: "ACTIVE", roleCode, username: "audit-user", emailVerified: true },
      onboardingRequired: false, profile: { fullName: "Audit User", userProfileStatus: "ACTIVE" },
      ...(roleCode === "CUSTOMER" ? { customer: { customerStatus: "ACTIVE", customerApprovalStatus: "APPROVED" } } : { employee: { employeeStatus: "ACTIVE" } }),
    };
    else if (path.endsWith("/unread/count")) data = { unreadCount: 0 };
    else if (path.endsWith("/avatar-status") || path.includes("/deletion/") || path.includes("/active-guide")) data = null;
    return route.fulfill({ json: envelope(data) });
  });
  return requests;
}

for (const { code } of roles) {
  test(`audit sidebar ${code}`, async ({ page }) => {
    await actor(page, code);
    await page.goto(code === "CUSTOMER" ? "/customer/profile" : "/admin/profile");
    const sidebar = page.getByLabel("CoParking admin sidebar");
    if (code === "CUSTOMER") await expect(sidebar).toHaveCount(0);
    else {
      await expect(sidebar).toBeVisible();
      if (code === "EMPLOYEE") {
        for (const path of ["/admin/account", "/admin/employee", "/admin/role", "/admin/avatar-approvals", "/admin/announcements"]) {
          await expect(sidebar.locator(`a[href="${path}"]`)).toHaveCount(0);
        }
      }
      if (code === "PARTNER_ADMIN" || code === "PARKING_MANAGER") await expect(sidebar.locator('a[href="/admin/role"]')).toHaveCount(0);
    }
    const readLinks = () => sidebar.locator("a").evaluateAll((items) => items.map((item) => ({ text: item.textContent?.trim(), href: item.getAttribute("href") })));
    const links = await readLinks();
    const groupButtons = sidebar.locator("button[aria-expanded]");
    for (let index = 0; index < await groupButtons.count(); index += 1) {
      const button = groupButtons.nth(index);
      if (await button.getAttribute("aria-expanded") !== "true") await button.click();
      links.push(...await readLinks());
    }
    const uniqueLinks = [...new Map(links.map((link) => [link.href, link])).values()];
    if (code === "EMPLOYEE") {
      expect(uniqueLinks.map((link) => link.href)).not.toEqual(expect.arrayContaining(["/admin/account"]));
      for (const path of ["/admin/account", "/admin/employee", "/admin/role", "/admin/avatar-approvals", "/admin/announcements"]) expect(uniqueLinks.some((link) => link.href === path)).toBe(false);
    }
    if (code === "PARTNER_ADMIN" || code === "PARKING_MANAGER") expect(uniqueLinks.some((link) => link.href === "/admin/role")).toBe(false);
    mkdirSync(evidenceDir, { recursive: true });
    writeFileSync(resolve(evidenceDir, `${code}-navigation.json`), JSON.stringify(uniqueLinks, null, 2));
    await expect(page.getByText("Unexpected Application Error!", { exact: true })).toHaveCount(0);
  });
}

for (const code of ["EMPLOYEE", "PARKING_MANAGER"]) {
  test(`audit unassigned ${code}`, async ({ page }) => {
    await actor(page, code, [], "ORGANIZATION_ACCESS_REQUIRED");
    await page.goto("/admin/profile");
    await expect(page.getByText("Bạn chưa được phân công vào bãi xe.", { exact: false })).toBeVisible();
    await expect(page.getByLabel("CoParking admin sidebar")).toHaveCount(0);
  });
}

test("regression employee cannot see card creation controls", async ({ page }) => {
  await actor(page, "EMPLOYEE");
  await page.goto("/admin/card");
  await expect(page.getByRole("button", { name: /Cấp thẻ mới/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: /Cấp hàng loạt/ })).toHaveCount(0);
  await page.screenshot({ path: resolve(evidenceDir, "employee-card-disabled-actions.png"), fullPage: true });
});

test("regression read-only shift user cannot see write controls", async ({ page }) => {
  const requests = await actor(page, "PARKING_MANAGER", ["SHIFT_READ_ALL", "SHIFT_ASSIGNMENT_READ_ALL", "PARKING_SCOPE_ASSIGNED"]);
  await page.goto("/admin/shifts");
  await expect(page.getByRole("button", { name: /Sinh lịch tuần/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: /Duyệt lịch/ })).toHaveCount(0);
  await expect(page.getByRole("tab", { name: "Mẫu ca", exact: true })).toBeVisible();
  await page.screenshot({ path: resolve(evidenceDir, "read-only-shift-write-actions.png"), fullPage: true });
  expect(requests.some((request) => request.includes("/employees") || request.includes("/gates"))).toBe(false);
  await page.getByRole("tab", { name: "Mẫu ca", exact: true }).click();
  await expect(page.getByRole("button", { name: "Tạo mẫu ca", exact: true })).toHaveCount(0);
});

test("regression employee can open own scheduled shift", async ({ page }) => {
  await actor(page, "EMPLOYEE");
  await page.route("**/operations/shift-assignments/me**", (route) => route.fulfill({ json: envelope([
    { shiftAssignmentId: "00000000-0000-0000-0000-0000000000b1", shiftId: "00000000-0000-0000-0000-0000000000c1", parkingLotId: "00000000-0000-0000-0000-0000000000e1", parkingLotName: "Audit Lot", shiftCode: "AUDIT-SHIFT", startTime: new Date(Date.now() - 60000).toISOString(), endTime: new Date(Date.now() + 3600000).toISOString(), shiftStatus: "SCHEDULED", status: "SCHEDULED" },
  ]) }));
  await page.goto("/admin/shifts");
  await expect(page.getByText("AUDIT-SHIFT · Audit Lot", { exact: true })).toBeVisible();
  let openingBody: unknown;
  await page.route("**/operations/shifts/*/open", (route) => {
    openingBody = route.request().postDataJSON();
    return route.fulfill({ json: envelope({ status: "OPEN" }) });
  });
  await page.getByRole("button", { name: "Mở ca", exact: true }).click();
  await page.getByLabel("Tiền đầu ca (đồng)").fill("100000");
  await page.getByRole("button", { name: "Xác nhận mở ca", exact: true }).click();
  await expect.poll(() => openingBody).toEqual({ openingCash: 100000, note: null });
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.screenshot({ path: resolve(evidenceDir, "employee-own-shift.png"), fullPage: true });
});

test("regression read-only voucher user cannot create", async ({ page }) => {
  await actor(page, "PARTNER_ADMIN", ["VOUCHER_READ_ALL", "PARKING_SCOPE_PARTNER"]);
  await page.goto("/admin/vouchers");
  await expect(page.getByRole("button", { name: /Tạo voucher/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Lưu voucher", exact: true })).toHaveCount(0);
  await page.screenshot({ path: resolve(evidenceDir, "read-only-voucher-create.png"), fullPage: true });
});

test("regression employee lot assignment is reserved for partner", async ({ page }) => {
  await actor(page, "PARKING_MANAGER");
  await page.goto("/admin/shifts");
  await expect(page.getByRole("heading", { name: "Ca trực & Phân công", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: /Phân công nhân viên vào bãi/ })).toHaveCount(0);
  await expect(page.getByRole("tab", { name: "Phân công bãi xe", exact: true })).toHaveCount(0);
});

test("regression employee parking navigation hides unauthorized layout", async ({ page }) => {
  await actor(page, "EMPLOYEE");
  await page.route("**/parking/parking-lots**", (route) => route.fulfill({ json: envelope([
    { parkingLotId: "00000000-0000-0000-0000-0000000000e1", organizationId: "00000000-0000-0000-0000-0000000000f1", code: "AUDIT", name: "Audit Lot", status: "ACTIVE", managementState: "MANAGED", latitude: null, longitude: null },
  ]) }));
  await page.goto("/admin/parking-lots");
  await expect(page.getByRole("navigation", { name: "Điều hướng quản lý bãi xe" }).getByRole("link", { name: "Thiết kế sơ đồ", exact: true })).toHaveCount(0);
  await expect(page.getByText("Bạn đang được phân công phụ trách bãi xe này.")).toHaveCount(0);
  await page.screenshot({ path: resolve(evidenceDir, "employee-unauthorized-layout-link.png"), fullPage: true });
});

for (const role of ["PARKING_MANAGER", "PARTNER_ADMIN", "EMPLOYEE"]) {
  test(`global announcements denied for ${role} despite stale grants`, async ({ page }) => {
    const requests = await actor(page, role, ["BROADCAST_NOTIFICATION_READ_ALL", "BROADCAST_NOTIFICATION_CREATE_ALL", "PARKING_LOT_READ_ALL"]);
    await page.goto("/admin/announcements");
    await expect(page.getByRole("button", { name: /Tạo thông báo/ })).toHaveCount(0);
    await expect(page).not.toHaveURL(/\/admin\/announcements/);
    expect(requests.some((request) => request.includes("broadcast-announcements") && !request.endsWith("/active"))).toBe(false);
  });
}

test("system admin can create global announcements", async ({ page }) => {
  await actor(page, "SYSTEM_ADMIN");
  await page.goto("/admin/announcements");
  await expect(page.getByLabel("CoParking admin sidebar").getByRole("link", { name: "Thông báo toàn sàn" })).toBeVisible();
  await page.getByRole("button", { name: /Tạo thông báo/ }).click();
  await expect(page.getByText("Tất cả tài khoản đang hoạt động", { exact: true }).last()).toBeVisible();
});


test("platform can inspect manager without status or role mutation controls", async ({ page }) => {
  await actor(page, "SYSTEM_ADMIN");
  await page.route("**/iam/accounts/provisioned**", (route) => route.fulfill({ json: envelope([{
    account: { accountId: "00000000-0000-0000-0000-0000000000f2", username: "Managed by Partner", email: "manager@example.test", accountStatus: "ACTIVE", keycloakUserId: "fixture", createdAt: "2026-11-28T00:00:00Z", updatedAt: "2026-11-28T00:00:00Z" },
    role: { roleCode: "PARKING_MANAGER", roleName: "Quản lý bãi xe", permissionCodes: [] },
  }]) }));
  await page.goto("/admin/account");
  await expect(page.getByText("Managed by Partner", { exact: true }).first()).toBeVisible();
  await expect(page.getByRole("button", { name: /Đổi trạng thái/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: /Đổi vai trò/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: /Xem quyền/ })).toBeEnabled();
});

test("employee cannot open a scheduled shift after its time window", async ({ page }) => {
  await actor(page, "EMPLOYEE");
  await page.route("**/operations/shift-assignments/me**", (route) => route.fulfill({ json: envelope([{
    shiftAssignmentId: "expired-assignment", shiftId: "expired-shift", parkingLotId: "own-lot", parkingLotName: "Audit Lot", shiftCode: "EXPIRED-SHIFT",
    startTime: new Date(Date.now() - 7200000).toISOString(), endTime: new Date(Date.now() - 3600000).toISOString(), shiftStatus: "SCHEDULED", status: "SCHEDULED",
  }]) }));
  await page.goto("/admin/shifts");
  await expect(page.getByRole("button", { name: "Mở ca", exact: true })).toBeDisabled();
});
