import { Button } from "@/components/ui";
import type { ParkingSessionCheckInPreviewResponse } from "@/features/parking/api/parkingSessionApi";
import { getSuggestedParkingSpaceId } from "@/features/parking/utils/parkingCheckInState";
import { cn } from "@/lib/cn";

type ParkingCheckInLocationPanelProps = {
  isLoading: boolean;
  levelLabel?: string;
  onChooseSpace: () => void;
  preview: ParkingSessionCheckInPreviewResponse | null;
  previewError?: string;
  previewStale?: boolean;
  selectedParkingSpaceId?: string;
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
  levelLabel,
  onChooseSpace,
  preview,
  previewError,
  previewStale = false,
  selectedParkingSpaceId,
  zoneLabel,
}: ParkingCheckInLocationPanelProps) {
  if (!preview && !isLoading && !previewError) return null;

  const selectedSpace = preview?.eligibleSpaces.find(
    (space) => space.parkingSpaceId === selectedParkingSpaceId,
  );
  const allocatedSpaceId = getSuggestedParkingSpaceId(preview);
  const allocatedSpace = preview?.eligibleSpaces.find(
    (space) => space.parkingSpaceId === allocatedSpaceId,
  );
  const location = [zoneLabel, levelLabel].filter(Boolean).join(" • ") || "Đang xác định khu vực";

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
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-sm tw-bg-white tw-p-2.5">
          <div className="tw-grid tw-gap-0.5">
            <span className="tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">
              {preview.eligibleSpaces.length} ô phù hợp
            </span>
            <strong className="tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-900">
              {selectedSpace ? `Ô đã chọn: ${selectedSpace.code}` : "Chưa chọn ô đỗ"}
            </strong>
          </div>
          {preview.selectionRequired ? (
            <Button size="sm" variant="secondary" onClick={onChooseSpace}>
              <i className="fas fa-th-large" /> {selectedSpace ? "Đổi ô" : "Chọn ô đỗ"}
            </Button>
          ) : null}
        </div>
      ) : null}

      {preview?.trackingMode === "SPACE" && preview.customerType === "SUBSCRIPTION" ? (
        <div className="tw-grid tw-gap-1 tw-rounded-vm-sm tw-border tw-border-solid tw-border-emerald-100 tw-bg-white tw-p-2.5">
          <span className="tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">Ô cố định được cấp</span>
          <strong className="tw-text-[0.86rem] tw-font-extrabold tw-text-emerald-700">
            {allocatedSpace ? `${allocatedSpace.code} • ${allocatedSpace.status}` : "Không có allocation hợp lệ"}
          </strong>
        </div>
      ) : null}
    </section>
  );
}
