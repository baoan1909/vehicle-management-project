import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const qrApi = read("../src/features/subscriptions/api/subscriptionQrApi.ts");
const approvalApi = read("../src/features/catalog/api/subscriptionApprovalsApi.ts");
const approvalPage = read("../src/features/catalog/pages/SubscriptionApprovalPage.tsx");
const customerPage = read("../src/features/customer-portal/pages/SubscriptionPage.tsx");
const operationPage = read("../src/features/parking/pages/SwipeListPage.tsx");
const scannerModal = read("../src/features/parking/components/SubscriptionQrScannerModal.tsx");

assert.ok(qrApi.includes("/me/${subscriptionId}/qr"), "Thiếu API cấp QR cho chính khách hàng");
assert.ok(qrApi.includes("/me/${subscriptionId}/qr/refresh"), "Thiếu API làm mới QR của khách hàng");
assert.ok(qrApi.includes("/me/${subscriptionId}/access/block"), "Thiếu API tạm khóa quyền sử dụng vé");
assert.ok(qrApi.includes("/me/${subscriptionId}/access/unblock"), "Thiếu API kích hoạt lại quyền sử dụng vé");
assert.ok(qrApi.includes("/qr/verify"), "Thiếu API kiểm tra QR tại bãi xe");
assert.ok(qrApi.includes("/qr/verify-image"), "Thiếu API tải ảnh QR lên backend để giải mã đa trình duyệt");
assert.ok(customerPage.includes('detailSubscription?.status === "ACTIVE"'), "QR khách hàng chưa bị giới hạn theo vé ACTIVE");
assert.ok(!customerPage.includes("detailSubscription.physicalCardRequested === false"), "QR vẫn đang bị giới hạn cho vé không dùng thẻ vật lý");
assert.ok(customerPage.includes("Tải ảnh QR"), "Khách hàng chưa thể tải ảnh QR để thử trên cùng thiết bị");
assert.ok(customerPage.includes("Làm mới QR"), "Khách hàng chưa thể thu hồi QR cũ và cấp QR mới");
assert.ok(customerPage.includes("Khóa vé đang sử dụng"), "Khách hàng chưa thể tạm khóa QR và thẻ vật lý");
assert.ok(customerPage.includes("Kích hoạt lại vé"), "Khách hàng chưa thể kích hoạt lại vé sau khi khóa");
assert.ok(approvalApi.includes("/${subscriptionId}/access/block"), "Thiếu API cho quản lý tạm khóa vé");
assert.ok(approvalApi.includes("/${subscriptionId}/access/unblock"), "Thiếu API cho quản lý kích hoạt lại vé");
assert.ok(approvalPage.includes('hasAnyPermission(user, ["SUBSCRIPTION_UPDATE_ALL"])'), "Thao tác quản lý vé chưa kiểm tra quyền cập nhật");
assert.ok(approvalPage.includes('hasAnyPermission(user, ["PARKING_SCOPE_PARTNER", "PARKING_SCOPE_ASSIGNED"])'), "Thao tác quản lý vé chưa giới hạn theo phạm vi bãi");
assert.ok(approvalPage.includes("Kích hoạt lại vé"), "Quản lý chưa thể kích hoạt lại vé sau khi khóa");
assert.ok(operationPage.includes("SubscriptionQrScannerModal"), "Màn hình vận hành chưa kết nối bộ đọc QR");
assert.ok(operationPage.includes("subscriptionQrPayload"), "Màn hình vận hành chưa gửi QR vào yêu cầu check-in");
assert.ok(operationPage.includes("handleConfirmSubscriptionQr"), "Màn hình vận hành chưa có bước nhân viên xác nhận QR");
assert.ok(scannerModal.includes("verifySubscriptionQrImage"), "Bộ đọc QR chưa hỗ trợ tải ảnh lên backend");
assert.ok(scannerModal.includes('BLOCKED: "Vé đang bị tạm khóa"'), "Bộ đọc QR chưa hiển thị trạng thái vé bị khóa");
assert.ok(!scannerModal.includes("licensePlate.trim() || undefined"), "Bộ đọc QR vẫn đối chiếu biển số quá sớm trước bước check-in");
assert.ok(scannerModal.includes('verification.status === "VALID"'), "QR hợp lệ chưa tự động quay lại form check-in");
assert.ok(!scannerModal.includes("Dùng QR để check-in"), "Hộp thoại vẫn còn nút xác nhận QR trung gian không cần thiết");

console.log("Subscription QR issuance, download, image scan, confirmation, and check-in wiring checks passed.");
