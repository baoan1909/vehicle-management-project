import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const apiPath = new URL("../src/features/parking/api/customerReservationApi.ts", import.meta.url);
const pagePath = new URL("../src/features/parking/pages/CustomerReservationPage.tsx", import.meta.url);
const prepaymentApiPath = new URL("../src/features/parking/api/parkingPrepaymentApi.ts", import.meta.url);
const prepaymentPanelPath = new URL("../src/features/parking/components/ParkingPrepaymentPanel.tsx", import.meta.url);
const [api, page, prepaymentApi, prepaymentPanel] = await Promise.all([
  readFile(apiPath, "utf8"),
  readFile(pagePath, "utf8"),
  readFile(prepaymentApiPath, "utf8"),
  readFile(prepaymentPanelPath, "utf8"),
]);

assert.match(api, /ReservationPaymentMethod = "VNPAY" \| "SEPAY"/);
assert.match(api, /reservation\.prepaidAmount < VNPAY_MINIMUM_AMOUNT/);
assert.match(api, /`\$\{path\}\/sepay`/);
assert.match(api, /paymentMethod: "SEPAY"/);
assert.match(page, /Mã VietQR thanh toán đặt chỗ qua SePay/);
assert.match(page, /Vui lòng giữ nguyên số tiền và nội dung chuyển khoản/);
assert.match(page, /Khoản dưới 10\.000đ thanh toán bằng SePay/);
assert.doesNotMatch(page, /Thanh toán đặt chỗ dưới 10\.000đ đang tạm chưa hỗ trợ/);
assert.match(prepaymentApi, /\/qr`, \{[\s\S]*?body: \{ licensePlate, cardNumber \}/);
assert.match(prepaymentPanel, /!paid && !expired[\s\S]*?Tải ảnh QR/);
assert.match(prepaymentPanel, /link\.download = `sepay-\$\{checkout\.payment\.transactionRef\}\.png`/);

console.log("SePay reservation routing, VietQR display, and prepayment QR download safeguards passed.");
