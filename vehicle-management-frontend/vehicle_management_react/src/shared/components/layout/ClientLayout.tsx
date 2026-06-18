import type { PropsWithChildren } from "react";
import { useLocation } from "react-router-dom";
import { ClientNavbar } from "./ClientNavbar";

export function ClientLayout({ children }: PropsWithChildren) {
  const location = useLocation();
  const isCustomerPortal = location.pathname.startsWith("/customer");

  return (
    <div className="wrapper vm-client-shell">
      {!isCustomerPortal && <ClientNavbar />}
      <div className="content-wrapper">{children}</div>
    </div>
  );
}
