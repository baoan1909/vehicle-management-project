import { Button, Card, CardContent, CardHeader } from "@/components/ui";
import { VNPAY_MINIMUM_AMOUNT } from "@/features/parking/api/parkingPaymentApi";
import type {
  ParkingSessionCheckOutPreviewResponse,
  ParkingSessionOperationResponse,
} from "@/features/parking/api/parkingSessionApi";
import { cn } from "@/lib/cn";
import type { ParkingOperationMode } from "./OperationModeTabs";

type ParkingSessionSummaryProps = {
  isPaymentActionLoading?: boolean;
  mode: ParkingOperationMode;
  onPayCash?: () => void;
  onRetryVnpay?: () => void;
  paymentActionError?: string;
  preview?: ParkingSessionCheckOutPreviewResponse | null;
  result?: ParkingSessionOperationResponse | null;
};

function formatCurrency(value?: number | null) {
  if (typeof value !== "number" || !Number.isFinite(value)) return "--";
  return new Intl.NumberFormat("vi-VN", {
    currency: "VND",
    maximumFractionDigits: 0,
    style: "currency",
  }).format(value);
}

function fallback(value?: string | null) {
  return value?.trim() || "Chưa có dữ liệu";
}

function formatCurrentCheckOutTime() {
  const now = new Date();
  const pad = (value: number) => value.toString().padStart(2, "0");
  return `${pad(now.getHours())}:${pad(now.getMinutes())} ${pad(now.getDate())}-${pad(now.getMonth() + 1)}-${now.getFullYear()}`;
}

function DetailRow({ icon, label, tone, value }: { icon: string; label: string; tone?: "success" | "warning"; value: string }) {
  return (
    <div className="tw-grid tw-grid-cols-[20px_minmax(0,1fr)_auto] tw-items-center tw-gap-3">
      <i className={cn(icon, "tw-text-center tw-text-vm-slate-500")} />
      <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      <strong
        className={cn(
          "tw-min-w-0 tw-text-right tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900",
          tone === "success" ? "tw-text-emerald-600" : "",
          tone === "warning" ? "tw-text-amber-600" : "",
        )}
      >
        {value}
      </strong>
    </div>
  );
}

function MiniLaneImage({ alt, empty, imagePath }: { alt: string; empty?: boolean; imagePath?: string | null }) {
  if (imagePath) {
    return (
      <div className="tw-relative tw-h-[210px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25">
        <img src={imagePath} alt={alt} className="tw-h-full tw-w-full tw-object-cover" />
      </div>
    );
  }

  if (empty) {
    return (
      <div className="tw-flex tw-h-[210px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-[2rem] tw-text-vm-slate-500">
        <i className="far fa-image" />
      </div>
    );
  }

  return (
    <div className="tw-relative tw-h-[210px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100">
      <div className="tw-absolute tw-inset-0 tw-bg-[linear-gradient(90deg,#d9e4d1_0_14%,#56616d_14%_76%,#f59e0b_76%_80%,#e2e8f0_80%)]" />
      <div className="tw-absolute tw-bottom-[22%] tw-left-[29%] tw-h-[44%] tw-w-[46%] tw-rounded-[20px_20px_8px_8px] tw-bg-white tw-shadow-[0_10px_20px_rgba(15,23,42,0.28)]" />
      <div className="tw-absolute tw-bottom-[28%] tw-left-[43%] tw-h-[14%] tw-w-[18%] tw-rounded-vm-sm tw-border tw-border-solid tw-border-slate-700 tw-bg-white" />
    </div>
  );
}

function PaymentStatus({ label, value, tone }: { label: string; tone?: "success" | "warning"; value: string }) {
  const toneClassName =
    tone === "success"
      ? "tw-bg-emerald-50 tw-text-emerald-700"
      : tone === "warning"
        ? "tw-bg-amber-50 tw-text-amber-700"
        : "tw-text-vm-slate-900";

  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
      <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      <span className={cn("tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-extrabold", toneClassName)}>{value}</span>
    </div>
  );
}

function FeeHighlight({ amount, isCheckIn }: { amount?: number | null; isCheckIn: boolean }) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-brand-50 tw-px-4 tw-py-3">
      <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-primary">
        <i className="fas fa-file-invoice-dollar" />
        Phí cần thu
      </span>
      <strong className="tw-text-[1.4rem] tw-font-black tw-leading-none tw-text-slate-950">{isCheckIn ? "--" : formatCurrency(amount)}</strong>
    </div>
  );
}

function customerTypeLabel(customerType?: string) {
  if (customerType === "SUBSCRIPTION") return "Khách đăng ký";
  if (customerType === "VISITOR") return "Khách vãng lai";
  return "Chưa có dữ liệu";
}

export function ParkingSessionSummary({
  isPaymentActionLoading = false,
  mode,
  onPayCash,
  onRetryVnpay,
  paymentActionError,
  preview,
  result,
}: ParkingSessionSummaryProps) {
  const isCheckIn = mode === "check-in";
  const session = result?.parkingSession ?? preview?.parkingSession;
  const event = result?.parkingEvent;
  const checkInEvent = preview?.checkInEvent;
  const invoice = result && "invoice" in result ? result.invoice : null;
  const payableAmount = invoice?.finalAmount ?? session?.totalPrice ?? preview?.estimatedTotalPrice ?? null;
  const customerType = result?.customerType ?? preview?.customerType;
  const checkOutTime = result
    ? session?.checkOutTime ?? event?.eventTime ?? preview?.previewCheckOutTime
    : preview?.previewCheckOutTime ?? (isCheckIn ? null : formatCurrentCheckOutTime());
  const checkInImagePath = checkInEvent?.licensePlateImagePath;
  const checkInPersonImagePath = checkInEvent?.personImagePath;
  const hasPricingWarning = Boolean(preview?.pricingMessage && !invoice);
  const paid = !hasPricingWarning && (invoice?.status === "PAID" || payableAmount === 0);
  const paymentStatus = hasPricingWarning
    ? "Chưa cấu hình giá"
    : invoice?.status === "PAID"
    ? "Đã thanh toán"
    : invoice
      ? "Chờ thu phí"
      : payableAmount === 0
        ? "Không phát sinh phí"
        : "Chưa có hóa đơn";
  const showPaymentActions = Boolean(result && invoice?.status === "UNPAID");
  const vnpayAvailable =
    typeof payableAmount === "number" && payableAmount >= VNPAY_MINIMUM_AMOUNT;

  return (
    <Card className="tw-flex tw-min-h-0 tw-flex-col tw-overflow-hidden">
      <CardHeader className="tw-flex tw-min-h-[50px] tw-items-center tw-px-4 tw-py-0">
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Thông tin phiên gửi</h2>
      </CardHeader>

      <CardContent className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col tw-gap-3 tw-p-4">
        <FeeHighlight amount={payableAmount} isCheckIn={isCheckIn} />

        <div className="tw-grid tw-gap-2.5">
          <DetailRow icon="far fa-user" label="Loại hành khách" value={customerTypeLabel(customerType)} />
          <DetailRow icon="far fa-address-card" label="Mã phiên đỗ xe" value={session?.parkingSessionId?.slice(0, 13) ?? "Chưa có dữ liệu"} />
          <DetailRow icon="far fa-clock" label="Thời gian check-in" value={fallback(session?.checkInTime)} />
          <DetailRow
            icon="far fa-clock"
            label="Thời gian check-out"
            value={event?.eventType === "CHECK_OUT_PENDING" ? "Chưa hoàn tất" : fallback(checkOutTime)}
          />
          <DetailRow icon="fas fa-car" label="Biển số" value={fallback(session?.licensePlateOut ?? session?.licensePlateIn ?? event?.licensePlateDetected)} />
        </div>

        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <div className="tw-mb-3 tw-flex tw-items-baseline tw-gap-2">
            <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900">Đối chiếu ảnh</h3>
          </div>

          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[1280px]:tw-grid-cols-1">
            <div className="tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">Ảnh xe khi vào</span>
              <MiniLaneImage alt="Ảnh xe khi vào" empty={!session && !checkInImagePath} imagePath={checkInImagePath} />
            </div>
            <div className="tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">Ảnh người / tài xế khi vào</span>
              <MiniLaneImage alt="Ảnh người / tài xế khi vào" empty={!session && !checkInPersonImagePath} imagePath={checkInPersonImagePath} />
            </div>
            <span className="tw-col-span-full tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{fallback(session?.checkInTime ?? checkInEvent?.eventTime)}</span>
          </div>
        </div>

        <div className="tw-grid tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <PaymentStatus label="Mã hóa đơn" value={invoice?.invoiceNo ?? "--"} />
          <PaymentStatus label="Trạng thái thanh toán" tone={paid ? "success" : "warning"} value={paymentStatus} />
        </div>

        {showPaymentActions ? (
          <div className="tw-grid tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3">
            <div className="tw-flex tw-items-start tw-gap-2.5">
              <i className="fas fa-exclamation-circle tw-mt-0.5 tw-text-amber-600" />
              <div className="tw-grid tw-gap-0.5">
                <strong className="tw-text-[0.82rem] tw-font-extrabold tw-text-amber-800">Hóa đơn chưa thanh toán</strong>
                <span className="tw-text-[0.74rem] tw-font-semibold tw-leading-snug tw-text-amber-700">
                  Có thể xác nhận tiền mặt hoặc tạo lại giao dịch VNPAY.
                </span>
              </div>
            </div>

            {!vnpayAvailable ? (
              <div className="tw-rounded-vm-sm tw-bg-white/80 tw-px-3 tw-py-2 tw-text-[0.72rem] tw-font-bold tw-text-amber-700">
                VNPAY Sandbox yêu cầu hóa đơn tối thiểu 10.000 đồng. Hóa đơn này chỉ có thể chuyển sang tiền mặt.
              </div>
            ) : null}

            {paymentActionError ? (
              <div className="tw-rounded-vm-sm tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.74rem] tw-font-bold tw-text-red-600">
                {paymentActionError}
              </div>
            ) : null}

            <div className="tw-grid tw-grid-cols-2 tw-gap-2">
              <Button
                className="tw-h-10 tw-w-full"
                disabled={!onPayCash || isPaymentActionLoading}
                loading={isPaymentActionLoading}
                onClick={onPayCash}
              >
                {!isPaymentActionLoading ? <i className="fas fa-money-bill-wave" /> : null}
                Xác nhận tiền mặt
              </Button>
              <Button
                className="tw-h-10 tw-w-full"
                disabled={!vnpayAvailable || !onRetryVnpay || isPaymentActionLoading}
                variant="secondary"
                onClick={onRetryVnpay}
              >
                <i className="fas fa-qrcode" />
                Thanh toán VNPAY
              </Button>
            </div>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
