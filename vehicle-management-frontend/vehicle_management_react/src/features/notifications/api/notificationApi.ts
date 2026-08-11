import { apiClient } from "@/core/api/apiClient";
import { apiEndpoints } from "@/core/api/apiEndpoints";

type ApiResponse<T> = {
  data: T;
  message: string;
  success: boolean;
  timestamp: string;
};

export type NotificationType =
  | "SYSTEM_NOTICE"
  | "SUBSCRIPTION_REQUESTED"
  | "SUBSCRIPTION_APPROVED"
  | "SUBSCRIPTION_REJECTED"
  | "SUBSCRIPTION_EXPIRING_SOON"
  | "SUBSCRIPTION_EXPIRED"
  | "SUBSCRIPTION_CANCELLED"
  | "SUBSCRIPTION_PAYMENT_COMPLETED"
  | "INVOICE_CREATED"
  | "PAYMENT_SUCCEEDED"
  | "PAYMENT_FAILED"
  | "SUPPORT_TICKET_CREATED"
  | "SUPPORT_TICKET_ASSIGNED"
  | "SUPPORT_TICKET_IN_PROGRESS"
  | "SUPPORT_TICKET_RESPONDED"
  | "SUPPORT_TICKET_REOPENED"
  | "SUPPORT_TICKET_CLOSED"
  | "SHIFT_ASSIGNED"
  | "SHIFT_CHANGED"
  | "SHIFT_CANCELLED"
  | "DEVICE_OFFLINE"
  | "DEVICE_MAINTENANCE"
  | "LANE_MAINTENANCE"
  | "PARKING_LOT_MAINTENANCE"
  | "PRICE_PLAN_CHANGED"
  | "PRICE_RULE_CHANGED"
  | "TICKET_TYPE_CHANGED"
  | "ACCOUNT_REGISTERED"
  | "ACCOUNT_PROVISIONED"
  | "ACCOUNT_STATUS_CHANGED"
  | "ACCOUNT_PROFILE_SUBMITTED"
  | "CUSTOMER_ONBOARDING_APPROVED"
  | "CUSTOMER_ONBOARDING_REJECTED"
  | "CUSTOMER_ONBOARDING_RESUBMITTED"
  | "INTERNAL_EMPLOYEE_APPROVED"
  | "INTERNAL_EMPLOYEE_REJECTED"
  | "INTERNAL_EMPLOYEE_RESUBMITTED"
  | "SYSTEM_ADMIN_APPROVED"
  | "SYSTEM_ADMIN_REJECTED"
  | "SYSTEM_ADMIN_RESUBMITTED";

export type NotificationStatus = "PENDING" | "SENT" | "READ" | "FAILED";
export type NotificationChannel = "WEB" | "EMAIL" | "PUSH" | "SMS";

export type NotificationUserResponse = {
  notificationId: string;
  broadcastId: string | null;
  channel: NotificationChannel;
  notificationType: NotificationType;
  title: string;
  message: string;
  status: NotificationStatus;
  sentAt: string | null;
  readAt: string | null;
  redirectUrl: string | null;
  relatedSchema: string | null;
  relatedTable: string | null;
  relatedId: string | null;
  createdAt: string | null;
};

export type NotificationUnreadCountResponse = {
  unreadCount: number;
};

export type NotificationActiveRoleResponse = {
  code: string;
  name: string;
  roleId: string;
};

export type BroadcastAnnouncementAudienceType = "ALL_ACTIVE_ACCOUNTS" | "ROLE_CODES";
export type BroadcastAnnouncementStatus = "DRAFT" | "PUBLISHED" | "CANCELLED";

export type BroadcastAnnouncementResponse = {
  broadcastId: string;
  notificationType: NotificationType;
  title: string;
  message: string;
  audienceType: BroadcastAnnouncementAudienceType;
  roleCodes: string[] | null;
  startAt: string | null;
  endAt: string | null;
  displayOrder: number | null;
  enabled: boolean | null;
  redirectUrl: string | null;
  status: BroadcastAnnouncementStatus;
  publishedAt: string | null;
  cancelledAt: string | null;
  relatedSchema: string | null;
  relatedTable: string | null;
  relatedId: string | null;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
};

export type BroadcastAnnouncementPayload = {
  notificationType: NotificationType;
  title: string;
  message: string;
  audienceType: BroadcastAnnouncementAudienceType;
  roleCodes: string[];
  startAt: string;
  endAt?: string | null;
  displayOrder?: number | null;
  enabled: boolean;
  redirectUrl?: string | null;
  relatedSchema?: string | null;
  relatedTable?: string | null;
  relatedId?: string | null;
};

export type NotificationFilter = {
  beforeCreatedAt?: string | null;
  limit?: number;
  unreadOnly?: boolean;
};

export type BroadcastAnnouncementFilter = {
  status?: BroadcastAnnouncementStatus | null;
};

function buildQuery(filter: Record<string, string | number | boolean | null | undefined>) {
  const params = new URLSearchParams();

  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, String(value));
    }
  });

  const query = params.toString();
  return query ? `?${query}` : "";
}

export function getMyNotifications(filter: NotificationFilter = {}) {
  return apiClient<ApiResponse<NotificationUserResponse[]>>(
    `${apiEndpoints.notifications.mine}${buildQuery(filter)}`,
  );
}

export function countMyUnreadNotifications() {
  return apiClient<ApiResponse<NotificationUnreadCountResponse>>(apiEndpoints.notifications.unreadCount);
}

export function markNotificationRead(notificationId: string) {
  return apiClient<ApiResponse<NotificationUserResponse>>(
    `${apiEndpoints.notifications.mine}/${notificationId}/read`,
    { method: "PATCH" },
  );
}

export function markAllNotificationsRead() {
  return apiClient<ApiResponse<void>>(apiEndpoints.notifications.readAll, { method: "PATCH" });
}

export function getNotificationActiveRoles() {
  return apiClient<ApiResponse<NotificationActiveRoleResponse[]>>(apiEndpoints.notifications.activeRoles);
}

export function getBroadcastAnnouncements(filter: BroadcastAnnouncementFilter = {}) {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse[]>>(
    `${apiEndpoints.notifications.broadcastAnnouncements}${buildQuery(filter)}`,
  );
}

export function getActiveBroadcastAnnouncements() {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse[]>>(
    apiEndpoints.notifications.activeBroadcastAnnouncements,
  );
}

export function createBroadcastAnnouncement(payload: BroadcastAnnouncementPayload) {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse>>(apiEndpoints.notifications.broadcastAnnouncements, {
    body: payload,
    method: "POST",
  });
}

export function updateBroadcastAnnouncement(broadcastId: string, payload: BroadcastAnnouncementPayload) {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse>>(
    `${apiEndpoints.notifications.broadcastAnnouncements}/${broadcastId}`,
    {
      body: payload,
      method: "PUT",
    },
  );
}

export function updateBroadcastAnnouncementDisplayOrder(broadcastId: string, displayOrder: number) {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse>>(
    `${apiEndpoints.notifications.broadcastAnnouncements}/${broadcastId}/display-order`,
    {
      body: { displayOrder },
      method: "PATCH",
    },
  );
}

export function publishBroadcastAnnouncement(broadcastId: string) {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse>>(
    `${apiEndpoints.notifications.broadcastAnnouncements}/${broadcastId}/publish`,
    { method: "PATCH" },
  );
}

export function cancelBroadcastAnnouncement(broadcastId: string) {
  return apiClient<ApiResponse<BroadcastAnnouncementResponse>>(
    `${apiEndpoints.notifications.broadcastAnnouncements}/${broadcastId}/cancel`,
    { method: "PATCH" },
  );
}

export function deleteBroadcastAnnouncement(broadcastId: string) {
  return apiClient<ApiResponse<void>>(
    `${apiEndpoints.notifications.broadcastAnnouncements}/${broadcastId}`,
    { method: "DELETE" },
  );
}
