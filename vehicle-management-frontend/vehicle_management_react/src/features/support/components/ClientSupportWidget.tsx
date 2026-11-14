import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { useAuth } from "@/core/auth/useAuth";
import { GuestSupportFloatingWidget } from "@/features/support/components/GuestSupportFloatingWidget";
import { SupportFloatingWidget } from "@/features/support/components/SupportFloatingWidget";
import { openSupportWidget } from "@/features/support/supportWidgetEvents";

export function ClientSupportWidget() {
  const { authenticationStatus, user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const isGuest = authenticationStatus === "unauthenticated" && !user;

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get("support") !== "open" || (!isGuest && !user)) return;
    openSupportWidget();
    params.delete("support");
    const search = params.toString();
    navigate({ pathname: location.pathname, search: search ? `?${search}` : "", hash: location.hash }, { replace: true });
  }, [isGuest, user, location.pathname, location.search, location.hash, navigate]);

  if (isGuest) return <GuestSupportFloatingWidget />;
  if (user) return <SupportFloatingWidget key={user.id} />;
  return null;
}
