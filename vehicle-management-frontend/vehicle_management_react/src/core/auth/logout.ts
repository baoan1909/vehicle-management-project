import type { Dispatch, SetStateAction } from "react";
import { clearAuthLogoutInProgress, clearAuthTokens, getIdToken, markAuthLogoutInProgress } from "@/core/auth/session";
import { buildKeycloakLogoutUrl } from "@/features/auth/api/authApi";
import type { CurrentUser } from "@/shared/types/common";

const LOGOUT_REDIRECT_GUARD_KEY = "vm_logout_redirect_guard";
const LOGOUT_REDIRECT_PATH = "/pricing";
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
    clearAuthLogoutInProgress();
    logoutNavigationStarted = false;
  }
  return guarded;
}

export function logoutCurrentUser(setUser: Dispatch<SetStateAction<CurrentUser | null>>) {
  if (logoutNavigationStarted) return;
  logoutNavigationStarted = true;

  const idToken = getIdToken();
  const logoutUrl = idToken ? buildLogoutUrlSafely(idToken) : null;

  markAuthLogoutInProgress();
  sessionStorage.setItem(LOGOUT_REDIRECT_GUARD_KEY, "1");
  clearAuthTokens();

  if (logoutUrl) {
    void logoutKeycloakInBackground(logoutUrl);
  }

  setUser(null);
  window.location.replace(LOGOUT_REDIRECT_PATH);
}

function buildLogoutUrlSafely(idToken: string) {
  try {
    return buildKeycloakLogoutUrl(idToken, LOGOUT_REDIRECT_PATH);
  } catch (error) {
    console.error(error);
    return null;
  }
}

async function logoutKeycloakInBackground(logoutUrl: string) {
  try {
    await fetch(logoutUrl, {
      credentials: "include",
      keepalive: true,
      method: "GET",
      mode: "no-cors",
    });
  } catch (error) {
    console.error(error);
  }
}
