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

async function workforceFixture(page: Page, options: { permissions?: string[]; fail?: boolean; future?: boolean; empty?: boolean; expired?: boolean; delayed?: boolean } = {}) {
  await actor(page, "PARTNER_ADMIN", options.permissions);
  const org = randomUUID();
  const lots = ["A", "B"].map(code => ({ parkingLotId: randomUUID(), organizationId: org, code: `NEW-${code}`, name: `Bãi mới ${code}`, status: "ACTIVE" }));
  const people = ["PARKING_MANAGER", "EMPLOYEE"].map((roleCode, index) => ({
    employeeId: randomUUID(), accountId: randomUUID(), roleCode, roleName: roleCode, employeeCode: `EMP-${index}`, status: "ACTIVE", accountStatus: "ACTIVE",
    accountUsername: index ? "employee01" : "manager01", accountEmail: `person${index}@example.test`, jobTitle: roleCode,
    userProfile: { fullName: index ? "Nhân viên mới" : "Quản lý mới", status: "ACTIVE", userProfileId: randomUUID() },
  }));
  const history = options.empty ? [] : [{ assignmentId: randomUUID(), organizationId: org, parkingLotId: lots[0].parkingLotId, managerAccountId: people[0].accountId,
    managementRole: "PRIMARY", status: "ACTIVE", effectiveFrom: new Date(Date.now() + (options.future ? 86400000 : -86400000)).toISOString(), effectiveTo: options.expired ? new Date(Date.now() - 60000).toISOString() : null }];
  const employeeAssignments = options.empty ? [] : [{ assignmentId: randomUUID(), organizationId: org, parkingLotId: lots[0].parkingLotId, employeeId: people[1].employeeId,
    assignmentType: "PRIMARY", status: "ACTIVE", effectiveFrom: new Date(Date.now()-86400000).toISOString(), effectiveTo: null, assignmentSource: "PARTNER" }];
  const calls: string[] = [];
  let fail = options.fail ?? false;
  let releaseHistory = () => {};
  const historyGate = new Promise<void>(resolve => { releaseHistory = resolve; });
  if (!options.delayed) releaseHistory();
  await page.route("**/vehicle-management/api/**", async route => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace("/vehicle-management/api", "");
    calls.push(path + url.search);
    if (path === "/parking/parking-lots") return route.fulfill({ json: envelope(lots) });
    if (path === "/people/employees") return route.fulfill({ json: envelope(url.searchParams.get("parkingLotId") === lots[1].parkingLotId ? [] : people) });
    if (path === `/iam/parking-lot-assignments/organizations/${org}/employees`) return route.fulfill({ json: envelope(employeeAssignments) });
    if (path === `/iam/parking-lot-assignments/organizations/${org}/parking-lots/${lots[0].parkingLotId}/management-history`) {
      await historyGate;
      return route.fulfill(fail ? { status: 503, json: { success: false, message: "Lỗi tải quản lý thử nghiệm" } } : { json: envelope(history) });
    }
    if (path === `/iam/parking-lot-assignments/organizations/${org}/parking-lots/${lots[1].parkingLotId}/management-history`) return route.fulfill({ json: envelope([]) });
    return route.fallback();
  });
  return { calls, lots, releaseHistory, recover: () => { fail = false; } };
}
const assignmentUrl = "/admin/shifts?workspace=parkingAssignments";

test("employee lot filter sends selected lot and keeps both manager and employee", async ({page}) => {
  const { calls, lots } = await workforceFixture(page);
  await page.goto("/admin/employee");
  await page.getByRole("button", {name: "Lọc nhân viên theo bãi xe", exact: true}).click();
  await page.getByRole("button", {name: "Bãi mới A", exact: true}).click();
  await expect.poll(() => calls.some(path => path.includes(`/people/employees?parkingLotId=${lots[0].parkingLotId}`))).toBe(true);
  await expect(page.getByText("Quản lý mới", {exact: true}).first()).toBeVisible();
  await expect(page.getByText("Nhân viên mới", {exact: true}).first()).toBeVisible();
});

test("assignment tab separates managers and employees with correct management actions", async ({page}) => {
  const { lots, calls } = await workforceFixture(page);
  await page.goto(assignmentUrl);
  await expect(page.getByRole("heading", {name: "Quản lý bãi", exact: true})).toBeVisible();
  await expect(page.getByRole("heading", {name: "Nhân viên công tác", exact: true})).toBeVisible();
  const manager = page.getByRole("row").filter({hasText: "Quản lý mới"});
  const employee = page.getByRole("row").filter({hasText: "Nhân viên mới"});
  await expect(manager.getByText("Đang quản lý", {exact: true})).toBeVisible();
  await expect(manager.getByRole("link", {name: "Quản lý phân công", exact: true})).toHaveAttribute("href", `/admin/parking-lots?lotId=${lots[0].parkingLotId}&action=manage-assignments`);
  await expect(manager.getByRole("button", {name: /Chuyển công tác/})).toHaveCount(0);
  await expect(employee.getByRole("button", {name: /Chuyển công tác/})).toBeVisible();
  await expect(employee.getByRole("link", {name: /Quản lý phân công/})).toHaveCount(0);
  expect(calls.some(path => path.startsWith("/iam/parking-lot-assignments/organizations/") && path.endsWith("/management-history"))).toBe(true);
});

test("shared search and lot filters apply to both assignment groups", async ({page}) => {
  await workforceFixture(page);
  await page.goto(assignmentUrl);
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"})).toBeVisible();
  const search = page.getByPlaceholder("Tìm quản lý, nhân viên hoặc bãi xe...");
  await search.fill("Quản lý mới");
  await expect(page.getByRole("row").filter({hasText: "Nhân viên mới"})).toHaveCount(0);
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"})).toBeVisible();
  await search.clear();
  await page.getByRole("button", {name: "Lọc theo bãi xe", exact: true}).click();
  await page.getByRole("button", {name: "NEW-B - Bãi mới B", exact: true}).click();
  await expect(page.getByText("Không có phân công quản lý phù hợp với bộ lọc.", {exact: true})).toBeVisible();
  await expect(page.getByRole("row").filter({hasText: /Quản lý mới|Nhân viên mới/})).toHaveCount(0);
});

test("future manager assignment is scheduled and excluded from active filter", async ({page}) => {
  await workforceFixture(page, {future: true});
  await page.goto(assignmentUrl);
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"}).getByText("Sắp hiệu lực", {exact: true})).toBeVisible();
  await page.getByRole("button", {name: "Lọc trạng thái phân công bãi xe", exact: true}).click();
  await page.getByRole("button", {name: "Đang hiệu lực", exact: true}).click();
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"})).toHaveCount(0);
  await expect(page.getByRole("row").filter({hasText: "Nhân viên mới"})).toBeVisible();
});

test("read-only partner sees both groups without mutation actions", async ({page}) => {
  await workforceFixture(page, {permissions: ["PARKING_SCOPE_PARTNER", "EMPLOYEE_READ_ALL", "PARKING_LOT_READ_ALL", "ORGANIZATION_READ_ALL", "SHIFT_READ_ALL", "SHIFT_ASSIGNMENT_READ_ALL"]});
  await page.goto(assignmentUrl);
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"})).toBeVisible();
  await expect(page.getByRole("row").filter({hasText: "Nhân viên mới"})).toBeVisible();
  await expect(page.getByRole("link", {name: /Quản lý phân công/})).toHaveCount(0);
  await expect(page.getByRole("button", {name: /Chuyển công tác/})).toHaveCount(0);
});

test("without organization read permission no manager history API is requested", async ({page}) => {
  const { calls } = await workforceFixture(page, {permissions: ["PARKING_SCOPE_PARTNER", "EMPLOYEE_READ_ALL", "PARKING_LOT_READ_ALL", "SHIFT_READ_ALL", "SHIFT_ASSIGNMENT_READ_ALL"]});
  await page.goto(assignmentUrl);
  await expect(page.getByRole("row").filter({hasText: "Nhân viên mới"})).toBeVisible();
  await expect(page.getByRole("heading", {name: "Quản lý bãi", exact: true})).toHaveCount(0);
  expect(calls.some(path => path.endsWith("/management-history"))).toBe(false);
});

test("manager history failure is visible while employee assignments remain and retry recovers", async ({page}) => {
  const fixture = await workforceFixture(page, {fail: true});
  await page.goto(assignmentUrl);
  await expect(page.getByRole("alert").filter({hasText: "Không tải được phân công quản lý:"})).toBeVisible();
  await expect(page.getByRole("row").filter({hasText: "Nhân viên mới"})).toBeVisible();
  await expect(page.getByText("Không có phân công quản lý phù hợp với bộ lọc.", {exact: true})).toHaveCount(0);
  fixture.recover();
  await page.getByRole("button", {name: "Thử lại", exact: true}).click();
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"})).toBeVisible();
});

test("new empty lot shows separate empty groups and offers manager assignment", async ({page}) => {
  const { lots } = await workforceFixture(page, {empty: true});
  await page.goto(`${assignmentUrl}&parkingLotId=${lots[0].parkingLotId}`);
  await expect(page.getByText("Không có phân công quản lý phù hợp với bộ lọc.", {exact: true})).toBeVisible();
  await expect(page.getByRole("heading", {name: "Nhân viên công tác", exact: true})).toBeVisible();
  await expect(page.getByRole("link", {name: "Quản lý phân công tại bãi này", exact: true})).toHaveAttribute("href", `/admin/parking-lots?lotId=${lots[0].parkingLotId}&action=manage-assignments`);
});

test("expired manager is history even before stored status changes", async ({page}) => {
  await workforceFixture(page, {expired: true});
  await page.goto(assignmentUrl);
  await expect(page.getByText("Không có phân công quản lý phù hợp với bộ lọc.", {exact: true})).toBeVisible();
  await page.getByRole("button", {name: "Lọc trạng thái phân công bãi xe", exact: true}).click();
  await page.getByRole("button", {name: "Tất cả lịch sử", exact: true}).click();
  await expect(page.getByRole("row").filter({hasText: "Quản lý mới"}).getByText("Đã kết thúc", {exact: true})).toBeVisible();
});

test("late manager history from previous lot cannot restore its rows", async ({page}) => {
  const fixture = await workforceFixture(page, {delayed: true});
  try {
    await page.goto(assignmentUrl);
    await expect.poll(() => fixture.calls.some(path => path.includes(fixture.lots[0].parkingLotId) && path.endsWith("/management-history"))).toBe(true);
    await page.getByRole("button", {name: "Lọc theo bãi xe", exact: true}).click();
    await page.getByRole("button", {name: "NEW-B - Bãi mới B", exact: true}).click();
    await expect(page.getByText("Không có phân công quản lý phù hợp với bộ lọc.", {exact: true})).toBeVisible();
    const response = page.waitForResponse(res => res.url().includes(fixture.lots[0].parkingLotId) && res.url().endsWith("/management-history"));
    fixture.releaseHistory();
    await response;
    await expect(page.getByRole("row").filter({hasText: "Quản lý mới"})).toHaveCount(0);
    await expect(page.getByText("Không có phân công quản lý phù hợp với bộ lọc.", {exact: true})).toBeVisible();
  } finally { fixture.releaseHistory(); }
});
