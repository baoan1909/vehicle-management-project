import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const page = await readFile(new URL("../src/features/dashboard/pages/DashboardPage.tsx", import.meta.url), "utf8");
const api = await readFile(new URL("../src/features/dashboard/api/dashboardApi.ts", import.meta.url), "utf8");
const navigation = await readFile(new URL("../src/config/navigation.ts", import.meta.url), "utf8");
const sidebar = await readFile(new URL("../src/shared/components/layout/AdminSidebar.tsx", import.meta.url), "utf8");

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
assert.match(page, /tw-grid-cols-3 tw-gap-4 max-\[1080px\]:tw-grid-cols-2/,
  "KPI dashboard chưa được giới hạn còn ba thông số mỗi hàng");
assert.doesNotMatch(page, /label: "Xe đang gửi"/,
  "Dashboard vẫn còn KPI xe đang gửi");
assert.doesNotMatch(page, /label: "Tỷ lệ lấp đầy"/,
  "Dashboard vẫn còn KPI tỷ lệ lấp đầy");
assert.doesNotMatch(page, /label: "Thiết bị offline"/,
  "Dashboard vẫn còn KPI thiết bị offline");
assert.doesNotMatch(page, />Thiết bị offline<\/th>/,
  "Bảng tình hình theo bãi vẫn còn cột thiết bị offline");
assert.match(page, /tw-w-\[180px\] max-\[560px\]:tw-w-full/,
  "Bộ lọc nhóm doanh thu chưa được thu gọn");

assert.match(page, /label: "Khách đăng ký mới"/,
  "Dashboard Partner và Manager chưa hiển thị khách đăng ký mới");
assert.match(page,
  /\? \[checkOutCard, platformFeeCard\]\s*: \[newCustomerCard, platformFeeCard, checkOutCard\]/,
  "Check-in/Check-out hoặc phí nền tảng chưa được xếp đúng cột KPI");

assert.doesNotMatch(sidebar, /PlatformMonitoringScopePicker/,
  "Sidebar System Admin vẫn còn bộ chọn phạm vi giám sát cố định");
for (const redundantLabel of [
  "Lượt xe, mức lấp đầy",
  "Vé đăng ký",
  "Giám sát hệ thống",
  "Giá gửi xe của bãi (chỉ xem)",
]) {
  const platformNavigation = navigation.slice(navigation.indexOf("export const platformAdminNavigation"));
  assert.doesNotMatch(platformNavigation, new RegExp(`label: "${redundantLabel.replace(/[.*+?^${}()|[\\]\\]/g, "\\$&")}"`),
    `Sidebar System Admin vẫn còn mục chi tiết không cần thiết: ${redundantLabel}`);
}

console.log("Dashboard platform, partner and assigned parking-lot scope checks passed.");
