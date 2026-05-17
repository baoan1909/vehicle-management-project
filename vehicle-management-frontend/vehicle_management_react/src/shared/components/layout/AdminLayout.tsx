import type { PropsWithChildren } from "react";
import { AdminHeader } from "./AdminHeader";
import { AdminSidebar } from "./AdminSidebar";

interface AdminLayoutProps extends PropsWithChildren {
}

export function AdminLayout({ children }: AdminLayoutProps) {
  return (
    <div className="wrapper vm-admin-shell">
      <AdminHeader />
      <AdminSidebar />
      <div className="content-wrapper">{children}</div>
    </div>
  );
}
