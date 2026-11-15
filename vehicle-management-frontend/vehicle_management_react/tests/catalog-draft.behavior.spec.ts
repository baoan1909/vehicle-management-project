import { expect, test, type Page } from "@playwright/test";

test.setTimeout(60_000);
test.use({ actionTimeout: 15_000 });
const api = "**/vehicle-management/api";
const accountId = "00000000-0000-0000-0000-0000000000a1";
const envelope = (data: unknown) => ({ success: true, message: "ok", data, timestamp: new Date().toISOString() });
const readPermissions = ["PARKING_LOT_READ_ALL", "TICKET_TYPE_READ_ALL", "VEHICLE_TYPE_READ_ALL", "PRICE_PLAN_READ_ALL", "PRICE_RULE_READ_ALL"];

async function setup(page: Page, roleCode = "PARTNER_ADMIN", writePermissions = ["PARKING_SCOPE_PARTNER", "PRICE_PLAN_UPDATE_ALL", "PRICE_PLAN_CREATE_ALL", "VEHICLE_TYPE_UPDATE_ALL"]) {
  const permissions = [...readPermissions, ...writePermissions];
  await page.addInitScript(({ accountId }) => {
    const authority = "http://localhost:8081/realms/vehicle-management";
    const clientId = "vehicle-management-frontend";
    const encode = (value: object) => btoa(JSON.stringify(value)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
    const exp = Math.floor(Date.now() / 1000) + 3600;
    sessionStorage.setItem(`oidc.user:${authority}:${clientId}`, JSON.stringify({
      access_token: `${encode({ alg: "none" })}.${encode({ account_id: accountId, exp, preferred_username: "catalog-user" })}.signature`,
      expires_at: exp, scope: "openid profile email roles", token_type: "Bearer",
      profile: { aud: clientId, exp, iat: exp - 3600, iss: authority, preferred_username: "catalog-user", sub: accountId },
    }));
  }, { accountId });
  await page.route(`${api}/**`, (route) => route.fulfill({ json: envelope([]) }));
  await page.route(`${api}/iam/accounts/current-access`, (route) => route.fulfill({ json: envelope({
    accountId, username: "catalog-user", accountStatus: "ACTIVE", employeeStatus: "ACTIVE",
    accessState: "ACTIVE", accessRevision: "1", roleCode, onboardingRequired: false,
    activeOrganizationIds: ["org-a"], assignedParkingLotIds: ["lot-a"], permissionCodes: permissions,
  }) }));
  await page.route(`${api}/iam/accounts/onboarding`, (route) => route.fulfill({ json: envelope({
    account: { accountId, username: "catalog-user", roleCode, permissionCodes: permissions },
    profile: { fullName: "Catalog user" }, onboardingRequired: false,
  }) }));
  await page.route(`${api}/public/application-time`, (route) => route.fulfill({ json: envelope({ timeZone: "Asia/Ho_Chi_Minh" }) }));
  await page.route(`${api}/parking/parking-lots**`, (route) => route.fulfill({ json: envelope([
    { parkingLotId: "lot-a", organizationId: "org-a", code: "LOT_A", name: "Assigned lot", status: "SETUP" },
  ]) }));
  await page.route(`${api}/catalog/parking-lots/lot-a/availability`, (route) => route.fulfill({ json: envelope({
    parkingLotId: "lot-a", organizationId: "org-a", excludedVehicleTypeIds: [], excludedTicketTypeIds: [], drainingVehicleTypeIds: [],
  }) }));
  const common = { organizationId: "org-a", description: "", createdAt: null, updatedAt: null, createdBy: null, updatedBy: null };
  const vehicles = [
    { ...common, vehicleTypeId: "vehicle-active", code: "MOTORBIKE", name: "Active vehicle", isActive: true, lifecycleStatus: "ACTIVE" },
    { ...common, vehicleTypeId: "vehicle-draft", code: "CAR", name: "Draft vehicle", isActive: false, lifecycleStatus: "DRAFT" },
  ];
  const tickets = [
    { ...common, ticketTypeId: "ticket-active", code: "DAILY", name: "Active ticket", durationDays: 1, status: "ACTIVE" },
    { ...common, ticketTypeId: "ticket-draft", code: "MONTHLY", name: "Draft ticket", durationDays: 30, status: "DRAFT" },
  ];
  let plans = [
    { ...common, pricePlanId: "plan-active", code: "PLAN_ACTIVE", name: "Active plan", appliesTo: "VISITOR", effectiveFrom: "2000-01-01", effectiveTo: null, isActive: true, lifecycleStatus: "ACTIVE" },
    { ...common, pricePlanId: "plan-draft", code: "PLAN_DRAFT", name: "Draft plan", appliesTo: "CUSTOMER", effectiveFrom: "2000-01-01", effectiveTo: null, isActive: false, lifecycleStatus: "DRAFT" },
  ];
  const rules = [
    { ...common, priceRuleId: "rule-active", pricePlanId: "plan-active", vehicleTypeId: "vehicle-active", ticketTypeId: "ticket-active", ruleName: "Active rule", basePrice: 5000, lostCardFee: 0, priority: 1, unit: "TURN", timeFrom: "06:00:00", timeTo: "19:59:59", isActive: true, lifecycleStatus: "ACTIVE" },
    { ...common, priceRuleId: "rule-draft", pricePlanId: "plan-draft", vehicleTypeId: "vehicle-draft", ticketTypeId: "ticket-draft", ruleName: "Draft rule", basePrice: 50000, lostCardFee: 0, priority: 1, unit: "MONTH", timeFrom: null, timeTo: null, isActive: false, lifecycleStatus: "DRAFT" },
  ];
  await page.route(`${api}/catalog/vehicle-types**`, (route) => route.fulfill({ json: envelope(vehicles) }));
  await page.route(`${api}/catalog/ticket-types**`, (route) => route.fulfill({ json: envelope(tickets) }));
  await page.route(`${api}/catalog/price-rules**`, (route) => route.fulfill({ json: envelope(rules) }));
  const savedPayloads: Record<string, unknown>[] = [];
  await page.route(`${api}/catalog/price-plans**`, async (route) => {
    if (route.request().method() === "PUT") {
      const payload = route.request().postDataJSON();
      savedPayloads.push(payload);
      plans = plans.map((plan) => plan.pricePlanId === "plan-draft" ? { ...plan, ...payload } : plan);
      return route.fulfill({ json: envelope(plans.find((plan) => plan.pricePlanId === "plan-draft")) });
    }
    return route.fulfill({ json: envelope(plans) });
  });
  return { savedPayloads };
}

for (const [path, activeName, draftName] of [
  ["/admin/ticket", "Active ticket", "Draft ticket"],
  ["/admin/vehicle", "Active vehicle", "Draft vehicle"],
  ["/admin/price-plans", "Active plan", "Draft plan"],
  ["/admin/price-rules", "Active rule", "Draft rule"],
]) {
  test(`${path} defaults to active after reload and exposes a separate draft list`, async ({ page }) => {
    await setup(page);
    await page.goto(path);
    await expect(page.getByText(activeName, { exact: true }).first()).toBeVisible({ timeout: 20_000 });
    await expect(page.getByText(draftName, { exact: true })).toHaveCount(0);
    await page.getByRole("button", { name: "Xem nháp", exact: true }).click();
    await expect(page.getByText(draftName, { exact: true }).first()).toBeVisible();
    await page.reload();
    await expect(page.getByText(activeName, { exact: true }).first()).toBeVisible();
    await expect(page.getByText(draftName, { exact: true })).toHaveCount(0);
  });
}

test("editing a draft plan persists through the API and survives reload without publishing", async ({ page }) => {
  const { savedPayloads } = await setup(page);
  await page.goto("/admin/price-plans");
  await page.getByRole("button", { name: "Xem nháp", exact: true }).click();
  await page.getByText("Draft plan", { exact: true }).click();
  await page.locator('input[value="Draft plan"]').fill("Saved draft plan");
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect.poll(() => savedPayloads.length).toBe(1);
  expect(savedPayloads[0]).toMatchObject({ name: "Saved draft plan", effectiveFrom: "2000-01-01" });
  expect(savedPayloads[0]).not.toHaveProperty("isActive");
  await page.reload();
  await page.getByRole("button", { name: "Xem nháp", exact: true }).click();
  await expect(page.getByText("Saved draft plan", { exact: true })).toBeVisible();
});

test("assigned manager keeps permitted vehicle edits but cannot write organization pricing with action permission alone", async ({ page }) => {
  await setup(page, "PARKING_MANAGER", ["PARKING_SCOPE_ASSIGNED", "VEHICLE_TYPE_UPDATE_ALL", "PRICE_PLAN_CREATE_ALL", "PRICE_PLAN_UPDATE_ALL"]);
  await page.goto("/admin/vehicle");
  await page.getByText("Active vehicle", { exact: true }).first().click();
  await expect(page.getByRole("button", { name: /Cập nhật$/ })).toBeVisible();
  await page.goto("/admin/price-plans");
  await expect(page.getByText("Active plan", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: /Tạo kế hoạch/ })).toHaveCount(0);
});

test("employee with explicitly granted action and organization scope is not blocked by role name", async ({ page }) => {
  await setup(page, "EMPLOYEE", ["PARKING_SCOPE_PARTNER", "PRICE_PLAN_CREATE_ALL", "PRICE_PLAN_UPDATE_ALL"]);
  await page.goto("/admin/price-plans");
  await expect(page.getByRole("button", { name: /Tạo kế hoạch/ })).toBeVisible();
});

test("publishing a plan requires explicit confirmation of template effective dates", async ({ page }) => {
  await setup(page);
  let publications = 0;
  await page.route(`${api}/catalog/price-plans/plan-draft/activate`, (route) => {
    expect(route.request().method()).toBe("PATCH");
    publications++;
    return route.fulfill({ json: envelope({ pricePlanId: "plan-draft", code: "PLAN_DRAFT", name: "Draft plan", appliesTo: "CUSTOMER", effectiveFrom: "2000-01-01", effectiveTo: null, isActive: true, lifecycleStatus: "ACTIVE" }) });
  });
  await page.goto("/admin/price-plans");
  await page.getByRole("button", { name: "Xem nháp", exact: true }).click();
  await page.getByRole("button", { name: "Kích hoạt lại kế hoạch Draft plan", exact: true }).click();
  const confirm = page.getByRole("button", { name: "Kích hoạt lại", exact: true });
  await expect(confirm).toBeDisabled();
  expect(publications).toBe(0);
  await page.getByRole("checkbox", { name: /Tôi xác nhận ngày hiệu lực/ }).check();
  await expect(confirm).toBeEnabled();
  await confirm.click();
  await expect.poll(() => publications).toBe(1);
});
