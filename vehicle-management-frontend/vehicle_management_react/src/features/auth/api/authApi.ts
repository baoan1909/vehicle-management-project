import { appConfig } from "@/config/env";
import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

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

let keycloakPreconnectInitialized = false;

export function registerAccount(payload: RegisterAccountRequest) {
  return apiClient<ApiResponse<RegisterAccountResponse>>(apiEndpoints.auth.register, {
    method: "POST",
    body: payload,
    skipAuth: true,
  });
}

export function requestPasswordReset(payload: ForgotPasswordRequest) {
  return apiClient<ApiResponse<null>>(apiEndpoints.auth.forgotPassword, {
    method: "POST",
    body: payload,
    skipAuth: true,
  });
}

export function resendVerificationEmail(payload: ResendVerificationEmailRequest) {
  return apiClient<ApiResponse<null>>(apiEndpoints.auth.resendVerificationEmail, {
    method: "POST",
    body: payload,
    skipAuth: true,
  });
}

export function preconnectKeycloakLoginOrigin() {
  if (keycloakPreconnectInitialized) return;
  keycloakPreconnectInitialized = true;

  try {
    const authorityUrl = new URL(appConfig.oidcAuthority);
    if (authorityUrl.origin === window.location.origin) return;

    const addResourceHint = (rel: "dns-prefetch" | "preconnect") => {
      const link = document.createElement("link");
      link.rel = rel;
      link.href = authorityUrl.origin;
      if (rel === "preconnect") link.crossOrigin = "anonymous";
      document.head.appendChild(link);
    };

    addResourceHint("dns-prefetch");
    addResourceHint("preconnect");
  } catch {
    keycloakPreconnectInitialized = false;
  }
}
