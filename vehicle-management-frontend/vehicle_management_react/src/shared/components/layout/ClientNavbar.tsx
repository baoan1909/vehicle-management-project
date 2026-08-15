import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";

import { canAccessCustomerRoute } from "@/app/routePermissions";
import { logoutCurrentUser } from "@/core/auth/logout";
import { useAuth } from "@/core/auth/useAuth";
import { preconnectKeycloakLoginOrigin, prepareKeycloakLoginUrl } from "@/features/auth/api/authApi";
import { NotificationBell } from "@/features/notifications/components/NotificationBell";
import { cn } from "@/lib/cn";
import { FullPageCarLoader } from "@/shared/components/ui/PageTransitionLoader";
import { DEFAULT_USER_AVATAR_URL, getApprovalStatusValue, getRoleLabel, getStatusMeta } from "@/shared/utils/accountStatus";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

const publicNavigation = [
  { label: "Giới thiệu", href: "/" },
  { label: "Bảng giá", href: "/pricing" },
  { label: "Hướng dẫn", href: "/guide" },
  { label: "Liên hệ", href: "/contact" },
];

const profilePanelClassName =
  "tw-absolute tw-right-0 tw-top-[calc(100%+12px)] tw-z-[1080] tw-w-[330px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]";

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
  const loginUrlPromiseRef = useRef<Promise<string> | null>(null);
  const profileRef = useRef<HTMLDivElement | null>(null);
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

  function prepareLoginRedirect() {
    preconnectKeycloakLoginOrigin();
    if (!loginUrlPromiseRef.current) {
      loginUrlPromiseRef.current = prepareKeycloakLoginUrl().catch((error) => {
        loginUrlPromiseRef.current = null;
        throw error;
      });
    }

    return loginUrlPromiseRef.current;
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
      const loginUrl = await prepareLoginRedirect();
      window.location.assign(loginUrl);
    } catch (error) {
      console.error(error);
      window.location.assign("/login");
    }
  }

  function handleLogout() {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    setProfileOpen(false);
    logoutCurrentUser(setUser);
  }

  return (
    <header className="tw-sticky tw-top-0 tw-z-[1050] tw-block tw-min-h-[72px] tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white/95 tw-shadow-[0_8px_24px_rgba(15,23,42,0.04)] tw-backdrop-blur-[16px]">
      {isLoginRedirecting ? <FullPageCarLoader label="Đang chuyển đến đăng nhập..." /> : null}
      <div className="tw-grid tw-min-h-[72px] tw-w-full tw-grid-cols-[auto_minmax(0,1fr)_auto] tw-items-center tw-gap-[2.1rem] tw-px-[1.15rem] max-[992px]:tw-grid-cols-[auto_auto_1fr] max-[992px]:tw-gap-3 max-[992px]:tw-px-4 max-[640px]:tw-grid-cols-[112px_42px_1fr] max-[640px]:tw-px-3">
        <Link to="/" className="tw-inline-flex tw-w-fit tw-items-center hover:tw-no-underline" aria-label="CoParking">
          <img className="tw-block tw-h-[62px] tw-w-28 tw-object-contain max-[992px]:tw-w-24 max-[640px]:tw-w-[88px]" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
        </Link>

        <button
          className="tw-hidden tw-h-[42px] tw-w-[42px] tw-place-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-slate-900 max-[992px]:tw-grid"
          type="button"
          onClick={() => setOpen((value) => !value)}
          aria-label="Mở menu điều hướng"
          aria-expanded={open}
        >
          <i className="fas fa-bars" />
        </button>

        <nav
          className={cn(
            "tw-flex tw-min-w-0 tw-items-center tw-justify-start tw-gap-[2.35rem] max-[992px]:tw-absolute max-[992px]:tw-left-4 max-[992px]:tw-right-4 max-[992px]:tw-top-[calc(100%+8px)] max-[992px]:tw-hidden max-[992px]:tw-flex-col max-[992px]:tw-items-stretch max-[992px]:tw-gap-0 max-[992px]:tw-rounded-vm-md max-[992px]:tw-border max-[992px]:tw-border-solid max-[992px]:tw-border-vm-slate-100 max-[992px]:tw-bg-white max-[992px]:tw-p-2 max-[992px]:tw-shadow-soft",
            open && "max-[992px]:tw-flex",
          )}
          aria-label="Điều hướng chính"
        >
          {publicNavigation.map((item, index) => {
            const active = item.href === "/" ? location.pathname === "/" : location.pathname === item.href;

            return (
              <Link
                key={`${item.label}-${item.href}-${index}`}
                to={item.href}
                className={cn(
                  "tw-relative tw-inline-flex tw-min-h-[72px] tw-items-center tw-p-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-700 tw-transition hover:tw-text-vm-primary hover:tw-no-underline max-[992px]:tw-min-h-[42px] max-[992px]:tw-rounded-vm-sm max-[992px]:tw-px-3",
                  active && "tw-text-vm-primary after:tw-absolute after:tw-bottom-[15px] after:tw-left-0 after:tw-right-0 after:tw-h-[3px] after:tw-rounded-full after:tw-bg-vm-primary max-[992px]:after:tw-hidden",
                )}
                onClick={() => setOpen(false)}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="tw-flex tw-items-center tw-justify-end tw-gap-3 tw-justify-self-end max-[640px]:tw-gap-2">
          {user ? (
            <>
              <NotificationBell variant="admin" />
              <div className="tw-relative" ref={profileRef}>
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
                  <div className={profilePanelClassName}>
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

                    <Link to="/customer/profile" className={profileItemClassName} onClick={() => setProfileOpen(false)}>
                      <HeaderItemIcon icon="fas fa-user-circle" />
                      <HeaderItemCopy title="Hồ sơ cá nhân" meta="Cập nhật thông tin tài khoản" />
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
                ) : null}
              </div>
            </>
          ) : (
            <>
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
    </header>
  );
}
