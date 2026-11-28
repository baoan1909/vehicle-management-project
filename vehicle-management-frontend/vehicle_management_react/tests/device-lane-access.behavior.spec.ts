import { expect, test, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { fileURLToPath } from "node:url";
const roles = JSON.parse(readFileSync(fileURLToPath(new URL("../../../vehicle-management-backend/vehicle-management/docs/qa/role-permission-audit-2026-11-28/effective-permissions-after-fix.json", import.meta.url)), "utf8")) as Array<{code: string; permissions: string[]}>;
const accountId = randomUUID();
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

async function deviceFixture(page: Page, role: string, options: { empty?: boolean; fail?: boolean; permissions?: string[] } = {}) {
  await actor(page, role, options.permissions);
  const lots = ["A", "B"].map(code => ({ parkingLotId: randomUUID(), code: `LOT-${code}`, name: `Bãi ${code}`, status: "ACTIVE" }));
  const lanes = options.empty ? [] : lots.map((lot, index) => ({ laneId: randomUUID(), parkingLotId: lot.parkingLotId, gateId: randomUUID(), code: `LANE-${index + 1}`, name: `Làn ${index + 1}`, direction: "IN", status: index ? "MAINTENANCE" : "ACTIVE", gateName: "Cổng mới", zoneName: "Khu mới" }));
  const devices = lanes.map((lane, index) => ({ deviceId: randomUUID(), deviceCode: `CAM-${index + 1}`, name: `Camera ${index + 1}`, deviceType: "CAMERA", parkingLotId: lane.parkingLotId, laneId: lane.laneId, status: "ACTIVE", config: {}, createdAt: new Date().toISOString() }));
  const calls: string[] = [];
  let fail = options.fail ?? false;
  await page.route("**/vehicle-management/api/**", async route => {
    const path = new URL(route.request().url()).pathname.replace("/vehicle-management/api", "");
    calls.push(path);
    if (path === "/hardware/devices/lane-options") return route.fulfill(fail ? { status: 403, json: { success: false, message: "Không tải được dữ liệu thử nghiệm" } } : { json: envelope(lanes) });
    if (path === "/hardware/devices") return route.fulfill({ json: envelope(devices) });
    if (path === "/parking/parking-lots") return route.fulfill({ json: envelope(lots) });
    if (path.startsWith("/parking/operations/") || ["/parking/zones", "/parking/gates", "/parking/lanes"].includes(path)) return route.fulfill({ status: 403, json: { success: false, message: "Structure access denied" } });
    return route.fallback();
  });
  return { calls, recover: () => { fail = false; } };
}

for (const role of ["EMPLOYEE", "PARKING_MANAGER", "PARTNER_ADMIN"]) {
  test(`${role} device lane filter uses scoped device lookup`, async ({page}) => {
    const { calls } = await deviceFixture(page, role);
    await page.goto("/admin/devices");
    await expect(page.getByText("LANE-1 · Làn 1", {exact: true})).toBeVisible();
    await page.getByRole("button", {name: "Làn xe", exact: true}).click();
    await expect(page.getByRole("button", {name: "LANE-2 - Làn 2", exact: true})).toBeVisible();
    await page.getByRole("button", {name: "LANE-1 - Làn 1", exact: true}).click();
    await expect(page.getByText("CAM-1", {exact: true})).toBeVisible();
    await expect(page.getByText("CAM-2", {exact: true})).toHaveCount(0);
    expect(calls).toContain("/hardware/devices/lane-options");
    expect(calls.some(path => path.startsWith("/parking/operations/") || ["/parking/zones", "/parking/gates", "/parking/lanes"].includes(path))).toBe(false);
  });
}

test("changing parking lot resets lane filter and excludes other lots", async ({page}) => {
  await deviceFixture(page, "PARTNER_ADMIN");
  await page.goto("/admin/devices");
  await expect(page.getByRole("button", {name: "Làn xe", exact: true})).toBeEnabled();
  await page.getByRole("button", {name: "Làn xe", exact: true}).click();
  await page.getByRole("button", {name: "LANE-1 - Làn 1", exact: true}).click();
  await page.getByRole("button", {name: "Bãi xe", exact: true}).click();
  await page.getByRole("button", {name: "LOT-B - Bãi B", exact: true}).click();
  await expect(page.getByText("CAM-2", {exact: true})).toBeVisible();
  await expect(page.getByText("CAM-1", {exact: true})).toHaveCount(0);
  await page.getByRole("button", {name: "Làn xe", exact: true}).click();
  await expect(page.getByRole("button", {name: "LANE-1 - Làn 1", exact: true})).toHaveCount(0);
  await expect(page.getByRole("button", {name: "LANE-2 - Làn 2", exact: true})).toBeVisible();
});

test("device-only reader needs no check-in or structure permission and has no edit actions", async ({page}) => {
  await deviceFixture(page, "EMPLOYEE", {permissions: ["DEVICE_READ_ALL", "PARKING_LOT_READ_ALL"]});
  await page.goto("/admin/devices");
  await expect(page.getByText("LANE-1 · Làn 1", {exact: true})).toBeVisible();
  await expect(page.getByRole("button", {name: /Thêm thiết bị/})).toHaveCount(0);
  await expect(page.getByTitle("Chỉnh sửa thiết bị")).toHaveCount(0);
  await expect(page.getByRole("button", {name: /Đánh dấu bảo trì thiết bị|Kích hoạt lại thiết bị|Ngưng sử dụng thiết bị/})).toHaveCount(0);
  await page.getByRole("button", {name: "Xem chi tiết thiết bị CAM-1", exact: true}).click();
  await expect(page.getByRole("heading", {name: "Chi tiết thiết bị", exact: true})).toBeVisible();
  await expect(page.getByRole("button", {name: /Lưu thay đổi/})).toHaveCount(0);
});

test("new lot without lanes has no permission warning", async ({page}) => {
  await deviceFixture(page, "PARTNER_ADMIN", {empty: true});
  await page.goto("/admin/devices");
  await expect(page.getByRole("button", {name: /Làm mới/})).toBeEnabled();
  await expect(page.getByRole("button", {name: "Làn xe", exact: true})).toBeDisabled();
  await expect(page.getByText("Không tải được bộ lọc làn:", {exact: false})).toHaveCount(0);
});

test("lane lookup failure stays visible without erasing devices and recovers on refresh", async ({page}) => {
  const fixture = await deviceFixture(page, "EMPLOYEE", {fail: true});
  await page.goto("/admin/devices");
  await expect(page.getByRole("alert").filter({hasText: "Không tải được bộ lọc làn:"})).toBeVisible();
  await expect(page.getByText("CAM-1", {exact: true})).toBeVisible();
  await expect(page.getByText("Chưa tải được thông tin làn", {exact: true})).toHaveCount(2);
  await expect(page.getByRole("button", {name: "Làn xe", exact: true})).toBeDisabled();
  fixture.recover();
  await page.getByRole("button", {name: /Làm mới/}).click();
  await expect(page.getByText("LANE-1 · Làn 1", {exact: true})).toBeVisible();
  await expect(page.getByRole("alert").filter({hasText: "Không tải được bộ lọc làn:"})).toHaveCount(0);
});
