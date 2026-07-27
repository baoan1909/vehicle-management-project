import { useEffect, useMemo, useRef, useState } from "react";
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
  getMyCustomerVehicles,
  getMySubscriptions,
  type CustomerPortalPriceRule,
  type CustomerPortalProfile,
  type CustomerPortalSubscription,
  type CustomerPortalTicketType,
  type CustomerPortalVehicle,
  type CustomerPortalVehicleType,
} from "@/features/customer-portal/api/customerPortalApi";

import { CustomerPageHeader, CustomerPortalLayout, Field, PaginationLite, StatCard, StatusPill } from "./PortalShared";
import { Modal, useToast } from "@/components/ui";

type SubscriptionForm = {
  customerVehicleId: string;
  requestedEffectiveFrom: string;
  ticketTypeId: string;
};

type StatusTone = "green" | "blue" | "orange" | "red" | "gray" | "purple";
type CustomerPaymentChoice = "VNPAY" | "AT_COUNTER";

const today = new Date().toISOString().slice(0, 10);

function formatCurrency(value?: number | string | null) {
  const numberValue = Number(value ?? 0);
  return `${new Intl.NumberFormat("vi-VN").format(Number.isFinite(numberValue) ? numberValue : 0)} đ`;
}

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN").format(date);
}

function formatDateRange(from?: string | null, to?: string | null) {
  return `${formatDate(from)} - ${to ? formatDate(to) : "Chưa xác định"}`;
}

function statusTone(status?: string | null): StatusTone {
  if (status === "ACTIVE") return "green";
  if (status === "PENDING" || status === "PENDING_PAYMENT" || status === "PENDING_CARD") return "orange";
  if (status === "EXPIRED") return "gray";
  if (status === "CANCELLED" || status === "REJECTED") return "red";
  return "gray";
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

function daysUntil(value?: string | null) {
  if (!value) return null;
  const end = new Date(value);
  const start = new Date();
  end.setHours(0, 0, 0, 0);
  start.setHours(0, 0, 0, 0);
  return Math.ceil((end.getTime() - start.getTime()) / 86_400_000);
}

function toDateOnly(value?: string | null) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
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

function remainingPercent(subscription?: CustomerPortalSubscription) {
  const start = getDisplayEffectiveFrom(subscription);
  const end = toDateOnly(subscription?.effectiveTo);
  if (!start || !end || end.getTime() <= start.getTime()) return 0;

  const todayValue = toDateOnly(today);
  if (!todayValue) return 0;

  const totalDays = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / 86_400_000));
  const remainingDays = Math.max(0, Math.ceil((end.getTime() - todayValue.getTime()) / 86_400_000));
  return Math.max(0, Math.min(100, Math.round((remainingDays / totalDays) * 100)));
}

function compactCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 18 ? `${value.slice(0, 8)}...${value.slice(-6)}` : value;
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

export function SubscriptionPage() {
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const handledVnpayReturnRef = useRef(false);
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [vehicles, setVehicles] = useState<CustomerPortalVehicle[]>([]);
  const [subscriptions, setSubscriptions] = useState<CustomerPortalSubscription[]>([]);
  const [priceRules, setPriceRules] = useState<CustomerPortalPriceRule[]>([]);
  const [ticketTypes, setTicketTypes] = useState<CustomerPortalTicketType[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<CustomerPortalVehicleType[]>([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [keyword, setKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [form, setForm] = useState<SubscriptionForm>({
    customerVehicleId: "",
    requestedEffectiveFrom: today,
    ticketTypeId: "",
  });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [paymentModalOpen, setPaymentModalOpen] = useState(false);
  const [paymentChoice, setPaymentChoice] = useState<CustomerPaymentChoice>("VNPAY");
  const [paymentInvoice, setPaymentInvoice] = useState<InvoiceSummaryResponse | null>(null);
  const [paymentLoading, setPaymentLoading] = useState(false);
  const [paymentError, setPaymentError] = useState("");

  const vehicleById = useMemo(() => new Map(vehicles.map((vehicle) => [vehicle.customerVehicleId, vehicle])), [vehicles]);
  const ticketTypeById = useMemo(() => new Map(ticketTypes.map((ticketType) => [ticketType.ticketTypeId, ticketType])), [ticketTypes]);
  const priceRuleById = useMemo(() => new Map(priceRules.map((rule) => [rule.priceRuleId, rule])), [priceRules]);
  const vehicleTypeById = useMemo(() => new Map(vehicleTypes.map((type) => [type.vehicleTypeId, type])), [vehicleTypes]);

  const activeVehicles = vehicles.filter((vehicle) => vehicle.status === "ACTIVE");
  const selectedVehicle = vehicleById.get(form.customerVehicleId);
  const selectedPriceRule = findMatchingPriceRule(selectedVehicle, form.ticketTypeId, priceRules);
  const activeSubscription = subscriptions.find((subscription) => subscription.status === "ACTIVE");
  const pendingCount = subscriptions.filter((subscription) => subscription.status?.startsWith("PENDING")).length;
  const activeDaysLeft = daysUntil(activeSubscription?.effectiveTo);
  const activeTotal = subscriptions
    .filter((subscription) => subscription.status === "ACTIVE")
    .reduce((sum, subscription) => sum + Number(subscription.price ?? 0), 0);

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
    try {
      const nextProfile = await getCustomerPortalProfile();
      const [nextVehicles, nextSubscriptions, lookups] = await Promise.all([
        getMyCustomerVehicles(nextProfile),
        getMySubscriptions(nextProfile),
        getCustomerPortalLookups(),
      ]);
      setProfile(nextProfile);
      setVehicles(nextVehicles);
      setSubscriptions(nextSubscriptions);
      setPriceRules(lookups.priceRules);
      setTicketTypes(lookups.ticketTypes);
      setVehicleTypes(lookups.vehicleTypes);
      setForm((current) => ({
        ...current,
        customerVehicleId: current.customerVehicleId || nextVehicles.find((vehicle) => vehicle.status === "ACTIVE")?.customerVehicleId || "",
        ticketTypeId: current.ticketTypeId || lookups.ticketTypes[0]?.ticketTypeId || "",
      }));
    } catch (requestError) {
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

  const handleCreate = async () => {
    setSaving(true);
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
    } catch (requestError) {
      toast.error(
        requestError instanceof Error ? requestError.message : "Không thể gửi yêu cầu đăng ký vé.",
        "Đăng ký không thành công",
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

  const currentVehicle = activeSubscription ? vehicleById.get(activeSubscription.customerVehicleId) : undefined;
  const currentTicketType = activeSubscription ? ticketTypeById.get(activeSubscription.ticketTypeId) : undefined;
  const currentPriceRule = activeSubscription?.priceRuleId ? priceRuleById.get(activeSubscription.priceRuleId) : undefined;
  const currentEffectiveFrom = getDisplayEffectiveFrom(activeSubscription);
  const currentEffectiveFromValue = toDateInputValue(currentEffectiveFrom);
  const activeRemainingPercent = remainingPercent(activeSubscription);

  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Vé tháng"
        subtitle="Đăng ký, gia hạn và theo dõi vé gửi xe của bạn"
        action={<button type="button" onClick={handleCreate} disabled={saving || !profile}><i className="fas fa-plus" /> Đăng ký vé mới</button>}
      />

      <div className="vm-stat-grid vm-stat-grid-four">
        <StatCard icon="far fa-calendar-check" label="Vé đang hoạt động" value={String(subscriptions.filter((item) => item.status === "ACTIVE").length)} note="vé" tone="green" />
        <StatCard icon="far fa-clock" label="Sắp hết hạn" value={activeDaysLeft === null ? "--" : `${Math.max(activeDaysLeft, 0)} ngày`} note={activeSubscription?.effectiveTo ? formatDate(activeSubscription.effectiveTo) : "Chưa có vé"} tone="orange" />
        <StatCard icon="fas fa-hourglass-half" label="Chờ xử lý" value={String(pendingCount)} note="vé" tone="orange" />
        <StatCard icon="fas fa-wallet" label="Phí vé đang hiệu lực" value={formatCurrency(activeTotal)} />
      </div>

      <section className="vm-customer-card vm-subscription-current">
        <div className="vm-section-title-row">
          <h2>Vé tháng hiện tại <StatusPill tone={statusTone(activeSubscription?.status)}>{statusLabel(activeSubscription?.status)}</StatusPill></h2>
          <div className="vm-progress-inline"><span>Thời hạn còn lại</span><b>{activeDaysLeft === null ? "--" : `${Math.max(activeDaysLeft, 0)} ngày`}</b><em><i style={{ width: `${activeRemainingPercent}%` }} /></em><strong>{activeSubscription?.effectiveTo ? formatDate(activeSubscription.effectiveTo) : "--"}</strong></div>
        </div>
        <div className="vm-subscription-detail">
          <dl className="vm-info-list">
            <dt><i className="fas fa-motorcycle" /> Xe đăng ký</dt><dd>{vehicleLabel(currentVehicle, vehicleTypeById)}</dd>
            <dt><i className="far fa-id-badge" /> Loại vé</dt><dd>{currentTicketType?.name ?? "--"}</dd>
            <dt><i className="fas fa-tags" /> Quy tắc giá</dt><dd>{currentPriceRule?.ruleName ?? "--"}</dd>
            <dt><i className="far fa-credit-card" /> Thẻ sử dụng</dt><dd title={activeSubscription?.cardId ?? undefined}>{activeSubscription?.cardId ? compactCode(activeSubscription.cardId) : "Chưa gán thẻ"}</dd>
          </dl>
          <dl className="vm-info-list">
            <dt><i className="far fa-calendar" /> Ngày nhận thẻ</dt><dd>{formatDate(activeSubscription?.cardReceiptDate)}</dd>
            <dt><i className="far fa-clock" /> Hiệu lực từ</dt><dd>{formatDate(currentEffectiveFromValue)}</dd>
            <dt><i className="far fa-calendar-check" /> Hiệu lực đến</dt><dd>{formatDate(activeSubscription?.effectiveTo)}</dd>
            <dt><i className="far fa-money-bill-alt" /> Giá vé</dt><dd>{formatCurrency(activeSubscription?.price)}</dd>
            <dt><i className="far fa-question-circle" /> Ngày duyệt</dt><dd>{activeSubscription?.approvedAt ?? "--"}</dd>
          </dl>
        </div>
        {!activeSubscription ? <div className="vm-info-note"><i className="fas fa-info-circle" /> Chưa có vé tháng đang hoạt động.</div> : null}
      </section>

      <div className="vm-two-column-even vm-subscription-workspace">
        <section className="vm-customer-card vm-table-card vm-subscription-history-card">
          <h2>Lịch sử đăng ký vé</h2>
          <div className="vm-table-filters">
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="PENDING">Chờ duyệt</option>
              <option value="PENDING_PAYMENT">Chờ thanh toán</option>
              <option value="PENDING_CARD">Chờ gán thẻ</option>
              <option value="EXPIRED">Hết hạn</option>
              <option value="CANCELLED">Đã hủy</option>
              <option value="REJECTED">Từ chối</option>
            </select>
            <label><i className="fas fa-search" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="Tìm biển số, mã vé..." /></label>
          </div>
          <table className="vm-customer-table">
            <thead><tr><th>Mã vé</th><th>Biển số</th><th>Loại vé</th><th>Hiệu lực</th><th>Giá</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>
              {pagedSubscriptions.map((subscription) => {
                const vehicle = vehicleById.get(subscription.customerVehicleId);
                const ticketType = ticketTypeById.get(subscription.ticketTypeId);
                return (
                  <tr key={subscription.subscriptionId}>
                    <td title={subscription.subscriptionId}>{compactCode(subscription.subscriptionId)}</td>
                    <td>{vehicle?.licensePlate ?? "--"}</td>
                    <td>{ticketType?.name ?? "--"}</td>
                    <td>{formatDateRange(toDateInputValue(getDisplayEffectiveFrom(subscription)) ?? subscription.requestedEffectiveFrom, subscription.effectiveTo)}</td>
                    <td>{formatCurrency(subscription.price)}</td>
                    <td><StatusPill tone={statusTone(subscription.status)}>{statusLabel(subscription.status)}</StatusPill></td>
                    <td>
                      {subscription.status === "PENDING_PAYMENT" ? (
                        <button
                          className="tw-inline-flex tw-h-9 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-3 tw-text-[0.78rem] tw-font-extrabold tw-text-white"
                          type="button"
                          onClick={() => void handleOpenPayment(subscription)}
                        >
                          <i className="far fa-credit-card" /> Thanh toán
                        </button>
                      ) : <span>--</span>}
                    </td>
                  </tr>
                );
              })}
              {!loading && filteredSubscriptions.length === 0 ? <tr><td colSpan={7}>Chưa có vé phù hợp với bộ lọc.</td></tr> : null}
              {loading ? <tr><td colSpan={7}>Đang tải dữ liệu...</td></tr> : null}
            </tbody>
          </table>
          <PaginationLite
            currentPage={safeCurrentPage}
            pageSize={pageSize}
            totalRecords={filteredSubscriptions.length}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </section>

        <section className="vm-customer-card vm-side-form">
          <h2>Đăng ký / gia hạn nhanh</h2>
          <div className="vm-form-grid">
            <Field label="Xe đăng ký">
              <select value={form.customerVehicleId} onChange={(event) => setForm((current) => ({ ...current, customerVehicleId: event.target.value }))}>
                <option value="">Chọn xe</option>
                {activeVehicles.map((vehicle) => <option key={vehicle.customerVehicleId} value={vehicle.customerVehicleId}>{vehicleLabel(vehicle, vehicleTypeById)}</option>)}
              </select>
            </Field>
            <Field label="Loại vé">
              <select value={form.ticketTypeId} onChange={(event) => setForm((current) => ({ ...current, ticketTypeId: event.target.value }))}>
                <option value="">Chọn loại vé</option>
                {ticketTypes.map((ticketType) => <option key={ticketType.ticketTypeId} value={ticketType.ticketTypeId}>{ticketType.name}</option>)}
              </select>
            </Field>
            <Field label="Ngày bắt đầu"><input type="date" value={form.requestedEffectiveFrom} onChange={(event) => setForm((current) => ({ ...current, requestedEffectiveFrom: event.target.value }))} /></Field>
            <Field label="Tổng phí"><input value={formatCurrency(selectedPriceRule?.basePrice)} readOnly /></Field>
          </div>
          <button type="button" disabled={saving || !profile} onClick={handleCreate}>{saving ? "Đang gửi..." : "Gửi đăng ký"}</button>
          <small><i className="fas fa-info-circle" /> Yêu cầu mới sẽ chờ nhân viên duyệt, xác nhận thanh toán và gán thẻ theo quy trình backend.</small>
        </section>
      </div>

      <section className="vm-customer-card vm-note-list">
        <h2>Lưu ý về vé tháng</h2>
        <ul>
          <li><i className="fas fa-check-circle" /> Chỉ xe đang hoạt động mới được đăng ký vé.</li>
          <li><i className="fas fa-check-circle" /> Vé mới cần được duyệt trước khi kích hoạt.</li>
          <li><i className="fas fa-check-circle" /> Giá hiển thị theo quy tắc giá đang hoạt động của hệ thống.</li>
          <li><i className="fas fa-check-circle" /> Vé bị hủy hoặc từ chối không thể kích hoạt lại trực tiếp từ phía khách hàng.</li>
        </ul>
      </section>

      <Modal
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <button
              className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-font-bold tw-text-vm-slate-700"
              type="button"
              disabled={paymentLoading}
              onClick={() => setPaymentModalOpen(false)}
            >
              Đóng
            </button>
            <button
              className="tw-h-10 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-font-bold tw-text-white disabled:tw-bg-vm-slate-200"
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
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-600">
              {paymentError}
            </div>
          ) : null}
          <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4">
            <span className="tw-block tw-text-xs tw-font-bold tw-text-vm-slate-500">Số tiền cần thanh toán</span>
            <strong className="tw-mt-1 tw-block tw-text-xl tw-font-black tw-text-vm-primary">
              {paymentInvoice ? formatCurrency(paymentInvoice.finalAmount) : "Đang tải..."}
            </strong>
            {paymentInvoice ? <span className="tw-mt-1 tw-block tw-text-xs tw-font-semibold tw-text-vm-slate-500">{paymentInvoice.invoiceNo}</span> : null}
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <button
              className={`tw-min-h-[92px] tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left ${paymentChoice === "VNPAY" ? "tw-border-vm-primary tw-bg-brand-50" : "tw-border-vm-slate-100 tw-bg-white"}`}
              type="button"
              onClick={() => {
                setPaymentChoice("VNPAY");
                setPaymentError("");
              }}
            >
              <i className="fas fa-qrcode tw-mr-2 tw-text-vm-primary" />
              <strong>VNPay</strong>
              <span className="tw-mt-2 tw-block tw-text-xs tw-font-semibold tw-text-vm-slate-500">Chuyển sang cổng thanh toán VNPay Sandbox.</span>
            </button>
            <button
              className={`tw-min-h-[92px] tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left ${paymentChoice === "AT_COUNTER" ? "tw-border-emerald-500 tw-bg-emerald-50" : "tw-border-vm-slate-100 tw-bg-white"}`}
              type="button"
              onClick={() => {
                setPaymentChoice("AT_COUNTER");
                setPaymentError("");
              }}
            >
              <i className="fas fa-money-bill-wave tw-mr-2 tw-text-emerald-600" />
              <strong>Tại quầy</strong>
              <span className="tw-mt-2 tw-block tw-text-xs tw-font-semibold tw-text-vm-slate-500">Hồ sơ tiếp tục chờ đến khi nhân viên nhận tiền và xác nhận.</span>
            </button>
          </div>
        </div>
      </Modal>
    </CustomerPortalLayout>
  );
}
