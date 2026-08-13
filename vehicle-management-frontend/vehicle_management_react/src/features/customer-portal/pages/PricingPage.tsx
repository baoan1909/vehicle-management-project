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

function vehicleIcon(vehicleType?: VehicleTypeApiResponse) {
  const category = vehicleCategory(vehicleType);
  if (category === "MOTORBIKE") return "fas fa-motorcycle";
  if (category === "CAR") return "fas fa-car";
  return "fas fa-truck";
}

function toneForRule(audience: PricingAudience, rule: DisplayRule) {
  if (audience === "CUSTOMER") return "subscription";
  if (rule.timeFrom && rule.timeFrom >= "18:00") return "night";
  return "day";
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
  const description = audience === "CUSTOMER" ? "Gói gửi xe theo chu kỳ" : "Áp dụng theo lượt gửi xe";
  const tone = toneForRule(audience, rule);

  return (
    <article className={`vm-price-card vm-price-card-${tone}`}>
      <div className="vm-price-card-head">
        <span className="vm-price-icon"><i className={iconForRule(audience, rule)} /></span>
        <div>
          <h3>{rule.ruleName || ticketName}</h3>
          <p>{description}</p>
        </div>
      </div>
      <div className="vm-price-amount">
        <strong className={colorClass}>{formatCurrency(rule.basePrice)}</strong>
        <span>{unitLabel(audience, rule.unit, rule.ticketType)}</span>
      </div>
      <div className="vm-price-facts">
        <span><i className={vehicleIcon(rule.vehicleType)} /> {vehicleName}</span>
        <span><i className={tone === "night" ? "fas fa-moon" : tone === "day" ? "far fa-sun" : "far fa-calendar-check"} /> {ticketName}</span>
        <span><i className="far fa-clock" /> {timeRange(rule)}</span>
      </div>
      {rule.lostCardFee ? (
        <div className="vm-price-fee">
          <i className="fas fa-receipt" />
          <span>Phí mất thẻ:</span>
          <strong>{formatCurrency(rule.lostCardFee)}</strong>
        </div>
      ) : null}
      {audience === "CUSTOMER" ? (
        <Link to="/customer/subscriptions">Đăng ký {ticketName.toLocaleLowerCase("vi-VN")}</Link>
      ) : null}
    </article>
  );
}

function EmptyPricing({ label }: { label: string }) {
  return (
    <article className="vm-price-card vm-price-card-empty">
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
      <p className="vm-price-empty-note"><i className="fas fa-info-circle" /> Kiểm tra kế hoạch và quy tắc giá đang hiệu lực.</p>
    </article>
  );
}

function PricingSectionHeading({ icon, planName, title }: { icon: string; planName?: string; title: string }) {
  return (
    <div className="vm-section-heading-lite">
      <h2><i className={icon} /> {title}</h2>
      {planName ? <span className="vm-price-plan-label"><i className="fas fa-receipt" /> {planName}</span> : null}
    </div>
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
  const visitorPlanName = filteredVisitorRules.find((rule) => rule.plan)?.plan?.name
    ?? visitorRules.find((rule) => rule.plan)?.plan?.name;
  const customerPlanName = filteredCustomerRules.find((rule) => rule.plan)?.plan?.name
    ?? customerRules.find((rule) => rule.plan)?.plan?.name;

  return (
    <ClientPage>
      <div className="vm-public-page vm-pricing-page">
        <section className="vm-pricing-hero-panel">
          <div>
            <h1>Bảng giá gửi xe</h1>
            <p>Minh bạch - Tiện lợi - An toàn</p>
          </div>
          <aside className="vm-current-plan-card">
            <span><i className="far fa-calendar-alt" /> Bảng giá hiện hành</span>
            <strong>{loading ? "Đang tải dữ liệu bảng giá..." : planRange}</strong>
          </aside>
        </section>

        <section className="vm-pricing-section">
          {error ? <p className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-font-bold tw-text-red-600">{error}</p> : null}
          <VehicleFilterTabs selected={selectedVehicleFilter} onChange={setSelectedVehicleFilter} />
        </section>

        <section className="vm-pricing-section">
          <PricingSectionHeading icon="fas fa-user-friends" planName={visitorPlanName} title="Khách vãng lai" />
          <div className="vm-price-grid">
            {filteredVisitorRules.length
              ? filteredVisitorRules.map((rule) => <PriceCard audience="VISITOR" key={rule.priceRuleId} rule={rule} />)
              : <EmptyPricing label={`Khách vãng lai - ${selectedVehicleLabel}`} />}
          </div>
        </section>

        <section className="vm-pricing-section">
          <PricingSectionHeading icon="fas fa-users" planName={customerPlanName} title="Khách đăng ký" />
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
