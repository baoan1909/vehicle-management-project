import { createContext, useMemo, useState, type PropsWithChildren } from "react";
import type { CurrentUser } from "../../shared/types/common";

interface AuthContextValue {
  user: CurrentUser | null;
  setUser: (user: CurrentUser | null) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

const defaultUser: CurrentUser = {
  id: "A001",
  username: "admin",
  fullName: "Nguyễn Văn Admin",
  role: "ADMIN",
  avatarUrl: "/assets/admin/dist/img/user2-160x160.jpg",
};

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<CurrentUser | null>(defaultUser);
  const value = useMemo(() => ({ user, setUser }), [user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
