import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/customer-portal/pages/PricingPage.tsx", import.meta.url);
const parkingApiPath = new URL("../src/features/parking/api/publicParkingApi.ts", import.meta.url);
const pricingApiPath = new URL("../src/features/pricing/api/pricingApi.ts", import.meta.url);
const endpointsPath = new URL("../src/core/api/apiEndpoints.ts", import.meta.url);
const subscriptionPagePath = new URL("../src/features/customer-portal/pages/SubscriptionPage.tsx", import.meta.url);
const routesPath = new URL("../src/app/routes.tsx", import.meta.url);
const [page, parkingApi, pricingApi, endpoints, subscriptionPage, routes] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(parkingApiPath, "utf8"),
  readFile(pricingApiPath, "utf8"),
  readFile(endpointsPath, "utf8"),
  readFile(subscriptionPagePath, "utf8"),
  readFile(routesPath, "utf8"),
]);

assert.match(endpoints, /parkingLotDirectory: "\/public\/parking-lots\/directory"/);
assert.match(parkingApi, /getPublicParkingLotDirectory/);
assert.match(parkingApi, /visitorAvailable: number/);
assert.match(parkingApi, /minimumPrice: number \| null/);
assert.match(parkingApi, /completedSessionCount: number/);
assert.match(parkingApi, /vehicleTypes: PublicParkingLotVehicleAvailability\[\]/);
assert.match(pricingApi, /export type PricePlanFilter = \{[\s\S]*parkingLotId\?: string/);
assert.match(page, /getPublicParkingLotDirectory\(\)/);
assert.match(page, /getPublicPricePlans\(\{[\s\S]*parkingLotId: selectedParkingLotId/);
assert.match(page, /getPublicPriceRules\(\{ isActive: true, parkingLotId: selectedParkingLotId \}\)/);
assert.match(page, /min-\[1100px\]:tw-grid-cols-4/);
assert.match(page, /filter\(\(lot\) => lot\.status === "ACTIVE"\)/);
assert.match(page, /formatPriceRange\(lot\)/);
assert.match(page, /Đã phục vụ \{formatServedCount\(lot\.completedSessionCount\)\} lượt/);
assert.match(page, /getCustomerPortalVoucherBanners/);
assert.match(page, /to=\{`\/pricing\/\$\{lot\.parkingLotId\}`\}/);
assert.match(routes, /path: "\/pricing\/:parkingLotId"/);
assert.match(page, /Đang hoạt động/);
assert.match(page, /Đang bảo trì/);
assert.match(page, /Đang thiết lập/);
assert.match(page, /registrationDisabled=\{selectedParkingLot\?\.status !== "ACTIVE"\}/);
assert.match(page, /options: VehicleTypeApiResponse\[\]/);
assert.match(page, /\(\) => \[\.\.\.vehicleTypes\]\.sort/);
assert.match(page, /function vehicleDisplayOrder\(vehicleType: VehicleTypeApiResponse\)/);
assert.match(page, /vehicleDisplayOrder\(left\) - vehicleDisplayOrder\(right\)/);
assert.match(page, /return "fas fa-bus"/);
assert.match(page, /rule\.vehicleTypeId === selectedVehicleTypeId/);
assert.match(page, /option\.vehicleTypeId === selectedVehicleTypeId/);
assert.match(page, /selectedTicketTypeId/);
assert.match(page, /availableCustomerTicketTypes/);
assert.match(page, /setShowAllCustomerRules\(true\)/);
assert.match(page, /title="Tất cả vé đăng ký"/);
assert.match(page, /vehicleCustomerRules\.map\(\(rule\)/);
assert.doesNotMatch(page, /type VehicleFilterKey = "MOTORBIKE" \| "CAR" \| "OTHER"/);
assert.match(page, /parkingLotId=\$\{encodeURIComponent\(registrationParkingLotId\)\}/);
assert.match(subscriptionPage, /const parkingLotIdFromUrl = searchParams\.get\("parkingLotId"\)/);
assert.match(subscriptionPage, /void changeParkingLot\(parkingLotIdFromUrl\)/);
assert.match(page, /navigator\.geolocation\.getCurrentPosition/);
assert.match(page, /Chưa có quyền GPS; bạn vẫn có thể xem và chọn bãi theo danh sách/);

console.log("Public pricing directory, per-lot pricing, status, GPS fallback, and registration guard checks passed.");
