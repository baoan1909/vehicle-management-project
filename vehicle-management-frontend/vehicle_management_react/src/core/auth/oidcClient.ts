import { UserManager, WebStorageStateStore, type UserManagerSettings } from "oidc-client-ts";
import { appConfig } from "@/config/env";

export const oidcSettings: UserManagerSettings = {
  authority: appConfig.oidcAuthority,
  client_id: appConfig.oidcClientId,
  redirect_uri: appConfig.oidcRedirectUri,
  post_logout_redirect_uri: appConfig.oidcPostLogoutRedirectUri,
  response_type: "code",
  scope: appConfig.oidcScope,
  loadUserInfo: false,
  disablePKCE: false,
  automaticSilentRenew: false,
  accessTokenExpiringNotificationTimeInSeconds: 90,
  includeIdTokenInSilentRenew: true,
  monitorSession: false,
  revokeTokensOnSignout: false,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
};

export const oidcUserManager = new UserManager(oidcSettings);
