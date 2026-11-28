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

async function parkingFixture(page: Page, role: string, empty = false, fail = "") {
  await actor(page, role);
  const lotId = randomUUID(), levelId = randomUUID(), zoneId = randomUUID(), gateId = randomUUID(), spaceId = randomUUID();
  const lot = { parkingLotId: lotId, organizationId: randomUUID(), code: "NEW-LOT", name: "Bãi kiểm thử mới", status: "ACTIVE", managementState: "PARTNER_SUPERVISED", totalCapacity: empty ? 0 : 1, addressDisplay: "Địa chỉ thử", latitude: 10.85, longitude: 106.77 };
  const level = { parkingLevelId: levelId, parkingLotId: lotId, code: "L1", name: "Tầng mới", status: "ACTIVE", canvasWidth: 500, canvasHeight: 400 };
  const zone = { zoneId, parkingLotId: lotId, parkingLevelId: levelId, code: "Z1", name: "Khu mới", status: "ACTIVE", trackingMode: "SPACE", layoutStatus: "PUBLISHED", vehicleTypeId: null, capacity: 1 };
  const space = { parkingSpaceId: spaceId, zoneId, code: "NEW-01", status: "AVAILABLE", lifecycleStatus: "ACTIVE", x: 10, y: 20, width: 60, height: 100, rotation: 0, vehicleTypeId: null };
  const summary = { total: empty ? 0 : 1, available: empty ? 0 : 1, occupied: 0, reserved: 0, maintenance: 0 };
  const calls: string[] = [];
  await page.route("**/vehicle-management/api/**", async route => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace("/vehicle-management/api", "");
    if (!path.includes("parking") && !path.includes("organizations")) return route.fallback();
    calls.push(path);
    if (route.request().method() !== "GET") return route.fulfill({status: 403, json: {message: "No writes in read test"}});
    if (fail && path.endsWith(fail)) return route.fulfill({status: 403, json: {success:false, message:"Không tải được dữ liệu thử nghiệm"}});
    let data: unknown = [];
    if (path === "/parking/parking-lots") data = [lot];
    else if (path.endsWith("/responsibility")) data = {organizationName:"Đơn vị thử", managers:[]};
    else if (path.endsWith("/levels")) data = empty ? [] : [level];
    else if (path.endsWith("/zones") || path === "/parking/zones") data = empty ? [] : [zone];
    else if (path === "/parking/gates") data = empty ? [] : [{gateId, zoneId, status:"ACTIVE", code:"G1", name:"Cổng mới"}];
    else if (path.endsWith("/lanes")) data = empty ? [] : [{laneId: randomUUID(), gateId, direction:url.searchParams.get("direction") ?? "IN", status:"ACTIVE", code:url.searchParams.get("direction") === "OUT" ? "NEW-OUT" : "NEW-IN", name:"Làn mới"}];
    else if (path.endsWith("/summary") || path.endsWith("/space-summary")) data = summary;
    else if (path.endsWith("/map")) data = {level, zones:[zone], spaces:empty ? [] : [space], summary};
    else if (path.endsWith("/layout/published")) data = {items:[{...space, spaceCode:space.code}], elements:[]};
    else if (path.endsWith("/spaces")) data = empty ? [] : [space];
    else if (path.endsWith("/open-session")) data = null;
    else if (path === "/organizations") data = [{organizationId:lot.organizationId, name:"Đơn vị thử"}];
    else if (path.includes("parking-manager")) data = {parkingManagerAccountIds:[]};
    return route.fulfill({json:envelope(data)});
  });
  await page.route("https://*.openstreetmap.org/**", route => route.abort("namenotresolved"));
  return {lotId, levelId, zoneId, calls};
}

for (const role of ["EMPLOYEE", "PARKING_MANAGER", "PARTNER_ADMIN"]) {
  test(`${role} reads active lanes for new arbitrary lot in both directions`, async ({page}) => {
    const {calls} = await parkingFixture(page, role);
    await page.goto("/admin/swipe");
    await expect(page.getByText("NEW-IN", {exact:true}).first()).toBeVisible();
    await page.getByRole("tab", {name:"Check-out", exact:true}).click();
    await expect(page.getByText("NEW-OUT", {exact:true}).first()).toBeVisible();
    expect(calls.some(path=>path.includes("/parking/operations/lots/") && path.endsWith("/lanes"))).toBe(true);
    expect(calls).not.toContain("/parking/lanes");
    expect(calls).not.toContain("/parking/zones");
  });
  test(`${role} reads published live map on a new arbitrary lot`, async ({page}) => {
    const {lotId, calls} = await parkingFixture(page, role);
    await page.goto(`/admin/parking-live?lotId=${lotId}`);
    await expect(page.getByText("NEW-01", {exact:true}).first()).toBeVisible();
    expect(calls.some(path=>path.endsWith("/layout/published"))).toBe(true);
    await expect(page.getByText("Bãi chưa có tầng", {exact:true})).toHaveCount(0);
    if (role === "EMPLOYEE") {
      await expect(page.getByRole("link", {name:"Cấu trúc", exact:true})).toHaveCount(0);
      await expect(page.getByRole("button", {name:"Cập nhật trạng thái", exact:true})).toHaveCount(0);
      await expect(page.getByRole("button", {name:"Gán chỗ", exact:true})).toHaveCount(0);
    }
  });
}

test("new lot with no levels reports empty state after successful fetch", async ({page}) => {
  const {lotId} = await parkingFixture(page,"EMPLOYEE",true);
  await page.goto(`/admin/parking-live?lotId=${lotId}`);
  await expect(page.getByText("Bãi chưa có tầng",{exact:true})).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
});

test("failed level request is not represented as an unconfigured lot", async ({page}) => {
  const {lotId} = await parkingFixture(page,"EMPLOYEE",false,"/levels");
  await page.goto(`/admin/parking-live?lotId=${lotId}`);
  await expect(page.getByRole("alert").filter({hasText:"Không tải được dữ liệu sơ đồ"})).toBeVisible();
  await expect(page.getByText("Bãi chưa có tầng",{exact:true})).toHaveCount(0);
});

test("failed lane request is not represented as missing active lanes", async ({page}) => {
  await parkingFixture(page,"EMPLOYEE",false,"/lanes");
  await page.goto("/admin/swipe");
  await expect(page.getByText("Không tải được danh sách làn",{exact:true}).first()).toBeVisible();
  await expect(page.getByText("Chưa có làn hoạt động",{exact:true})).toHaveCount(0);
});

for (const role of ["PARKING_MANAGER", "PARTNER_ADMIN"]) {
  test(`${role} overview actually fetches zones gates and lanes`, async ({page}) => {
    const {lotId, calls} = await parkingFixture(page,role);
    await page.goto(`/admin/parking-lots?lotId=${lotId}`);
    await expect.poll(()=>calls.includes("/parking/lanes")).toBe(true);
    expect(calls).toContain("/parking/zones");
    expect(calls).toContain("/parking/gates");
    await expect(page.getByText("Chưa cấu hình",{exact:true})).toHaveCount(0);
  });
}

test("overview does not present failed topology as zero configuration", async ({page}) => {
  const {lotId} = await parkingFixture(page,"PARTNER_ADMIN",false,"/zones");
  await page.goto(`/admin/parking-lots?lotId=${lotId}`);
  await expect(page.getByText("Chưa tải được đầy đủ số liệu cấu hình.",{exact:false})).toBeVisible();
  await expect(page.getByText("Chưa cấu hình",{exact:true})).toHaveCount(0);
});

test("geographic map shows a recoverable error when tile DNS fails", async ({page}) => {
  const {lotId} = await parkingFixture(page,"PARTNER_ADMIN");
  await page.goto(`/admin/parking-lots?lotId=${lotId}`);
  const retry = page.getByRole("button",{name:"Thử lại bản đồ"});
  await expect(retry).toBeVisible();
  await retry.click();
  await expect(retry).toBeVisible();
});


test("switching lots ignores a slow response from the previous lot", async ({page}) => {
  const {lotId, levelId} = await parkingFixture(page,"EMPLOYEE");
  const otherId = randomUUID();
  await page.route("**/parking/parking-lots**", route => route.fulfill({json:envelope([
    {parkingLotId:lotId,code:"FIRST",name:"Bãi thứ nhất",status:"ACTIVE"},
    {parkingLotId:otherId,code:"SECOND",name:"Bãi thứ hai",status:"ACTIVE"}
  ])}));
  let releaseOld!: () => void;
  const oldPending = new Promise<void>(resolve=>{releaseOld=resolve;});
  await page.route(`**/operations/lots/${lotId}/levels`, async route => {
    await oldPending;
    await route.fulfill({json:envelope([{parkingLevelId:levelId,parkingLotId:lotId,code:"OLD",name:"Tầng bãi cũ",status:"ACTIVE"}])});
  });
  await page.route(`**/operations/lots/${otherId}/**`, route => route.fulfill({json:envelope(
    route.request().url().endsWith("/summary") ? {total:0,available:0,occupied:0,reserved:0,maintenance:0} : []
  )}));
  try {
  await page.goto(`/admin/parking-live?lotId=${lotId}`);
  await page.getByRole("button",{name:"Chọn bãi xe",exact:true}).click();
  await page.getByRole("button",{name:"SECOND — Bãi thứ hai",exact:true}).click();
  await expect(page.getByText("Bãi chưa có tầng",{exact:true})).toBeVisible();
  const oldResponse = page.waitForResponse(response => response.url().endsWith(`/lots/${lotId}/levels`));
  releaseOld();
  await oldResponse;
  await expect(page.getByRole("heading",{name:/Bãi thứ hai/})).toBeVisible();
  await expect(page.getByText("Tầng bãi cũ",{exact:false})).toHaveCount(0);
  } finally { releaseOld(); }
});

test("employee direct structure URL does not issue administrative topology requests", async ({page}) => {
  const {lotId,calls} = await parkingFixture(page,"EMPLOYEE");
  await page.goto(`/admin/parking-structure?lotId=${lotId}`);
  await expect(page).not.toHaveURL(/parking-structure/);
  expect(calls).not.toContain("/parking/zones");
});


test("lot creation permission alone does not grant structure read access", async ({page}) => {
  await actor(page,"PARTNER_ADMIN",["PARKING_LOT_CREATE_ALL","PARKING_SCOPE_PARTNER"]);
  await page.goto("/admin/parking-structure");
  await expect(page).not.toHaveURL(/parking-structure/);
});
