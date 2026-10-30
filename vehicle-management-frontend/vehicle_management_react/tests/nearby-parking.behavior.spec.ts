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
    await page.getByRole("button", { name: `${radius} km`, exact: true }).click();
    await expect(page.getByRole("heading", { name: `Bãi xe trong ${radius} km` })).toBeVisible();
    await expect(page.getByText("Bản đồ chưa thể tải")).toHaveCount(0);
  }

  await expect.poll(() => requestedRadii).toEqual(["5", "1", "3", "5"]);
  expect(pageErrors).toEqual([]);
});
