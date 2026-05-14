import type { PropsWithChildren } from "react";
import { ClientNavbar } from "./ClientNavbar";

export function ClientLayout({ children }: PropsWithChildren) {
  return (
    <div className="wrapper vm-client-shell">
      <ClientNavbar />
      <div className="content-wrapper">{children}</div>
    </div>
  );
}
