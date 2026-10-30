import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { User } from "oidc-client-ts";
import { AppProviders } from "@/app/providers/AppProviders";
import {
  canAccessAdminRoute,
  canAccessCustomerRoute,
  canAccessRestrictedCustomerSupportRoute,
} from "@/app/routePermissions";
import { apiClient } from "@/core/api/apiClient";
import { resolvePostLoginPath } from "@/core/auth/landing";
import { oidcSettings, oidcUserManager } from "@/core/auth/oidcClient";
import { removeOidcUser } from "@/core/auth/oidcSession";
import { useAuth } from "@/core/auth/useAuth";
import { useAccountStateSync } from "@/features/notifications/context/useAccountStateSync";

function tokenFor(accountId: string, username: string) {
  const encode = (value: object) => btoa(JSON.stringify(value))
    .replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
  return `${encode({ alg: "none" })}.${encode({
    account_id: accountId,
    exp: Math.floor(Date.now() / 1000) + 3600,
    preferred_username: username,
  })}.signature`;
}

const tokenA = tokenFor("00000000-0000-0000-0000-0000000000a1", "actor-a");
const tokenB = tokenFor("00000000-0000-0000-0000-0000000000b2", "actor-b");
const oidcUserStorageKey = `oidc.user:${oidcSettings.authority}:${oidcSettings.client_id}`;

function oidcUser(accountId: string, username: string, accessToken: string) {
  return new User({
    access_token: accessToken,
    expires_at: Math.floor(Date.now() / 1000) + 3600,
    profile: {
      iss: oidcSettings.authority,
      aud: oidcSettings.client_id,
      sub: accountId,
      exp: Math.floor(Date.now() / 1000) + 3600,
      iat: Math.floor(Date.now() / 1000),
      preferred_username: username,
    },
    scope: oidcSettings.scope,
    token_type: "Bearer",
  });
}

function Probe() {
  const auth = useAuth();
  const accountStateSync = useAccountStateSync();
  const [snapshotChanges, setSnapshotChanges] = useState(0);
  const [requestResult, setRequestResult] = useState("idle");
  const previousResolvedUser = useRef(auth.user);

  useEffect(() => {
    if (auth.user && Array.isArray(auth.user.permissionCodes)
        && previousResolvedUser.current !== auth.user) {
      previousResolvedUser.current = auth.user;
      setSnapshotChanges((value) => value + 1);
    }
  }, [auth.user]);

  const freshLogin = async () => {
    const nextUser = oidcUser("00000000-0000-0000-0000-0000000000a1", "actor-a", tokenA);
    await oidcUserManager.storeUser(nextUser);
    auth.beginAuthenticatedSession(nextUser);
  };

  const switchToB = async () => {
    await removeOidcUser();
    auth.setUser(null);
    const nextUser = oidcUser("00000000-0000-0000-0000-0000000000b2", "actor-b", tokenB);
    await oidcUserManager.storeUser(nextUser);
    auth.beginAuthenticatedSession(nextUser);
  };

  const logout = async () => {
    await removeOidcUser();
    auth.setUser(null);
  };

  const requestProtected = async (count: number) => {
    setRequestResult("pending");
    try {
      await Promise.all(Array.from({ length: count }, () => apiClient("/test-renew")));
      setRequestResult("success");
    } catch (error) {
      const status = error && typeof error === "object" && "status" in error ? error.status : "error";
      setRequestResult(`error:${String(status)}`);
    }
  };

  return (
    <main>
      <output data-testid="status">{auth.accessStatus}</output>
      <output data-testid="error">{auth.accessError?.type ?? "none"}</output>
      <output data-testid="user">{auth.user?.username ?? "none"}</output>
      <output data-testid="permissions">{auth.user?.permissionCodes?.join(",") ?? "unresolved"}</output>
      <output data-testid="revision">{auth.user?.accessRevision ?? "none"}</output>
      <output data-testid="full-name">{auth.user?.fullName ?? "none"}</output>
      <output data-testid="profile-version">{accountStateSync.profileInvalidationVersion}</output>
      <output data-testid="landing">{auth.user ? resolvePostLoginPath(auth.user) : "/login"}</output>
      <output data-testid="can-dashboard">{String(canAccessAdminRoute(auth.user, "/admin/dashboard"))}</output>
      <output data-testid="can-customer-support">{String(canAccessCustomerRoute(auth.user, "/customer/support"))}</output>
      <output data-testid="can-customer-chat">{String(canAccessCustomerRoute(auth.user, "/customer/support/chat"))}</output>
      <output data-testid="can-restricted-customer-support">{String(canAccessRestrictedCustomerSupportRoute(auth.user, "/customer/support"))}</output>
      <output data-testid="can-restricted-customer-chat">{String(canAccessRestrictedCustomerSupportRoute(auth.user, "/customer/support/chat"))}</output>
      <output data-testid="can-restricted-customer-vehicles">{String(canAccessRestrictedCustomerSupportRoute(auth.user, "/customer/vehicles"))}</output>
      <output data-testid="can-customer-vehicles">{String(canAccessCustomerRoute(auth.user, "/customer/vehicles"))}</output>
      <output data-testid="can-customer-wallet">{String(canAccessCustomerRoute(auth.user, "/customer/wallet"))}</output>
      <output data-testid="snapshot-changes">{snapshotChanges}</output>
      <output data-testid="token-present">{String(Boolean(sessionStorage.getItem(oidcUserStorageKey)))}</output>
      <output data-testid="request-result">{requestResult}</output>
      <button data-testid="fresh-login" onClick={() => void freshLogin()}>fresh login</button>
      <button data-testid="retry" onClick={auth.retryAccess}>retry</button>
      <button data-testid="refresh" onClick={() => void auth.refreshAccess()}>refresh</button>
      <button data-testid="invalidate" onClick={() => void auth.invalidateAndRefreshAccess("test-mutation")}>invalidate</button>
      <button data-testid="switch-b" onClick={() => void switchToB()}>switch B</button>
      <button data-testid="logout" onClick={() => void logout()}>logout</button>
      <button data-testid="request-once" onClick={() => void requestProtected(1)}>request once</button>
      <button data-testid="request-twice" onClick={() => void requestProtected(2)}>request twice</button>
      <button data-testid="reload-notifications" onClick={() => void accountStateSync.reloadNotifications()}>
        reload notifications
      </button>
    </main>
  );
}

createRoot(document.getElementById("root")!).render(
  <AppProviders><Probe /></AppProviders>,
);
