import { useCallback, useState, type PropsWithChildren } from "react";
import { useLocation } from "react-router-dom";
import { AdminAnnouncementTicker } from "@/features/notifications/components/AdminAnnouncementTicker";
import { cn } from "@/lib/cn";
import { ClientNavbar } from "./ClientNavbar";

export function ClientLayout({ children }: PropsWithChildren) {
  const location = useLocation();
  const isCustomerPortal = location.pathname.startsWith("/customer");
  const [announcementTickerVisible, setAnnouncementTickerVisible] = useState(false);

  const handleTickerVisibleChange = useCallback((visible: boolean) => {
    setAnnouncementTickerVisible(visible);
  }, []);

  return (
    <div className="wrapper vm-client-shell">
      <ClientNavbar />
      <AdminAnnouncementTicker
        onVisibleChange={handleTickerVisibleChange}
        topClassName="tw-top-[72px]"
      />
      <div
        className={cn(
          "content-wrapper tw-transition-[padding] tw-duration-300",
          announcementTickerVisible && (isCustomerPortal ? "vm-client-announcement-offset-compact" : "vm-client-announcement-offset"),
        )}
      >
        {children}
      </div>
    </div>
  );
}
