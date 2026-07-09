import { Link, useLocation } from "react-router-dom";
import type { ReactNode } from "react";

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

  return (
    <div className="vm-customer-shell">
      <header className="vm-customer-topbar">
        <Link to="/customer/dashboard" className="vm-customer-logo">CoParking</Link>
        <button className="vm-customer-menu" type="button" aria-label="Menu"><i className="fas fa-bars" /></button>
        <div className="vm-customer-user">
          <span className="vm-customer-bell"><i className="far fa-bell" /><b>3</b></span>
          <span className="vm-customer-avatar">KH</span>
          <span>Xin chào,<strong>Nguyễn Văn A</strong></span>
          <i className="fas fa-chevron-down" />
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

export function PaginationLite() {
  return (
    <div className="vm-table-footer-lite">
      <span>Hiển thị 1 đến 5 của 5</span>
      <div className="vm-pagination-lite">
        <button type="button"><i className="fas fa-chevron-left" /></button>
        <button className="active" type="button">1</button>
        <button type="button"><i className="fas fa-chevron-right" /></button>
      </div>
      <select defaultValue="10 / trang">
        <option>10 / trang</option>
      </select>
    </div>
  );
}
