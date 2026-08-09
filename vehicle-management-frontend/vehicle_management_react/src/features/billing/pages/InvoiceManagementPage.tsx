import { useCallback, useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, DateRangeInput, PaginationFooter, SelectMenu, useToast } from "@/components/ui";
import {
  getInvoiceManagementDetail,
  getInvoiceManagementList,
  getInvoiceManagementSummary,
  type InvoiceManagementDetail,
  type InvoiceManagementFilter,
  type InvoiceManagementItem,
  type InvoiceManagementSummary,
  type InvoiceSource,
  type InvoiceStatus,
  type PaymentMethod,
} from "@/features/billing/api/invoiceManagementApi";
import { cn } from "@/lib/cn";

const emptySummary: InvoiceManagementSummary = {
  total: 0,
  unpaid: 0,
  paid: 0,
  cancelled: 0,
  refunded: 0,
};

const statusOptions = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Chờ thanh toán", value: "UNPAID" },
  { label: "Đã thanh toán", value: "PAID" },
  { label: "Đã hủy", value: "CANCELLED" },
  { label: "Đã hoàn tiền", value: "REFUNDED" },
];

const paymentMethodOptions = [
  { label: "Tất cả hình thức", value: "all" },
  { label: "Tiền mặt", value: "CASH" },
  { label: "VNPay", value: "VNPAY" },
  { label: "Chuyển khoản", value: "BANK_TRANSFER" },
  { label: "Mã QR", value: "QR" },
  { label: "MoMo", value: "MOMO" },
];

const statusMeta: Record<InvoiceStatus, { label: string; tone: "danger" | "neutral" | "success" | "warning" }> = {
  CANCELLED: { label: "Đã hủy", tone: "danger" },
  PAID: { label: "Đã thanh toán", tone: "success" },
  REFUNDED: { label: "Đã hoàn tiền", tone: "neutral" },
  UNPAID: { label: "Chờ thanh toán", tone: "warning" },
};

const paymentMethodLabels: Record<PaymentMethod, string> = {
  BANK_TRANSFER: "Chuyển khoản",
  CASH: "Tiền mặt",
  MOMO: "MoMo",
  QR: "Mã QR",
  VNPAY: "VNPay",
};

const sourceLabels: Record<InvoiceSource, string> = {
  LOST_CARD: "Báo mất thẻ",
  MANUAL: "Khoản thu khác",
  PARKING_SESSION: "Phí gửi xe",
  SUBSCRIPTION: "Đăng ký vé",
};

function formatMoney(value?: number | null) {
  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(Number(value ?? 0));
}

function formatDisplayDate(value?: string | null) {
  return value || "--";
}

function toBoundaryInstant(value: string, endOfDay = false) {
  if (!value) return undefined;
  const suffix = endOfDay ? "T23:59:59.999+07:00" : "T00:00:00+07:00";
  const date = new Date(`${value}${suffix}`);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

function splitDateRange(value: string) {
  const [fromDate = "", toDate = ""] = value.split("|");
  return { fromDate, toDate };
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "Không thể tải dữ liệu hóa đơn.";
}

function escapeCsv(value: unknown) {
  const text = String(value ?? "");
  return `"${text.replaceAll('"', '""')}"`;
}

function escapeHtml(value: unknown) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function SummaryCard({ accent, count, icon, label }: { accent: string; count: number; icon: string; label: string }) {
  return (
    <Card className="tw-flex tw-min-h-[84px] tw-items-center tw-gap-3 tw-p-4">
      <span className={cn("tw-inline-flex tw-h-11 tw-w-11 tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1.05rem]", accent)}>
        <i className={icon} aria-hidden="true" />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.45rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{count}</strong>
      </span>
    </Card>
  );
}

function InvoiceStatusBadge({ status }: { status: InvoiceStatus }) {
  const meta = statusMeta[status];
  return <Badge tone={meta.tone}>{meta.label}</Badge>;
}

function InvoiceDetailPanel({ detail, loading, onPrint }: { detail: InvoiceManagementDetail | null; loading: boolean; onPrint: () => void }) {
  if (loading) {
    return (
      <Card className="tw-flex tw-min-h-[520px] tw-items-center tw-justify-center tw-text-vm-slate-500">
        <span className="tw-flex tw-items-center tw-gap-2 tw-font-semibold"><i className="fas fa-spinner fa-spin" />Đang tải chi tiết</span>
      </Card>
    );
  }

  if (!detail) {
    return (
      <Card className="tw-flex tw-min-h-[520px] tw-items-center tw-justify-center tw-p-8 tw-text-center">
        <span>
          <i className="far fa-file-alt tw-text-[2rem] tw-text-vm-slate-300" />
          <strong className="tw-mt-3 tw-block tw-text-[0.95rem] tw-text-vm-slate-700">Chọn một hóa đơn để xem chi tiết</strong>
        </span>
      </Card>
    );
  }

  const invoice = detail.invoice;
  const successfulPayment = detail.payments.find((payment) => payment.status === "SUCCESS");

  return (
    <Card className="tw-min-h-[520px] tw-overflow-hidden">
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
        <div className="tw-min-w-0">
          <h2 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Chi tiết hóa đơn</h2>
          <strong className="tw-mt-1 tw-block tw-truncate tw-text-[0.9rem] tw-text-vm-primary" title={invoice.invoiceNo}>{invoice.invoiceNo}</strong>
        </div>
        <InvoiceStatusBadge status={invoice.status} />
      </div>

      <div className="tw-grid tw-gap-4 tw-p-4">
        <dl className="tw-m-0 tw-grid tw-grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] tw-gap-x-4 tw-gap-y-2 tw-text-[0.8rem]">
          <dt className="tw-font-semibold tw-text-vm-slate-500">Khách hàng</dt>
          <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-vm-slate-900">{invoice.customerName}</dd>
          <dt className="tw-font-semibold tw-text-vm-slate-500">Biển số xe</dt>
          <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-vm-slate-900">{invoice.licensePlate || "Không áp dụng"}</dd>
          <dt className="tw-font-semibold tw-text-vm-slate-500">Nguồn phát sinh</dt>
          <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-vm-slate-900">{sourceLabels[invoice.source]}</dd>
          <dt className="tw-font-semibold tw-text-vm-slate-500">Ngày tạo</dt>
          <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-vm-slate-900">{formatDisplayDate(invoice.createdAt)}</dd>
        </dl>

        <section className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3">
          <h3 className="tw-m-0 tw-mb-2 tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">Chi tiết thanh toán</h3>
          <div className="tw-grid tw-gap-2">
            {detail.lineItems.map((item) => (
              <div className="tw-flex tw-items-start tw-justify-between tw-gap-3 tw-text-[0.78rem]" key={item.code}>
                <span className="tw-text-vm-slate-600">{item.description}</span>
                <strong className="tw-whitespace-nowrap tw-text-vm-slate-900">{formatMoney(item.amount)}</strong>
              </div>
            ))}
            {invoice.discountAmount > 0 ? (
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.78rem]">
                <span className="tw-text-vm-slate-600">Giảm giá</span>
                <strong className="tw-text-green-600">-{formatMoney(invoice.discountAmount)}</strong>
              </div>
            ) : null}
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-2">
              <strong className="tw-text-[0.82rem] tw-text-vm-slate-900">Tổng tiền</strong>
              <strong className="tw-text-[1rem] tw-text-vm-primary">{formatMoney(invoice.finalAmount)}</strong>
            </div>
          </div>
        </section>

        <dl className="tw-m-0 tw-grid tw-grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] tw-gap-x-4 tw-gap-y-2 tw-text-[0.78rem]">
          <dt className="tw-font-semibold tw-text-vm-slate-500">Hình thức thanh toán</dt>
          <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-vm-slate-900">{invoice.paymentMethod ? paymentMethodLabels[invoice.paymentMethod] : "Chưa thanh toán"}</dd>
          <dt className="tw-font-semibold tw-text-vm-slate-500">Mã giao dịch</dt>
          <dd className="tw-m-0 tw-break-all tw-text-right tw-font-bold tw-text-vm-slate-900">{successfulPayment?.transactionRef || invoice.transactionRef || "--"}</dd>
          <dt className="tw-font-semibold tw-text-vm-slate-500">Ngày thanh toán</dt>
          <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-vm-slate-900">{formatDisplayDate(invoice.paidAt)}</dd>
        </dl>

        <section>
          <h3 className="tw-m-0 tw-mb-3 tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">Trạng thái thanh toán</h3>
          <div className="tw-grid tw-gap-3 tw-border-0 tw-border-l-2 tw-border-solid tw-border-vm-slate-100 tw-pl-4 tw-text-[0.78rem]">
            <div className="tw-relative tw-flex tw-justify-between tw-gap-3">
              <i className="fas fa-check tw-absolute tw-left-[-25px] tw-top-0 tw-flex tw-h-4 tw-w-4 tw-items-center tw-justify-center tw-rounded-full tw-bg-green-500 tw-text-[0.48rem] tw-text-white" />
              <strong>Tạo hóa đơn</strong><span className="tw-text-right tw-text-vm-slate-500">{formatDisplayDate(invoice.createdAt)}</span>
            </div>
            {invoice.status === "PAID" ? (
              <div className="tw-relative tw-flex tw-justify-between tw-gap-3">
                <i className="fas fa-check tw-absolute tw-left-[-25px] tw-top-0 tw-flex tw-h-4 tw-w-4 tw-items-center tw-justify-center tw-rounded-full tw-bg-green-500 tw-text-[0.48rem] tw-text-white" />
                <strong>Đã thanh toán</strong><span className="tw-text-right tw-text-vm-slate-500">{formatDisplayDate(invoice.paidAt)}</span>
              </div>
            ) : null}
            {invoice.status === "CANCELLED" ? <strong className="tw-text-red-600">Hóa đơn đã hủy</strong> : null}
            {invoice.status === "REFUNDED" ? <strong className="tw-text-vm-slate-600">Khoản thanh toán đã được hoàn</strong> : null}
          </div>
        </section>

        <Button className="tw-w-full" variant="secondary" onClick={onPrint}>
          <i className="fas fa-print" /> In hóa đơn
        </Button>
      </div>
    </Card>
  );
}

export function InvoiceManagementPage() {
  const toast = useToast();
  const [summary, setSummary] = useState(emptySummary);
  const [items, setItems] = useState<InvoiceManagementItem[]>([]);
  const [detail, setDetail] = useState<InvoiceManagementDetail | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [status, setStatus] = useState("all");
  const [paymentMethod, setPaymentMethod] = useState("all");
  const [dateRange, setDateRange] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalRecords, setTotalRecords] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setKeyword(keywordInput.trim());
      setPage(1);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [keywordInput]);

  const filter = useMemo<InvoiceManagementFilter>(() => {
    const { fromDate, toDate } = splitDateRange(dateRange);

    return {
      fromDate: toBoundaryInstant(fromDate),
      keyword: keyword || undefined,
      page: page - 1,
      paymentMethod: paymentMethod === "all" ? undefined : paymentMethod as PaymentMethod,
      size: pageSize,
      status: status === "all" ? undefined : status as InvoiceStatus,
      toDate: toBoundaryInstant(toDate, true),
    };
  }, [dateRange, keyword, page, pageSize, paymentMethod, status]);

  const loadList = useCallback(async () => {
    setLoading(true);
    try {
      const response = await getInvoiceManagementList(filter);
      setItems(response.data.items);
      setTotalRecords(response.data.totalElements);
      setTotalPages(Math.max(response.data.totalPages, 1));
      setSelectedId((current) => current && response.data.items.some((item) => item.invoiceId === current) ? current : response.data.items[0]?.invoiceId ?? null);
    } catch (error) {
      setItems([]);
      setSelectedId(null);
      toast.error(getErrorMessage(error), "Không thể tải hóa đơn");
    } finally {
      setLoading(false);
    }
  }, [filter, toast]);

  useEffect(() => {
    void loadList();
  }, [loadList, refreshKey]);

  useEffect(() => {
    void getInvoiceManagementSummary()
      .then((response) => setSummary(response.data))
      .catch((error) => toast.error(getErrorMessage(error), "Không thể tải thống kê"));
  }, [refreshKey, toast]);

  useEffect(() => {
    if (!selectedId) {
      setDetail(null);
      return;
    }
    setDetailLoading(true);
    void getInvoiceManagementDetail(selectedId)
      .then((response) => setDetail(response.data))
      .catch((error) => {
        setDetail(null);
        toast.error(getErrorMessage(error), "Không thể tải chi tiết hóa đơn");
      })
      .finally(() => setDetailLoading(false));
  }, [selectedId, toast]);

  const clearFilters = () => {
    setStatus("all");
    setPaymentMethod("all");
    setDateRange("");
    setKeywordInput("");
    setKeyword("");
    setPage(1);
  };

  const exportData = async () => {
    try {
      const response = await getInvoiceManagementList({ ...filter, page: 0, size: 5000 });
      const headers = ["Mã hóa đơn", "Khách hàng", "Biển số", "Nguồn phát sinh", "Ngày tạo", "Tổng tiền", "Hình thức", "Trạng thái"];
      const rows = response.data.items.map((item) => [
        item.invoiceNo,
        item.customerName,
        item.licensePlate ?? "",
        sourceLabels[item.source],
        item.createdAt ?? "",
        item.finalAmount,
        item.paymentMethod ? paymentMethodLabels[item.paymentMethod] : "Chưa thanh toán",
        statusMeta[item.status].label,
      ]);
      const csv = `\uFEFF${[headers, ...rows].map((row) => row.map(escapeCsv).join(",")).join("\r\n")}`;
      const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
      const link = document.createElement("a");
      link.href = url;
      link.download = `hoa-don-${new Date().toISOString().slice(0, 10)}.csv`;
      link.click();
      URL.revokeObjectURL(url);
      toast.success(`Đã xuất ${rows.length} hóa đơn.`, "Xuất dữ liệu thành công");
    } catch (error) {
      toast.error(getErrorMessage(error), "Không thể xuất dữ liệu");
    }
  };

  const printInvoice = () => {
    if (!detail) return;
    const popup = window.open("", "_blank", "width=760,height=820");
    if (!popup) {
      toast.warning("Trình duyệt đang chặn cửa sổ in.", "Không thể mở bản in");
      return;
    }
    const invoice = detail.invoice;
    const rows = detail.lineItems.map((item) => `<tr><td>${escapeHtml(item.description)}</td><td>${escapeHtml(formatMoney(item.amount))}</td></tr>`).join("");
    popup.document.write(`<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${escapeHtml(invoice.invoiceNo)}</title><style>body{font-family:Arial,sans-serif;color:#0f172a;padding:32px}h1{font-size:24px;margin:0 0 8px}.meta{color:#64748b;margin-bottom:24px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:20px 0}.grid div:nth-child(even){text-align:right;font-weight:700}table{border-collapse:collapse;width:100%;margin-top:20px}td{border-bottom:1px solid #e2e8f0;padding:10px 0}td:last-child{text-align:right;font-weight:700}.total{font-size:20px;color:#2563eb;text-align:right;margin-top:20px;font-weight:800}@media print{button{display:none}}</style></head><body><h1>HÓA ĐƠN</h1><div class="meta">${escapeHtml(invoice.invoiceNo)}</div><div class="grid"><div>Khách hàng</div><div>${escapeHtml(invoice.customerName)}</div><div>Biển số</div><div>${escapeHtml(invoice.licensePlate || "Không áp dụng")}</div><div>Ngày tạo</div><div>${escapeHtml(formatDisplayDate(invoice.createdAt))}</div><div>Trạng thái</div><div>${escapeHtml(statusMeta[invoice.status].label)}</div></div><table>${rows}</table><div class="total">Tổng tiền: ${escapeHtml(formatMoney(invoice.finalAmount))}</div><script>window.onload=()=>window.print();<\/script></body></html>`);
    popup.document.close();
  };

  const startIndex = totalRecords === 0 ? 0 : (page - 1) * pageSize + 1;
  const endIndex = Math.min(page * pageSize, totalRecords);

  return (
    <main className="tw-grid tw-gap-4 tw-p-5 max-[700px]:tw-p-3">
      <header className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[700px]:tw-items-start">
        <h1 className="tw-m-0 tw-text-[1.45rem] tw-font-black tw-text-vm-slate-900">Quản lý hóa đơn</h1>
        <div className="tw-flex tw-gap-2">
          <Button variant="secondary" onClick={() => setRefreshKey((value) => value + 1)}><i className="fas fa-sync-alt" /> Làm mới</Button>
          <Button onClick={() => void exportData()}><i className="fas fa-download" /> Xuất dữ liệu</Button>
        </div>
      </header>

      <section className="tw-grid tw-grid-cols-4 tw-gap-3 max-[1050px]:tw-grid-cols-2 max-[600px]:tw-grid-cols-1">
        <SummaryCard accent="tw-bg-blue-50 tw-text-vm-primary" count={summary.total} icon="far fa-file-alt" label="Tổng hóa đơn" />
        <SummaryCard accent="tw-bg-amber-50 tw-text-amber-600" count={summary.unpaid} icon="far fa-clock" label="Chờ thanh toán" />
        <SummaryCard accent="tw-bg-green-50 tw-text-green-600" count={summary.paid} icon="far fa-check-circle" label="Đã thanh toán" />
        <SummaryCard accent="tw-bg-red-50 tw-text-red-500" count={summary.cancelled} icon="far fa-times-circle" label="Đã hủy" />
      </section>

      <Card className="tw-grid tw-grid-cols-[minmax(260px,1.6fr)_minmax(170px,0.7fr)_minmax(170px,0.7fr)_minmax(230px,0.9fr)_auto] tw-items-end tw-gap-3 tw-p-3 max-[1200px]:tw-grid-cols-3 max-[700px]:tw-grid-cols-1">
        <label className="tw-m-0 tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-600">
          Tìm kiếm
          <span className="tw-relative">
            <i className="fas fa-search tw-absolute tw-left-3 tw-top-1/2 tw--translate-y-1/2 tw-text-vm-slate-400" />
            <input className="tw-h-[42px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-pl-9 tw-pr-3 tw-text-[0.86rem] focus:tw-border-brand-200 focus:tw-outline-none" placeholder="Mã hóa đơn, khách hàng, biển số..." value={keywordInput} onChange={(event) => setKeywordInput(event.target.value)} />
          </span>
        </label>
        <label className="tw-m-0 tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-600">Trạng thái<SelectMenu ariaLabel="Lọc trạng thái hóa đơn" options={statusOptions} portal value={status} onChange={(value) => { setStatus(value); setPage(1); }} /></label>
        <label className="tw-m-0 tw-grid tw-gap-1.5 tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-600">Hình thức<SelectMenu ariaLabel="Lọc hình thức thanh toán" options={paymentMethodOptions} portal value={paymentMethod} onChange={(value) => { setPaymentMethod(value); setPage(1); }} /></label>
        <DateRangeInput label="Khoảng ngày" value={dateRange} onChange={(value) => { setDateRange(value); setPage(1); }} />
        <Button variant="secondary" onClick={clearFilters}><i className="fas fa-undo-alt" /> Xóa lọc</Button>
      </Card>

      <section className="tw-grid tw-grid-cols-[minmax(0,1fr)_minmax(330px,0.38fr)] tw-items-start tw-gap-3 max-[1100px]:tw-grid-cols-1">
        <Card className="tw-min-w-0 tw-overflow-hidden">
          <div className="tw-flex tw-items-center tw-justify-between tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3">
            <h2 className="tw-m-0 tw-text-[1rem] tw-font-black tw-text-vm-slate-900">Danh sách hóa đơn</h2>
            {loading ? <i className="fas fa-spinner fa-spin tw-text-vm-primary" aria-label="Đang tải" /> : null}
          </div>
          <div className="tw-overflow-x-auto">
            <table className="tw-w-full tw-min-w-[840px] tw-border-collapse tw-text-left tw-text-[0.78rem]">
              <thead className="tw-bg-vm-slate-25 tw-text-vm-slate-600">
                <tr>{["Mã hóa đơn", "Khách hàng", "Nguồn phát sinh", "Ngày tạo", "Tổng tiền", "Hình thức", "Trạng thái"].map((label) => <th className="tw-px-3 tw-py-3 tw-font-black" key={label}>{label}</th>)}</tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr
                    className={cn("tw-cursor-pointer tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-transition hover:tw-bg-brand-50", selectedId === item.invoiceId ? "tw-bg-brand-50" : "tw-bg-white")}
                    key={item.invoiceId}
                    tabIndex={0}
                    onClick={() => setSelectedId(item.invoiceId)}
                    onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") setSelectedId(item.invoiceId); }}
                  >
                    <td className={cn("tw-max-w-[180px] tw-truncate tw-px-3 tw-py-3 tw-font-black tw-text-vm-primary", selectedId === item.invoiceId ? "tw-border-0 tw-border-l-4 tw-border-solid tw-border-vm-primary" : "") } title={item.invoiceNo}>{item.invoiceNo}</td>
                    <td className="tw-px-3 tw-py-3"><strong className="tw-block tw-text-vm-slate-900">{item.customerName}</strong><span className="tw-mt-0.5 tw-block tw-text-[0.7rem] tw-text-vm-slate-500">{item.licensePlate || "Không có biển số"}</span></td>
                    <td className="tw-px-3 tw-py-3 tw-font-semibold tw-text-vm-slate-700">{sourceLabels[item.source]}</td>
                    <td className="tw-whitespace-nowrap tw-px-3 tw-py-3 tw-text-vm-slate-600">{formatDisplayDate(item.createdAt)}</td>
                    <td className="tw-whitespace-nowrap tw-px-3 tw-py-3 tw-font-black tw-text-vm-slate-900">{formatMoney(item.finalAmount)}</td>
                    <td className="tw-whitespace-nowrap tw-px-3 tw-py-3 tw-font-semibold tw-text-vm-slate-700">{item.paymentMethod ? paymentMethodLabels[item.paymentMethod] : "--"}</td>
                    <td className="tw-whitespace-nowrap tw-px-3 tw-py-3"><InvoiceStatusBadge status={item.status} /></td>
                  </tr>
                ))}
                {!loading && items.length === 0 ? <tr><td className="tw-p-8 tw-text-center tw-font-semibold tw-text-vm-slate-500" colSpan={7}>Không có hóa đơn phù hợp</td></tr> : null}
              </tbody>
            </table>
          </div>
          <PaginationFooter
            currentPage={page}
            endIndex={endIndex}
            onPageChange={setPage}
            onPageSizeChange={(value) => { setPageSize(value); setPage(1); }}
            pageSize={pageSize}
            pageSizeOptions={[5, 10, 20]}
            startIndex={startIndex}
            totalPages={totalPages}
            totalRecords={totalRecords}
          />
        </Card>
        <InvoiceDetailPanel detail={detail} loading={detailLoading} onPrint={printInvoice} />
      </section>
    </main>
  );
}
