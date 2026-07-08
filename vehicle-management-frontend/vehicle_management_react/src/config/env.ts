type AppConfig = {
  appName: string;
  apiBaseUrl: string;
  assetBaseUrl: string;
  keycloakLoginUrl: string;
};

export const appConfig: AppConfig = {
  appName: import.meta.env.VITE_APP_NAME ?? "Vehicle Management",
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? "/api",
  assetBaseUrl: import.meta.env.VITE_ASSET_BASE_URL ?? "/assets",
  keycloakLoginUrl: import.meta.env.VITE_KEYCLOAK_LOGIN_URL ?? "",
};
