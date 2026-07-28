import { appConfig } from "@/config/env";
import {
  clearAuthTokens,
  getAccessToken,
  getRefreshToken,
  isAuthLogoutInProgress,
  isAccessTokenExpiringWithin,
  saveRefreshedAuthTokens,
} from "@/core/auth/session";

type KeycloakTokenResponse = {
  access_token: string;
  expires_in?: number;
  id_token?: string;
  refresh_expires_in?: number;
  refresh_token?: string;
  scope?: string;
  token_type?: string;
};

let refreshPromise: Promise<string | null> | null = null;

export async function getValidAccessToken() {
  if (isAuthLogoutInProgress()) return null;

  const accessToken = getAccessToken();
  if (!accessToken) return null;

  if (!isAccessTokenExpiringWithin(90)) {
    return accessToken;
  }

  return refreshAccessToken();
}

export async function refreshAccessToken() {
  if (isAuthLogoutInProgress()) return null;
  if (refreshPromise) return refreshPromise;

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearAuthTokens();
    return null;
  }

  refreshPromise = requestTokenRefresh(refreshToken)
    .then((tokenResponse) => {
      if (isAuthLogoutInProgress()) return null;

      saveRefreshedAuthTokens({
        accessToken: tokenResponse.access_token,
        refreshToken: tokenResponse.refresh_token ?? refreshToken,
        idToken: tokenResponse.id_token,
      });
      return tokenResponse.access_token;
    })
    .catch((error) => {
      clearAuthTokens();
      throw error;
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

async function requestTokenRefresh(refreshToken: string) {
  const loginUrl = new URL(appConfig.keycloakLoginUrl);
  const tokenUrl = new URL(loginUrl.toString());
  tokenUrl.pathname = tokenUrl.pathname.replace(/\/auth$/, "/token");

  const formData = new URLSearchParams();
  formData.set("grant_type", "refresh_token");
  formData.set("client_id", loginUrl.searchParams.get("client_id") ?? "vehicle-management-frontend");
  formData.set("refresh_token", refreshToken);

  const response = await fetch(tokenUrl.toString(), {
    body: formData,
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    method: "POST",
  });

  const responseBody = (await response.json()) as KeycloakTokenResponse & { error_description?: string };

  if (!response.ok) {
    throw new Error(responseBody.error_description ?? `Keycloak refresh token error ${response.status}`);
  }

  return responseBody;
}
