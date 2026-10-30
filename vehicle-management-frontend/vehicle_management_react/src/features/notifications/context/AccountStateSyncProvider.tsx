import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren,
} from "react";
import { useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  countMyUnreadNotifications,
  getMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationUserResponse,
} from "@/features/notifications/api/notificationApi";
import { subscribeNotificationRealtime } from "@/features/notifications/api/notificationRealtime";
import { publishNotificationReceived } from "@/features/notifications/utils/notificationEvents";
import {
  classifyAccountStateInvalidation,
  hasAccountStateInvalidation,
  type AccountStateInvalidation,
} from "@/features/notifications/utils/accountStateInvalidation";
import { AccountStateSyncCoordinator } from "@/features/notifications/utils/accountStateSyncCoordinator";
import { getMyAccountProfile } from "@/features/iam/api/accountProfileApi";
import { mergeProfileDetails } from "@/features/iam/utils/accountProfileMapper";

const FOREGROUND_RECONCILE_THROTTLE_MS = 15_000;
const MAX_PROCESSED_NOTIFICATION_IDS = 100;

type AccountStateSyncContextValue = {
  approvalInvalidationVersion: number;
  avatarInvalidationVersion: number;
  items: NotificationUserResponse[];
  loading: boolean;
  markAllRead: () => Promise<void>;
  markRead: (notificationId: string) => Promise<void>;
  profileInvalidationVersion: number;
  reloadNotifications: () => Promise<void>;
  syncError: Error | null;
  unreadCount: number;
};

export const AccountStateSyncContext = createContext<AccountStateSyncContextValue | null>(null);

function isUnread(notification: NotificationUserResponse) {
  return notification.status !== "READ" && !notification.readAt;
}

function asError(error: unknown) {
  return error instanceof Error ? error : new Error("Không thể đồng bộ trạng thái tài khoản.");
}

function cursorKey(accountId: string) {
  return `vehicle-management:account-state-sync:${accountId}`;
}

function readProcessedIds(accountId: string) {
  try {
    const parsed = JSON.parse(sessionStorage.getItem(cursorKey(accountId)) ?? "[]");
    return new Set<string>(Array.isArray(parsed) ? parsed.filter((value) => typeof value === "string") : []);
  } catch {
    return new Set<string>();
  }
}

function writeProcessedIds(accountId: string, ids: Set<string>) {
  try {
    sessionStorage.setItem(cursorKey(accountId), JSON.stringify(Array.from(ids).slice(-MAX_PROCESSED_NOTIFICATION_IDS)));
  } catch {
    // sessionStorage is only a dedupe optimization; authoritative state is always fetched from APIs.
  }
}

export function AccountStateSyncProvider({ children }: PropsWithChildren) {
  const { invalidateAndRefreshAccess, setUser, user } = useAuth();
  const hasResolvedAccess = Boolean(user && Array.isArray(user.permissionCodes));
  const toast = useToast();
  const [items, setItems] = useState<NotificationUserResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [syncError, setSyncError] = useState<Error | null>(null);
  const [profileInvalidationVersion, setProfileInvalidationVersion] = useState(0);
  const [approvalInvalidationVersion, setApprovalInvalidationVersion] = useState(0);
  const [avatarInvalidationVersion, setAvatarInvalidationVersion] = useState(0);
  const accountIdRef = useRef<string | null>(null);
  const processedIdsRef = useRef(new Set<string>());
  const profileRequestIdRef = useRef(0);
  const notificationRequestIdRef = useRef(0);
  const profileAbortRef = useRef<AbortController | null>(null);
  const notificationLoadPromiseRef = useRef<Promise<void> | null>(null);
  const lastForegroundSyncRef = useRef(0);
  const coordinatorRef = useRef<AccountStateSyncCoordinator | null>(null);

  const refreshProfile = useCallback(async () => {
    const expectedAccountId = accountIdRef.current;
    if (!expectedAccountId) return;
    const requestId = profileRequestIdRef.current + 1;
    profileRequestIdRef.current = requestId;
    profileAbortRef.current?.abort();
    const controller = new AbortController();
    profileAbortRef.current = controller;
    try {
      const response = await getMyAccountProfile({ signal: controller.signal, timeoutMs: 8000 });
      if (requestId !== profileRequestIdRef.current || accountIdRef.current !== expectedAccountId) return;
      setUser((currentUser) => currentUser && currentUser.id === expectedAccountId
        ? mergeProfileDetails(currentUser, response.data)
        : currentUser);
    } finally {
      if (requestId === profileRequestIdRef.current) profileAbortRef.current = null;
    }
  }, [setUser]);

  const executeSync = useCallback(async (invalidation: AccountStateInvalidation) => {
    setSyncError(null);
    const failures: Error[] = [];
    if (invalidation.refreshAccess) {
      try {
        await invalidateAndRefreshAccess("account-state-invalidation");
      } catch (error) {
        failures.push(asError(error));
      }
    }
    if (invalidation.refreshProfile || invalidation.refreshAvatar || invalidation.refreshApproval) {
      try {
        await refreshProfile();
      } catch (error) {
        failures.push(asError(error));
      }
    }
    if (invalidation.refreshProfile) setProfileInvalidationVersion((value) => value + 1);
    if (invalidation.refreshApproval) setApprovalInvalidationVersion((value) => value + 1);
    if (invalidation.refreshAvatar) setAvatarInvalidationVersion((value) => value + 1);
    if (failures.length > 0) setSyncError(failures[0]);
  }, [invalidateAndRefreshAccess, refreshProfile]);

  useEffect(() => {
    coordinatorRef.current = new AccountStateSyncCoordinator(executeSync);
    return () => {
      coordinatorRef.current = null;
    };
  }, [executeSync]);

  const requestSync = useCallback((invalidation: AccountStateInvalidation) => {
    if (!hasAccountStateInvalidation(invalidation)) return Promise.resolve();
    return coordinatorRef.current?.request(invalidation) ?? Promise.resolve();
  }, []);

  const processNotification = useCallback((notification: NotificationUserResponse, showToast: boolean) => {
    const accountId = accountIdRef.current;
    if (!accountId || !notification.notificationId || processedIdsRef.current.has(notification.notificationId)) {
      return;
    }
    processedIdsRef.current.add(notification.notificationId);
    while (processedIdsRef.current.size > MAX_PROCESSED_NOTIFICATION_IDS) {
      const oldest = processedIdsRef.current.values().next().value as string | undefined;
      if (!oldest) break;
      processedIdsRef.current.delete(oldest);
    }
    writeProcessedIds(accountId, processedIdsRef.current);
    publishNotificationReceived(notification);
    void requestSync(classifyAccountStateInvalidation(notification));
    if (showToast) {
      toast.notification(
        notification.message || "Bạn có thông báo mới.",
        notification.title || "Thông báo mới",
        undefined,
        isUnread(notification),
      );
    }
  }, [requestSync, toast]);

  const reloadNotifications = useCallback(() => {
    if (notificationLoadPromiseRef.current) return notificationLoadPromiseRef.current;
    const expectedAccountId = accountIdRef.current;
    if (!expectedAccountId) return Promise.resolve();
    const requestId = notificationRequestIdRef.current + 1;
    notificationRequestIdRef.current = requestId;
    const promise = (async () => {
      setLoading(true);
      try {
        const [listResponse, countResponse] = await Promise.all([
          getMyNotifications({ limit: 20 }),
          countMyUnreadNotifications(),
        ]);
        if (
          requestId !== notificationRequestIdRef.current
          || accountIdRef.current !== expectedAccountId
        ) return;
        const nextItems = listResponse.data ?? [];
        setItems(nextItems);
        setUnreadCount(countResponse.data?.unreadCount ?? 0);
        nextItems.slice().reverse().forEach((notification) => processNotification(notification, false));
      } catch (error) {
        if (
          requestId === notificationRequestIdRef.current
          && accountIdRef.current === expectedAccountId
        ) setSyncError(asError(error));
      } finally {
        if (requestId === notificationRequestIdRef.current) setLoading(false);
      }
    })().finally(() => {
      if (notificationLoadPromiseRef.current === promise) notificationLoadPromiseRef.current = null;
    });
    notificationLoadPromiseRef.current = promise;
    return promise;
  }, [processNotification]);

  useEffect(() => {
    const accountId = hasResolvedAccess ? user?.id ?? null : null;
    accountIdRef.current = accountId;
    profileRequestIdRef.current += 1;
    notificationRequestIdRef.current += 1;
    profileAbortRef.current?.abort();
    notificationLoadPromiseRef.current = null;
    setItems([]);
    setUnreadCount(0);
    setSyncError(null);
    if (!accountId) {
      processedIdsRef.current = new Set();
      return undefined;
    }

    processedIdsRef.current = readProcessedIds(accountId);
    let active = true;
    const reconcileAfterConnection = (reconnected: boolean) => {
      if (!active || !reconnected) return;
      // REST notifications are the durable catch-up channel. Processing unseen
      // notifications below will request only the account domains they affect;
      // reconnecting the transport must not invalidate the whole profile.
      void reloadNotifications();
    };
    const unsubscribe = subscribeNotificationRealtime({
      onConnected: reconcileAfterConnection,
      onNotification: (notification) => {
        if (!active) return;
        setItems((current) => [
          notification,
          ...current.filter((item) => item.notificationId !== notification.notificationId),
        ].slice(0, 20));
        if (isUnread(notification) && !processedIdsRef.current.has(notification.notificationId)) {
          setUnreadCount((current) => current + 1);
        }
        processNotification(notification, true);
      },
      onError: (error) => setSyncError(error),
    });
    void reloadNotifications();

    return () => {
      active = false;
      unsubscribe();
    };
  }, [hasResolvedAccess, processNotification, reloadNotifications, requestSync, user?.id]);

  useEffect(() => {
    if (!hasResolvedAccess || !user?.id) return undefined;
    const reconcile = () => {
      const now = Date.now();
      if (now - lastForegroundSyncRef.current < FOREGROUND_RECONCILE_THROTTLE_MS) return;
      lastForegroundSyncRef.current = now;
      void requestSync({
        refreshAccess: true,
        refreshApproval: false,
        refreshAvatar: false,
        refreshOrganizationScope: false,
        refreshProfile: false,
      });
      void reloadNotifications();
    };
    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") reconcile();
    };
    window.addEventListener("online", reconcile);
    document.addEventListener("visibilitychange", onVisibilityChange);
    return () => {
      window.removeEventListener("online", reconcile);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, [hasResolvedAccess, reloadNotifications, requestSync, user?.id]);

  const markRead = useCallback(async (notificationId: string) => {
    const readAt = new Date().toISOString();
    setItems((current) => current.map((item) => item.notificationId === notificationId
      ? { ...item, readAt, status: "READ" }
      : item));
    setUnreadCount((current) => Math.max(0, current - 1));
    try {
      await markNotificationRead(notificationId);
    } catch (error) {
      setSyncError(asError(error));
      await reloadNotifications();
    }
  }, [reloadNotifications]);

  const markAllRead = useCallback(async () => {
    const readAt = new Date().toISOString();
    setItems((current) => current.map((item) => ({ ...item, readAt: item.readAt ?? readAt, status: "READ" })));
    setUnreadCount(0);
    try {
      await markAllNotificationsRead();
    } catch (error) {
      setSyncError(asError(error));
      await reloadNotifications();
    }
  }, [reloadNotifications]);

  const value = useMemo(() => ({
    approvalInvalidationVersion,
    avatarInvalidationVersion,
    items,
    loading,
    markAllRead,
    markRead,
    profileInvalidationVersion,
    reloadNotifications,
    syncError,
    unreadCount,
  }), [
    approvalInvalidationVersion,
    avatarInvalidationVersion,
    items,
    loading,
    markAllRead,
    markRead,
    profileInvalidationVersion,
    reloadNotifications,
    syncError,
    unreadCount,
  ]);

  return <AccountStateSyncContext.Provider value={value}>{children}</AccountStateSyncContext.Provider>;
}
