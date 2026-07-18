import { useEffect, useRef, useState } from "react";

import type { SelectMenuOption } from "@/components/ui";
import { Button, Card, CardContent, CardHeader, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { ParkingOperationMode } from "./OperationModeTabs";

type OcrStatus = "idle" | "recognizing" | "success" | "review" | "error";

type ParkingOperationFormProps = {
  cardUid: string;
  cardOptions: SelectMenuOption[];
  error?: string;
  isSubmitting?: boolean;
  isLoadingCards?: boolean;
  laneId: string;
  laneOptions: SelectMenuOption[];
  licensePlate: string;
  mode: ParkingOperationMode;
  note: string;
  ocrConfidence?: number;
  ocrMessage?: string;
  ocrStatus?: OcrStatus;
  showVehicleTypeField?: boolean;
  vehicleTypeDisabled?: boolean;
  vehicleTypeId: string;
  vehicleTypeOptions: SelectMenuOption[];
  vehicleTypeRequired?: boolean;
  onCardUidChange: (value: string) => void;
  onLaneChange: (laneId: string) => void;
  onLicensePlateChange: (value: string) => void;
  onNoteChange: (value: string) => void;
  onSubmit: () => void;
  onVehicleTypeChange: (value: string) => void;
};

function ValidationChip({ icon, label }: { icon: string; label: string }) {
  return (
    <span className="tw-flex tw-h-9 tw-min-w-0 tw-items-center tw-justify-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-emerald-200 tw-bg-emerald-50 tw-px-2 tw-text-[0.72rem] tw-font-extrabold tw-text-emerald-700">
      <i className={cn(icon, "tw-text-[0.9rem]")} />
      {label}
    </span>
  );
}

function FieldShell({
  actionIcon,
  icon,
  label,
  onChange,
  placeholder,
  value,
}: {
  actionIcon?: string;
  icon: string;
  label: string;
  onChange: (value: string) => void;
  placeholder: string;
  value: string;
}) {
  return (
    <label className="tw-m-0 tw-grid tw-gap-2">
      <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      <span className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-900">
        <i className={cn(icon, "tw-w-5 tw-text-center tw-text-vm-slate-500")} />
        <input
          className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
          value={value}
          placeholder={placeholder}
          onChange={(event) => onChange(event.target.value)}
        />
        {actionIcon ? <i className={cn(actionIcon, "tw-text-vm-primary")} /> : null}
      </span>
    </label>
  );
}

function OcrStatusMessage({
  confidence,
  message,
  status,
}: {
  confidence?: number;
  message?: string;
  status: OcrStatus;
}) {
  if (status === "idle" || !message) return null;

  const toneClassName =
    status === "success"
      ? "tw-border-emerald-100 tw-bg-emerald-50 tw-text-emerald-700"
      : status === "recognizing"
        ? "tw-border-brand-100 tw-bg-brand-50 tw-text-vm-primary"
        : status === "review"
          ? "tw-border-amber-100 tw-bg-amber-50 tw-text-amber-700"
          : "tw-border-red-100 tw-bg-red-50 tw-text-red-600";
  const icon =
    status === "success"
      ? "fas fa-check-circle"
      : status === "recognizing"
        ? "fas fa-spinner fa-spin"
        : status === "review"
          ? "fas fa-exclamation-triangle"
          : "fas fa-info-circle";

  return (
    <div className={cn("tw-flex tw-min-h-9 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-px-3 tw-py-2 tw-text-[0.8rem] tw-font-extrabold", toneClassName)}>
      <i className={cn(icon, "tw-w-4 tw-text-center")} />
      <span className="tw-min-w-0 tw-flex-1">{message}</span>
      {typeof confidence === "number" && Number.isFinite(confidence) ? (
        <span className="tw-rounded-full tw-bg-white/70 tw-px-2 tw-py-0.5">{Math.round(confidence * 100)}%</span>
      ) : null}
    </div>
  );
}

function CardUidField({
  cardOptions,
  cardUid,
  helperText,
  isLoadingCards,
  onCardUidChange,
}: {
  cardOptions: SelectMenuOption[];
  cardUid: string;
  helperText?: string;
  isLoadingCards: boolean;
  onCardUidChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement | null>(null);
  const cardList = cardOptions.filter((option) => option.value);
  const dropdownLabel = isLoadingCards ? "Đang tải thẻ..." : "Chưa có thẻ phù hợp";

  useEffect(() => {
    if (!open) return undefined;

    const handlePointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpen(false);
      }
    };

    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  return (
    <div className="tw-m-0 tw-grid tw-gap-2" ref={rootRef}>
      <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Thẻ xe / RFID</span>
      <div className="tw-relative">
        <div className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-pl-3.5 tw-pr-2 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-900 focus-within:tw-border-brand-200 focus-within:tw-shadow-vm-focus">
          <i className="far fa-id-badge tw-w-5 tw-text-center tw-text-vm-slate-500" />
          <input
            className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
            value={cardUid}
            placeholder="Quẹt, nhập UID hoặc chọn thẻ"
            onChange={(event) => onCardUidChange(event.target.value)}
            onFocus={() => {
              if (cardList.length || isLoadingCards) setOpen(true);
            }}
          />
          <button
            aria-expanded={open}
            aria-label="Chọn thẻ có sẵn"
            className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-sm tw-border-0 tw-bg-transparent tw-text-vm-primary tw-transition hover:tw-bg-brand-50 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
            type="button"
            onClick={() => setOpen((current) => !current)}
          >
            <i className={cn(isLoadingCards ? "fas fa-spinner fa-spin" : "fas fa-chevron-down", "tw-text-[0.8rem]", open && !isLoadingCards ? "tw-rotate-180" : "")} />
          </button>
        </div>

        {open ? (
          <div className="tw-absolute tw-left-0 tw-right-0 tw-top-[calc(100%+6px)] tw-z-[85] tw-max-h-60 tw-overflow-y-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-1 tw-shadow-[0_12px_28px_rgba(15,23,42,0.16)] tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
            {cardList.length ? (
              cardList.map((option) => (
                <button
                  className={cn(
                    "tw-flex tw-min-h-[38px] tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-transparent tw-px-3 tw-py-2 tw-text-left tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary",
                    option.value === cardUid ? "tw-bg-brand-50 tw-text-vm-primary" : "",
                  )}
                  key={option.value}
                  type="button"
                  onClick={() => {
                    onCardUidChange(option.value);
                    setOpen(false);
                  }}
                >
                  <i className="fas fa-credit-card tw-w-4 tw-text-center tw-text-vm-slate-500" />
                  <span className="tw-min-w-0 tw-flex-1 tw-truncate">{option.label}</span>
                </button>
              ))
            ) : (
              <div className="tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-500">{dropdownLabel}</div>
            )}
          </div>
        ) : null}
      </div>
      {helperText ? (
        <span className="tw-text-[0.72rem] tw-font-semibold tw-leading-snug tw-text-vm-slate-500">{helperText}</span>
      ) : null}
    </div>
  );
}

export function ParkingOperationForm({
  cardUid,
  cardOptions,
  error,
  isLoadingCards = false,
  isSubmitting = false,
  laneId,
  laneOptions,
  licensePlate,
  mode,
  note,
  ocrConfidence,
  ocrMessage,
  ocrStatus = "idle",
  showVehicleTypeField = true,
  vehicleTypeDisabled = false,
  vehicleTypeId,
  vehicleTypeOptions,
  vehicleTypeRequired = true,
  onCardUidChange,
  onLaneChange,
  onLicensePlateChange,
  onNoteChange,
  onSubmit,
  onVehicleTypeChange,
}: ParkingOperationFormProps) {
  const isCheckIn = mode === "check-in";
  const canSubmit = Boolean(cardUid.trim() && licensePlate.trim() && laneId && (!isCheckIn || !vehicleTypeRequired || vehicleTypeId) && !isSubmitting && ocrStatus !== "recognizing");

  return (
    <Card className="tw-flex tw-min-h-0 tw-flex-col tw-overflow-hidden">
      <CardHeader className="tw-flex tw-min-h-[50px] tw-items-center tw-px-4 tw-py-0">
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-slate-900">Thông tin xử lý</h2>
      </CardHeader>

      <CardContent className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col tw-gap-3 tw-p-4">
        <div className="tw-grid tw-grid-cols-3 tw-gap-2">
          <ValidationChip icon="fas fa-shield-alt" label="Thẻ hợp lệ" />
          <ValidationChip icon={isCheckIn ? "fas fa-arrow-down" : "fas fa-arrow-up"} label={isCheckIn ? "Làn vào" : "Làn ra"} />
          <ValidationChip icon="fas fa-parking" label={isCheckIn ? "Còn chỗ" : "Đủ điều kiện"} />
        </div>

        <CardUidField
          cardOptions={cardOptions}
          cardUid={cardUid}
          isLoadingCards={isLoadingCards}
          onCardUidChange={onCardUidChange}
        />

        {showVehicleTypeField ? (
          <label className="tw-m-0 tw-grid tw-gap-2">
            <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Loại xe</span>
            <div className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5">
              <span className="tw-inline-flex tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-vm-slate-700">
                <i className="fas fa-car" />
              </span>
              <SelectMenu
                className="tw-min-w-0 tw-flex-1"
                ariaLabel="Chọn loại xe"
                options={vehicleTypeOptions.length ? vehicleTypeOptions : [{ label: "Chưa có loại xe active", value: "" }]}
                value={vehicleTypeId}
                clearValue=""
                disabled={vehicleTypeDisabled}
                onChange={onVehicleTypeChange}
                menuClassName="tw-min-w-[240px]"
                triggerClassName="!tw-h-10 !tw-border-0 !tw-px-0 !tw-shadow-none tw-text-[0.92rem] tw-font-bold"
              />
            </div>
          </label>
        ) : null}

        <FieldShell
          actionIcon="fas fa-car"
          icon="fas fa-car-side"
          label="Biển số nhận diện"
          value={licensePlate}
          placeholder="VD: 30A-123.45"
          onChange={onLicensePlateChange}
        />
        <OcrStatusMessage confidence={ocrConfidence} message={ocrMessage} status={ocrStatus} />

        <label className="tw-m-0 tw-grid tw-gap-2">
          <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Làn xe</span>
          <div className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5">
            <span className="tw-inline-flex tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-vm-slate-700">
              <i className="fas fa-road" />
            </span>
            <SelectMenu
              className="tw-min-w-0 tw-flex-1"
              ariaLabel="Chọn làn xe"
              options={laneOptions.length ? laneOptions : [{ label: "Chưa có làn active", value: "" }]}
              value={laneId}
              clearValue={laneOptions[0]?.value ?? ""}
              onChange={onLaneChange}
              triggerClassName="!tw-h-10 !tw-border-0 !tw-px-0 !tw-shadow-none tw-text-[0.92rem] tw-font-bold"
            />
          </div>
        </label>

        <label className="tw-m-0 tw-grid tw-gap-1.5">
          <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Ghi chú (tùy chọn)</span>
          <span className="tw-relative tw-flex">
            <textarea
              className="tw-h-[110px] tw-w-full tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-4 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500 focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
              maxLength={200}
              placeholder="Nhập ghi chú nếu có..."
              value={note}
              onChange={(event) => onNoteChange(event.target.value)}
            />
            <span className="tw-absolute tw-bottom-3 tw-right-4 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">{note.length} / 200</span>
          </span>
        </label>

        {error ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-bold tw-text-red-600">
            {error}
          </div>
        ) : null}

        <Button className="tw-h-[48px] tw-w-full tw-text-[0.96rem] tw-font-extrabold" size="lg" disabled={!canSubmit} loading={isSubmitting} onClick={onSubmit}>
          {!isSubmitting ? <i className="far fa-check-circle tw-text-[1.18rem]" /> : null}
          {isSubmitting ? "Đang xử lý..." : isCheckIn ? "Xác nhận check-in" : "Xác nhận check-out"}
        </Button>
      </CardContent>
    </Card>
  );
}
