import type { ReactNode } from "react";

export type AppLayout = "admin" | "client" | "auth" | "fullscreen";

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

export interface TableColumn<T> {
  key: string;
  label: string;
  width?: string;
  className?: string;
  render?: (row: T, index: number) => ReactNode;
}

export interface SelectOption {
  label: string;
  value: string;
}

export interface CurrentUser {
  id: string;
  username: string;
  fullName: string;
  role: "SYSTEM_ADMIN" | "PARKING_MANAGER" | "EMPLOYEE" | "CUSTOMER" | "UNKNOWN";
  avatarUrl: string;
  accountStatus?: string;
  customerApprovalStatus?: string;
  customerStatus?: string;
  email?: string;
  employeeStatus?: string;
  jobTitle?: string;
  onboardingRequired?: boolean;
  profileStatus?: string;
  roleLabel?: string;
}

export type AdminSidebarIcon = "dashboard" | "swipe" | "card" | "catalog" | "parking" | "pricing" | "members" | "role" | "settings" | "support";

export interface AdminSidebarLeaf {
  label: string;
  to: string;
  matches: string[];
}

export type AdminSidebarEntry =
  | {
      kind: "link";
      label: string;
      to: string;
      matches: string[];
      icon: AdminSidebarIcon;
    }
  | {
      kind: "group";
      label: string;
      icon: AdminSidebarIcon;
      items: AdminSidebarLeaf[];
      defaultExpanded?: boolean;
    }
  | {
      kind: "divider";
    };
