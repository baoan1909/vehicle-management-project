import type { PropsWithChildren } from "react";
import { AuthProvider } from "@/core/auth/AuthProvider";
import { AccountStateSyncProvider } from "@/features/notifications/context/AccountStateSyncProvider";
import { ToastProvider } from "@/shared/components/ui/ToastProvider";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ToastProvider>
      <AuthProvider>
        <AccountStateSyncProvider>{children}</AccountStateSyncProvider>
      </AuthProvider>
    </ToastProvider>
  );
}
