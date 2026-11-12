import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const source = await readFile(
  new URL("../src/features/billing/components/PlatformFeeCreateSection.tsx", import.meta.url),
  "utf8",
);
const pageSource = await readFile(
  new URL("../src/features/billing/pages/PlatformFeePage.tsx", import.meta.url),
  "utf8",
);

assert.match(source, /type CreateStep = 1 \| 2 \| 3/);
assert.match(source, /Thông tin & phạm vi/);
assert.match(source, /Quy tắc áp dụng/);
assert.match(source, /Phiên bản tỷ lệ/);
assert.match(source, /Xác nhận/);
assert.match(source, /step === 1 && !validateStepOne\(\)/);
assert.match(source, /step === 2 && !buildVersionPayload\(\)/);
assert.match(source, /scopeType === "GLOBAL" \? null : transactionType/);
assert.match(source, /await createPlatformFeeVersion\(feeTypeId, versionPayload\)/);
assert.match(source, /disabled=\{!canComplete\}/);
assert.match(source, /hasAnyPermission\(user, platformFeePermissions\.create\)/);
assert.match(source, /hasAnyPermission\(user, platformFeePermissions\.update\)/);
assert.match(source, /<PlatformFeeDateTimeInput/);
assert.match(source, /<SelectMenu/);
assert.match(source, /portal/);
assert.doesNotMatch(source, /<select/);
assert.doesNotMatch(source, /roleCode|SYSTEM_ADMIN/);
assert.match(pageSource, /goDetail\(feeTypeId, "versions"\)/);

console.log("Platform fee three-step creation flow checks passed.");
