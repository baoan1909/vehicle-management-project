import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type OnboardingApprovalPolicies = {
  customerAutoApproveEnabled: boolean;
  customerEffectiveFrom?: string | null;
  partnerAutoApproveEnabled: boolean;
  partnerEffectiveFrom?: string | null;
  avatarAutoApproveEnabled: boolean;
  avatarEffectiveFrom?: string | null;
};

export type UpdateOnboardingApprovalPoliciesRequest = Pick<
  OnboardingApprovalPolicies,
  "customerAutoApproveEnabled" | "partnerAutoApproveEnabled" | "avatarAutoApproveEnabled"
>;

export async function getOnboardingApprovalPolicies() {
  const response = await apiClient<ApiResponse<OnboardingApprovalPolicies>>(
    apiEndpoints.operations.onboardingApprovalPolicies,
  );
  return response.data;
}

export async function updateOnboardingApprovalPolicies(
  request: UpdateOnboardingApprovalPoliciesRequest,
) {
  const response = await apiClient<ApiResponse<OnboardingApprovalPolicies>>(
    apiEndpoints.operations.onboardingApprovalPolicies,
    { body: request, method: "PUT" },
  );
  return response.data;
}
