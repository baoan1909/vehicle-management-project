import type { ReactNode } from "react";

export type AppLayout = "admin" | "client" | "auth";

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

export interface NavItem {
  label: string;
  icon: string;
  href?: string;
  match: string[];
  children?: NavItem[];
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
