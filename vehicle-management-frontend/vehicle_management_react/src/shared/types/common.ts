import type { ReactNode } from "react";

export type AppLayout = "admin" | "client" | "auth";

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
  role: "ADMIN" | "EMPLOYEE" | "CUSTOMER";
  avatarUrl: string;
}

export type AdminSidebarIcon = "dashboard" | "swipe" | "card" | "catalog" | "pricing" | "members" | "role";

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
