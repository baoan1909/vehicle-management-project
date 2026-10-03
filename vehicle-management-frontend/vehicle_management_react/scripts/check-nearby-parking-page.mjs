import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/parking/pages/NearbyParkingLotsPage.tsx", import.meta.url);
const apiPath = new URL("../src/features/parking/api/publicParkingApi.ts", import.meta.url);
const mapPath = new URL("../src/features/parking/components/NearbyParkingMap.tsx", import.meta.url);
const [page, api, map] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(apiPath, "utf8"),
  readFile(mapPath, "utf8"),
]);

assert.match(page, /function handleFindNearMe\(\)[\s\S]*navigator\.geolocation\.getCurrentPosition/);
assert.doesNotMatch(page, /localStorage|sessionStorage/);
assert.match(page, /const RADIUS_OPTIONS = \[1, 3, 5\]/);
assert.match(page, /api: "1"/);
assert.match(page, /travelmode: "driving"/);
assert.match(page, /ParkingAddressPicker/);
assert.match(page, /Bản đồ hoặc tile đang lỗi[\s\S]*Danh sách và nút chỉ đường vẫn hoạt động/);
assert.match(api, /getParkingMapFeatures/);
assert.match(api, /parkingMapFeatures/);
assert.match(page, /void getParkingMapFeatures\(\)/);
assert.match(page, /Bản đồ tìm bãi hiện chưa được bật/);
assert.match(page, /!featureStatus\?\.customerParkingMapEnabled/);
assert.match(page, /featureStatus\?\.geocodingProviderEnabled/);
assert.match(page, /Đang kiểm tra trạng thái bản đồ/);
assert.match(page, /Không có bãi trong \{radiusKm\} km/);
assert.match(page, /Đang tìm bãi xe/);
assert.match(page, /Bạn đã từ chối quyền vị trí/);
assert.match(page, /Không lấy được vị trí/);
assert.match(page, /Không thể tìm bãi xe lúc này/);
assert.match(page, /origin: `\$\{origin\.latitude\},\$\{origin\.longitude\}`/);
assert.match(page, /destination: `\$\{destination\.latitude\},\$\{destination\.longitude\}`/);assert.match(api, /nearbyParkingLots/);
assert.match(api, /recordGeolocationOutcome/);
assert.match(api, /body: \{ outcome \}/);
assert.doesNotMatch(api, /body: \{[^}]*latitude[^}]*\}/);
assert.match(map, /alt={`Bãi xe/);
assert.match(map, /alt="Vị trí tìm kiếm của bạn"/);

console.log("Nearby parking page privacy, fallback, accessibility, and routing checks passed.");
