import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const operationPage = read("../src/features/parking/pages/SwipeListPage.tsx");
const operationApi = read("../src/features/parking/api/parkingSessionApi.ts");

assert.match(operationPage, /getParkingLots\(\{ status: "ACTIVE" \}\)/);
assert.match(operationPage, /ariaLabel="Chọn bãi xe vận hành"/);
assert.match(operationPage, /fetchParkingLanes\(laneDirection, parkingLotId\)/);
assert.match(operationPage, /fetchParkingCards\(status, parkingLotId\)/);
assert.match(operationPage, /getDevices\(\{ deviceType: "CAMERA", parkingLotId \}\)/);

assert.match(operationApi, /if \(parkingLotId\) query\.set\("parkingLotId", parkingLotId\)/);

console.log("Parking operation resources are scoped to the selected parking lot.");
