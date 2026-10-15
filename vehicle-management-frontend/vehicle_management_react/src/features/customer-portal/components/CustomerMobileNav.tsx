import { useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { canAccessCustomerRoute } from "@/app/routePermissions";
import { logoutCurrentUser } from "@/core/auth/logout";
import { useAuth } from "@/core/auth/useAuth";
import { cn } from "@/lib/cn";

type MobileNavItem = {
  icon: string;
  label: string;
  to: string;
};

const primaryNavItems: MobileNavItem[] = [
  { icon: "fas fa-home", label: "Tổng quan", to: "/customer/dashboard" },
  { icon: "fas fa-map-marker-alt", label: "Tìm bãi", to: "/parking-lots/nearby" },
  { icon: "far fa-calendar-check", label: "Vé", to: "/customer/subscriptions" },
  { icon: "fas fa-wallet", label: "Ví", to: "/customer/wallet" },
];

const overflowNavItems: MobileNavItem[] = [
  { icon: "far fa-user", label: "Hồ sơ", to: "/customer/profile" },
  { icon: "fas fa-motorcycle", label: "Xe của tôi", to: "/customer/vehicles" },
  { icon: "far fa-clock", label: "Lịch sử", to: "/customer/parking-history" },
  { icon: "far fa-question-circle", label: "Hỗ trợ", to: "/customer/support" },
  { icon: "far fa-comment-dots", label: "Chat hỗ trợ", to: "/customer/support/chat" },
];

export function CustomerMobileNav() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, setUser } = useAuth();
  const [moreOpen, setMoreOpen] = useState(false);
  const moreButtonRef = useRef<HTMLButtonElement | null>(null);
  const sheetRef = useRef<HTMLDivElement | null>(null);

  const visiblePrimary = primaryNavItems.filter((item) => canAccessCustomerRoute(user, item.to));
  const visibleOverflow = overflowNavItems.filter((item) => canAccessCustomerRoute(user, item.to));
  if (visiblePrimary.length === 0 && visibleOverflow.length === 0) {
    return null;
  }

  const isOverflowActive = visibleOverflow.some(
    (item) => location.pathname === item.to || location.pathname.startsWith(`${item.to}/`),
  );

  useEffect(() => {
    setMoreOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!moreOpen) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    sheetRef.current?.querySelector<HTMLElement>("a, button")?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMoreOpen(false);
        requestAnimationFrame(() => moreButtonRef.current?.focus());
        return;
      }

      if (event.key !== "Tab" || !sheetRef.current) return;
      const focusable = Array.from(sheetRef.current.querySelectorAll<HTMLElement>("a, button:not([disabled])"));
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

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [moreOpen]);

  function handleLogout() {
    setMoreOpen(false);
    logoutCurrentUser(setUser);
    navigate("/login");
  }

  return (
    <>
      <nav className="vm-customer-mobile-nav" aria-label="Điều hướng khách hàng trên di động">
        <ul className="vm-customer-mobile-nav-list">
          {visiblePrimary.slice(0, 4).map((item) => {
            const active = location.pathname === item.to || location.pathname.startsWith(`${item.to}/`);
            return (
              <li key={item.to}>
                <Link
                  to={item.to}
                  className="vm-customer-mobile-nav-link"
                  aria-current={active ? "page" : undefined}
                >
                  <i className={item.icon} aria-hidden="true" />
                  <span>{item.label}</span>
                </Link>
              </li>
            );
          })}
          <li>
            <button
              ref={moreButtonRef}
              type="button"
              className={cn("vm-customer-mobile-nav-link", isOverflowActive && "vm-customer-mobile-nav-active")}
              aria-expanded={moreOpen}
              aria-haspopup="dialog"
              aria-label="Mở thêm chức năng khách hàng"
              onClick={() => setMoreOpen((value) => !value)}
            >
              <i className="fas fa-ellipsis-h" aria-hidden="true" />
              <span>Thêm</span>
            </button>
          </li>
        </ul>
      </nav>

      {moreOpen ? (
        <>
          <button className="vm-customer-more-backdrop" type="button" aria-label="Đóng bảng chức năng" onClick={() => setMoreOpen(false)} />
          <div ref={sheetRef} className="vm-customer-more-sheet" data-open="true" role="dialog" aria-modal="true" aria-labelledby="vm-customer-more-title">
            <span className="vm-customer-more-handle" aria-hidden="true" />
            <header className="vm-customer-more-header">
              <span>
                <small>TIỆN ÍCH CỦA BẠN</small>
                <strong id="vm-customer-more-title">Truy cập nhanh</strong>
              </span>
              <button className="vm-customer-more-close" type="button" aria-label="Đóng" onClick={() => setMoreOpen(false)}>
                <i className="fas fa-times" aria-hidden="true" />
              </button>
            </header>
            <div className="vm-customer-more-grid">
              {visibleOverflow.map((item) => (
                <Link key={item.to} to={item.to} onClick={() => setMoreOpen(false)}>
                  <span><i className={item.icon} aria-hidden="true" /></span>
                  {item.label}
                </Link>
              ))}
            </div>
            <button className="vm-customer-more-logout" type="button" onClick={handleLogout}>
              <i className="fas fa-sign-out-alt" aria-hidden="true" />
              Đăng xuất
            </button>
          </div>
        </>
      ) : null}
    </>
  );
}
