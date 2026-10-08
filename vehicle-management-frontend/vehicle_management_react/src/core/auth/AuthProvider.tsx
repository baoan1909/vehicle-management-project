import { createContext, useCallback, useEffect, useMemo, useRef, useState, type Dispatch, type PropsWithChildren, type SetStateAction } from "react";
import { getCurrentUserFromStoredToken, saveCurrentUserSnapshot } from "@/core/auth/session";
import { getValidAccessToken } from "@/core/auth/tokenRefresh";
import { getMyAccountAccess } from "@/features/iam/api/currentAccountAccessApi";
import { mergeCurrentUserWithCurrentAccess } from "@/features/iam/utils/accountProfileMapper";
import type { CurrentUser } from "@/shared/types/common";

const ACCESS_HYDRATION_TIMEOUT_MS = 2500;

interface AuthContextValue {
  isAccessLoading: boolean;
  isProfileLoading: boolean;
  user: CurrentUser | null;
  setUser: Dispatch<SetStateAction<CurrentUser | null>>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

function createTimedRequest<T>(factory: (signal: AbortSignal) => Promise<T>, timeoutMs: number, message: string) {
  const controller = new AbortController();
  let timeoutId: number | undefined;

  const timeoutPromise = new Promise<T>((_, reject) => {
    timeoutId = window.setTimeout(() => {
      controller.abort();
      reject(new Error(message));
    }, timeoutMs);
  });

  return {
    abort: () => {
      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
      }
      controller.abort();
    },
    promise: Promise.race([factory(controller.signal), timeoutPromise]).finally(() => {
      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
      }
    }),
  };
}

function buildUserKey(user: CurrentUser) {
  return `${user.id}:${user.username}:${user.role}`;
}

function clearPermissions(currentUser: CurrentUser | null, expectedUserKey: string) {
  if (!currentUser || buildUserKey(currentUser) !== expectedUserKey) {
    return currentUser;
  }

  return {
    ...currentUser,
    permissionCodes: [],
  };
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUserState] = useState<CurrentUser | null>(() => getCurrentUserFromStoredToken());
  const [isAccessLoading, setIsAccessLoading] = useState(() => Boolean(user && !Array.isArray(user.permissionCodes)));
  const accessRequestIdRef = useRef(0);
  const setUser = useCallback<Dispatch<SetStateAction<CurrentUser | null>>>((nextUser) => {
    setUserState((currentUser) => {
      const resolvedUser = typeof nextUser === "function" ? nextUser(currentUser) : nextUser;
      saveCurrentUserSnapshot(resolvedUser);
      return resolvedUser;
    });
  }, []);
  const value = useMemo(
    () => ({ isAccessLoading, isProfileLoading: isAccessLoading, user, setUser }),
    [isAccessLoading, user, setUser],
  );

  useEffect(() => {
    if (!user) {
      accessRequestIdRef.current += 1;
      setIsAccessLoading(false);
      return;
    }

    if (Array.isArray(user.permissionCodes)) {
      setIsAccessLoading(false);
      return;
    }

    const userKey = buildUserKey(user);
    const requestId = accessRequestIdRef.current + 1;
    accessRequestIdRef.current = requestId;
    setIsAccessLoading(true);

    let active = true;

    const request = createTimedRequest(
      (signal) => getMyAccountAccess({ signal }),
      ACCESS_HYDRATION_TIMEOUT_MS,
      "Current account access loading timed out",
    );

    request.promise
      .then((response) => {
        if (!active || accessRequestIdRef.current !== requestId) return;
        setUser((currentUser) => (currentUser ? mergeCurrentUserWithCurrentAccess(currentUser, response.data) : currentUser));
      })
      .catch(() => {
        if (!active || accessRequestIdRef.current !== requestId) return;
        setUser((currentUser) => clearPermissions(currentUser, userKey));
      })
      .finally(() => {
        if (active && accessRequestIdRef.current === requestId) {
          setIsAccessLoading(false);
        }
      });

    return () => {
      active = false;
      request.abort();
    };
  }, [user?.id, user?.permissionCodes, user?.role, user?.username, setUser]);

  useEffect(() => {
    if (!user) return;

    let mounted = true;
    const keepSessionFresh = () => {
      getValidAccessToken().catch(() => {
        if (mounted) setUser(null);
      });
    };

    const intervalId = window.setInterval(keepSessionFresh, 60_000);

    return () => {
      mounted = false;
      window.clearInterval(intervalId);
    };
  }, [user?.id, setUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
