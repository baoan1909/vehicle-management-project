import type { PropsWithChildren } from "react";
import { AuthProvider } from "@/core/auth/AuthProvider";
import { GuestSupportConversationProvider } from "@/features/support/context/GuestSupportConversationProvider";
import { AccountStateSyncProvider } from "@/features/notifications/context/AccountStateSyncProvider";
import { ToastProvider } from "@/shared/components/ui/ToastProvider";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ToastProvider>
      <AuthProvider>
        <AccountStateSyncProvider>
          <GuestSupportConversationProvider>{children}</GuestSupportConversationProvider>
        </AccountStateSyncProvider>
      </AuthProvider>
    </ToastProvider>
  );
}
