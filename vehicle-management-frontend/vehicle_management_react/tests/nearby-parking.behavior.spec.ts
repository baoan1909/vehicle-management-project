import { expect, test, type Page, type Request } from "@playwright/test";

const apiBase = "**/vehicle-management/api";
const transparentPng = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M/wHwAF/gL+AvZ9WQAAAABJRU5ErkJggg==",
  "base64",
);

function envelope(data: unknown) {
  return { data, message: "ok", success: true, timestamp: new Date().toISOString() };
}

function expectNoAuthorization(request: Request) {
  expect(request.headers().authorization).toBeUndefined();
}

async function mockNearbyParkingApi(page: Page, requestedRadii: string[]) {
  await page.route("https://*.tile.openstreetmap.org/**", (route) => route.fulfill({
    body: transparentPng,
    contentType: "image/png",
  }));
  await page.route(`${apiBase}/notifications/broadcast-announcements/active`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope([])),
  }));
  await page.route(`${apiBase}/public/application-time`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope({ timeZone: "Asia/Ho_Chi_Minh" })),
  }));
  await page.route(`${apiBase}/public/parking-map/features`, (route) => route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(envelope({
      customerParkingMapEnabled: true,
      geocodingProviderEnabled: true,
      publicNearbySearchEnabled: true,
    })),
  }));
  await page.route(`${apiBase}/public/parking-lots/telemetry/geolocation`, (route) => {
    expectNoAuthorization(route.request());
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(null)) });
  });
  await page.route(`${apiBase}/public/parking-lots/nearby?**`, (route) => {
    expectNoAuthorization(route.request());
    const url = new URL(route.request().url());
    requestedRadii.push(url.searchParams.get("radiusKm") ?? "");
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope([])) });
  });
}

test.use({
  geolocation: { latitude: 10.850123, longitude: 106.771234 },
  permissions: ["geolocation"],
});

test("nearby map stays mounted and searches again when radius changes", async ({ page }) => {
  const pageErrors: Error[] = [];
  const requestedRadii: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error));
  await mockNearbyParkingApi(page, requestedRadii);

  await page.goto("/parking-lots/nearby");
  await page.getByRole("button", { name: "Tìm bãi gần tôi" }).click();
  await expect(page.getByRole("heading", { name: "Bãi xe trong 5 km" })).toBeVisible();
  await expect(page.getByText("Bản đồ chưa thể tải")).toHaveCount(0);

  for (const radius of [1, 3, 5]) {
    await page.getByRole("combobox", { name: "Bán kính tùy chỉnh (km)" }).click();
    await page.getByRole("option", { name: `${radius} km`, exact: true }).click();
    await page.getByRole("form", { name: "Nhập bán kính tìm kiếm", exact: true }).getByRole("button", { name: "Áp dụng", exact: true }).click();
    await expect(page.getByRole("heading", { name: `Bãi xe trong ${radius} km` })).toBeVisible();
    await expect(page.getByText("Bản đồ chưa thể tải")).toHaveCount(0);
  }

  await expect.poll(() => requestedRadii).toEqual(["5", "1", "3", "5"]);
  expect(pageErrors).toEqual([]);
});

test("manual radius accepts only integers, validates the range and accepts suggested radii", async ({ page }, testInfo) => {
  const radii: string[] = [];
  await mockNearbyParkingApi(page, radii);
  await page.goto("/parking-lots/nearby");
  const input = page.getByRole("combobox", { name: "Bán kính tùy chỉnh (km)" });
  const form = page.getByRole("form", { name: "Nhập bán kính tìm kiếm", exact: true });
  await input.fill("2");
  await input.press("Enter");
  await expect(page.getByRole("heading", { name: "Bãi xe trong 2 km" })).toBeVisible();
  await expect.poll(() => radii).toEqual(["2"]);
  for (const invalid of ["", "0", "51", "999"]) {
    await input.fill(invalid);
    await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
    await expect(input).toHaveAttribute("aria-invalid", "true");
    await expect(page.getByRole("alert")).toContainText("số nguyên từ 1 đến 50 km");
    await expect(page.getByRole("heading", { name: "Bãi xe trong 2 km" })).toBeVisible();
    expect(radii).toEqual(["2"]);
  }
  await input.fill("20");
  for (const forbidden of ["20km", "abc", "2.5", "1,5", "-1", "+2", "1e2"]) {
    await input.fill(forbidden);
    await expect(input).toHaveValue("20");
  }
  await input.pressSequentially("abc");
  await expect(input).toHaveValue("20");
  expect(radii).toEqual(["2"]);
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect.poll(() => radii).toEqual(["2", "20"]);
  await input.click();
  await page.getByRole("option", { name: "3 km", exact: true }).click();
  await expect(input).toHaveValue("3");
  expect(radii).toEqual(["2", "20"]);
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect.poll(() => radii).toEqual(["2", "20", "3"]);
  for (const radius of ["6", "7", "12", "50"]) {
    await input.fill(radius);
    await input.press("Enter");
    await expect(page.getByRole("heading", { name: `Bãi xe trong ${radius} km` })).toBeVisible();
    await expect.poll(() => radii.at(-1)).toEqual(radius);
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await input.scrollIntoViewIfNeeded();
  expect((await form.boundingBox())!.width).toBeLessThanOrEqual(390);
  await page.screenshot({ path: testInfo.outputPath("manual-radius-mobile.png") });
});

test("radius input uses one focus border, opens shared suggestions and supports keyboard and mobile", async ({ page }, testInfo) => {
  const radii: string[] = [];
  await mockNearbyParkingApi(page, radii);
  await page.setViewportSize({ width: 1440, height: 1080 });
  await page.goto("/parking-lots/nearby");
  const form = page.getByRole("form", { name: "Nhập bán kính tìm kiếm", exact: true });
  const input = form.getByRole("combobox", { name: "Bán kính tùy chỉnh (km)" });
  await expect(page.getByRole("button", { name: /^[135] km$/ })).toHaveCount(0);
  await input.click();
  const suggestions = page.getByRole("listbox", { name: "Gợi ý Bán kính tùy chỉnh (km)", exact: true });
  await expect(suggestions.getByText("Gợi ý", { exact: true })).toBeVisible();
  await expect(input.locator("..").locator(".fa-chevron-down")).toHaveCount(0);
  await expect(suggestions.getByRole("option")).toHaveText(["1 km", "3 km", "5 km"]);
  await expect(input).toBeFocused();
  await expect(input).toHaveCSS("box-shadow", "none");
  await expect(input).toHaveCSS("outline-width", "0px");
  await expect(input).toHaveCSS("border-top-width", "0px");
  expect((await input.locator("..").boundingBox())!.width).toBeGreaterThanOrEqual(400);
  await form.screenshot({ path: testInfo.outputPath("radius-input-desktop.png"), animations: "disabled" });
  await input.press("ArrowDown");
  await input.press("ArrowDown");
  await input.press("Enter");
  await expect(input).toHaveValue("3");
  await expect(suggestions).toBeHidden();
  await expect(page.getByRole("heading", { name: "Bãi xe trong 5 km" })).toBeVisible();
  await input.press("Enter");
  await expect(page.getByRole("heading", { name: "Bãi xe trong 3 km" })).toBeVisible();
  await expect.poll(() => radii).toEqual(["3"]);
  await input.fill("15");
  await input.press("Enter");
  await expect(page.getByRole("heading", { name: "Bãi xe trong 15 km" })).toBeVisible();
  await expect.poll(() => radii).toEqual(["3", "15"]);
  await input.click();
  await input.press("Escape");
  await expect(suggestions).toBeHidden();
  await input.click();
  await page.getByRole("heading", { name: "Bãi xe trong 15 km" }).click();
  await expect(suggestions).toBeHidden();
  await page.setViewportSize({ width: 390, height: 844 });
  await input.click();
  await expect(suggestions).toBeVisible();
  const bounds = (await form.boundingBox())!;
  expect(bounds.x + bounds.width).toBeLessThanOrEqual(390);
  await expect(suggestions).toBeInViewport();
  await page.screenshot({ path: testInfo.outputPath("radius-suggestions-mobile.png"), animations: "disabled" });
  await page.getByRole("option", { name: "1 km", exact: true }).click();
  await expect(input).toHaveValue("1");
});

test("Apply acquires location once, prevents duplicate requests and reuses it even for the same radius", async ({ page }) => {
  const radii: string[] = [];
  await mockNearbyParkingApi(page, radii);
  await page.addInitScript(() => {
    const probe = window as Window & { geolocationCalls: number; finishGeolocation: () => void };
    probe.geolocationCalls = 0;
    navigator.geolocation.getCurrentPosition = (success) => {
      probe.geolocationCalls++;
      probe.finishGeolocation = () => success({
        coords: { latitude: 10.850123, longitude: 106.771234, accuracy: 10 }, timestamp: Date.now(),
      } as GeolocationPosition);
    };
  });
  await page.goto("/parking-lots/nearby");
  const form = page.getByRole("form", { name: "Nhập bán kính tìm kiếm", exact: true });
  const input = form.getByRole("combobox", { name: "Bán kính tùy chỉnh (km)" });
  await input.fill("0");
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  expect(await page.evaluate(() => (window as Window & { geolocationCalls: number }).geolocationCalls)).toBe(0);
  await input.fill("7");
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect(form.getByRole("button", { name: "Đang lấy vị trí...", exact: true })).toBeDisabled();
  expect(radii).toEqual([]);
  await form.evaluate((element) => (element as HTMLFormElement).requestSubmit());
  expect(await page.evaluate(() => (window as Window & { geolocationCalls: number }).geolocationCalls)).toBe(1);
  await page.evaluate(() => (window as Window & { finishGeolocation: () => void }).finishGeolocation());
  await expect.poll(() => radii).toEqual(["7"]);
  await input.fill("50");
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect.poll(() => radii).toEqual(["7", "50"]);
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect.poll(() => radii).toEqual(["7", "50", "50"]);
  expect(await page.evaluate(() => (window as Window & { geolocationCalls: number }).geolocationCalls)).toBe(1);
});

test("Apply handles denied location through the existing address fallback", async ({ page }) => {
  const radii: string[] = [];
  await mockNearbyParkingApi(page, radii);
  await mockAddressApi(page, []);
  await page.addInitScript(() => {
    navigator.geolocation.getCurrentPosition = (_success, error) => error?.({
      code: 1, PERMISSION_DENIED: 1, POSITION_UNAVAILABLE: 2, TIMEOUT: 3, message: "Permission denied",
    });
  });
  await page.goto("/parking-lots/nearby");
  const form = page.getByRole("form", { name: "Nhập bán kính tìm kiếm", exact: true });
  await form.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("Bạn đã từ chối quyền vị trí");
  await expect(page.getByRole("dialog", { name: "Khu vực tìm kiếm", exact: true })).toBeVisible();
  expect(radii).toEqual([]);
});

async function mockAddressApi(page: Page, queries: string[], results = true, wardPathGate?: Promise<void>) {
  const province = { code: "79", name: "Hồ Chí Minh", fullName: "Thành phố Hồ Chí Minh" };
  const otherProvince = { code: "01", name: "Hà Nội", fullName: "Thành phố Hà Nội" };
  const district = { code: "760", name: "Quận 1", fullName: "Quận 1" };
  const ward = { code: "26734", name: "Bến Nghé", fullName: "Phường Bến Nghé" };
  await page.route(`${apiBase}/public/administrative-divisions/**`, async (route) => {
    expectNoAuthorization(route.request());
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/path")) await wardPathGate;
    const data = path.endsWith("/path")
      ? { province, district: path.includes("/legacy/") ? district : null, ward }
      : path.endsWith("/provinces") ? [province, otherProvince]
        : path.endsWith("/districts") ? [district] : [ward];
    return route.fulfill({ json: envelope(data) });
  });
  await page.route(`${apiBase}/public/parking-locations/search?**`, (route) => {
    expectNoAuthorization(route.request());
    queries.push(new URL(route.request().url()).searchParams.get("query") ?? "");
    return route.fulfill({ json: envelope(results ? [{ displayName: "Địa chỉ đã chọn", latitude: 10.78, longitude: 106.7 }] : []) });
  });
  await page.route(`${apiBase}/public/parking-locations/reverse?**`, (route) => route.fulfill({ json: envelope([]) }));
}

test("result tabs preserve lookup and keep the map mounted", async ({ page }, testInfo) => {
  await mockNearbyParkingApi(page, []);
  await page.route(`${apiBase}/public/parking/reservations/lookup?**`, (route) => {
    expectNoAuthorization(route.request());
    return route.fulfill({ json: envelope({ sessions: [], reservations: [] }) });
  });
  await page.goto("/parking-lots/nearby");
  const nearby = page.getByRole("tab", { name: "Kết quả gần nhất" });
  const lookup = page.getByRole("tab", { name: "Tra cứu vị trí đỗ xe" });
  await expect(nearby).toHaveAttribute("aria-selected", "true");
  await expect(page.getByLabel("Biển số xe", { exact: true })).toBeHidden();
  await page.locator(".leaflet-container").evaluate((element) => element.setAttribute("data-test-mounted", "yes"));
  await lookup.click();
  await expect(page.getByRole("heading", { name: "Bãi xe trong 5 km" })).toBeHidden();
  await page.getByLabel("Biển số xe", { exact: true }).fill("60A-999.99");
  await page.getByRole("button", { name: "Tra cứu", exact: true }).click();
  await expect(page.getByText("Không có phiên gửi xe đang mở", { exact: false })).toBeVisible();
  await nearby.click();
  await nearby.press("ArrowRight");
  await expect(lookup).toBeFocused();
  await expect(lookup).toHaveAttribute("aria-selected", "true");
  await expect(page.getByLabel("Biển số xe", { exact: true })).toHaveValue("60A-999.99");
  await expect(page.getByText("Không có phiên gửi xe đang mở", { exact: false })).toBeVisible();
  await expect(page.locator(".leaflet-container")).toHaveAttribute("data-test-mounted", "yes");
  await expect(page.getByRole("link", { name: "Trợ giúp", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Trợ giúp", exact: true })).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath("result-tabs.png") });
});

test("address dropdown searches only on Apply and restores the applied selection", async ({ page }, testInfo) => {
  const queries: string[] = [];
  const radii: string[] = [];
  let releaseWardPath!: () => void;
  const wardPathGate = new Promise<void>((resolve) => { releaseWardPath = resolve; });
  await mockNearbyParkingApi(page, radii);
  await mockAddressApi(page, queries, true, wardPathGate);
  await page.setViewportSize({ width: 1440, height: 1080 });
  await page.goto("/parking-lots/nearby");
  const trigger = page.getByRole("button", { name: "Tìm địa chỉ", exact: true });
  const mapButton = page.getByRole("button", { name: "Chọn điểm trên bản đồ", exact: true });
  await expect(trigger).toBeVisible();
  expect((await mapButton.boundingBox())!.x).toBeLessThan((await trigger.boundingBox())!.x);
  await trigger.click();
  const dialog = page.getByRole("dialog", { name: "Khu vực tìm kiếm" });
  await expect(page.getByRole("button", { name: "Phường hoặc xã", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Tỉnh hoặc thành phố", exact: true }).click();
  await page.getByRole("textbox", { name: "Tìm kiếm Tỉnh hoặc thành phố" }).fill("Hồ Chí Minh");
  await page.getByRole("button", { name: "Thành phố Hồ Chí Minh", exact: true }).click();
  await expect(dialog).toBeVisible();
  await page.getByRole("button", { name: "Phường hoặc xã", exact: true }).click();
  await page.getByRole("button", { name: "Phường Bến Nghé", exact: true }).click();
  await page.getByRole("textbox", { name: "Số nhà, tên đường" }).fill("12 Lê Duẩn");
  releaseWardPath();
  await expect(page.getByRole("button", { name: "Tỉnh hoặc thành phố", exact: true })).toBeEnabled();
  await expect(page.getByRole("textbox", { name: "Số nhà, tên đường" })).toHaveValue("12 Lê Duẩn");
  expect(queries).toEqual([]);
  await page.screenshot({ path: testInfo.outputPath("address-desktop.png") });
  await dialog.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect(dialog).toBeHidden();
  await expect.poll(() => queries).toEqual(["12 Lê Duẩn, Phường Bến Nghé, Thành phố Hồ Chí Minh"]);
  await expect.poll(() => radii).toEqual(["5"]);
  await trigger.click();
  await expect(page.getByRole("textbox", { name: "Số nhà, tên đường" })).toHaveValue("12 Lê Duẩn");
  await page.getByRole("button", { name: "Xóa lọc", exact: true }).click();
  await expect(page.getByRole("textbox", { name: "Số nhà, tên đường" })).toBeEmpty();
  await expect(dialog.getByRole("button", { name: "Áp dụng", exact: true })).toBeDisabled();
  await page.keyboard.press("Escape");
  await expect(dialog).toBeHidden();
  await trigger.click();
  await expect(page.getByRole("textbox", { name: "Số nhà, tên đường" })).toHaveValue("12 Lê Duẩn");
  await page.getByRole("button", { name: "Tỉnh hoặc thành phố", exact: true }).click();
  await page.keyboard.press("Escape");
  await expect(dialog).toBeVisible();
  await expect(page.getByRole("textbox", { name: "Tìm kiếm Tỉnh hoặc thành phố" })).toBeHidden();
  await page.getByRole("textbox", { name: "Số nhà, tên đường" }).fill("Địa chỉ chưa áp dụng");
  await page.getByRole("heading", { name: "Bãi xe trong 5 km" }).click();
  await expect(dialog).toBeHidden();
  await trigger.click();
  await expect(page.getByRole("textbox", { name: "Số nhà, tên đường" })).toHaveValue("12 Lê Duẩn");
  expect(queries).toHaveLength(1);
});

test("legacy dropdown fits mobile, resets dependent fields and retains failed search draft", async ({ page }, testInfo) => {
  const queries: string[] = [];
  await page.setViewportSize({ width: 390, height: 844 });
  await mockNearbyParkingApi(page, []);
  await mockAddressApi(page, queries, false);
  await page.goto("/parking-lots/nearby");
  await page.getByRole("button", { name: "Tìm địa chỉ", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Khu vực tìm kiếm" });
  await page.getByRole("button", { name: "Địa chỉ cũ (có huyện)", exact: true }).click();
  await expect(page.getByRole("button", { name: "Quận hoặc huyện", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "Tỉnh hoặc thành phố", exact: true }).click();
  await page.getByRole("button", { name: "Thành phố Hồ Chí Minh", exact: true }).click();
  await page.getByRole("button", { name: "Quận hoặc huyện", exact: true }).click();
  await page.getByRole("button", { name: "Quận 1", exact: true }).click();
  await page.getByRole("button", { name: "Phường hoặc xã", exact: true }).click();
  await page.getByRole("button", { name: "Phường Bến Nghé", exact: true }).click();
  await page.getByRole("textbox", { name: "Số nhà, tên đường" }).fill("12 Lê Duẩn");
  await dialog.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect(dialog.getByRole("alert")).toContainText("Không tìm thấy tọa độ");
  await expect(page.getByRole("textbox", { name: "Số nhà, tên đường" })).toHaveValue("12 Lê Duẩn");
  expect(queries).toEqual(["12 Lê Duẩn, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh"]);
  const bounds = (await dialog.boundingBox())!;
  expect(bounds.x).toBeGreaterThanOrEqual(0);
  expect(bounds.x + bounds.width).toBeLessThanOrEqual(390);
  expect(bounds.y + bounds.height).toBeLessThanOrEqual(844);
  await expect(dialog.getByRole("button", { name: "Xóa lọc", exact: true })).toBeInViewport();
  const clearBounds = (await dialog.getByRole("button", { name: "Xóa lọc", exact: true }).boundingBox())!;
  const applyBounds = (await dialog.getByRole("button", { name: "Áp dụng", exact: true }).boundingBox())!;
  expect(clearBounds.height).toBeGreaterThanOrEqual(44);
  expect(applyBounds.y).toBe(clearBounds.y);
  expect(applyBounds.x).toBeGreaterThan(clearBounds.x);
  await page.screenshot({ path: testInfo.outputPath("address-mobile.png") });
  await page.getByRole("button", { name: "Tỉnh hoặc thành phố", exact: true }).click();
  await page.getByRole("button", { name: "Thành phố Hà Nội", exact: true }).click();
  await expect(page.getByRole("button", { name: "Phường hoặc xã", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Quận hoặc huyện", exact: true })).toContainText("Chọn quận/huyện");
});

test("map picking remains available before address and is mutually exclusive with dropdown", async ({ page }) => {
  const radii: string[] = [];
  await mockNearbyParkingApi(page, radii);
  await mockAddressApi(page, []);
  await page.goto("/parking-lots/nearby");
  await page.getByRole("button", { name: "Chọn điểm trên bản đồ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Đang chờ chọn điểm", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Tìm địa chỉ", exact: true }).click();
  await expect(page.getByRole("button", { name: "Chọn điểm trên bản đồ", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Đóng tìm địa chỉ" }).click();
  await page.getByRole("button", { name: "Chọn điểm trên bản đồ", exact: true }).click();
  await page.locator(".leaflet-container").click({ position: { x: 180, y: 150 } });
  await expect.poll(() => radii).toEqual(["5"]);
  await expect(page.getByRole("button", { name: "Chọn điểm trên bản đồ", exact: true })).toBeVisible();
});

test("nearby ticket action spans the two existing actions and opens the matching pricing detail", async ({ page }, testInfo) => {
  await mockNearbyParkingApi(page, []);
  const lot = { parkingLotId: "lot-hcmute", name: "Bãi xe HCMUTE", addressDisplay: "Thủ Đức", latitude: 10.85, longitude: 106.77, status: "ACTIVE", distanceMeters: 91, distanceKm: 0.091 };
  await page.route(`${apiBase}/public/parking-lots/nearby?**`, (route) => route.fulfill({ json: envelope([lot]) }));
  await page.route(`${apiBase}/public/parking-lots/directory`, (route) => route.fulfill({ json: envelope([{ ...lot, vehicleTypes: [], hasAvailability: true, visitorAvailable: 10 }]) }));
  await page.route(`${apiBase}/public/pricing/**`, (route) => route.fulfill({ json: envelope([]) }));
  await page.route(`${apiBase}/public/parking-lots/lot-hcmute/reviews?**`, (route) => route.fulfill({ json: envelope({
    summary: { averageRating: 0, reviewCount: 0, distribution: {} }, content: [], totalElements: 0, totalPages: 0,
  }) }));
  await page.goto("/parking-lots/nearby");
  await page.getByRole("button", { name: "Tìm bãi gần tôi" }).click();
  const card = page.getByRole("article").filter({ hasText: lot.name });
  await card.getByRole("button", { name: /Bãi xe HCMUTE/ }).click();
  const ticket = card.getByRole("link", { name: "Đăng ký vé", exact: true });
  await expect(ticket).toBeVisible();
  const direction = (await card.getByRole("link", { name: /Mở Google Maps chỉ đường/ }).boundingBox())!;
  const reservation = (await card.getByRole("button", { name: "Đặt chỗ trước", exact: true }).boundingBox())!;
  const ticketBounds = (await ticket.boundingBox())!;
  expect(Math.abs(ticketBounds.x - direction.x)).toBeLessThanOrEqual(2);
  expect(Math.abs(ticketBounds.width - (reservation.x + reservation.width - direction.x))).toBeLessThanOrEqual(2);
  expect(ticketBounds.y).toBeGreaterThanOrEqual(direction.y + direction.height);
  await card.screenshot({ path: testInfo.outputPath("nearby-ticket-action.png"), animations: "disabled" });
  await ticket.click();
  await expect(page).toHaveURL(/\/pricing\/lot-hcmute$/);
  await expect(page.getByText("Chi tiết bảng giá", { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: lot.name, exact: true })).toBeVisible();
});
