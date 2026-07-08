import { createContext, useMemo, useState, type PropsWithChildren } from "react";
import { getCurrentUserFromStoredToken } from "@/core/auth/session";
import type { CurrentUser } from "@/shared/types/common";

interface AuthContextValue {
  user: CurrentUser | null;
  setUser: (user: CurrentUser | null) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<CurrentUser | null>(() => getCurrentUserFromStoredToken());
  const value = useMemo(() => ({ user, setUser }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
