import type { AccountProfileStatusResponse } from "@/features/iam/api/accountProfileApi";
import type { CurrentAccountAccessResponse } from "@/features/iam/api/currentAccountAccessApi";
import { normalizePermissionCodes } from "@/shared/auth/permissions";
import { DEFAULT_USER_AVATAR_URL, getRoleLabel } from "@/shared/utils/accountStatus";
import type { CurrentUser } from "@/shared/types/common";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

/**
 * Merges onboarding/profile data only. Authorization fields stay owned by
 * current-access; a profile reload must never restore a stale role, status,
 * permission list, access state, or revision.
 */
export function mergeProfileDetails(currentUser: CurrentUser, profile: AccountProfileStatusResponse): CurrentUser {
  const username = currentUser.username?.trim() || currentUser.email?.trim() || "user";
  const fullName = profile.profile?.fullName?.trim() || currentUser.fullName?.trim() || username;
  const avatarUrl = resolvePublicMediaUrl(profile.profile?.avatarUrl) || resolvePublicMediaUrl(currentUser.avatarUrl) || DEFAULT_USER_AVATAR_URL;

  return {
    ...currentUser,
    addressDisplay: profile.profile?.addressDisplay ?? currentUser.addressDisplay,
    avatarUrl,
    dateOfBirth: profile.profile?.dateOfBirth ?? currentUser.dateOfBirth,
    fullName,
    gender: profile.profile?.gender ?? currentUser.gender,
    identifyCard: profile.profile?.identifyCard ?? currentUser.identifyCard,
    jobTitle: profile.employee?.jobTitle ?? currentUser.jobTitle,
    phoneNumber: profile.profile?.phoneNumber ?? currentUser.phoneNumber,
    profileStatus: profile.profile?.userProfileStatus ?? currentUser.profileStatus,
    username,
  };
}

/**
 * Replaces the authorization snapshot atomically from the authoritative
 * current-access endpoint. No field falls back to token, cache, or onboarding.
 */
export function mergeAuthorizationSnapshot(currentUser: CurrentUser, access: CurrentAccountAccessResponse): CurrentUser {
  const username = access.username?.trim() || currentUser.username?.trim() || currentUser.email?.trim() || "user";
  const roleCode = access.roleCode.trim();
  const role = resolveProfileRole(roleCode) ?? "UNKNOWN";

  return {
    ...currentUser,
    accountStatus: access.accountStatus,
    customerApprovalStatus: access.customerApprovalStatus ?? undefined,
    customerStatus: access.customerStatus ?? undefined,
    email: access.email ?? currentUser.email,
    employeeStatus: access.employeeStatus ?? undefined,
    id: access.accountId,
    permissionCodes: normalizePermissionCodes(access.permissionCodes),
    onboardingRequired: access.onboardingRequired,
    accessState: access.accessState,
    requiredAction: access.requiredAction ?? null,
    accessRevision: access.accessRevision,
    role,
    roleCode,
    roleLabel: role === "UNKNOWN" ? formatRoleCode(roleCode) : getRoleLabel(role),
    username,
  };
}

function formatRoleCode(roleCode: string) {
  return roleCode
    .replace(/^ROLE_/, "")
    .replace(/_/g, " ")
    .trim()
    .toUpperCase();
}

function resolveProfileRole(roleCode?: string): CurrentUser["role"] | null {
  const normalizedRole = normalizeRoleText(roleCode).replace(/^ROLE_/, "").replace(/\s+/g, "_");

  switch (normalizedRole) {
    case "SYSTEM_ADMIN":
    case "QUAN_TRI":
    case "QUAN_TRI_HE_THONG":
      return "SYSTEM_ADMIN";
    case "PARTNER_ADMIN":
    case "QUAN_TRI_DOI_TAC":
      return "PARTNER_ADMIN";
    case "PARKING_MANAGER":
    case "QUAN_LY":
      return "PARKING_MANAGER";
    case "EMPLOYEE":
    case "NHAN_VIEN":
      return "EMPLOYEE";
    case "CUSTOMER":
    case "KHACH_HANG":
      return "CUSTOMER";
    default:
      return null;
  }
}

function normalizeRoleText(value?: string | null) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toUpperCase();
}
