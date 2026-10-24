import type { User } from "oidc-client-ts";
import type { CurrentUser } from "@/shared/types/common";
import { DEFAULT_USER_AVATAR_URL } from "@/shared/utils/accountStatus";

const LEGACY_AUTH_KEYS = [
  "vm_access_token",
  "vm_refresh_token",
  "vm_id_token",
  "vm_current_user",
  "vm_pkce_code_verifier",
  "vm_oauth_state",
  "vm_keycloak_processed_authorization_code",
];

export function createCurrentUserFromOidcUser(user: User): CurrentUser {
  const profile = user.profile;
  const username = textClaim(profile.preferred_username) || textClaim(profile.email) || profile.sub;
  const fullName = textClaim(profile.name)
    || [textClaim(profile.given_name), textClaim(profile.family_name)].filter(Boolean).join(" ")
    || username;

  return {
    id: profile.sub,
    username,
    fullName,
    role: "UNKNOWN",
    avatarUrl: DEFAULT_USER_AVATAR_URL,
    email: textClaim(profile.email) || undefined,
    permissionCodes: undefined,
  };
}

export function getIdentityProvider(user: User) {
  const profile = user.profile as typeof user.profile & { identity_provider?: unknown };
  return textClaim(profile.identity_provider).toLowerCase() || null;
}

export function clearLegacyAuthStorage() {
  [localStorage, sessionStorage].forEach((storage) => {
    LEGACY_AUTH_KEYS.forEach((key) => storage.removeItem(key));
  });
}

function textClaim(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}
