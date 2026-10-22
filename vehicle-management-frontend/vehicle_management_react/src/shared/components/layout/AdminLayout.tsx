import type { PropsWithChildren } from "react";
import { useCallback, useState } from "react";
import { useLocation } from "react-router-dom";
import { AdminHeader } from "./AdminHeader";
import { AdminSidebar } from "./AdminSidebar";
import { cn } from "@/lib/cn";
import { AdminAnnouncementTicker } from "@/features/notifications/components/AdminAnnouncementTicker";
import { UserGuideBackendHost } from "@/features/user-guide/components/UserGuideBackendHost";
import { deriveGuideContext } from "@/features/user-guide/utils/guideContext";
import { PlatformMonitoringScopeProvider } from "@/shared/monitoring/PlatformMonitoringScope";
import { MonitoringScopeBanner } from "./MonitoringScopeBanner";

interface AdminLayoutProps extends PropsWithChildren {
}

export function AdminLayout({ children }: AdminLayoutProps) {
  const location = useLocation();
  const guideContext = deriveGuideContext(location.pathname);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [announcementTickerVisible, setAnnouncementTickerVisible] = useState(false);
  const handleTickerVisibleChange = useCallback((visible: boolean) => {
    setAnnouncementTickerVisible(visible);
  }, []);

  return (
    <PlatformMonitoringScopeProvider>
      <div className="wrapper tw-min-h-screen tw-bg-vm-canvas tw-text-vm-slate-700">
        <AdminHeader />
        <AdminAnnouncementTicker onVisibleChange={handleTickerVisibleChange} />
        <AdminSidebar
          collapsed={sidebarCollapsed}
          offsetTop={announcementTickerVisible ? 112 : 72}
          onCollapsedChange={setSidebarCollapsed}
        />
        <div
          className={cn(
            "content-wrapper tw-bg-[radial-gradient(circle_at_top_right,rgba(37,99,235,0.08),transparent_30%),linear-gradient(180deg,#f8fafc_0%,#eef2f7_100%)] tw-pb-6 tw-transition-[margin,padding] tw-duration-300",
            announcementTickerVisible ? "tw-min-h-[calc(100vh-112px)] tw-pt-[112px]" : "tw-min-h-[calc(100vh-72px)] tw-pt-[72px]",
            sidebarCollapsed ? "min-[993px]:!tw-ml-[84px]" : "min-[993px]:!tw-ml-[248px]",
          )}
        >
          <MonitoringScopeBanner />
          {children}
          {guideContext.featureCode ? (
            <UserGuideBackendHost
              key={location.pathname}
              contextKey={guideContext.contextKey}
              featureCode={guideContext.featureCode}
            />
          ) : null}
        </div>
      </div>
    </PlatformMonitoringScopeProvider>
  );
}
