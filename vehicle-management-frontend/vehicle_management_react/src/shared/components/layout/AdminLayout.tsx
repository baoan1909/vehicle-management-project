import type { PropsWithChildren } from "react";
import { useState } from "react";
import { AdminHeader } from "./AdminHeader";
import { AdminSidebar } from "./AdminSidebar";
import { cn } from "@/lib/cn";

interface AdminLayoutProps extends PropsWithChildren {
}

export function AdminLayout({ children }: AdminLayoutProps) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className="wrapper tw-min-h-screen tw-bg-vm-canvas tw-text-vm-slate-700">
      <AdminHeader />
      <AdminSidebar collapsed={sidebarCollapsed} onCollapsedChange={setSidebarCollapsed} />
      <div
        className={cn(
          "content-wrapper tw-min-h-[calc(100vh-72px)] tw-bg-[radial-gradient(circle_at_top_right,rgba(37,99,235,0.08),transparent_30%),linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] tw-pb-6 tw-pt-[72px] tw-transition-[margin] tw-duration-300",
          sidebarCollapsed ? "min-[993px]:!tw-ml-[84px]" : "min-[993px]:!tw-ml-[248px]",
        )}
      >
        {children}
      </div>
    </div>
  );
}
