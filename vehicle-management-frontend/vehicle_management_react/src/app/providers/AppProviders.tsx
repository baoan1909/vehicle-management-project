import type { PropsWithChildren } from "react";
import { AuthProvider } from "@/core/auth/AuthProvider";

export function AppProviders({ children }: PropsWithChildren) {
  return <AuthProvider>{children}</AuthProvider>;
}
