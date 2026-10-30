import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { classifyAccountStateInvalidation } from "../src/features/notifications/utils/accountStateInvalidation.ts";
import { AccountStateSyncCoordinator } from "../src/features/notifications/utils/accountStateSyncCoordinator.ts";

function notification(notificationType, overrides = {}) {
  return {
    notificationId: crypto.randomUUID(),
    notificationType,
    redirectUrl: null,
    relatedSchema: null,
    relatedTable: null,
    ...overrides,
  };
}

for (const notificationType of [
  "ACCOUNT_ACCESS_CHANGED",
  "ACCOUNT_STATUS_CHANGED",
  "CUSTOMER_ONBOARDING_APPROVED",
  "CUSTOMER_ONBOARDING_REJECTED",
  "CUSTOMER_ONBOARDING_RESUBMITTED",
  "INTERNAL_EMPLOYEE_APPROVED",
  "INTERNAL_EMPLOYEE_REJECTED",
  "INTERNAL_EMPLOYEE_RESUBMITTED",
  "SYSTEM_ADMIN_APPROVED",
  "SYSTEM_ADMIN_REJECTED",
  "SYSTEM_ADMIN_RESUBMITTED",
]) {
  const invalidation = classifyAccountStateInvalidation(notification(notificationType));
  assert.equal(invalidation.refreshAccess, true, `${notificationType} must refresh authoritative access`);
  assert.equal(invalidation.refreshProfile, true, `${notificationType} must refresh profile state`);
  assert.equal(invalidation.refreshApproval, true, `${notificationType} must refresh approval state`);
}
assert.equal(classifyAccountStateInvalidation(notification("ACCOUNT_ACCESS_CHANGED")).refreshOrganizationScope, true);
for (const notificationType of ["AVATAR_APPROVED", "AVATAR_REJECTED"]) {
  const invalidation = classifyAccountStateInvalidation(notification(notificationType));
  assert.equal(invalidation.refreshAvatar, true, `${notificationType} must refresh avatar state`);
  assert.equal(invalidation.refreshProfile, true, `${notificationType} must refresh profile state`);
  assert.equal(invalidation.refreshAccess, false, `${notificationType} must not trigger an access refresh`);
}
assert.equal(classifyAccountStateInvalidation(notification("PAYMENT_SUCCEEDED")).refreshAccess, false);
const partnerReviewInvalidation = classifyAccountStateInvalidation(notification("SYSTEM_NOTICE", {
  redirectUrl: "/admin/profile",
  relatedSchema: "operations",
  relatedTable: "approval_requests",
}));
assert.equal(partnerReviewInvalidation.refreshAccess, true);
assert.equal(partnerReviewInvalidation.refreshOrganizationScope, true);
assert.equal(classifyAccountStateInvalidation(notification("SYSTEM_NOTICE", {
  redirectUrl: "/admin/support",
  relatedSchema: "operations",
  relatedTable: "approval_requests",
})).refreshAccess, false, "unrelated system notices must not refresh access");

let releaseFirst;
const firstGate = new Promise((resolve) => { releaseFirst = resolve; });
const calls = [];
const coordinator = new AccountStateSyncCoordinator(async (work) => {
  calls.push(work);
  if (calls.length === 1) await firstGate;
});
const first = coordinator.request({
  refreshAccess: true,
  refreshApproval: false,
  refreshAvatar: false,
  refreshOrganizationScope: false,
  refreshProfile: false,
});
const second = coordinator.request({
  refreshAccess: false,
  refreshApproval: true,
  refreshAvatar: true,
  refreshOrganizationScope: false,
  refreshProfile: true,
});
assert.equal(calls.length, 1, "in-flight invalidation must not start a parallel request");
releaseFirst();
await Promise.all([first, second]);
assert.equal(calls.length, 2, "an invalidation received in flight must trigger exactly one queued rerun");
assert.equal(calls[1].refreshApproval, true);
assert.equal(calls[1].refreshAvatar, true);
assert.equal(calls[1].refreshProfile, true);

const bell = await readFile(new URL("../src/features/notifications/components/NotificationBell.tsx", import.meta.url), "utf8");
const provider = await readFile(new URL("../src/features/notifications/context/AccountStateSyncProvider.tsx", import.meta.url), "utf8");
const customerProfile = await readFile(new URL("../src/features/customer-portal/pages/ProfilePage.tsx", import.meta.url), "utf8");
const internalProfile = await readFile(new URL("../src/features/iam/pages/InternalProfilePage.tsx", import.meta.url), "utf8");

assert.doesNotMatch(bell, /subscribeNotificationRealtime|invalidateAndRefreshAccess|subscribeNotificationReceived/);
assert.match(provider, /subscribeNotificationRealtime/);
assert.match(provider, /user\?\.id/);
assert.match(provider, /AccountStateSyncCoordinator/);
assert.match(provider, /visibilitychange/);
assert.match(provider, /reloadNotifications/);
assert.match(provider, /const reconcileAfterConnection = \(reconnected: boolean\)/);
assert.match(provider, /if \(!active \|\| !reconnected\) return;/);
assert.doesNotMatch(provider, /refreshProfile: true/,
  "transport reconnect and foreground reconciliation must not invalidate the whole profile");
assert.doesNotMatch(customerProfile, /subscribeNotificationReceived/);
assert.doesNotMatch(internalProfile, /subscribeNotificationReceived/);
assert.match(customerProfile, /profileInvalidationVersion/);
assert.match(customerProfile, /const showInitialLoading = profile === null/);
assert.match(customerProfile, /if \(showInitialLoading\) setLoading\(true\)/);
assert.match(customerProfile, /if \(!ignore && showInitialLoading\) setLoading\(false\)/);
assert.match(internalProfile, /profileInvalidationVersion/);

console.log("Account-state invalidation policy, single-flight queue, and provider ownership checks passed.");
