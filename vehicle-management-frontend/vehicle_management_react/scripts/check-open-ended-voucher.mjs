import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/pricing/pages/VoucherManagementPage.tsx", import.meta.url);
const apiPath = new URL("../src/features/pricing/api/voucherApi.ts", import.meta.url);

const [page, api] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(apiPath, "utf8"),
]);

assert.match(api, /validTo: string \| null/);
assert.match(page, /validTo: form\.validTo \? endOfApplicationDayIso\(form\.validTo\) \?\? null : null/);
assert.match(page, /Kết thúc hiệu lực \(không bắt buộc\)/);
assert.match(page, /Để trống = vô thời hạn/);
assert.doesNotMatch(page, /Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc hiệu lực/);

console.log("Voucher expiration is optional and an empty end date is submitted as null.");
