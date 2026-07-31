import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { canAccessAdminRoute } from "@/app/routePermissions";
import { useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  countMyUnreadNotifications,
  getMyNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationType,
  type NotificationUserResponse,
} from "@/features/notifications/api/notificationApi";
import { subscribeNotificationRealtime } from "@/features/notifications/api/notificationRealtime";
import { cn } from "@/lib/cn";

type NotificationBellProps = {
  variant?: "admin" | "customer";
};

type NotificationGroupKey = "service" | "operations" | "account";

const notificationGroups: Array<{
  description: string;
  icon: string;
  key: NotificationGroupKey;
  title: string;
  tone: string;
}> = [
  {
    description: "Hệ thống, vé, thanh toán, bảng giá",
    icon: "fas fa-bullhorn",
    key: "service",
    title: "Hệ thống & dịch vụ",
    tone: "tw-bg-brand-50 tw-text-vm-primary",
  },
  {
    description: "Ticket, ca trực, thiết bị, bãi xe",
    icon: "fas fa-parking",
    key: "operations",
    title: "Vận hành bãi xe",
    tone: "tw-bg-amber-50 tw-text-amber-700",
  },
  {
    description: "Tài khoản, hồ sơ, phê duyệt",
    icon: "fas fa-user-shield",
    key: "account",
    title: "Tài khoản & phê duyệt",
    tone: "tw-bg-emerald-50 tw-text-emerald-700",
  },
];

const typeMeta: Record<NotificationType, { icon: string; tone: string }> = {
  SYSTEM_NOTICE: { icon: "fas fa-bullhorn", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  SUBSCRIPTION_REQUESTED: { icon: "far fa-calendar-plus", tone: "tw-bg-amber-50 tw-text-amber-700" },
  SUBSCRIPTION_APPROVED: { icon: "far fa-calendar-check", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  SUBSCRIPTION_REJECTED: { icon: "far fa-calendar-times", tone: "tw-bg-red-50 tw-text-red-600" },
  SUBSCRIPTION_EXPIRING_SOON: { icon: "far fa-clock", tone: "tw-bg-amber-50 tw-text-amber-700" },
  SUBSCRIPTION_EXPIRED: { icon: "fas fa-calendar-times", tone: "tw-bg-red-50 tw-text-red-600" },
  SUBSCRIPTION_CANCELLED: { icon: "fas fa-ban", tone: "tw-bg-red-50 tw-text-red-600" },
  SUBSCRIPTION_PAYMENT_COMPLETED: { icon: "fas fa-receipt", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  INVOICE_CREATED: { icon: "far fa-file-invoice", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  PAYMENT_SUCCEEDED: { icon: "fas fa-check-circle", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  PAYMENT_FAILED: { icon: "fas fa-exclamation-circle", tone: "tw-bg-red-50 tw-text-red-600" },
  SUPPORT_TICKET_CREATED: { icon: "far fa-life-ring", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  SUPPORT_TICKET_ASSIGNED: { icon: "fas fa-user-check", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  SUPPORT_TICKET_IN_PROGRESS: { icon: "fas fa-spinner", tone: "tw-bg-amber-50 tw-text-amber-700" },
  SUPPORT_TICKET_RESPONDED: { icon: "far fa-comment-dots", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  SUPPORT_TICKET_REOPENED: { icon: "fas fa-redo", tone: "tw-bg-amber-50 tw-text-amber-700" },
  SUPPORT_TICKET_CLOSED: { icon: "far fa-check-circle", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  SHIFT_ASSIGNED: { icon: "far fa-calendar-alt", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  SHIFT_CHANGED: { icon: "fas fa-exchange-alt", tone: "tw-bg-amber-50 tw-text-amber-700" },
  SHIFT_CANCELLED: { icon: "far fa-calendar-times", tone: "tw-bg-red-50 tw-text-red-600" },
  DEVICE_OFFLINE: { icon: "fas fa-plug", tone: "tw-bg-red-50 tw-text-red-600" },
  DEVICE_MAINTENANCE: { icon: "fas fa-tools", tone: "tw-bg-amber-50 tw-text-amber-700" },
  LANE_MAINTENANCE: { icon: "fas fa-road", tone: "tw-bg-amber-50 tw-text-amber-700" },
  PARKING_LOT_MAINTENANCE: { icon: "fas fa-parking", tone: "tw-bg-amber-50 tw-text-amber-700" },
  PRICE_PLAN_CHANGED: { icon: "fas fa-file-invoice-dollar", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  PRICE_RULE_CHANGED: { icon: "fas fa-tags", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  TICKET_TYPE_CHANGED: { icon: "fas fa-ticket-alt", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  ACCOUNT_REGISTERED: { icon: "far fa-user", tone: "tw-bg-amber-50 tw-text-amber-700" },
  ACCOUNT_PROVISIONED: { icon: "fas fa-user-plus", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  ACCOUNT_STATUS_CHANGED: { icon: "fas fa-user-cog", tone: "tw-bg-brand-50 tw-text-vm-primary" },
  ACCOUNT_PROFILE_SUBMITTED: { icon: "fas fa-id-card", tone: "tw-bg-amber-50 tw-text-amber-700" },
  CUSTOMER_ONBOARDING_APPROVED: { icon: "fas fa-user-check", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  CUSTOMER_ONBOARDING_REJECTED: { icon: "fas fa-user-times", tone: "tw-bg-red-50 tw-text-red-600" },
  CUSTOMER_ONBOARDING_RESUBMITTED: { icon: "fas fa-user-clock", tone: "tw-bg-amber-50 tw-text-amber-700" },
  INTERNAL_EMPLOYEE_APPROVED: { icon: "fas fa-user-tie", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  INTERNAL_EMPLOYEE_REJECTED: { icon: "fas fa-user-times", tone: "tw-bg-red-50 tw-text-red-600" },
  INTERNAL_EMPLOYEE_RESUBMITTED: { icon: "fas fa-user-clock", tone: "tw-bg-amber-50 tw-text-amber-700" },
  SYSTEM_ADMIN_APPROVED: { icon: "fas fa-user-shield", tone: "tw-bg-emerald-50 tw-text-emerald-700" },
  SYSTEM_ADMIN_REJECTED: { icon: "fas fa-user-times", tone: "tw-bg-red-50 tw-text-red-600" },
  SYSTEM_ADMIN_RESUBMITTED: { icon: "fas fa-user-clock", tone: "tw-bg-amber-50 tw-text-amber-700" },
};

function formatDateTime(value: string | null | undefined) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    hour12: false,
    minute: "2-digit",
    month: "2-digit",
  }).format(date);
}

function isUnread(notification: NotificationUserResponse) {
  return notification.status !== "READ" && !notification.readAt;
}

function getGroupKey(type: NotificationType): NotificationGroupKey {
  if (
    type.startsWith("SUPPORT_TICKET") ||
    type.startsWith("SHIFT") ||
    type.startsWith("DEVICE") ||
    type === "LANE_MAINTENANCE" ||
    type === "PARKING_LOT_MAINTENANCE"
  ) {
    return "operations";
  }

  if (
    type.startsWith("ACCOUNT") ||
    type.includes("ONBOARDING") ||
    type.includes("EMPLOYEE") ||
    type.includes("SYSTEM_ADMIN")
  ) {
    return "account";
  }

  return "service";
}

function normalizeNotificationTarget(notification: NotificationUserResponse, role?: string) {
  const redirectUrl = notification.redirectUrl?.trim();
  if (redirectUrl) return redirectUrl;

  if (notification.notificationType.startsWith("SUPPORT_TICKET")) {
    return role === "CUSTOMER" ? "/customer/support" : "/admin/support-center";
  }
  if (notification.notificationType.startsWith("SUBSCRIPTION") || notification.notificationType.startsWith("PAYMENT") || notification.notificationType === "INVOICE_CREATED") {
    return role === "CUSTOMER" ? "/customer/subscriptions" : "/admin/subscription-approvals";
  }
  if (notification.notificationType.startsWith("SHIFT")) {
    return "/admin/shifts";
  }
  if (notification.notificationType.startsWith("DEVICE") || notification.notificationType === "LANE_MAINTENANCE" || notification.notificationType === "PARKING_LOT_MAINTENANCE") {
    return "/admin/devices";
  }
  if (notification.notificationType.startsWith("PRICE") || notification.notificationType === "TICKET_TYPE_CHANGED") {
    return "/admin/price-plans";
  }
  if (notification.notificationType.includes("ONBOARDING") || notification.notificationType.includes("EMPLOYEE") || notification.notificationType.includes("SYSTEM_ADMIN")) {
    return role === "CUSTOMER" ? "/customer/profile" : "/admin/account";
  }
  if (notification.notificationType.startsWith("ACCOUNT")) {
    return role === "CUSTOMER" ? "/customer/profile" : "/admin/account";
  }

  return null;
}

export function NotificationBell({ variant = "admin" }: NotificationBellProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const rootRef = useRef<HTMLDivElement | null>(null);
  const [activeGroup, setActiveGroup] = useState<NotificationGroupKey | null>(null);
  const [items, setItems] = useState<NotificationUserResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const loadNotifications = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const [listResponse, countResponse] = await Promise.all([
        getMyNotifications({ limit: 20 }),
        countMyUnreadNotifications(),
      ]);
      setItems(listResponse.data ?? []);
      setUnreadCount(countResponse.data?.unreadCount ?? 0);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  useEffect(() => {
    if (!user) return undefined;

    return subscribeNotificationRealtime({
      onNotification: (notification) => {
        toast.notification(
          notification.message || "Bạn có thông báo mới.",
          notification.title || "Thông báo mới",
          formatDateTime(notification.createdAt ?? notification.sentAt),
          isUnread(notification),
        );
        setItems((current) => [
          notification,
          ...current.filter((item) => item.notificationId !== notification.notificationId),
        ].slice(0, 20));
        if (isUnread(notification)) {
          setUnreadCount((current) => current + 1);
        }
      },
    });
  }, [toast, user]);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (rootRef.current && !rootRef.current.contains(target)) {
        setOpen(false);
        setActiveGroup(null);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  const groupedItems = useMemo(
    () =>
      notificationGroups.reduce<Record<NotificationGroupKey, NotificationUserResponse[]>>(
        (result, group) => {
          result[group.key] = items.filter((item) => getGroupKey(item.notificationType) === group.key);
          return result;
        },
        { account: [], operations: [], service: [] },
      ),
    [items],
  );
  const activeGroupMeta = notificationGroups.find((group) => group.key === activeGroup) ?? notificationGroups[0];
  const activeItems = activeGroup ? groupedItems[activeGroup] : [];
  const badgeValue = unreadCount > 99 ? "99+" : String(unreadCount);

  function closePanel() {
    setOpen(false);
    setActiveGroup(null);
  }

  async function handleItemClick(notification: NotificationUserResponse) {
    const target = normalizeNotificationTarget(notification, user?.role);
    const canNavigate =
      target &&
      (!target.startsWith("/admin/") || canAccessAdminRoute(user, target));

    closePanel();
    if (isUnread(notification)) {
      setItems((current) =>
        current.map((item) =>
          item.notificationId === notification.notificationId
            ? { ...item, readAt: new Date().toISOString(), status: "READ" }
            : item,
        ),
      );
      setUnreadCount((current) => Math.max(0, current - 1));
      void markNotificationRead(notification.notificationId).catch(() => void loadNotifications());
    }

    if (canNavigate) {
      if (target.startsWith("http://") || target.startsWith("https://")) {
        window.open(target, "_blank", "noreferrer");
      } else {
        navigate(target);
      }
    }
  }

  async function handleMarkAllRead() {
    if (unreadCount === 0) return;
    setItems((current) =>
      current.map((item) => ({ ...item, readAt: item.readAt ?? new Date().toISOString(), status: "READ" })),
    );
    setUnreadCount(0);
    await markAllNotificationsRead().catch(() => void loadNotifications());
  }

  const buttonClassName =
    variant === "admin"
      ? "tw-relative tw-inline-flex tw-h-[54px] tw-w-[54px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-transparent tw-bg-transparent tw-text-[1.18rem] tw-text-slate-900 tw-transition hover:tw-border-slate-200 hover:tw-bg-slate-100"
      : "vm-customer-bell";

  return (
    <div className={variant === "admin" ? "tw-relative" : "vm-customer-action-wrap"} ref={rootRef}>
      <button
        aria-expanded={open}
        aria-label="Thông báo"
        className={cn(buttonClassName, open && variant === "admin" ? "tw-border-slate-200 tw-bg-white tw-shadow-[0_8px_20px_rgba(15,23,42,0.06)]" : "")}
        type="button"
        onClick={() => {
          setOpen((current) => !current);
          setActiveGroup(null);
          if (!open) void loadNotifications();
        }}
      >
        <i className="far fa-bell" />
        {unreadCount > 0 ? (
          variant === "admin" ? (
            <span className="tw-absolute tw-right-2.5 tw-top-2.5 tw-inline-flex tw-h-[17px] tw-min-w-[17px] tw-items-center tw-justify-center tw-rounded-full tw-bg-red-500 tw-px-1 tw-text-[0.58rem] tw-font-extrabold tw-leading-none tw-text-white">
              {badgeValue}
            </span>
          ) : (
            <b>{badgeValue}</b>
          )
        ) : null}
      </button>

      {open ? (
        <div
          className={cn(
            "tw-overflow-hidden",
            variant === "admin"
              ? "tw-absolute tw-right-0 tw-top-[calc(100%+12px)] tw-z-[1080] tw-w-[360px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]"
              : "vm-customer-popover vm-customer-notifications tw-w-[360px] tw-max-w-[calc(100vw-1rem)] !tw-p-0",
          )}
        >
          <div
            className="tw-flex tw-w-[200%] tw-transition-transform tw-duration-300 tw-ease-out"
            style={{ transform: activeGroup ? "translateX(-50%)" : "translateX(0)" }}
          >
            <section className="tw-w-1/2 tw-flex-none tw-p-2">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-px-3 tw-py-2">
                <div>
                  <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">Thông báo</h3>
                  <p className="tw-m-0 tw-mt-0.5 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                    {unreadCount > 0 ? `${unreadCount.toLocaleString("vi-VN")} chưa đọc` : "Tất cả đã đọc"}
                  </p>
                </div>
                <button
                  className="tw-whitespace-nowrap tw-rounded-vm-sm tw-border-0 tw-bg-brand-50 tw-px-2.5 tw-py-1.5 tw-text-[0.72rem] tw-font-extrabold tw-text-vm-primary hover:tw-bg-brand-100 disabled:tw-cursor-not-allowed disabled:tw-opacity-50"
                  disabled={unreadCount === 0}
                  type="button"
                  onClick={() => void handleMarkAllRead()}
                >
                  Đã đọc tất cả
                </button>
              </div>

              <div className="tw-grid tw-gap-1 tw-px-1 tw-pb-1">
                {notificationGroups.map((group) => {
                  const groupItems = groupedItems[group.key];
                  const groupUnread = groupItems.filter(isUnread).length;
                  const latest = groupItems[0];

                  return (
                    <button
                      className="tw-grid tw-min-h-[76px] tw-w-full tw-grid-cols-[42px_minmax(0,1fr)_18px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-2.5 tw-py-2.5 tw-text-left tw-transition hover:tw-bg-brand-50"
                      key={group.key}
                      type="button"
                      onClick={() => setActiveGroup(group.key)}
                    >
                      <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1rem]", group.tone)}>
                        <i className={group.icon} />
                      </span>
                      <span className="tw-min-w-0">
                        <span className="tw-flex tw-items-center tw-gap-2">
                          <strong className="tw-truncate tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{group.title}</strong>
                          {groupUnread > 0 ? (
                            <span className="tw-inline-flex tw-h-[18px] tw-min-w-[18px] tw-items-center tw-justify-center tw-rounded-full tw-bg-red-500 tw-px-1 tw-text-[0.62rem] tw-font-black tw-text-white">
                              {groupUnread > 99 ? "99+" : groupUnread}
                            </span>
                          ) : null}
                        </span>
                        <small className="tw-mt-1 tw-line-clamp-1 tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">
                          {latest ? latest.title : group.description}
                        </small>
                      </span>
                      <i className="fas fa-chevron-right tw-text-[0.74rem] tw-text-vm-slate-400" />
                    </button>
                  );
                })}

                {items.length === 0 ? (
                  <div className="tw-grid tw-place-items-center tw-gap-2 tw-px-4 tw-py-7 tw-text-center">
                    <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-50 tw-text-vm-slate-400">
                      <i className={loading ? "fas fa-spinner fa-spin" : "far fa-bell"} />
                    </span>
                    <p className="tw-m-0 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">
                      {loading ? "Đang tải thông báo..." : "Chưa có thông báo nào."}
                    </p>
                  </div>
                ) : null}
              </div>
            </section>

            <section className="tw-w-1/2 tw-flex-none tw-p-2">
              <div className="tw-flex tw-items-center tw-gap-3 tw-px-2 tw-py-2">
                <button
                  className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-25 tw-text-vm-slate-700 hover:tw-bg-vm-slate-100"
                  type="button"
                  aria-label="Quay lại danh mục thông báo"
                  onClick={() => setActiveGroup(null)}
                >
                  <i className="fas fa-chevron-left" />
                </button>
                <div className="tw-min-w-0">
                  <h3 className="tw-m-0 tw-truncate tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">{activeGroupMeta.title}</h3>
                  <p className="tw-m-0 tw-mt-0.5 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                    {activeItems.length.toLocaleString("vi-VN")} thông báo gần nhất
                  </p>
                </div>
              </div>

              <div className="tw-max-h-[390px] tw-overflow-y-auto tw-px-1 tw-pb-1">
                {activeItems.map((item) => {
                  const meta = typeMeta[item.notificationType] ?? typeMeta.SYSTEM_NOTICE;
                  const itemUnread = isUnread(item);

                  return (
                    <button
                      className={cn(
                        "tw-my-1 tw-grid tw-w-full tw-grid-cols-[40px_minmax(0,1fr)] tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-2.5 tw-py-2.5 tw-text-left tw-transition hover:tw-bg-brand-50",
                        itemUnread ? "tw-bg-brand-50/70" : "",
                      )}
                      key={item.notificationId}
                      type="button"
                      onClick={() => void handleItemClick(item)}
                    >
                      <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1rem]", meta.tone)}>
                        <i className={meta.icon} />
                      </span>
                      <span className="tw-min-w-0">
                        <span className="tw-flex tw-items-start tw-justify-between tw-gap-2">
                          <strong className="tw-line-clamp-1 tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">
                            {item.title}
                          </strong>
                          {itemUnread ? <span className="tw-mt-1 tw-h-2 tw-w-2 tw-flex-none tw-rounded-full tw-bg-vm-primary" /> : null}
                        </span>
                        <small className="tw-mt-1 tw-line-clamp-2 tw-text-[0.75rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
                          {item.message}
                        </small>
                        <span className="tw-mt-1.5 tw-block tw-text-[0.68rem] tw-font-bold tw-text-vm-slate-400">
                          {formatDateTime(item.createdAt ?? item.sentAt)}
                        </span>
                      </span>
                    </button>
                  );
                })}

                {activeItems.length === 0 ? (
                  <div className="tw-grid tw-place-items-center tw-gap-2 tw-px-4 tw-py-8 tw-text-center">
                    <span className={cn("tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-full", activeGroupMeta.tone)}>
                      <i className={activeGroupMeta.icon} />
                    </span>
                    <p className="tw-m-0 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">
                      Chưa có thông báo trong nhóm này.
                    </p>
                  </div>
                ) : null}
              </div>
            </section>
          </div>
        </div>
      ) : null}
    </div>
  );
}
