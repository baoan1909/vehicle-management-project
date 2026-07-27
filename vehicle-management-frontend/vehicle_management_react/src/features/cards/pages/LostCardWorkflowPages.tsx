import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  createVnpayInvoicePayment,
  VNPAY_MINIMUM_AMOUNT,
} from "@/features/billing/api/invoicePaymentsApi";
import {
  createLostCardReport,
  getAvailableReplacementCards,
  getLostCardReportById,
  previewLostCardReport,
  recordLostCardInvoicePayment,
  resolveLostCardReport,
  type LostCardInvoiceDetailResponse,
  type LostCardPaymentMethod,
  type LostCardReportContext,
  type LostCardReportDetailResponse,
  type LostCardReportDetailReportResponse,
  type LostCardPreviewResponse,
  type LostCardReplacementCardResponse,
} from "@/features/cards/api/lostCardReportsApi";
import { Modal } from "@/shared/components/ui/Modal";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

type WorkflowStep = {
  number: number;
  title: string;
  subtitle: string;
  state?: "done" | "active";
};

type InfoBoxProps = {
  label: string;
  value: string;
};

type DetailPaymentState = "unpaid" | "paid" | "resolved";

const paymentMethodOptions: Array<{ label: string; value: LostCardPaymentMethod }> = [
  { label: "Tiền mặt", value: "CASH" },
  { label: "Chuyển khoản", value: "BANK_TRANSFER" },
  { label: "QR", value: "QR" },
  { label: "MoMo", value: "MOMO" },
  { label: "VNPay", value: "VNPAY" },
];

function getPaymentMethodLabel(value: string | null | undefined) {
  return paymentMethodOptions.find((option) => option.value === value)?.label ?? value ?? "-";
}

function toDateTimeLocalValue(date: Date) {
  const offsetMs = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function toOptionalValue(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function formatCurrency(value: number | null | undefined) {
  return new Intl.NumberFormat("vi-VN").format(value ?? 0) + " đ";
}

function getContextLabel(context: LostCardReportContext | null | undefined) {
  switch (context) {
    case "VISITOR_IN_PARKING":
      return "Vãng lai trong bãi";
    case "REGISTERED_IN_PARKING":
      return "Đăng ký trong bãi";
    case "REGISTERED_OUTSIDE":
      return "Đăng ký ngoài bãi";
    default:
      return "Không xác định";
  }
}

function getReportStatusLabel(status: string | null | undefined) {
  switch (status) {
    case "OPEN":
      return "Đang mở";
    case "RESOLVED":
      return "Đã xử lý";
    case "CANCELLED":
      return "Đã hủy";
    default:
      return "Không xác định";
  }
}

function getPaymentStateFromDetail(
  report: LostCardReportDetailReportResponse,
  invoice: LostCardInvoiceDetailResponse | null,
  paymentParam: string | null,
): DetailPaymentState {
  if (report.status === "RESOLVED") return "resolved";
  if (invoice?.status === "PAID") return "paid";
  return paymentParam === "paid" || paymentParam === "resolved" ? paymentParam : "unpaid";
}

function getTotalAmount(report: LostCardReportDetailReportResponse, invoice: LostCardInvoiceDetailResponse | null) {
  return invoice?.finalAmount ?? (Number(report.ticketPrice ?? 0) + Number(report.lostCardFee ?? 0));
}

function getCardLabel(cardId: string | null | undefined) {
  return cardId ? cardId : "-";
}

function getSessionLabel(detail: LostCardReportDetailResponse) {
  const session = detail.parkingSession;
  if (!session) return "Không có phiên gửi xe";
  return `${session.status ?? "Không xác định"} · ${session.checkInTime ?? "-"}`;
}

function requiresReplacementCard(context: LostCardReportContext | null | undefined) {
  return context === "REGISTERED_IN_PARKING" || context === "REGISTERED_OUTSIDE";
}

const createSteps: WorkflowStep[] = [
  { number: 1, title: "Tra cứu biển số", subtitle: "Xác định xe và thẻ", state: "done" },
  { number: 2, title: "Xác nhận thông tin", subtitle: "Người báo + giấy tờ", state: "active" },
  { number: 3, title: "Tạo phiếu", subtitle: "In biên bản cho khách ký" },
  { number: 4, title: "Thanh toán hóa đơn", subtitle: "Nhân viên xác nhận" },
  { number: 5, title: "Hoàn tất xử lý", subtitle: "Mở thanh chắn / cấp thẻ mới" },
];

const historySteps: WorkflowStep[] = [
  { number: 1, title: "Tạo phiếu", subtitle: "Khóa thẻ cũ, phiên gửi xe chuyển sang trạng thái mất thẻ, tạo hóa đơn chờ thanh toán" },
  { number: 2, title: "Xác nhận thanh toán", subtitle: "Giao dịch thành công, hóa đơn đã thanh toán" },
  { number: 3, title: "Chờ cấp thẻ mới", subtitle: "Chọn thẻ đang sẵn sàng và cùng loại với thẻ đã mất để hoàn tất" },
];

function StepList({ steps }: { steps: WorkflowStep[] }) {
  return (
    <div className="tw-grid tw-gap-4">
      {steps.map((step) => (
        <div
          className={`tw-grid tw-grid-cols-[38px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-rounded-vm-lg tw-px-3 tw-py-2.5 ${
            step.state === "active" ? "tw-bg-brand-50" : ""
          }`}
          key={step.number}
        >
          <span
            className={`tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-xl tw-text-[0.95rem] tw-font-extrabold ${
              step.state === "done"
                ? "tw-bg-green-100 tw-text-green-600"
                : step.state === "active"
                  ? "tw-bg-vm-primary tw-text-white"
                  : "tw-bg-slate-100 tw-text-vm-slate-700"
            }`}
          >
            {step.number}
          </span>
          <span className="tw-min-w-0">
            <b className={`tw-block tw-text-[0.92rem] tw-leading-tight ${step.state === "done" ? "tw-text-green-600" : step.state === "active" ? "tw-text-vm-primary" : "tw-text-vm-slate-700"}`}>
              {step.title}
            </b>
            <small className="tw-mt-1 tw-block tw-text-[0.76rem] tw-font-semibold tw-leading-tight tw-text-vm-slate-500">{step.subtitle}</small>
          </span>
        </div>
      ))}
    </div>
  );
}

function InfoBox({ label, value }: InfoBoxProps) {
  return (
    <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3">
      <span className="tw-block tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
      <b className="tw-mt-1 tw-block tw-text-[0.94rem] tw-text-slate-900">{value}</b>
    </div>
  );
}

function LostCardEvidenceImage({
  alt,
  emptyText,
  imagePath,
  label,
}: {
  alt: string;
  emptyText: string;
  imagePath?: string | null;
  label: string;
}) {
  const [hasLoadError, setHasLoadError] = useState(false);
  const imageUrl = resolvePublicMediaUrl(imagePath);

  useEffect(() => {
    setHasLoadError(false);
  }, [imagePath]);

  return (
    <div className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
      <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2">
        <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">{label}</span>
      </div>
      {imageUrl && !hasLoadError ? (
        <div className="tw-aspect-[16/9] tw-w-full tw-bg-vm-slate-25">
          <img
            alt={alt}
            className="tw-h-full tw-w-full tw-object-cover"
            src={imageUrl}
            onError={() => setHasLoadError(true)}
          />
        </div>
      ) : (
        <div className="tw-flex tw-aspect-[16/9] tw-w-full tw-flex-col tw-items-center tw-justify-center tw-gap-2 tw-bg-vm-slate-25 tw-p-4 tw-text-center tw-text-vm-slate-500">
          <i className="far fa-image tw-text-2xl" />
          <span className="tw-text-[0.86rem] tw-font-bold tw-leading-snug">
            {hasLoadError ? "Không tải được ảnh. Hãy tra cứu lại để tạo URL ảnh mới." : emptyText}
          </span>
        </div>
      )}
    </div>
  );
}

function Field({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <label className={wide ? "tw-col-span-2 max-md:tw-col-span-1" : ""}>
      <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      {wide ? (
        <textarea
          className="tw-min-h-[86px] tw-w-full tw-resize-none tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-3 tw-text-[0.92rem] tw-font-semibold tw-text-vm-slate-700 tw-outline-none"
          defaultValue={value}
        />
      ) : (
        <input
          className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none"
          defaultValue={value}
        />
      )}
    </label>
  );
}

function CostPanel({
  lostCardFee = 120000,
  paid,
  ticketPrice = 0,
  totalAmount = ticketPrice + lostCardFee,
}: {
  lostCardFee?: number;
  paid?: boolean;
  ticketPrice?: number;
  totalAmount?: number;
}) {
  return (
    <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
        <div>
          <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">{paid ? "Hóa đơn & thanh toán" : "Chi phí & kết quả"}</h2>
          {paid ? <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Chỉ được hoàn tất khi hóa đơn đã thanh toán</p> : null}
        </div>
        {paid ? (
          <span className="tw-inline-flex tw-min-h-8 tw-items-center tw-rounded-full tw-bg-green-100 tw-px-4 tw-text-[0.78rem] tw-font-extrabold tw-text-green-600">Đã thanh toán</span>
        ) : null}
      </div>
      <div className="tw-grid tw-gap-3 tw-p-4">
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.95rem] tw-font-extrabold">
          <span className="tw-text-vm-slate-500">Tiền vé</span>
          <b className="tw-text-slate-900">{formatCurrency(ticketPrice)}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.95rem] tw-font-extrabold">
          <span className="tw-text-vm-slate-500">Phí mất thẻ</span>
          <b className="tw-text-slate-900">{formatCurrency(lostCardFee)}</b>
        </div>
        <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <span className="tw-text-[1.05rem] tw-font-extrabold tw-text-vm-slate-500">Tổng thành toán</span>
            <strong className="tw-text-[1.55rem] tw-font-extrabold tw-text-red-500">{formatCurrency(totalAmount)}</strong>
          </div>
        </div>
        <div className={`${paid ? "tw-border-green-200 tw-bg-green-50 tw-text-green-700" : "tw-border-orange-200 tw-bg-orange-50 tw-text-orange-700"} tw-rounded-vm-lg tw-border tw-border-solid tw-p-3 tw-text-[0.82rem] tw-font-bold tw-leading-relaxed`}>
          {paid
            ? "Đã thanh toán. Có thể hoàn tất phiếu sau khi chọn thẻ mới cho khách đăng ký."
            : "Sau khi tạo phiếu, hệ thống khóa thẻ cũ và tạo hóa đơn chờ thanh toán. Nhân viên in phiếu để khách ký, sau đó xác nhận thanh toán ở màn hóa đơn."}
        </div>
      </div>
    </section>
  );
}

function InvoicePaymentPanel({
  canConfirmPayment,
  invoice,
  isPaymentSubmitting,
  lostCardFee,
  onStartPayment,
  onShowInvoice,
  state,
  ticketPrice,
  totalAmount,
}: {
  canConfirmPayment: boolean;
  invoice: LostCardInvoiceDetailResponse | null;
  isPaymentSubmitting: boolean;
  lostCardFee: number;
  onStartPayment: (paymentMethod: LostCardPaymentMethod) => void;
  onShowInvoice: () => void;
  state: DetailPaymentState;
  ticketPrice: number;
  totalAmount: number;
}) {
  const isPaid = state === "paid" || state === "resolved";

  return (
    <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
        <div>
          <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">Hóa đơn & thanh toán</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Chỉ được hoàn tất khi hóa đơn đã thanh toán</p>
        </div>
        <span
          className={`tw-inline-flex tw-min-h-8 tw-items-center tw-rounded-full tw-px-4 tw-text-[0.78rem] tw-font-extrabold ${
            isPaid ? "tw-bg-green-100 tw-text-green-600" : "tw-bg-orange-100 tw-text-orange-600"
          }`}
        >
          {isPaid ? "Đã thanh toán" : "Chờ thanh toán"}
        </span>
      </div>
      <div className="tw-grid tw-gap-3 tw-p-4">
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.95rem] tw-font-extrabold">
          <span className="tw-text-vm-slate-500">Tiền vé</span>
          <b className="tw-text-slate-900">{formatCurrency(ticketPrice)}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.95rem] tw-font-extrabold">
          <span className="tw-text-vm-slate-500">Phí mất thẻ</span>
          <b className="tw-text-slate-900">{formatCurrency(lostCardFee)}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-text-[0.9rem] tw-font-bold">
          <span className="tw-text-vm-slate-500">Hóa đơn</span>
          {invoice ? (
            <button
              className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-right tw-font-extrabold tw-text-vm-primary hover:tw-text-brand-700"
              type="button"
              onClick={onShowInvoice}
            >
              {invoice.invoiceNo}
            </button>
          ) : (
            <b className="tw-text-slate-900">Chưa có</b>
          )}
        </div>
        <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <span className="tw-text-[1.05rem] tw-font-extrabold tw-text-vm-slate-500">Tổng tiền</span>
            <strong className="tw-text-[1.55rem] tw-font-extrabold tw-text-red-500">{formatCurrency(totalAmount)}</strong>
          </div>
        </div>
        <div
          className={`tw-rounded-vm-lg tw-border tw-border-solid tw-p-3 tw-text-[0.82rem] tw-font-bold tw-leading-relaxed ${
            isPaid ? "tw-border-green-200 tw-bg-green-50 tw-text-green-700" : "tw-border-orange-200 tw-bg-orange-50 tw-text-orange-700"
          }`}
        >
          {isPaid
            ? "Đã thanh toán. Có thể hoàn tất phiếu sau khi chọn thẻ mới cho khách đăng ký."
            : "Phiếu đã được tạo và hóa đơn đang chờ thanh toán. Nhân viên cần xác nhận khách đã thanh toán trước khi hoàn tất xử lý."}
        </div>
        {!isPaid ? (
          <div className="tw-grid tw-grid-cols-2 tw-gap-2 max-sm:tw-grid-cols-1">
            <button
              className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-border-green-600 tw-bg-white tw-px-3 tw-text-[0.88rem] tw-font-extrabold tw-text-green-700 hover:tw-bg-green-50 disabled:tw-border-slate-200 disabled:tw-bg-slate-100 disabled:tw-text-vm-slate-500"
              disabled={!invoice || !canConfirmPayment || isPaymentSubmitting}
              type="button"
              onClick={() => onStartPayment("CASH")}
            >
              <i className="fas fa-money-bill-wave" />
              <span>Thu tiền mặt</span>
            </button>
            <button
              className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-lg tw-bg-vm-primary tw-px-3 tw-text-[0.88rem] tw-font-extrabold tw-text-white tw-shadow-[0_12px_22px_rgba(37,99,235,0.18)] hover:tw-bg-brand-700 disabled:tw-bg-slate-200 disabled:tw-text-vm-slate-500"
              disabled={!invoice || !canConfirmPayment || isPaymentSubmitting || totalAmount < VNPAY_MINIMUM_AMOUNT}
              title={totalAmount < VNPAY_MINIMUM_AMOUNT ? "VNPay áp dụng cho hóa đơn từ 10.000 đồng" : "Thanh toán qua VNPay"}
              type="button"
              onClick={() => onStartPayment("VNPAY")}
            >
              <i className="fas fa-qrcode" />
              <span>Thanh toán VNPay</span>
            </button>
          </div>
        ) : null}
      </div>
    </section>
  );
}

function getDetailHistorySteps(
  state: DetailPaymentState,
  context: LostCardReportContext | null | undefined,
): WorkflowStep[] {
  const needsNewCard = requiresReplacementCard(context);

  if (state === "unpaid") {
    return [
      historySteps[0],
      { number: 2, title: "Chờ thanh toán", subtitle: "Invoice đang UNPAID, chưa được phép hoàn tất xử lý", state: "active" },
      {
        number: 3,
        title: needsNewCard ? "Cấp lại thẻ và hoàn tất" : "Hoàn tất xử lý",
        subtitle: needsNewCard
          ? "Sau khi thanh toán, chọn thẻ mới để gán lại cho vé đăng ký"
          : "Sau khi thanh toán, đóng phiên gửi xe và hoàn tất phiếu",
      },
    ];
  }

  if (state === "resolved") {
    const resolvedSubtitle = context === "REGISTERED_IN_PARKING"
      ? "Đã gán thẻ mới vào vé đăng ký, đóng phiên gửi xe và mở thanh chắn"
      : context === "REGISTERED_OUTSIDE"
        ? "Đã gán thẻ mới vào vé đăng ký và hoàn tất phiếu"
        : "Đã đóng phiên gửi xe và mở thanh chắn";

    return [
      historySteps[0],
      historySteps[1],
      {
        number: 3,
        title: needsNewCard ? "Cấp thẻ mới và hoàn tất" : "Hoàn tất xử lý",
        subtitle: resolvedSubtitle,
        state: "done",
      },
    ];
  }

  return [
    historySteps[0],
    historySteps[1],
    needsNewCard
      ? historySteps[2]
      : { number: 3, title: "Chờ hoàn tất xử lý", subtitle: "Đóng phiên gửi xe và mở thanh chắn" },
  ];
}

function LostCardResolvePanel({
  availableCards,
  context,
  contextLabel,
  isCardsLoading,
  isCompleting,
  onComplete,
  onNewCardChange,
  reportStatus,
  selectedNewCardId,
  state,
}: {
  availableCards: LostCardReplacementCardResponse[];
  context: LostCardReportContext | null | undefined;
  contextLabel: string;
  isCardsLoading: boolean;
  isCompleting: boolean;
  onComplete: () => void;
  onNewCardChange: (value: string) => void;
  reportStatus: LostCardReportDetailReportResponse["status"];
  selectedNewCardId: string;
  state: DetailPaymentState;
}) {
  const isPaid = state === "paid" || state === "resolved";
  const isResolved = state === "resolved";
  const isCancelled = reportStatus === "CANCELLED";
  const needsNewCard = requiresReplacementCard(context);
  const canComplete = isPaid && !isResolved && !isCancelled && (!needsNewCard || Boolean(selectedNewCardId));
  const panelTitle = isCancelled
    ? "Phiếu đã hủy"
    : needsNewCard
      ? "Cấp lại thẻ cho khách đăng ký"
      : isPaid
        ? "Hoàn tất xử lý"
        : "Chờ thanh toán";

  return (
    <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
      <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
        <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">{panelTitle}</h2>
      </div>
      <div className="tw-grid tw-gap-3 tw-p-4">
        <div className="tw-flex tw-items-center tw-justify-between tw-text-[0.9rem] tw-font-bold">
          <span className="tw-text-vm-slate-500">Ngữ cảnh</span>
          <b className="tw-text-slate-900">{contextLabel}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-text-[0.9rem] tw-font-bold">
          <span className="tw-text-vm-slate-500">Yêu cầu</span>
          <b className="tw-text-slate-900">
            {isCancelled
              ? "Không thể tiếp tục"
              : isPaid
                ? needsNewCard
                  ? "Chọn và cấp thẻ mới"
                  : "Hoàn tất phiếu"
                : "Thanh toán hóa đơn"}
          </b>
        </div>
        {needsNewCard ? (
          <label>
            <span className="tw-mb-1.5 tw-block tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-600">
              Thẻ mới
            </span>
            <select
              className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900 disabled:tw-bg-slate-100 disabled:tw-text-vm-slate-500"
              disabled={!isPaid || isResolved || isCancelled || isCardsLoading}
              value={selectedNewCardId}
              onChange={(event) => onNewCardChange(event.target.value)}
            >
              <option value="">
                {isCardsLoading
                  ? "Đang tải thẻ khả dụng..."
                  : isPaid
                    ? "Chọn thẻ mới cùng loại với thẻ đã mất"
                    : "Thanh toán hóa đơn trước khi cấp thẻ mới"}
              </option>
              {availableCards.map((card) => (
                <option key={card.cardId} value={card.cardId}>
                  {card.cardNumber} {card.uid ? `· UID ${card.uid}` : ""}
                </option>
              ))}
            </select>
          </label>
        ) : null}
        {needsNewCard && isPaid && !isCardsLoading && availableCards.length === 0 && !isResolved ? (
          <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3 tw-text-[0.82rem] tw-font-bold tw-leading-relaxed tw-text-amber-700">
            Không có thẻ cùng loại ở trạng thái sẵn sàng. Hãy bổ sung hoặc kích hoạt thẻ trước khi cấp lại.
          </div>
        ) : null}
        <div
          className={`tw-rounded-vm-lg tw-border tw-border-solid tw-p-3 tw-text-[0.82rem] tw-font-bold tw-leading-relaxed ${
            isCancelled
              ? "tw-border-slate-200 tw-bg-slate-50 tw-text-slate-600"
              : isPaid
                ? "tw-border-green-200 tw-bg-green-50 tw-text-green-700"
                : "tw-border-orange-200 tw-bg-orange-50 tw-text-orange-700"
          }`}
        >
          {isCancelled
            ? "Phiếu đã hủy nên không thể thanh toán hoặc cấp thẻ thay thế."
            : isPaid
            ? needsNewCard
              ? "Chọn thẻ mới rồi xác nhận cấp thẻ. Hệ thống sẽ gán thẻ vào vé đăng ký và hoàn tất phiếu."
              : "Sau khi hoàn tất: phiên mất thẻ được đóng và thanh chắn có thể mở theo kết quả backend."
            : "Hóa đơn chưa thanh toán nên chưa thể hoàn tất phiếu. Hãy xác nhận thanh toán trước."}
        </div>
        {isCancelled ? (
          <button className="tw-min-h-11 tw-rounded-vm-lg tw-bg-slate-100 tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-600" disabled type="button">
            Phiếu đã hủy
          </button>
        ) : !isPaid ? (
          <button className="tw-min-h-11 tw-rounded-vm-lg tw-bg-slate-100 tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-600" disabled type="button">
            Chờ thanh toán hóa đơn
          </button>
        ) : isResolved ? (
          <button className="tw-min-h-11 tw-rounded-vm-lg tw-bg-green-100 tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-green-700" disabled type="button">
            Đã hoàn tất
          </button>
        ) : (
          <button
            className="tw-min-h-11 tw-rounded-vm-lg tw-bg-green-600 tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white disabled:tw-bg-green-100 disabled:tw-text-green-700"
            disabled={!canComplete || isCompleting}
            type="button"
            onClick={onComplete}
          >
            {isCompleting
              ? "Đang cấp thẻ..."
              : needsNewCard
                ? selectedNewCardId
                  ? "Cấp thẻ mới và hoàn tất"
                  : "Chọn thẻ mới để tiếp tục"
                : "Hoàn tất phiếu"}
          </button>
        )}
        <button className="tw-min-h-11 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-700" type="button">
          In lại biên bản
        </button>
      </div>
    </section>
  );
}

function LostCardCancelPanel({ hidden }: { hidden: boolean }) {
  return (
    <section className={`${hidden ? "tw-hidden" : ""} tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-4 tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]`}>
      <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">Hủy phiếu</h2>
      <p className="tw-mb-3 tw-mt-2 tw-text-[0.82rem] tw-font-semibold tw-leading-relaxed tw-text-vm-slate-500">Chỉ hủy khi phiếu đang mở và hóa đơn chưa thanh toán.</p>
      <button className="tw-min-h-10 tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-200 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-extrabold tw-text-red-500" type="button">
        Hủy phiếu
      </button>
    </section>
  );
}

function InvoiceDetailPanel({
  invoice,
  isOpen,
  onClose,
}: {
  invoice: LostCardInvoiceDetailResponse | null;
  isOpen: boolean;
  onClose: () => void;
}) {
  if (!isOpen || !invoice) return null;

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end">
          <button
            className="tw-min-h-10 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-700"
            type="button"
            onClick={onClose}
          >
            Đóng
          </button>
        </div>
      }
      onClose={onClose}
      open={isOpen}
      title="Chi tiết hóa đơn"
      width="lg"
    >
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
        <div>
          <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">Chi tiết hóa đơn</h2>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">{invoice.invoiceNo}</p>
        </div>
      </div>
      <div className="tw-mt-4 tw-grid tw-gap-3 tw-text-[0.9rem] tw-font-bold">
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <span className="tw-text-vm-slate-500">Trạng thái</span>
          <b className="tw-text-slate-900">{invoice.status}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <span className="tw-text-vm-slate-500">Ngày lập</span>
          <b className="tw-text-slate-900">{invoice.issuedAt ?? "-"}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <span className="tw-text-vm-slate-500">Ngày thanh toán</span>
          <b className="tw-text-slate-900">{invoice.paidAt ?? "-"}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <span className="tw-text-vm-slate-500">Số tiền</span>
          <b className="tw-text-slate-900">{formatCurrency(invoice.amount)}</b>
        </div>
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <span className="tw-text-vm-slate-500">Giảm trừ</span>
          <b className="tw-text-slate-900">{formatCurrency(invoice.discountAmount)}</b>
        </div>
        <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-3">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <span className="tw-text-vm-slate-500">Thành tiền</span>
            <strong className="tw-text-[1.2rem] tw-text-red-500">{formatCurrency(invoice.finalAmount)}</strong>
          </div>
        </div>
      </div>
      {invoice.payments.length > 0 ? (
        <div className="tw-mt-4 tw-rounded-vm-lg tw-bg-vm-slate-25 tw-p-3">
          <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-slate-900">Thanh toán</h3>
          <div className="tw-mt-3 tw-grid tw-gap-2">
            {invoice.payments.map((payment) => (
              <div className="tw-rounded-vm-md tw-bg-white tw-p-3 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700" key={payment.paymentId}>
                {getPaymentMethodLabel(payment.paymentMethod)} · {formatCurrency(payment.amount)} · {payment.status ?? "-"} · {payment.paidAt ?? "-"}
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </Modal>
  );
}

function PaymentConfirmModal({
  amount,
  errorMessage,
  isSubmitting,
  note,
  onClose,
  onNoteChange,
  onPaymentMethodChange,
  onSubmit,
  onTransactionRefChange,
  open,
  paymentMethod,
  transactionRef,
}: {
  amount: number;
  errorMessage: string;
  isSubmitting: boolean;
  note: string;
  onClose: () => void;
  onNoteChange: (value: string) => void;
  onPaymentMethodChange: (value: LostCardPaymentMethod) => void;
  onSubmit: () => void;
  onTransactionRefChange: (value: string) => void;
  open: boolean;
  paymentMethod: LostCardPaymentMethod;
  transactionRef: string;
}) {
  return (
    <Modal
      actions={
        <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-3">
          <button
            className="tw-min-h-10 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-700"
            disabled={isSubmitting}
            type="button"
            onClick={onClose}
          >
            Đóng
          </button>
          <button
            className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-lg tw-bg-vm-primary tw-px-4 tw-text-[0.9rem] tw-font-extrabold tw-text-white disabled:tw-bg-slate-200 disabled:tw-text-vm-slate-500"
            disabled={isSubmitting}
            type="button"
            onClick={onSubmit}
          >
            {isSubmitting ? <i className="fas fa-spinner fa-spin" /> : null}
            {isSubmitting
              ? "Đang xử lý..."
              : paymentMethod === "VNPAY"
                ? "Thanh toán qua VNPay"
                : "Xác nhận đã thanh toán"}
          </button>
        </div>
      }
      onClose={onClose}
      open={open}
      title="Xác nhận thanh toán"
      width="md"
    >
      <div className="tw-grid tw-gap-4">
        {errorMessage ? (
          <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-[0.86rem] tw-font-bold tw-text-red-600">
            {errorMessage}
          </div>
        ) : null}
        <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3">
          <span className="tw-block tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Số tiền cần thu</span>
          <strong className="tw-mt-1 tw-block tw-text-[1.25rem] tw-font-extrabold tw-text-red-500">{formatCurrency(amount)}</strong>
        </div>
        <label>
          <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Hình thức thanh toán</span>
          <select
            className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none"
            value={paymentMethod}
            onChange={(event) => onPaymentMethodChange(event.target.value as LostCardPaymentMethod)}
          >
            {paymentMethodOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        {paymentMethod === "VNPAY" ? (
          <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-blue-200 tw-bg-blue-50 tw-p-3 tw-text-[0.82rem] tw-font-bold tw-leading-relaxed tw-text-blue-700">
            Hệ thống sẽ chuyển sang cổng VNPay. Hóa đơn chỉ được ghi nhận đã thanh toán khi VNPay trả về giao dịch thành công.
            Số tiền tối thiểu là {formatCurrency(VNPAY_MINIMUM_AMOUNT)}.
          </div>
        ) : null}
        {paymentMethod !== "CASH" && paymentMethod !== "VNPAY" ? (
          <label>
            <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Mã giao dịch</span>
            <input
              className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-semibold tw-text-slate-900 tw-outline-none"
              placeholder="Nhập mã giao dịch"
              value={transactionRef}
              onChange={(event) => onTransactionRefChange(event.target.value)}
            />
          </label>
        ) : null}
        <label>
          <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Ghi chú</span>
          <textarea
            className="tw-min-h-[94px] tw-w-full tw-resize-none tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-3 tw-text-[0.92rem] tw-font-semibold tw-leading-relaxed tw-text-slate-900 tw-outline-none"
            value={note}
            onChange={(event) => onNoteChange(event.target.value)}
          />
        </label>
      </div>
    </Modal>
  );
}

export function LostCardCreatePage() {
  const navigate = useNavigate();
  const [licensePlate, setLicensePlate] = useState("");
  const [preview, setPreview] = useState<LostCardPreviewResponse | null>(null);
  const [timeOfLost, setTimeOfLost] = useState(() => toDateTimeLocalValue(new Date()));
  const [reporterName, setReporterName] = useState("");
  const [reporterPhone, setReporterPhone] = useState("");
  const [identifyCard, setIdentifyCard] = useState("");
  const [registrationLicense, setRegistrationLicense] = useState("");
  const [note, setNote] = useState("");
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState("");

  const handlePreview = async () => {
    const normalizedLicensePlate = licensePlate.trim();
    if (!normalizedLicensePlate) {
      setFormError("Vui lòng nhập biển số cần tra cứu.");
      return;
    }

    setIsPreviewLoading(true);
    setFormError("");
    setPreview(null);

    try {
      const response = await previewLostCardReport(normalizedLicensePlate);
      setPreview(response.data);
      setReporterName((current) => current || response.data.customerName || "");
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không tra cứu được dữ liệu mất thẻ.");
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleCreateReport = async () => {
    if (!preview) {
      setFormError("Vui lòng tra cứu biển số trước khi tạo phiếu.");
      return;
    }
    if (!reporterName.trim() || !reporterPhone.trim()) {
      setFormError("Vui lòng nhập người báo mất và số điện thoại.");
      return;
    }
    if (!identifyCard.trim() && !registrationLicense.trim()) {
      setFormError("Vui lòng nhập CCCD hoặc giấy tờ xe.");
      return;
    }
    const lostAt = new Date(timeOfLost);
    if (!timeOfLost || Number.isNaN(lostAt.getTime())) {
      setFormError("Vui lòng nhập thời gian mất thẻ hợp lệ.");
      return;
    }

    const parkingSessionId = preview.parkingSession?.parkingSessionId ?? null;
    const subscriptionId = preview.subscription?.subscriptionId ?? null;
    if (!parkingSessionId && !subscriptionId) {
      setFormError("Dữ liệu tra cứu thiếu phiên gửi xe hoặc gói đăng ký để tạo phiếu.");
      return;
    }

    setIsSubmitting(true);
    setFormError("");

    try {
      const response = await createLostCardReport({
        parkingSessionId,
        subscriptionId,
        timeOfLost: lostAt.toISOString(),
        reporterName: reporterName.trim(),
        reporterPhone: reporterPhone.trim(),
        identifyCard: toOptionalValue(identifyCard),
        registrationLicense: toOptionalValue(registrationLicense),
        note: toOptionalValue(note),
      });

      navigate(`/admin/lost/detail?reportId=${response.data.lostCardReport.lostCardReportId}&payment=unpaid`);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không tạo được phiếu báo mất thẻ.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-4 tw-pb-4">
            <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-extrabold tw-leading-tight tw-text-slate-900">Tạo phiếu báo mất thẻ</h1>
            <Link
              className="tw-inline-flex tw-min-h-11 tw-items-center tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-700"
              to="/admin/lost"
            >
              <i className="fas fa-arrow-left" />
              <span>Quay lại</span>
            </Link>
          </div>

          <div className="tw-grid tw-grid-cols-[260px_minmax(0,1fr)_360px] tw-gap-4 max-xl:tw-grid-cols-[240px_minmax(0,1fr)] max-lg:tw-grid-cols-1">
            <aside className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
              <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
                <h2 className="tw-m-0 tw-text-[1.05rem] tw-font-extrabold tw-text-slate-900">Quy trình</h2>
              </div>
              <div className="tw-p-4">
                <StepList steps={createSteps} />
                <div className="tw-mt-4 tw-rounded-vm-lg tw-bg-brand-50 tw-p-4">
                  <h3 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900">Quy tắc nhanh</h3>
                  <ul className="tw-mb-0 tw-mt-3 tw-grid tw-gap-2 tw-pl-4 tw-text-[0.8rem] tw-font-bold tw-leading-relaxed tw-text-blue-900">
                    <li>Phiếu chỉ tạo sau khi đã đối chiếu ảnh và biển số.</li>
                    <li>Khách phải cung cấp CCCD hoặc giấy tờ xe.</li>
                    <li>Sau khi tạo phiếu, thẻ cũ bị khóa để tránh sử dụng lại.</li>
                  </ul>
                </div>
              </div>
            </aside>

            <main className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
                <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">Thông tin báo mất</h2>
                <span className="tw-inline-flex tw-min-h-7 tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-3 tw-text-[0.76rem] tw-font-extrabold tw-text-vm-primary">
                  {preview ? getContextLabel(preview.context) : "Chưa tra cứu"}
                </span>
              </div>

              <div className="tw-grid tw-gap-4 tw-p-4">
                {formError ? (
                  <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-[0.86rem] tw-font-bold tw-text-red-600">
                    {formError}
                  </div>
                ) : null}
                <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_150px] tw-gap-3 max-md:tw-grid-cols-1">
                  <label className="tw-flex tw-min-h-11 tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
                    <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Biển số</span>
                    <input
                      className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none"
                      placeholder="VD: 59A1-12345"
                      value={licensePlate}
                      onChange={(event) => setLicensePlate(event.target.value)}
                    />
                  </label>
                  <button
                    className="tw-min-h-11 tw-rounded-vm-lg tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white tw-shadow-[0_12px_22px_rgba(37,99,235,0.18)] disabled:tw-bg-slate-200 disabled:tw-text-vm-slate-500"
                    disabled={isPreviewLoading}
                    type="button"
                    onClick={handlePreview}
                  >
                    {isPreviewLoading ? "Đang tra cứu..." : "Tra cứu"}
                  </button>
                </div>

                <div className="tw-grid tw-grid-cols-3 tw-gap-3 max-md:tw-grid-cols-1">
                  <InfoBox label="Phiên gửi xe" value={preview?.parkingSession ? getSessionLabel({ parkingSession: preview.parkingSession } as LostCardReportDetailResponse) : "Không có phiên trong bãi"} />
                  <InfoBox label="Thẻ cũ" value={preview?.oldCardNumber || "-"} />
                  <InfoBox label="Khách hàng" value={preview?.customerName || (preview?.customerId ? "Khách hàng đã liên kết" : "Không liên kết")} />
                </div>

                <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-md:tw-grid-cols-1">
                  <LostCardEvidenceImage
                    alt="Ảnh người gửi xe khi vào bãi"
                    emptyText="Phiên gửi xe chưa có ảnh người gửi xe"
                    imagePath={preview?.checkInPersonImagePath}
                    label="Ảnh người gửi xe khi vào"
                  />
                  <LostCardEvidenceImage
                    alt="Ảnh biển số xe khi vào bãi"
                    emptyText="Phiên gửi xe chưa có ảnh biển số"
                    imagePath={preview?.checkInLicensePlateImagePath}
                    label="Ảnh biển số khi vào"
                  />
                </div>

                <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-md:tw-grid-cols-1">
                  <label>
                    <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Thời gian mất thẻ</span>
                    <input className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none" type="datetime-local" value={timeOfLost} onChange={(event) => setTimeOfLost(event.target.value)} />
                  </label>
                  <label>
                    <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Người báo mất</span>
                    <input className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none" value={reporterName} onChange={(event) => setReporterName(event.target.value)} />
                  </label>
                  <label>
                    <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Số điện thoại</span>
                    <input className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none" value={reporterPhone} onChange={(event) => setReporterPhone(event.target.value)} />
                  </label>
                  <label>
                    <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">CCCD</span>
                    <input className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none" value={identifyCard} onChange={(event) => setIdentifyCard(event.target.value)} />
                  </label>
                  <label>
                    <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Giấy đăng ký xe</span>
                    <input className="tw-h-11 tw-w-full tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.95rem] tw-font-extrabold tw-text-slate-900 tw-outline-none" value={registrationLicense} onChange={(event) => setRegistrationLicense(event.target.value)} />
                  </label>
                  <label className="tw-col-span-2 max-md:tw-col-span-1">
                    <span className="tw-mb-1.5 tw-block tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Ghi chú</span>
                    <textarea className="tw-min-h-[86px] tw-w-full tw-resize-none tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-3 tw-text-[0.92rem] tw-font-semibold tw-text-vm-slate-700 tw-outline-none" value={note} onChange={(event) => setNote(event.target.value)} />
                  </label>
                </div>
              </div>
            </main>

            <aside className="tw-grid tw-content-start tw-gap-4 max-xl:tw-col-span-2 max-lg:tw-col-span-1">
              <CostPanel
                lostCardFee={Number(preview?.lostCardFee ?? 0)}
                ticketPrice={Number(preview?.ticketPrice ?? 0)}
                totalAmount={Number(preview?.totalAmount ?? 0)}
              />
              <section className="tw-rounded-vm-lg tw-bg-brand-50 tw-p-4 tw-text-blue-900">
                <h3 className="tw-m-0 tw-text-[0.95rem] tw-font-extrabold">Sau khi tạo phiếu</h3>
                <ul className="tw-mb-0 tw-mt-3 tw-grid tw-gap-2 tw-pl-4 tw-text-[0.82rem] tw-font-bold tw-leading-relaxed">
                  <li>Hóa đơn được tạo ở trạng thái chờ thanh toán.</li>
                  <li>Phiếu có thể hủy nếu khách tìm lại thẻ trước khi thanh toán.</li>
                  <li>Khi khách đã thanh toán, chuyển sang bước hoàn tất xử lý.</li>
                </ul>
              </section>
              <button
                className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-lg tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white tw-shadow-[0_12px_22px_rgba(37,99,235,0.18)] hover:tw-bg-brand-700 hover:tw-text-white hover:tw-no-underline"
                disabled={isSubmitting}
                type="button"
                onClick={handleCreateReport}
              >
                {isSubmitting ? <i className="fas fa-spinner fa-spin" /> : null}
                {isSubmitting ? "Đang tạo phiếu..." : "Tạo phiếu và in biên bản"}
              </button>
            </aside>
          </div>
        </div>
      </section>
    </div>
  );
}

export function LostCardDetailPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const reportId = searchParams.get("reportId");
  const paymentParam = searchParams.get("payment");
  const [detail, setDetail] = useState<LostCardReportDetailResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");
  const [isInvoiceDetailOpen, setIsInvoiceDetailOpen] = useState(false);
  const [isPaymentFormOpen, setIsPaymentFormOpen] = useState(false);
  const [paymentFormError, setPaymentFormError] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<LostCardPaymentMethod>("CASH");
  const [paymentNote, setPaymentNote] = useState("Nhân viên xác nhận thanh toán phiếu báo mất thẻ");
  const [transactionRef, setTransactionRef] = useState("");
  const [availableCards, setAvailableCards] = useState<LostCardReplacementCardResponse[]>([]);
  const [selectedNewCardId, setSelectedNewCardId] = useState("");
  const [isCardsLoading, setIsCardsLoading] = useState(false);
  const [isPaymentSubmitting, setIsPaymentSubmitting] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  useEffect(() => {
    if (!reportId) {
      setErrorMessage("Thiếu mã định danh phiếu báo mất thẻ.");
      setDetail(null);
      return;
    }

    const currentReportId = reportId;
    let cancelled = false;

    async function fetchDetail() {
      setIsLoading(true);
      setErrorMessage("");

      try {
        const response = await getLostCardReportById(currentReportId);
        if (!cancelled) {
          setDetail(response.data);
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error instanceof Error ? error.message : "Không tải được chi tiết phiếu báo mất thẻ.");
          setDetail(null);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    fetchDetail();

    return () => {
      cancelled = true;
    };
  }, [reportId]);

  useEffect(() => {
    const vnpayResult = searchParams.get("vnpayResult");
    if (!vnpayResult) return;

    if (vnpayResult === "success" && searchParams.get("paymentStatus") === "SUCCESS") {
      setActionMessage("Thanh toán VNPay thành công. Bạn có thể tiếp tục hoàn tất phiếu.");
      setActionError("");
      if (reportId) {
        void getLostCardReportById(reportId)
          .then((response) => setDetail(response.data))
          .catch((error) => {
            setActionError(
              error instanceof Error
                ? error.message
                : "Thanh toán thành công nhưng chưa tải lại được trạng thái hóa đơn.",
            );
          });
      }
    } else if (vnpayResult === "cancelled") {
      setActionError("Giao dịch VNPay đã bị hủy. Hóa đơn vẫn đang chờ và có thể thanh toán lại bằng VNPay hoặc phương thức khác.");
    } else {
      setActionError("Giao dịch VNPay chưa thành công. Hóa đơn chưa được ghi nhận và có thể thực hiện thanh toán lại.");
    }

    const nextParams = new URLSearchParams(searchParams);
    ["vnpayResult", "transactionRef", "responseCode", "paymentStatus"].forEach((key) => nextParams.delete(key));
    setSearchParams(nextParams, { replace: true });
  }, [reportId, searchParams, setSearchParams]);

  const report = detail?.lostCardReport ?? null;
  const paymentState: DetailPaymentState = detail && report ? getPaymentStateFromDetail(report, detail.invoice, paymentParam) : "unpaid";
  const isPaid = paymentState === "paid" || paymentState === "resolved";
  const isResolved = report?.status === "RESOLVED";
  const isCancelled = report?.status === "CANCELLED";
  const detailHistorySteps = getDetailHistorySteps(paymentState, report?.context);
  const contextLabel = getContextLabel(report?.context);
  const totalAmount = report ? getTotalAmount(report, detail?.invoice ?? null) : 0;
  const needsNewCard = requiresReplacementCard(report?.context);

  useEffect(() => {
    if (!report || !isPaid || isResolved || !needsNewCard) {
      setAvailableCards([]);
      setSelectedNewCardId("");
      return;
    }

    let cancelled = false;
    const currentReportId = report.lostCardReportId;

    async function fetchAvailableCards() {
      setIsCardsLoading(true);

      try {
        const response = await getAvailableReplacementCards(currentReportId);
        if (!cancelled) {
          setAvailableCards(response.data);
        }
      } catch (error) {
        if (!cancelled) {
          setActionError(error instanceof Error ? error.message : "Không tải được danh sách thẻ khả dụng.");
          setAvailableCards([]);
        }
      } finally {
        if (!cancelled) {
          setIsCardsLoading(false);
        }
      }
    }

    fetchAvailableCards();

    return () => {
      cancelled = true;
    };
  }, [isPaid, isResolved, needsNewCard, report]);

  const stepTimes = useMemo(() => {
    if (!report) {
      return new Map<number, string>();
    }

    return new Map<number, string>([
      [1, report.createdAt || report.notificationTime || "-"],
      [2, detail?.invoice?.paidAt || (detail?.invoice?.status === "UNPAID" ? "Đang chờ" : detail?.invoice?.status ?? "-")],
      [3, report.resolvedAt || (isResolved ? report.updatedAt : "Đang xử lý")],
    ]);
  }, [detail?.invoice?.paidAt, detail?.invoice?.status, isResolved, report]);

  const statusBadgeClass = isResolved
    ? "tw-bg-green-100 tw-text-green-600"
    : isCancelled
      ? "tw-bg-slate-100 tw-text-slate-600"
      : "tw-bg-red-100 tw-text-red-500";

  const refreshDetail = async (lostCardReportId: string) => {
    const response = await getLostCardReportById(lostCardReportId);
    setDetail(response.data);
  };

  const canConfirmPayment = Boolean(detail?.invoice && detail.invoice.status === "UNPAID" && report?.status !== "CANCELLED");

  const handleOpenPaymentForm = (method: LostCardPaymentMethod) => {
    if (!canConfirmPayment) return;
    setActionError("");
    setActionMessage("");
    setPaymentFormError("");
    setPaymentMethod(method);
    setIsPaymentFormOpen(true);
  };

  const handleConfirmPayment = async () => {
    if (!detail?.invoice || !report) return;
    if (!canConfirmPayment) {
      setPaymentFormError("Không thể xác nhận thanh toán cho phiếu đã hủy hoặc hóa đơn không còn chờ thanh toán.");
      return;
    }
    if (paymentMethod !== "CASH" && paymentMethod !== "VNPAY" && !transactionRef.trim()) {
      setPaymentFormError("Vui lòng nhập mã giao dịch khi thanh toán không dùng tiền mặt.");
      return;
    }

    setIsPaymentSubmitting(true);
    setPaymentFormError("");
    setActionMessage("");

    try {
      if (paymentMethod === "VNPAY") {
        if (Number(detail.invoice.finalAmount) < VNPAY_MINIMUM_AMOUNT) {
          throw new Error("VNPay Sandbox chỉ nhận hóa đơn từ 10.000 đồng. Vui lòng chọn phương thức khác.");
        }
        const response = await createVnpayInvoicePayment(
          detail.invoice.invoiceId,
          `/admin/lost/detail?reportId=${report.lostCardReportId}`,
        );
        window.location.assign(response.data.paymentUrl);
        return;
      }

      await recordLostCardInvoicePayment(detail.invoice.invoiceId, {
        amount: Number(detail.invoice.finalAmount),
        note: paymentNote.trim(),
        paymentMethod,
        transactionRef: toOptionalValue(transactionRef) ?? undefined,
      });
      await refreshDetail(report.lostCardReportId);
      setIsPaymentFormOpen(false);
      setActionMessage("Đã xác nhận thanh toán hóa đơn.");
    } catch (error) {
      setPaymentFormError(error instanceof Error ? error.message : "Không xác nhận được thanh toán.");
    } finally {
      setIsPaymentSubmitting(false);
    }
  };

  const handleCompleteReport = async () => {
    if (!report) return;
    if (requiresReplacementCard(report.context) && !selectedNewCardId) {
      setActionError("Vui lòng chọn thẻ mới trước khi hoàn tất phiếu.");
      return;
    }

    setIsCompleting(true);
    setActionError("");
    setActionMessage("");

    try {
      await resolveLostCardReport(report.lostCardReportId, selectedNewCardId || undefined);
      await refreshDetail(report.lostCardReportId);
      setActionMessage("Đã hoàn tất xử lý phiếu báo mất thẻ.");
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Không hoàn tất được phiếu báo mất thẻ.");
    } finally {
      setIsCompleting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
        <section className="content tw-pb-8">
          <div className="container-fluid tw-max-w-[1480px]">
            <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-6 tw-text-center tw-font-bold tw-text-vm-slate-600">
              Đang tải chi tiết phiếu báo mất thẻ...
            </div>
          </div>
        </section>
      </div>
    );
  }

  if (errorMessage || !detail || !report) {
    return (
      <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
        <section className="content tw-pb-8">
          <div className="container-fluid tw-max-w-[1480px]">
            <div className="tw-grid tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-6 tw-text-red-600">
              <strong>{errorMessage || "Không tìm thấy phiếu báo mất thẻ."}</strong>
              <Link className="tw-font-bold tw-text-vm-primary hover:tw-text-brand-700" to="/admin/lost">
                Quay lại danh sách
              </Link>
            </div>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-4 tw-pb-4">
            <div className="tw-flex tw-items-center tw-gap-3">
              <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-extrabold tw-leading-tight tw-text-slate-900">Chi tiết phiếu báo mất</h1>
              <span className={`tw-inline-flex tw-min-h-7 tw-items-center tw-rounded-full tw-px-3 tw-text-[0.76rem] tw-font-extrabold ${statusBadgeClass}`}>
                {getReportStatusLabel(report.status)}
              </span>
            </div>
            <Link
              className="tw-inline-flex tw-min-h-11 tw-items-center tw-gap-2 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.9rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] hover:tw-bg-vm-slate-25 hover:tw-text-vm-slate-700"
              to="/admin/lost"
            >
              <i className="fas fa-arrow-left" />
              <span>Quay lại</span>
            </Link>
          </div>

          <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_420px] tw-gap-4 max-xl:tw-grid-cols-1">
            <main className="tw-grid tw-gap-4">
              <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
                <div className="tw-flex tw-items-start tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
                  <div>
                    <h2 className="tw-m-0 tw-text-[1.12rem] tw-font-extrabold tw-text-slate-900">Phiếu báo mất thẻ</h2>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">
                      Báo lúc {report.notificationTime || "-"} · Mất lúc {report.timeOfLost || "-"}
                    </p>
                  </div>
                  <span className="tw-inline-flex tw-min-h-7 tw-items-center tw-rounded-full tw-bg-brand-50 tw-px-3 tw-text-[0.76rem] tw-font-extrabold tw-text-vm-primary">{contextLabel}</span>
                </div>
                <div className="tw-grid tw-gap-4 tw-p-4">
                  <div className="tw-grid tw-grid-cols-3 tw-gap-3 max-md:tw-grid-cols-1">
                    <InfoBox label="Biển số" value={detail.licensePlate || "-"} />
                    <InfoBox label="Thẻ cũ" value={detail.oldCardNumber || "-"} />
                    <InfoBox label="Phiên gửi xe" value={getSessionLabel(detail)} />
                    <InfoBox label="Khách hàng" value={detail.customerName || report.reporterName || "Không liên kết"} />
                    <InfoBox label="Người báo" value={`${report.reporterName || "-"} · ${report.reporterPhone || "-"}`} />
                    <InfoBox label="Giấy tờ" value={report.identifyCard || report.registrationLicense || "-"} />
                  </div>
                  {report.note ? (
                    <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3">
                      <span className="tw-block tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">Ghi chú</span>
                      <p className="tw-m-0 tw-mt-1 tw-text-[0.92rem] tw-font-semibold tw-leading-relaxed tw-text-slate-900">{report.note}</p>
                    </div>
                  ) : null}
                  <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-md:tw-grid-cols-1">
                    <LostCardEvidenceImage
                      alt="Ảnh người gửi xe khi vào bãi"
                      emptyText="Phiên gửi xe chưa có ảnh người gửi xe"
                      imagePath={detail.checkInPersonImagePath}
                      label="Ảnh người gửi xe khi vào"
                    />
                    <LostCardEvidenceImage
                      alt="Ảnh biển số xe khi vào bãi"
                      emptyText="Phiên gửi xe chưa có ảnh biển số"
                      imagePath={detail.checkInLicensePlateImagePath}
                      label="Ảnh biển số khi vào"
                    />
                  </div>
                </div>
              </section>

              <section className="tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_18px_40px_rgba(15,23,42,0.06)]">
                <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
                  <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-extrabold tw-text-slate-900">Lịch sử xử lý</h2>
                  <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">Các mốc xử lý của hệ thống</p>
                </div>
                <div className="tw-grid tw-gap-3 tw-p-4">
                  {detailHistorySteps.map((step) => (
                    <div className="tw-grid tw-grid-cols-[32px_minmax(0,1fr)_80px] tw-items-center tw-gap-3" key={step.number}>
                      <span className={`tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-xl tw-text-[0.9rem] tw-font-extrabold tw-text-white ${step.number === 1 ? "tw-bg-vm-primary" : step.number === 2 ? "tw-bg-green-600" : "tw-bg-orange-500"}`}>
                        {step.number}
                      </span>
                      <span>
                        <b className="tw-block tw-text-[0.92rem] tw-text-slate-900">{step.title}</b>
                        <small className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">{step.subtitle}</small>
                      </span>
                      <time className="tw-text-right tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">
                        {stepTimes.get(step.number) ?? "-"}
                      </time>
                    </div>
                  ))}
                </div>
              </section>
            </main>

            <aside className="tw-grid tw-content-start tw-gap-4">
              {actionMessage ? (
                <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-green-200 tw-bg-green-50 tw-p-3 tw-text-[0.86rem] tw-font-bold tw-text-green-700">
                  {actionMessage}
                </div>
              ) : null}
              {actionError ? (
                <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-[0.86rem] tw-font-bold tw-text-red-600">
                  {actionError}
                </div>
              ) : null}
              <InvoicePaymentPanel
                canConfirmPayment={canConfirmPayment}
                invoice={detail.invoice}
                isPaymentSubmitting={isPaymentSubmitting}
                lostCardFee={Number(report.lostCardFee ?? 0)}
                onStartPayment={handleOpenPaymentForm}
                onShowInvoice={() => setIsInvoiceDetailOpen(true)}
                state={paymentState}
                ticketPrice={Number(report.ticketPrice ?? 0)}
                totalAmount={totalAmount}
              />
              <InvoiceDetailPanel
                invoice={detail.invoice}
                isOpen={isInvoiceDetailOpen}
                onClose={() => setIsInvoiceDetailOpen(false)}
              />
              <PaymentConfirmModal
                amount={Number(detail.invoice?.finalAmount ?? totalAmount)}
                errorMessage={paymentFormError}
                isSubmitting={isPaymentSubmitting}
                note={paymentNote}
                onClose={() => setIsPaymentFormOpen(false)}
                onNoteChange={(value) => {
                  setPaymentFormError("");
                  setPaymentNote(value);
                }}
                onPaymentMethodChange={(value) => {
                  setPaymentFormError("");
                  setPaymentMethod(value);
                }}
                onSubmit={handleConfirmPayment}
                onTransactionRefChange={(value) => {
                  setPaymentFormError("");
                  setTransactionRef(value);
                }}
                open={isPaymentFormOpen}
                paymentMethod={paymentMethod}
                transactionRef={transactionRef}
              />
              <LostCardResolvePanel
                availableCards={availableCards}
                context={report.context}
                contextLabel={contextLabel}
                isCardsLoading={isCardsLoading}
                isCompleting={isCompleting}
                onComplete={handleCompleteReport}
                onNewCardChange={setSelectedNewCardId}
                reportStatus={report.status}
                selectedNewCardId={selectedNewCardId}
                state={paymentState}
              />
              <LostCardCancelPanel hidden={isPaid || report.status !== "OPEN"} />
            </aside>
          </div>
        </div>
      </section>
    </div>
  );
}
