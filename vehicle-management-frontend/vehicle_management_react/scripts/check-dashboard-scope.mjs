import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/dashboard/pages/DashboardPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/dashboard/api/dashboardApi.ts", import.meta.url), "utf8");

assert.match(api, /organizationId\?: string/,
  "Dashboard API chưa hỗ trợ chọn phạm vi Partner");
assert.match(api, /parkingLotId\?: string/,
  "Dashboard API chưa hỗ trợ chọn phạm vi bãi xe");
assert.match(page, /PARKING_SCOPE_PLATFORM/,
  "Dashboard chưa phân biệt quyền chọn phạm vi toàn sàn");
assert.match(page, /Tất cả bãi được phân công/,
  "Dashboard chưa hỗ trợ Manager được phân công nhiều bãi");
assert.match(page, /overview\.scope\.level === "PLATFORM"/,
  "Dữ liệu tăng trưởng toàn sàn chưa được giới hạn ở phạm vi nền tảng");
assert.match(page, /Tình hình theo bãi xe/,
  "Dashboard chưa có bảng tổng hợp tình hình theo bãi");
assert.match(page, /setSearchParams\(nextParams, \{ replace: true \}\)/,
  "Phạm vi dashboard chưa được lưu trên URL");

console.log("Dashboard platform, partner and assigned parking-lot scope checks passed.");
