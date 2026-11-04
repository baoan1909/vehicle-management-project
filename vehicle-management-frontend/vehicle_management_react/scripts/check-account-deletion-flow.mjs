import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const api = await readFile(new URL("../src/features/iam/api/accountDeletionApi.ts", import.meta.url), "utf8");
const endpoints = await readFile(new URL("../src/core/api/apiEndpoints.ts", import.meta.url), "utf8");
const profile = await readFile(new URL("../src/features/customer-portal/pages/ProfilePage.tsx", import.meta.url), "utf8");

assert.match(endpoints, /deletionRequests:\s*"\/iam\/account-deletion-requests"/);
assert.match(endpoints, /currentDeletionRequest:\s*"\/iam\/account-deletion-requests\/current"/);
assert.match(api, /method:\s*"POST"/);
assert.match(api, /method:\s*"DELETE"/);
assert.match(profile, /ACCOUNT_DELETION_REQUEST_SELF/);
assert.match(profile, /ACCOUNT_DELETION_CANCEL_SELF/);
assert.match(profile, /formatCountdown/);
assert.match(profile, /Xác nhận yêu cầu xóa tài khoản/);
assert.match(profile, /logoutCurrentUser\(setUser\)/);
assert.match(profile, /vm_account_deletion_requested/);
assert.match(profile, /handleCancelDeletion/);
assert.doesNotMatch(api, /role|CUSTOMER/);

console.log("Account deletion permission, confirmation, countdown, cancel and logout guards passed.");
