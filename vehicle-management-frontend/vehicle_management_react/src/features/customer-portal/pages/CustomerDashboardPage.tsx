import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { canAccessCustomerRoute } from "@/app/routePermissions";
import { useAuth } from "@/core/auth/useAuth";
import {
  getSubscriptionInvoice,
  type InvoiceSummaryResponse,
} from "@/features/billing/api/invoicePaymentsApi";
import {
  getCustomerPortalLookups,
  getCustomerPortalProfile,
  getCustomerPortalVoucherBanners,
  getMyCustomerVehicles,
  getMySubscriptions,
  type CustomerPortalProfile,
  type CustomerPortalSubscription,
  type CustomerPortalTicketType,
  type CustomerPortalVehicle,
  type CustomerPortalVoucherBanner,
  type CustomerPortalVehicleType,
} from "@/features/customer-portal/api/customerPortalApi";
import { PortalTicketArtwork } from "@/features/customer-portal/components/PortalTicketArtwork";
import { PortalTicketFrame } from "@/features/customer-portal/components/PortalTicketFrame";
import { VehicleVisual } from "@/features/customer-portal/components/VehicleVisual";
import { VoucherPromotionBanner } from "@/features/customer-portal/components/VoucherPromotionBanner";
import { parsePortalDate } from "@/features/customer-portal/utils/portalDate";

import { CustomerPortalLayout, StatusPill } from "./PortalShared";

type StatusTone = "green" | "blue" | "orange" | "red" | "gray" | "purple";

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date);
}

function formatDateTime(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date);
}

function statusLabel(status?: string | null) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "PENDING") return "Chờ duyệt";
  if (status === "PENDING_PAYMENT") return "Chờ thanh toán";
  if (status === "PENDING_CARD") return "Chờ gán thẻ";
  if (status === "EXPIRED") return "Hết hạn";
  if (status === "CANCELLED") return "Đã hủy";
  if (status === "REJECTED") return "Từ chối";
  return status || "--";
}

function approvalLabel(status?: string | null) {
  if (status === "APPROVED") return "Đã duyệt";
  if (status === "PENDING") return "Chờ duyệt";
  if (status === "REJECTED") return "Từ chối";
  if (status === "SUSPENDED") return "Tạm khóa";
  return status || "--";
}

function approvalTone(status?: string | null): StatusTone {
  if (status === "APPROVED") return "green";
  if (status === "PENDING") return "orange";
  if (status === "REJECTED" || status === "SUSPENDED") return "red";
  return "gray";
}

function StatusBadge({ label, tone, checked = false }: { checked?: boolean; label: string; tone: StatusTone }) {
  return (
    <StatusPill tone={tone}>
      {checked ? <i className="fas fa-check-circle" /> : null}
      {label}
    </StatusPill>
  );
}

function formatCurrency(value?: number | string | null) {
  const numberValue = Number(value ?? 0);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(numberValue) ? numberValue : 0)} đ`;
}

function remainingDays(value?: string | null) {
  if (!value) return null;
  const end = new Date(value);
  if (Number.isNaN(end.getTime())) return null;
  return Math.max(0, Math.ceil((end.getTime() - Date.now()) / 86_400_000));
}

function vehicleLabel(vehicle?: CustomerPortalVehicle, vehicleTypeById?: Map<string, CustomerPortalVehicleType>) {
  if (!vehicle) return "--";
  const typeName = vehicle.vehicleTypeId ? vehicleTypeById?.get(vehicle.vehicleTypeId)?.name : "";
  return [vehicle.licensePlate, vehicle.brand, typeName].filter(Boolean).join(" - ");
}

function vehicleDescription(vehicle?: CustomerPortalVehicle, vehicleTypeById?: Map<string, CustomerPortalVehicleType>) {
  if (!vehicle) return "--";
  const typeName = vehicle.vehicleTypeId ? vehicleTypeById?.get(vehicle.vehicleTypeId)?.name : "";
  return [vehicle.brand, typeName].filter(Boolean).join(" - ") || "--";
}

function shortCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 14 ? `${value.slice(0, 8)}...${value.slice(-4)}` : value;
}

export function CustomerDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [subscriptions, setSubscriptions] = useState<CustomerPortalSubscription[]>([]);
  const [invoiceBySubscriptionId, setInvoiceBySubscriptionId] = useState<Record<string, InvoiceSummaryResponse>>({});
  const [voucherBanners, setVoucherBanners] = useState<CustomerPortalVoucherBanner[]>([]);
  const [ticketTypes, setTicketTypes] = useState<CustomerPortalTicketType[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadData() {
      setLoading(true);
      setError("");
      try {
        const nextProfile = await getCustomerPortalProfile();
        const needsOnboarding = nextProfile.onboardingRequired || !nextProfile.customer?.customerId;
        if (needsOnboarding) {
          if (ignore) return;
          setProfile(nextProfile);
          setVehicles([]);
          setSubscriptions([]);
          setInvoiceBySubscriptionId({});
          setVoucherBanners([]);
          setTicketTypes([]);
          setVehicleTypes([]);
          return;
        }
        const [nextVehicles, nextSubscriptions, lookups, nextVoucherBanners] = await Promise.all([
          getMyCustomerVehicles(nextProfile),
          getMySubscriptions(nextProfile),
          getCustomerPortalLookups(),
          getCustomerPortalVoucherBanners().catch(() => []),
        ]);
        const invoiceEntries = await Promise.all(nextSubscriptions.map(async (subscription) => {
          try {
            const invoice = await getSubscriptionInvoice(subscription.subscriptionId);
            return invoice ? [subscription.subscriptionId, invoice] as const : null;
          } catch {
            return null;
          }
        }));
        if (ignore) return;
        setProfile(nextProfile);
        setVehicles(nextVehicles);
        setSubscriptions(nextSubscriptions);
        setInvoiceBySubscriptionId(Object.fromEntries(
          invoiceEntries.filter((entry): entry is readonly [string, InvoiceSummaryResponse] => entry !== null),
        ));
        setVoucherBanners(nextVoucherBanners);
        setTicketTypes(lookups.ticketTypes);
        setVehicleTypes(lookups.vehicleTypes);
      } catch (requestError) {
        if (!ignore) setError(requestError instanceof Error ? requestError.message : "Không thể tải tổng quan khách hàng.");
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    void loadData();
    return () => {
      ignore = true;
    };
  }, []);

  const vehicleById = useMemo(() => new Map(vehicles.map((vehicle) => [vehicle.customerVehicleId, vehicle])), [vehicles]);
  const ticketTypeById = useMemo(() => new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType])), [ticketTypes]);
  const vehicleTypeById = useMemo(() => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])), [vehicleTypes]);
  const getSubscriptionTotal = (subscription?: CustomerPortalSubscription | null) => (
    subscription ? invoiceBySubscriptionId[subscription.subscriptionId]?.finalAmount ?? subscription.price : null
  );
  const dashboardVoucherBanners = voucherBanners.filter((banner) => banner.showOnDashboard);
  const activeSubscription = subscriptions.find((subscription) => subscription.status === "ACTIVE");
  const pendingSubscriptions = subscriptions.filter((subscription) => subscription.status?.startsWith("PENDING"));
  const defaultVehicle = vehicles.find((vehicle) => vehicle.isDefault);
  const recentSubscriptions = [...subscriptions]
    .sort((a, b) => (parsePortalDate(b.createdAt)?.getTime() ?? 0) - (parsePortalDate(a.createdAt)?.getTime() ?? 0))
    .slice(0, 3);
  const displayName = profile?.profile?.fullName ?? profile?.account?.username ?? "Khách hàng";
  const approvalStatus = profile?.customer?.customerApprovalStatus;
  const needsOnboarding = Boolean(profile?.onboardingRequired || (profile && !profile.customer?.customerId));
  const activeVehicles = vehicles.filter((vehicle) => vehicle.status === "ACTIVE").length;
  const inactiveVehicles = vehicles.filter((vehicle) => vehicle.status !== "ACTIVE").length;
  const canViewParkingHistory = canAccessCustomerRoute(user, "/customer/parking-history");
  const canViewSupport = canAccessCustomerRoute(user, "/customer/support");
  const daysRemaining = remainingDays(activeSubscription?.effectiveTo);
  const panelClass = "tw-overflow-hidden tw-rounded-[12px] tw-border tw-border-solid tw-border-[#e0e8f3] tw-bg-white tw-p-4 tw-shadow-[0_7px_18px_rgba(15,23,42,0.055)]";
  const panelTitleClass = "tw-m-0 tw-mb-3.5 tw-flex tw-items-center tw-gap-3 tw-text-base tw-font-bold tw-text-[#14213d] [&>i]:tw-text-[#0d62de]";

  return (
    <CustomerPortalLayout>
      {error ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {error}</div> : null}
      {loading ? <div className="vm-info-note"><i className="fas fa-spinner fa-spin" /> Đang tải dữ liệu khách hàng...</div> : null}

      {needsOnboarding ? (
        <section className="vm-info-note tw-items-start tw-justify-between tw-gap-4 tw-bg-amber-50 tw-text-amber-800">
          <div className="tw-flex tw-min-w-0 tw-gap-3">
            <i className="fas fa-exclamation-circle tw-mt-1" />
            <div>
              <strong className="tw-block tw-text-vm-slate-900">Cần hoàn tất hồ sơ khách hàng</strong>
              <span className="tw-mt-1 tw-block">Bổ sung thông tin cần thiết và gửi hồ sơ để nhân viên phê duyệt trước khi đăng ký vé.</span>
            </div>
          </div>
          <Link className="vm-outline-action tw-flex-shrink-0 tw-bg-white hover:tw-no-underline" to="/customer/profile">
            <i className="fas fa-paper-plane" /> Hoàn tất hồ sơ
          </Link>
        </section>
      ) : null}

      {dashboardVoucherBanners.length > 0 ? <div className="tw-mb-4"><VoucherPromotionBanner vouchers={dashboardVoucherBanners} /></div> : null}

      <section className="tw-relative tw-min-h-[374px] max-[1100px]:tw-min-h-[370px] tw-overflow-hidden tw-rounded-[14px] tw-bg-[#031632] tw-px-10 tw-py-8 tw-pb-[7.5rem] max-[1100px]:tw-px-6 max-[640px]:tw-px-4 max-[640px]:tw-pb-8 tw-text-white tw-shadow-sm">
        <img alt="" aria-hidden="true" src="/assets/customer/portal/smart-parking-hero-full-car-v2.png" className="tw-absolute tw-inset-0 tw-h-full tw-w-full tw-object-cover tw-object-[center_60%] max-[640px]:tw-opacity-40" />
        <div aria-hidden="true" className="tw-absolute tw-inset-0 tw-bg-[linear-gradient(90deg,rgba(2,17,49,.8),rgba(2,17,49,.3)_48%,transparent_70%)]" />
        <div className="tw-relative tw-z-[1] tw-max-w-[510px]">
          <h1 className="tw-m-0 tw-font-[Cambria,Georgia,serif] tw-text-[clamp(2.35rem,4vw,3.6rem)] tw-font-bold tw-leading-[1.04] tw-text-white">Chào ngày mới,<br /><span className="!tw-font-[Cambria,Georgia,serif] tw-text-[#f3c45d]">{displayName}</span></h1>
          <strong className="tw-mt-3.5 tw-block tw-text-[1.06rem] tw-font-semibold tw-text-[#d8e7ff]">Mọi hành trình đã sẵn sàng</strong>
          <div className="tw-mt-5 tw-flex tw-flex-wrap tw-gap-3.5">
            <Link className="tw-inline-flex tw-min-h-12 tw-items-center tw-gap-3 tw-rounded-[9px] tw-border tw-border-solid tw-border-[#1674ff] tw-bg-[linear-gradient(135deg,#146cf3,#0756d8)] tw-px-[1.05rem] tw-py-3 tw-font-semibold tw-text-white tw-shadow-[0_10px_18px_rgba(0,0,0,0.18)] hover:tw-translate-y-[-1px] hover:tw-text-white hover:tw-no-underline" to={needsOnboarding ? "/customer/profile" : "/customer/subscriptions"}><i className="fas fa-ticket-alt" />{needsOnboarding ? "Hoàn tất hồ sơ" : "Đăng ký vé"}<i className="fas fa-chevron-right tw-ml-2 tw-text-[0.75rem]" /></Link>
            <Link className="tw-inline-flex tw-min-h-12 tw-items-center tw-gap-3 tw-rounded-[9px] tw-border tw-border-solid tw-border-white/45 tw-bg-[#03183d]/40 tw-px-[1.05rem] tw-py-3 tw-font-semibold tw-text-white tw-shadow-[0_10px_18px_rgba(0,0,0,0.18)] hover:tw-translate-y-[-1px] hover:tw-text-white hover:tw-no-underline" to="/customer/vehicles"><i className="fas fa-car-side" />Quản lý xe<i className="fas fa-chevron-right tw-ml-2 tw-text-[0.75rem]" /></Link>
          </div>
        </div>
        <div className="tw-absolute tw-bottom-4 tw-left-10 max-[1100px]:tw-left-6 max-[640px]:tw-static max-[640px]:tw-mt-6 max-[640px]:tw-w-full tw-z-[2] tw-flex tw-w-[min(690px,calc(100%_-_5.5rem))] tw-rounded-xl tw-border tw-border-solid tw-border-[#99c1ff]/30 tw-bg-[#03173d]/80 tw-px-3.5 tw-py-4 tw-backdrop-blur">
          {[["fas fa-car-side", String(vehicles.length).padStart(2, "0"), "Xe đã đăng ký"], ["fas fa-ticket-alt", String(subscriptions.filter((item) => item.status === "ACTIVE").length).padStart(2, "0"), "Vé đang hoạt động"], ["far fa-file-alt", String(pendingSubscriptions.length).padStart(2, "0"), "Yêu cầu đang xử lý"]].map(([icon, value, label], index) => <div className={`tw-grid tw-flex-1 tw-grid-cols-[42px_minmax(0,1fr)] max-[640px]:tw-grid-cols-1 tw-grid-rows-2 tw-items-center tw-gap-x-2.5 tw-px-4 ${index < 2 ? "tw-border-0 tw-border-r tw-border-solid tw-border-white/20" : ""}`} key={label}><i className={`${icon} tw-row-span-2 tw-grid tw-h-[38px] tw-w-[38px] tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-white/40 tw-text-[1.1rem] tw-text-[#dceaff]`} /><strong className="tw-text-[1.55rem] tw-leading-none">{value}</strong><span className="tw-text-[0.76rem] tw-font-bold tw-text-[#cbdaf1]">{label}</span></div>)}
        </div>
      </section>

      <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)] max-[900px]:tw-grid-cols-1 tw-gap-4">
        <div className="tw-grid tw-min-w-0 tw-content-start tw-gap-4">
          <section className={panelClass}>
            <h2 className={panelTitleClass}><i className="fas fa-ticket-alt" /> Vé tháng đang sử dụng</h2>
            {activeSubscription ? (
              <div className="tw-relative tw-isolate tw-min-h-[200px] tw-text-white">
                <PortalTicketFrame />
                <PortalTicketArtwork className="tw-pointer-events-none tw-absolute tw-bottom-5 tw-left-8 tw-h-[90px] tw-w-[38%] tw-text-[#b9a366]/20" />
                <div className="tw-relative tw-grid tw-grid-cols-[minmax(0,2.15fr)_minmax(100px,1fr)_minmax(116px,1.15fr)_minmax(64px,.65fr)] tw-gap-y-5 tw-px-9 tw-py-7 max-[1100px]:tw-px-6 max-[640px]:tw-grid-cols-2 max-[640px]:tw-gap-y-6">
                  <div className="tw-row-span-2 tw-min-w-0 tw-border-0 tw-border-r tw-border-solid tw-border-white/15 tw-pr-5 max-[640px]:tw-row-span-1">
                    <span className="tw-block tw-text-[0.66rem] tw-font-normal tw-leading-4 tw-text-[#c5cee0]">LOẠI VÉ</span>
                    <strong className="tw-mt-1.5 tw-block !tw-font-[Cambria,Georgia,serif] tw-text-[1.2rem] tw-font-semibold tw-leading-[1.3] tw-text-[#f1d178]">{ticketTypeById.get(activeSubscription.ticketTypeId)?.name ?? "Vé tháng CoParking"}</strong>
                  </div>
                  <div className="tw-min-w-0 tw-border-0 tw-border-r tw-border-solid tw-border-white/15 tw-px-5 max-[1400px]:tw-px-3 max-[640px]:tw-border-r-0">
                    <span className="tw-block tw-text-[0.66rem] tw-font-normal tw-leading-4 tw-text-[#c5cee0]">BIỂN SỐ XE</span>
                    <strong className="tw-mt-2 tw-block tw-whitespace-nowrap tw-text-[0.92rem] tw-font-medium">{vehicleById.get(activeSubscription.customerVehicleId)?.licensePlate ?? "--"}</strong>
                  </div>
                  <div className="tw-min-w-0 tw-border-0 tw-border-r tw-border-solid tw-border-white/15 tw-px-5 max-[1400px]:tw-px-3 max-[640px]:tw-px-0">
                    <span className="tw-block tw-text-[0.66rem] tw-font-normal tw-leading-4 tw-text-[#c5cee0]">HIỆU LỰC</span>
                    <strong className="tw-mt-2 tw-block tw-whitespace-nowrap tw-text-[0.92rem] tw-font-medium tw-leading-6">{formatDate(activeSubscription.effectiveFrom)}<br />– {formatDate(activeSubscription.effectiveTo)}</strong>
                  </div>
                  <div className="tw-min-w-0 tw-pl-3 tw-text-center">
                    <span className="tw-block tw-whitespace-nowrap tw-text-[0.66rem] tw-font-normal tw-leading-4 tw-text-[#c5cee0]">CÒN LẠI</span>
                    <strong className="tw-mt-1 tw-block !tw-font-[Cambria,Georgia,serif] tw-text-[2.75rem] tw-font-normal tw-leading-none tw-text-[#f1d178]">{daysRemaining ?? "--"}</strong>
                    <small className="tw-block tw-text-[0.72rem] tw-leading-4 tw-text-[#d3d8e3]">ngày</small>
                  </div>
                  <Link className="tw-col-start-3 tw-col-span-2 tw-inline-flex tw-min-h-10 tw-min-w-[168px] tw-items-center tw-justify-center tw-justify-self-end tw-gap-5 tw-rounded-[7px] tw-border tw-border-solid tw-border-[#efcf73] tw-bg-[linear-gradient(110deg,#f9e49b,#e7bb55)] tw-px-5 tw-py-2 tw-text-[0.92rem] tw-font-semibold tw-text-[#282719] tw-shadow-[inset_0_1px_0_rgba(255,255,255,.45),0_2px_6px_rgba(0,0,0,.12)] hover:tw-text-[#282719] hover:tw-no-underline focus-visible:tw-outline focus-visible:tw-outline-2 focus-visible:tw-outline-offset-2 focus-visible:tw-outline-[#f1d178] max-[640px]:tw-col-start-1" to="/customer/subscriptions">Xem chi tiết <i aria-hidden="true" className="fas fa-chevron-right tw-text-[0.72rem]" /></Link>
                </div>
              </div>
            ) : <div className="tw-grid tw-min-h-[155px] tw-place-content-center tw-justify-items-center tw-gap-2 tw-text-center tw-text-[#64748b]"><i className="fas fa-ticket-alt tw-text-[1.6rem] tw-text-[#95b9ee]" /><strong className="tw-text-[#334155]">Chưa có vé tháng hoạt động</strong><Link className="tw-text-[0.84rem] tw-font-semibold tw-text-[#075fe4]" to="/customer/subscriptions">Đăng ký vé ngay <i className="fas fa-arrow-right" /></Link></div>}
          </section>

          <section className={panelClass}>
            <h2 className={panelTitleClass}><i className="far fa-file-alt" /> Đăng ký gần đây</h2>
            <div>
              {recentSubscriptions.map((subscription, index) => {
                const vehicle = vehicleById.get(subscription.customerVehicleId);
                const ticket = ticketTypeById.get(subscription.ticketTypeId);
                const createdAt = parsePortalDate(subscription.createdAt);
                const active = subscription.status === "ACTIVE";
                return (
                  <div className="tw-relative tw-pl-5" key={subscription.subscriptionId}>
                    <span aria-hidden="true" className={`tw-pointer-events-none tw-absolute tw-left-[5px] tw-w-px tw-bg-[#9bc4fa] ${index === 0 ? "tw-top-1/2" : "tw-top-0"} ${index === recentSubscriptions.length - 1 ? "tw-bottom-1/2" : "tw-bottom-0"}`} />
                    <span aria-hidden="true" className={`tw-pointer-events-none tw-absolute tw-left-0 tw-top-1/2 tw-h-[11px] tw-w-[11px] tw-rounded-full tw-border-2 tw-border-solid tw-border-[#1263e9] [transform:translateY(-50%)] ${active ? "tw-bg-[#1263e9]" : "tw-bg-white"}`} />
                    <button
                      className={`tw-grid tw-min-h-[58px] tw-w-full tw-grid-cols-[74px_minmax(0,1.2fr)_minmax(0,1.2fr)_max-content_max-content] tw-items-center tw-gap-3 tw-border tw-border-solid tw-border-[#e8edf4] tw-bg-white tw-px-3 tw-py-2 tw-text-left tw-text-[0.72rem] tw-font-normal tw-text-[#263a5d] hover:tw-bg-[#f6faff] max-[1100px]:tw-gap-2 max-[640px]:tw-grid-cols-[70px_minmax(0,1fr)_max-content] ${index > 0 ? "tw-border-t-0" : "tw-rounded-t-md"} ${index === recentSubscriptions.length - 1 ? "tw-rounded-b-md" : ""}`}
                      type="button"
                      onClick={() => navigate(`/customer/subscriptions?subscriptionId=${encodeURIComponent(subscription.subscriptionId)}`)}
                    >
                      {createdAt ? (
                        <time dateTime={createdAt.toISOString()} className="tw-grid tw-gap-1 tw-whitespace-nowrap">
                          {new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: "Asia/Ho_Chi_Minh" }).format(createdAt)}
                          <small className="tw-text-[0.68rem] tw-text-[#71819a]">{new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Ho_Chi_Minh" }).format(createdAt)}</small>
                        </time>
                      ) : <span className="tw-text-[0.68rem] tw-text-[#71819a]" title="Ngày đăng ký chưa được cung cấp hoặc không hợp lệ">Chưa có ngày đăng ký</span>}
                      <span className="tw-grid tw-min-w-0 tw-gap-1"><span className="tw-text-[#223554]">{ticket?.name ?? "Vé tháng CoParking"}</span><small className="tw-text-[0.68rem] tw-text-[#71819a]">{vehicle?.licensePlate ?? "Chưa gán xe"}</small></span>
                      <span className="max-[640px]:tw-col-start-2 max-[640px]:tw-row-start-2">{formatDate(subscription.effectiveFrom ?? subscription.requestedEffectiveFrom)} – {formatDate(subscription.effectiveTo)}</span>
                      <span className="tw-whitespace-nowrap tw-text-[#172c4b]">{formatCurrency(getSubscriptionTotal(subscription))}</span>
                      <span className={`tw-inline-flex tw-items-center tw-justify-self-end tw-gap-1 tw-whitespace-nowrap tw-rounded-md tw-border tw-border-solid tw-px-2 tw-py-1.5 tw-text-[0.64rem] max-[640px]:tw-col-start-3 max-[640px]:tw-row-start-2 ${active ? "tw-border-[#ccebd8] tw-bg-[#effaf3] tw-text-[#17824f]" : subscription.status?.startsWith("PENDING") ? "tw-border-[#f5dfb9] tw-bg-[#fff8ed] tw-text-[#b77516]" : "tw-border-[#e3e7ee] tw-bg-[#f8fafc] tw-text-[#64748b]"}`}>
                        <span aria-hidden="true" className="tw-h-1.5 tw-w-1.5 tw-shrink-0 tw-rounded-full tw-bg-current" />{statusLabel(subscription.status)}
                      </span>
                    </button>
                  </div>
                );
              })}
              {!loading && recentSubscriptions.length === 0 ? <div className="tw-px-4 tw-py-8 tw-text-center tw-text-[0.84rem] tw-text-[#71819a]">Chưa có đăng ký vé nào.</div> : null}
            </div>
            <Link className="tw-mt-2.5 tw-flex tw-items-center tw-justify-center tw-gap-3 tw-text-[0.74rem] tw-font-normal tw-text-[#1767df] hover:tw-no-underline" to="/customer/subscriptions">Xem tất cả <i aria-hidden="true" className="fas fa-chevron-right tw-text-[0.6rem]" /></Link>
          </section>
        </div>

        <aside className="tw-grid tw-min-w-0 tw-content-start tw-gap-4">
          <section className={`${panelClass} tw-grid tw-justify-items-center tw-text-center [&>span[role=img]]:tw-h-[126px] [&>h2]:tw-mb-1`}>
            <h2 className={`${panelTitleClass} tw-justify-self-stretch`}><i className="fas fa-car-side" /> Xe mặc định</h2>
            {defaultVehicle ? <>
              <VehicleVisual
                color={defaultVehicle.color}
                size="dashboard"
                typeName={defaultVehicle.vehicleTypeId ? vehicleTypeById.get(defaultVehicle.vehicleTypeId)?.name : undefined}
              />
              <strong className="tw-mt-1 tw-text-[1.25rem] tw-text-[#1a2945]">{defaultVehicle.licensePlate}</strong>
              <span className="tw-mt-1 tw-text-[0.8rem] tw-text-[#70819b]">{vehicleDescription(defaultVehicle, vehicleTypeById)}</span>
            </> : <div className="tw-px-4 tw-py-8 tw-text-[0.84rem] tw-text-[#71819a]">Chưa chọn xe mặc định.</div>}
            <Link className="tw-mt-2 tw-w-[80%] tw-rounded-md tw-border tw-border-solid tw-border-[#7aa9ff] tw-p-2 tw-text-[0.82rem] tw-font-medium tw-text-[#0759d8] hover:tw-text-[#0759d8] hover:tw-no-underline" to="/customer/vehicles">Quản lý xe <i className="fas fa-chevron-right tw-ml-3 tw-text-[0.72rem]" /></Link>
          </section>

          <section className={`${panelClass} !tw-pb-0`}>
            <h2 className={panelTitleClass}><i className="far fa-star" /> Lối tắt của bạn</h2>
            {[["/customer/profile", "fas fa-user", "Cập nhật hồ sơ", "Cập nhật thông tin cá nhân"], ...(canViewParkingHistory ? [["/customer/parking-history", "far fa-clock", "Lịch sử gửi xe", "Xem lịch sử gửi xe của bạn"]] : []), ...(canViewSupport ? [["/customer/support", "fas fa-headset", "Trung tâm hỗ trợ", "Liên hệ hỗ trợ khi cần thiết"]] : [])].map(([to, icon, title, description]) => <Link className="tw-mx-[-1rem] tw-grid tw-grid-cols-[38px_minmax(0,1fr)_16px] tw-items-center tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-[#e9eef5] tw-px-4 tw-py-3 tw-text-[#203554] hover:tw-bg-[#f7faff] hover:tw-text-[#203554] hover:tw-no-underline" key={to} to={to}><i className={`${icon} tw-grid tw-h-[34px] tw-w-[34px] tw-place-items-center tw-rounded-lg tw-bg-[#edf4ff] tw-text-[#1263e9]`} /><span className="tw-grid tw-min-w-0 tw-gap-0.5"><strong className="tw-text-[0.83rem]">{title}</strong><small className="tw-text-[0.7rem] tw-text-[#7a8ba4]">{description}</small></span><b className="tw-text-2xl tw-font-normal">›</b></Link>)}
          </section>
        </aside>
      </div>

      <section className="tw-mt-4 tw-flex tw-items-center tw-gap-4 tw-rounded-[11px] tw-bg-[linear-gradient(100deg,#051d4d,#072969)] tw-px-[1.4rem] tw-py-4 tw-text-white"><span className="tw-grid tw-h-[42px] tw-w-[42px] tw-place-items-center tw-rounded-full tw-border tw-border-solid tw-border-[#d7ad5a] tw-text-[#f2cc76]"><i className="fas fa-headset" /></span><div className="tw-min-w-0"><strong className="tw-text-[0.98rem]">Chúng tôi luôn sẵn sàng hỗ trợ bạn</strong><p className="tw-m-0 tw-mt-0.5 tw-text-[0.78rem] tw-text-[#bccce7]">Đội ngũ CoParking luôn đồng hành cùng bạn trên mọi hành trình.</p></div>{canViewSupport ? <Link className="tw-ml-auto tw-whitespace-nowrap tw-rounded-[7px] tw-border tw-border-solid tw-border-[#d7ad5a] tw-px-4 tw-py-2.5 tw-text-[0.82rem] tw-font-semibold tw-text-[#f5ce7d] hover:tw-text-[#f5ce7d] hover:tw-no-underline" to="/customer/support">Liên hệ hỗ trợ <i className="fas fa-chevron-right tw-ml-2 tw-text-[0.7rem]" /></Link> : null}</section>

    </CustomerPortalLayout>
  );
}
