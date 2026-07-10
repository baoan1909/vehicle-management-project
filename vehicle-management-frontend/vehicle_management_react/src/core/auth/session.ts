import type { CurrentUser } from "@/shared/types/common";
import { DEFAULT_USER_AVATAR_URL, getRoleLabel } from "@/shared/utils/accountStatus";

const ACCESS_TOKEN_KEY = "vm_access_token";
const REFRESH_TOKEN_KEY = "vm_refresh_token";
const ID_TOKEN_KEY = "vm_id_token";
const CURRENT_USER_KEY = "vm_current_user";

export type AuthTokenSet = {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
};

type JwtPayload = {
  account_id?: string;
  authorities?: string[];
  email?: string;
  family_name?: string;
  given_name?: string;
  groups?: string[];
  name?: string;
  preferred_username?: string;
  role?: string;
  role_code?: string;
  roleCode?: string;
  roles?: string[];
  realm_access?: {
    roles?: string[];
  };
  resource_access?: Record<
    string,
    {
      roles?: string[];
    }
  >;
};

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getIdToken() {
  return localStorage.getItem(ID_TOKEN_KEY) ?? sessionStorage.getItem(ID_TOKEN_KEY);
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
    storage.removeItem(CURRENT_USER_KEY);
  });
}

export function getCurrentUserFromStoredToken(): CurrentUser | null {
  const accessToken = getAccessToken();
  if (!accessToken) return null;
  const tokenUser = getCurrentUserFromAccessToken(accessToken);
  if (!tokenUser) return null;

  const cachedUser = getStoredCurrentUserSnapshot(tokenUser);
  if (!cachedUser) return tokenUser;

  return {
    ...tokenUser,
    ...cachedUser,
    role: tokenUser.role,
    roleLabel: tokenUser.roleLabel
  };
}

export function getCurrentUserFromAccessToken(accessToken: string): CurrentUser | null {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) return null;

  const username = payload.preferred_username?.trim() || payload.email?.trim() || "user";
  const tokenFullName = payload.name?.trim() || [payload.given_name, payload.family_name].filter(Boolean).join(" ").trim();
  const fullName = tokenFullName || username;

  const role = resolveAppRole(getTokenRoles(payload));

  return {
    id: payload.account_id ?? username,
    username,
    fullName,
    role,
    avatarUrl: DEFAULT_USER_AVATAR_URL,
    email: payload.email,
    roleLabel: getRoleLabel(role),
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

function getTokenRoles(payload: JwtPayload) {
  return [
    payload.role,
    payload.roleCode,
    payload.role_code,
    ...(payload.roles ?? []),
    ...(payload.groups ?? []),
    ...(payload.authorities ?? []),
    ...(payload.realm_access?.roles ?? []),
    ...Object.values(payload.resource_access ?? {}).flatMap((resource) => resource.roles ?? [])
  ].filter((role): role is string => typeof role === "string" && role.trim().length > 0);
}

function resolveAppRole(roles: string[]): CurrentUser["role"] {
  const normalizedRoles = new Set(roles.map((role) => role.replace(/^ROLE_/, "").trim().toUpperCase()));

  if (normalizedRoles.has("SYSTEM_ADMIN")) return "SYSTEM_ADMIN";
  if (normalizedRoles.has("PARKING_MANAGER")) return "PARKING_MANAGER";
  if (normalizedRoles.has("EMPLOYEE")) return "EMPLOYEE";
  if (normalizedRoles.has("CUSTOMER")) return "CUSTOMER";
  return "UNKNOWN";
}

export function saveCurrentUserSnapshot(user: CurrentUser | null) {
  if (!user) {
    localStorage.removeItem(CURRENT_USER_KEY);
    sessionStorage.removeItem(CURRENT_USER_KEY);
    return;
  }

  const storage = getTokenStorage();
  storage.setItem(CURRENT_USER_KEY, JSON.stringify(user));

  const otherStorage = storage === localStorage ? sessionStorage : localStorage;
  otherStorage.removeItem(CURRENT_USER_KEY);
}

function getStoredCurrentUserSnapshot(tokenUser: CurrentUser) {
  const storage = getTokenStorage();
  const rawUser = storage.getItem(CURRENT_USER_KEY);
  if (!rawUser) return null;

  try {
    const cachedUser = JSON.parse(rawUser) as CurrentUser;
    const isSameUser =
      cachedUser.username === tokenUser.username ||
      Boolean(cachedUser.email && tokenUser.email && cachedUser.email === tokenUser.email);

    return isSameUser ? cachedUser : null;
  } catch {
    storage.removeItem(CURRENT_USER_KEY);
    return null;
  }
}

function getTokenStorage() {
  if (localStorage.getItem(ACCESS_TOKEN_KEY)) return localStorage;
  if (sessionStorage.getItem(ACCESS_TOKEN_KEY)) return sessionStorage;
  return localStorage;
}
