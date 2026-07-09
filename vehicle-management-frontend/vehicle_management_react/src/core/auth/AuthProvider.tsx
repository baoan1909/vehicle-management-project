import { createContext, useCallback, useEffect, useMemo, useRef, useState, type Dispatch, type PropsWithChildren, type SetStateAction } from "react";
import { getCurrentUserFromStoredToken, saveCurrentUserSnapshot } from "@/core/auth/session";
import { getMyAccountProfile } from "@/features/iam/api/accountProfileApi";
import { mergeCurrentUserWithAccountProfile } from "@/features/iam/utils/accountProfileMapper";
import type { CurrentUser } from "@/shared/types/common";

interface AuthContextValue {
  user: CurrentUser | null;
  setUser: Dispatch<SetStateAction<CurrentUser | null>>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUserState] = useState<CurrentUser | null>(() => getCurrentUserFromStoredToken());
  const hydratedUserKeyRef = useRef<string | null>(null);
  const setUser = useCallback<Dispatch<SetStateAction<CurrentUser | null>>>((nextUser) => {
    setUserState((currentUser) => {
      const resolvedUser = typeof nextUser === "function" ? nextUser(currentUser) : nextUser;
      saveCurrentUserSnapshot(resolvedUser);
      return resolvedUser;
    });
  }, []);
  const value = useMemo(() => ({ user, setUser }), [user]);

  useEffect(() => {
    if (!user) {
      hydratedUserKeyRef.current = null;
      return;
    }

    const userKey = `${user.id}:${user.username}:${user.role}`;
    if (hydratedUserKeyRef.current === userKey) return;
    hydratedUserKeyRef.current = userKey;

    let mounted = true;

    getMyAccountProfile()
      .then((response) => {
        if (!mounted) return;
        setUser((currentUser) => (currentUser ? mergeCurrentUserWithAccountProfile(currentUser, response.data) : currentUser));
      })
      .catch(() => {
        if (mounted) hydratedUserKeyRef.current = null;
      });

    return () => {
      mounted = false;
    };
  }, [user?.id, user?.username]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
