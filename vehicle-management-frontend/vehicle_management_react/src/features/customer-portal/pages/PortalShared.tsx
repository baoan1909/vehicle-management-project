import { Link, useLocation } from "react-router-dom";
import type { ReactNode } from "react";

import { cn } from "@/lib/cn";

export const publicContactItems = [
  { icon: "fas fa-phone-alt", title: "Hotline", value: "1900 1234", note: "Hỗ trợ 24/7" },
  { icon: "far fa-envelope", title: "Email", value: "support@coparking.vn", note: "Phản hồi trong 24h" },
  { icon: "fas fa-map-marker-alt", title: "Địa chỉ", value: "Số 1 Võ Văn Ngân, phường Thủ Đức, TP. Hồ Chí Minh", note: "" },
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

function scrollToPageTop() {
  window.scrollTo({ left: 0, top: 0 });
}

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
  const watermarkRows = Array.from({ length: 9 });

  return (
    <footer className="tw-relative tw-isolate tw-block tw-w-full tw-overflow-hidden tw-bg-[#052a5a] tw-text-brand-100">
      <div
        className="tw-pointer-events-none tw-absolute -tw-left-[42%] -tw-top-24 tw-z-0 tw-flex tw-w-[195%] tw-origin-center tw-flex-col tw-gap-16 tw-opacity-100"
        aria-hidden="true"
        style={{ transform: "rotate(-16deg)" }}
      >
        {watermarkRows.map((_, index) => (
          <div
            className={cn(
              "tw-flex tw-w-full tw-whitespace-nowrap tw-text-[clamp(3.25rem,5.4vw,6.8rem)] tw-font-black tw-leading-none",
              index % 2 === 0 ? "tw-text-brand-500/[0.14]" : "tw-text-brand-500/[0.08]",
            )}
            key={index}
          >
            <span className="tw-mr-16">CoParking</span>
            <span className="tw-mr-16">CoParking</span>
            <span className="tw-mr-16">CoParking</span>
            <span className="tw-mr-16">CoParking</span>
            <span className="tw-mr-16">CoParking</span>
          </div>
        ))}
      </div>
      <div className="tw-relative tw-z-[1] tw-mx-auto tw-grid tw-w-[min(1180px,calc(100%_-_48px))] tw-grid-cols-[minmax(280px,1fr)_minmax(460px,0.92fr)] tw-gap-16 tw-py-16 max-[992px]:tw-grid-cols-1 max-[992px]:tw-gap-10 max-[640px]:tw-w-[min(100%_-_32px,1180px)] max-[640px]:tw-py-12">
        <section>
          <img className="tw-mb-4 tw-block tw-w-[156px] tw-opacity-95 ![filter:brightness(0)_invert(1)_contrast(1.25)]" src="/assets/admin/dist/img/AdminLTELogo.png" alt="CoParking" />
          <p className="tw-m-0 tw-mb-10 tw-text-[1.05rem] tw-font-bold tw-text-brand-100">Quản lý bãi xe thông minh</p>
          <span className="tw-mb-9 tw-block tw-text-[0.92rem] tw-font-semibold tw-text-brand-200">Copyright © 2026 CoParking. All Rights Reserved</span>
          <div className="tw-mt-6">
            <strong className="tw-mb-3 tw-block tw-text-base tw-font-black tw-text-white">Trụ sở</strong>
            <p className="tw-m-0 tw-max-w-[440px] tw-text-[0.95rem] tw-font-semibold tw-leading-7 tw-text-[#c7d8f2]">10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh</p>
          </div>
          <div className="tw-mt-6">
            <strong className="tw-mb-3 tw-block tw-text-base tw-font-black tw-text-white">Liên hệ</strong>
            <p className="tw-m-0 tw-max-w-[440px] tw-text-[0.95rem] tw-font-semibold tw-leading-7 tw-text-[#c7d8f2]">0912345678<br />support@coparking.vn</p>
          </div>
        </section>

        <section className="tw-grid tw-grid-cols-2 tw-gap-x-14 tw-gap-y-9 max-[640px]:tw-grid-cols-1">
          <div className="tw-flex tw-min-w-0 tw-flex-col tw-gap-3">
            <h3 className="tw-m-0 tw-mb-1 tw-text-base tw-font-black tw-text-white">Công ty</h3>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/" onClick={scrollToPageTop}>Về chúng tôi</Link>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/pricing" onClick={scrollToPageTop}>Bảng giá</Link>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/contact" onClick={scrollToPageTop}>Liên hệ</Link>
          </div>
          <div className="tw-flex tw-min-w-0 tw-flex-col tw-gap-3">
            <h3 className="tw-m-0 tw-mb-1 tw-text-base tw-font-black tw-text-white">Tính năng</h3>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/guide" onClick={scrollToPageTop}>Quản lý bãi xe</Link>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/guide" onClick={scrollToPageTop}>OCR biển số</Link>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/customer/subscriptions" onClick={scrollToPageTop}>Vé tháng</Link>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/pricing" onClick={scrollToPageTop}>Thanh toán</Link>
          </div>
          <div className="tw-flex tw-min-w-0 tw-flex-col tw-gap-3">
            <h3 className="tw-m-0 tw-mb-1 tw-text-base tw-font-black tw-text-white">Thông tin</h3>
            <Link className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" to="/guide" onClick={scrollToPageTop}>Hướng dẫn sử dụng</Link>
            <a className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" href="#">Điều khoản sử dụng</a>
            <a className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" href="#">Quy định bảo mật</a>
            <a className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" href="#">Cơ chế giải quyết khiếu nại</a>
            <a className="tw-w-fit tw-text-[0.95rem] tw-font-bold tw-text-brand-100 hover:tw-text-white hover:tw-no-underline" href="#">Yêu cầu xóa tài khoản</a>
          </div>
          <div className="tw-flex tw-min-w-0 tw-flex-col tw-gap-3">
            <h3 className="tw-m-0 tw-mb-1 tw-text-base tw-font-black tw-text-white">Kết nối với chúng tôi</h3>
            <div className="tw-flex tw-gap-3">
              <a className="tw-grid tw-h-[34px] tw-w-[34px] tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-brand-200/30 tw-bg-[#0d3a73]/80 tw-text-white hover:tw-bg-brand-700 hover:tw-text-white hover:tw-no-underline" href="#" aria-label="Facebook"><i className="fab fa-facebook-f" /></a>
              <a className="tw-grid tw-h-[34px] tw-w-[34px] tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-brand-200/30 tw-bg-[#0d3a73]/80 tw-text-white hover:tw-bg-brand-700 hover:tw-text-white hover:tw-no-underline" href="#" aria-label="TikTok"><i className="fab fa-tiktok" /></a>
              <a className="tw-grid tw-h-[34px] tw-w-[34px] tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-brand-200/30 tw-bg-[#0d3a73]/80 tw-text-white hover:tw-bg-brand-700 hover:tw-text-white hover:tw-no-underline" href="#" aria-label="Email"><i className="fas fa-at" /></a>
            </div>
          </div>
        </section>
      </div>
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
  value: ReactNode;
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
