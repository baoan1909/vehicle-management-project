import { apiClient } from "@/core/api/apiClient";
import { ApiError } from "@/core/api/apiError";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type CurrentAccountAccessResponse = {
  accountId: string;
  accountStatus: string;
  customerApprovalStatus?: string | null;
  customerStatus?: string | null;
  email?: string | null;
  employeeStatus?: string | null;
  permissionCodes: string[];
  roleCode: string;
  roleId?: string | null;
  username?: string | null;
  onboardingRequired: boolean;
  accessState: AccessState;
  requiredAction?: RequiredAction | null;
  accessRevision: string;
};

export type AccessState =
  | "ACTIVE"
  | "ONBOARDING_REQUIRED"
  | "PENDING_APPROVAL"
  | "ACCOUNT_PENDING"
  | "ACCOUNT_LOCKED"
  | "ACCOUNT_DISABLED"
  | "ROLE_DISABLED"
  | "CUSTOMER_INACTIVE"
  | "CUSTOMER_APPROVAL_REQUIRED"
  | "EMPLOYEE_INACTIVE"
  | "ORGANIZATION_ACCESS_REQUIRED"
  | "NO_EFFECTIVE_PERMISSIONS";

export type RequiredAction =
  | "VERIFY_EMAIL"
  | "COMPLETE_PROFILE"
  | "SUBMIT_FOR_APPROVAL"
  | "WAIT_FOR_APPROVAL"
  | "CONTACT_ADMIN"
  | "CONTACT_ORGANIZATION_ADMIN";

const ACCESS_STATES = new Set<string>([
  "ACTIVE",
  "ONBOARDING_REQUIRED",
  "PENDING_APPROVAL",
  "ACCOUNT_PENDING",
  "ACCOUNT_LOCKED",
  "ACCOUNT_DISABLED",
  "ROLE_DISABLED",
  "CUSTOMER_INACTIVE",
  "CUSTOMER_APPROVAL_REQUIRED",
  "EMPLOYEE_INACTIVE",
  "ORGANIZATION_ACCESS_REQUIRED",
  "NO_EFFECTIVE_PERMISSIONS",
] satisfies AccessState[]);

const REQUIRED_ACTIONS = new Set<string>([
  "VERIFY_EMAIL",
  "COMPLETE_PROFILE",
  "SUBMIT_FOR_APPROVAL",
  "WAIT_FOR_APPROVAL",
  "CONTACT_ADMIN",
  "CONTACT_ORGANIZATION_ADMIN",
] satisfies RequiredAction[]);

export async function getMyAccountAccess(
  options: { signal?: AbortSignal; timeoutMs?: number } = {},
) {
  const response = await apiClient<ApiResponse<CurrentAccountAccessResponse>>(
    apiEndpoints.iam.accountProfile.currentAccess,
    {
      signal: options.signal,
      timeoutMs: options.timeoutMs,
    },
  );

  if (!isValidCurrentAccessEnvelope(response)) {
    throw new ApiError("Máy chủ trả về dữ liệu quyền truy cập không hợp lệ.", {
      kind: "invalid-response",
    });
  }

  return response;
}

function isValidCurrentAccessEnvelope(value: unknown): value is ApiResponse<CurrentAccountAccessResponse> {
  if (!isRecord(value) || value.success !== true || !isRecord(value.data)) return false;

  const data = value.data;
  if (!isNonEmptyString(data.accountId)) return false;
  if (!isNonEmptyString(data.accountStatus)) return false;
  if (!isNonEmptyString(data.roleCode)) return false;
  if (!isNonEmptyString(data.accessRevision)) return false;
  if (!Array.isArray(data.permissionCodes)) return false;
  if (!data.permissionCodes.every((permissionCode) => typeof permissionCode === "string")) return false;
  if (typeof data.onboardingRequired !== "boolean") return false;
  if (!isNonEmptyString(data.accessState) || !ACCESS_STATES.has(data.accessState)) return false;
  if (data.requiredAction !== undefined && data.requiredAction !== null) {
    if (!isNonEmptyString(data.requiredAction) || !REQUIRED_ACTIONS.has(data.requiredAction)) return false;
  }

  return isOptionalNullableString(data.customerApprovalStatus)
    && isOptionalNullableString(data.customerStatus)
    && isOptionalNullableString(data.email)
    && isOptionalNullableString(data.employeeStatus)
    && isOptionalNullableString(data.roleId)
    && isOptionalNullableString(data.username);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

function isOptionalNullableString(value: unknown) {
  return value === undefined || value === null || typeof value === "string";
}
