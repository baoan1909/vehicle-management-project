import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type Dispatch,
  type PropsWithChildren,
  type SetStateAction,
} from "react";
import type { User } from "oidc-client-ts";
import {
  classifyAccessError,
  isAbortAccessError,
  isAccessLoadingStatus,
  isForbiddenAccessError,
  isResolvedAccessStatus,
  isUnauthorizedAccessError,
  shouldReplaceAccessSnapshot,
  type AccessError,
  type AccessStatus,
} from "@/core/auth/accessState";
import { resolveAuthoritativeAccess } from "@/core/auth/accessResolution";
import {
  getOidcUser,
  getValidAccessToken,
  isOidcSigninResponse,
  removeOidcUser,
  subscribeOidcSessionEvents,
} from "@/core/auth/oidcSession";
import { clearLegacyAuthStorage, createCurrentUserFromOidcUser } from "@/core/auth/session";
import { getMyAccountAccess } from "@/features/iam/api/currentAccountAccessApi";
import { getMyAccountProfile } from "@/features/iam/api/accountProfileApi";
import { mergeAuthorizationSnapshot } from "@/features/iam/utils/accountProfileMapper";
import type { CurrentUser } from "@/shared/types/common";

const ACCESS_REQUEST_TIMEOUT_MS = 8000;
const ACCESS_REFRESH_INTERVAL_MS = 60_000;

export type AuthenticationStatus =
  | "initializing"
  | "unauthenticated"
  | "authenticated"
  | "renewing"
  | "error";

interface AuthContextValue {
  accessError: AccessError | null;
  accessStatus: AccessStatus;
  authenticationError: Error | null;
  authenticationStatus: AuthenticationStatus;
  beginAuthenticatedSession: (oidcUser: User) => void;
  canRetryAccess: boolean;
  isAccessLoading: boolean;
  isAccessRefreshing: boolean;
  isAccessStale: boolean;
  isProfileLoading: boolean;
  invalidateAndRefreshAccess: (reason?: string) => Promise<CurrentUser | null>;
  refreshAccess: (options?: { force?: boolean }) => Promise<CurrentUser | null>;
  resolveAccess: (user: CurrentUser) => void;
  retryAccess: () => void;
  retryAuthentication: () => Promise<void>;
  user: CurrentUser | null;
  setUser: Dispatch<SetStateAction<CurrentUser | null>>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

function buildUserKey(user: CurrentUser) {
  return `${user.id}:${user.username}`;
}

function isSameUser(user: CurrentUser | null, expectedUserKey: string) {
  return Boolean(user && buildUserKey(user) === expectedUserKey);
}

function asError(error: unknown) {
  return error instanceof Error ? error : new Error("Không thể khôi phục phiên đăng nhập.");
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUserState] = useState<CurrentUser | null>(null);
  const [authenticationStatus, setAuthenticationStatus] = useState<AuthenticationStatus>("initializing");
  const [authenticationError, setAuthenticationError] = useState<Error | null>(null);
  const [accessStatus, setAccessStatus] = useState<AccessStatus>("idle");
  const [accessError, setAccessError] = useState<AccessError | null>(null);
  const [retryNonce, setRetryNonce] = useState(0);

  const userRef = useRef<CurrentUser | null>(null);
  const oidcSubjectRef = useRef("");
  const accessStatusRef = useRef<AccessStatus>("idle");
  const accessErrorRef = useRef<AccessError | null>(null);
  const accessRequestIdRef = useRef(0);
  const refreshPromiseRef = useRef<Promise<CurrentUser | null> | null>(null);
  const refreshAbortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    userRef.current = user;
  }, [user]);

  useEffect(() => {
    accessStatusRef.current = accessStatus;
  }, [accessStatus]);

  useEffect(() => {
    accessErrorRef.current = accessError;
  }, [accessError]);

  const cancelAccessWork = useCallback(() => {
    accessRequestIdRef.current += 1;
    refreshAbortRef.current?.abort();
    refreshAbortRef.current = null;
    refreshPromiseRef.current = null;
  }, []);

  const clearAccessSnapshot = useCallback(() => {
    cancelAccessWork();
    userRef.current = null;
    setUserState(null);
    setAccessError(null);
    setAccessStatus("idle");
  }, [cancelAccessWork]);

  const clearSession = useCallback(() => {
    clearAccessSnapshot();
    oidcSubjectRef.current = "";
    setAuthenticationError(null);
    setAuthenticationStatus("unauthenticated");
    clearLegacyAuthStorage();
    void removeOidcUser();
  }, [clearAccessSnapshot]);

  const beginAuthenticatedSession = useCallback((oidcUser: User) => {
    cancelAccessWork();
    const identity = createCurrentUserFromOidcUser(oidcUser);
    oidcSubjectRef.current = oidcUser.profile.sub;
    userRef.current = identity;
    setUserState(identity);
    setAuthenticationError(null);
    setAuthenticationStatus("authenticated");
    setAccessError(null);
    setAccessStatus("loading");
  }, [cancelAccessWork]);

  const resolveAccess = useCallback((nextUser: CurrentUser) => {
    cancelAccessWork();
    userRef.current = nextUser;
    setUserState(nextUser);
    setAuthenticationError(null);
    setAuthenticationStatus("authenticated");
    setAccessError(null);
    setAccessStatus("ready");
  }, [cancelAccessWork]);

  const setUser = useCallback<Dispatch<SetStateAction<CurrentUser | null>>>((nextUser) => {
    const resolvedUser = typeof nextUser === "function" ? nextUser(userRef.current) : nextUser;
    if (resolvedUser === null) {
      clearAccessSnapshot();
      return;
    }
    userRef.current = resolvedUser;
    setUserState(resolvedUser);
  }, [clearAccessSnapshot]);

  const restoreAuthentication = useCallback(async () => {
    setAuthenticationError(null);
    setAuthenticationStatus("initializing");
    clearLegacyAuthStorage();

    const storedUser = await getOidcUser();
    if (!storedUser) {
      oidcSubjectRef.current = "";
      setAuthenticationStatus("unauthenticated");
      return;
    }

    try {
      const token = await getValidAccessToken();
      const validUser = token ? await getOidcUser() : null;
      if (!validUser) {
        clearSession();
        return;
      }
      beginAuthenticatedSession(validUser);
    } catch (error) {
      oidcSubjectRef.current = storedUser.profile.sub;
      userRef.current = createCurrentUserFromOidcUser(storedUser);
      setUserState(userRef.current);
      setAuthenticationError(asError(error));
      setAuthenticationStatus("error");
      setAccessStatus("idle");
    }
  }, [beginAuthenticatedSession, clearSession]);

  const retryAuthentication = useCallback(async () => {
    await restoreAuthentication();
  }, [restoreAuthentication]);

  useEffect(() => {
    let active = true;
    const unsubscribe = subscribeOidcSessionEvents({
      onRenewing: () => {
        if (active) setAuthenticationStatus("renewing");
      },
      onRenewError: (error) => {
        if (!active) return;
        setAuthenticationError(asError(error));
        setAuthenticationStatus(userRef.current ? "error" : "unauthenticated");
      },
      onUserLoaded: (oidcUser) => {
        if (!active) return;
        if (oidcSubjectRef.current && oidcSubjectRef.current === oidcUser.profile.sub && userRef.current) {
          setAuthenticationError(null);
          setAuthenticationStatus("authenticated");
          return;
        }
        beginAuthenticatedSession(oidcUser);
      },
      onUserRemoved: () => {
        if (!active) return;
        clearAccessSnapshot();
        oidcSubjectRef.current = "";
        setAuthenticationError(null);
        setAuthenticationStatus("unauthenticated");
      },
    });

    if (isOidcSigninResponse(window.location.search)) {
      setAuthenticationStatus("initializing");
      return () => {
        active = false;
        unsubscribe();
        cancelAccessWork();
      };
    }

    void restoreAuthentication().catch((error) => {
      if (!active) return;
      setAuthenticationError(asError(error));
      setAuthenticationStatus("error");
    });

    return () => {
      active = false;
      unsubscribe();
      cancelAccessWork();
    };
  }, [beginAuthenticatedSession, cancelAccessWork, clearAccessSnapshot, restoreAuthentication]);

  const performAccessRefresh = useCallback(async (force: boolean): Promise<CurrentUser | null> => {
    const currentUser = userRef.current;
    if (!currentUser) return null;

    const currentStatus = accessStatusRef.current;
    const currentError = accessErrorRef.current;
    const canRefresh = isResolvedAccessStatus(currentStatus)
      || (currentStatus === "error" && Boolean(currentError?.retryable))
      || force;
    if (!canRefresh) return null;

    const expectedUserKey = buildUserKey(currentUser);
    const requestId = accessRequestIdRef.current + 1;
    accessRequestIdRef.current = requestId;
    const controller = new AbortController();
    refreshAbortRef.current = controller;
    setAccessStatus(Array.isArray(currentUser.permissionCodes) ? "refreshing" : "loading");

    try {
      const response = await getMyAccountAccess({
        signal: controller.signal,
        timeoutMs: ACCESS_REQUEST_TIMEOUT_MS,
      });
      if (accessRequestIdRef.current !== requestId || !isSameUser(userRef.current, expectedUserKey)) return null;

      const latestUser = userRef.current;
      if (!latestUser) return null;
      if (!shouldReplaceAccessSnapshot(latestUser.accessRevision, response.data.accessRevision)) {
        setAccessError(null);
        setAccessStatus("ready");
        return latestUser;
      }

      const nextUser = mergeAuthorizationSnapshot(latestUser, response.data);
      userRef.current = nextUser;
      setUserState(nextUser);
      setAccessError(null);
      setAccessStatus("ready");
      return nextUser;
    } catch (error) {
      if (accessRequestIdRef.current !== requestId || !isSameUser(userRef.current, expectedUserKey)) return null;
      if (isAbortAccessError(error)) return null;
      if (isUnauthorizedAccessError(error)) {
        clearSession();
        return null;
      }

      const classifiedError = classifyAccessError(error);
      if (!classifiedError) return null;
      setAccessError(classifiedError);
      if (isForbiddenAccessError(error)) {
        setAccessStatus("error");
        return null;
      }
      setAccessStatus(Array.isArray(userRef.current?.permissionCodes) ? "stale" : "error");
      return null;
    } finally {
      if (accessRequestIdRef.current === requestId) refreshAbortRef.current = null;
    }
  }, [clearSession]);

  const startAccessRefresh = useCallback((force: boolean) => {
    const promise = performAccessRefresh(force).finally(() => {
      if (refreshPromiseRef.current === promise) refreshPromiseRef.current = null;
    });
    refreshPromiseRef.current = promise;
    return promise;
  }, [performAccessRefresh]);

  const refreshAccess = useCallback((options: { force?: boolean } = {}) => {
    const existing = refreshPromiseRef.current;
    if (existing) {
      if (!options.force) return existing;
      return existing.then(() => refreshPromiseRef.current ?? startAccessRefresh(true));
    }
    return startAccessRefresh(Boolean(options.force));
  }, [startAccessRefresh]);

  const invalidateAndRefreshAccess = useCallback((_reason?: string) => (
    refreshAccess({ force: true })
  ), [refreshAccess]);

  const retryAccess = useCallback(() => {
    const currentUser = userRef.current;
    if (!currentUser) return;
    if (Array.isArray(currentUser.permissionCodes)) {
      void refreshAccess();
      return;
    }
    setAccessError(null);
    setAccessStatus("loading");
    setRetryNonce((currentNonce) => currentNonce + 1);
  }, [refreshAccess]);

  useEffect(() => {
    if (!user || Array.isArray(user.permissionCodes) || accessStatus !== "loading") return;

    const expectedUserKey = buildUserKey(user);
    const requestId = accessRequestIdRef.current + 1;
    accessRequestIdRef.current = requestId;
    const controller = new AbortController();
    let active = true;
    setAccessError(null);

    resolveAuthoritativeAccess({
      currentUser: user,
      fetchCurrentAccess: () => getMyAccountAccess({
        signal: controller.signal,
        timeoutMs: ACCESS_REQUEST_TIMEOUT_MS,
      }).then((response) => response.data),
      fetchProfile: () => getMyAccountProfile({
        signal: controller.signal,
        timeoutMs: ACCESS_REQUEST_TIMEOUT_MS,
      }).then((response) => response.data),
    })
      .then((resolvedUser) => {
        if (!active || accessRequestIdRef.current !== requestId || !isSameUser(userRef.current, expectedUserKey)) return;
        resolveAccess(resolvedUser);
      })
      .catch((error) => {
        if (!active || accessRequestIdRef.current !== requestId || !isSameUser(userRef.current, expectedUserKey)) return;
        if (isAbortAccessError(error)) return;
        if (isUnauthorizedAccessError(error)) {
          clearSession();
          return;
        }
        const classifiedError = classifyAccessError(error);
        if (!classifiedError) return;
        setAccessError(classifiedError);
        setAccessStatus("error");
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [accessStatus, clearSession, resolveAccess, retryNonce, user]);

  const hasResolvedAccess = Boolean(user && Array.isArray(user.permissionCodes));
  useEffect(() => {
    if (!user || !hasResolvedAccess) return;
    const intervalId = window.setInterval(() => {
      if (document.visibilityState === "visible") void refreshAccess();
    }, ACCESS_REFRESH_INTERVAL_MS);
    return () => {
      window.clearInterval(intervalId);
      refreshAbortRef.current?.abort();
    };
  }, [hasResolvedAccess, refreshAccess, user?.id]);

  const isAccessLoading = Boolean(user && isAccessLoadingStatus(accessStatus));
  const isAccessRefreshing = accessStatus === "refreshing";
  const isAccessStale = accessStatus === "stale";
  const canRetryAccess = accessStatus === "stale"
    || (accessStatus === "error" && Boolean(accessError?.retryable));

  const value = useMemo(() => ({
    accessError,
    accessStatus,
    authenticationError,
    authenticationStatus,
    beginAuthenticatedSession,
    canRetryAccess,
    isAccessLoading,
    isAccessRefreshing,
    isAccessStale,
    isProfileLoading: isAccessLoading,
    invalidateAndRefreshAccess,
    refreshAccess,
    resolveAccess,
    retryAccess,
    retryAuthentication,
    user,
    setUser,
  }), [
    accessError,
    accessStatus,
    authenticationError,
    authenticationStatus,
    beginAuthenticatedSession,
    canRetryAccess,
    isAccessLoading,
    isAccessRefreshing,
    isAccessStale,
    invalidateAndRefreshAccess,
    refreshAccess,
    resolveAccess,
    retryAccess,
    retryAuthentication,
    user,
    setUser,
  ]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
