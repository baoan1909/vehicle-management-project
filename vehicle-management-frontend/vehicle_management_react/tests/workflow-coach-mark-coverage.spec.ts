import { expect, test, type Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";
import { getWorkflowCoachMarks, WORKFLOW_COACH_MARKS } from "../src/features/user-guide/utils/workflowCoachMarks";
import type { CurrentUser } from "../src/shared/types/common";

const sql = fs.readFileSync(path.resolve("../../vehicle-management-backend/vehicle-management/src/main/resources/db/migration/V20261115120000__seed_workflow_coach_mark_coverage.sql"), "utf8");
const rows = [...sql.matchAll(/^\('([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', ARRAY\[([^\]]*)\], \d+, '(.+)'::jsonb\)/gm)]
  .map((m) => ({ code: m[3], context: m[4], title: m[5], route: m[6], mode: m[7], permissions: [...m[8].matchAll(/'([^']+)'/g)].map((p) => p[1]), steps: JSON.parse(m[9].replaceAll("''", "'")) as Array<{ anchor: string; title: string; body: string }> }));
const actor = (permissionCodes: string[], role: CurrentUser["role"] = "EMPLOYEE"): CurrentUser => ({
  id: "actor", username: "actor", fullName: "Actor", avatarUrl: "", role, permissionCodes, accessState: "ACTIVE", onboardingRequired: false,
});
test("all manual guide selectors agree with seed audiences and every step has an application anchor", () => {
  expect(rows).toHaveLength(19);
  const walk = (directory: string): string[] => fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => entry.isDirectory() ? walk(path.join(directory, entry.name)) : [path.join(directory, entry.name)]);
  const sources = walk("src").filter((p) => /\.tsx?$/.test(p)).map((p) => fs.readFileSync(p, "utf8")).join("\n");
  for (const item of WORKFLOW_COACH_MARKS) {
    const seed = rows.find((row) => row.code === item.code)!;
    expect(seed, item.code).toBeTruthy();
    expect(seed.permissions).toEqual([...item.permissions]);
    expect(seed.mode).toBe(item.mode === "all" ? "PERMISSION_ALL" : "PERMISSION_ANY");
    expect(seed.context).toBe(item.path.toUpperCase());
    expect(seed.steps.length).toBeGreaterThanOrEqual(3);
    for (const step of seed.steps) expect(sources, step.anchor).toContain(step.anchor);
  }
  expect(sql).toContain("'MANUAL_ONLY'");
  expect(sql).not.toMatch(/UPDATE\s+iam\.|INSERT\s+INTO\s+iam\.|DELETE\s+FROM/i);
});
test("same employee role can see delegated topology guide only with both required actions", () => {
  expect(getWorkflowCoachMarks(actor(["PARKING_SPACE_CONFIGURE_ALL"]), "/admin/parking-structure")).toHaveLength(0);
  const delegated = actor(["PARKING_SPACE_CONFIGURE_ALL", "PARKING_TOPOLOGY_CONFIGURE_ALL"]);
  expect(getWorkflowCoachMarks(delegated, "/admin/parking-structure")[0].code).toBe("WORKFLOW_STRUCTURE_SETUP");
  expect(getWorkflowCoachMarks({ ...delegated, accessState: "ORGANIZATION_ACCESS_REQUIRED" }, "/admin/parking-structure")).toHaveLength(0);
  expect(getWorkflowCoachMarks({ ...delegated, onboardingRequired: true }, "/admin/parking-structure")).toHaveLength(0);
});
test("checkout-only actor gets checkout and recovery without check-in guides", () => {
  const guides = getWorkflowCoachMarks(actor(["PARKING_SESSION_CHECK_OUT_ALL"]), "/admin/swipe", "?mode=check-out");
  expect(guides.map((guide) => guide.code)).toEqual(["WORKFLOW_CHECKOUT", "WORKFLOW_OPERATION_RECOVERY"]);
  expect(getWorkflowCoachMarks(actor(["SHIFT_ASSIGNMENT_READ_OWN"]), "/admin/shifts").map((guide) => guide.code)).toEqual(["WORKFLOW_OWN_SHIFT"]);
  expect(getWorkflowCoachMarks(actor(["CARD_READ_ALL"]), "/admin/swipe/sessions")[0].code).toBe("WORKFLOW_SESSION_LOOKUP");
});

async function setup(page: Page) {
  const accountId = "00000000-0000-0000-0000-00000000c001";
  const writes: string[] = [];
  const requested: string[] = [];
  const permissionCodes = ["PARKING_SCOPE_ASSIGNED", "PARKING_LOT_READ_ALL", "CARD_READ_ALL", "PARKING_SESSION_CHECK_IN_ALL"];
  const envelope = (data: unknown) => ({ success: true, data, message: "ok", timestamp: new Date().toISOString() });
  await page.addInitScript(({ accountId }) => {
    const authority = "http://localhost:8081/realms/vehicle-management", clientId = "vehicle-management-frontend";
    const encode = (value: object) => btoa(JSON.stringify(value)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
    const exp = Math.floor(Date.now() / 1000) + 3600;
    sessionStorage.setItem("oidc.user:" + authority + ":" + clientId, JSON.stringify({
      access_token: encode({ alg: "none" }) + "." + encode({ account_id: accountId, exp }) + ".signature",
      expires_at: exp, scope: "openid profile email roles", token_type: "Bearer",
      profile: { aud: clientId, exp, iat: exp - 3600, iss: authority, sub: accountId, preferred_username: "coach-user" },
    }));
  }, { accountId });
  await page.route("**/vehicle-management/api/**", async (route) => {
    const request = route.request(), url = new URL(request.url()), p = url.pathname;
    requested.push(p);
    if (request.method() !== "GET") writes.push(p);
    if (p.endsWith("/iam/accounts/current-access")) return route.fulfill({ json: envelope({ accountId, username: "coach-user", roleCode: "EMPLOYEE", accountStatus: "ACTIVE", employeeStatus: "ACTIVE", accessState: "ACTIVE", accessRevision: "1", onboardingRequired: false, permissionCodes }) });
    if (p.endsWith("/iam/accounts/onboarding")) return route.fulfill({ json: envelope({ account: { accountId, roleCode: "EMPLOYEE", permissionCodes }, profile: { fullName: "Coach user" }, onboardingRequired: false }) });
    if (p.endsWith("/iam/accounts/profile/avatar-status")) return route.fulfill({ json: envelope(null) });
    if (p.endsWith("/user-guides/POST_ONBOARDING_EMPLOYEE_OPERATIONS")) return route.fulfill({ json: envelope({
      guideId: "overview", code: "POST_ONBOARDING_EMPLOYEE_OPERATIONS", title: "Hướng dẫn tổng quan", summary: "", deepLink: "/admin/dashboard", displayType: "COACH_MARK",
      replayPolicy: "MANUAL_ONLY", priority: 1, displayOrder: 1, versionNumber: 1, userState: null,
      steps: [{ stepId: "overview-first", displayOrder: 0, title: "Tổng quan chuẩn bị vận hành", body: "Kiểm tra phân công, bãi và ca.", anchorKey: "ADMIN.HELP.LAUNCHER", placement: "BOTTOM", collapseMs: 0, mediaUrl: null }],
    }) });
    if (p.endsWith("/notifications/user-guides/post-onboarding-readiness")) return route.fulfill({ json: envelope({
      accessRevision: "1", guideCode: "POST_ONBOARDING_EMPLOYEE_OPERATIONS", selectedParkingLotId: null, parkingLots: [], steps: [],
    }) });
    if (p.endsWith("/user-guides/WORKFLOW_CARD_STOCK")) {
      const guide = rows.find((row) => row.code === "WORKFLOW_CARD_STOCK")!;
      return route.fulfill({ json: envelope({
        guideId: "guide-stock", code: guide.code, title: guide.title, summary: "", deepLink: "/admin/card", displayType: "COACH_MARK",
        replayPolicy: "MANUAL_ONLY", priority: 1, displayOrder: 1, versionNumber: 1, userState: null,
        steps: guide.steps.map((step, index) => ({ stepId: "step-" + index, displayOrder: index, title: step.title, body: step.body, anchorKey: step.anchor, placement: "BOTTOM", collapseMs: 0, mediaUrl: null })),
      }) });
    }
    return route.fulfill({ json: envelope([]) });
  });
  return { writes, requested };
}
test("card help opens the card guide while header opens only overview and drawer has no coach launchers", async ({ page }) => {
  test.setTimeout(60_000);
  const state = await setup(page);
  await page.goto("/admin/card");
  await expect(page.getByText("1. Quyền tạo và quyền cấp thẻ khác nhau", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Mở lại hướng dẫn & trợ giúp", exact: true }).click();
  await expect(page.getByText("1. Quyền tạo và quyền cấp thẻ khác nhau", { exact: true })).toBeVisible();
  await expect(page.locator('[data-user-guide-anchor="WORKFLOW.CARDS.HEADER"]')).toBeVisible();
  const next = page.getByRole("button", { name: /Tiếp theo/ });
  await next.click();
  await expect(page.getByText("2. Chọn bãi và loại thẻ", { exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: "Mở hướng dẫn và hỗ trợ", exact: true }).click();
  await page.getByRole("menuitem", { name: "Hướng dẫn", exact: true }).click();
  await expect(page.getByText("Tổng quan chuẩn bị vận hành", { exact: true })).toBeVisible();
  await expect(page.getByText("1. Quyền tạo và quyền cấp thẻ khác nhau", { exact: true })).toHaveCount(0);
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: "Mở hướng dẫn và hỗ trợ", exact: true }).click();
  await page.getByRole("menuitem", { name: "Trợ giúp", exact: true }).click();
  const drawer = page.getByRole("dialog", { name: "Lộ trình từ thiết lập đến vận hành" });
  await expect(drawer).toBeVisible();
  await expect(drawer.getByText("Hướng dẫn bằng coach mark", { exact: true })).toHaveCount(0);
  await expect(drawer.getByRole("button", { name: /Hướng dẫn màn hình này|Xem coach mark tổng quan/ })).toHaveCount(0);
  await page.keyboard.press("Escape");
  await page.reload();
  await expect(page.getByText("1. Quyền tạo và quyền cấp thẻ khác nhau", { exact: true })).toHaveCount(0);
  expect(state.requested.some((p) => p.endsWith("/user-guides/WORKFLOW_CARD_STOCK"))).toBeTruthy();
  expect(state.requested.some((p) => p.endsWith("/user-guides/POST_ONBOARDING_EMPLOYEE_OPERATIONS"))).toBeTruthy();
  expect(state.writes.every((p) => p.includes("/user-guides/") && p.endsWith("/events"))).toBeTruthy();
});

