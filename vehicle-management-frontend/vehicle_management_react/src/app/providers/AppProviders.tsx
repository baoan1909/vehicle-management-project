import type { PropsWithChildren } from "react";
import { AuthProvider } from "@/core/auth/AuthProvider";
import { ToastProvider } from "@/shared/components/ui/ToastProvider";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <ToastProvider>
      <AuthProvider>{children}</AuthProvider>
    </ToastProvider>
  );
}
