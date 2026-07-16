import type { AccountProfileStatusResponse } from "@/features/iam/api/accountProfileApi";
import { DEFAULT_USER_AVATAR_URL, getRoleLabel } from "@/shared/utils/accountStatus";
import type { CurrentUser } from "@/shared/types/common";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

export function mergeCurrentUserWithAccountProfile(currentUser: CurrentUser, profile: AccountProfileStatusResponse): CurrentUser {
  const username = profile.account?.username?.trim() || currentUser.username?.trim() || currentUser.email?.trim() || "user";
  const fullName = profile.profile?.fullName?.trim() || currentUser.fullName?.trim() || username;
  const avatarUrl = resolvePublicMediaUrl(profile.profile?.avatarUrl) || resolvePublicMediaUrl(currentUser.avatarUrl) || DEFAULT_USER_AVATAR_URL;
  const role = resolveProfileRole(profile.account?.roleCode) ?? resolveProfileRole(profile.account?.roleName) ?? inferRoleFromProfile(profile) ?? currentUser.role;

  return {
    ...currentUser,
    accountStatus: profile.account?.accountStatus ?? currentUser.accountStatus,
    avatarUrl,
    customerApprovalStatus: profile.customer?.customerApprovalStatus ?? currentUser.customerApprovalStatus,
    customerStatus: profile.customer?.customerStatus ?? currentUser.customerStatus,
    email: profile.account?.email ?? currentUser.email,
    employeeStatus: profile.employee?.employeeStatus ?? currentUser.employeeStatus,
    fullName,
    id: profile.account?.accountId ?? currentUser.id,
    jobTitle: profile.employee?.jobTitle ?? currentUser.jobTitle,
    onboardingRequired: profile.onboardingRequired,
    profileStatus: profile.profile?.userProfileStatus ?? currentUser.profileStatus,
    role,
    roleLabel: getRoleLabel(role),
    username
  };
}

function inferRoleFromProfile(profile: AccountProfileStatusResponse): CurrentUser["role"] | null {
  if (profile.customer?.customerId || profile.customer?.customerCode || profile.customer?.customerStatus || profile.customer?.customerApprovalStatus) {
    return "CUSTOMER";
  }

  if (profile.employee?.employeeId || profile.employee?.employeeCode || profile.employee?.employeeStatus || profile.employee?.jobTitle) {
    const jobTitle = normalizeRoleText(profile.employee?.jobTitle);
    if (jobTitle === "SYSTEM ADMIN" || jobTitle === "QUAN TRI HE THONG" || jobTitle === "QUAN TRI") {
      return "SYSTEM_ADMIN";
    }
    if (jobTitle === "PARKING MANAGER" || jobTitle === "MANAGER" || jobTitle === "QUAN LY") {
      return "PARKING_MANAGER";
    }
    return "EMPLOYEE";
  }

  return null;
}

function resolveProfileRole(roleCode?: string): CurrentUser["role"] | null {
  const normalizedRole = normalizeRoleText(roleCode).replace(/^ROLE_/, "").replace(/\s+/g, "_");

  switch (normalizedRole) {
    case "SYSTEM_ADMIN":
    case "QUAN_TRI":
    case "QUAN_TRI_HE_THONG":
      return "SYSTEM_ADMIN";
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
