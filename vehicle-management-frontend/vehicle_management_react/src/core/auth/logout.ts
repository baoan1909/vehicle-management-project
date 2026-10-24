import type { Dispatch, SetStateAction } from "react";
import { clearLegacyAuthStorage } from "@/core/auth/session";
import { removeOidcUser, signoutRedirect } from "@/core/auth/oidcSession";
import type { CurrentUser } from "@/shared/types/common";

const LOGOUT_REDIRECT_GUARD_KEY = "vm_logout_redirect_guard";
const LOGOUT_REDIRECT_PATH = "/";
const LOGOUT_FALLBACK_MS = 4000;
let logoutNavigationStarted = false;

export function getLogoutRedirectPath() {
  return LOGOUT_REDIRECT_PATH;
}

export function isLogoutRedirectGuardActive() {
  return sessionStorage.getItem(LOGOUT_REDIRECT_GUARD_KEY) === "1";
}

export function consumeLogoutRedirectGuard() {
  const guarded = isLogoutRedirectGuardActive();
  if (guarded) {
    sessionStorage.removeItem(LOGOUT_REDIRECT_GUARD_KEY);
    logoutNavigationStarted = false;
  }
  return guarded;
}

export function logoutCurrentUser(setUser: Dispatch<SetStateAction<CurrentUser | null>>) {
  if (logoutNavigationStarted) return;
  logoutNavigationStarted = true;

  setUser(null);
  clearLegacyAuthStorage();
  sessionStorage.setItem(LOGOUT_REDIRECT_GUARD_KEY, "1");

  const fallbackId = window.setTimeout(() => {
    void removeOidcUser().finally(() => window.location.replace(LOGOUT_REDIRECT_PATH));
  }, LOGOUT_FALLBACK_MS);

  void signoutRedirect().catch(() => {
    window.clearTimeout(fallbackId);
    return removeOidcUser().finally(() => window.location.replace(LOGOUT_REDIRECT_PATH));
  });
}
