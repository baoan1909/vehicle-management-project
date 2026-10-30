import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/customer-portal/pages/PricingPage.tsx", import.meta.url);
const parkingApiPath = new URL("../src/features/parking/api/publicParkingApi.ts", import.meta.url);
const pricingApiPath = new URL("../src/features/pricing/api/pricingApi.ts", import.meta.url);
const endpointsPath = new URL("../src/core/api/apiEndpoints.ts", import.meta.url);
const subscriptionPagePath = new URL("../src/features/customer-portal/pages/SubscriptionPage.tsx", import.meta.url);
const [page, parkingApi, pricingApi, endpoints, subscriptionPage] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(parkingApiPath, "utf8"),
  readFile(pricingApiPath, "utf8"),
  readFile(endpointsPath, "utf8"),
  readFile(subscriptionPagePath, "utf8"),
]);

assert.match(endpoints, /parkingLotDirectory: "\/public\/parking-lots\/directory"/);
assert.match(parkingApi, /getPublicParkingLotDirectory/);
assert.match(parkingApi, /visitorAvailable: number/);
assert.match(parkingApi, /vehicleTypes: PublicParkingLotVehicleAvailability\[\]/);
assert.match(pricingApi, /export type PricePlanFilter = \{[\s\S]*parkingLotId\?: string/);
assert.match(page, /getPublicParkingLotDirectory\(\)/);
assert.match(page, /getPublicPricePlans\(\{[\s\S]*parkingLotId: selectedParkingLotId/);
assert.match(page, /getPublicPriceRules\(\{ isActive: true, parkingLotId: selectedParkingLotId \}\)/);
assert.match(page, /min-\[1100px\]:tw-grid-cols-4/);
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
assert.doesNotMatch(page, /type VehicleFilterKey = "MOTORBIKE" \| "CAR" \| "OTHER"/);
assert.match(page, /parkingLotId=\$\{encodeURIComponent\(registrationParkingLotId\)\}/);
assert.match(subscriptionPage, /const parkingLotIdFromUrl = searchParams\.get\("parkingLotId"\)/);
assert.match(subscriptionPage, /void changeParkingLot\(parkingLotIdFromUrl\)/);
assert.match(page, /navigator\.geolocation\.getCurrentPosition/);
assert.match(page, /Chưa có quyền GPS; bạn vẫn có thể xem và chọn bãi theo danh sách/);

console.log("Public pricing directory, per-lot pricing, status, GPS fallback, and registration guard checks passed.");
