import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const qrApi = read("../src/features/subscriptions/api/subscriptionQrApi.ts");
const customerPage = read("../src/features/customer-portal/pages/SubscriptionPage.tsx");
const operationPage = read("../src/features/parking/pages/SwipeListPage.tsx");
const scannerModal = read("../src/features/parking/components/SubscriptionQrScannerModal.tsx");

assert.ok(qrApi.includes("/me/${subscriptionId}/qr"), "Thiếu API cấp QR cho chính khách hàng");
assert.ok(qrApi.includes("/qr/verify"), "Thiếu API kiểm tra QR tại bãi xe");
assert.ok(qrApi.includes("/qr/verify-image"), "Thiếu API tải ảnh QR lên backend để giải mã đa trình duyệt");
assert.ok(customerPage.includes('detailSubscription?.status === "ACTIVE"'), "QR khách hàng chưa bị giới hạn theo vé ACTIVE");
assert.ok(!customerPage.includes("detailSubscription.physicalCardRequested === false"), "QR vẫn đang bị giới hạn cho vé không dùng thẻ vật lý");
assert.ok(customerPage.includes("Tải ảnh QR"), "Khách hàng chưa thể tải ảnh QR để thử trên cùng thiết bị");
assert.ok(customerPage.includes("Sao chép nội dung QR"), "Thiếu phương án kiểm tra thủ công khi trình duyệt không đọc được ảnh QR");
assert.ok(operationPage.includes("SubscriptionQrScannerModal"), "Màn hình vận hành chưa kết nối bộ đọc QR");
assert.ok(scannerModal.includes("verifySubscriptionQrImage"), "Bộ đọc QR chưa hỗ trợ tải ảnh lên backend");
assert.ok(scannerModal.includes("licensePlate.trim() || undefined"), "Bộ đọc QR chưa đối chiếu biển số quan sát");
assert.ok(scannerModal.includes("chưa tự động check-in/check-out"), "Giao diện chưa mô tả đúng giới hạn giai đoạn QR hiện tại");

console.log("Subscription QR issuance, download, image scan, and scoped verification checks passed.");
