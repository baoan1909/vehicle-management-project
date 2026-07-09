import type { AccountProfileStatusResponse } from "@/features/iam/api/accountProfileApi";
import { DEFAULT_USER_AVATAR_URL, getRoleLabel } from "@/shared/utils/accountStatus";
import type { CurrentUser } from "@/shared/types/common";

export function mergeCurrentUserWithAccountProfile(currentUser: CurrentUser, profile: AccountProfileStatusResponse): CurrentUser {
  const username = profile.account?.username?.trim() || currentUser.username?.trim() || currentUser.email?.trim() || "user";
  const fullName = profile.profile?.fullName?.trim() || currentUser.fullName?.trim() || username;
  const avatarUrl = profile.profile?.avatarUrl?.trim() || currentUser.avatarUrl || DEFAULT_USER_AVATAR_URL;
  const role = resolveProfileRole(profile.account?.roleCode) ?? inferRoleFromProfile(profile) ?? currentUser.role;

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
    const jobTitle = profile.employee?.jobTitle?.trim().toUpperCase();
    return jobTitle === "PARKING MANAGER" ? "PARKING_MANAGER" : "EMPLOYEE";
  }

  return null;
}

function resolveProfileRole(roleCode?: string): CurrentUser["role"] | null {
  const normalizedRole = roleCode?.replace(/^ROLE_/, "").trim().toUpperCase();

  switch (normalizedRole) {
    case "SYSTEM_ADMIN":
    case "PARKING_MANAGER":
    case "EMPLOYEE":
    case "CUSTOMER":
      return normalizedRole;
    default:
      return null;
  }
}
