import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useSearchParams } from "react-router-dom";

import {
  createVnpayInvoicePayment,
  getSubscriptionInvoice,
  type InvoiceSummaryResponse,
  VNPAY_MINIMUM_AMOUNT,
} from "@/features/billing/api/invoicePaymentsApi";
import {
  createMySubscription,
  getCustomerPortalLookups,
  getCustomerPortalProfile,
  getCustomerPortalVoucherBanners,
  getMyCustomerVehicles,
  getMySubscriptions,
  quoteMySubscriptionVoucher,
  type CustomerPortalPriceRule,
  type CustomerPortalProfile,
  type CustomerPortalSubscription,
  type CustomerPortalTicketType,
  type CustomerPortalVehicle,
  type CustomerPortalSubscriptionVoucherQuote,
  type CustomerPortalVoucherBanner,
  type CustomerPortalVehicleType,
} from "@/features/customer-portal/api/customerPortalApi";

import { PortalTicketArtwork } from "../components/PortalTicketArtwork";
import { SubscriptionTicketFrame } from "../components/SubscriptionTicketFrame";
import { VoucherPromotionBanner } from "../components/VoucherPromotionBanner";
import { parsePortalDate } from "../utils/portalDate";
import { getSubscriptionPeriod } from "../utils/subscriptionPeriod";
import { CustomerPortalLayout, PortalPagination } from "./PortalShared";
import { DatePicker, Modal, useToast } from "@/components/ui";

type SubscriptionForm = {
  customerVehicleId: string;
  requestedEffectiveFrom: string;
  ticketTypeId: string;
  voucherCode: string;
};

type CustomerPaymentChoice = "VNPAY" | "AT_COUNTER";

const today = toDateInputValue(new Date())!;

function formatCurrency(value?: number | string | null) {
  const numberValue = Number(value ?? 0);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(numberValue) ? numberValue : 0)} đ`;
}

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date);
}

function formatDateRange(from?: string | null, to?: string | null) {
  return `${formatDate(from)} - ${to ? formatDate(to) : "Chưa xác định"}`;
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

function toDateOnly(value?: string | null) {
  if (!value) return null;
  const date = parsePortalDate(value);
  if (!date) return null;
  date.setHours(0, 0, 0, 0);
  return date;
}

function toDateInputValue(date: Date | null) {
  if (!date) return null;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getDisplayEffectiveFrom(subscription?: CustomerPortalSubscription) {
  if (!subscription) return null;
  const candidates = [
    subscription.effectiveFrom,
    subscription.requestedEffectiveFrom,
    subscription.approvedAt,
    subscription.cardReceiptDate,
  ]
    .map(toDateOnly)
    .filter((date): date is Date => Boolean(date));

  if (candidates.length === 0) return null;
  return new Date(Math.max(...candidates.map((date) => date.getTime())));
}

function compactCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 18 ? `${value.slice(0, 8)}…${value.slice(-4)}` : value;
}

function vehicleLabel(vehicle?: CustomerPortalVehicle, vehicleTypeById?: Map<string, CustomerPortalVehicleType>) {
  if (!vehicle) return "--";
  const typeName = vehicle.vehicleTypeId ? vehicleTypeById?.get(vehicle.vehicleTypeId)?.name : "";
  return [vehicle.licensePlate, vehicle.brand, typeName].filter(Boolean).join(" - ");
}

function findMatchingPriceRule(
  vehicle?: CustomerPortalVehicle,
  ticketTypeId?: string,
  priceRules: CustomerPortalPriceRule[] = [],
) {
  if (!vehicle?.vehicleTypeId || !ticketTypeId) return undefined;
  return priceRules.find((rule) => (
    rule.isActive !== false
    && rule.vehicleTypeId === vehicle.vehicleTypeId
    && rule.ticketTypeId === ticketTypeId
  ));
}

function subscriptionStatusClass(status?: string | null) {
  if (status === "ACTIVE") return "tw-bg-[#eaf9f0] tw-text-[#078553] tw-ring-[#b9ebcc]";
  if (status === "PENDING" || status === "PENDING_PAYMENT" || status === "PENDING_CARD") return "tw-bg-[#fff5e4] tw-text-[#d66b00] tw-ring-[#ffd59b]";
  if (status === "CANCELLED" || status === "REJECTED") return "tw-bg-[#fff0f0] tw-text-[#d83434] tw-ring-[#ffcaca]";
  return "tw-bg-[#f0f3f7] tw-text-[#687893] tw-ring-[#dce3ec]";
}

function SubscriptionStatusPill({ status, onTicket = false }: { status?: string | null; onTicket?: boolean }) {
  const ticketTone = status === "ACTIVE" ? "tw-text-[#cee8db]"
    : status?.startsWith("PENDING") ? "tw-text-[#f6d58c]"
    : status === "CANCELLED" || status === "REJECTED" ? "tw-text-[#ffc3c3]" : "tw-text-[#c4ccda]";
  const colorClass = onTicket
    ? `tw-border tw-border-solid tw-border-white/25 tw-bg-white/[.025] ${ticketTone}`
    : subscriptionStatusClass(status);
  return <span className={`tw-inline-flex tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-full tw-px-2.5 tw-py-1.5 tw-text-[0.72rem] tw-font-normal ${colorClass}`}><span aria-hidden="true" className="tw-h-[7px] tw-w-[7px] tw-shrink-0 tw-rounded-full tw-bg-current" />{statusLabel(status)}</span>;
}

type SubscriptionIconName = "ticket" | "calendar" | "clock" | "wallet" | "vehicle";

function SubscriptionIcon({ name, className = "" }: { name: SubscriptionIconName; className?: string }) {
  return <svg aria-hidden="true" className={`tw-h-6 tw-w-6 tw-shrink-0 ${className}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    {name === "ticket" ? <g transform="rotate(-18 12 12)"><path d="M3 5h18v4a3 3 0 0 0 0 6v4H3v-4a3 3 0 0 0 0-6Z" /><path d="M16 5v2m0 3v1m0 3v1m0 3v1" /></g> : null}
    {name === "calendar" ? <><rect x="3" y="5" width="18" height="17" rx="2" /><path d="M7 2v6m10-6v6M3 11h18" /></> : null}
    {name === "clock" ? <><circle cx="12" cy="12" r="9" /><path d="M12 6v6l4 3" /></> : null}
    {name === "wallet" ? <><path d="M4 6l13-3 1 4M3 7h16a2 2 0 0 1 2 2v11H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2" /><path d="M21 12h-6v5h6m-3-2.5h.01" /></> : null}
    {name === "vehicle" ? <><path d="M4 11l2-6h12l2 6M5 18v3m14-3v3M3 11h18v7H3Zm0 3h4m10 0h4M8 18h8" /></> : null}
  </svg>;
}

function SubscriptionStat({ icon, label, value, monetary = false }: { icon: SubscriptionIconName; label: string; value: ReactNode; monetary?: boolean }) {
  return <article className="tw-flex tw-min-h-[142px] tw-min-w-0 tw-items-center tw-gap-3 tw-rounded-[14px] tw-border tw-border-solid tw-border-[#e0e4ec] tw-bg-white tw-px-4 tw-shadow-[0_2px_5px_rgba(18,48,88,0.065)] max-[1200px]:tw-gap-2 max-[1200px]:tw-px-3">
    <span className="tw-grid tw-h-14 tw-w-14 tw-shrink-0 tw-place-items-center tw-rounded-full tw-bg-[linear-gradient(125deg,#f6fafc,#eaf2ff)] tw-text-[#1263bd] max-[1200px]:tw-h-12 max-[1200px]:tw-w-12"><SubscriptionIcon name={icon} className="!tw-h-8 !tw-w-8" /></span>
    <span className="tw-min-w-0"><strong className={`tw-block tw-whitespace-nowrap tw-font-semibold tw-leading-none tw-text-[#081332] ${monetary ? "tw-text-[clamp(1rem,1.65vw,1.65rem)]" : "tw-text-[2.3rem]"}`}>{value}</strong><small className="tw-mt-3 tw-block tw-text-[0.8rem] tw-font-normal tw-text-[#424c60]">{label}</small></span>
  </article>;
}

function SubscriptionProgress({ percent }: { percent: number }) {
  return <svg role="img" aria-label={`${percent}% thời hạn còn lại`} className="tw-block tw-h-[9px] tw-w-full tw-overflow-hidden tw-rounded-full" viewBox="0 0 100 9" preserveAspectRatio="none"><rect width="100" height="9" fill="#334660" /><rect width={percent} height="9" fill="#f5c866" /><path d={`M0 2H${percent}`} stroke="#ffe8a8" strokeWidth="1.5" /></svg>;
}

export function SubscriptionPage() {
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const voucherCodeFromUrl = searchParams.get("voucherCode")?.trim().toUpperCase() ?? "";
  const handledVnpayReturnRef = useRef(false);
  const voucherQuoteRequestRef = useRef(0);
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [subscriptions, setSubscriptions] = useState<CustomerPortalSubscription[]>([]);
  const [invoiceBySubscriptionId, setInvoiceBySubscriptionId] = useState<Record<string, InvoiceSummaryResponse>>({});
  const [voucherBanners, setVoucherBanners] = useState<CustomerPortalVoucherBanner[]>([]);
  const [priceRules, setPriceRules] = useState<CustomerPortalPriceRule[]>([]);
  const [ticketTypes, setTicketTypes] = useState<CustomerPortalTicketType[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [keyword, setKeyword] = useState(() => searchParams.get("subscriptionId") ?? "");
  const [selectedSubscriptionId, setSelectedSubscriptionId] = useState(() => searchParams.get("subscriptionId") ?? "");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [form, setForm] = useState<SubscriptionForm>({
    customerVehicleId: "",
    requestedEffectiveFrom: today,
    ticketTypeId: "",
    voucherCode: voucherCodeFromUrl,
  });
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [saving, setSaving] = useState(false);
  const [subscriptionFormError, setSubscriptionFormError] = useState("");
  const [voucherQuote, setVoucherQuote] = useState<CustomerPortalSubscriptionVoucherQuote | null>(null);
  const [voucherQuoteError, setVoucherQuoteError] = useState("");
  const [voucherQuoteLoading, setVoucherQuoteLoading] = useState(false);
  const [registrationModalOpen, setRegistrationModalOpen] = useState(false);
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [paymentChoice, setPaymentChoice] = useState<CustomerPaymentChoice>("VNPAY");
  const [paymentInvoice, setPaymentInvoice] = useState<InvoiceSummaryResponse | null>(null);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError, setPaymentError] = useState("");

  const openRegistrationModal = () => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set("register", "true");
    setSearchParams(nextSearchParams);
  };

  const closeRegistrationModal = () => {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete("register");
    setSearchParams(nextSearchParams, { replace: true });
    setRegistrationModalOpen(false);
  };

  const vehicleById = useMemo(() => new Map(vehicles.map((vehicle) => [vehicle.customerVehicleId, vehicle])), [vehicles]);
  const ticketTypeById = useMemo(() => new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType])), [ticketTypes]);
  const vehicleTypeById = useMemo(() => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])), [vehicleTypes]);

  const activeVehicles = vehicles.filter((vehicle) => vehicle.status === "ACTIVE");
  const selectedVehicle = vehicleById.get(form.customerVehicleId);
  const selectedPriceRule = findMatchingPriceRule(selectedVehicle, form.ticketTypeId, priceRules);
  const activeSubscription = subscriptions.find((subscription) => subscription.status === "ACTIVE");
  const selectedSubscription = subscriptions.find((subscription) => subscription.subscriptionId === selectedSubscriptionId);
  const detailSubscription = selectedSubscription ?? activeSubscription;
  const detailInvoice = detailSubscription ? invoiceBySubscriptionId[detailSubscription.subscriptionId] : undefined;
  // API chỉ trả các voucher đã được bật quảng bá. Một chiến dịch bật ở trang tổng quan
  // cũng cần xuất hiện ở đầu trang vé tháng để khách có thể áp dụng mã ngay tại nơi đăng ký.
  const subscriptionVoucherBanners = voucherBanners;
  const pendingCount = subscriptions.filter((subscription) => subscription.status?.startsWith("PENDING")).length;
  const activePeriod = getSubscriptionPeriod(toDateInputValue(getDisplayEffectiveFrom(activeSubscription)), activeSubscription?.effectiveTo);
  const activeDaysLeft = activePeriod?.remainingDays ?? null;
  const activeTotal = subscriptions
    .filter((subscription) => subscription.status === "ACTIVE")
    .reduce((sum, subscription) => sum + Number(invoiceBySubscriptionId[subscription.subscriptionId]?.finalAmount ?? subscription.price ?? 0), 0);

  const getSubscriptionTotal = (subscription?: CustomerPortalSubscription | null) => (
    subscription ? invoiceBySubscriptionId[subscription.subscriptionId]?.finalAmount ?? subscription.price : null
  );

  const filteredSubscriptions = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return subscriptions.filter((subscription) => {
      const vehicle = vehicleById.get(subscription.customerVehicleId);
      const ticketType = ticketTypeById.get(subscription.ticketTypeId);
      const matchesStatus = statusFilter === "ALL" || subscription.status === statusFilter;
      const matchesKeyword = !normalizedKeyword
        || subscription.subscriptionId.toLowerCase().includes(normalizedKeyword)
        || (vehicle?.licensePlate ?? "").toLowerCase().includes(normalizedKeyword)
        || (ticketType?.name ?? "").toLowerCase().includes(normalizedKeyword);
      return matchesStatus && matchesKeyword;
    });
  }, [keyword, statusFilter, subscriptions, ticketTypeById, vehicleById]);
  const totalPages = Math.max(1, Math.ceil(filteredSubscriptions.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const pagedSubscriptions = filteredSubscriptions.slice((safeCurrentPage - 1) * pageSize, safeCurrentPage * pageSize);

  async function loadData() {
    setLoading(true);
    setLoadError("");
    try {
      const nextProfile = await getCustomerPortalProfile();
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
      setProfile(nextProfile);
      setVehicles(nextVehicles);
      setSubscriptions(nextSubscriptions);
      setInvoiceBySubscriptionId(Object.fromEntries(
        invoiceEntries.filter((entry): entry is readonly [string, InvoiceSummaryResponse] => entry !== null),
      ));
      setVoucherBanners(nextVoucherBanners);
      setPriceRules(lookups.priceRules);
      setTicketTypes(lookups.ticketTypes);
      setVehicleTypes(lookups.vehicleTypes);
      setForm((current) => ({
        ...current,
        customerVehicleId: current.customerVehicleId || nextVehicles.find((vehicle) => vehicle.status === "ACTIVE")?.customerVehicleId || "",
        ticketTypeId: current.ticketTypeId || lookups.ticketTypes[0]?.ticketTypeId || "",
      }));
    } catch (requestError) {
      setLoadError(requestError instanceof Error ? requestError.message : "Không thể tải dữ liệu vé tháng.");
      toast.error(
        requestError instanceof Error ? requestError.message : "Không thể tải dữ liệu vé tháng.",
        "Tải dữ liệu thất bại",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  useEffect(() => {
    if (!voucherCodeFromUrl) return;
    setForm((current) => current.voucherCode === voucherCodeFromUrl
      ? current
      : { ...current, voucherCode: voucherCodeFromUrl });
  }, [voucherCodeFromUrl]);

  useEffect(() => {
    if (searchParams.get("register") === "true") setRegistrationModalOpen(true);
  }, [searchParams]);

  useEffect(() => {
    const vnpayResult = searchParams.get("vnpayResult");
    if (!vnpayResult || handledVnpayReturnRef.current) return;
    handledVnpayReturnRef.current = true;

    if (vnpayResult === "success" && searchParams.get("paymentStatus") === "SUCCESS") {
      toast.success(
        "Hồ sơ đang chờ nhân viên gán thẻ.",
        "Thanh toán VNPay thành công",
      );
    } else if (vnpayResult === "cancelled") {
      toast.warning(
        "Hóa đơn vẫn đang chờ; bạn có thể chọn lại VNPay hoặc thanh toán tại quầy.",
        "Đã hủy giao dịch VNPay",
      );
    } else {
      toast.error(
        "Hóa đơn vẫn được giữ ở trạng thái chờ và có thể thực hiện thanh toán lại.",
        "Giao dịch VNPay chưa thành công",
      );
    }

    const nextParams = new URLSearchParams(searchParams);
    ["vnpayResult", "transactionRef", "responseCode", "paymentStatus"].forEach((key) => nextParams.delete(key));
    setSearchParams(nextParams, { replace: true });
  }, [searchParams, setSearchParams, toast]);

  useEffect(() => {
    setCurrentPage(1);
  }, [keyword, pageSize, statusFilter]);

  const clearVoucherQuote = () => {
    voucherQuoteRequestRef.current += 1;
    setVoucherQuote(null);
    setVoucherQuoteError("");
    setVoucherQuoteLoading(false);
  };

  const handleQuoteVoucher = async () => {
    const voucherCode = form.voucherCode.trim();
    if (!voucherCode) {
      clearVoucherQuote();
      return;
    }

    if (!form.customerVehicleId || !form.ticketTypeId || !form.requestedEffectiveFrom) {
      setVoucherQuote(null);
      setVoucherQuoteError("Chọn phương tiện, loại vé và ngày bắt đầu trước khi áp dụng mã ưu đãi.");
      return;
    }

    setVoucherQuoteLoading(true);
    setVoucherQuoteError("");
    const requestId = voucherQuoteRequestRef.current + 1;
    voucherQuoteRequestRef.current = requestId;
    try {
      const quote = await quoteMySubscriptionVoucher({ ...form, voucherCode });
      if (requestId !== voucherQuoteRequestRef.current) return;
      setVoucherQuote(quote);
    } catch (error) {
      if (requestId !== voucherQuoteRequestRef.current) return;
      const message = error instanceof Error ? error.message : "Không thể kiểm tra mã ưu đãi.";
      setVoucherQuote(null);
      setVoucherQuoteError(
        message === "Dữ liệu gửi lên không hợp lệ."
          ? "Mã chưa áp dụng được cho thông tin đã chọn. Vui lòng kiểm tra điều kiện hoặc thử mã khác."
          : message,
      );
    } finally {
      if (requestId === voucherQuoteRequestRef.current) setVoucherQuoteLoading(false);
    }
  };

  const handleCreate = async () => {
    setSaving(true);
    setSubscriptionFormError("");
    try {
      if (!form.customerVehicleId) {
        throw new Error("Vui lòng chọn xe đăng ký.");
      }
      if (!form.ticketTypeId) {
        throw new Error("Vui lòng chọn loại vé.");
      }
      if (!form.requestedEffectiveFrom) {
        throw new Error("Vui lòng chọn ngày bắt đầu.");
      }
      await createMySubscription(form);
      toast.success(
        "Yêu cầu đang chờ nhân viên phê duyệt.",
        "Đã gửi đăng ký vé",
      );
      if (profile) {
        setSubscriptions(await getMySubscriptions(profile));
      }
      closeRegistrationModal();
    } catch (requestError) {
      const message = requestError instanceof Error
        ? requestError.message
        : "Không thể gửi yêu cầu đăng ký vé.";
      setSubscriptionFormError(message);
      toast.error(
        message,
        message.includes("Ngày bắt đầu") ? "Ngày bắt đầu chưa hợp lệ" : "Đăng ký không thành công",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleOpenPayment = async (subscription: CustomerPortalSubscription) => {
    setPaymentModalOpen(true);
    setPaymentChoice("VNPAY");
    setPaymentInvoice(null);
    setPaymentError("");
    setPaymentLoading(true);

    try {
      const invoice = await getSubscriptionInvoice(subscription.subscriptionId);
      if (!invoice || invoice.status !== "UNPAID") {
        throw new Error("Không tìm thấy hóa đơn đang chờ thanh toán của đăng ký này.");
      }
      setPaymentInvoice(invoice);
    } catch (requestError) {
      setPaymentError(requestError instanceof Error ? requestError.message : "Không thể tải hóa đơn đăng ký.");
    } finally {
      setPaymentLoading(false);
    }
  };

  const handleSubmitPayment = async () => {
    if (!paymentInvoice) return;

    if (paymentChoice === "AT_COUNTER") {
      setPaymentModalOpen(false);
      toast.info(
        `Hóa đơn ${paymentInvoice.invoiceNo} sẽ được nhân viên xác nhận sau khi nhận tiền.`,
        "Đã chọn thanh toán tại quầy",
      );
      return;
    }

    if (Number(paymentInvoice.finalAmount) < VNPAY_MINIMUM_AMOUNT) {
      setPaymentError("VNPay Sandbox chỉ nhận hóa đơn từ 10.000 đồng. Vui lòng chọn thanh toán tại quầy.");
      return;
    }

    setPaymentLoading(true);
    setPaymentError("");
    try {
      const response = await createVnpayInvoicePayment(
        paymentInvoice.invoiceId,
        "/customer/subscriptions",
      );
      window.location.assign(response.data.paymentUrl);
    } catch (requestError) {
      setPaymentError(requestError instanceof Error ? requestError.message : "Không thể tạo giao dịch VNPay.");
      setPaymentLoading(false);
    }
  };

  const currentVehicle = detailSubscription ? vehicleById.get(detailSubscription.customerVehicleId) : undefined;
  const currentTicketType = detailSubscription ? ticketTypeById.get(detailSubscription.ticketTypeId) : undefined;
  const currentEffectiveFrom = getDisplayEffectiveFrom(detailSubscription);
  const currentEffectiveFromValue = toDateInputValue(currentEffectiveFrom);
  const isDetailSubscriptionActive = detailSubscription?.status === "ACTIVE";
  const detailPeriod = getSubscriptionPeriod(currentEffectiveFromValue, detailSubscription?.effectiveTo);
  const detailDaysLeft = isDetailSubscriptionActive ? detailPeriod?.remainingDays : null;
  const detailRemainingPercent = isDetailSubscriptionActive ? detailPeriod?.remainingPercent ?? 0 : 0;

  return (
    <CustomerPortalLayout>
      <div className="min-[1440px]:tw-px-7">
      {subscriptionVoucherBanners.length > 0 ? <div className="tw-mb-5"><VoucherPromotionBanner vouchers={subscriptionVoucherBanners} /></div> : null}
      <header className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-6 max-[640px]:tw-flex-wrap">
        <div>
          <span className="tw-block tw-text-[0.75rem] tw-font-semibold tw-leading-4 tw-tracking-[0.04em] tw-text-[#1263e9]">DỊCH VỤ ĐỊNH KỲ</span>
          <h1 className="tw-m-0 tw-mt-1.5 tw-font-[Cambria,Georgia,serif] tw-text-[clamp(2.25rem,3.4vw,3.25rem)] tw-font-bold tw-leading-[1.1] tw-text-[#0b1c3d]">Vé tháng của tôi</h1>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.94rem] tw-font-medium tw-text-[#61738e]">Quản lý hành trình định kỳ trong một không gian rõ ràng.</p>
        </div>
        <button className="tw-inline-flex tw-h-11 tw-shrink-0 tw-items-center tw-gap-2 tw-rounded-md tw-border-0 tw-bg-[linear-gradient(135deg,#146cf3,#0756d8)] tw-px-5 tw-text-[0.84rem] tw-font-semibold tw-text-white tw-shadow-[0_10px_20px_rgba(20,99,230,0.2)] disabled:tw-opacity-60" type="button" disabled={!profile} onClick={openRegistrationModal}><i className="fas fa-plus" />Đăng ký vé mới</button>
      </header>

      {loadError ? <div role="alert" className="tw-mb-4 tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-700">{loadError}</div> : null}
      <section aria-label="Tổng quan vé tháng" className="tw-grid tw-grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)] tw-gap-4 max-[1100px]:tw-grid-cols-1">
        <article aria-label="Thông tin vé tháng" className="tw-relative tw-min-w-0 tw-text-white [filter:drop-shadow(0_6px_9px_rgba(4,24,64,0.16))]">
          <SubscriptionTicketFrame />
          <div className="tw-relative tw-grid tw-min-h-[296px] tw-grid-cols-[52%_48%]">
            <div className="tw-min-w-0 tw-py-6 tw-pl-7 tw-pr-4">
              <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-normal tw-text-[#f7ce68]"><SubscriptionIcon name="ticket" className="!tw-h-6 !tw-w-6" />{detailSubscription && !isDetailSubscriptionActive ? "Chi tiết vé tháng" : "Vé tháng đang sử dụng"}</span>
              <h2 className="tw-m-0 tw-mt-2 tw-text-[1.18rem] tw-font-semibold tw-leading-snug tw-text-white">{loading ? "Đang tải vé tháng..." : currentTicketType?.name ?? (detailSubscription ? "Vé tháng" : "Chưa có vé tháng")}</h2>
              <div className="tw-mt-3 tw-flex tw-flex-wrap tw-items-center tw-gap-x-2 tw-gap-y-2">
                <strong className="tw-whitespace-nowrap tw-rounded-[4px] tw-border tw-border-solid tw-border-[#b5c6da] tw-px-2.5 tw-py-1 tw-text-[1.1rem] tw-font-semibold tw-leading-tight">{currentVehicle?.licensePlate ?? "--"}</strong>
                <span className="tw-min-w-0 tw-text-[0.76rem] tw-font-normal tw-text-[#e0e7f2]">{[currentVehicle?.brand, currentVehicle?.vehicleTypeId ? vehicleTypeById.get(currentVehicle.vehicleTypeId)?.name : undefined].filter(Boolean).join(" · ") || "Chưa gán phương tiện"}</span>
              </div>
              <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,.8fr)_minmax(0,1.4fr)] tw-gap-2">
                <span className="tw-min-w-0"><small className="tw-block tw-text-[0.72rem] tw-font-normal tw-text-[#b8c6db]">Mã vé</small><span title={detailSubscription?.subscriptionId} className="tw-mt-1 tw-block tw-break-words tw-text-[0.8rem] tw-font-normal">{compactCode(detailSubscription?.subscriptionId)}</span></span>
                <span><small className="tw-block tw-text-[0.72rem] tw-font-normal tw-text-[#b8c6db]">{isDetailSubscriptionActive || detailSubscription?.effectiveFrom ? "Hiệu lực" : "Thời gian dự kiến"}</small><span className="tw-mt-1 tw-block tw-text-[0.77rem] tw-font-normal tw-leading-5">{detailSubscription ? formatDateRange(currentEffectiveFromValue, detailSubscription.effectiveTo) : "--"}</span></span>
              </div>
              <div className="tw-mt-3 tw-border-0 tw-border-t tw-border-solid tw-border-white/25 tw-pt-2.5">
                <small className="tw-block tw-text-[0.72rem] tw-font-normal tw-text-[#b8c6db]">{detailInvoice ? "Đã thanh toán" : "Tổng phí"}</small>
                <strong className="tw-mt-1 tw-block tw-text-[1.5rem] tw-font-semibold tw-leading-tight tw-text-[#f7ce68]">{getSubscriptionTotal(detailSubscription) != null ? formatCurrency(getSubscriptionTotal(detailSubscription)) : "--"}</strong>
                {detailInvoice && detailInvoice.discountAmount > 0 ? <small className="tw-mt-1 tw-block tw-text-[0.68rem] tw-font-normal tw-text-[#b8c6db]">Giá gốc {formatCurrency(detailInvoice.amount)} · Giảm {formatCurrency(detailInvoice.discountAmount)}</small> : null}
              </div>
            </div>
            <div className="tw-relative tw-flex tw-min-w-0 tw-flex-col tw-py-5 tw-pl-4 tw-pr-6">
              <div className="tw-flex tw-min-h-8 tw-justify-end">{detailSubscription ? <SubscriptionStatusPill status={detailSubscription.status} onTicket /> : null}</div>
              <div className="tw-relative tw-z-[1] tw-mt-5 tw-flex tw-items-center tw-justify-center tw-gap-3"><span className="!tw-font-[Cambria,Georgia,serif] tw-text-[4.5rem] tw-font-normal tw-leading-none tw-text-[#f7ce68]">{detailDaysLeft ?? "--"}</span><span className="tw-text-[1.05rem] tw-font-normal tw-leading-6 tw-text-white">ngày<br />còn lại</span></div>
              <div className="tw-mt-auto tw-h-[62px] tw-overflow-hidden"><PortalTicketArtwork className="tw-pointer-events-none tw-h-full tw-w-full tw-text-[#c9b170]/30 [transform:scaleX(1.8)]" /></div>
              <SubscriptionProgress percent={detailRemainingPercent} />
              <div className="tw-mt-1.5 tw-flex tw-items-center tw-justify-between tw-gap-2 tw-text-[0.66rem] tw-font-normal tw-text-[#c8d4e6]"><span>{isDetailSubscriptionActive && detailPeriod ? `${detailPeriod.elapsedDays} ngày đã qua` : "-- ngày đã qua"}</span><span>{detailPeriod ? `${detailPeriod.totalDays} ngày` : "-- ngày"}</span></div>
              <button disabled={!detailSubscription} className="tw-mt-3 tw-inline-flex tw-h-7 tw-w-fit tw-items-center tw-justify-center tw-gap-2 tw-self-end tw-rounded-md tw-border tw-border-solid tw-border-white/35 tw-bg-transparent tw-px-3 tw-text-[0.72rem] tw-font-normal tw-text-white hover:tw-bg-white/10 disabled:tw-opacity-50" type="button" onClick={() => { if (detailSubscription) { setSelectedSubscriptionId(detailSubscription.subscriptionId); setStatusFilter("ALL"); setKeyword(detailSubscription.subscriptionId); } }}>Xem chi tiết <i className="fas fa-chevron-right tw-text-[0.6rem]" /></button>
            </div>
          </div>
        </article>
        <div className="tw-grid tw-grid-cols-2 tw-gap-3">
          <SubscriptionStat icon="ticket" label="Vé đang hoạt động" value={String(subscriptions.filter((item) => item.status === "ACTIVE").length).padStart(2, "0")} />
          <SubscriptionStat icon="calendar" label="Ngày còn lại" value={activeDaysLeft ?? "--"} />
          <SubscriptionStat icon="clock" label="Chờ xử lý" value={String(pendingCount).padStart(2, "0")} />
          <SubscriptionStat icon="wallet" label="Đã thanh toán vé hiệu lực" value={formatCurrency(activeTotal)} monetary />
        </div>
      </section>


      <div className="tw-mt-5">
        <section className="tw-min-w-0 tw-overflow-hidden tw-rounded-[14px] tw-border tw-border-solid tw-border-[#e0e4ec] tw-bg-white tw-p-4 tw-shadow-[0_2px_5px_rgba(18,48,88,0.065)]">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
            <h2 className="tw-m-0 tw-text-[1.05rem] tw-font-bold tw-text-[#12213d]">Lịch sử đăng ký</h2>
            <div className="tw-flex tw-min-w-0 tw-flex-wrap tw-items-center tw-gap-3">
              <select aria-label="Lọc trạng thái vé" className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#dce5f0] tw-bg-white tw-px-3 tw-text-[0.78rem] tw-font-normal tw-text-[#344766]" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="ALL">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="PENDING">Chờ duyệt</option><option value="PENDING_PAYMENT">Chờ thanh toán</option><option value="PENDING_CARD">Chờ gán thẻ</option><option value="EXPIRED">Hết hạn</option><option value="CANCELLED">Đã hủy</option><option value="REJECTED">Từ chối</option></select>
              <label className="tw-relative"><i className="fas fa-search tw-pointer-events-none tw-absolute tw-left-3 tw-top-1/2 [transform:translateY(-50%)] tw-text-[#7a8ba4]" /><input aria-label="Tìm biển số hoặc mã vé" className="tw-h-10 tw-w-[218px] max-[1300px]:tw-w-[180px] max-[640px]:tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#dce5f0] tw-pl-9 tw-pr-3 tw-text-[0.78rem] tw-font-normal tw-text-[#334a6e] placeholder:tw-text-[#91a0b6]" value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm biển số, mã vé..." /></label>
            </div>
          </div>

          <div className="tw-mt-4 tw-grid tw-gap-2">
            {pagedSubscriptions.map((subscription) => {
              const vehicle = vehicleById.get(subscription.customerVehicleId);
              const ticketType = ticketTypeById.get(subscription.ticketTypeId);
              const selected = selectedSubscriptionId === subscription.subscriptionId;
              return (
                <article key={subscription.subscriptionId} aria-label={"Xem chi tiết vé " + compactCode(subscription.subscriptionId)} aria-pressed={selected} className={["tw-grid tw-min-h-[94px] tw-grid-cols-[48px_minmax(0,1.1fr)_minmax(0,1fr)_minmax(0,1.25fr)_112px_8px] max-[1300px]:tw-grid-cols-[38px_minmax(0,1fr)_minmax(0,.85fr)_minmax(0,1.15fr)_105px_8px] max-[640px]:tw-grid-cols-[38px_minmax(0,1fr)_minmax(0,1fr)] tw-items-center tw-gap-x-3 tw-gap-y-2 tw-rounded-xl tw-border tw-border-solid tw-p-3 tw-transition", selected ? "tw-border-[#1263e9] tw-bg-[#f4f8ff]" : "tw-border-[#e0e3e9] tw-bg-white hover:tw-border-[#b9d3fb]"].join(" ")} role="button" tabIndex={0} title={"Xem vé " + subscription.subscriptionId} onClick={() => setSelectedSubscriptionId(subscription.subscriptionId)} onKeyDown={(event) => { if (event.target !== event.currentTarget) return; if (event.key === "Enter" || event.key === " ") { event.preventDefault(); setSelectedSubscriptionId(subscription.subscriptionId); } }}>
                  <span className={["tw-grid tw-h-12 tw-w-12 tw-place-items-center tw-rounded-full max-[1300px]:tw-h-9 max-[1300px]:tw-w-9", subscription.status === "ACTIVE" ? "tw-bg-[#edf7f0] tw-text-[#438c76]" : subscription.status?.startsWith("PENDING") ? "tw-bg-[#fff5e6] tw-text-[#e69a49]" : "tw-bg-[#f3f3f3] tw-text-[#777777]"].join(" ")}><SubscriptionIcon name="ticket" /></span>
                  <span className="tw-min-w-0"><strong title={subscription.subscriptionId} className="tw-block tw-truncate tw-text-[0.82rem] tw-font-semibold tw-text-[#17233e]">{compactCode(subscription.subscriptionId)}</strong><small title={ticketType?.name} className="tw-mt-1 tw-block tw-truncate tw-text-[0.72rem] tw-font-normal tw-text-[#4b5668]">{ticketType?.name ?? "Vé tháng"}</small></span>
                  <span className="tw-min-w-0 tw-border-0 tw-border-l tw-border-solid tw-border-[#edf0f3] tw-pl-3"><strong className="tw-block tw-whitespace-nowrap tw-text-[0.82rem] tw-font-semibold tw-text-[#17233e]">{vehicle?.licensePlate ?? "--"}</strong><small title={vehicleLabel(vehicle, vehicleTypeById)} className="tw-mt-1 tw-block tw-truncate tw-text-[0.72rem] tw-font-normal tw-text-[#4b5668]">{[vehicle?.brand, vehicle?.vehicleTypeId ? vehicleTypeById.get(vehicle.vehicleTypeId)?.name : undefined].filter(Boolean).join(" · ") || "--"}</small></span>
                  <span className="tw-min-w-0 tw-border-0 tw-border-l tw-border-solid tw-border-[#edf0f3] tw-pl-3 max-[640px]:tw-col-start-2">
                    <span className="tw-flex tw-flex-wrap tw-gap-x-1 tw-text-[0.7rem] tw-font-semibold tw-leading-5 tw-text-[#17233e]"><span className="tw-whitespace-nowrap">{formatDate(toDateInputValue(getDisplayEffectiveFrom(subscription)) ?? subscription.requestedEffectiveFrom)}</span><span className="tw-whitespace-nowrap">— {subscription.effectiveTo ? formatDate(subscription.effectiveTo) : "Chưa xác định"}</span></span>
                    <small className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-normal tw-text-[#4b5668]">{getSubscriptionTotal(subscription) != null ? formatCurrency(getSubscriptionTotal(subscription)) : "Chưa xác định phí"}</small>
                  </span>
                  <span className="tw-grid tw-justify-items-end tw-gap-1.5"><SubscriptionStatusPill status={subscription.status} />{subscription.status === "PENDING_PAYMENT" ? <button className="tw-h-7 tw-w-full tw-rounded-[4px] tw-border tw-border-solid tw-border-[#1683ff] tw-bg-[linear-gradient(135deg,#087bff,#0059e4)] tw-px-2 tw-text-[0.74rem] tw-font-normal tw-text-white hover:tw-brightness-110" type="button" onClick={(event) => { event.stopPropagation(); void handleOpenPayment(subscription); }}>Thanh toán</button> : null}</span>
                  <i aria-hidden="true" className="fas fa-chevron-right tw-text-[0.6rem] tw-text-[#5c7090] max-[640px]:tw-hidden" />
                </article>
              );
            })}
            {!loading && filteredSubscriptions.length === 0 ? <p className="tw-my-8 tw-text-center tw-text-[0.8rem] tw-font-semibold tw-text-[#7a8ba4]">Chưa có vé phù hợp với bộ lọc.</p> : null}
            {loading ? <p className="tw-my-8 tw-text-center tw-text-[0.8rem] tw-font-semibold tw-text-[#7a8ba4]">Đang tải dữ liệu...</p> : null}
          </div>

          <PortalPagination currentPage={safeCurrentPage} pageSize={pageSize} totalRecords={filteredSubscriptions.length} onPageChange={setCurrentPage} onPageSizeChange={setPageSize} />
        </section>

        <Modal description="Chọn phương tiện và gói phù hợp với bạn." onClose={closeRegistrationModal} open={registrationModalOpen} title="Đăng ký vé mới">
        <div className="tw-min-w-0 [&_select]:tw-w-full [&_input]:tw-w-full [&_label]:tw-min-w-0">
          {subscriptionFormError ? <div className="tw-mt-3 tw-flex tw-gap-2 tw-rounded-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-[0.76rem] tw-font-medium tw-text-red-700" role="alert"><i className="fas fa-exclamation-circle tw-mt-0.5" /><span>{subscriptionFormError}</span></div> : null}
          <div className="tw-mt-3 tw-grid tw-gap-2.5">
            <label className="tw-grid tw-gap-1 tw-text-[0.72rem] tw-font-normal tw-text-[#263044]">Phương tiện
              <span className="tw-relative"><SubscriptionIcon name="vehicle" className="tw-pointer-events-none tw-absolute tw-left-3 tw-top-2.5 !tw-h-4 !tw-w-4 tw-text-[#677181]" /><select className="tw-h-9 tw-rounded-md tw-border tw-border-solid tw-border-[#dcdfe5] tw-bg-white tw-pl-10 tw-pr-3 tw-text-[0.78rem] tw-font-normal tw-text-[#273345]" value={form.customerVehicleId} onChange={(event) => { setSubscriptionFormError(""); clearVoucherQuote(); setForm((current) => ({ ...current, customerVehicleId: event.target.value })); }}><option value="">Chọn xe</option>{activeVehicles.map((vehicle) => <option key={vehicle.customerVehicleId} value={vehicle.customerVehicleId}>{vehicleLabel(vehicle, vehicleTypeById)}</option>)}</select></span>
            </label>
            <label className="tw-grid tw-gap-1 tw-text-[0.72rem] tw-font-normal tw-text-[#263044]">Loại vé
              <span className="tw-relative"><SubscriptionIcon name="ticket" className="tw-pointer-events-none tw-absolute tw-left-3 tw-top-2.5 !tw-h-4 !tw-w-4 tw-text-[#677181]" /><select className="tw-h-9 tw-rounded-md tw-border tw-border-solid tw-border-[#dcdfe5] tw-bg-white tw-pl-10 tw-pr-3 tw-text-[0.78rem] tw-font-normal tw-text-[#273345]" value={form.ticketTypeId} onChange={(event) => { setSubscriptionFormError(""); clearVoucherQuote(); setForm((current) => ({ ...current, ticketTypeId: event.target.value })); }}><option value="">Chọn loại vé</option>{ticketTypes.map((ticketType) => <option key={ticketType.ticketTypeId} value={ticketType.ticketTypeId}>{ticketType.name}</option>)}</select></span>
            </label>
            <label className="tw-grid tw-gap-1 tw-text-[0.72rem] tw-font-normal tw-text-[#263044]">Ngày bắt đầu
              <DatePicker
                ariaDescribedBy={subscriptionFormError.includes("Ngày bắt đầu") ? "subscription-effective-from-error" : undefined}
                ariaInvalid={subscriptionFormError.includes("Ngày bắt đầu")}
                ariaLabel="Ngày bắt đầu"
                size="compact"
                triggerClassName={subscriptionFormError.includes("Ngày bắt đầu") ? "!tw-border-red-500 !tw-bg-red-50" : "!tw-border-[#dcdfe5]"}
                value={form.requestedEffectiveFrom}
                onChange={(value) => {
                  setSubscriptionFormError("");
                  clearVoucherQuote();
                  setForm((current) => ({ ...current, requestedEffectiveFrom: value }));
                }}
              />
              {subscriptionFormError.includes("Ngày bắt đầu") ? <small className="tw-text-[0.7rem] tw-font-medium tw-text-red-600" id="subscription-effective-from-error">{subscriptionFormError}</small> : null}
            </label>
            <label className="tw-grid tw-gap-1 tw-text-[0.72rem] tw-font-normal tw-text-[#263044]">Mã ưu đãi <span className="tw-font-normal tw-text-[#7b879b]">(nếu có)</span>
              <span className="tw-grid tw-grid-cols-[minmax(0,1fr)_auto] tw-gap-2"><span className="tw-relative"><i className="fas fa-tag tw-pointer-events-none tw-absolute tw-left-3 tw-top-2.5 tw-text-[0.75rem] tw-text-[#677181]" /><input className="tw-h-9 tw-rounded-md tw-border tw-border-solid tw-border-[#dcdfe5] tw-bg-white tw-pl-9 tw-pr-3 tw-text-[0.78rem] tw-font-normal tw-uppercase tw-text-[#273345] placeholder:tw-normal-case placeholder:tw-text-[#93a0b3]" maxLength={50} value={form.voucherCode} onChange={(event) => { setSubscriptionFormError(""); clearVoucherQuote(); setForm((current) => ({ ...current, voucherCode: event.target.value.toUpperCase() })); }} placeholder="Ví dụ: WELCOME10" /></span><button className="tw-h-9 tw-rounded-md tw-border tw-border-solid tw-border-[#1263e9] tw-bg-[#1263e9] tw-px-3 tw-text-[0.74rem] tw-font-medium tw-text-white tw-transition hover:tw-bg-[#0751cf] disabled:tw-cursor-not-allowed disabled:tw-opacity-60" type="button" disabled={voucherQuoteLoading || !form.voucherCode.trim()} onClick={() => { void handleQuoteVoucher(); }}>{voucherQuoteLoading ? "Đang kiểm tra" : "Áp dụng"}</button></span>
              {voucherQuoteLoading ? <small className="tw-text-[0.66rem] tw-font-normal tw-text-[#64748b]"><i className="fas fa-spinner fa-spin tw-mr-1" />Đang kiểm tra mã ưu đãi...</small> : null}
              {!voucherQuoteLoading && voucherQuote ? <small className="tw-text-[0.66rem] tw-font-medium tw-text-[#16824a]"><i className="fas fa-check-circle tw-mr-1" />Mã {voucherQuote.voucherCode} hợp lệ, đã áp dụng ưu đãi.</small> : null}
              {!voucherQuoteLoading && voucherQuoteError ? <small className="tw-text-[0.66rem] tw-font-medium tw-text-red-600"><i className="fas fa-exclamation-circle tw-mr-1" />{voucherQuoteError}</small> : null}
              {!voucherQuoteLoading && !voucherQuote && !voucherQuoteError ? <small className="tw-text-[0.66rem] tw-font-normal tw-text-[#64748b]">Chỉ áp dụng cho vé đăng ký; nhập mã rồi nhấn Áp dụng để kiểm tra ưu đãi.</small> : null}
            </label>
          </div>
          <div className="tw-mt-3 tw-grid tw-gap-1.5 tw-rounded-lg tw-border tw-border-solid tw-border-[#e8e8f0] tw-bg-[#f8f8fc] tw-p-3">
            <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-1"><span className="tw-text-[0.78rem] tw-font-medium tw-text-[#273345]">Tổng cần thanh toán</span><strong className="tw-text-[1.1rem] tw-font-semibold tw-text-[#101d39]">{voucherQuote ? formatCurrency(voucherQuote.finalAmount) : selectedPriceRule?.basePrice != null ? formatCurrency(selectedPriceRule.basePrice) : "Chưa có mức phí"}</strong></div>
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-2 tw-text-[0.68rem] tw-font-normal tw-text-[#4f586a]"><span>Tạm tính · {ticketTypeById.get(form.ticketTypeId)?.name ?? "Chưa chọn loại vé"}</span>{selectedPriceRule?.basePrice != null ? <span className="tw-whitespace-nowrap">{formatCurrency(voucherQuote?.baseAmount ?? selectedPriceRule.basePrice)}</span> : null}</div>
            {voucherQuote ? <div className="tw-flex tw-items-start tw-justify-between tw-gap-2 tw-text-[0.68rem] tw-font-medium tw-text-[#16824a]"><span>Giảm giá {voucherQuote.voucherCode ? `· ${voucherQuote.voucherCode}` : ""}</span><span className="tw-whitespace-nowrap">−{formatCurrency(voucherQuote.discountAmount)}</span></div> : null}
            <div className="tw-flex tw-items-start tw-justify-between tw-gap-2 tw-text-[0.68rem] tw-font-normal tw-text-[#4f586a]"><span>{selectedVehicle ? `Phương tiện: ${selectedVehicle.licensePlate} · ${selectedVehicle.brand || vehicleTypeById.get(selectedVehicle.vehicleTypeId ?? "")?.name || "--"}` : "Chưa chọn phương tiện"}</span>{selectedPriceRule ? <span className="tw-shrink-0 tw-text-[#257640]">Phù hợp</span> : null}</div>
          </div>
          <small className="tw-mt-2 tw-block tw-text-[0.66rem] tw-font-normal tw-text-[#586273]">Mức phí theo gói và phương tiện đã chọn.</small>
          <button className="tw-mt-3 tw-h-11 tw-w-full tw-rounded-md tw-border tw-border-solid tw-border-[#1683ff] tw-bg-[linear-gradient(135deg,#087bff,#0059e4)] tw-text-[0.86rem] tw-font-medium tw-text-white tw-shadow-[0_2px_4px_rgba(20,99,230,0.15)] disabled:tw-opacity-60" type="button" disabled={saving || !profile} onClick={handleCreate}>{saving ? "Đang gửi..." : "Gửi đăng ký"}</button>
        </div>
        </Modal>
      </div>

      <details className="tw-mt-4 tw-rounded-lg tw-border tw-border-solid tw-border-[#e0e8f3] tw-bg-white tw-px-4 tw-py-3 tw-text-[0.76rem] tw-font-normal tw-text-[#52627a]">
        <summary className="tw-cursor-pointer tw-font-medium">Lưu ý về vé tháng và quy trình đăng ký</summary>
        <div className="tw-mt-3 tw-grid tw-gap-2">
          <p className="tw-m-0">Chỉ xe đang hoạt động mới được đăng ký vé. Giá hiển thị theo quy tắc giá đang hoạt động của hệ thống.</p>
          <p className="tw-m-0">Yêu cầu mới sẽ chờ nhân viên duyệt, xác nhận thanh toán và gán thẻ theo quy trình.</p>
          <p className="tw-m-0">Vé bị hủy hoặc từ chối không thể kích hoạt lại trực tiếp từ phía khách hàng.</p>
        </div>
      </details>

      </div>
      <Modal
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <button
              className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-[#dce5f0] tw-bg-white tw-px-4 tw-font-bold tw-text-[#3d506d]"
              type="button"
              disabled={paymentLoading}
              onClick={() => setPaymentModalOpen(false)}
            >
              Đóng
            </button>
            <button
              className="tw-h-10 tw-rounded-md tw-border-0 tw-bg-[#1263e9] tw-px-4 tw-font-bold tw-text-white disabled:tw-bg-[#dce5f0]"
              type="button"
              disabled={paymentLoading || !paymentInvoice}
              onClick={() => void handleSubmitPayment()}
            >
              {paymentLoading
                ? "Đang xử lý..."
                : paymentChoice === "VNPAY"
                  ? "Thanh toán qua VNPay"
                  : "Xác nhận trả tại quầy"}
            </button>
          </div>
        )}
        description="Chọn thanh toán trực tuyến hoặc thanh toán trực tiếp với nhân viên tại quầy."
        onClose={() => setPaymentModalOpen(false)}
        open={paymentModalOpen}
        title="Thanh toán đăng ký vé"
      >
        <div className="tw-grid tw-gap-4">
          {paymentError ? (
            <div className="tw-rounded-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-600">
              {paymentError}
            </div>
          ) : null}
          <div className="tw-rounded-md tw-bg-[#f4f7fb] tw-p-4">
            <span className="tw-block tw-text-xs tw-font-bold tw-text-[#71819a]">Số tiền cần thanh toán</span>
            <strong className="tw-mt-1 tw-block tw-text-xl tw-font-bold tw-text-[#1263e9]">
              {paymentInvoice ? formatCurrency(paymentInvoice.finalAmount) : "Đang tải..."}
            </strong>
            {paymentInvoice ? <span className="tw-mt-1 tw-block tw-text-xs tw-font-semibold tw-text-[#71819a]">{paymentInvoice.invoiceNo}</span> : null}
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <button
              className={`tw-min-h-[92px] tw-rounded-md tw-border tw-border-solid tw-p-3 tw-text-left ${paymentChoice === "VNPAY" ? "tw-border-[#1263e9] tw-bg-[#edf4ff]" : "tw-border-[#dce5f0] tw-bg-white"}`}
              type="button"
              onClick={() => {
                setPaymentChoice("VNPAY");
                setPaymentError("");
              }}
            >
              <i className="fas fa-qrcode tw-mr-2 tw-text-[#1263e9]" />
              <strong>VNPay</strong>
              <span className="tw-mt-2 tw-block tw-text-xs tw-font-semibold tw-text-[#71819a]">Chuyển sang cổng thanh toán VNPay Sandbox.</span>
            </button>
            <button
              className={`tw-min-h-[92px] tw-rounded-md tw-border tw-border-solid tw-p-3 tw-text-left ${paymentChoice === "AT_COUNTER" ? "tw-border-emerald-500 tw-bg-emerald-50" : "tw-border-[#dce5f0] tw-bg-white"}`}
              type="button"
              onClick={() => {
                setPaymentChoice("AT_COUNTER");
                setPaymentError("");
              }}
            >
              <i className="fas fa-money-bill-wave tw-mr-2 tw-text-emerald-600" />
              <strong>Tại quầy</strong>
              <span className="tw-mt-2 tw-block tw-text-xs tw-font-semibold tw-text-[#71819a]">Hồ sơ tiếp tục chờ đến khi nhân viên nhận tiền và xác nhận.</span>
            </button>
          </div>
        </div>
      </Modal>
    </CustomerPortalLayout>
  );
}
