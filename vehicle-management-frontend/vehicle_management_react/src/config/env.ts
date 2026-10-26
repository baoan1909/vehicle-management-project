const LEGACY_LOCAL_API_BASE_URLS = new Set([
  "http://localhost:8080/vehicle-management/api",
  "http://127.0.0.1:8080/vehicle-management/api",
]);

function resolveApiBaseUrl() {
  const configuredUrl = import.meta.env.VITE_API_BASE_URL;
  if (import.meta.env.DEV && configuredUrl && LEGACY_LOCAL_API_BASE_URLS.has(configuredUrl)) {
    return "/vehicle-management/api";
  }
  return configuredUrl ?? (import.meta.env.DEV ? "/vehicle-management/api" : "/api");
}
type AppConfig = {
  appName: string;
  apiBaseUrl: string;
  assetBaseUrl: string;
  keycloakLoginUrl: string;
  oidcAuthority: string;
  oidcClientId: string;
  oidcPostLogoutRedirectUri: string;
  oidcRedirectUri: string;
  oidcScope: string;
  publicFileBaseUrl: string;
};

const defaultKeycloakLoginUrl =
  "http://localhost:8081/realms/vehicle-management/protocol/openid-connect/auth?client_id=vehicle-management-frontend&redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Flogin&response_type=code&scope=openid%20profile%20email%20roles";
const configuredKeycloakLoginUrl = import.meta.env.VITE_KEYCLOAK_LOGIN_URL ?? defaultKeycloakLoginUrl;
const keycloakLoginUrl = new URL(configuredKeycloakLoginUrl);
const frontendOrigin = typeof window === "undefined" ? "http://localhost:5173" : window.location.origin;
const inferredAuthority = keycloakLoginUrl.toString().replace(/\/protocol\/openid-connect\/auth.*$/, "").replace(/\/$/, "");

export const appConfig: AppConfig = {
  appName: import.meta.env.VITE_APP_NAME ?? "Vehicle Management",
  apiBaseUrl: resolveApiBaseUrl(),
  assetBaseUrl: import.meta.env.VITE_ASSET_BASE_URL ?? "/assets",
  publicFileBaseUrl: import.meta.env.VITE_PUBLIC_FILE_BASE_URL ?? "http://localhost:9000/vehicle-public",
  keycloakLoginUrl: configuredKeycloakLoginUrl,
  oidcAuthority: import.meta.env.VITE_OIDC_AUTHORITY ?? inferredAuthority,
  oidcClientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? keycloakLoginUrl.searchParams.get("client_id") ?? "vehicle-management-frontend",
  oidcRedirectUri: import.meta.env.VITE_OIDC_REDIRECT_URI ?? keycloakLoginUrl.searchParams.get("redirect_uri") ?? `${frontendOrigin}/login`,
  oidcPostLogoutRedirectUri: import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI ?? `${frontendOrigin}/`,
  oidcScope: import.meta.env.VITE_OIDC_SCOPE ?? "openid profile email roles",
};
