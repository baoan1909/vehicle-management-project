import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const projectRoot = fileURLToPath(new URL("..", import.meta.url));

async function loadTsModule(relativePath, stubs = {}) {
  const filename = path.resolve(projectRoot, relativePath);
  const source = await readFile(filename, "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { esModuleInterop: true, module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    fileName: filename,
  });
  const module = { exports: {} };
  const localRequire = (moduleId) => {
    if (Object.prototype.hasOwnProperty.call(stubs, moduleId)) return stubs[moduleId];
    throw new Error(`Unmocked runtime import "${moduleId}" from ${relativePath}`);
  };
  new Function("exports", "require", "module", "__filename", "__dirname", outputText)(
    module.exports, localRequire, module, filename, path.dirname(filename),
  );
  return module.exports;
}

const permissions = await loadTsModule("src/shared/auth/permissions.ts");
const mapper = await loadTsModule("src/features/iam/utils/accountProfileMapper.ts", {
  "@/shared/auth/permissions": permissions,
  "@/shared/utils/accountStatus": { DEFAULT_USER_AVATAR_URL: "default-avatar", getRoleLabel: (role) => role },
  "@/shared/utils/mediaUrl": { resolvePublicMediaUrl: (value) => value ?? "" },
});
const accessState = await loadTsModule("src/core/auth/accessState.ts");
const resolution = await loadTsModule("src/core/auth/accessResolution.ts", {
  "@/core/auth/accessState": accessState,
  "@/features/iam/utils/accountProfileMapper": mapper,
});
const navigationStub = {
  adminNavigation: [{ kind: "link", label: "Dashboard", to: "/admin/dashboard", matches: ["/admin/dashboard"], icon: "dashboard" }],
  platformAdminNavigation: [{ kind: "link", label: "Dashboard", to: "/admin/dashboard", matches: ["/admin/dashboard"], icon: "dashboard" }],
};
const routePermissions = await loadTsModule("src/app/routePermissions.ts", {
  "@/config/navigation": navigationStub,
  "@/shared/auth/permissions": permissions,
});
const landing = await loadTsModule("src/core/auth/landing.ts", { "@/app/routePermissions": routePermissions });
const provisionedAccountPolicy = await loadTsModule("src/features/iam/policies/provisionedAccountPolicy.ts", {
  "@/shared/auth/permissions": permissions,
});
const apiError = await loadTsModule("src/core/api/apiError.ts");

async function readCurrentAccessEnvelope(envelope) {
  const accessApi = await loadTsModule("src/features/iam/api/currentAccountAccessApi.ts", {
    "@/core/api/apiClient": { apiClient: async () => envelope },
    "@/core/api/apiError": apiError,
    "@/core/api/apiEndpoints": { apiEndpoints: { iam: { accountProfile: { currentAccess: "/current-access" } } } },
  });
  return accessApi.getMyAccountAccess();
}

const tokenUser = {
  id: "account-1",
  username: "alice",
  fullName: "Alice Token",
  role: "UNKNOWN",
  avatarUrl: "default-avatar",
  permissionCodes: undefined,
};
const currentAccess = {
  accountId: "account-1",
  accountStatus: "ACTIVE",
  accessRevision: "revision-1",
  accessState: "ACTIVE",
  onboardingRequired: false,
  permissionCodes: ["DASHBOARD_READ_ALL", "SCOPE_ACCESS_READ"],
  requiredAction: null,
  roleCode: "CUSTOM_PLATFORM_OPERATOR",
  username: "alice",
};
const conflictingProfile = {
  account: {
    accountId: "account-1",
    accountStatus: "LOCKED",
    email: "alice.profile@example.com",
    permissionCodes: ["CUSTOMER_ONLY"],
    roleCode: "CUSTOMER",
    username: "alice",
  },
  customer: { customerApprovalStatus: "PENDING", customerStatus: "INACTIVE" },
  employee: { employeeStatus: "INACTIVE", jobTitle: "Nhân viên" },
  onboardingRequired: true,
  profile: { avatarUrl: "/avatar.png", fullName: "Alice Profile", phoneNumber: "0901234567" },
};

// current-access replaces authorization atomically; profile only enriches presentation.
{
  const authorized = mapper.mergeAuthorizationSnapshot(tokenUser, currentAccess);
  const merged = mapper.mergeProfileDetails(authorized, conflictingProfile);
  assert.equal(merged.role, "UNKNOWN");
  assert.equal(merged.roleCode, "CUSTOM_PLATFORM_OPERATOR");
  assert.deepEqual(merged.permissionCodes, ["DASHBOARD_READ_ALL", "ACCESS_READ"]);
  assert.equal(merged.accountStatus, "ACTIVE");
  assert.equal(merged.customerStatus, undefined);
  assert.equal(merged.employeeStatus, undefined);
  assert.equal(merged.onboardingRequired, false);
  assert.equal(merged.accessRevision, "revision-1");
  assert.equal(merged.fullName, "Alice Profile");
}

// The resolver never uses onboarding/profile as an access fallback.
{
  const result = await resolution.resolveAuthoritativeAccess({
    currentUser: tokenUser,
    fetchCurrentAccess: async () => currentAccess,
    fetchProfile: async () => conflictingProfile,
  });
  assert.equal(result.roleCode, "CUSTOM_PLATFORM_OPERATOR");
  assert.deepEqual(result.permissionCodes, ["DASHBOARD_READ_ALL", "ACCESS_READ"]);
  assert.equal(result.accountStatus, "ACTIVE");
  assert.equal(result.fullName, "Alice Profile");
}
for (const status of [404, 405, 501]) {
  let profileFetched = false;
  await assert.rejects(resolution.resolveAuthoritativeAccess({
    currentUser: tokenUser,
    fetchCurrentAccess: async () => { throw Object.assign(new Error("unavailable"), { kind: "http", status }); },
    fetchProfile: async () => { profileFetched = true; return conflictingProfile; },
  }));
  assert.equal(profileFetched, false, `${status} must fail closed before profile is requested`);
}
{
  const result = await resolution.resolveAuthoritativeAccess({
    currentUser: tokenUser,
    fetchCurrentAccess: async () => currentAccess,
    fetchProfile: async () => { throw Object.assign(new Error("offline"), { kind: "network" }); },
  });
  assert.deepEqual(result.permissionCodes, ["DASHBOARD_READ_ALL", "ACCESS_READ"]);
}
await assert.rejects(resolution.resolveAuthoritativeAccess({
  currentUser: tokenUser,
  fetchCurrentAccess: async () => currentAccess,
  fetchProfile: async () => { throw Object.assign(new Error("expired"), { kind: "http", status: 401 }); },
}), /expired/);

// The required current-access contract is validated without token/profile substitution.
{
  const validEnvelope = { success: true, message: "ok", timestamp: "now", data: currentAccess };
  assert.equal((await readCurrentAccessEnvelope(validEnvelope)).data.accessRevision, "revision-1");
  for (const field of ["accountId", "accountStatus", "roleCode", "permissionCodes", "onboardingRequired", "accessState", "accessRevision"]) {
    const invalidData = { ...currentAccess };
    delete invalidData[field];
    await assert.rejects(readCurrentAccessEnvelope({ ...validEnvelope, data: invalidData }), (error) => error.kind === "invalid-response");
  }
}

// Error/status and revision semantics.
assert.equal(accessState.classifyAccessError({ kind: "timeout" }).retryable, true);
assert.equal(accessState.classifyAccessError({ kind: "network" }).type, "offline");
assert.equal(accessState.classifyAccessError({ kind: "http", status: 401 }).retryable, false);
assert.equal(accessState.classifyAccessError({ kind: "http", status: 403 }).type, "forbidden");
assert.equal(accessState.classifyAccessError({ kind: "invalid-response" }).type, "invalid-response");
assert.equal(accessState.shouldReplaceAccessSnapshot("rev-1", "rev-1"), false);
assert.equal(accessState.shouldReplaceAccessSnapshot("rev-1", "rev-2"), true);
for (const status of ["ready", "refreshing", "stale"]) assert.equal(accessState.isResolvedAccessStatus(status), true);
for (const status of ["idle", "loading", "error", "legacy-ready"]) assert.equal(accessState.isResolvedAccessStatus(status), false);

// Role names never grant routes/actions; custom roles work through permissions.
{
  const systemAdminWithoutPermission = { ...tokenUser, role: "SYSTEM_ADMIN", permissionCodes: [] };
  const partnerWithoutPermission = { ...tokenUser, role: "PARTNER_ADMIN", permissionCodes: [] };
  const customWithDashboard = { ...tokenUser, role: "UNKNOWN", permissionCodes: ["DASHBOARD_READ_ALL"] };
  assert.equal(permissions.hasAnyPermission(systemAdminWithoutPermission, ["DASHBOARD_READ_ALL"]), false);
  assert.equal(permissions.hasAnyPermission(partnerWithoutPermission, ["PARKING_SCOPE_PARTNER"]), false);
  assert.equal(routePermissions.canAccessAdminRoute(systemAdminWithoutPermission, "/admin/dashboard"), false);
  assert.equal(routePermissions.canAccessAdminRoute(customWithDashboard, "/admin/dashboard"), true);
  assert.equal(landing.resolvePostLoginPath(customWithDashboard), "/admin/dashboard");
}

// Target-role options follow effective scope permissions, never current role code.
{
  const systemAdminWithoutPermission = { ...tokenUser, role: "SYSTEM_ADMIN", permissionCodes: [] };
  const partnerRoleWithoutScope = { ...tokenUser, role: "PARTNER_ADMIN", permissionCodes: ["ACCOUNT_CREATE_ALL"] };
  const customPlatform = { ...tokenUser, role: "UNKNOWN", permissionCodes: ["ACCOUNT_CREATE_ALL", "PARKING_SCOPE_PLATFORM"] };
  const customPartner = { ...tokenUser, role: "UNKNOWN", permissionCodes: ["ACCOUNT_CREATE_ALL", "PARKING_SCOPE_PARTNER"] };
  const customAssigned = { ...tokenUser, role: "UNKNOWN", permissionCodes: ["ACCOUNT_CREATE_ALL", "PARKING_SCOPE_ASSIGNED"] };
  assert.deepEqual(provisionedAccountPolicy.getManageableProvisionedAccountRoleCodes(systemAdminWithoutPermission), []);
  assert.deepEqual(provisionedAccountPolicy.getManageableProvisionedAccountRoleCodes(partnerRoleWithoutScope), []);
  assert.deepEqual(provisionedAccountPolicy.getManageableProvisionedAccountRoleCodes(customPlatform), ["SYSTEM_ADMIN", "PARTNER_ADMIN"]);
  assert.deepEqual(provisionedAccountPolicy.getManageableProvisionedAccountRoleCodes(customPartner), ["PARKING_MANAGER", "EMPLOYEE"]);
  assert.deepEqual(provisionedAccountPolicy.getManageableProvisionedAccountRoleCodes(customAssigned), ["EMPLOYEE", "CUSTOMER"]);
}

console.log("Current-access sole-source, permission-first, landing, and scope-policy checks passed.");
