import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pickerPath = new URL("../src/features/parking/components/ParkingAddressPicker.tsx", import.meta.url);
const pagePath = new URL("../src/features/parking/pages/ParkingOperationsPage.tsx", import.meta.url);
const dataPath = new URL("../src/shared/data/vietnamAddress.ts", import.meta.url);
const endpointsPath = new URL("../src/core/api/apiEndpoints.ts", import.meta.url);
const [picker, page, data, endpoints] = await Promise.all([
  readFile(pickerPath, "utf8"),
  readFile(pagePath, "utf8"),
  readFile(dataPath, "utf8"),
  readFile(endpointsPath, "utf8"),
]);

assert.match(page, /key=\{form\.id \|\| "new-parking-lot"\}/);
assert.match(picker, /loadLegacyWardPath\(selectedWardCode\)/);
assert.match(picker, /loadCurrentWardPath\(selectedWardCode\)/);
assert.match(picker, /setProvinceCode\(path\.province\.code\)/);
assert.match(picker, /setDistrictCode\(path\.district\?\.code \?\? ""\)/);
assert.match(picker, /setWardCode\(path\.ward\.code\)/);
assert.match(picker, /setDetail\(extractAddressDetail\(value\.addressDisplay, path\)\)/);
assert.match(picker, /commit\(detail, "", code, ""\)/);
assert.match(picker, /commit\(detail, "", provinceCode, code\)/);
assert.match(data, /currentWardPaths/);
assert.match(data, /legacyWardPaths/);
assert.match(endpoints, /current\/wards\/\$\{wardCode\}\/path/);
assert.match(endpoints, /legacy\/wards\/\$\{wardCode\}\/path/);

console.log("Parking address edit hydration checks passed.");