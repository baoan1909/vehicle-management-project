import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const overview = read("../src/features/parking/pages/ParkingLotOverviewPage.tsx");
const header = read("../src/features/parking/components/ParkingLotWorkspaceHeader.tsx");
const routes = read("../src/app/routes.tsx");
const navigation = read("../src/config/navigation.ts");

assert.match(routes, /path: "\/admin\/parking-lots"[\s\S]*<ParkingLotOverviewPage/);
assert.match(routes, /path: "\/admin\/parking-structure"[\s\S]*<ParkingLotStructurePage/);
assert.match(navigation, /label: "Bãi xe"[\s\S]*"\/admin\/parking-structure"[\s\S]*"\/admin\/parking-layouts"[\s\S]*"\/admin\/parking-live"/);

for (const tab of ["Tổng quan", "Cấu trúc", "Thiết kế sơ đồ", "Trực tiếp"]) {
  assert.ok(header.includes(tab), `Thiếu tab ${tab}`);
}

for (const integration of ["getParkingLots", "getLevelsByLot", "getLotSpaceSummary", "getZones", "getGates", "getLanes", "activateParkingLot", "requestParkingLotActivation"]) {
  assert.ok(overview.includes(integration), `Trang Tổng quan chưa tích hợp ${integration}`);
}

for (const section of ["Mức độ hoàn thiện cấu hình", "Trạng thái vận hành", "Thông tin bãi xe", "Người phụ trách", "Đi nhanh"]) {
  assert.ok(overview.includes(section), `Thiếu khu vực ${section}`);
}

assert.doesNotMatch(overview, /MOCK_|mockParking|Math\.max\([^)]*,\s*[3568]\)/);
assert.doesNotMatch(overview, /<a\s+href=/);
assert.doesNotMatch(overview, /<select/);
assert.match(header, /SelectMenu/);
assert.match(overview, /parkingLots=\{lots\}/);
assert.match(overview, /onParkingLotChange=\{setSelectedLotId\}/);
assert.match(overview, /Bãi xe chưa đủ điều kiện hoạt động/);
assert.match(overview, /Kích hoạt bãi/);
assert.match(overview, /Yêu cầu kích hoạt/);

console.log("Parking lot overview checks passed.");
