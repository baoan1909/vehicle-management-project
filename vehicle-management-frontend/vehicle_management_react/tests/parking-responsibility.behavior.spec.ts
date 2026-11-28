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

async function responsibilityFixture(page: Page, role: string, options: {fail?: boolean; empty?: boolean} = {}) {
  await actor(page, role);
  const lotId=randomUUID(), org=randomUUID();
  const lot={parkingLotId:lotId,organizationId:org,code:"NEW-RESP",name:"Bãi kiểm thử người phụ trách",status:"ACTIVE",managementState: options.empty ? "PARTNER_SUPERVISED" : "MANAGED",latitude:null,longitude:null,totalCapacity:0};
  const calls: string[]=[];
  let fail=options.fail ?? false;
  await page.route("**/vehicle-management/api/**", async route => {
    const path=new URL(route.request().url()).pathname.replace("/vehicle-management/api", ""); calls.push(path);
    if (path === "/parking/parking-lots") return route.fulfill({json:envelope([lot])});
    if (path === `/parking/parking-lots/${lotId}/responsibility`) return route.fulfill(fail
      ? {status:503,json:{success:false,message:"Lỗi đọc người phụ trách"}}
      : {json:envelope({organizationName:"Đơn vị quản lý kiểm thử",managers:options.empty?[]:[{accountId:randomUUID(),displayName:"Quản lý được phân công"}]})});
    if (path.endsWith("/space-summary")) return route.fulfill({json:envelope({total:0,available:0,occupied:0,reserved:0,maintenance:0})});
    return route.fallback();
  });
  return {lotId,calls,recover:()=>{fail=false;}};
}
for (const role of ["PARKING_MANAGER","EMPLOYEE","PARTNER_ADMIN"]) {
  test(`${role} sees real lot responsibility without reading account directory`,async({page})=>{
    const fixture=await responsibilityFixture(page,role);
    await page.goto(`/admin/parking-lots?lotId=${fixture.lotId}`);
    await expect(page.getByText("Quản lý được phân công",{exact:true})).toBeVisible();
    await expect(page.getByText("Đơn vị quản lý kiểm thử",{exact:true})).toBeVisible();
    if (role !== "EMPLOYEE") await expect(page.getByText("Đã phân công Manager",{exact:true})).toBeVisible();
    expect(fixture.calls.some(path=>path==="/iam/organizations" || path==="/iam/provisioned-accounts" || path.endsWith("/parking-managers"))).toBe(false);
    if (role!=="PARTNER_ADMIN") await expect(page.getByRole("button",{name:/Quản lý phân công/})).toHaveCount(0);
  });
}
test("responsibility failure is explicit and recovers on refresh",async({page})=>{
  const fixture=await responsibilityFixture(page,"PARKING_MANAGER",{fail:true});
  await page.goto(`/admin/parking-lots?lotId=${fixture.lotId}`);
  await expect(page.getByText("Chưa tải được thông tin người phụ trách. Hãy làm mới để thử lại.",{exact:true})).toBeVisible();
  await expect(page.getByText("Bãi xe chưa xác định được người phụ trách.",{exact:true})).toHaveCount(0);
  await expect(page.getByText("100%",{exact:true})).toHaveCount(0);
  fixture.recover();
  await page.getByRole("button",{name:"Thử lại",exact:true}).first().click();
  await expect(page.getByText("Quản lý được phân công",{exact:true})).toBeVisible();
});
test("new partner-supervised lot displays organization and direct responsibility",async({page})=>{
  const fixture=await responsibilityFixture(page,"PARTNER_ADMIN",{empty:true});
  await page.goto(`/admin/parking-lots?lotId=${fixture.lotId}`);
  await expect(page.getByText("Đơn vị quản lý kiểm thử",{exact:true})).toBeVisible();
  await expect(page.getByText("Partner Admin đang trực tiếp phụ trách bãi xe này.",{exact:false})).toBeVisible();
});
