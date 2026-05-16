import type { PropsWithChildren } from "react";
import { AdminNavbar } from "./AdminNavbar";
import { AdminSidebar } from "./AdminSidebar";

interface AdminLayoutProps extends PropsWithChildren {
  currentPath: string;
}

export function AdminLayout({ children, currentPath }: AdminLayoutProps) {
  return (
    <div className="wrapper">
      <AdminNavbar />
      <AdminSidebar currentPath={currentPath} />
      <div className="content-wrapper">{children}</div>
    </div>
  );
}
