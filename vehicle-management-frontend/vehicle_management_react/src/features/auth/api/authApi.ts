import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";
import { appConfig } from "@/config/env";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type RegisterAccountRequest = {
  username: string;
  email: string;
  password: string;
  fullName: string;
};

export type RegisterAccountResponse = {
  accountId: string;
  accountStatus: string;
  nextAction: string;
  onboardingRequired: boolean;
};

export type ForgotPasswordRequest = {
  email: string;
};

export type ResendVerificationEmailRequest = {
  email: string;
};

type KeycloakTokenResponse = {
  access_token: string;
  expires_in?: number;
  id_token?: string;
  refresh_expires_in?: number;
  refresh_token?: string;
  scope?: string;
  token_type?: string;
};

const PKCE_CODE_VERIFIER_KEY = "vm_pkce_code_verifier";
type KeycloakLoginOptions = {
  prompt?: "login";
};

export async function registerAccount(payload: RegisterAccountRequest) {
  return apiClient<ApiResponse<RegisterAccountResponse>>(apiEndpoints.auth.register, {
    method: "POST",
    body: payload,
    skipAuth: true,
  });
}

export async function requestPasswordReset(payload: ForgotPasswordRequest) {
  return apiClient<ApiResponse<null>>(apiEndpoints.auth.forgotPassword, {
    method: "POST",
    body: payload,
    skipAuth: true,
  });
}

export async function resendVerificationEmail(payload: ResendVerificationEmailRequest) {
  return apiClient<ApiResponse<null>>(apiEndpoints.auth.resendVerificationEmail, {
    method: "POST",
    body: payload,
    skipAuth: true,
  });
}

export async function buildKeycloakLoginUrl(options: KeycloakLoginOptions = {}) {
  const loginUrl = new URL(appConfig.keycloakLoginUrl);
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await buildCodeChallenge(codeVerifier);

  sessionStorage.setItem(PKCE_CODE_VERIFIER_KEY, codeVerifier);
  loginUrl.searchParams.set("code_challenge", codeChallenge);
  loginUrl.searchParams.set("code_challenge_method", "S256");
  loginUrl.searchParams.set("scope", normalizeLoginScopes(loginUrl.searchParams.get("scope")));
  if (options.prompt) {
    loginUrl.searchParams.set("prompt", options.prompt);
  } else {
    loginUrl.searchParams.delete("prompt");
  }

  return loginUrl.toString();
}

function normalizeLoginScopes(scope: string | null) {
  const scopes = new Set((scope ?? "openid").split(/\s+/).filter(Boolean));
  ["openid", "profile", "email", "roles", "offline_access"].forEach((requiredScope) => scopes.add(requiredScope));
  return Array.from(scopes).join(" ");
}

export async function exchangeKeycloakAuthorizationCode(code: string) {
  const loginUrl = new URL(appConfig.keycloakLoginUrl);
  const tokenUrl = new URL(loginUrl.toString());
  tokenUrl.pathname = tokenUrl.pathname.replace(/\/auth$/, "/token");

  const formData = new URLSearchParams();
  formData.set("grant_type", "authorization_code");
  formData.set("client_id", loginUrl.searchParams.get("client_id") ?? "vehicle-management-frontend");
  formData.set("code", code);
  formData.set("redirect_uri", loginUrl.searchParams.get("redirect_uri") ?? `${window.location.origin}${window.location.pathname}`);

  const codeVerifier = sessionStorage.getItem(PKCE_CODE_VERIFIER_KEY);
  if (codeVerifier) {
    formData.set("code_verifier", codeVerifier);
  }

  const response = await fetch(tokenUrl.toString(), {
    body: formData,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    method: "POST",
  });

  const responseBody = (await response.json()) as KeycloakTokenResponse & { error_description?: string };

  if (!response.ok) {
    throw new Error(responseBody.error_description ?? `Keycloak token error ${response.status}`);
  }

  return responseBody;
}

export function buildKeycloakLogoutUrl(idToken?: string | null, postLogoutRedirectPath = "/pricing") {
  const loginUrl = new URL(appConfig.keycloakLoginUrl);
  const logoutUrl = new URL(loginUrl.toString());
  logoutUrl.pathname = logoutUrl.pathname.replace(/\/auth$/, "/logout");
  logoutUrl.search = "";
  logoutUrl.searchParams.set("client_id", loginUrl.searchParams.get("client_id") ?? "vehicle-management-frontend");
  logoutUrl.searchParams.set("post_logout_redirect_uri", buildFrontendRedirectUri(loginUrl, postLogoutRedirectPath));
  if (idToken) {
    logoutUrl.searchParams.set("id_token_hint", idToken);
  }
  return logoutUrl.toString();
}

function buildFrontendRedirectUri(loginUrl: URL, path: string) {
  const configuredRedirectUri = loginUrl.searchParams.get("redirect_uri");
  const redirectUrl = configuredRedirectUri ? new URL(configuredRedirectUri) : new URL(window.location.origin);
  redirectUrl.pathname = path.startsWith("/") ? path : `/${path}`;
  redirectUrl.search = "";
  redirectUrl.hash = "";
  return redirectUrl.toString();
}

function generateCodeVerifier() {
  const randomValues = new Uint8Array(64);
  crypto.getRandomValues(randomValues);
  return base64UrlEncode(randomValues);
}

async function buildCodeChallenge(codeVerifier: string) {
  const encodedVerifier = new TextEncoder().encode(codeVerifier);
  const digest = await crypto.subtle.digest("SHA-256", encodedVerifier);
  return base64UrlEncode(new Uint8Array(digest));
}

function base64UrlEncode(bytes: Uint8Array) {
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
