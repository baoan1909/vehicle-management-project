import { useState, type ContextType } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router-dom";

import { AuthContext, type AuthenticationStatus } from "@/core/auth/AuthProvider";
import { ClientSupportWidget } from "@/features/support/components/ClientSupportWidget";
import { GuestSupportConversationProvider } from "@/features/support/context/GuestSupportConversationProvider";
import { openSupportWidget } from "@/features/support/supportWidgetEvents";
import { ToastProvider } from "@/shared/components/ui/ToastProvider";
import type { CurrentUser } from "@/shared/types/common";
import "@/app/styles/globals.css";

function Harness() {
  const [authenticationStatus, setAuthenticationStatus] = useState<AuthenticationStatus>("unauthenticated");
  const [user, setUser] = useState<CurrentUser | null>(null);
  function signIn(permission = true) {
    setUser({ id: "account-a", username: "customer-a", fullName: "Customer A", role: "CUSTOMER", avatarUrl: "",
      permissionCodes: permission ? ["SUPPORT_WIDGET_ACCESS_OWN"] : [],
    });
    setAuthenticationStatus("authenticated");
  }
  const auth = { user, authenticationStatus } as NonNullable<ContextType<typeof AuthContext>>;
  return <AuthContext.Provider value={auth}>
    <GuestSupportConversationProvider>
      <button onClick={() => signIn()}>Sign in</button>
      <button onClick={() => signIn(false)}>Sign in without permission</button>
      <button onClick={() => { setUser(null); setAuthenticationStatus("unauthenticated"); }}>Sign out</button>
      <button onClick={() => { setUser(null); setAuthenticationStatus("error"); }}>Authentication error</button>
      <button onClick={openSupportWidget}>Open support</button>
      <ClientSupportWidget />
    </GuestSupportConversationProvider>
  </AuthContext.Provider>;
}

createRoot(document.getElementById("root")!).render(<ToastProvider><MemoryRouter><Harness /></MemoryRouter></ToastProvider>);
