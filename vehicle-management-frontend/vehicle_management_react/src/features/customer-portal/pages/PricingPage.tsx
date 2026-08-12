import { useEffect, useMemo, useState } from "react";
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

import { PublicContactStrip, PublicFooter } from "./PortalShared";

type PricingAudience = "VISITOR" | "CUSTOMER";
type VehicleFilterKey = "MOTORBIKE" | "CAR" | "OTHER";

type DisplayRule = PriceRuleApiResponse & {
  plan?: PricePlanApiResponse;
  ticketType?: TicketTypeApiResponse;
  vehicleType?: VehicleTypeApiResponse;
};

const vehicleFilterOptions: Array<{ icon: string; label: string; value: VehicleFilterKey }> = [
  { icon: "fas fa-motorcycle", label: "Xe máy", value: "MOTORBIKE" },
  { icon: "fas fa-car", label: "Ô tô", value: "CAR" },
  { icon: "far fa-ellipsis-h", label: "Xe khác", value: "OTHER" },
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

  if (text.includes("motor") || text.includes("moto") || text.includes("xe may")) {
    return "MOTORBIKE";
  }

  if (text.includes("car") || text.includes("oto") || text.includes("o to")) {
    return "CAR";
  }

  return "OTHER";
}

function matchesVehicleFilter(rule: DisplayRule, selectedVehicleFilter: VehicleFilterKey) {
  return vehicleCategory(rule.vehicleType) === selectedVehicleFilter;
}

function formatCurrency(value: number | null) {
  if (value === null || Number.isNaN(value)) {
    return "Chưa cấu hình";
  }

  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function formatDate(value: string | null) {
  if (!value) return "Không giới hạn";
  return new Intl.DateTimeFormat("vi-VN").format(new Date(value));
}

function unitLabel(unit: string | null, ticketType?: TicketTypeApiResponse) {
  const normalized = unit?.trim().toUpperCase();

  if (normalized === "MONTH") return "/ tháng";
  if (normalized === "QUARTER") return "/ quý";
  if (normalized === "YEAR") return "/ năm";
  if (normalized === "DAY") return "/ ngày";
  if (normalized === "HOUR") return "/ giờ";
  if (normalized === "TURN" || normalized === "SESSION") return "/ lượt";
  if (ticketType?.durationDays) return `/ ${ticketType.durationDays} ngày`;
  if (unit) return `/ ${unit.toLowerCase()}`;
  return "";
}

function timeRange(rule: PriceRuleApiResponse) {
  if (rule.timeFrom && rule.timeTo) return `${rule.timeFrom} - ${rule.timeTo}`;
  if (rule.timeFrom) return `Từ ${rule.timeFrom}`;
  if (rule.timeTo) return `Đến ${rule.timeTo}`;
  return "Cả ngày";
}

function iconForRule(audience: PricingAudience, rule: DisplayRule) {
  if (audience === "CUSTOMER") return "far fa-calendar-check";
  if (rule.timeFrom && rule.timeFrom >= "18:00") return "fas fa-moon";
  return "far fa-sun";
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

function PriceCard({ audience, rule }: { audience: PricingAudience; rule: DisplayRule }) {
  const vehicleName = rule.vehicleType?.name ?? "Tất cả loại xe";
  const ticketName = rule.ticketType?.name ?? (audience === "VISITOR" ? "Khách vãng lai" : "Vé đăng ký");
  const colorClass = audience === "CUSTOMER" ? "vm-price-green" : "vm-price-blue";
  const iconClass = audience === "CUSTOMER" ? "vm-price-icon vm-price-icon-green" : "vm-price-icon";
  const description = audience === "CUSTOMER" ? "Gói gửi xe theo chu kỳ" : "Áp dụng theo lượt gửi xe";

  return (
    <article className={audience === "CUSTOMER" ? "vm-price-card vm-price-card-subscription" : "vm-price-card"}>
      <div className="vm-price-card-head">
        <span className={iconClass}><i className={iconForRule(audience, rule)} /></span>
        <div>
          <h3>{rule.ruleName || ticketName}</h3>
          <p>{description}</p>
        </div>
      </div>
      <div className="vm-price-amount">
        <strong className={colorClass}>{formatCurrency(rule.basePrice)}</strong>
        <span>{unitLabel(rule.unit, rule.ticketType)}</span>
      </div>
      <div className="vm-price-tags">
        <span><i /> {vehicleName}</span>
        <span><i /> {ticketName}</span>
        <span><i /> {timeRange(rule)}</span>
      </div>
      <ul>
        {rule.lostCardFee ? <li><i className="fas fa-check-circle" /> Phí mất thẻ: {formatCurrency(rule.lostCardFee)}</li> : null}
        {rule.plan ? <li><i className="fas fa-check-circle" /> Bảng giá: {rule.plan.name}</li> : null}
      </ul>
      {audience === "CUSTOMER" ? <Link to="/customer/subscriptions">Đăng ký vé tháng</Link> : null}
    </article>
  );
}

function EmptyPricing({ label }: { label: string }) {
  return (
    <article className="vm-price-card">
      <div className="vm-price-card-head">
        <span className="vm-price-icon"><i className="fas fa-tags" /></span>
        <div>
          <h3>{label}</h3>
          <p>API chưa trả quy tắc giá phù hợp</p>
        </div>
      </div>
      <div className="vm-price-amount">
        <strong className="vm-price-blue">Chưa có dữ liệu</strong>
      </div>
      <ul>
        <li><i className="fas fa-info-circle" /> Kiểm tra kế hoạch giá đang hiệu lực</li>
        <li><i className="fas fa-info-circle" /> Kiểm tra quy tắc giá đang hoạt động</li>
      </ul>
    </article>
  );
}

function VehicleFilterTabs({
  selected,
  onChange,
}: {
  selected: VehicleFilterKey;
  onChange: (value: VehicleFilterKey) => void;
}) {
  return (
    <div className="vm-vehicle-tabs" role="tablist" aria-label="Lọc bảng giá theo loại xe">
      {vehicleFilterOptions.map((option) => (
        <button
          aria-selected={selected === option.value}
          className={selected === option.value ? "active" : ""}
          key={option.value}
          onClick={() => onChange(option.value)}
          role="tab"
          type="button"
        >
          <i className={option.icon} /> {option.label}
        </button>
      ))}
    </div>
  );
}

export function PricingPage() {
  const [plans, setPlans] = useState<PricePlanApiResponse[]>([]);
  const [rules, setRules] = useState<PriceRuleApiResponse[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeApiResponse[]>([]);
  const [ticketTypes, setTicketTypes] = useState<TicketTypeApiResponse[]>([]);
  const [selectedVehicleFilter, setSelectedVehicleFilter] = useState<VehicleFilterKey>("MOTORBIKE");
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

    return () => {
      ignore = true;
    };
  }, []);

  const visitorRules = useMemo(
    () => buildRules("VISITOR", plans, rules, vehicleTypes, ticketTypes),
    [plans, rules, ticketTypes, vehicleTypes],
  );
  const customerRules = useMemo(
    () => buildRules("CUSTOMER", plans, rules, vehicleTypes, ticketTypes),
    [plans, rules, ticketTypes, vehicleTypes],
  );
  const filteredVisitorRules = useMemo(
    () => visitorRules.filter((rule) => matchesVehicleFilter(rule, selectedVehicleFilter)),
    [selectedVehicleFilter, visitorRules],
  );
  const filteredCustomerRules = useMemo(
    () => customerRules.filter((rule) => matchesVehicleFilter(rule, selectedVehicleFilter)),
    [customerRules, selectedVehicleFilter],
  );
  const planRange = plans.length
    ? `${formatDate(plans[0]?.effectiveFrom ?? null)} - ${formatDate(plans[0]?.effectiveTo ?? null)}`
    : "Theo bảng giá đang hiệu lực";
  const selectedVehicleLabel = vehicleFilterLabel(selectedVehicleFilter);

  return (
    <ClientPage>
      <div className="vm-public-page vm-pricing-page">
        <section className="vm-pricing-hero-panel">
          <div>
            <h1>Bảng giá gửi xe</h1>
            <p>Minh bạch - Tiện lợi - An toàn</p>
          </div>
          <aside className="vm-current-plan-card">
            <span><i className="fas fa-tags" /> Bảng giá hiện hành</span>
            <strong>{loading ? "Đang tải dữ liệu bảng giá..." : planRange}</strong>
          </aside>
        </section>

        <section className="vm-pricing-section">
          {error ? <p className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-font-bold tw-text-red-600">{error}</p> : null}
          <VehicleFilterTabs selected={selectedVehicleFilter} onChange={setSelectedVehicleFilter} />
        </section>

        <section className="vm-pricing-section">
          <div className="vm-section-heading-lite">
            <h2><i className="fas fa-user" /> Khách vãng lai</h2>
          </div>
          <div className="vm-price-grid">
            {filteredVisitorRules.length
              ? filteredVisitorRules.map((rule) => <PriceCard audience="VISITOR" key={rule.priceRuleId} rule={rule} />)
              : <EmptyPricing label={`Khách vãng lai - ${selectedVehicleLabel}`} />}
          </div>
        </section>

        <section className="vm-pricing-section">
          <div className="vm-section-heading-lite">
            <h2><i className="fas fa-users" /> Khách đăng ký</h2>
          </div>
          <div className="vm-price-grid">
            {filteredCustomerRules.length
              ? filteredCustomerRules.map((rule) => <PriceCard audience="CUSTOMER" key={rule.priceRuleId} rule={rule} />)
              : <EmptyPricing label={`Khách đăng ký - ${selectedVehicleLabel}`} />}
          </div>
        </section>

        <PublicContactStrip />
      </div>
      <PublicFooter />
    </ClientPage>
  );
}
