import assert from "node:assert/strict";
import { readFile, readdir, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("..", import.meta.url));
const read = (relativePath) => readFile(path.join(root, relativePath), "utf8");
const [
  packageJson,
  oidcClient,
  oidcSession,
  apiClient,
  authProvider,
  login,
  logout,
  identitySession,
  notificationRealtime,
  chatRealtime,
  parkingRealtime,
] = await Promise.all([
  read("package.json"),
  read("src/core/auth/oidcClient.ts"),
  read("src/core/auth/oidcSession.ts"),
  read("src/core/api/apiClient.ts"),
  read("src/core/auth/AuthProvider.tsx"),
  read("src/features/auth/pages/LoginPage.tsx"),
  read("src/core/auth/logout.ts"),
  read("src/core/auth/session.ts"),
  read("src/features/notifications/api/notificationRealtime.ts"),
  read("src/features/support/api/chatRealtime.ts"),
  read("src/features/parking/api/parkingRealtime.ts"),
]);

const pkg = JSON.parse(packageJson);
assert.match(pkg.dependencies?.["oidc-client-ts"] ?? "", /^\^?3\./);
assert.match(oidcClient, /new UserManager\(oidcSettings\)/);
assert.match(oidcClient, /response_type:\s*"code"/);
assert.match(oidcClient, /disablePKCE:\s*false/);
assert.match(oidcClient, /loadUserInfo:\s*false/);
assert.match(oidcClient, /automaticSilentRenew:\s*false/);
assert.match(oidcClient, /accessTokenExpiringNotificationTimeInSeconds:\s*90/);
assert.match(oidcClient, /window\.sessionStorage/);
assert.doesNotMatch(oidcClient, /client_secret|offline_access/);

assert.match(oidcSession, /signinRedirectCallback/);
assert.match(oidcSession, /signinSilent/);
assert.match(oidcSession, /if \(renewPromise\) return renewPromise/);
assert.match(oidcSession, /addAccessTokenExpiring[\s\S]*?renewToken/);
assert.match(oidcSession, /signoutRedirect/);
assert.doesNotMatch(oidcSession, /grant_type|code_verifier|code_challenge|\/token/);

assert.doesNotMatch(identitySession, /realm_access|resource_access|authorities|roles\?/);
assert.match(identitySession, /role:\s*"UNKNOWN"/);
assert.match(identitySession, /permissionCodes:\s*undefined/);
assert.match(login, /await handleSigninCallback\(\)/);
assert.match(login, /await bootstrapSocialAccount[\s\S]*?beginAuthenticatedSession/);
assert.doesNotMatch(login, /exchangeKeycloakAuthorizationCode|saveAuthTokens|code_verifier|code_challenge/);

assert.match(authProvider, /AuthenticationStatus/);
assert.match(authProvider, /"initializing"[\s\S]*?"unauthenticated"[\s\S]*?"authenticated"[\s\S]*?"renewing"[\s\S]*?"error"/);
assert.match(authProvider, /fetchCurrentAccess:[\s\S]*?fetchProfile:/);
assert.match(authProvider, /permissionCodes\) \|\| accessStatus !== "loading"/);
assert.match(apiClient, /response\.status === 401[\s\S]*?renewToken[\s\S]*?sendJsonRequest/);
assert.doesNotMatch(apiClient, /response\.status === 403[\s\S]*?renew/);
assert.match(logout, /signoutRedirect/);
assert.match(logout, /removeOidcUser/);

for (const realtime of [notificationRealtime, chatRealtime, parkingRealtime]) {
  assert.match(realtime, /beforeConnect:[\s\S]*?await getValidAccessToken/);
  assert.match(realtime, /subscribeAccessTokenChanged[\s\S]*?deactivate[\s\S]*?activate/);
  assert.doesNotMatch(realtime, /refresh_token/i);
}

async function sourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map((entry) => {
    const absolute = path.join(directory, entry.name);
    return entry.isDirectory()
      ? sourceFiles(absolute)
      : /\.(ts|tsx)$/.test(entry.name) ? [absolute] : [];
  }));
  return nested.flat();
}

const sources = await sourceFiles(path.join(root, "src"));
const allSource = (await Promise.all(sources.map((file) => readFile(file, "utf8")))).join("\n");
assert.equal((allSource.match(/new UserManager\(/g) ?? []).length, 1, "Only one UserManager may exist");
assert.doesNotMatch(allSource, /from ["']@\/core\/auth\/tokenRefresh["']/);
assert.equal(await stat(path.join(root, "src/core/auth/tokenRefresh.ts")).then(() => true, () => false), false);

console.log("oidc-client-ts migration, single-manager, refresh, access-gate, logout, and realtime guards passed.");
