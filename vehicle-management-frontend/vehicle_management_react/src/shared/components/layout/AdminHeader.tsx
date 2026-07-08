import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../core/auth/useAuth";
import { cn } from "@/lib/cn";

const searchSuggestions = ["Tìm thẻ xe", "Tra cứu khách hàng", "Kiểm tra xe đang trong bãi"];

const notifications = [
  {
    href: "/admin/support-center",
    icon: "fas fa-envelope",
    targetBlank: true,
    title: "Tin nhắn mới",
    meta: "4 cuộc trò chuyện cần phản hồi",
  },
  {
    href: "/admin/customer",
    icon: "fas fa-user-check",
    title: "Yêu cầu đăng ký",
    meta: "8 yêu cầu chờ xác nhận",
  },
  {
    href: "/admin/dashboard",
    icon: "fas fa-chart-line",
    title: "Báo cáo mới",
    meta: "3 báo cáo vừa được tạo",
  },
];

function getRoleLabel(role?: string) {
  switch (role) {
    case "ADMIN":
      return "Quản trị viên";
    case "EMPLOYEE":
      return "Nhân viên";
    case "CUSTOMER":
      return "Khách hàng";
    default:
      return "Người dùng";
  }
}

const panelClassName =
  "tw-absolute tw-right-0 tw-top-[calc(100%+12px)] tw-z-[1080] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]";

const itemClassName =
  "tw-my-1 tw-grid tw-grid-cols-[40px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-rounded-vm-md tw-px-2.5 tw-py-2 tw-text-left tw-text-slate-900 tw-transition hover:tw-bg-brand-50 hover:tw-text-slate-900 hover:tw-no-underline";

function HeaderItemIcon({ icon }: { icon: string }) {
  return (
    <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.12rem] tw-text-vm-primary">
      <i className={icon} />
    </span>
  );
}

function HeaderItemCopy({ title, meta }: { title: string; meta: string }) {
  return (
    <span className="tw-grid tw-min-w-0 tw-gap-1">
      <strong className="tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">{title}</strong>
      <small className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{meta}</small>
    </span>
  );
}

export function AdminHeader() {
  const navigate = useNavigate();
  const { user, setUser } = useAuth();
  const [searchValue, setSearchValue] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement | null>(null);
  const notificationsRef = useRef<HTMLDivElement | null>(null);
  const profileRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;

      if (searchRef.current && !searchRef.current.contains(target)) setSearchOpen(false);
      if (notificationsRef.current && !notificationsRef.current.contains(target)) setNotificationsOpen(false);
      if (profileRef.current && !profileRef.current.contains(target)) setProfileOpen(false);
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  function handleSuggestionSelect(value: string) {
    setSearchValue(value);
    setSearchOpen(false);
  }

  function handleLogout() {
    setUser(null);
    setProfileOpen(false);
    navigate("/login");
  }

  const displayName = user?.fullName ?? user?.username ?? "Người dùng";
  const roleLabel = getRoleLabel(user?.role);

  return (
    <header className="tw-fixed tw-inset-x-0 tw-top-0 tw-z-[1050] tw-border-0 tw-border-b tw-border-solid tw-border-slate-200/95 tw-bg-white/95 tw-shadow-[0_10px_28px_rgba(15,23,42,0.08)] tw-backdrop-blur-[14px]">
      <div className="tw-grid tw-min-h-[72px] tw-grid-cols-[240px_minmax(280px,1fr)_auto] tw-items-center tw-gap-6 tw-px-6 max-[768px]:tw-grid-cols-[minmax(0,1fr)_auto] max-[768px]:tw-gap-4 max-[768px]:tw-px-4">
        <div className="tw-min-w-0">
          <Link to="/admin/dashboard" className="tw-flex tw-min-w-0 tw-items-center tw-gap-3 tw-text-slate-900 hover:tw-text-slate-900 hover:tw-no-underline">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center">
              <img className="tw-block tw-h-12 tw-w-12 tw-object-contain" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
            </span>
            <span className="tw-flex tw-min-w-0 tw-flex-col max-[768px]:tw-hidden">
              <strong className="tw-text-[1.35rem] tw-font-extrabold tw-leading-none tw-text-vm-primary">CoParking</strong>
              <small className="tw-mt-1 tw-text-[0.74rem] tw-font-bold tw-uppercase tw-tracking-[0.08em] tw-text-vm-slate-500">Admin Portal</small>
            </span>
          </Link>
        </div>

        <div className="tw-flex tw-justify-center max-[768px]:tw-hidden">
          <div ref={searchRef} className="tw-relative tw-w-[min(100%,520px)]">
            <div
              className={cn(
                "tw-flex tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-px-4 tw-shadow-[0_8px_18px_rgba(15,23,42,0.04)] tw-transition",
                searchOpen ? "tw-border-brand-200 tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]" : "",
              )}
            >
              <i className="fas fa-search tw-text-vm-slate-500" />
              <input
                className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.9rem] tw-font-semibold tw-text-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
                type="search"
                value={searchValue}
                placeholder="Tìm khách hàng, biển số, thẻ xe..."
                aria-label="Tìm kiếm"
                onFocus={() => setSearchOpen(true)}
                onChange={(event) => setSearchValue(event.target.value)}
              />
              <span className="tw-rounded-vm-sm tw-bg-slate-100 tw-px-2 tw-py-1 tw-text-[0.7rem] tw-font-extrabold tw-text-vm-slate-500">Ctrl+K</span>
            </div>

            {searchOpen ? (
              <div className="tw-absolute tw-left-0 tw-right-0 tw-top-[calc(100%+10px)] tw-z-[1080] tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-2 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]">
                <div className="tw-px-3 tw-py-2 tw-text-[0.78rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Gợi ý tìm kiếm</div>
                <div className="tw-grid tw-gap-1">
                  {searchSuggestions.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      className="tw-flex tw-min-h-10 tw-w-full tw-items-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-white tw-px-3 tw-text-left tw-text-[0.88rem] tw-font-bold tw-text-vm-slate-700 hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                      onClick={() => handleSuggestionSelect(suggestion)}
                    >
                      <i className="fas fa-arrow-up-right-from-square" />
                      <span>{suggestion}</span>
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        </div>

        <div className="tw-flex tw-items-center tw-justify-end tw-gap-[1.1rem]">
          <div ref={notificationsRef} className="tw-relative">
            <button
              type="button"
              className={cn(
                "tw-relative tw-inline-flex tw-h-[54px] tw-w-[54px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-transparent tw-bg-transparent tw-text-[1.18rem] tw-text-slate-900 tw-transition hover:tw-border-slate-200 hover:tw-bg-white hover:tw-shadow-[0_10px_24px_rgba(15,23,42,0.08)]",
                "hover:tw-bg-slate-100 hover:tw-shadow-none",
                notificationsOpen ? "tw-border-slate-200 tw-bg-white tw-shadow-[0_8px_20px_rgba(15,23,42,0.06)]" : "",
              )}
              onClick={() => {
                setNotificationsOpen((current) => !current);
                setProfileOpen(false);
              }}
              aria-label="Thông báo"
            >
              <i className="far fa-bell tw-text-[1.3rem]" />
              <span className="tw-absolute tw-right-2.5 tw-top-2.5 tw-inline-flex tw-h-[17px] tw-min-w-[17px] tw-items-center tw-justify-center tw-rounded-full tw-bg-red-500 tw-px-1 tw-text-[0.58rem] tw-font-extrabold tw-leading-none tw-text-white">3</span>
            </button>

            {notificationsOpen ? (
              <div className={cn(panelClassName, "tw-w-[320px]")}>
                <div className="tw-px-3 tw-py-2 tw-text-[0.78rem] tw-font-extrabold tw-uppercase tw-tracking-[0.04em] tw-text-vm-slate-500">Thông báo</div>
                {notifications.map((item) => (
                  <Link
                    key={item.title}
                    to={item.href}
                    className={itemClassName}
                    target={item.targetBlank ? "_blank" : undefined}
                    rel={item.targetBlank ? "noreferrer" : undefined}
                    onClick={() => setNotificationsOpen(false)}
                  >
                    <HeaderItemIcon icon={item.icon} />
                    <HeaderItemCopy title={item.title} meta={item.meta} />
                  </Link>
                ))}
              </div>
            ) : null}
          </div>

          <div ref={profileRef} className="tw-relative">
            <button
              type="button"
              className={cn(
                "tw-flex tw-min-h-12 tw-w-auto tw-min-w-[210px] tw-items-center tw-gap-3 tw-rounded-full tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-px-3 tw-py-1 tw-text-left tw-transition hover:tw-bg-brand-50",
                profileOpen ? "tw-bg-brand-50 tw-shadow-[0_8px_20px_rgba(37,99,235,0.08)]" : "",
              )}
              onClick={() => {
                setProfileOpen((current) => !current);
                setNotificationsOpen(false);
              }}
              aria-label={`Mở hồ sơ ${displayName}`}
            >
              <img src={user?.avatarUrl} alt={displayName} className="tw-h-9 tw-w-9 tw-flex-shrink-0 tw-rounded-full tw-object-cover" />
              <span className="tw-min-w-0 tw-flex-1">
                <strong className="tw-block tw-whitespace-nowrap tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">{displayName}</strong>
              </span>
            </button>

            {profileOpen ? (
              <div className={cn(panelClassName, "tw-w-[330px]")}>
                <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-bg-brand-50 tw-p-3">
                  <img src={user?.avatarUrl} alt={displayName} className="tw-h-12 tw-w-12 tw-rounded-full tw-object-cover" />
                  <div className="tw-min-w-0">
                    <strong className="tw-block tw-truncate tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">{displayName}</strong>
                    <small className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">{roleLabel}</small>
                  </div>
                </div>

                <Link to="/admin/profile" className={itemClassName} onClick={() => setProfileOpen(false)}>
                  <HeaderItemIcon icon="fas fa-user-circle" />
                  <HeaderItemCopy title="Thông tin tài khoản" meta="Cập nhật hồ sơ quản trị" />
                </Link>

                <button
                  type="button"
                  className={cn(itemClassName, "tw-w-full tw-border-0 tw-bg-white")}
                  onClick={() => {
                    setProfileOpen(false);
                    navigate("/admin/profile?action=change-password");
                  }}
                >
                  <HeaderItemIcon icon="fas fa-key" />
                  <HeaderItemCopy title="Đổi mật khẩu" meta="Cập nhật mật khẩu đăng nhập nội bộ" />
                </button>

                <Link to="/contact" className={itemClassName} onClick={() => setProfileOpen(false)}>
                  <HeaderItemIcon icon="fas fa-question-circle" />
                  <HeaderItemCopy title="Trợ giúp" meta="Xem hướng dẫn và liên hệ hỗ trợ" />
                </Link>

                <button type="button" className={cn(itemClassName, "tw-w-full tw-border-0 tw-bg-white")} onClick={handleLogout}>
                  <HeaderItemIcon icon="fas fa-sign-out-alt" />
                  <HeaderItemCopy title="Đăng xuất" meta="Thoát khỏi phiên làm việc hiện tại" />
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
