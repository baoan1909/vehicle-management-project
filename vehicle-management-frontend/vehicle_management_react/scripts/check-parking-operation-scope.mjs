import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const operationPage = read("../src/features/parking/pages/SwipeListPage.tsx");
const parkingSessionPage = read("../src/features/parking/pages/ParkingSessionPage.tsx");
const operationApi = read("../src/features/parking/api/parkingSessionApi.ts");
const lostCardListPage = read("../src/features/cards/pages/LostCardListPage.tsx");

assert.match(operationPage, /getParkingLots\(\{ status: "ACTIVE" \}\)/);
assert.match(operationPage, /ariaLabel="Chọn bãi xe vận hành"/);
assert.match(operationPage, /fetchParkingLanes\(laneDirection, parkingLotId\)/);
assert.match(operationPage, /fetchParkingCards\(status, parkingLotId\)/);
assert.match(operationPage, /getDevices\(\{ deviceType: "CAMERA", parkingLotId \}\)/);

assert.match(operationApi, /if \(parkingLotId\) query\.set\("parkingLotId", parkingLotId\)/);
assert.ok(parkingSessionPage.includes('"PARKING_SCOPE_ASSIGNED"'), "Trang phiên gửi xe chưa nhận phạm vi bãi được phân công");
assert.ok(parkingSessionPage.includes('assignedLotIds.map((parkingLotId) => fetchParkingZones("ACTIVE", parkingLotId))'), "Trang phiên gửi xe chưa tải khu vực theo từng bãi được phân công");
assert.ok(parkingSessionPage.includes('Tất cả bãi được phân công'), "Bộ lọc phiên gửi xe chưa mô tả đúng phạm vi Manager");
assert.match(operationApi, /fetchParkingZones\(status\?: ZoneStatus, parkingLotId\?: string\)/);

assert.ok(lostCardListPage.includes('"PARKING_SCOPE_ASSIGNED"'), "Danh sách mất thẻ chưa hỗ trợ phạm vi bãi được phân công");
assert.ok(lostCardListPage.includes('"PARKING_SCOPE_PARTNER"'), "Danh sách mất thẻ thiếu phạm vi bãi của Partner");
assert.ok(lostCardListPage.includes('"PARKING_SCOPE_PLATFORM"'), "Danh sách mất thẻ thiếu phạm vi toàn sàn");
assert.ok(lostCardListPage.includes('nextParkingLots.length === 1'), "Danh sách mất thẻ chưa tự chọn bãi duy nhất của Manager");
assert.ok(lostCardListPage.includes('Tất cả bãi được phân công'), "Danh sách mất thẻ chưa mô tả đúng phạm vi của Manager");
assert.match(lostCardListPage, /parkingLotId: parkingLotValue !== "all" \? parkingLotValue : undefined/);
assert.match(lostCardListPage, /getLostCardReports\(filter\)/);
assert.match(lostCardListPage, /getLostCardReportSummary\(\{[\s\S]*parkingLotId: filter\.parkingLotId/);

console.log("Parking operation and lost-card resources are scoped to the selected parking lot.");
