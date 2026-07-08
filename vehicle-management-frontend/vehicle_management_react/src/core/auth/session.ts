import type { CurrentUser } from "@/shared/types/common";

const ACCESS_TOKEN_KEY = "vm_access_token";
const REFRESH_TOKEN_KEY = "vm_refresh_token";
const ID_TOKEN_KEY = "vm_id_token";

export type AuthTokenSet = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
};

type JwtPayload = {
  account_id?: string;
  email?: string;
  family_name?: string;
  given_name?: string;
  name?: string;
  preferred_username?: string;
  realm_access?: {
    roles?: string[];
  };
};

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function saveAuthTokens(tokens: AuthTokenSet, remember = true) {
  const storage = remember ? localStorage : sessionStorage;
  clearAuthTokens();
  storage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  if (tokens.refreshToken) storage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  if (tokens.idToken) storage.setItem(ID_TOKEN_KEY, tokens.idToken);
}

export function clearAuthTokens() {
  [localStorage, sessionStorage].forEach((storage) => {
    storage.removeItem(ACCESS_TOKEN_KEY);
    storage.removeItem(REFRESH_TOKEN_KEY);
    storage.removeItem(ID_TOKEN_KEY);
  });
}

export function getCurrentUserFromStoredToken(): CurrentUser | null {
  const accessToken = getAccessToken();
  if (!accessToken) return null;
  return getCurrentUserFromAccessToken(accessToken);
}

export function getCurrentUserFromAccessToken(accessToken: string): CurrentUser | null {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return null;

  const username = payload.preferred_username ?? payload.email ?? "user";
  const fullName = payload.name ?? [payload.given_name, payload.family_name].filter(Boolean).join(" ") ?? username;

  return {
    id: payload.account_id ?? username,
    username,
    fullName,
    role: resolveAppRole(payload.realm_access?.roles ?? []),
    avatarUrl: "/assets/admin/dist/img/user2-160x160.jpg",
  };
}

function decodeJwtPayload(token: string): JwtPayload | null {
  const [, encodedPayload] = token.split(".");
  if (!encodedPayload) return null;

  try {
    const base64 = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const json = decodeURIComponent(
      atob(padded)
        .split("")
        .map((character) => `%${character.charCodeAt(0).toString(16).padStart(2, "0")}`)
        .join(""),
    );
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

function resolveAppRole(roles: string[]): CurrentUser["role"] {
  if (roles.includes("CUSTOMER")) return "CUSTOMER";
  if (roles.includes("EMPLOYEE") || roles.includes("PARKING_MANAGER")) return "EMPLOYEE";
  return "ADMIN";
}
