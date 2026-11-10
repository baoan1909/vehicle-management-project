import type { CheckInSpaceHoldResponse, ParkingSessionCheckInPreviewResponse } from "@/features/parking/api/parkingSessionApi";
import { cn } from "@/lib/cn";

type ParkingCheckInLocationPanelProps = {
  isLoading: boolean;
  hold?: CheckInSpaceHoldResponse | null;
  holdError?: string;
  holdRemainingSeconds?: number;
  levelLabel?: string;
  preview: ParkingSessionCheckInPreviewResponse | null;
  previewError?: string;
  previewStale?: boolean;
  zoneLabel?: string;
};

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="tw-grid tw-gap-1 tw-rounded-vm-sm tw-bg-white tw-px-3 tw-py-2">
      <span className="tw-text-[0.68rem] tw-font-bold tw-text-vm-slate-500">{label}</span>
      <strong className="tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
    </div>
  );
}

export function ParkingCheckInLocationPanel({
  isLoading,
  hold,
  holdError,
  holdRemainingSeconds = 0,
  levelLabel,
  preview,
  previewError,
  previewStale = false,
  zoneLabel,
}: ParkingCheckInLocationPanelProps) {
  if (!preview && !isLoading && !previewError) return null;

  const location = [zoneLabel, levelLabel].filter(Boolean).join(" • ") || "Đang xác định khu vực";
  const countdown = `${String(Math.floor(holdRemainingSeconds / 60)).padStart(2, "0")}:${String(holdRemainingSeconds % 60).padStart(2, "0")}`;

  return (
    <section
      className={cn(
        "tw-grid tw-gap-2.5 tw-rounded-vm-md tw-border tw-border-solid tw-p-3",
        previewError
          ? "tw-border-red-200 tw-bg-red-50"
          : previewStale
            ? "tw-border-amber-200 tw-bg-amber-50"
            : "tw-border-brand-100 tw-bg-brand-50/60",
      )}
      aria-live="polite"
    >
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
        <div className="tw-grid tw-gap-0.5">
          <strong className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-900">Vị trí check-in</strong>
          <span className="tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">{location}</span>
        </div>
        {preview ? (
          <span className="tw-rounded-full tw-bg-white tw-px-2.5 tw-py-1 tw-text-[0.68rem] tw-font-extrabold tw-text-vm-primary">
            {preview.trackingMode === "CAPACITY" ? "Theo sức chứa" : "Theo từng ô"}
          </span>
        ) : null}
      </div>

      {isLoading ? (
        <div className="tw-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-bold tw-text-vm-primary">
          <i className="fas fa-spinner fa-spin" /> Đang kiểm tra điều kiện...
        </div>
      ) : null}
      {previewStale ? (
        <div className="tw-flex tw-items-center tw-gap-2 tw-text-[0.74rem] tw-font-bold tw-text-amber-700">
          <i className="fas fa-sync-alt fa-spin" /> Availability vừa thay đổi, đang kiểm tra lại.
        </div>
      ) : null}
      {previewError ? (
        <div className="tw-flex tw-items-start tw-gap-2 tw-text-[0.76rem] tw-font-bold tw-text-red-600">
          <i className="fas fa-exclamation-circle tw-mt-0.5" /> {previewError}
        </div>
      ) : null}
      {holdError ? (
        <div className="tw-flex tw-items-start tw-gap-2 tw-text-[0.76rem] tw-font-bold tw-text-red-600">
          <i className="fas fa-exclamation-circle tw-mt-0.5" /> {holdError}
        </div>
      ) : null}

      {preview?.trackingMode === "CAPACITY" ? (
        <>
          <div className="tw-grid tw-grid-cols-2 tw-gap-2">
            <Metric label="Chỗ vật lý còn trống" value={preview.physicalAvailable} />
            <Metric label="Chỗ dành cho khách vãng lai" value={preview.visitorAvailable} />
          </div>
          <span className="tw-text-[0.74rem] tw-font-bold tw-text-vm-slate-600">Không cần chọn ô đỗ.</span>
        </>
      ) : null}

      {preview?.trackingMode === "SPACE" && preview.customerType === "VISITOR" ? (
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-sm tw-border tw-border-solid tw-border-amber-200 tw-bg-white tw-p-3">
          <div className="tw-grid tw-gap-0.5">
            <span className="tw-text-[0.7rem] tw-font-bold tw-text-amber-700">Ô được hệ thống tạm giữ</span>
            <strong className="tw-text-[1.15rem] tw-font-black tw-text-vm-slate-900">
              {hold ? `Ô ${hold.parkingSpaceCode}` : "Đang tự động gán ô..."}
            </strong>
          </div>
          {hold ? <strong className={holdRemainingSeconds <= 30 ? "tw-text-red-600" : "tw-text-amber-700"}>{countdown}</strong> : null}
        </div>
      ) : null}

      {preview?.trackingMode === "SPACE" && preview.customerType === "SUBSCRIPTION" ? (
        <div className="tw-grid tw-gap-1 tw-rounded-vm-sm tw-border tw-border-solid tw-border-emerald-100 tw-bg-white tw-p-2.5">
          <span className="tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">Ô cố định được cấp</span>
          <strong className="tw-text-[0.86rem] tw-font-extrabold tw-text-emerald-700">
            {hold ? `${hold.parkingSpaceCode} • tạm giữ ${countdown}` : "Đang xác nhận ô được cấp..."}
          </strong>
        </div>
      ) : null}
    </section>
  );
}
