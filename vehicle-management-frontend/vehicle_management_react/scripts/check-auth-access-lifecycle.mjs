import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const paths = {
  authProvider: new URL("../src/core/auth/AuthProvider.tsx", import.meta.url),
  login: new URL("../src/features/auth/pages/LoginPage.tsx", import.meta.url),
  mapper: new URL("../src/features/iam/utils/accountProfileMapper.ts", import.meta.url),
  permissions: new URL("../src/shared/auth/permissions.ts", import.meta.url),
  routePermissions: new URL("../src/app/routePermissions.ts", import.meta.url),
  router: new URL("../src/app/router/index.tsx", import.meta.url),
  session: new URL("../src/core/auth/session.ts", import.meta.url),
};

const entries = await Promise.all(
  Object.entries(paths).map(async ([name, path]) => [name, await readFile(path, "utf8")]),
);
const files = Object.fromEntries(entries);

const unresolvedAssignments = files.session.match(/permissionCodes:\s*undefined/g) ?? [];
assert.ok(unresolvedAssignments.length >= 2, "Token restore must leave permissionCodes unresolved");

assert.match(files.permissions, /Array\.isArray\(user\?\.permissionCodes\)/,
  "Permission resolution must distinguish undefined from an empty resolved list");
assert.match(files.router, /!hasResolvedPermissions\(user\)\s*&&\s*isAccessLoading[\s\S]*?<AdminPermissionLoading\s*\/>/,
  "Protected routes must render a permission loader while access is unresolved");
assert.match(files.router, /canAccessAdminRoute\(user, path\)/,
  "Resolved admin route access must use the current user's permissions");
assert.match(files.routePermissions, /return hasAnyPermission\(user, getRoutePermissions\(path\)\)/,
  "Route decisions must preserve hasAnyPermission semantics");

const googleFlow = files.login.match(/if \(getIdentityProviderFromAccessToken\(accessToken\) === "google"\) \{[\s\S]*?\n\s*\}/)?.[0] ?? "";
assert.ok(googleFlow, "Google login flow was not found");
assert.ok(
  googleFlow.indexOf("await bootstrapSocialAccount()") < googleFlow.indexOf("await getMyAccountProfile()"),
  "Google social bootstrap must run before profile/access hydration",
);
assert.match(files.login, /user\.onboardingRequired\s*\?\s*"\/customer\/profile"/,
  "Customer onboarding must redirect to /customer/profile");
assert.match(files.login, /PARTNER_ADMIN[\s\S]*accountStatus\s*!==\s*"ACTIVE"[\s\S]*"\/admin\/profile"/,
  "Pending Partner Admin must redirect to /admin/profile");
assert.match(files.routePermissions, /const adminFallbackRoute = "\/admin\/profile"/,
  "Unauthorized admin routes must preserve the current fallback");

assert.match(files.authProvider, /accessRequestIdRef\.current !== requestId/,
  "Stale access responses must be rejected by request identity");
assert.match(files.authProvider, /if \(!active[\s\S]*?return;/,
  "Cancelled access requests must not update auth state");
assert.match(files.authProvider, /buildUserKey\(currentUser\) !== expectedUserKey/,
  "An old user's failed access request must not clear a replacement user's permissions");
assert.match(files.mapper, /permissionCodes:\s*normalizePermissionCodes\(access\.permissionCodes \?\? \[\]\)/,
  "Current user permission state must be hydrated from current-access");

console.log("Auth access lifecycle characterization checks passed.");
