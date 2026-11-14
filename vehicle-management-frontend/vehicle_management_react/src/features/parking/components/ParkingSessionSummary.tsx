import { Button, Card, CardContent, CardHeader } from "@/components/ui";
import { VNPAY_MINIMUM_AMOUNT } from "@/features/parking/api/parkingPaymentApi";
import type {
  ParkingSessionCheckOutPreviewResponse,
  ParkingSessionOperationResponse,
} from "@/features/parking/api/parkingSessionApi";
import { cn } from "@/lib/cn";
import { userGuideAnchorProps } from "@/features/user-guide";
import { formatApplicationDateTime } from "@/shared/time/applicationTime";
import { displayLicensePlate } from "@/shared/utils/licensePlate";
import type { ParkingOperationMode } from "./OperationModeTabs";

type ParkingSessionSummaryProps = {
  isPaymentActionLoading?: boolean;
  mode: ParkingOperationMode;
  onPayCash?: () => void;
  onRetryVnpay?: () => void;
  paymentActionError?: string;
  preview?: ParkingSessionCheckOutPreviewResponse | null;
  result?: ParkingSessionOperationResponse | null;
  parkingSpaceCode?: string;
  trackingMode?: "CAPACITY" | "SPACE" | null;
  zoneLabel?: string;
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
  return value?.trim() || "--";
}

function formatCurrentCheckOutTime() {
  return formatApplicationDateTime(new Date());
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
      <div className="tw-relative tw-h-[150px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-slate-950">
        <img src={imagePath} alt={alt} className="tw-h-full tw-w-full tw-object-contain tw-object-center" />
      </div>
    );
  }

  if (empty) {
    return (
      <div className="tw-flex tw-h-[150px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-500">
        --
      </div>
    );
  }

  return (
    <div className="tw-relative tw-h-[150px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100">
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

function LicensePlateHighlight({ value }: { value: string }) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-brand-50 tw-px-4 tw-py-3">
      <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-primary">
        <i className="fas fa-car" />
        Biển số
      </span>
      <strong className="tw-min-w-0 tw-text-right tw-text-[1.4rem] tw-font-black tw-leading-none tw-text-slate-950">
        {value}
      </strong>
    </div>
  );
}

function customerTypeLabel(customerType?: string) {
  if (customerType === "SUBSCRIPTION") return "Khách đăng ký";
  if (customerType === "VISITOR") return "Khách vãng lai";
  return "--";
}

export function ParkingSessionSummary({
  isPaymentActionLoading = false,
  mode,
  onPayCash,
  onRetryVnpay,
  paymentActionError,
  preview,
  result,
  parkingSpaceCode,
  trackingMode,
  zoneLabel,
}: ParkingSessionSummaryProps) {
  const isCheckIn = mode === "check-in";
  const session = result?.parkingSession ?? preview?.parkingSession;
  const event = result?.parkingEvent;
  const checkInEvent = preview?.checkInEvent;
  const invoice = result && "invoice" in result ? result.invoice : null;
  const payableAmount = invoice?.finalAmount ?? session?.totalPrice ?? preview?.estimatedTotalPrice ?? null;
  const customerType = result?.customerType ?? preview?.customerType;
  const resolvedTrackingMode = session?.trackingMode ?? trackingMode;
  const resolvedSpaceCode = session?.parkingSpaceCode ?? parkingSpaceCode;
  const licensePlate = fallback(
    session?.licensePlateOutDisplay
    ?? session?.licensePlateInDisplay
    ?? event?.licensePlateDetectedDisplay
    ?? displayLicensePlate(session?.licensePlateOut ?? session?.licensePlateIn ?? event?.licensePlateDetected),
  );
  const checkOutTime = result
    ? session?.checkOutTime ?? event?.eventTime ?? preview?.previewCheckOutTime
    : preview?.previewCheckOutTime ?? (isCheckIn ? null : formatCurrentCheckOutTime());
  const checkInImagePath = checkInEvent?.licensePlateImagePath;
  const checkInVehicleImagePath = checkInEvent?.vehicleImagePath;
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
        <div {...userGuideAnchorProps("PARKING_OPERATION.PRICE")}>
          <FeeHighlight amount={payableAmount} isCheckIn={isCheckIn} />
        </div>

        <div className="tw-grid tw-gap-2.5">
          <DetailRow icon="far fa-user" label="Loại hành khách" value={customerTypeLabel(customerType)} />
          <DetailRow icon="fas fa-map-marker-alt" label="Khu vực" value={fallback(zoneLabel)} />
          {resolvedTrackingMode !== "CAPACITY" && (session?.parkingSpaceId || resolvedSpaceCode) ? (
            <DetailRow icon="fas fa-parking" label="Ô đỗ" value={fallback(resolvedSpaceCode)} />
          ) : null}
          <DetailRow icon="far fa-address-card" label="Mã phiên đỗ xe" value={session?.parkingSessionId?.slice(0, 13) ?? "--"} />
          <DetailRow icon="far fa-clock" label="Thời gian check-in" value={formatApplicationDateTime(session?.checkInTime)} />
          <DetailRow
            icon="far fa-clock"
            label="Thời gian check-out"
            value={event?.eventType === "CHECK_OUT_PENDING" ? "Chưa hoàn tất" : formatApplicationDateTime(checkOutTime)}
          />
        </div>

        <LicensePlateHighlight value={licensePlate} />

        {!isCheckIn ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3" {...userGuideAnchorProps("PARKING_OPERATION.PLATE_COMPARISON")}>
            <div className="tw-mb-3 tw-grid tw-gap-1">
              <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900">Hình ảnh khi vào</h3>
              <span className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                Nhân viên đối chiếu với hình ảnh camera hiện tại trước khi xác nhận check-out.
              </span>
            </div>

            <div className="tw-grid tw-grid-cols-[96px_minmax(0,1fr)] tw-gap-2.5">
              <span className="tw-self-center tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-700">Biển số</span>
              <MiniLaneImage alt="Ảnh biển số khi vào" empty={!checkInImagePath} imagePath={checkInImagePath} />

              <span className="tw-self-center tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-700">Xe / phương tiện</span>
              <MiniLaneImage alt="Ảnh xe khi vào" empty={!checkInVehicleImagePath} imagePath={checkInVehicleImagePath} />

              <span className="tw-self-center tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-700">Người / tài xế</span>
              <MiniLaneImage alt="Ảnh người khi vào" empty={!checkInPersonImagePath} imagePath={checkInPersonImagePath} />

              <span />
              <span className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{formatApplicationDateTime(session?.checkInTime ?? checkInEvent?.eventTime)}</span>
            </div>
          </div>
        ) : null}

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

            <div className="tw-grid tw-grid-cols-2 tw-gap-2" {...userGuideAnchorProps("PARKING_OPERATION.PAYMENT_ACTION")}>
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
        ) : !isCheckIn ? (
          <div
            className="tw-grid tw-gap-2 tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-3"
            {...userGuideAnchorProps("PARKING_OPERATION.PAYMENT_ACTION")}
          >
            <strong className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-700">Hoàn tất thanh toán</strong>
            <span className="tw-text-[0.74rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-500">
              Thao tác tiền mặt hoặc VNPay sẽ xuất hiện tại đây khi phiên xe ra có hóa đơn cần thanh toán.
            </span>
          </div>
        ) : null}
      </CardContent>
    </Card>
  );
}
