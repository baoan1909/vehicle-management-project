import { expect, test, type Page } from "@playwright/test";

const accessUrl = "**/vehicle-management/api/iam/accounts/current-access";
const profileUrl = "**/vehicle-management/api/iam/accounts/onboarding";
const actorA = "00000000-0000-0000-0000-0000000000a1";
const actorB = "00000000-0000-0000-0000-0000000000b2";

type AccessOverrides = Partial<{
  accountId: string;
  accountStatus: string;
  accessRevision: string;
  accessState: string;
  onboardingRequired: boolean;
  permissionCodes: string[];
  requiredAction: string | null;
  roleCode: string;
  username: string;
}>;

function envelope(data: unknown) {
  return { data, message: "ok", success: true, timestamp: new Date().toISOString() };
}

function access(overrides: AccessOverrides = {}) {
  return {
    accountId: actorA,
    accountStatus: "ACTIVE",
    accessRevision: "revision-1",
    accessState: "ACTIVE",
    onboardingRequired: false,
    permissionCodes: ["DASHBOARD_READ_ALL"],
    requiredAction: null,
    roleCode: "CUSTOM_OPERATIONS",
    username: "actor-a",
    ...overrides,
  };
}

function token(accountId = actorA, username = "actor-a") {
  const encode = (value: object) => Buffer.from(JSON.stringify(value)).toString("base64url");
  return `${encode({ alg: "none" })}.${encode({
    account_id: accountId,
    exp: Math.floor(Date.now() / 1000) + 3600,
    preferred_username: username,
  })}.signature`;
}

async function seedToken(page: Page, accountId = actorA, username = "actor-a", refreshToken?: string) {
  await page.addInitScript(({ accessToken, subject, preferredUsername, storedRefreshToken }) => {
    const authority = "http://localhost:8081/realms/vehicle-management";
    const clientId = "vehicle-management-frontend";
    const storedUser = {
      access_token: accessToken,
      expires_at: Math.floor(Date.now() / 1000) + 3600,
      profile: {
        aud: clientId,
        exp: Math.floor(Date.now() / 1000) + 3600,
        iat: Math.floor(Date.now() / 1000),
        iss: authority,
        preferred_username: preferredUsername,
        sub: subject,
      },
      scope: "openid profile email roles",
      token_type: "Bearer",
    };
    if (storedRefreshToken) Object.assign(storedUser, { refresh_token: storedRefreshToken });
    sessionStorage.setItem(`oidc.user:${authority}:${clientId}`, JSON.stringify(storedUser));
  }, {
    accessToken: token(accountId, username),
    preferredUsername: username,
    storedRefreshToken: refreshToken,
    subject: accountId,
  });
}

async function mockOidcRefresh(
  page: Page,
  onTokenRequest: () => void,
  renewedAccessToken = token(actorA, "actor-a-renewed"),
) {
  const authority = "http://localhost:8081/realms/vehicle-management";
  await page.route(`${authority}/.well-known/openid-configuration`, (route) => route.fulfill({
    contentType: "application/json",
    headers: { "Access-Control-Allow-Origin": "*" },
    body: JSON.stringify({
      authorization_endpoint: `${authority}/protocol/openid-connect/auth`,
      end_session_endpoint: `${authority}/protocol/openid-connect/logout`,
      issuer: authority,
      jwks_uri: `${authority}/protocol/openid-connect/certs`,
      token_endpoint: `${authority}/protocol/openid-connect/token`,
    }),
  }));
  await page.route(`${authority}/protocol/openid-connect/token`, (route) => {
    onTokenRequest();
    return route.fulfill({
      contentType: "application/json",
      headers: { "Access-Control-Allow-Origin": "*" },
      body: JSON.stringify({
        access_token: renewedAccessToken,
        expires_in: 3600,
        refresh_token: "refresh-b",
        scope: "openid profile email roles",
        token_type: "Bearer",
      }),
    });
  });
}

async function mockProfile(page: Page, status = 200) {
  await page.route(profileUrl, (route) => route.fulfill({
    contentType: "application/json",
    status,
    body: JSON.stringify(status === 200
      ? envelope({ onboardingRequired: false, profile: { fullName: "Presentation name" } })
      : { message: "profile failed" }),
  }));
}

async function mockAccess(page: Page, value: ReturnType<typeof access>, status = 200) {
  await page.route(accessUrl, (route) => route.fulfill({
    contentType: "application/json",
    status,
    body: JSON.stringify(status === 200 ? envelope(value) : { message: "access failed" }),
  }));
}

async function openWithResolvedAccess(page: Page, value = access()) {
  await seedToken(page, value.accountId, value.username);
  await mockProfile(page);
  await mockAccess(page, value);
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
}

test("1 fresh login resolves current-access before exposing permissions", async ({ page }) => {
  await mockProfile(page);
  await mockAccess(page, access());
  await page.goto("/tests/auth-access-harness.html");
  await page.getByTestId("fresh-login").click();
  await expect(page.getByTestId("status")).toHaveText("ready");
  await expect(page.getByTestId("permissions")).toHaveText("DASHBOARD_READ_ALL");
});

test("2 reload with a token remains unresolved while current-access is pending", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let release!: () => void;
  const gate = new Promise<void>((resolve) => { release = resolve; });
  await page.route(accessUrl, async (route) => {
    await gate;
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(access())) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("loading");
  await expect(page.getByTestId("permissions")).toHaveText("unresolved");
  release();
  await expect(page.getByTestId("status")).toHaveText("ready");
});

test("3 empty effective permissions is a resolved state, not a bootstrap error", async ({ page }) => {
  await openWithResolvedAccess(page, access({ permissionCodes: [], accessState: "NO_EFFECTIVE_PERMISSIONS" }));
  await expect(page.getByTestId("error")).toHaveText("none");
  await expect(page.getByTestId("permissions")).toHaveText("");
  await expect(page.getByTestId("can-dashboard")).toHaveText("false");
});

test("4 initial current-access 401 clears the session", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  await mockAccess(page, access(), 401);
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("user")).toHaveText("none");
  await expect(page.getByTestId("token-present")).toHaveText("false");
});

test("5 initial current-access 403 fails closed without onboarding fallback", async ({ page }) => {
  await seedToken(page);
  let profileCalls = 0;
  await page.route(profileUrl, (route) => { profileCalls += 1; return route.abort(); });
  await mockAccess(page, access(), 403);
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("error");
  await expect(page.getByTestId("error")).toHaveText("forbidden");
  expect(profileCalls).toBe(0);
});

test("6 initial current-access timeout exposes a retryable error", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  await page.route(accessUrl, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 9_000));
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(access())) }).catch(() => {});
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("error")).toHaveText("timeout", { timeout: 10_000 });
});

test("7 initial offline failure does not create an empty permission snapshot", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  await page.route(accessUrl, (route) => route.abort("internetdisconnected"));
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("error")).toHaveText("offline");
  await expect(page.getByTestId("permissions")).toHaveText("unresolved");
});

test("8 initial 5xx is retryable and retry can recover", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, (route) => {
    calls += 1;
    return route.fulfill({ contentType: "application/json", status: calls === 1 ? 503 : 200,
      body: JSON.stringify(calls === 1 ? { message: "down" } : envelope(access())) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("error");
  await page.getByTestId("retry").click();
  await expect(page.getByTestId("status")).toHaveText("ready");
});

test("9 malformed current-access fails closed as invalid-response", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  await page.route(accessUrl, (route) => route.fulfill({ contentType: "application/json",
    body: JSON.stringify(envelope({ permissionCodes: null })) }));
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("error")).toHaveText("invalid-response");
});

test("10 non-auth profile failure preserves successful authorization", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page, 500);
  await mockAccess(page, access());
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await expect(page.getByTestId("can-dashboard")).toHaveText("true");
});

test("11 profile 401 ends the session even after current-access succeeds", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page, 401);
  await mockAccess(page, access());
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("user")).toHaveText("none");
  await expect(page.getByTestId("token-present")).toHaveText("false");
});

test("12 logout aborts an in-flight bootstrap and prevents stale state publication", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  await page.route(accessUrl, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 500));
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(access())) }).catch(() => {});
  });
  await page.goto("/tests/auth-access-harness.html");
  await page.getByTestId("logout").click();
  await page.waitForTimeout(650);
  await expect(page.getByTestId("user")).toHaveText("none");
  await expect(page.getByTestId("permissions")).toHaveText("unresolved");
});

test("13 responses arriving for an old user cannot overwrite a newer user", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, async (route) => {
    calls += 1;
    if (calls === 2) {
      await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(access({
        accountId: actorB, username: "actor-b", accessRevision: "revision-b",
      }))) });
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
    await route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(access())) }).catch(() => {});
  });
  await page.goto("/tests/auth-access-harness.html");
  await page.getByTestId("switch-b").click();
  await expect(page.getByTestId("user")).toHaveText("actor-b");
  await expect(page.getByTestId("revision")).toHaveText("revision-b");
  await page.waitForTimeout(600);
  await expect(page.getByTestId("user")).toHaveText("actor-b");
});

test("14 same revision does not replace the authorization snapshot", async ({ page }) => {
  await openWithResolvedAccess(page);
  await expect(page.getByTestId("snapshot-changes")).toHaveText("1");
  const before = await page.getByTestId("snapshot-changes").textContent();
  await page.getByTestId("refresh").click();
  await expect(page.getByTestId("status")).toHaveText("ready");
  await expect(page.getByTestId("snapshot-changes")).toHaveText(before ?? "1");
});

test("15 changed revision atomically replaces permissions", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, (route) => {
    calls += 1;
    const value = calls === 1 ? access() : access({ accessRevision: "revision-2", permissionCodes: ["ROLE_READ_ALL"] });
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(value)) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("refresh").click();
  await expect(page.getByTestId("permissions")).toHaveText("ROLE_READ_ALL");
  await expect(page.getByTestId("revision")).toHaveText("revision-2");
});

test("16 revoked current permission recalculates route access", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, (route) => {
    calls += 1;
    const value = calls === 1 ? access() : access({ accessRevision: "revision-2", permissionCodes: [], accessState: "NO_EFFECTIVE_PERMISSIONS" });
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(value)) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("can-dashboard")).toHaveText("true");
  await page.getByTestId("refresh").click();
  await expect(page.getByTestId("can-dashboard")).toHaveText("false");
  await expect(page.getByTestId("landing")).toHaveText("/admin/profile");
});

test("17 locked account refresh moves to a safe route with no permission", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, (route) => {
    calls += 1;
    const value = calls === 1 ? access() : access({ accountStatus: "LOCKED", accessRevision: "locked",
      accessState: "ACCOUNT_LOCKED", requiredAction: "CONTACT_ADMIN", permissionCodes: [] });
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(value)) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("refresh").click();
  await expect(page.getByTestId("landing")).toHaveText("/admin/profile");
  await expect(page.getByTestId("permissions")).toHaveText("");
});

test("18 transient background refresh failure keeps last-known permissions as stale", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, (route) => {
    calls += 1;
    return route.fulfill({ contentType: "application/json", status: calls === 1 ? 200 : 503,
      body: JSON.stringify(calls === 1 ? envelope(access()) : { message: "temporary" }) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("refresh").click();
  await expect(page.getByTestId("status")).toHaveText("stale");
  await expect(page.getByTestId("permissions")).toHaveText("DASHBOARD_READ_ALL");
});

test("19 access-affecting mutation invalidates and refreshes immediately", async ({ page }) => {
  await seedToken(page);
  await mockProfile(page);
  let calls = 0;
  await page.route(accessUrl, (route) => {
    calls += 1;
    const value = calls === 1 ? access() : access({ accessRevision: "post-mutation", permissionCodes: ["ROLE_READ_ALL"] });
    return route.fulfill({ contentType: "application/json", body: JSON.stringify(envelope(value)) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("invalidate").click();
  await expect(page.getByTestId("revision")).toHaveText("post-mutation");
  expect(calls).toBe(2);
});

test("20 custom role with effective permission receives the same route access", async ({ page }) => {
  await openWithResolvedAccess(page, access({ roleCode: "CUSTOM_FLEET_OPERATOR" }));
  await expect(page.getByTestId("can-dashboard")).toHaveText("true");
  await expect(page.getByTestId("landing")).toHaveText("/admin/dashboard");
});

test("21 role labels never grant missing permission or missing partner scope", async ({ page }) => {
  await openWithResolvedAccess(page, access({ roleCode: "SYSTEM_ADMIN", permissionCodes: [], accessState: "NO_EFFECTIVE_PERMISSIONS" }));
  await expect(page.getByTestId("can-dashboard")).toHaveText("false");

  await page.unroute(accessUrl);
  await mockAccess(page, access({ roleCode: "PARTNER_ADMIN", permissionCodes: [], accessState: "ORGANIZATION_ACCESS_REQUIRED",
    requiredAction: "CONTACT_ORGANIZATION_ADMIN", accessRevision: "partner-no-scope" }));
  await page.getByTestId("invalidate").click();
  await expect(page.getByTestId("revision")).toHaveText("partner-no-scope");
  await expect(page.getByTestId("can-dashboard")).toHaveText("false");
});

test("22 concurrent 401 responses share one OIDC refresh operation", async ({ page }) => {
  await seedToken(page, actorA, "actor-a", "refresh-a");
  await mockProfile(page);
  await mockAccess(page, access());
  let tokenRequests = 0;
  const renewedAccessToken = token(actorA, "actor-a-renewed");
  await mockOidcRefresh(page, () => { tokenRequests += 1; }, renewedAccessToken);
  let apiRequests = 0;
  await page.route("**/vehicle-management/api/test-renew", (route) => {
    apiRequests += 1;
    const authorization = route.request().headers().authorization ?? "";
    const renewed = authorization === `Bearer ${renewedAccessToken}`;
    return route.fulfill({
      contentType: "application/json",
      status: renewed ? 200 : 401,
      body: JSON.stringify(renewed ? { ok: true } : { message: "expired" }),
    });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("request-twice").click();
  await expect(page.getByTestId("request-result")).toHaveText("success");
  expect(tokenRequests).toBe(1);
  expect(apiRequests).toBe(4);
});

test("23 a request is retried at most once after successful renewal", async ({ page }) => {
  await seedToken(page, actorA, "actor-a", "refresh-a");
  await mockProfile(page);
  await mockAccess(page, access());
  let tokenRequests = 0;
  await mockOidcRefresh(page, () => { tokenRequests += 1; });
  let apiRequests = 0;
  await page.route("**/vehicle-management/api/test-renew", (route) => {
    apiRequests += 1;
    return route.fulfill({ contentType: "application/json", status: 401, body: JSON.stringify({ message: "denied" }) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("request-once").click();
  await expect(page.getByTestId("request-result")).toHaveText("error:401");
  expect(tokenRequests).toBe(1);
  expect(apiRequests).toBe(2);
});

test("24 a 403 response never triggers token renewal", async ({ page }) => {
  await seedToken(page, actorA, "actor-a", "refresh-a");
  await mockProfile(page);
  await mockAccess(page, access());
  let tokenRequests = 0;
  await mockOidcRefresh(page, () => { tokenRequests += 1; });
  let apiRequests = 0;
  await page.route("**/vehicle-management/api/test-renew", (route) => {
    apiRequests += 1;
    return route.fulfill({ contentType: "application/json", status: 403, body: JSON.stringify({ message: "forbidden" }) });
  });
  await page.goto("/tests/auth-access-harness.html");
  await expect(page.getByTestId("status")).toHaveText("ready");
  await page.getByTestId("request-once").click();
  await expect(page.getByTestId("request-result")).toHaveText("error:403");
  expect(tokenRequests).toBe(0);
  expect(apiRequests).toBe(1);
});
