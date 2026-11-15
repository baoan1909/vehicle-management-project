import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [api, page, data] = await Promise.all([
  readFile(new URL("../src/features/pricing/api/pricingApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/pricing/pages/PricingManagePages.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/pricing/components/pricingManageData.ts", import.meta.url), "utf8"),
]);

assert.match(api, /export type CreatePriceRuleRequest = \{[\s\S]*parkingLotId: string/);
assert.match(api, /export type PriceRuleApiResponse = \{[\s\S]*parkingLotId: string \| null/);
assert.match(page, /getParkingLots\(\)/);
assert.match(page, /getPriceRules\(\{ parkingLotId: selectedParkingLotId \|\| undefined \}\)/);
assert.match(page, /hasAnyPermission\(user, \["PARKING_SCOPE_ASSIGNED"\]\)/);
assert.match(page, /parkingLots\.length === 1/);
assert.match(page, /Vui lòng chọn một bãi xe trước khi tạo quy tắc giá/);
assert.match(page, /parkingLotId: row\?\.parkingLotId \|\| parkingLotId/);
assert.match(page, /onCreateClick=\{canCreate && Boolean\(selectedParkingLotId\)/);
assert.match(data, /parkingLotName: string/);

console.log("Parking-lot scoped price-rule UI and API checks passed.");
