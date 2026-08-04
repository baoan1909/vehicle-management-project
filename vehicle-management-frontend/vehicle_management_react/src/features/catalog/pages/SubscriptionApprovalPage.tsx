import { useCallback, useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, DateRangeInput, Drawer, Modal, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import {
  getSubscriptionInvoice,
  recordCashInvoicePayment,
  type InvoiceSummaryResponse,
} from "@/features/billing/api/invoicePaymentsApi";
import {
  approveSubscription,
  assignSubscriptionCard,
  getSubscriptionLookupData,
  getSubscriptions,
  rejectSubscription,
  type CardResponse,
  type CustomerApiResponse,
  type CustomerVehicleApiResponse,
  type SubscriptionApiResponse,
  type SubscriptionStatus,
  type TicketTypeApiResponse,
  type VehicleTypeApiResponse,
} from "@/features/catalog/api/subscriptionApprovalsApi";
import { cn } from "@/lib/cn";

type ViewMode = "pipeline" | "table";

type SubscriptionView = {
  amount: string;
  cardLabel: string;
  cardNumber: string;
  createdTime: string;
  customerEmail: string;
  customerName: string;
  customerPhone: string;
  customerVehicleId: string;
  date: string;
  id: string;
  invoiceHint: string;
  packageLabel: string;
  packageTone: "primary" | "success";
  plate: string;
  status: SubscriptionStatus;
  statusLabel: string;
  subscriptionId: string;
  vehicleModel: string;
};

type LookupData = {
  cards: CardResponse[];
  customers: CustomerApiResponse[];
  customerVehicles: CustomerVehicleApiResponse[];
  ticketTypes: TicketTypeApiResponse[];
  vehicleTypes: VehicleTypeApiResponse[];
};

const statusOrder: SubscriptionStatus[] = ["PENDING", "PENDING_PAYMENT", "PENDING_CARD", "ACTIVE"];

const statusLabels: Record<SubscriptionStatus, string> = {
  ACTIVE: "Đang hoạt động",
  CANCELLED: "Đã hủy",
  EXPIRED: "Hết hạn",
  PENDING: "Chờ duyệt",
  PENDING_CARD: "Chờ gán thẻ",
  PENDING_PAYMENT: "Chờ thanh toán",
  REJECTED: "Bị từ chối",
};

const statusMeta: Record<SubscriptionStatus, { accent: string; icon: string; ring: string }> = {
  ACTIVE: { accent: "tw-bg-green-50 tw-text-green-600", icon: "far fa-check-circle", ring: "tw-border-t-green-500" },
  CANCELLED: { accent: "tw-bg-slate-100 tw-text-slate-500", icon: "far fa-times-circle", ring: "tw-border-t-slate-400" },
  EXPIRED: { accent: "tw-bg-slate-100 tw-text-slate-500", icon: "far fa-calendar-times", ring: "tw-border-t-slate-400" },
  PENDING: { accent: "tw-bg-blue-50 tw-text-vm-primary", icon: "far fa-calendar-check", ring: "tw-border-t-vm-primary" },
  PENDING_CARD: { accent: "tw-bg-purple-50 tw-text-purple-600", icon: "fas fa-id-card", ring: "tw-border-t-purple-500" },
  PENDING_PAYMENT: { accent: "tw-bg-orange-50 tw-text-orange-500", icon: "far fa-credit-card", ring: "tw-border-t-orange-400" },
  REJECTED: { accent: "tw-bg-red-50 tw-text-red-500", icon: "fas fa-ban", ring: "tw-border-t-red-500" },
};

const statusOptions = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Chờ duyệt", value: "PENDING" },
  { label: "Chờ thanh toán", value: "PENDING_PAYMENT" },
  { label: "Chờ gán thẻ", value: "PENDING_CARD" },
  { label: "Đang hoạt động", value: "ACTIVE" },
  { label: "Bị từ chối", value: "REJECTED" },
  { label: "Đã hủy", value: "CANCELLED" },
  { label: "Hết hạn", value: "EXPIRED" },
];

function formatMoney(value?: number | string | null) {
  const amount = Number(value ?? 0);
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0, style: "currency", currency: "VND" }).format(Number.isFinite(amount) ? amount : 0);
}

function formatDate(value?: string | null) {
  if (!value) return "Chưa có";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("vi-VN").format(date);
}

function formatTime(value?: string | null) {
  if (!value) return "--:--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--:--";
  return new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit" }).format(date);
}

function shortCode(prefix: string, id: string) {
  return `${prefix}-${id.slice(0, 8).toUpperCase()}`;
}

function statusTagClass(status: SubscriptionStatus) {
  if (status === "PENDING_PAYMENT") return "tw-bg-orange-50 tw-text-orange-600";
  if (status === "PENDING_CARD") return "tw-bg-purple-50 tw-text-purple-600";
  if (status === "ACTIVE") return "tw-bg-green-50 tw-text-green-600";
  if (status === "REJECTED") return "tw-bg-red-50 tw-text-red-600";
  if (status === "CANCELLED" || status === "EXPIRED") return "tw-bg-slate-100 tw-text-slate-600";
  return "tw-bg-blue-50 tw-text-vm-primary";
}

function splitDateRange(value: string) {
  const [effectiveFrom = "", effectiveTo = ""] = value.split("|");
  return { effectiveFrom, effectiveTo };
}

function buildLookupMap<T extends Record<K, string>, K extends keyof T>(items: T[], key: K) {
  return new Map(items.map((item) => [item[key], item]));
}

function toSubscriptionView(
  subscription: SubscriptionApiResponse,
  lookup: {
    cardMap: Map<string, CardResponse>;
    customerMap: Map<string, CustomerApiResponse>;
    ticketTypeMap: Map<string, TicketTypeApiResponse>;
    vehicleMap: Map<string, CustomerVehicleApiResponse>;
    vehicleTypeMap: Map<string, VehicleTypeApiResponse>;
  },
): SubscriptionView {
  const customer = lookup.customerMap.get(subscription.customerId);
  const vehicle = lookup.vehicleMap.get(subscription.customerVehicleId);
  const ticketType = lookup.ticketTypeMap.get(subscription.ticketTypeId);
  const vehicleType = vehicle?.vehicleTypeId ? lookup.vehicleTypeMap.get(vehicle.vehicleTypeId) : undefined;
  const card = subscription.cardId ? lookup.cardMap.get(subscription.cardId) : undefined;
  const ticketName = ticketType?.name ?? shortCode("Loại vé", subscription.ticketTypeId);
  const isVip = [ticketType?.code, ticketName].some((value) => value?.toUpperCase().includes("VIP"));

  return {
    amount: formatMoney(subscription.price),
    cardLabel: card?.cardNumber ?? (subscription.cardId ? "Thẻ đã giữ chỗ" : "Chưa giữ thẻ"),
    cardNumber: card?.cardNumber ?? (subscription.cardId ? shortCode("CARD", subscription.cardId) : "Chưa có"),
    createdTime: formatTime(subscription.createdAt),
    customerEmail: customer?.accountEmail ?? "Chưa có email",
    customerName: customer?.userProfile?.fullName ?? customer?.customerCode ?? shortCode("KH", subscription.customerId),
    customerPhone: customer?.userProfile?.phoneNumber ?? "Chưa có SĐT",
    customerVehicleId: subscription.customerVehicleId,
    date: formatDate(subscription.requestedEffectiveFrom ?? subscription.effectiveFrom),
    id: shortCode("SUB", subscription.subscriptionId),
    invoiceHint: subscription.status === "PENDING" ? "Chưa tạo hóa đơn" : "Hóa đơn theo subscription",
    packageLabel: ticketName,
    packageTone: isVip ? "success" : "primary",
    plate: vehicle?.licensePlate ?? shortCode("XE", subscription.customerVehicleId),
    status: subscription.status,
    statusLabel: statusLabels[subscription.status],
    subscriptionId: subscription.subscriptionId,
    vehicleModel: [vehicleType?.name, vehicle?.brand, vehicle?.color].filter(Boolean).join(" - ") || "Chưa có thông tin xe",
  };
}

function SummaryCard({ count, icon, label, accent }: { accent: string; count: number; icon: string; label: string }) {
  return (
    <Card className="tw-flex tw-min-h-[86px] tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-p-4">
      <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1rem]", accent)}>
        <i className={icon} />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.35rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{count}</strong>
      </span>
    </Card>
  );
}

function RequestCard({ active, item, onSelect }: { active: boolean; item: SubscriptionView; onSelect: (item: SubscriptionView) => void }) {
  return (
    <button
      type="button"
      className={cn(
        "tw-grid tw-w-full tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition hover:tw-border-brand-200 hover:tw-shadow-[0_14px_28px_rgba(37,99,235,0.12)]",
        active ? "tw-border-vm-primary tw-shadow-[0_14px_28px_rgba(37,99,235,0.12)]" : "tw-border-vm-slate-100",
      )}
      onClick={() => onSelect(item)}
    >
      <span className="tw-flex tw-items-center tw-justify-between tw-gap-3">
        <strong className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">{item.id}</strong>
        <span className="tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-500">{item.createdTime}</span>
      </span>
      <span className="tw-grid tw-gap-1.5 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700">
        <span><i className="far fa-user tw-mr-2 tw-text-vm-slate-500" />{item.customerName}</span>
        <span><i className="fas fa-car tw-mr-2 tw-text-vm-slate-500" />{item.plate}</span>
        <span><i className="far fa-calendar-alt tw-mr-2 tw-text-vm-slate-500" />{item.date}</span>
      </span>
      <span className="tw-flex tw-items-center tw-justify-between tw-gap-2">
        <Badge tone={item.packageTone} className="tw-rounded-vm-sm tw-px-2.5">{item.packageLabel}</Badge>
        <span className={cn("tw-rounded-vm-sm tw-px-2 tw-py-1 tw-text-[0.66rem] tw-font-black", statusTagClass(item.status))}>{item.statusLabel}</span>
      </span>
      <span className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500"><i className="far fa-id-card tw-mr-2" />{item.cardLabel}</span>
    </button>
  );
}

function PipelineColumn({ activeId, items, status, onSelect }: { activeId?: string; items: SubscriptionView[]; status: SubscriptionStatus; onSelect: (item: SubscriptionView) => void }) {
  const meta = statusMeta[status];

  return (
    <section className={cn("tw-min-w-[245px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-border-t-4 tw-bg-vm-slate-25 tw-p-3", meta.ring)}>
      <header className="tw-mb-3 tw-flex tw-items-center tw-justify-between">
        <h2 className="tw-m-0 tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">{statusLabels[status]}</h2>
        <span className="tw-text-[0.82rem] tw-font-black tw-text-vm-slate-700">{items.length}</span>
      </header>
      <div className="tw-grid tw-gap-2.5">
        {items.length ? items.map((item) => <RequestCard key={item.subscriptionId} active={activeId === item.subscriptionId} item={item} onSelect={onSelect} />) : (
          <div className="tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-white tw-p-4 tw-text-center tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">
            Không có hồ sơ
          </div>
        )}
      </div>
    </section>
  );
}

function SubscriptionTableView({ activeId, items, onSelect }: { activeId?: string; items: SubscriptionView[]; onSelect: (item: SubscriptionView) => void }) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const startOffset = items.length === 0 ? 0 : (currentPage - 1) * pageSize;
  const pagedItems = items.slice(startOffset, startOffset + pageSize);
  const startIndex = items.length === 0 ? 0 : startOffset + 1;
  const endIndex = Math.min(startOffset + pageSize, items.length);

  useEffect(() => {
    setPage(1);
  }, [items.length]);

  return (
    <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3 max-[720px]:tw-flex-col max-[720px]:tw-items-stretch">
        <div>
          <h2 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Danh sách đăng ký vé tháng</h2>
        </div>
        <span className="tw-inline-flex tw-h-8 tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-3 tw-text-[0.78rem] tw-font-black tw-text-vm-primary">
          {items.length} hồ sơ
        </span>
      </div>

      <div className="tw-overflow-x-auto">
        <table className="table tw-m-0 tw-min-w-[1120px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.76rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-uppercase [&_thead_th]:tw-tracking-normal [&_thead_th]:tw-text-vm-slate-500">
          <thead>
            <tr>
              <th>Mã đăng ký</th>
              <th>Khách hàng</th>
              <th>Phương tiện</th>
              <th>Loại vé</th>
              <th>Ngày hiệu lực</th>
              <th>Trạng thái</th>
              <th>Thẻ</th>
              <th>Số tiền</th>
              <th className="tw-text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {pagedItems.map((item) => (
              <tr
                className={cn("tw-cursor-pointer tw-transition hover:[&>td]:tw-bg-brand-50/60", activeId === item.subscriptionId ? "[&>td]:tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "")}
                key={item.subscriptionId}
                onClick={() => onSelect(item)}
              >
                <td>
                  <strong className="tw-block tw-text-[0.86rem] tw-font-black tw-text-vm-primary">{item.id}</strong>
                  <span className="tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-500">{item.createdTime}</span>
                </td>
                <td>
                  <strong className="tw-block tw-text-[0.86rem] tw-font-black tw-text-vm-slate-900">{item.customerName}</strong>
                  <span className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{item.customerPhone}</span>
                </td>
                <td>
                  <strong className="tw-block tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{item.plate}</strong>
                  <span className="tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{item.vehicleModel}</span>
                </td>
                <td>
                  <Badge tone={item.packageTone} className="tw-rounded-vm-sm tw-px-2.5">{item.packageLabel}</Badge>
                </td>
                <td className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700">{item.date}</td>
                <td>
                  <span className={cn("tw-inline-flex tw-rounded-vm-sm tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-black", statusTagClass(item.status))}>
                    {item.statusLabel}
                  </span>
                </td>
                <td>
                  <span className="tw-inline-flex tw-rounded-vm-sm tw-bg-vm-slate-50 tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-black tw-text-vm-slate-600">
                    {item.cardLabel}
                  </span>
                </td>
                <td className="tw-text-[0.84rem] tw-font-black tw-text-vm-slate-900">{item.amount}</td>
                <td className="tw-text-right">
                  <button
                    className="tw-inline-flex tw-h-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-px-3 tw-text-[0.76rem] tw-font-black tw-text-vm-primary tw-transition hover:tw-bg-brand-50"
                    onClick={(event) => {
                      event.stopPropagation();
                      onSelect(item);
                    }}
                    type="button"
                  >
                    Xem
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <PaginationFooter
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={setPage}
        onPageSizeChange={(nextPageSize) => {
          setPageSize(nextPageSize);
          setPage(1);
        }}
        pageSize={pageSize}
        pageSizeOptions={[5, 10, 20]}
        startIndex={startIndex}
        totalPages={totalPages}
        totalRecords={items.length}
      />
    </section>
  );
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="tw-grid tw-grid-cols-[128px_minmax(0,1fr)] tw-gap-4 tw-text-[0.82rem]">
      <span className="tw-font-semibold tw-text-vm-slate-500">{label}</span>
      <strong className="tw-break-words tw-font-black tw-text-vm-slate-900">{value}</strong>
    </div>
  );
}

function ReviewDrawer({
  actionError,
  actionLoading,
  onApprove,
  onAssignCard,
  onClose,
  onOpenCashPayment,
  onReject,
  open,
  request,
}: {
  actionError: string;
  actionLoading: boolean;
  onApprove: (request: SubscriptionView) => void;
  onAssignCard: (request: SubscriptionView) => void;
  onClose: () => void;
  onOpenCashPayment: (request: SubscriptionView) => void;
  onReject: (request: SubscriptionView, reason: string) => void;
  open: boolean;
  request: SubscriptionView | null;
}) {
  const [rejectReason, setRejectReason] = useState("");

  useEffect(() => {
    setRejectReason("");
  }, [request?.subscriptionId]);

  if (!request) return null;

  const canApprove = request.status === "PENDING";
  const canAssignCard = request.status === "PENDING_CARD";
  const waitingPayment = request.status === "PENDING_PAYMENT";

  return (
    <Drawer
      actions={
        <div className="tw-grid tw-gap-3">
          {actionError ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">
              {actionError}
            </div>
          ) : null}
          <div className={cn("tw-grid tw-gap-3", canApprove ? "tw-grid-cols-[1fr_1fr_1.15fr]" : "tw-grid-cols-[1fr_1.15fr]")}>
            <Button variant="secondary" onClick={onClose}>Đóng</Button>
            {canApprove ? <Button disabled={actionLoading} variant="danger" onClick={() => onReject(request, rejectReason)}>Từ chối</Button> : null}
            {canApprove ? <Button disabled={actionLoading} onClick={() => onApprove(request)}>Duyệt yêu cầu</Button> : null}
            {canAssignCard ? <Button disabled={actionLoading} onClick={() => onAssignCard(request)}>Gán thẻ</Button> : null}
            {waitingPayment ? <Button disabled={actionLoading} onClick={() => onOpenCashPayment(request)}>Xác nhận tại quầy</Button> : null}
            {!canApprove && !canAssignCard && !waitingPayment ? <Button disabled>Không có thao tác</Button> : null}
          </div>
        </div>
      }
      description="Duyệt hồ sơ, theo dõi hóa đơn và gán thẻ tháng theo subscription"
      onClose={onClose}
      open={open}
      title="Chi tiết đăng ký"
      width="md"
    >
      <div className="tw-grid tw-gap-5">
        <div className="tw-flex tw-items-center tw-gap-2">
          <Badge tone="primary" className="tw-rounded-vm-sm tw-px-3">{request.id}</Badge>
          <span className={cn("tw-rounded-vm-sm tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-black", statusTagClass(request.status))}>{request.statusLabel}</span>
        </div>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="far fa-user tw-mr-2 tw-text-vm-slate-500" />Thông tin khách hàng</h3>
          <div className="tw-grid tw-grid-cols-[52px_minmax(0,1fr)] tw-gap-3">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-100 tw-text-vm-slate-500"><i className="fas fa-user" /></span>
            <div className="tw-grid tw-gap-1">
              <strong className="tw-text-[0.95rem] tw-font-black tw-text-vm-slate-900">{request.customerName}</strong>
              <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-600"><i className="fas fa-phone tw-mr-2" />{request.customerPhone}</span>
              <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-600"><i className="far fa-envelope tw-mr-2" />{request.customerEmail}</span>
            </div>
          </div>
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="fas fa-car tw-mr-2 tw-text-vm-slate-500" />Thông tin phương tiện</h3>
          <DetailLine label="Biển số" value={request.plate} />
          <DetailLine label="Thông tin xe" value={request.vehicleModel} />
          <DetailLine label="Mã phương tiện" value={shortCode("XE", request.customerVehicleId)} />
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="far fa-calendar-alt tw-mr-2 tw-text-vm-slate-500" />Thông tin đăng ký</h3>
          <DetailLine label="Ngày hiệu lực" value={request.date} />
          <DetailLine label="Loại vé" value={`${request.packageLabel} - ${request.amount}`} />
          <DetailLine label="Hóa đơn" value={request.invoiceHint} />
        </section>

        <section className="tw-grid tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-pb-4">
          <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900"><i className="far fa-id-card tw-mr-2 tw-text-vm-slate-500" />Thẻ tháng</h3>
          <DetailLine label="Thẻ giữ chỗ" value={request.cardNumber} />
          <DetailLine label="Trạng thái" value={request.status === "PENDING" ? "Backend sẽ tự giữ thẻ khi duyệt" : request.statusLabel} />
          {waitingPayment ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-orange-100 tw-bg-orange-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-orange-700">
              Chỉ gán thẻ sau khi hóa đơn của đăng ký này đã được thanh toán.
            </div>
          ) : null}
        </section>

        {canApprove ? (
          <section className="tw-grid tw-gap-2">
            <label className="tw-text-[0.8rem] tw-font-black tw-text-vm-slate-700" htmlFor="subscription-reject-reason">Lý do từ chối</label>
            <textarea
              className="tw-min-h-[92px] tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-800 tw-outline-none focus:tw-border-vm-primary focus:tw-shadow-vm-focus"
              id="subscription-reject-reason"
              placeholder="Nhập lý do trước khi bấm Từ chối"
              value={rejectReason}
              onChange={(event) => setRejectReason(event.target.value)}
            />
          </section>
        ) : null}
      </div>
    </Drawer>
  );
}

export function SubscriptionApprovalPage() {
  const toast = useToast();
  const [subscriptions, setSubscriptions] = useState<SubscriptionApiResponse[]>([]);
  const [lookupData, setLookupData] = useState<LookupData | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [statusFilter, setStatusFilter] = useState("all");
  const [ticketTypeFilter, setTicketTypeFilter] = useState("all");
  const [searchValue, setSearchValue] = useState("");
  const [dateRange, setDateRange] = useState("");
  const [viewMode, setViewMode] = useState<ViewMode>("pipeline");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const [cashPaymentOpen, setCashPaymentOpen] = useState(false);
  const [cashPaymentInvoice, setCashPaymentInvoice] = useState<InvoiceSummaryResponse | null>(null);
  const [cashPaymentNote, setCashPaymentNote] = useState("Nhân viên xác nhận đã thu tiền đăng ký vé tại quầy");
  const [cashPaymentError, setCashPaymentError] = useState("");
  const [cashPaymentLoading, setCashPaymentLoading] = useState(false);

  const loadData = useCallback(async () => {
    const { effectiveFrom, effectiveTo } = splitDateRange(dateRange);
    setLoading(true);
    setError("");

    try {
      const [nextSubscriptions, nextLookupData] = await Promise.all([
        getSubscriptions({
          effectiveFrom,
          effectiveTo,
          status: statusFilter === "all" ? undefined : (statusFilter as SubscriptionStatus),
          ticketTypeId: ticketTypeFilter === "all" ? undefined : ticketTypeFilter,
        }),
        getSubscriptionLookupData(),
      ]);

      setSubscriptions(nextSubscriptions);
      setLookupData(nextLookupData);
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : "Không thể tải dữ liệu đăng ký vé tháng.";
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [dateRange, statusFilter, ticketTypeFilter]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const subscriptionViews = useMemo(() => {
    if (!lookupData) return [];

    const lookup = {
      cardMap: buildLookupMap(lookupData.cards, "cardId"),
      customerMap: buildLookupMap(lookupData.customers, "customerId"),
      ticketTypeMap: buildLookupMap(lookupData.ticketTypes, "ticketTypeId"),
      vehicleMap: buildLookupMap(lookupData.customerVehicles, "customerVehicleId"),
      vehicleTypeMap: buildLookupMap(lookupData.vehicleTypes, "vehicleTypeId"),
    };

    return subscriptions.map((subscription) => toSubscriptionView(subscription, lookup));
  }, [lookupData, subscriptions]);

  const filteredRequests = useMemo(() => {
    const search = searchValue.trim().toLowerCase();
    if (!search) return subscriptionViews;

    return subscriptionViews.filter((request) =>
      [request.id, request.customerName, request.customerPhone, request.plate, request.cardNumber, request.packageLabel]
        .some((value) => value.toLowerCase().includes(search)),
    );
  }, [searchValue, subscriptionViews]);

  const selectedRequest = useMemo(
    () => filteredRequests.find((request) => request.subscriptionId === selectedId) ?? null,
    [filteredRequests, selectedId],
  );

  const ticketTypeOptions = useMemo(() => [
    { label: "Tất cả loại vé", value: "all" },
    ...(lookupData?.ticketTypes ?? []).map((ticketType) => ({
      label: ticketType.name,
      value: ticketType.ticketTypeId,
    })),
  ], [lookupData?.ticketTypes]);

  const counts = useMemo(() => {
    const result = new Map<SubscriptionStatus, number>();
    subscriptionViews.forEach((request) => {
      result.set(request.status, (result.get(request.status) ?? 0) + 1);
    });
    return result;
  }, [subscriptionViews]);

  const refreshAfterAction = useCallback(async (message: string) => {
    toast.success(message);
    await loadData();
  }, [loadData, toast]);

  const handleApprove = useCallback(async (request: SubscriptionView) => {
    setActionLoading(true);
    setActionError("");
    try {
      await approveSubscription(request.subscriptionId);
      await refreshAfterAction("Đã duyệt đăng ký và giữ thẻ tự động.");
    } catch (approveError) {
      setActionError(approveError instanceof Error ? approveError.message : "Không thể duyệt đăng ký.");
    } finally {
      setActionLoading(false);
    }
  }, [refreshAfterAction]);

  const handleReject = useCallback(async (request: SubscriptionView, reason: string) => {
    const normalizedReason = reason.trim();
    if (!normalizedReason) {
      setActionError("Vui lòng nhập lý do từ chối.");
      return;
    }

    setActionLoading(true);
    setActionError("");
    try {
      await rejectSubscription(request.subscriptionId, normalizedReason);
      await refreshAfterAction("Đã từ chối đăng ký vé tháng.");
      setDrawerOpen(false);
    } catch (rejectError) {
      setActionError(rejectError instanceof Error ? rejectError.message : "Không thể từ chối đăng ký.");
    } finally {
      setActionLoading(false);
    }
  }, [refreshAfterAction]);

  const handleAssignCard = useCallback(async (request: SubscriptionView) => {
    setActionLoading(true);
    setActionError("");
    try {
      await assignSubscriptionCard(request.subscriptionId);
      await refreshAfterAction("Đã gán thẻ cho đăng ký vé tháng.");
    } catch (assignError) {
      setActionError(assignError instanceof Error ? assignError.message : "Không thể gán thẻ.");
    } finally {
      setActionLoading(false);
    }
  }, [refreshAfterAction]);

  const handleOpenCashPayment = useCallback(async (request: SubscriptionView) => {
    setCashPaymentOpen(true);
    setCashPaymentInvoice(null);
    setCashPaymentError("");
    setCashPaymentLoading(true);
    try {
      const invoice = await getSubscriptionInvoice(request.subscriptionId);
      if (!invoice || invoice.status !== "UNPAID") {
        throw new Error("Không tìm thấy hóa đơn đang chờ thanh toán của đăng ký này.");
      }
      setCashPaymentInvoice(invoice);
    } catch (paymentError) {
      setCashPaymentError(paymentError instanceof Error ? paymentError.message : "Không thể tải hóa đơn đăng ký.");
    } finally {
      setCashPaymentLoading(false);
    }
  }, []);

  const handleConfirmCashPayment = useCallback(async () => {
    if (!cashPaymentInvoice) return;
    setCashPaymentLoading(true);
    setCashPaymentError("");
    try {
      await recordCashInvoicePayment(
        cashPaymentInvoice.invoiceId,
        Number(cashPaymentInvoice.finalAmount),
        cashPaymentNote,
      );
      setCashPaymentOpen(false);
      setDrawerOpen(false);
      await refreshAfterAction("Đã xác nhận thanh toán tại quầy. Hồ sơ chuyển sang chờ gán thẻ.");
    } catch (paymentError) {
      setCashPaymentError(paymentError instanceof Error ? paymentError.message : "Không thể xác nhận thanh toán tại quầy.");
    } finally {
      setCashPaymentLoading(false);
    }
  }, [cashPaymentInvoice, cashPaymentNote, refreshAfterAction]);

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
            <header className="tw-flex tw-items-start tw-justify-between tw-gap-3 max-[720px]:tw-flex-col">
              <div>
                <h1 className="tw-m-0 tw-text-vm-page-title tw-font-black tw-text-vm-slate-900">Duyệt đăng ký vé tháng & Gán thẻ</h1>
              </div>
              <Button variant="secondary" onClick={() => void loadData()} disabled={loading}>
                <i className="fas fa-sync-alt tw-mr-2" />Làm mới
              </Button>
            </header>

            <div className="tw-grid tw-grid-cols-5 tw-gap-4 max-[1280px]:tw-grid-cols-3 max-[900px]:tw-grid-cols-1">
              {statusOrder.map((status) => <SummaryCard key={status} {...statusMeta[status]} count={counts.get(status) ?? 0} label={statusLabels[status]} />)}
              <SummaryCard {...statusMeta.REJECTED} count={counts.get("REJECTED") ?? 0} label="Bị từ chối" />
            </div>

            <div className="tw-grid tw-grid-cols-[minmax(230px,1fr)_170px_190px_236px_104px_84px] tw-gap-2 max-[1080px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <label className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.82rem] tw-font-semibold tw-outline-none placeholder:tw-text-vm-slate-500" placeholder="Tìm tên, SĐT, biển số, mã đăng ký..." value={searchValue} onChange={(event) => setSearchValue(event.target.value)} />
                <i className="fas fa-search tw-text-vm-slate-500" />
              </label>
              <SelectMenu ariaLabel="Trạng thái" options={statusOptions} value={statusFilter} onChange={setStatusFilter} />
              <SelectMenu ariaLabel="Loại vé" options={ticketTypeOptions} value={ticketTypeFilter} onChange={setTicketTypeFilter} />
              <DateRangeInput value={dateRange} onChange={setDateRange} />
              <Button variant="secondary" className="tw-h-10 tw-whitespace-nowrap tw-px-3" onClick={() => {
                setDateRange("");
                setSearchValue("");
                setStatusFilter("all");
                setTicketTypeFilter("all");
              }}>
                <i className="fas fa-filter-circle-xmark tw-mr-2" />Xóa lọc
              </Button>
              <div className="tw-flex tw-h-10 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
                <button
                  aria-label="Xem pipeline"
                  className={cn(
                    "tw-inline-flex tw-h-full tw-w-10 tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                    viewMode === "pipeline" ? "tw-bg-vm-primary tw-text-white hover:tw-bg-vm-primary hover:tw-text-white" : "",
                  )}
                  onClick={() => setViewMode("pipeline")}
                  type="button"
                >
                  <i className="fas fa-columns" />
                </button>
                <button
                  aria-label="Xem bảng"
                  className={cn(
                    "tw-inline-flex tw-h-full tw-w-10 tw-items-center tw-justify-center tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                    viewMode === "table" ? "tw-bg-vm-primary tw-text-white hover:tw-bg-vm-primary hover:tw-text-white" : "",
                  )}
                  onClick={() => setViewMode("table")}
                  type="button"
                >
                  <i className="fas fa-list" />
                </button>
              </div>
            </div>

            {error ? (
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.84rem] tw-font-bold tw-text-red-600">
                {error}
              </div>
            ) : null}

            {loading ? (
              <div className="tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-8 tw-text-center tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-500">
                Đang tải dữ liệu đăng ký vé tháng...
              </div>
            ) : viewMode === "pipeline" ? (
              <div className="tw-overflow-x-auto tw-pb-2">
                <div className="tw-grid tw-min-w-[1040px] tw-grid-cols-4 tw-gap-4">
                  {statusOrder.map((status) => (
                    <PipelineColumn
                      key={status}
                      status={status}
                      activeId={selectedId ?? undefined}
                      items={filteredRequests.filter((request) => request.status === status)}
                      onSelect={(request) => {
                        setSelectedId(request.subscriptionId);
                        setActionError("");
                        setDrawerOpen(true);
                      }}
                    />
                  ))}
                </div>
              </div>
            ) : (
              <SubscriptionTableView
                activeId={selectedId ?? undefined}
                items={filteredRequests}
                onSelect={(request) => {
                  setSelectedId(request.subscriptionId);
                  setActionError("");
                  setDrawerOpen(true);
                }}
              />
            )}
          </div>
        </div>
      </section>

      <ReviewDrawer
        actionError={actionError}
        actionLoading={actionLoading}
        onApprove={handleApprove}
        onAssignCard={handleAssignCard}
        onClose={() => setDrawerOpen(false)}
        onOpenCashPayment={handleOpenCashPayment}
        onReject={handleReject}
        open={drawerOpen}
        request={selectedRequest}
      />

      <Modal
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <Button variant="secondary" disabled={cashPaymentLoading} onClick={() => setCashPaymentOpen(false)}>
              Đóng
            </Button>
            <Button disabled={cashPaymentLoading || !cashPaymentInvoice} onClick={() => void handleConfirmCashPayment()}>
              {cashPaymentLoading ? "Đang xử lý..." : "Xác nhận đã thu tiền"}
            </Button>
          </div>
        )}
        description="Chỉ xác nhận sau khi nhân viên đã nhận đủ tiền từ khách hàng."
        onClose={() => setCashPaymentOpen(false)}
        open={cashPaymentOpen}
        title="Thanh toán đăng ký vé tại quầy"
      >
        <div className="tw-grid tw-gap-4">
          {cashPaymentError ? (
            <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-[0.82rem] tw-font-bold tw-text-red-600">
              {cashPaymentError}
            </div>
          ) : null}
          <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-4">
            <span className="tw-block tw-text-[0.75rem] tw-font-bold tw-text-vm-slate-500">Số tiền cần thu</span>
            <strong className="tw-mt-1 tw-block tw-text-[1.3rem] tw-font-black tw-text-vm-primary">
              {cashPaymentInvoice ? formatMoney(cashPaymentInvoice.finalAmount) : "Đang tải..."}
            </strong>
            {cashPaymentInvoice ? <span className="tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{cashPaymentInvoice.invoiceNo}</span> : null}
          </div>
          <label>
            <span className="tw-mb-1.5 tw-block tw-text-[0.8rem] tw-font-black tw-text-vm-slate-700">Ghi chú</span>
            <textarea
              className="tw-min-h-[92px] tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3 tw-text-[0.86rem] tw-font-semibold tw-outline-none focus:tw-border-vm-primary"
              value={cashPaymentNote}
              onChange={(event) => setCashPaymentNote(event.target.value)}
            />
          </label>
        </div>
      </Modal>
    </div>
  );
}
