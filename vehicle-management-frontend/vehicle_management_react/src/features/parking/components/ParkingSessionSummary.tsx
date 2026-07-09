import { Button, Card, CardContent, CardHeader } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { ParkingOperationMode } from "./OperationModeTabs";

type ParkingSessionSummaryProps = {
  mode: ParkingOperationMode;
};

function DetailRow({ icon, label, tone, value }: { icon: string; label: string; tone?: "success"; value: string }) {
  return (
    <div className="tw-grid tw-grid-cols-[20px_minmax(0,1fr)_auto] tw-items-center tw-gap-3">
      <i className={cn(icon, "tw-text-center tw-text-vm-slate-500")} />
      <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      <strong className={cn("tw-min-w-0 tw-text-right tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900", tone === "success" ? "tw-text-emerald-600" : "")}>{value}</strong>
    </div>
  );
}

function MiniLaneImage({ empty }: { empty?: boolean }) {
  if (empty) {
    return (
      <div className="tw-flex tw-h-[96px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-text-[2rem] tw-text-vm-slate-500">
        <i className="far fa-image" />
      </div>
    );
  }

  return (
    <div className="tw-relative tw-h-[96px] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100">
      <div className="tw-absolute tw-inset-0 tw-bg-[linear-gradient(90deg,#d9e4d1_0_14%,#56616d_14%_76%,#f59e0b_76%_80%,#e2e8f0_80%)]" />
      <div className="tw-absolute tw-bottom-[22%] tw-left-[29%] tw-h-[44%] tw-w-[46%] tw-rounded-[20px_20px_8px_8px] tw-bg-white tw-shadow-[0_10px_20px_rgba(15,23,42,0.28)]" />
      <div className="tw-absolute tw-bottom-[28%] tw-left-[43%] tw-h-[14%] tw-w-[18%] tw-rounded-vm-sm tw-border tw-border-solid tw-border-slate-700 tw-bg-white" />
      <span className="tw-absolute tw-bottom-2 tw-left-2 tw-rounded-vm-sm tw-bg-slate-900/70 tw-px-2 tw-py-1 tw-text-[0.62rem] tw-font-bold tw-text-white">09:15:24</span>
    </div>
  );
}

function PaymentStatus({ label, value, tone }: { label: string; tone?: "success" | "warning"; value: string }) {
  const toneClassName = tone === "success" ? "tw-bg-emerald-50 tw-text-emerald-700" : tone === "warning" ? "tw-bg-amber-50 tw-text-amber-700" : "tw-text-vm-slate-900";

  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
      <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      <span className={cn("tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-extrabold", toneClassName)}>{value}</span>
    </div>
  );
}

function FeeHighlight({ isCheckIn }: { isCheckIn: boolean }) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-200 tw-bg-brand-50 tw-px-4 tw-py-3">
      <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.82rem] tw-font-extrabold tw-text-vm-primary">
        <i className="fas fa-file-invoice-dollar" />
        Phí dự kiến
      </span>
      <strong className="tw-text-[1.4rem] tw-font-black tw-leading-none tw-text-slate-950">{isCheckIn ? "--" : "15.000đ"}</strong>
    </div>
  );
}

export function ParkingSessionSummary({ mode }: ParkingSessionSummaryProps) {
  const isCheckIn = mode === "check-in";

  return (
    <Card className="tw-flex tw-min-h-0 tw-flex-col tw-overflow-hidden">
      <CardHeader className="tw-flex tw-min-h-[50px] tw-items-center tw-px-4 tw-py-0">
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Phiên hiện tại</h2>
      </CardHeader>

      <CardContent className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col tw-gap-3 tw-p-4">
        <FeeHighlight isCheckIn={isCheckIn} />

        <div className="tw-grid tw-gap-2.5">
          <DetailRow icon="far fa-user" label="Loại khách hàng" value="Khách vãng lai" />
          <DetailRow icon="fas fa-exchange-alt" label="Hành động barrier" tone="success" value={isCheckIn ? "Mở barrier" : "Chờ thanh toán"} />
          <DetailRow icon="far fa-address-card" label="Mã phiên đỗ xe" value="PS250509000123" />
          <DetailRow icon="far fa-clock" label="Thời gian check-in" value="09/05/2025 09:15:24" />
          <DetailRow icon="fas fa-map-marker-alt" label="Khu vực" value="Khu A" />
          <DetailRow icon="fas fa-car" label="Loại phương tiện" value="Ô tô con" />
        </div>

        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <div className="tw-mb-3 tw-flex tw-items-baseline tw-gap-2">
            <h3 className="tw-m-0 tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900">Xem trước khi check-out</h3>
            <span className="tw-text-[0.68rem] tw-font-bold tw-text-vm-slate-500">(xem khi chuyển sang Check-out)</span>
          </div>

          <div className="tw-grid tw-grid-cols-[1fr_26px_1fr] tw-items-center tw-gap-3">
            <div className="tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">Ảnh khi vào</span>
              <MiniLaneImage />
              <span className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">09/05/2025 09:15:24</span>
            </div>
            <i className="fas fa-angle-double-right tw-text-center tw-text-[1.25rem] tw-text-vm-slate-700" />
            <div className="tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-700">Ảnh hiện tại</span>
              <MiniLaneImage empty={isCheckIn} />
              <span className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{isCheckIn ? "--:--:--" : "09/05/2025 11:28:08"}</span>
            </div>
          </div>
        </div>

        <div className="tw-grid tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3">
          <PaymentStatus label="Phương thức thanh toán" value={isCheckIn ? "--" : "Tiền mặt"} />
          <PaymentStatus label="Trạng thái thanh toán" tone="warning" value={isCheckIn ? "Chưa thanh toán" : "Chờ thu phí"} />
          <PaymentStatus label="Trạng thái barrier" tone="success" value={isCheckIn ? "Đã mở" : "Chưa mở"} />
        </div>

        <Button
          className={cn(
            "tw-mt-auto tw-h-[46px] tw-w-full tw-shadow-none disabled:tw-opacity-100",
            isCheckIn
              ? "tw-border-vm-slate-200 tw-bg-vm-slate-100 !tw-text-vm-slate-700 hover:tw-bg-vm-slate-100"
              : "tw-border-vm-primary tw-bg-vm-primary !tw-text-white hover:tw-bg-vm-primary-hover",
          )}
          disabled={isCheckIn}
          size="lg"
        >
          <i className="fas fa-lock" />
          Xác nhận check-out
        </Button>
      </CardContent>
    </Card>
  );
}
