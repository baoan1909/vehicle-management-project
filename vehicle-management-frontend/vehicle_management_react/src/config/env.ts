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
  keycloakLoginUrl:
    import.meta.env.VITE_KEYCLOAK_LOGIN_URL ??
    "http://localhost:8081/realms/vehicle-management/protocol/openid-connect/auth?client_id=vehicle-management-frontend&redirect_uri=http%3A%2F%2Flocalhost%3A5173%2Flogin&response_type=code&scope=openid%20profile%20email%20roles",
};
