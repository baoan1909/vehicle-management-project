import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const page = read("../src/features/customer-portal/pages/SubscriptionPage.tsx");
const api = read("../src/features/customer-portal/api/customerPortalApi.ts");

assert.ok(api.includes("/renewal"), "Thiếu API gửi yêu cầu gia hạn vé của khách hàng");
assert.ok(api.includes("/renewal/quote"), "Thiếu API báo giá gia hạn vé");
assert.ok(api.includes("physicalCardRequested"), "Gia hạn chưa xử lý lựa chọn thẻ vật lý");
assert.ok(page.includes("Gia hạn vé"), "Menu thao tác chưa có chức năng gia hạn vé");
assert.ok(page.includes("Gia hạn & thanh toán"), "Thiếu bước tạo hóa đơn thanh toán gia hạn");
assert.ok(page.includes("QR, thẻ vật lý và vị trí đã giữ không bị tạo lại"), "Form chưa giải thích nguyên tắc giữ nguyên vé");
assert.ok(page.includes("Thẻ vật lý hiện tại sẽ tiếp tục được dùng"), "Form chưa ngăn thu cọc thẻ lần hai");
assert.ok(page.includes("Yêu cầu thêm thẻ vật lý"), "Form chưa hỗ trợ bổ sung thẻ khi gia hạn");
assert.ok(api.includes("SUBSCRIPTION_RENEWAL"), "Frontend chưa nhận diện hóa đơn gia hạn");
assert.ok(page.includes("effectiveTo >= todayApplicationIsoDate()"), "Gia hạn chưa chặn vé đã hết hạn");
assert.ok(page.includes("Yêu cầu thẻ vật lý"), "Menu thao tác làm mất chức năng yêu cầu thẻ vật lý");
assert.ok(page.includes("Khóa vé đang sử dụng"), "Menu thao tác làm mất chức năng khóa vé");
assert.ok(page.includes("Kích hoạt lại vé"), "Menu thao tác làm mất chức năng kích hoạt lại vé");
assert.ok(!page.includes("fa-info-circle") || !page.includes("/>Xem chi tiết</button>"), "Menu thao tác vẫn còn chức năng xem chi tiết");
assert.ok(page.includes('"Áp dụng"'), "Nút áp dụng báo giá gia hạn chưa hiển thị đúng");
assert.ok(page.includes("fas fa-qrcode"), "Vé đăng ký chưa có nút QR độc lập");

console.log("Subscription renewal and consolidated customer actions checks passed.");
