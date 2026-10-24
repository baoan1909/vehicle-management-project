import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const paths = {
  accessResolution: new URL("../src/core/auth/accessResolution.ts", import.meta.url),
  accessScreens: new URL("../src/core/auth/AccessStatusScreens.tsx", import.meta.url),
  accessState: new URL("../src/core/auth/accessState.ts", import.meta.url),
  apiClient: new URL("../src/core/api/apiClient.ts", import.meta.url),
  accountCreate: new URL("../src/features/iam/components/AccountCreateDrawer.tsx", import.meta.url),
  authProvider: new URL("../src/core/auth/AuthProvider.tsx", import.meta.url),
  customerProfile: new URL("../src/features/customer-portal/pages/ProfilePage.tsx", import.meta.url),
  internalProfile: new URL("../src/features/iam/pages/InternalProfilePage.tsx", import.meta.url),
  login: new URL("../src/features/auth/pages/LoginPage.tsx", import.meta.url),
  oidcClient: new URL("../src/core/auth/oidcClient.ts", import.meta.url),
  oidcSession: new URL("../src/core/auth/oidcSession.ts", import.meta.url),
  mapper: new URL("../src/features/iam/utils/accountProfileMapper.ts", import.meta.url),
  notificationBell: new URL("../src/features/notifications/components/NotificationBell.tsx", import.meta.url),
  provisionedPolicy: new URL("../src/features/iam/policies/provisionedAccountPolicy.ts", import.meta.url),
  routePermissions: new URL("../src/app/routePermissions.ts", import.meta.url),
  router: new URL("../src/app/router/index.tsx", import.meta.url),
  session: new URL("../src/core/auth/session.ts", import.meta.url),
  supportCenter: new URL("../src/features/support/pages/OperationsSupportCenterPage.tsx", import.meta.url),
};
const entries = await Promise.all(Object.entries(paths).map(async ([name, file]) => [name, await readFile(file, "utf8")]));
const files = Object.fromEntries(entries);

// oidc-client-ts is the sole token/session manager and never loads UserInfo.
assert.match(files.oidcClient, /new UserManager\(oidcSettings\)/);
assert.match(files.oidcClient, /response_type:\s*"code"/);
assert.match(files.oidcClient, /loadUserInfo:\s*false/);
assert.match(files.oidcClient, /disablePKCE:\s*false/);
assert.match(files.oidcClient, /WebStorageStateStore\(\{ store: window\.sessionStorage \}\)/);
assert.doesNotMatch(files.oidcClient, /client_secret|offline_access/);
assert.match(files.oidcSession, /signinRedirectCallback/);
assert.match(files.oidcSession, /signinSilent/);
assert.match(files.oidcSession, /signoutRedirect/);
assert.match(files.oidcSession, /if \(renewPromise\) return renewPromise/);
assert.doesNotMatch(files.oidcSession, /grant_type|code_verifier|code_challenge|\/protocol\/openid-connect\/token/);
assert.doesNotMatch(files.session, /realm_access|resource_access|authorities|resolveAppRole/);
assert.match(files.session, /role:\s*"UNKNOWN"/);
assert.match(files.session, /permissionCodes:\s*undefined/);
assert.match(files.apiClient, /response\.status === 401[\s\S]*?await renewToken\(\)[\s\S]*?sendJsonRequest/);
assert.doesNotMatch(files.apiClient, /response\.status === 403[\s\S]*?renew/);

// Legacy authorization vocabulary and fallback paths must not exist in production.
for (const [name, source] of Object.entries(files)) {
  assert.doesNotMatch(source, /legacy-ready|legacy-onboarding|mergeLegacyOnboardingAccess|resolveAccessWithLegacyFallback|isLegacyAccessUnavailable/,
    `${name} must not contain a legacy authorization fallback`);
}
assert.match(files.accessState, /export type AccessStatus = "idle" \| "loading" \| "ready" \| "refreshing" \| "stale" \| "error"/);
assert.match(files.accessResolution, /const access = await fetchCurrentAccess\(\)[\s\S]*?mergeAuthorizationSnapshot\(currentUser, access\)[\s\S]*?await fetchProfile\(\)/,
  "current-access must resolve before optional profile details");
assert.doesNotMatch(files.accessResolution, /status\s*===\s*(404|405|501)|fetchProfile\(\)[\s\S]*?mergeAuthorizationSnapshot/);

// Profile/onboarding may not overwrite any authorization field.
const profileMerge = files.mapper.match(/export function mergeProfileDetails[\s\S]*?\n\}/)?.[0] ?? "";
assert.ok(profileMerge, "profile-only mapper must exist");
assert.doesNotMatch(profileMerge, /permissionCodes|roleCode|accountStatus|customerStatus|customerApprovalStatus|employeeStatus|onboardingRequired|accessState|requiredAction|accessRevision/);
assert.doesNotMatch(profileMerge, /profile\.account/, "profile account identity must not overwrite current-access identity");
assert.match(files.mapper, /export function mergeAuthorizationSnapshot[\s\S]*?accountStatus: access\.accountStatus[\s\S]*?permissionCodes: normalizePermissionCodes\(access\.permissionCodes\)[\s\S]*?accessRevision: access\.accessRevision/);

// Callback is library-verified, social bootstrap precedes the access gate, and
// AuthProvider calls current-access before optional profile presentation data.
const callbackFlow = files.login.match(/async function completeCallback[\s\S]*?\n    \}/)?.[0] ?? "";
assert.ok(callbackFlow.indexOf("await handleSigninCallback(") < callbackFlow.indexOf("await bootstrapSocialAccount("));
assert.ok(callbackFlow.indexOf("await bootstrapSocialAccount(") < callbackFlow.indexOf("beginAuthenticatedSession("));
assert.doesNotMatch(files.login, /code_verifier|code_challenge|exchangeKeycloakAuthorizationCode|saveAuthTokens/);
assert.match(files.authProvider, /resolveAuthoritativeAccess\([\s\S]*?fetchCurrentAccess:[\s\S]*?fetchProfile:/);
assert.match(files.login, /authenticationStatus === "authenticated"[\s\S]*?accessStatus === "ready"/);

// Reload, refresh, stale handling, and old-user request isolation.
assert.match(files.authProvider, /resolveAuthoritativeAccess\([\s\S]*?fetchCurrentAccess:[\s\S]*?fetchProfile:/);
assert.match(files.authProvider, /accessRequestIdRef\.current !== requestId/);
assert.match(files.authProvider, /isSameUser\(userRef\.current, expectedUserKey\)/);
assert.match(files.authProvider, /shouldReplaceAccessSnapshot\(latestUser\.accessRevision, response\.data\.accessRevision\)/);
assert.match(files.authProvider, /if \(!shouldReplaceAccessSnapshot[\s\S]*?return latestUser/);
assert.match(files.authProvider, /if \(isUnauthorizedAccessError\(error\)\) \{\s*clearSession\(\)/);
assert.match(files.authProvider, /if \(isForbiddenAccessError\(error\)\) \{\s*setAccessStatus\("error"\)/);
assert.match(files.authProvider, /setAccessStatus\(Array\.isArray\(userRef\.current\?\.permissionCodes\) \? "stale" : "error"\)/);
assert.match(files.authProvider, /ACCESS_REFRESH_INTERVAL_MS/);

// Protected content waits for current-access; access errors and empty permission are distinct.
assert.match(files.router, /isResolvedAccessStatus\(accessStatus\)[\s\S]*?<AdminPermissionLoading \/>/);
assert.match(files.router, /accessStatus === "error"[\s\S]*?<AccessErrorGate \/>/);
assert.match(files.router, /canAccessAdminRoute\(user, path\)/);
assert.match(files.router, /canAccessCustomerRoute\(user, path\)/);
assert.match(files.router, /getAccessStateRedirect\(user, path\)/);
assert.match(files.accessScreens, /export function NoAccessScreen/);
assert.match(files.accessScreens, /export function AccessErrorScreen/);

// Static architectural guard: current-actor authorization surfaces cannot branch on role.
const guardedActorAuthorizationFiles = {
  router: files.router,
  accountCreate: files.accountCreate,
  provisionedPolicy: files.provisionedPolicy,
  notificationBell: files.notificationBell,
};
for (const [name, source] of Object.entries(guardedActorAuthorizationFiles)) {
  assert.doesNotMatch(source, /(?:user\??\.role|currentRole)\s*===|switch\s*\([^)]*(?:user\??\.role|currentRole)/,
    `${name} contains current-actor role authorization`);
}
assert.match(files.accountCreate, /getManageableProvisionedAccountRoleCodes\(user\)/);
assert.match(files.provisionedPolicy, /ACCOUNT_CREATE_ALL[\s\S]*?PARKING_SCOPE_PLATFORM[\s\S]*?PARKING_SCOPE_PARTNER[\s\S]*?PARKING_SCOPE_ASSIGNED/);
assert.match(files.notificationBell, /canOpenNotificationTarget\(user, redirectUrl\)/);
assert.doesNotMatch(files.supportCenter, /user\?\.role === "CUSTOMER"/);

// Documented exceptions in routePermissions: profile classification and a tie-break
// between two routes that have already independently passed permission checks.
const allowedRouteRoleChecks = files.routePermissions.match(/user\?\.role === "CUSTOMER"/g) ?? [];
assert.equal(allowedRouteRoleChecks.length, 2);
assert.match(files.routePermissions, /Role may classify which safe profile surface/);
assert.match(files.routePermissions, /Role is only a[\s\S]*?tie-breaker between two already-authorized portals/);

// Access-changing self-service mutations must immediately refresh current-access.
assert.match(files.internalProfile, /completeMyAccountProfile[\s\S]*?invalidateAndRefreshAccess\(shouldCompleteOnboarding \? "profile-completed" : "profile-updated"\)/);
assert.match(files.internalProfile, /resubmitMyOnboardingApproval[\s\S]*?invalidateAndRefreshAccess\("onboarding-resubmitted"\)/);
assert.match(files.customerProfile, /completeMyAccountProfile[\s\S]*?invalidateAndRefreshAccess\(shouldCompleteOnboarding \? "profile-completed" : "profile-updated"\)/);
assert.match(files.customerProfile, /resubmitMyOnboardingApproval[\s\S]*?invalidateAndRefreshAccess\("onboarding-resubmitted"\)/);

console.log("Auth lifecycle, sole-source, mutation refresh, and actor-role static guards passed.");
