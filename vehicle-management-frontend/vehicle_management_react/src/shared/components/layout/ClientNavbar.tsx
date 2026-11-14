import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";

import { canAccessCustomerRoute, getProfileRoute } from "@/app/routePermissions";
import { logoutCurrentUser } from "@/core/auth/logout";
import { useAuth } from "@/core/auth/useAuth";
import { preconnectKeycloakLoginOrigin } from "@/features/auth/api/authApi";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";
import { ChatInboxButton } from "@/features/support/components/ChatInboxButton";
import { cn } from "@/lib/cn";
import { FullPageCarLoader } from "@/shared/components/ui/PageTransitionLoader";
import { DEFAULT_USER_AVATAR_URL, getApprovalStatusValue, getRoleLabel, getStatusMeta } from "@/shared/utils/accountStatus";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

const publicNavigation = [
  { label: "Giới thiệu", href: "/" },
  { label: "Tìm bãi", href: "/parking-lots/nearby" },
  { label: "Bảng giá", href: "/pricing" },
  { label: "Hướng dẫn", href: "/guide" },
  { label: "Liên hệ", href: "/contact" },
];

const profilePanelClassName =
  "vm-profile-popover tw-absolute tw-right-0 tw-top-[calc(100%+12px)] tw-z-[1080] tw-w-[330px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]";

const profileItemClassName =
  "tw-my-1 tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-rounded-vm-md tw-px-2.5 tw-py-2 tw-text-left tw-text-slate-900 tw-transition hover:tw-bg-brand-50 hover:tw-text-slate-900 hover:tw-no-underline";

function HeaderItemIcon({ icon }: { icon: string }) {
  return (
    <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.12rem] tw-text-vm-primary">
      <i className={icon} />
    </span>
  );
}

function HeaderItemCopy({ meta, title }: { meta: string; title: string }) {
  return (
    <span className="tw-grid tw-min-w-0 tw-gap-1">
      <strong className="tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">{title}</strong>
      <small className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{meta}</small>
    </span>
  );
}

function StatusBadge({ status }: { status?: string }) {
  const meta = getStatusMeta(status);

  return (
    <span className={cn("tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-extrabold tw-ring-1", meta.className)}>
      <span className={cn("tw-h-1.5 tw-w-1.5 tw-rounded-full", meta.dotClassName)} />
      {meta.label}
    </span>
  );
}

function UserAvatar({
  alt,
  className,
  status,
  src,
}: {
  alt: string;
  className: string;
  status?: string;
  src?: string;
}) {
  const meta = getStatusMeta(status);

  return (
    <span className="tw-relative tw-inline-flex tw-flex-shrink-0">
      <img
        src={src || DEFAULT_USER_AVATAR_URL}
        alt={alt}
        className={cn("tw-rounded-full tw-object-cover", className)}
        onError={(event) => {
          event.currentTarget.src = DEFAULT_USER_AVATAR_URL;
        }}
      />
      <span
        className={cn(
          "tw-absolute tw-bottom-0 tw-right-0 tw-inline-flex tw-h-3 tw-w-3 tw-items-center tw-justify-center tw-rounded-full tw-border-2 tw-border-white",
          meta.dotClassName,
        )}
        aria-label={meta.label}
        title={meta.label}
      />
    </span>
  );
}

export function ClientNavbar() {
  const { user, setUser } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [isLoginRedirecting, setIsLoginRedirecting] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const profileRef = useRef<HTMLDivElement | null>(null);
  const menuButtonRef = useRef<HTMLButtonElement | null>(null);
  const mobileDrawerRef = useRef<HTMLElement | null>(null);
  const usernameLabel = user?.username?.trim() || user?.email?.trim() || "";
  const displayName = user?.fullName?.trim() || usernameLabel || "Khách hàng";
  const roleLabel = getRoleLabel(user?.role, user?.roleLabel);
  const approvalStatus = getApprovalStatusValue(user);
  const accountStatus = user?.accountStatus;
  const avatarUrl = resolvePublicMediaUrl(user?.avatarUrl) || DEFAULT_USER_AVATAR_URL;

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (profileRef.current && !profileRef.current.contains(target)) {
        setProfileOpen(false);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  useEffect(() => {
    if (!profileOpen) return undefined;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setProfileOpen(false);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [profileOpen]);

  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!open) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    mobileDrawerRef.current?.querySelector<HTMLElement>("a, button")?.focus();

    function handleDrawerKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        requestAnimationFrame(() => menuButtonRef.current?.focus());
        return;
      }

      if (event.key !== "Tab" || !mobileDrawerRef.current) return;
      const focusable = Array.from(mobileDrawerRef.current.querySelectorAll<HTMLElement>("a, button:not([disabled])"));
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleDrawerKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleDrawerKeyDown);
    };
  }, [open]);

  function prepareLoginRedirect() {
    preconnectKeycloakLoginOrigin();
  }

  useEffect(() => {
    if (user) return;

    const prepare = () => {
      void prepareLoginRedirect();
    };
    const preloadTimerId = window.setTimeout(prepare, 300);

    return () => {
      window.clearTimeout(preloadTimerId);
    };
  }, [user]);

  async function handleLoginRedirect() {
    if (isLoginRedirecting) return;
    setIsLoginRedirecting(true);

    try {
      prepareLoginRedirect();
      window.location.assign("/login");
    } catch (error) {
      console.error(error);
      window.location.assign("/login");
    }
  }

  function handleLogout() {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    setProfileOpen(false);
    setOpen(false);
    logoutCurrentUser(setUser);
  }

  return (
    <header className="tw-sticky tw-top-0 tw-z-[1050] tw-block tw-min-h-[72px] tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white/95 tw-shadow-[0_8px_24px_rgba(15,23,42,0.04)] tw-backdrop-blur-[16px]">
      {isLoginRedirecting ? <FullPageCarLoader label="Đang chuyển đến đăng nhập..." /> : null}
      <div className="vm-client-header-row tw-grid tw-min-h-[72px] tw-w-full tw-grid-cols-[auto_minmax(0,1fr)_auto] tw-items-center tw-gap-[2.1rem] tw-px-[1.15rem] max-[992px]:tw-grid-cols-[auto_minmax(0,1fr)_auto] max-[992px]:tw-gap-2 max-[992px]:tw-px-4 max-[640px]:tw-px-3">
        <Link to="/" className="tw-inline-flex tw-w-fit tw-items-center hover:tw-no-underline" aria-label="CoParking">
          <img className="tw-block tw-h-[62px] tw-w-28 tw-object-contain max-[992px]:tw-w-24 max-[640px]:tw-w-[88px]" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
        </Link>

        <button
          ref={menuButtonRef}
          className="vm-client-menu-button tw-hidden tw-h-[44px] tw-w-[44px] tw-shrink-0 tw-place-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-slate-900 max-[992px]:tw-grid"
          type="button"
          onClick={() => setOpen((value) => !value)}
          aria-label="Mở menu điều hướng"
          aria-expanded={open}
          aria-controls="vm-client-mobile-drawer"
        >
          <i className="fas fa-bars" />
        </button>

        <nav
          className={cn(
            "vm-client-desktop-nav tw-flex tw-min-w-0 tw-items-center tw-justify-start tw-gap-[2.35rem] max-[992px]:tw-hidden",
          )}
          aria-label="Điều hướng chính"
        >
          {publicNavigation.map((item, index) => {
            const active = item.href === "/" ? location.pathname === "/" : location.pathname === item.href;

            return (
              <Link
                key={`${item.label}-${item.href}-${index}`}
                to={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "tw-relative tw-inline-flex tw-min-h-[44px] tw-items-center tw-rounded-vm-md tw-px-5 tw-text-[0.92rem] tw-font-extrabold tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary hover:tw-no-underline max-[992px]:tw-min-h-[42px] max-[992px]:tw-w-full",
                  active
                    ? "tw-bg-brand-50 tw-px-7 tw-text-vm-primary after:tw-absolute after:tw-bottom-[7px] after:tw-left-7 after:tw-right-7 after:tw-h-[3px] after:tw-rounded-full after:tw-bg-vm-primary after:tw-content-[''] max-[992px]:after:tw-hidden"
                    : "tw-text-vm-slate-700",
                )}
                onClick={() => setOpen(false)}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="vm-client-header-actions tw-flex tw-min-w-0 tw-items-center tw-justify-end tw-gap-3 tw-justify-self-end max-[640px]:tw-gap-1">
          {user ? (
            <>
              <span className="vm-header-chat-action"><ChatInboxButton /></span>
              <NotificationBell variant="admin" />
              <div className="vm-profile-popover-root tw-relative" ref={profileRef}>
                <button
                  type="button"
                  className={cn(
                    "tw-flex tw-min-h-12 tw-w-auto tw-min-w-[238px] tw-items-center tw-gap-3 tw-rounded-full tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-px-3 tw-py-1 tw-text-left tw-transition hover:tw-bg-brand-50 max-[992px]:tw-min-w-0",
                    profileOpen ? "tw-bg-brand-50 tw-shadow-[0_8px_20px_rgba(37,99,235,0.08)]" : "",
                  )}
                  aria-label={`Mở hồ sơ ${displayName}`}
                  aria-expanded={profileOpen}
                  onClick={() => setProfileOpen((value) => !value)}
                >
                  <UserAvatar src={avatarUrl} alt={displayName} status={approvalStatus} className="tw-h-9 tw-w-9" />
                  <span className="tw-min-w-0 tw-flex-1 max-[992px]:tw-hidden">
                    <strong className="tw-block tw-truncate tw-text-[0.92rem] tw-font-extrabold tw-leading-tight tw-text-slate-900">{displayName}</strong>
                    <small className="tw-mt-0.5 tw-block tw-truncate tw-text-[0.74rem] tw-font-bold tw-leading-tight tw-text-vm-slate-500">{roleLabel}</small>
                  </span>
                  <i className="fas fa-chevron-down tw-text-[0.72rem] tw-text-vm-slate-500 max-[992px]:tw-hidden" />
                </button>

                {profileOpen ? (
                  <>
                  <button className="vm-header-popover-backdrop" type="button" aria-label="Đóng menu hồ sơ" onClick={() => setProfileOpen(false)} />
                  <div className={profilePanelClassName} role="dialog" aria-label="Menu hồ sơ">
                    <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-bg-brand-50 tw-p-3">
                      <UserAvatar src={avatarUrl} alt={displayName} status={approvalStatus} className="tw-h-12 tw-w-12" />
                      <div className="tw-min-w-0">
                        <strong className="tw-block tw-truncate tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">{displayName}</strong>
                        <small className="tw-block tw-truncate tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">{roleLabel}</small>
                      </div>
                    </div>

                    <div className="tw-my-2 tw-grid tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
                      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                        <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Tên tài khoản</span>
                        <span className="tw-min-w-0 tw-truncate tw-text-right tw-text-[0.8rem] tw-font-black tw-text-vm-slate-900">{usernameLabel || displayName}</span>
                      </div>
                      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                        <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Trạng thái duyệt</span>
                        <StatusBadge status={approvalStatus} />
                      </div>
                      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                        <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">Tài khoản</span>
                        <StatusBadge status={accountStatus} />
                      </div>
                    </div>

                    {canAccessCustomerRoute(user, "/customer/dashboard") ? (
                      <Link to="/customer/dashboard" className={profileItemClassName} onClick={() => setProfileOpen(false)}>
                        <HeaderItemIcon icon="fas fa-tachometer-alt" />
                        <HeaderItemCopy title="Tổng quan" meta="Xem nhanh bãi xe và dịch vụ của bạn" />
                      </Link>
                    ) : null}

                    <Link to={getProfileRoute(user)} className={profileItemClassName} onClick={() => setProfileOpen(false)}>
                      <HeaderItemIcon icon="fas fa-user-circle" />
                      <HeaderItemCopy
                        title="Hồ sơ cá nhân"
                        meta="Cập nhật thông tin tài khoản"
                      />
                    </Link>

                    {canAccessCustomerRoute(user, "/customer/support") ? (
                      <Link to="/customer/support" className={profileItemClassName} onClick={() => setProfileOpen(false)}>
                        <HeaderItemIcon icon="fas fa-question-circle" />
                        <HeaderItemCopy title="Hỗ trợ" meta="Gửi yêu cầu và theo dõi phản hồi" />
                      </Link>
                    ) : null}

                    <button
                      type="button"
                      className={cn(profileItemClassName, "tw-w-full tw-border-0 tw-bg-white", isLoggingOut ? "tw-cursor-wait tw-opacity-75" : "")}
                      disabled={isLoggingOut}
                      aria-busy={isLoggingOut}
                      onClick={handleLogout}
                    >
                      <HeaderItemIcon icon={isLoggingOut ? "fas fa-spinner fa-spin" : "fas fa-sign-out-alt"} />
                      <HeaderItemCopy title={isLoggingOut ? "Đang đăng xuất" : "Đăng xuất"} meta="Thoát khỏi phiên làm việc hiện tại" />
                    </button>
                  </div>
                  </>
                ) : null}
              </div>
            </>
          ) : (
            <>
              <Link className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-vm-primary tw-bg-vm-primary tw-px-4 tw-text-[0.84rem] tw-font-black tw-text-white tw-transition hover:tw-border-vm-primary-hover hover:tw-bg-vm-primary-hover hover:tw-text-white hover:tw-no-underline max-[640px]:tw-min-h-9 max-[640px]:tw-px-3 max-[640px]:tw-text-[0.76rem]" to="/become-a-partner">Trở thành đối tác</Link>
              <button
                className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-vm-slate-200 tw-bg-white tw-px-4 tw-text-[0.84rem] tw-font-black tw-text-slate-900 tw-transition hover:tw-border-brand-200 hover:tw-bg-brand-50 hover:tw-text-vm-primary hover:tw-no-underline disabled:tw-cursor-wait disabled:tw-opacity-75 max-[640px]:tw-min-h-9 max-[640px]:tw-px-3 max-[640px]:tw-text-[0.76rem]"
                type="button"
                disabled={isLoginRedirecting}
                aria-busy={isLoginRedirecting}
                onFocus={prepareLoginRedirect}
                onPointerEnter={prepareLoginRedirect}
                onClick={handleLoginRedirect}
              >
                Đăng nhập
              </button>
              <Link className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-950 tw-bg-slate-950 tw-px-4 tw-text-[0.84rem] tw-font-black tw-text-white tw-transition hover:tw-border-brand-700 hover:tw-bg-brand-700 hover:tw-text-white hover:tw-no-underline max-[640px]:tw-min-h-9 max-[640px]:tw-px-3 max-[640px]:tw-text-[0.76rem]" to="/register">Đăng ký</Link>
            </>
          )}
        </div>
      </div>
      <button
        className="vm-client-drawer-backdrop"
        data-open={open ? "true" : "false"}
        type="button"
        aria-label="Đóng menu điều hướng"
        tabIndex={open ? 0 : -1}
        onClick={() => setOpen(false)}
      />
      <aside
        ref={mobileDrawerRef}
        id="vm-client-mobile-drawer"
        className="vm-client-mobile-drawer"
        data-open={open ? "true" : "false"}
        role="dialog"
        aria-modal="true"
        aria-hidden={!open}
        aria-labelledby="vm-client-mobile-drawer-title"
      >
        <header>
          <span>
            <small>ĐIỀU HƯỚNG</small>
            <strong id="vm-client-mobile-drawer-title">Khám phá CoParking</strong>
          </span>
          <button type="button" aria-label="Đóng menu" onClick={() => setOpen(false)}>
            <i className="fas fa-times" aria-hidden="true" />
          </button>
        </header>
        <nav aria-label="Khám phá CoParking">
          {publicNavigation.map((item) => {
            const active = item.href === "/" ? location.pathname === "/" : location.pathname === item.href;
            return (
              <Link key={item.href} to={item.href} aria-current={active ? "page" : undefined} onClick={() => setOpen(false)}>
                <span><i className={item.href === "/" ? "far fa-building" : item.href === "/parking-lots/nearby" ? "fas fa-map-marker-alt" : item.href === "/pricing" ? "fas fa-tags" : item.href === "/guide" ? "far fa-compass" : "far fa-envelope"} aria-hidden="true" /></span>
                {item.label}
                <i className="fas fa-chevron-right" aria-hidden="true" />
              </Link>
            );
          })}
        </nav>
        {user ? (
          <section aria-label="Tài khoản">
            <small>TÀI KHOẢN</small>
            <Link to="/customer/profile" onClick={() => setOpen(false)}><i className="far fa-user" aria-hidden="true" />Hồ sơ cá nhân</Link>
            {canAccessCustomerRoute(user, "/customer/support") ? <Link to="/customer/support" onClick={() => setOpen(false)}><i className="far fa-life-ring" aria-hidden="true" />Hỗ trợ</Link> : null}
            <button type="button" disabled={isLoggingOut} onClick={handleLogout}><i className="fas fa-sign-out-alt" aria-hidden="true" />{isLoggingOut ? "Đang đăng xuất..." : "Đăng xuất"}</button>
          </section>
        ) : (
          <section aria-label="Tài khoản">
            <small>TÀI KHOẢN</small>
            <button type="button" disabled={isLoginRedirecting} onClick={handleLoginRedirect}><i className="fas fa-sign-in-alt" aria-hidden="true" />Đăng nhập</button>
            <Link to="/register" onClick={() => setOpen(false)}><i className="far fa-user-circle" aria-hidden="true" />Đăng ký</Link>
          </section>
        )}
      </aside>
    </header>
  );
}
