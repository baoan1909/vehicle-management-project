import { ErrorResponse, type User } from "oidc-client-ts";
import { oidcUserManager } from "@/core/auth/oidcClient";

const TOKEN_EXPIRY_SKEW_SECONDS = 90;

type OidcSessionEvents = {
  onRenewError?: (error: unknown) => void;
  onRenewing?: () => void;
  onUserLoaded?: (user: User) => void;
  onUserRemoved?: () => void;
};

let callbackPromise: Promise<User> | null = null;
let completedCallbackUrl = "";
let callbackInProgress = false;
let renewPromise: Promise<User | null> | null = null;
let lastAccessToken = "";
const accessTokenListeners = new Set<() => void>();
const sessionListeners = new Set<OidcSessionEvents>();

oidcUserManager.events.addAccessTokenExpiring(() => {
  void renewToken().catch(() => undefined);
});

oidcUserManager.events.addUserLoaded((user) => {
  const tokenChanged = Boolean(lastAccessToken && user.access_token && lastAccessToken !== user.access_token);
  lastAccessToken = user.access_token;
  if (callbackInProgress) return;
  sessionListeners.forEach((listener) => listener.onUserLoaded?.(user));
  if (tokenChanged) accessTokenListeners.forEach((listener) => listener());
});

oidcUserManager.events.addUserUnloaded(() => {
  lastAccessToken = "";
  sessionListeners.forEach((listener) => listener.onUserRemoved?.());
});

export async function getOidcUser() {
  const user = await oidcUserManager.getUser();
  if (user?.access_token) lastAccessToken = user.access_token;
  return user;
}

export async function getAccessToken() {
  return (await getOidcUser())?.access_token ?? null;
}

export async function getValidAccessToken() {
  const user = await getOidcUser();
  if (!user) return null;

  const secondsRemaining = (user.expires_at ?? 0) - Math.floor(Date.now() / 1000);
  if (!user.expired && secondsRemaining > TOKEN_EXPIRY_SKEW_SECONDS) {
    return user.access_token;
  }

  return (await renewToken())?.access_token ?? null;
}

export function renewToken() {
  if (renewPromise) return renewPromise;

  renewPromise = getOidcUser()
    .then(async (user) => {
      if (!user?.refresh_token) {
        await oidcUserManager.removeUser();
        return null;
      }
      sessionListeners.forEach((listener) => listener.onRenewing?.());
      return oidcUserManager.signinSilent();
    })
    .catch(async (error: unknown) => {
      sessionListeners.forEach((listener) => listener.onRenewError?.(error));
      if (isTerminalOidcSessionError(error)) {
        await oidcUserManager.removeUser();
      }
      throw error;
    })
    .finally(() => {
      renewPromise = null;
    });

  return renewPromise;
}

export async function refreshAccessToken() {
  return (await renewToken())?.access_token ?? null;
}

export function signinRedirect(options: { prompt?: "login" } = {}) {
  return oidcUserManager.signinRedirect({ ...options, redirectMethod: "replace" });
}

export function handleSigninCallback(url = window.location.href) {
  if (callbackPromise) return callbackPromise;
  if (completedCallbackUrl === url) {
    return getOidcUser().then((user) => {
      if (!user) throw new Error("OIDC callback đã được xử lý nhưng không còn phiên đăng nhập.");
      return user;
    });
  }

  callbackInProgress = true;
  callbackPromise = oidcUserManager.signinRedirectCallback(url)
    .then((user) => {
      completedCallbackUrl = url;
      return user;
    })
    .finally(() => {
      callbackInProgress = false;
      callbackPromise = null;
    });
  return callbackPromise;
}

export function signoutRedirect() {
  return oidcUserManager.signoutRedirect({ redirectMethod: "replace" });
}

export function removeOidcUser() {
  return oidcUserManager.removeUser();
}

export function subscribeOidcSessionEvents(listener: OidcSessionEvents) {
  sessionListeners.add(listener);
  return () => sessionListeners.delete(listener);
}

export function subscribeAccessTokenChanged(listener: () => void) {
  accessTokenListeners.add(listener);
  return () => accessTokenListeners.delete(listener);
}

export function isOidcSigninResponse(search: string) {
  const params = new URLSearchParams(search);
  return params.has("code") || params.has("error");
}

function isTerminalOidcSessionError(error: unknown) {
  return error instanceof ErrorResponse
    && ["invalid_grant", "login_required", "session_expired"].includes(error.error ?? "");
}
