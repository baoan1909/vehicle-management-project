import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/pricing/pages/VoucherManagementPage.tsx", import.meta.url);
const apiPath = new URL("../src/features/pricing/api/voucherApi.ts", import.meta.url);
const customerApiPath = new URL("../src/features/customer-portal/api/customerPortalApi.ts", import.meta.url);

const [page, api, customerApi] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(apiPath, "utf8"),
  readFile(customerApiPath, "utf8"),
]);

assert.match(api, /VoucherScopeType = "PLATFORM" \| "PARTNER" \| "PARKING_LOT"/);
assert.match(page, /PARKING_SCOPE_PLATFORM/);
assert.match(page, /PARKING_SCOPE_PARTNER/);
assert.match(page, /PARKING_SCOPE_ASSIGNED/);
assert.match(page, /!isParkingManager \? <button/);
assert.match(page, /<option value="__ALL__">Tất cả<\/option>/);
assert.match(page, /scopeType: value === "__ALL__"/);
assert.match(customerApi, /parkingLotId=\$\{encodeURIComponent\(parkingLotId\)\}/);

console.log("Three-level voucher scope is represented in the admin UI and customer promotion query.");
