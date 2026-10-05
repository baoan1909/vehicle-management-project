import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const paths = {
  form: new URL("../src/features/customer-portal/pages/PartnerRegistrationPage.tsx", import.meta.url),
  login: new URL("../src/features/auth/pages/LoginPage.tsx", import.meta.url),
  navbar: new URL("../src/shared/components/layout/ClientNavbar.tsx", import.meta.url),
  router: new URL("../src/app/router/index.tsx", import.meta.url),
  routes: new URL("../src/app/routes.tsx", import.meta.url),
  status: new URL("../src/features/customer-portal/pages/PartnerApplicationStatusPage.tsx", import.meta.url),
};

const entries = await Promise.all(
  Object.entries(paths).map(async ([name, path]) => [name, await readFile(path, "utf8")]),
);
const files = Object.fromEntries(entries);

for (const field of [
  "fullName",
  "username",
  "email",
  "password",
  "confirmPassword",
  "organizationCode",
  "organizationName",
  "representativeName",
  "phoneNumber",
]) {
  assert.match(files.form, new RegExp(`\\b${field}\\b`), `Missing Partner form field ${field}`);
}

const submissionBlock = files.form.match(/submitPartnerRegistration\(\{[\s\S]*?\n\s*\}\);/)?.[0] ?? "";
assert.ok(submissionBlock, "Partner submission payload was not found");
assert.doesNotMatch(submissionBlock, /confirmPassword|roleCode|accountStatus/);
assert.match(submissionBlock, /organizationCode: form\.organizationCode\.trim\(\)\.toUpperCase\(\)/);
assert.match(submissionBlock, /email: normalizedEmail/);
assert.match(files.navbar, /to="\/become-a-partner">Trở thành đối tác<\/Link>/);
assert.match(files.routes, /path: "\/partner\/application-status"/);
assert.match(files.login, /partnerApplicationStatus[\s\S]*\/partner\/application-status/);
assert.match(files.router, /user\.partnerApplicationStatus && user\.accountStatus !== "ACTIVE"/);
assert.match(files.status, /Gửi lại email xác thực/);
assert.match(files.status, /reviewNote/);
assert.match(files.status, /subscribeNotificationReceived/);
assert.match(files.status, /relatedSchema === "operations"/);
assert.match(files.status, /relatedTable === "approval_requests"/);
assert.match(files.status, /void loadStatus\(\)/);

assert.ok(!files.form.includes('id="address"') && !files.form.includes("form.address"), "Partner form must not request an address");
assert.ok(!files.form.includes("expectedParkingLotCount"), "Partner form must not request a parking lot count");
assert.ok(!files.form.includes("parkingOperationDescription"), "Partner form must not request an operation description");
assert.ok(!files.form.includes("ClientPage"), "Partner registration must use a standalone layout");
assert.ok(!files.form.includes("PublicFooter"), "Partner registration must not render the public footer");
assert.ok(!files.form.includes(">Trang chủ<"), "Partner registration must not render the home link");
assert.ok(
  files.routes.includes('{ path: "/become-a-partner", title: "Trở thành đối tác CoParking", layout: "auth"'),
  "Partner registration route must use the auth layout",
);
assert.ok(files.form.includes("Gửi lại email"), "Partner form must support resending the verification email");
assert.ok(files.form.includes("acceptedTerms"), "Partner form must require terms acceptance");
assert.ok(files.form.includes("Điều khoản sử dụng") && files.form.includes("Chính sách bảo mật"), "Partner form must display legal consent links");
assert.ok(!files.form.includes("Đăng ký tài khoản Partner"), "Partner-facing copy must use the Vietnamese term đối tác");
assert.ok(!files.form.includes("Thông tin Partner"), "Partner section heading must use the Vietnamese term đối tác");
assert.ok(!files.form.includes("Đăng ký trở thành Partner"), "Partner submit label must use the Vietnamese term đối tác");
assert.ok(
  files.form.includes('toast.success("Tài khoản đối tác đã được tạo. Vui lòng kiểm tra email để xác thực.")'),
  "Successful Partner registration must use the exact toast message",
);
assert.ok(!files.form.includes('AuthInlineNotice tone="success"'), "Successful Partner registration must not render an inline success banner");

console.log("Partner registration form, workflow redirect, and status-page checks passed.");
