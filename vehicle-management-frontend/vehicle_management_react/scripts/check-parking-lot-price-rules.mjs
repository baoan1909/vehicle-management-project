import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [api, page, data] = await Promise.all([
  readFile(new URL("../src/features/pricing/api/pricingApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/pricing/pages/PricingManagePages.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/pricing/components/pricingManageData.ts", import.meta.url), "utf8"),
]);

assert.match(api, /export type CreatePriceRuleRequest = \{[\s\S]*parkingLotId: string/);
assert.match(api, /export type PriceRuleApiResponse = \{[\s\S]*parkingLotId: string \| null/);
assert.match(api, /initializePriceRuleDrafts\(parkingLotId: string\)/);
assert.match(api, /parking-lots\/\$\{parkingLotId\}\/initialize-drafts/);
assert.match(page, /getParkingLots\(\)/);
assert.match(page, /getPriceRules\(\{ parkingLotId: selectedParkingLotId \|\| undefined \}\)/);
assert.match(page, /hasAnyPermission\(user, \["PARKING_SCOPE_ASSIGNED"\]\)/);
assert.match(page, /parkingLots\.length === 1/);
assert.match(page, /Vui lòng chọn một bãi xe trước khi tạo quy tắc giá/);
assert.match(page, /if \(!selectedParkingLotId\) \{/);
assert.match(page, /PRICING\.RULE\.PARKING_LOT/);
assert.match(page, /PRICING\.RULE\.INITIALIZE_DRAFTS/);
assert.match(page, /Khởi tạo bảng giá nháp từ mẫu/);
assert.match(page, /initializePriceRuleDrafts\(selectedParkingLotId\)/);
assert.match(page, /searchParams\.get\("parkingLotId"\)/);
assert.match(page, /parkingLotId: row\?\.parkingLotId \|\| parkingLotId/);
assert.match(page, /onCreateClick=\{canCreate && Boolean\(selectedParkingLotId\)/);
assert.match(data, /parkingLotName: string/);

console.log("Parking-lot scoped price-rule UI and API checks passed.");
