import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";

import { ClientPage } from "@/shared/components/layout/ClientPage";
import {
  getPublicPricePlans,
  getPublicPriceRules,
  getPublicPricingTicketTypes,
  getPublicPricingVehicleTypes,
  type PricePlanApiResponse,
  type PriceRuleApiResponse,
  type TicketTypeApiResponse,
  type VehicleTypeApiResponse,
} from "@/features/pricing/api/pricingApi";

import { PublicFooter } from "./PortalShared";

type PricingAudience = "VISITOR" | "CUSTOMER";
type VehicleFilterKey = "MOTORBIKE" | "CAR" | "OTHER";
type PricingShineState = { direction: "enter" | "leave"; id: number; traveling: boolean };

const trustItems = [
  { icon: "fas fa-shield-alt", title: "An toàn 24/7", description: "Hệ thống giám sát hiện đại" },
  { icon: "far fa-clipboard", title: "Quản lý minh bạch", description: "Lịch sử rõ ràng, dễ tra cứu" },
  { icon: "fas fa-headset", title: "Hỗ trợ nhanh", description: "Đội ngũ tận tâm, kịp thời" },
  { icon: "far fa-credit-card", title: "Thanh toán tiện lợi", description: "Nhiều phương thức thanh toán" },
];

type DisplayRule = PriceRuleApiResponse & {
  plan?: PricePlanApiResponse;
  ticketType?: TicketTypeApiResponse;
  vehicleType?: VehicleTypeApiResponse;
};

const vehicleFilterOptions: Array<{ icon: string; label: string; value: VehicleFilterKey }> = [
  { icon: "fas fa-motorcycle", label: "Xe máy", value: "MOTORBIKE" },
  { icon: "fas fa-car", label: "Ô tô", value: "CAR" },
  { icon: "fas fa-truck", label: "Xe khác", value: "OTHER" },
];

function normalizeSearchText(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function vehicleFilterLabel(value: VehicleFilterKey) {
  return vehicleFilterOptions.find((option) => option.value === value)?.label ?? "Loại xe";
}

function vehicleCategory(vehicleType?: VehicleTypeApiResponse): VehicleFilterKey {
  const text = normalizeSearchText(`${vehicleType?.code ?? ""} ${vehicleType?.name ?? ""}`);

  if (text.includes("motor") || text.includes("moto") || text.includes("xe may")) return "MOTORBIKE";
  if (text.includes("car") || text.includes("oto") || text.includes("o to")) return "CAR";
  return "OTHER";
}

function matchesVehicleFilter(rule: DisplayRule, selectedVehicleFilter: VehicleFilterKey) {
  return vehicleCategory(rule.vehicleType) === selectedVehicleFilter;
}

function formatCurrency(value: number | null) {
  if (value === null || Number.isNaN(value)) return "Chưa cấu hình";

  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function formatDate(value: string | null) {
  if (!value) return "Không giới hạn";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(value));
}

function durationLabel(durationDays?: number | null) {
  if (!durationDays) return null;
  if (durationDays === 365) return "/ năm";
  if (durationDays === 90) return "/ quý";
  if (durationDays === 30) return "/ tháng";
  if (durationDays % 30 === 0) return `/ ${durationDays / 30} tháng`;
  return `/ ${durationDays} ngày`;
}

function subscriptionUnitLabel(ticketType?: TicketTypeApiResponse) {
  const code = ticketType?.code?.trim().toUpperCase();

  if (code === "MONTHLY") return "/ tháng";
  if (code === "QUARTERLY") return "/ quý";
  if (code === "YEARLY") return "/ năm";
  if (code === "FREE") return durationLabel(ticketType?.durationDays) ?? "/ 6 tháng";
  return durationLabel(ticketType?.durationDays);
}

function isMonthlyRule(rule: DisplayRule) {
  const ticketCode = rule.ticketType?.code?.trim().toUpperCase();
  if (ticketCode === "MONTHLY" || rule.ticketType?.durationDays === 30) return true;

  const label = normalizeSearchText(`${rule.ticketType?.name ?? ""} ${rule.ruleName ?? ""}`);
  return label.includes("ve thang") || label.includes("dang ky thang");
}

function unitLabel(audience: PricingAudience, unit: string | null, ticketType?: TicketTypeApiResponse) {
  if (audience === "CUSTOMER") {
    const subscriptionLabel = subscriptionUnitLabel(ticketType);
    if (subscriptionLabel) return subscriptionLabel;
  }

  const normalized = unit?.trim().toUpperCase();
  if (normalized === "MONTH") return "/ tháng";
  if (normalized === "QUARTER") return "/ quý";
  if (normalized === "YEAR") return "/ năm";
  if (normalized === "DAY") return "/ ngày";
  if (normalized === "HOUR") return "/ giờ";
  if (normalized === "TURN" || normalized === "SESSION") return "/ lượt";
  if (ticketType?.durationDays) return durationLabel(ticketType.durationDays) ?? "";
  return unit ? `/ ${unit.toLowerCase()}` : "";
}

function timeRange(rule: PriceRuleApiResponse) {
  if (rule.timeFrom && rule.timeTo) return `${rule.timeFrom} — ${rule.timeTo}`;
  if (rule.timeFrom) return `Từ ${rule.timeFrom}`;
  if (rule.timeTo) return `Đến ${rule.timeTo}`;
  return "Cả ngày";
}

function vehicleIcon(vehicleType?: VehicleTypeApiResponse) {
  const category = vehicleCategory(vehicleType);
  if (category === "MOTORBIKE") return "fas fa-motorcycle";
  if (category === "CAR") return "fas fa-car";
  return "fas fa-truck";
}

function VisitorPeriodIcon({ rule }: { rule: DisplayRule }) {
  const night = Boolean(rule.timeFrom && rule.timeFrom >= "18:00");

  return (
    <span className="tw-relative tw-grid tw-h-14 tw-w-14 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-[linear-gradient(145deg,#f8faff,#edf4ff)] tw-text-vm-primary tw-shadow-[inset_0_0_0_1px_rgba(37,99,235,0.035)]" aria-hidden="true">
      {night ? (
        <svg className="tw-h-8 tw-w-8" fill="none" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
          <path d="M21.9 23.9A10 10 0 0 1 9.1 10.8 10.4 10.4 0 1 0 21.9 23.9Z" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" />
          <path d="m22.5 7 .6 1.5 1.5.6-1.5.6-.6 1.5-.6-1.5-1.5-.6 1.5-.6.6-1.5Zm4.2 6.1.35.85.85.35-.85.35-.35.85-.35-.85-.85-.35.85-.35.35-.85Z" fill="currentColor" />
        </svg>
      ) : (
        <svg className="tw-h-8 tw-w-8" fill="none" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
          <circle cx="16" cy="16" r="5.25" stroke="currentColor" strokeWidth="1.8" />
          <path d="M16 3.5v3.25M16 25.25v3.25M3.5 16h3.25M25.25 16h3.25M7.15 7.15l2.3 2.3M22.55 22.55l2.3 2.3M24.85 7.15l-2.3 2.3M9.45 22.55l-2.3 2.3" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
        </svg>
      )}
    </span>
  );
}

function buildRules(
  audience: PricingAudience,
  plans: PricePlanApiResponse[],
  rules: PriceRuleApiResponse[],
  vehicleTypes: VehicleTypeApiResponse[],
  ticketTypes: TicketTypeApiResponse[],
) {
  const allowedPlans = plans.filter((plan) => plan.appliesTo === audience || plan.appliesTo === "ALL");
  const allowedPlanIds = new Set(allowedPlans.map((plan) => plan.pricePlanId));
  const planById = new Map(allowedPlans.map((plan) => [plan.pricePlanId, plan]));
  const vehicleById = new Map(vehicleTypes.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType]));
  const ticketById = new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType]));

  return rules
    .filter((rule) => Boolean(rule.pricePlanId && allowedPlanIds.has(rule.pricePlanId)))
    .map((rule) => ({
      ...rule,
      plan: rule.pricePlanId ? planById.get(rule.pricePlanId) : undefined,
      ticketType: rule.ticketTypeId ? ticketById.get(rule.ticketTypeId) : undefined,
      vehicleType: rule.vehicleTypeId ? vehicleById.get(rule.vehicleTypeId) : undefined,
    }))
    .sort((left, right) => (left.priority ?? 999) - (right.priority ?? 999) || left.ruleName.localeCompare(right.ruleName));
}

function PriceCard({ audience, featured = false, rule }: { audience: PricingAudience; featured?: boolean; rule: DisplayRule }) {
  const vehicleName = rule.vehicleType?.name ?? "Tất cả loại xe";
  const ticketName = rule.ticketType?.name ?? (audience === "VISITOR" ? "Khách vãng lai" : "Vé đăng ký");
  const subscription = audience === "CUSTOMER";
  const visitorDisplayName = rule.timeFrom
    ? rule.timeFrom >= "18:00" ? "Gửi xe ban đêm" : "Gửi xe ban ngày"
    : rule.ruleName || ticketName;
  const [cardActive, setCardActive] = useState(false);
  const [registerButtonActive, setRegisterButtonActive] = useState(false);
  const [cardShine, setCardShine] = useState<PricingShineState | null>(null);
  const shineIdRef = useRef(0);
  const shineTimeoutRef = useRef<number | null>(null);

  useEffect(() => () => {
    if (shineTimeoutRef.current) window.clearTimeout(shineTimeoutRef.current);
  }, []);

  function playCardShine(direction: PricingShineState["direction"]) {
    if (!subscription) return;
    if (shineTimeoutRef.current) window.clearTimeout(shineTimeoutRef.current);

    const id = shineIdRef.current + 1;
    shineIdRef.current = id;
    setCardShine({ direction, id, traveling: false });
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => setCardShine({ direction, id, traveling: true }));
    });
    shineTimeoutRef.current = window.setTimeout(() => {
      setCardShine((current) => (current?.id === id ? null : current));
    }, 1680);
  }

  function activateCard() {
    setCardActive(true);
    playCardShine("enter");
  }

  function deactivateCard() {
    setCardActive(false);
    playCardShine("leave");
  }

  if (subscription) {
    return (
      <div className="tw-relative" onBlur={deactivateCard} onFocus={activateCard} onPointerEnter={activateCard} onPointerLeave={deactivateCard} style={{ zIndex: cardActive ? 30 : undefined }}>
      <article className="tw-relative tw-isolate tw-min-h-[248px] tw-transform-gpu tw-overflow-hidden tw-rounded-[9px] tw-border-2 tw-border-solid tw-border-[#176cff] tw-bg-[radial-gradient(circle_at_0%_0%,rgba(106,172,255,0.25),transparent_31%),radial-gradient(circle_at_96%_7%,rgba(29,99,235,0.16),transparent_34%),linear-gradient(145deg,#08264d_0%,#04162f_66%,#03142c_100%)] tw-p-4 tw-text-white tw-shadow-[0_0_0_1px_rgba(54,129,255,0.2),0_18px_36px_rgba(5,27,64,0.28),inset_1px_1px_0_rgba(205,230,255,0.2)] tw-outline-none tw-transition-all tw-duration-300 tw-ease-out tw-will-change-transform focus-visible:tw-ring-2 focus-visible:tw-ring-brand-200" style={{ boxShadow: cardActive ? "0 20px 40px rgba(5,27,64,0.32), 0 0 0 1px rgba(54,129,255,0.28)" : undefined, transform: cardActive ? "translate3d(0, -11px, 0)" : "translate3d(0, 0, 0)" }} tabIndex={0}>
        <span aria-hidden="true" className="tw-pointer-events-none tw-absolute tw-inset-[1px] tw-rounded-[6px] tw-border tw-border-solid tw-border-white/[0.055]" />
        <span aria-hidden="true" className="tw-pointer-events-none tw-absolute -tw-left-[2px] -tw-top-[2px] tw-h-5 tw-w-5 tw-rounded-tl-[9px] tw-border-0 tw-border-l-2 tw-border-t-2 tw-border-solid tw-border-[#c6e3ff]" />
        <span
          key={cardShine?.id ?? "pricing-shine-idle"}
          aria-hidden="true"
          className="tw-pointer-events-none tw-absolute -tw-left-[34%] -tw-top-[128%] tw-z-[60] tw-h-[260%] tw-w-[28%] tw-opacity-0"
          data-pricing-shine="true"
          style={{
            background: "linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.035) 28%, rgba(255,255,255,0.34) 50%, rgba(255,255,255,0.035) 72%, transparent 100%)",
            opacity: cardShine?.traveling ? 0.76 : 0,
            transform: cardShine?.direction === "leave"
              ? cardShine.traveling ? "translate3d(-110%, -8%, 0) rotate(32deg)" : "translate3d(590%, 8%, 0) rotate(32deg)"
              : cardShine?.traveling ? "translate3d(590%, 8%, 0) rotate(32deg)" : "translate3d(-110%, -8%, 0) rotate(32deg)",
            transition: cardShine?.traveling
              ? "transform 1.45s cubic-bezier(0.4, 0, 0.2, 1), opacity 160ms ease-out"
              : "transform 0ms linear, opacity 180ms ease-in",
          }}
        />
        {featured ? (
          <span className="tw-absolute tw-right-4 tw-top-4 tw-z-10 tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-vm-sm tw-border tw-border-solid tw-border-[#f0c977] tw-bg-[linear-gradient(135deg,#c28b37,#e3b45f)] tw-px-3 tw-py-1.5 tw-text-[0.68rem] tw-font-black tw-text-white tw-shadow-[0_8px_18px_rgba(184,135,60,0.28)]">
            <i className="far fa-star" /> Lựa chọn phổ biến
          </span>
        ) : null}
        <p className="tw-relative tw-m-0 tw-text-[0.65rem] tw-font-black tw-uppercase tw-tracking-[0.14em] tw-text-brand-200">Gói gửi xe theo chu kỳ</p>
        <h3 className="tw-relative tw-m-0 tw-mt-2 tw-pr-36 tw-text-[1rem] tw-font-black tw-text-white">{rule.ruleName || ticketName}</h3>
        <div className="tw-relative tw-mt-3 tw-flex tw-items-baseline tw-gap-2">
          <strong className="tw-text-[2.45rem] tw-font-black tw-leading-none tw-tracking-[-0.035em] tw-text-[#72a7ff]">{formatCurrency(rule.basePrice)}</strong>
          <span className="tw-text-[0.9rem] tw-font-bold tw-text-slate-300">{unitLabel(audience, rule.unit, rule.ticketType)}</span>
        </div>
        <div className="tw-relative tw-mt-3 tw-grid tw-grid-cols-3 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-white/20 tw-pt-3">
          <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.7rem] tw-font-semibold tw-leading-4 tw-text-slate-100"><i className="fas fa-infinity tw-grid tw-h-9 tw-w-9 tw-flex-none tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-white/25 tw-bg-white/[0.035] tw-text-brand-200" /> Ra vào không giới hạn</span>
          <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.7rem] tw-font-semibold tw-leading-4 tw-text-slate-100"><i className="far fa-id-card tw-grid tw-h-9 tw-w-9 tw-flex-none tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-white/25 tw-bg-white/[0.035] tw-text-brand-200" /> Quản lý vé trực tuyến</span>
          <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.7rem] tw-font-semibold tw-leading-4 tw-text-slate-100"><i className="fas fa-headset tw-grid tw-h-9 tw-w-9 tw-flex-none tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-white/25 tw-bg-white/[0.035] tw-text-brand-200" /> Hỗ trợ ưu tiên</span>
        </div>
        <Link
          className="tw-relative tw-isolate tw-mt-3 tw-inline-flex tw-min-h-10 tw-w-full tw-items-center tw-justify-center tw-overflow-hidden tw-rounded-vm-md tw-bg-[linear-gradient(90deg,#1557dd,#1875ff)] tw-px-4 tw-text-[0.86rem] tw-font-black tw-text-white tw-shadow-[0_10px_22px_rgba(21,87,221,0.24)] tw-outline-none hover:tw-text-white hover:tw-no-underline focus-visible:tw-ring-2 focus-visible:tw-ring-white/80"
          onBlur={() => setRegisterButtonActive(false)}
          onFocus={() => setRegisterButtonActive(true)}
          onPointerEnter={() => setRegisterButtonActive(true)}
          onPointerLeave={() => setRegisterButtonActive(false)}
          to="/customer/subscriptions"
        >
          <span className="tw-relative tw-z-10 tw-inline-flex tw-items-center tw-justify-center tw-gap-4">Đăng ký vé <i className="fas fa-arrow-right" /></span>
          <span
            aria-hidden="true"
            className="tw-absolute tw-inset-0 tw-z-20 tw-inline-flex tw-items-center tw-justify-center tw-gap-4 tw-bg-white tw-text-black motion-reduce:tw-transition-none"
            style={{
              clipPath: registerButtonActive ? "inset(0 0 0 0)" : "inset(0 0 0 100%)",
              transition: "clip-path 500ms ease-in-out",
            }}
          >
            Đăng ký vé <i className="fas fa-arrow-right" />
          </span>
        </Link>
      </article>
      </div>
    );
  }

  return (
    <div className="tw-relative" onBlur={deactivateCard} onFocus={activateCard} onPointerEnter={activateCard} onPointerLeave={deactivateCard} style={{ zIndex: cardActive ? 30 : undefined }}>
    <article className="tw-relative tw-flex tw-min-h-[206px] tw-transform-gpu tw-flex-col tw-overflow-hidden tw-rounded-[10px] tw-border tw-border-solid tw-border-[#dce6f5] tw-bg-white tw-p-5 tw-shadow-[0_14px_34px_rgba(42,75,122,0.09)] tw-outline-none tw-transition-all tw-duration-300 tw-ease-out tw-will-change-transform focus-visible:tw-ring-2 focus-visible:tw-ring-brand-200" style={{ borderColor: cardActive ? "#bfdbfe" : undefined, boxShadow: cardActive ? "0 18px 38px rgba(37,99,235,0.14)" : undefined, transform: cardActive ? "translate3d(0, -11px, 0)" : "translate3d(0, 0, 0)" }} tabIndex={0}>
      <div className="tw-flex tw-items-start tw-gap-4">
        <VisitorPeriodIcon rule={rule} />
        <div className="tw-min-w-0">
          <h3 className="tw-m-0 tw-text-[0.96rem] tw-font-black tw-leading-5 tw-text-vm-slate-900">{visitorDisplayName}</h3>
          <div className="tw-mt-3 tw-flex tw-items-baseline tw-gap-2">
            <strong className="tw-text-[1.9rem] tw-font-black tw-leading-none tw-tracking-[-0.025em] tw-text-vm-primary">{formatCurrency(rule.basePrice)}</strong>
            <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">{unitLabel(audience, rule.unit, rule.ticketType)}</span>
          </div>
          <span className="tw-mt-3 tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500"><i className="far fa-clock tw-text-brand-500" /> {timeRange(rule)}</span>
        </div>
      </div>

      <div className="tw-mt-auto tw-grid tw-grid-cols-[auto_minmax(0,1fr)] tw-items-center tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
        <span className="tw-inline-flex tw-items-center tw-gap-2 tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-pr-3 tw-text-[0.7rem] tw-font-semibold tw-text-vm-slate-700"><i className={`${vehicleIcon(rule.vehicleType)} tw-w-4 tw-text-brand-500`} /> {vehicleName}</span>
        {rule.lostCardFee ? <span className="tw-inline-flex tw-min-w-0 tw-items-center tw-gap-2 tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-700"><i className="far fa-credit-card tw-w-4 tw-flex-none tw-text-brand-500" /> <span className="tw-truncate">Phí mất thẻ {formatCurrency(rule.lostCardFee)}</span></span> : <span className="tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-500">Giá đã gồm phí dịch vụ</span>}
      </div>
    </article>
    </div>
  );
}

function EmptyPricing({ label }: { label: string }) {
  return (
    <article className="tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-white tw-p-6 tw-text-center tw-shadow-vm-card">
      <i className="fas fa-tags tw-text-[1.6rem] tw-text-vm-primary" />
      <h3 className="tw-m-3 tw-text-[1.05rem] tw-font-black tw-text-vm-slate-900">{label}</h3>
      <p className="tw-m-0 tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-500">Chưa có quy tắc giá phù hợp trong bảng giá hiện hành.</p>
    </article>
  );
}

function PricingSectionHeading({ action, icon, title }: { action?: ReactNode; icon: string; subtitle?: string; title: string }) {
  return (
    <div className="tw-mb-4 tw-flex tw-min-h-10 tw-items-center tw-justify-between tw-gap-3">
      <div className="tw-flex tw-items-center tw-gap-3">
        <span className="tw-grid tw-h-9 tw-w-9 tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-text-[1.05rem] tw-text-vm-primary tw-shadow-[0_6px_16px_rgba(37,99,235,0.08)]"><i className={icon} /></span>
        <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-black tw-text-vm-slate-900">{title}</h2>
      </div>
      {action}
    </div>
  );
}

function VehicleFilterTabs({ selected, onChange }: { selected: VehicleFilterKey; onChange: (value: VehicleFilterKey) => void }) {
  return (
    <div className="tw-relative tw-z-10 tw-mx-auto tw-grid tw-w-full tw-max-w-[720px] tw-grid-cols-[minmax(170px,0.95fr)_repeat(3,minmax(0,1fr))] tw-overflow-hidden tw-rounded-[10px] tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-2 tw-shadow-[0_15px_35px_rgba(31,66,116,0.15)] max-[640px]:tw-grid-cols-1">
      <span className="tw-flex tw-items-center tw-px-5 tw-text-[0.85rem] tw-font-bold tw-text-vm-slate-700 max-[640px]:tw-justify-center">Chọn phương tiện</span>
      {vehicleFilterOptions.map((option) => {
        const active = selected === option.value;
        return (
          <button
            aria-selected={active}
            className={active ? "tw-flex tw-min-h-[52px] tw-items-center tw-justify-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-[linear-gradient(135deg,#1557dd,#1875ff)] tw-px-3 tw-text-[0.9rem] tw-font-black tw-text-white tw-shadow-[0_10px_22px_rgba(37,99,235,0.28)]" : "tw-flex tw-min-h-[52px] tw-items-center tw-justify-center tw-gap-3 tw-rounded-vm-md tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-transparent tw-px-3 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-700 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary"}
            key={option.value}
            onClick={() => onChange(option.value)}
            role="tab"
            type="button"
          >
            <i className={`${option.icon} tw-w-6 tw-text-center tw-text-[1.25rem]`} /> {option.label}
          </button>
        );
      })}
    </div>
  );
}

export function PricingPage() {
  const [plans, setPlans] = useState<PricePlanApiResponse[]>([]);
  const [rules, setRules] = useState<PriceRuleApiResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeApiResponse[]>([]);
  const [ticketTypes, setTicketTypes] = useState<TicketTypeApiResponse[]>([]);
  const [selectedVehicleFilter, setSelectedVehicleFilter] = useState<VehicleFilterKey>("MOTORBIKE");
  const [selectedCustomerRuleId, setSelectedCustomerRuleId] = useState("");
  const [customerMenuOpen, setCustomerMenuOpen] = useState(false);
  const [showAllVisitorRules, setShowAllVisitorRules] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadPricing() {
      setLoading(true);
      setError("");

      try {
        const [planResponse, ruleResponse, vehicleTypeResponse, ticketTypeResponse] = await Promise.all([
          getPublicPricePlans({ effectiveDate: new Date().toISOString().slice(0, 10), isActive: true }),
          getPublicPriceRules({ isActive: true }),
          getPublicPricingVehicleTypes(),
          getPublicPricingTicketTypes(),
        ]);

        if (ignore) return;
        setPlans(planResponse.data ?? []);
        setRules(ruleResponse.data ?? []);
        setVehicleTypes(vehicleTypeResponse.data ?? []);
        setTicketTypes(ticketTypeResponse.data ?? []);
      } catch (requestError) {
        if (ignore) return;
        setError(requestError instanceof Error ? requestError.message : "Không thể tải bảng giá.");
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    void loadPricing();
    return () => { ignore = true; };
  }, []);

  const visitorRules = useMemo(() => buildRules("VISITOR", plans, rules, vehicleTypes, ticketTypes), [plans, rules, ticketTypes, vehicleTypes]);
  const customerRules = useMemo(() => buildRules("CUSTOMER", plans, rules, vehicleTypes, ticketTypes), [plans, rules, ticketTypes, vehicleTypes]);
  const filteredVisitorRules = useMemo(
    () => {
      const matchingRules = visitorRules.filter((rule) => matchesVehicleFilter(rule, selectedVehicleFilter));
      const dayRules = matchingRules.filter((rule) => !rule.timeFrom || rule.timeFrom < "18:00");
      const nightRules = matchingRules.filter((rule) => Boolean(rule.timeFrom && rule.timeFrom >= "18:00"));
      const orderedRules = [];

      if (dayRules[0]) orderedRules.push(dayRules[0]);
      if (nightRules[0]) orderedRules.push(nightRules[0]);
      orderedRules.push(...dayRules.slice(1), ...nightRules.slice(1));
      return orderedRules;
    },
    [selectedVehicleFilter, visitorRules],
  );
  const filteredCustomerRules = useMemo(
    () => customerRules
      .filter((rule) => matchesVehicleFilter(rule, selectedVehicleFilter))
      .sort((left, right) => Number(isMonthlyRule(right)) - Number(isMonthlyRule(left))),
    [customerRules, selectedVehicleFilter],
  );
  const visibleVisitorRules = showAllVisitorRules ? filteredVisitorRules : filteredVisitorRules.slice(0, 2);
  const selectedCustomerRule = filteredCustomerRules.find((rule) => rule.priceRuleId === selectedCustomerRuleId)
    ?? filteredCustomerRules.find(isMonthlyRule)
    ?? filteredCustomerRules[0];
  const planStartLabel = plans.length ? `Áp dụng từ ${formatDate(plans[0]?.effectiveFrom ?? null)}` : "Theo bảng giá đang hiệu lực";
  const selectedVehicleLabel = vehicleFilterLabel(selectedVehicleFilter);

  function handleVehicleFilterChange(value: VehicleFilterKey) {
    setSelectedVehicleFilter(value);
    setSelectedCustomerRuleId("");
    setCustomerMenuOpen(false);
    setShowAllVisitorRules(false);
  }

  return (
    <ClientPage>
      <div className="tw-bg-[#f7faff] tw-pb-10">
        <main className="tw-mx-auto tw-w-full tw-max-w-[1600px] tw-px-4 tw-pt-5 min-[992px]:tw-px-9 min-[992px]:tw-pt-8">
          <section className="tw-relative tw-min-h-[382px] tw-overflow-hidden tw-rounded-[10px] tw-bg-[#061a36] tw-shadow-[0_24px_60px_rgba(5,27,64,0.2)]">
            <img className="tw-absolute tw-bottom-0 tw-right-0 tw-top-0 tw-h-full tw-w-[72%] tw-object-cover tw-object-[50%_38%] max-[767px]:tw-w-full" src="/assets/customer/pricing-hero-smart-parking.png" alt="Bãi xe thông minh CoParking về đêm" />
            <div className="tw-absolute tw-inset-0 tw-bg-[linear-gradient(90deg,rgba(5,22,47,0.99)_0%,rgba(5,22,47,0.94)_29%,rgba(5,22,47,0.6)_42%,rgba(5,22,47,0.08)_64%,rgba(5,22,47,0.05)_100%)]" />
            <div className="tw-relative tw-z-10 tw-flex tw-min-h-[382px] tw-max-w-[720px] tw-flex-col tw-justify-center tw-p-7 min-[768px]:tw-justify-start min-[768px]:tw-px-[70px] min-[768px]:tw-pt-[51px]">
              <h1 className="tw-m-0 tw-max-w-[600px] tw-font-[Cambria] !tw-text-[4.1rem] tw-font-bold tw-leading-[1.02] tw-text-white max-[1200px]:!tw-text-[3.65rem] max-[768px]:!tw-text-[3rem]">Bảng giá gửi xe <span className="tw-block tw-text-[#176fff]">minh bạch</span></h1>
              <p className="tw-m-0 tw-mt-4 tw-max-w-[480px] tw-text-[1.05rem] tw-font-semibold tw-leading-7 tw-text-brand-100">Chọn gói phù hợp. Quản lý hành trình an tâm.</p>
              <div className="tw-mt-7 tw-flex tw-flex-wrap tw-items-center tw-gap-7">
                <a className="tw-inline-flex tw-min-h-[50px] tw-min-w-[198px] tw-items-center tw-justify-center tw-gap-5 tw-rounded-vm-md tw-bg-[linear-gradient(135deg,#1557dd,#1875ff)] tw-px-5 tw-text-[0.9rem] tw-font-black tw-text-white tw-shadow-[0_10px_22px_rgba(21,87,221,0.28)] tw-transition hover:tw-bg-vm-primary-hover hover:tw-text-white hover:tw-no-underline" href="#pricing-rates">Xem bảng giá <i className="fas fa-arrow-right" /></a>
                <Link className="tw-inline-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-4 tw-border-0 tw-border-b-2 tw-border-solid tw-border-[#c89b55] tw-px-0 tw-text-[0.9rem] tw-font-black tw-text-white hover:tw-text-brand-100 hover:tw-no-underline" to="/contact">Tư vấn gói vé <i className="fas fa-chevron-right tw-text-[0.72rem] tw-text-[#d5ae6a]" /></Link>
              </div>
            </div>
            <aside className="tw-absolute tw-right-8 tw-top-[68px] tw-z-10 tw-hidden tw-min-h-[72px] tw-w-[232px] tw-grid-cols-[38px_1fr] tw-items-center tw-gap-3 tw-rounded-[9px] tw-border tw-border-solid tw-border-white/35 tw-bg-[linear-gradient(120deg,rgba(10,31,59,0.9),rgba(8,25,49,0.82))] tw-px-4 tw-py-2.5 tw-text-white tw-shadow-[0_14px_30px_rgba(0,0,0,0.25),inset_0_1px_0_rgba(255,255,255,0.12)] tw-backdrop-blur-[18px] min-[900px]:tw-grid">
              <span className="tw-grid tw-h-9 tw-w-9 tw-place-items-center tw-text-[1.45rem] tw-text-white"><i className="far fa-calendar-alt" /></span>
              <span className="tw-min-w-0"><strong className="tw-block tw-text-[0.78rem] tw-font-black">Bảng giá hiện hành</strong><small className="tw-mt-1 tw-block tw-whitespace-nowrap tw-text-[0.71rem] tw-font-semibold tw-text-slate-200">{loading ? "Đang tải dữ liệu..." : planStartLabel}</small></span>
            </aside>
          </section>

          <section className="tw-relative tw-z-20 tw-mx-auto -tw-mt-8 tw-w-[min(100%_-_32px,720px)]" id="pricing-rates">
            <VehicleFilterTabs selected={selectedVehicleFilter} onChange={handleVehicleFilterChange} />
          </section>

          <section className="tw-mx-auto tw-max-w-[1320px] tw-pb-6 tw-pt-10">
            {error ? <p className="tw-mb-5 tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-font-bold tw-text-red-600">{error}</p> : null}
            <div className="tw-grid tw-items-start tw-gap-7 min-[1180px]:tw-grid-cols-[300px_minmax(0,1fr)_424px]">
              <header className="tw-pt-2">
                <p className="tw-m-0 tw-text-[0.72rem] tw-font-black tw-uppercase tw-tracking-[0.15em] tw-text-vm-primary">Bảng giá minh bạch</p>
                <h2 className="tw-m-0 tw-mt-4 tw-font-[Cambria] tw-text-[clamp(2rem,2.65vw,2.45rem)] tw-font-bold tw-leading-[1.12] tw-text-vm-slate-900"><span className="tw-block tw-whitespace-nowrap">Giá linh hoạt cho</span><span className="tw-block tw-whitespace-nowrap">từng nhu cầu</span></h2>
                <span className="tw-mt-7 tw-block tw-h-px tw-w-14 tw-bg-[#c89b55]" />
              </header>
              <section>
                <PricingSectionHeading
                  action={filteredVisitorRules.length > 2 ? (
                    <button className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-px-3 tw-py-1.5 tw-text-[0.68rem] tw-font-black tw-text-vm-primary tw-shadow-[0_5px_12px_rgba(37,99,235,0.06)] hover:tw-border-brand-300 hover:tw-text-vm-primary-hover" type="button" onClick={() => setShowAllVisitorRules((value) => !value)}>
                      {showAllVisitorRules ? "Thu gọn" : `Xem thêm (${filteredVisitorRules.length - 2})`} <i className={showAllVisitorRules ? "fas fa-chevron-up" : "fas fa-chevron-down"} />
                    </button>
                  ) : null}
                  icon="far fa-user"
                  subtitle="Theo lượt"
                  title="Khách vãng lai"
                />
                <div className="tw-grid tw-gap-4 min-[640px]:tw-grid-cols-2">
                  {visibleVisitorRules.length ? visibleVisitorRules.map((rule) => <PriceCard audience="VISITOR" key={rule.priceRuleId} rule={rule} />) : <EmptyPricing label={`Khách vãng lai - ${selectedVehicleLabel}`} />}
                </div>
              </section>
              <section>
                <PricingSectionHeading
                  action={filteredCustomerRules.length > 1 ? (
                    <div className="tw-relative">
                      <button
                        aria-expanded={customerMenuOpen}
                        aria-haspopup="listbox"
                        className="tw-inline-flex tw-h-10 tw-w-[176px] tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-white tw-px-3.5 tw-text-left tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_6px_16px_rgba(37,99,235,0.08)] tw-outline-none tw-transition hover:tw-border-brand-400 focus:tw-ring-2 focus:tw-ring-brand-100"
                        onClick={() => setCustomerMenuOpen((value) => !value)}
                        type="button"
                      >
                        <span className="tw-truncate">{selectedCustomerRule?.ticketType?.name ?? selectedCustomerRule?.ruleName ?? "Chọn loại vé"}</span>
                        <i className={`fas fa-chevron-down tw-text-[0.65rem] tw-text-vm-primary tw-transition ${customerMenuOpen ? "tw-rotate-180" : ""}`} />
                      </button>
                      {customerMenuOpen ? (
                        <div className="tw-absolute tw-right-0 tw-top-[calc(100%+8px)] tw-z-40 tw-w-[250px] tw-overflow-hidden tw-rounded-[10px] tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-1.5 tw-shadow-[0_18px_42px_rgba(31,66,116,0.2)]" role="listbox" aria-label="Chọn gói vé đăng ký">
                          {filteredCustomerRules.map((rule) => {
                            const selected = rule.priceRuleId === selectedCustomerRule?.priceRuleId;
                            return (
                              <button
                                aria-selected={selected}
                                className={selected ? "tw-flex tw-w-full tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-brand-50 tw-px-3 tw-py-2.5 tw-text-left tw-text-[0.74rem] tw-font-black tw-text-vm-primary" : "tw-flex tw-w-full tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-px-3 tw-py-2.5 tw-text-left tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-700 hover:tw-bg-vm-slate-50"}
                                key={rule.priceRuleId}
                                onClick={() => { setSelectedCustomerRuleId(rule.priceRuleId); setCustomerMenuOpen(false); }}
                                role="option"
                                type="button"
                              >
                                <span className="tw-min-w-0"><strong className="tw-block tw-truncate">{rule.ruleName}</strong><small className="tw-mt-0.5 tw-block tw-font-semibold tw-text-vm-slate-500">{formatCurrency(rule.basePrice)} {unitLabel("CUSTOMER", rule.unit, rule.ticketType)}</small></span>
                                {selected ? <i className="fas fa-check tw-text-vm-primary" /> : null}
                              </button>
                            );
                          })}
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                  icon="far fa-id-card"
                  subtitle="Vé theo chu kỳ"
                  title="Khách đăng ký"
                />
                {selectedCustomerRule ? <PriceCard audience="CUSTOMER" featured rule={selectedCustomerRule} /> : <EmptyPricing label={`Khách đăng ký - ${selectedVehicleLabel}`} />}
              </section>
            </div>
          </section>

          <section className="tw-mx-auto tw-grid tw-max-w-[1240px] tw-overflow-hidden tw-rounded-[10px] tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-shadow-[0_12px_28px_rgba(31,66,116,0.08)] min-[768px]:tw-grid-cols-4">
            {trustItems.map((item) => (
              <article className="tw-flex tw-min-h-[86px] tw-items-center tw-justify-center tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-5 tw-py-4 last:tw-border-b-0 min-[768px]:tw-border-b-0 min-[768px]:tw-border-r min-[768px]:last:tw-border-r-0" key={item.title}>
                <span className="tw-grid tw-h-11 tw-w-11 tw-flex-none tw-place-items-center tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/60"><i className={`${item.icon} tw-text-[1.35rem] tw-text-vm-primary`} /></span>
                <span className="tw-min-w-0"><strong className="tw-block tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">{item.title}</strong><small className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-semibold tw-leading-4 tw-text-vm-slate-500">{item.description}</small></span>
              </article>
            ))}
          </section>

          <section className="tw-relative tw-mx-auto tw-mt-5 tw-max-w-[1390px] tw-transform-gpu tw-overflow-hidden tw-rounded-[10px] tw-bg-[linear-gradient(105deg,#071b39,#04162f)] tw-shadow-[0_18px_36px_rgba(5,27,64,0.16)] tw-transition-all tw-duration-300 tw-ease-out hover:-tw-translate-y-1 hover:tw-shadow-[0_24px_46px_rgba(5,27,64,0.24)] motion-reduce:tw-transform-none motion-reduce:tw-transition-none">
            <span aria-hidden="true" className="tw-absolute -tw-bottom-20 -tw-left-6 tw-h-32 tw-w-72 tw-rounded-[50%] tw-border tw-border-solid tw-border-[#b8873c]/55" />
            <span aria-hidden="true" className="tw-absolute -tw-bottom-14 -tw-right-12 tw-h-28 tw-w-64 tw-rounded-[50%] tw-border tw-border-solid tw-border-[#b8873c]/45" />
            <div className="tw-relative tw-z-10 tw-mx-auto tw-flex tw-min-h-[108px] tw-w-full tw-max-w-[1180px] tw-flex-wrap tw-items-center tw-justify-between tw-gap-6 tw-px-6 tw-py-5">
              <div className="tw-flex tw-items-center tw-gap-6"><span className="tw-grid tw-h-16 tw-w-16 tw-flex-none tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-brand-300/55 tw-bg-brand-500/10 tw-text-[1.75rem] tw-text-brand-100"><i className="fas fa-headset" /></span><div><h2 className="tw-m-0 tw-font-[Cambria] tw-text-[1.75rem] tw-font-bold tw-text-white">Cần tư vấn chọn gói?</h2><p className="tw-m-0 tw-mt-1 tw-text-[0.92rem] tw-font-semibold tw-text-brand-100">Đội ngũ CoParking luôn sẵn sàng hỗ trợ bạn.</p></div></div>
              <Link className="tw-inline-flex tw-min-h-[50px] tw-min-w-[205px] tw-items-center tw-justify-center tw-gap-5 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#d5ae6a] tw-px-6 tw-text-[0.88rem] tw-font-black tw-text-white tw-transition hover:tw-bg-[#b8873c] hover:tw-text-white hover:tw-no-underline" to="/contact">Liên hệ hỗ trợ <i className="fas fa-arrow-right tw-text-[#d5ae6a]" /></Link>
            </div>
          </section>
        </main>
      </div>
      <PublicFooter />
    </ClientPage>
  );
}
