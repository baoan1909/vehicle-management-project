import type { ReactNode } from "react";
import type { AccessState, RequiredAction } from "@/features/iam/api/currentAccountAccessApi";

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
  /** Presentation/workflow category only. Never use for authorization. */
  role: "SYSTEM_ADMIN" | "PARTNER_ADMIN" | "PARKING_MANAGER" | "EMPLOYEE" | "CUSTOMER" | "UNKNOWN";
  /** Authoritative role code from current-access. Custom codes are allowed. */
  roleCode?: string;
  avatarUrl: string;
  accountStatus?: string;
  addressDisplay?: string;
  customerApprovalStatus?: string;
  customerStatus?: string;
  dateOfBirth?: string;
  email?: string;
  employeeStatus?: string;
  gender?: string;
  identifyCard?: string;
  jobTitle?: string;
  onboardingRequired?: boolean;
  permissionCodes?: string[];
  phoneNumber?: string;
  profileStatus?: string;
  roleLabel?: string;
  accessState?: AccessState | "UNKNOWN";
  requiredAction?: RequiredAction | null;
  accessRevision?: string;
}

export type AdminSidebarIcon = "dashboard" | "swipe" | "card" | "catalog" | "parking" | "pricing" | "members" | "role" | "settings" | "support" | "notification";

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
      monitoring?: boolean;
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
    }
  | {
      kind: "section";
      label: string;
    }
  | {
      kind: "planned";
      label: string;
    };
