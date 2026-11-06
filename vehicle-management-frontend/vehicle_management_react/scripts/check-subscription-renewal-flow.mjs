import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const page = read("../src/features/customer-portal/pages/SubscriptionPage.tsx");
const api = read("../src/features/customer-portal/api/customerPortalApi.ts");

assert.ok(api.includes("/renewal"), "Thiếu API gửi yêu cầu gia hạn vé của khách hàng");
assert.ok(page.includes("Gia hạn vé"), "Menu thao tác chưa có chức năng gia hạn vé");
assert.ok(page.includes("Gửi yêu cầu gia hạn"), "Thiếu form xác nhận kỳ gia hạn");
assert.ok(page.includes("effectiveTo >= todayApplicationIsoDate()"), "Gia hạn chưa chặn vé đã hết hạn");
assert.ok(page.includes("Yêu cầu thẻ vật lý"), "Menu thao tác làm mất chức năng yêu cầu thẻ vật lý");
assert.ok(page.includes("Khóa vé đang sử dụng"), "Menu thao tác làm mất chức năng khóa vé");
assert.ok(page.includes("Kích hoạt lại vé"), "Menu thao tác làm mất chức năng kích hoạt lại vé");
assert.ok(page.includes("Xem chi tiết"), "Menu thao tác làm mất chức năng xem chi tiết");
assert.ok(page.includes("fas fa-qrcode"), "Vé đăng ký chưa có nút QR độc lập");

console.log("Subscription renewal and consolidated customer actions checks passed.");
