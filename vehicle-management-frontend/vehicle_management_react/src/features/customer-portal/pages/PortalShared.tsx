import { Link, useLocation } from "react-router-dom";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { logoutCurrentUser } from "@/core/auth/logout";
import { useAuth } from "@/core/auth/useAuth";

export const publicContactItems = [
  { icon: "fas fa-phone-alt", title: "Hotline", value: "1900 1234", note: "Hỗ trợ 24/7" },
  { icon: "far fa-envelope", title: "Email", value: "support@coparking.vn", note: "Phản hồi trong 24h" },
  { icon: "fas fa-map-marker-alt", title: "Địa chỉ", value: "123 Trần Hưng Đạo, Quận 1, TP. Hồ Chí Minh", note: "" },
  { icon: "far fa-clock", title: "Giờ hỗ trợ", value: "06:00 - 22:00", note: "Tất cả các ngày" },
];

const customerNavItems = [
  { label: "Tổng quan", to: "/customer/dashboard", icon: "fas fa-home" },
  { label: "Hồ sơ", to: "/customer/profile", icon: "far fa-user" },
  { label: "Xe của tôi", to: "/customer/vehicles", icon: "fas fa-motorcycle" },
  { label: "Vé tháng", to: "/customer/subscriptions", icon: "far fa-calendar-check" },
  { label: "Lịch sử gửi xe", to: "/customer/parking-history", icon: "far fa-clock" },
  { label: "Hỗ trợ", to: "/customer/support", icon: "far fa-question-circle" },
];

export function PublicHero({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <header className="vm-public-hero">
      <h1>{title}</h1>
      <p>{subtitle}</p>
    </header>
  );
}

export function PublicContactStrip() {
  return (
    <section className="vm-public-contact-strip">
      <h2>Liên hệ với chúng tôi</h2>
      <div className="vm-public-contact-grid">
        {publicContactItems.map((item) => (
          <div className="vm-public-contact-item" key={item.title}>
            <span className="vm-public-contact-icon"><i className={item.icon} /></span>
            <span>
              <strong>{item.title}</strong>
              <b>{item.value}</b>
              {item.note && <small>{item.note}</small>}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}

export function PublicFooter() {
  return (
    <footer className="vm-public-footer">
      <span>© 2024 CoParking. All rights reserved.</span>
      <nav>
        <a href="#">Chính sách bảo mật</a>
        <span>|</span>
        <a href="#">Điều khoản sử dụng</a>
      </nav>
    </footer>
  );
}

export function VehicleTabs() {
  return (
    <div className="vm-vehicle-tabs">
      <button className="active" type="button"><i className="fas fa-motorcycle" /> Xe máy</button>
      <button type="button"><i className="fas fa-car" /> Ô tô</button>
      <button type="button"><i className="far fa-ellipsis-h" /> Xe khác</button>
    </div>
  );
}

export function StatusPill({ children, tone = "green" }: { children: ReactNode; tone?: "green" | "blue" | "orange" | "red" | "gray" | "purple" }) {
  return <span className={`vm-status-pill vm-status-${tone}`}>{children}</span>;
}

export function CustomerPortalLayout({ children }: { children: ReactNode }) {
  const location = useLocation();
  const { user, setUser } = useAuth();
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const notificationsRef = useRef<HTMLDivElement | null>(null);
  const profileRef = useRef<HTMLDivElement | null>(null);
  const displayName = user?.fullName || user?.username || "Khách hàng";
  const initials = displayName
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((word) => word[0])
    .join("")
    .toUpperCase() || "KH";

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node;
      if (notificationsRef.current && !notificationsRef.current.contains(target)) setNotificationsOpen(false);
      if (profileRef.current && !profileRef.current.contains(target)) setProfileOpen(false);
    }

    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, []);

  function handleLogout() {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    setProfileOpen(false);
    logoutCurrentUser(setUser);
  }

  return (
    <div className="vm-customer-shell">
      <header className="vm-customer-topbar">
        <Link to="/customer/dashboard" className="vm-customer-logo">CoParking</Link>
        <button className="vm-customer-menu" type="button" aria-label="Menu"><i className="fas fa-bars" /></button>
        <div className="vm-customer-actions">
          <div className="vm-customer-action-wrap" ref={notificationsRef}>
            <button
              className="vm-customer-bell"
              type="button"
              aria-expanded={notificationsOpen}
              aria-label="Thông báo"
              onClick={() => {
                setNotificationsOpen((open) => !open);
                setProfileOpen(false);
              }}
            >
              <i className="far fa-bell" />
              <b>3</b>
            </button>
            {notificationsOpen ? (
              <div className="vm-customer-popover vm-customer-notifications">
                <h3>Thông báo</h3>
                <p>Chưa có thông báo mới từ hệ thống.</p>
                <Link to="/customer/support" onClick={() => setNotificationsOpen(false)}>Mở trung tâm hỗ trợ</Link>
              </div>
            ) : null}
          </div>
          <div className="vm-customer-action-wrap" ref={profileRef}>
            <button
              className="vm-customer-user"
              type="button"
              aria-expanded={profileOpen}
              onClick={() => {
                setProfileOpen((open) => !open);
                setNotificationsOpen(false);
              }}
            >
              <span className="vm-customer-avatar">{initials}</span>
          <span>Xin chào,<strong>{displayName}</strong></span>
              <i className="fas fa-chevron-down" />
            </button>
            {profileOpen ? (
              <div className="vm-customer-popover vm-customer-profile-menu">
                <Link to="/customer/profile" onClick={() => setProfileOpen(false)}><i className="far fa-user" /> Hồ sơ cá nhân</Link>
                <Link to="/customer/support" onClick={() => setProfileOpen(false)}><i className="far fa-question-circle" /> Hỗ trợ</Link>
                <button type="button" disabled={isLoggingOut} aria-busy={isLoggingOut} onClick={handleLogout}>
                  <i className={isLoggingOut ? "fas fa-spinner fa-spin" : "fas fa-sign-out-alt"} /> {isLoggingOut ? "Đang đăng xuất" : "Đăng xuất"}
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>
      <div className="vm-customer-body">
        <aside className="vm-customer-sidebar">
          {customerNavItems.map((item) => {
            const active = location.pathname === item.to;
            return (
              <Link className={`vm-customer-nav-item ${active ? "active" : ""}`} to={item.to} key={item.to}>
                <i className={item.icon} />
                <span>{item.label}</span>
              </Link>
            );
          })}
        </aside>
        <main className="vm-customer-content">{children}</main>
      </div>
    </div>
  );
}

export function CustomerPageHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle: string;
  action?: ReactNode;
}) {
  return (
    <div className="vm-customer-page-header">
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      {action}
    </div>
  );
}

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="vm-field-lite">
      <span>{label}</span>
      {children}
    </label>
  );
}

export function StatCard({
  icon,
  label,
  value,
  note,
  tone = "blue",
}: {
  icon: string;
  label: string;
  value: string;
  note?: ReactNode;
  tone?: "blue" | "green" | "orange" | "red" | "purple";
}) {
  return (
    <article className="vm-stat-card">
      <span className={`vm-stat-icon vm-stat-${tone}`}><i className={icon} /></span>
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
        {note && <small>{note}</small>}
      </div>
    </article>
  );
}

type PaginationLiteProps = {
  currentPage?: number;
  onPageChange?: (page: number) => void;
  onPageSizeChange?: (pageSize: number) => void;
  pageSize?: number;
  totalRecords?: number;
};

function formatCount(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value);
}

export function PaginationLite({
  currentPage = 1,
  onPageChange,
  onPageSizeChange,
  pageSize = 10,
  totalRecords = 5,
}: PaginationLiteProps) {
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
  const safeCurrentPage = Math.min(Math.max(currentPage, 1), totalPages);
  const startIndex = totalRecords === 0 ? 0 : (safeCurrentPage - 1) * pageSize + 1;
  const endIndex = Math.min(safeCurrentPage * pageSize, totalRecords);

  return (
    <div className="vm-table-footer vm-table-footer-lite">
      <span>
        {totalRecords === 0
          ? "Không có bản ghi phù hợp"
          : `Hiển thị ${formatCount(startIndex)} đến ${formatCount(endIndex)} của ${formatCount(totalRecords)} bản ghi`}
      </span>
      <div className="vm-table-footer-controls">
        <label className="vm-table-length">
          <span>Số dòng mỗi trang</span>
          <select
            aria-label="Số dòng mỗi trang"
            value={pageSize}
            onChange={(event) => onPageSizeChange?.(Number(event.target.value))}
          >
            {[5, 10, 20].map((option) => <option key={option} value={option}>{option}</option>)}
          </select>
        </label>
        <div className="vm-pagination">
          <button
            className="vm-page-btn"
            disabled={safeCurrentPage === 1}
            type="button"
            onClick={() => onPageChange?.(safeCurrentPage - 1)}
          >
            <i className="fas fa-chevron-left" />
          </button>
          <button className="vm-page-btn active" type="button">{safeCurrentPage}</button>
          <button
            className="vm-page-btn"
            disabled={safeCurrentPage === totalPages}
            type="button"
            onClick={() => onPageChange?.(safeCurrentPage + 1)}
          >
            <i className="fas fa-chevron-right" />
          </button>
        </div>
      </div>
    </div>
  );
}
