import { expect, test, type Page } from "@playwright/test";

const api = "**/vehicle-management/api";
const accountId = "00000000-0000-0000-0000-0000000000c1";
const envelope = (data: unknown) => ({ data, success: true, message: "ok", timestamp: new Date().toISOString() });
const oldVehicle = { customerVehicleId: "vehicle-1", vehicleTypeId: "motorbike", licensePlate: "59A-111.11", brand: "Honda", color: "Trắng", status: "ACTIVE", isDefault: true };

async function setup(page: Page, canCreate = true) {
  const permissions = ["SUBSCRIPTION_READ_OWN", "SUBSCRIPTION_CREATE_OWN", ...(canCreate ? ["CUSTOMER_VEHICLE_CREATE_OWN"] : [])];
  await page.addInitScript(({ accountId }) => {
    const authority = "http://localhost:8081/realms/vehicle-management";
    const clientId = "vehicle-management-frontend";
    const encode = (value: object) => btoa(JSON.stringify(value)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
    const exp = Math.floor(Date.now() / 1000) + 3600;
    sessionStorage.setItem(`oidc.user:${authority}:${clientId}`, JSON.stringify({
      access_token: `${encode({ alg: "none" })}.${encode({ account_id: accountId, exp, preferred_username: "vehicle-customer" })}.signature`,
      expires_at: exp, scope: "openid profile email roles", token_type: "Bearer",
      profile: { aud: clientId, exp, iat: exp - 3600, iss: authority, preferred_username: "vehicle-customer", sub: accountId },
    }));
  }, { accountId });
  await page.route(`${api}/**`, (route) => route.fulfill({ json: envelope([]) }));
  await page.route(`${api}/iam/accounts/current-access`, (route) => route.fulfill({ json: envelope({
    accountId, username: "vehicle-customer", accountStatus: "ACTIVE", accessState: "ACTIVE", accessRevision: "1", roleCode: "CUSTOMER",
    customerApprovalStatus: "APPROVED", customerStatus: "ACTIVE", onboardingRequired: false, permissionCodes: permissions,
  }) }));
  await page.route(`${api}/iam/accounts/onboarding`, (route) => route.fulfill({ json: envelope({
    account: { accountId, username: "vehicle-customer", roleCode: "CUSTOMER", permissionCodes: permissions },
    profile: { fullName: "Khách đăng ký vé" }, customer: { customerId: "customer-1" }, onboardingRequired: false,
  }) }));
  await page.route(`${api}/public/application-time`, (route) => route.fulfill({ json: envelope({ timeZone: "Asia/Ho_Chi_Minh" }) }));
  await page.route(`${api}/public/parking-lots`, (route) => route.fulfill({ json: envelope([
    { parkingLotId: "lot-1", name: "Bãi xe HCMUTE", addressDisplay: "Thủ Đức, TP Hồ Chí Minh", status: "ACTIVE" },
  ]) }));
  await page.route(`${api}/public/pricing/vehicle-types**`, (route) => route.fulfill({ json: envelope([
    { vehicleTypeId: "motorbike", name: "Xe máy", code: "MOTORBIKE", status: "ACTIVE" },
    { vehicleTypeId: "car", name: "Ô tô", code: "CAR", status: "ACTIVE" },
  ]) }));
  await page.route(`${api}/public/pricing/ticket-types**`, (route) => route.fulfill({ json: envelope([
    { ticketTypeId: "monthly", name: "Vé tháng", code: "MONTHLY", status: "ACTIVE", durationDays: 30 },
  ]) }));
  await page.route(`${api}/people/customer-vehicles/me`, (route) => route.fulfill({ json: envelope([oldVehicle]) }));
  await page.goto("/customer/subscriptions?register=true&parkingLotId=lot-1");
  const dialog = page.getByRole("dialog", { name: "Đăng ký vé mới", exact: true });
  await expect(dialog.getByRole("button", { name: "Chọn xe đăng ký vé", exact: true })).toContainText(oldVehicle.licensePlate);
  return dialog;
}

test("creating a vehicle returns to the existing registration with the new vehicle selected", async ({ page }, testInfo) => {
  const dialog = await setup(page);
  const payloads: unknown[] = [];
  let subscriptionPosts = 0;
  page.on("request", (request) => { if (request.method() === "POST" && request.url().endsWith("/subscriptions/me")) subscriptionPosts++; });
  await page.route(`${api}/people/customer-vehicles/me`, async (route) => {
    if (route.request().method() !== "POST") return route.fulfill({ json: envelope([oldVehicle]) });
    const payload = route.request().postDataJSON();
    payloads.push(payload);
    return route.fulfill({ status: 201, json: envelope({ ...payload, customerVehicleId: "vehicle-new", status: "ACTIVE" }) });
  });
  await dialog.getByRole("button", { name: "Tăng số kỳ thanh toán" }).click();
  await dialog.getByPlaceholder("Ví dụ: WELCOME10").fill("WELCOME10");
  await dialog.getByRole("button", { name: "Thêm mới", exact: true }).click();
  const vehicle = page.getByRole("dialog", { name: "Thêm phương tiện", exact: true });
  await expect(dialog).toBeHidden();
  await vehicle.getByRole("button", { name: "Chọn loại xe", exact: true }).click();
  await page.getByRole("button", { name: "Ô tô", exact: true }).click();
  await vehicle.getByLabel("Biển số xe", { exact: true }).fill("51a-222.22");
  await vehicle.getByLabel("Hãng xe", { exact: true }).fill("Toyota");
  await vehicle.getByLabel("Màu xe", { exact: true }).fill("Đen");
  await vehicle.getByLabel("Đặt làm xe mặc định").check();
  await page.screenshot({ path: testInfo.outputPath("create-vehicle-dialog.png"), animations: "disabled" });
  await vehicle.getByRole("button", { name: "Lưu xe", exact: true }).click();
  await expect(vehicle).toBeHidden();
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole("button", { name: "Chọn xe đăng ký vé", exact: true })).toContainText("51A-222.22");
  await expect(dialog.getByPlaceholder("Ví dụ: WELCOME10")).toHaveValue("WELCOME10");
  await expect(dialog.locator("label").filter({ hasText: "Số kỳ thanh toán" }).getByText("2", { exact: true })).toBeVisible();
  await dialog.getByRole("button", { name: "Chọn xe đăng ký vé", exact: true }).click();
  await expect(page.getByRole("button", { name: /51A-222.22 - Toyota - Ô tô/ })).toBeVisible();
  expect(payloads).toEqual([{ vehicleTypeId: "car", licensePlate: "51A-222.22", brand: "Toyota", color: "Đen", isDefault: true }]);
  expect(subscriptionPosts).toBe(0);
});

test("failed vehicle save keeps the form; cancel restores registration without changing its vehicle", async ({ page }) => {
  const dialog = await setup(page);
  await page.route(`${api}/people/customer-vehicles/me`, (route) => route.fulfill({ status: 409, json: { success: false, message: "Biển số xe đã tồn tại." } }));
  await dialog.getByRole("button", { name: "Thêm mới", exact: true }).click();
  const vehicle = page.getByRole("dialog", { name: "Thêm phương tiện", exact: true });
  await vehicle.getByLabel("Biển số xe", { exact: true }).fill("59A-111.11");
  await vehicle.getByRole("button", { name: "Lưu xe", exact: true }).click();
  await expect(vehicle.getByRole("alert")).toContainText("đã tồn tại");
  await expect(vehicle.getByLabel("Biển số xe", { exact: true })).toHaveValue("59A-111.11");
  await vehicle.getByRole("button", { name: "Hủy", exact: true }).click();
  await expect(dialog.getByRole("button", { name: "Chọn xe đăng ký vé", exact: true })).toContainText(oldVehicle.licensePlate);
});

test("a customer without vehicle creation permission keeps registration without the add action", async ({ page }) => {
  const dialog = await setup(page, false);
  await expect(dialog.getByRole("button", { name: "Thêm mới", exact: true })).toHaveCount(0);
});
