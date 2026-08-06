import { adminNavigation } from "@/config/navigation";
import { hasAnyPermission } from "@/shared/auth/permissions";
import type { AdminSidebarEntry, CurrentUser } from "@/shared/types/common";

export const adminRoutePermissions = {
  "/admin/dashboard": ["DASHBOARD_READ_ALL"],
  "/api/dashboard/overview": ["DASHBOARD_READ_ALL"],
  "/admin/swipe": ["PARKING_SESSION_CHECK_IN_ALL", "PARKING_SESSION_CHECK_OUT_ALL"],
  "/admin/swipe/swipein": ["PARKING_SESSION_CHECK_IN_ALL"],
  "/admin/swipe/swipeout": ["PARKING_SESSION_CHECK_OUT_ALL"],
  "/admin/swipe/sessions": ["CARD_READ_ALL", "PARKING_SESSION_CHECK_IN_ALL", "PARKING_SESSION_CHECK_OUT_ALL"],
  "/admin/card": ["CARD_READ_ALL"],
  "/admin/card/form": ["CARD_CREATE_ALL", "CARD_UPDATE_ALL"],
  "/admin/lost": ["LOST_CARD_REPORT_READ_ALL"],
  "/admin/lost/form": ["LOST_CARD_REPORT_CREATE_ALL"],
  "/admin/lost/detail": ["LOST_CARD_REPORT_READ_ALL"],
  "/admin/ticket": ["TICKET_TYPE_READ_ALL"],
  "/admin/ticket/form": ["TICKET_TYPE_CREATE_ALL", "TICKET_TYPE_UPDATE_ALL"],
  "/admin/subscription-approvals": [
    "SUBSCRIPTION_READ_ALL",
    "SUBSCRIPTION_APPROVE_ALL",
    "SUBSCRIPTION_REJECT_ALL",
    "SUBSCRIPTION_ASSIGN_CARD_ALL",
  ],
  "/admin/vehicle": ["VEHICLE_TYPE_READ_ALL"],
  "/admin/vehicle/form": ["VEHICLE_TYPE_CREATE_ALL", "VEHICLE_TYPE_UPDATE_ALL"],
  "/admin/parking-lots": ["PARKING_SESSION_READ_ALL", "PARKING_SESSION_CHECK_IN_ALL", "PARKING_SESSION_CHECK_OUT_ALL"],
  "/admin/devices": ["DEVICE_READ_ALL"],
  "/admin/price-plans": ["PRICE_PLAN_READ_ALL"],
  "/admin/price-rules": ["PRICE_RULE_READ_ALL"],
  "/admin/invoices": ["INVOICE_READ_ALL"],
  "/admin/visitorParkingFee": ["PRICE_RULE_READ_ALL"],
  "/admin/parkingFeeOfCustomer": ["PRICE_RULE_READ_ALL"],
  "/admin/employee": ["EMPLOYEE_READ_ALL"],
  "/admin/employee/form": ["EMPLOYEE_CREATE_ALL", "EMPLOYEE_UPDATE_ALL"],
  "/admin/shifts": ["SHIFT_READ_ALL", "SHIFT_ASSIGNMENT_READ_ALL"],
  "/admin/account": ["ACCOUNT_READ_ALL"],
  "/admin/account/form": ["ACCOUNT_CREATE_ALL", "ACCOUNT_UPDATE_ALL"],
  "/admin/onboarding-approvals": ["ACCOUNT_READ_ALL"],
  "/admin/profile": [],
  "/admin/customer": ["CUSTOMER_READ_ALL"],
  "/admin/customer/form": ["CUSTOMER_CREATE_ALL", "CUSTOMER_UPDATE_ALL"],
  "/admin/role": ["ROLE_READ_ALL"],
  "/admin/role/form": ["ROLE_CREATE_ALL", "ROLE_UPDATE_ALL"],
  "/admin/support-categories": ["SUPPORT_TICKET_READ_ALL", "SUPPORT_TICKET_ASSIGN", "SUPPORT_TICKET_PROCESS_ALL"],
  "/admin/support-center": [
    "CHAT_CONVERSATION_READ_OWN",
    "CHAT_CONVERSATION_READ_ALL",
  ],
} satisfies Record<string, string[]>;

const adminFallbackRoute = "/admin/profile";
const unmappedAdminRoutePermission = "__UNMAPPED_ADMIN_ROUTE__";

export function getRoutePermissions(path: string) {
  const permissions = adminRoutePermissions[path as keyof typeof adminRoutePermissions];
  if (permissions) return permissions;
  return path.startsWith("/admin/") || path.startsWith("/api/") ? [unmappedAdminRoutePermission] : [];
}

export function canAccessAdminRoute(user: CurrentUser | null | undefined, path: string) {
  return hasAnyPermission(user, getRoutePermissions(path));
}

export function getVisibleAdminNavigation(user: CurrentUser | null | undefined) {
  const entries: AdminSidebarEntry[] = [];

  adminNavigation.forEach((entry) => {
    if (entry.kind === "divider") {
      if (entries.length > 0 && entries.at(-1)?.kind !== "divider") {
        entries.push(entry);
      }
      return;
    }

    if (entry.kind === "group") {
      const items = entry.items.filter((item) => canAccessAdminRoute(user, item.to));
      if (items.length > 0) {
        entries.push({ ...entry, items });
      }
      return;
    }

    if (canAccessAdminRoute(user, entry.to)) {
      entries.push(entry);
    }
  });

  while (entries.at(-1)?.kind === "divider") {
    entries.pop();
  }

  return entries;
}

export function getFirstAccessibleAdminPath(user: CurrentUser | null | undefined) {
  for (const entry of getVisibleAdminNavigation(user)) {
    if (entry.kind === "link") {
      return entry.to;
    }

    if (entry.kind === "group") {
      return entry.items[0]?.to ?? adminFallbackRoute;
    }
  }

  return adminFallbackRoute;
}
