import type { NotificationUserResponse } from "@/features/notifications/api/notificationApi";

export type AccountStateInvalidation = {
  refreshAccess: boolean;
  refreshApproval: boolean;
  refreshAvatar: boolean;
  refreshOrganizationScope: boolean;
  refreshProfile: boolean;
};

export const noAccountStateInvalidation: AccountStateInvalidation = {
  refreshAccess: false,
  refreshApproval: false,
  refreshAvatar: false,
  refreshOrganizationScope: false,
  refreshProfile: false,
};

const ACCESS_AND_PROFILE_TYPES = new Set([
  "ACCOUNT_ACCESS_CHANGED",
  "ACCOUNT_STATUS_CHANGED",
  "CUSTOMER_ONBOARDING_APPROVED",
  "CUSTOMER_ONBOARDING_REJECTED",
  "CUSTOMER_ONBOARDING_RESUBMITTED",
  "INTERNAL_EMPLOYEE_APPROVED",
  "INTERNAL_EMPLOYEE_REJECTED",
  "INTERNAL_EMPLOYEE_RESUBMITTED",
  "SYSTEM_ADMIN_APPROVED",
  "SYSTEM_ADMIN_REJECTED",
  "SYSTEM_ADMIN_RESUBMITTED",
]);

const AVATAR_TYPES = new Set(["AVATAR_APPROVED", "AVATAR_REJECTED"]);

function isPartnerReviewOutcome(notification: NotificationUserResponse) {
  return notification.notificationType === "SYSTEM_NOTICE"
    && notification.redirectUrl === "/admin/profile"
    && notification.relatedSchema === "operations"
    && notification.relatedTable === "approval_requests";
}

/**
 * Maps a persisted notification to reconciliation work. The notification is
 * only an invalidation signal; none of its status or permission data is trusted.
 */
export function classifyAccountStateInvalidation(
  notification: NotificationUserResponse,
): AccountStateInvalidation {
  if (AVATAR_TYPES.has(notification.notificationType)) {
    return {
      ...noAccountStateInvalidation,
      refreshAvatar: true,
      refreshProfile: true,
    };
  }

  if (ACCESS_AND_PROFILE_TYPES.has(notification.notificationType) || isPartnerReviewOutcome(notification)) {
    return {
      refreshAccess: true,
      refreshApproval: true,
      refreshAvatar: false,
      refreshOrganizationScope: notification.notificationType === "ACCOUNT_ACCESS_CHANGED"
        || isPartnerReviewOutcome(notification),
      refreshProfile: true,
    };
  }

  return noAccountStateInvalidation;
}

export function hasAccountStateInvalidation(invalidation: AccountStateInvalidation) {
  return Object.values(invalidation).some(Boolean);
}

export function mergeAccountStateInvalidation(
  left: AccountStateInvalidation,
  right: AccountStateInvalidation,
): AccountStateInvalidation {
  return {
    refreshAccess: left.refreshAccess || right.refreshAccess,
    refreshApproval: left.refreshApproval || right.refreshApproval,
    refreshAvatar: left.refreshAvatar || right.refreshAvatar,
    refreshOrganizationScope: left.refreshOrganizationScope || right.refreshOrganizationScope,
    refreshProfile: left.refreshProfile || right.refreshProfile,
  };
}
