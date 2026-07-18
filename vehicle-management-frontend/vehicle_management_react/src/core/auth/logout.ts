import type { Dispatch, SetStateAction } from "react";
import { clearAuthTokens, getIdToken } from "@/core/auth/session";
import { buildKeycloakLogoutUrl } from "@/features/auth/api/authApi";
import type { CurrentUser } from "@/shared/types/common";

const LOGOUT_REDIRECT_GUARD_KEY = "vm_logout_redirect_guard";
const LOCAL_LOGOUT_REDIRECT_PATH = "/login";

export function consumeLogoutRedirectGuard() {
  const guarded = sessionStorage.getItem(LOGOUT_REDIRECT_GUARD_KEY) === "1";
  if (guarded) {
    sessionStorage.removeItem(LOGOUT_REDIRECT_GUARD_KEY);
  }
  return guarded;
}

export function logoutCurrentUser(setUser: Dispatch<SetStateAction<CurrentUser | null>>) {
  const idToken = getIdToken();
  const logoutUrl = idToken ? buildLogoutUrlSafely(idToken) : null;

  sessionStorage.setItem(LOGOUT_REDIRECT_GUARD_KEY, "1");
  clearAuthTokens();
  setUser(null);

  window.location.replace(logoutUrl ?? LOCAL_LOGOUT_REDIRECT_PATH);
}

function buildLogoutUrlSafely(idToken: string) {
  try {
    return buildKeycloakLogoutUrl(idToken);
  } catch (error) {
    console.error(error);
    return null;
  }
}
