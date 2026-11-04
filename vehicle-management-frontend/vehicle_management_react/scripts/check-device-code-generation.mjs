import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/hardware/pages/DeviceManagementPage.tsx", import.meta.url);
const apiPath = new URL("../src/features/hardware/api/deviceApi.ts", import.meta.url);

const [page, api] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(apiPath, "utf8"),
]);

assert.match(page, /CAM-, KIO-, CRD- hoặc BAR-/);
assert.doesNotMatch(page, /form\.deviceCode/);
assert.doesNotMatch(page, /CAM_GATE_IN_01/);
assert.doesNotMatch(api, /SaveDeviceRequest = \{[\s\S]*?deviceCode:/);

console.log("Device codes are backend-generated and immutable in the editor.");
