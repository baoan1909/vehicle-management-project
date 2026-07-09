import { Button, Card, CardContent, CardHeader, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";
import type { ParkingOperationMode } from "./OperationModeTabs";

type ParkingOperationFormProps = {
  laneId: string;
  mode: ParkingOperationMode;
  onLaneChange: (laneId: string) => void;
};

const laneOptions = [
  { label: "LANE-A1-IN", value: "lane-a1-in" },
  { label: "LANE-A1-OUT", value: "lane-a1-out" },
  { label: "LANE-B1-IN", value: "lane-b1-in" },
];

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
  value,
}: {
  actionIcon?: string;
  icon: string;
  label: string;
  value: string;
}) {
  return (
    <label className="tw-m-0 tw-grid tw-gap-2">
      <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">{label}</span>
      <span className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-900">
        <i className={cn(icon, "tw-w-5 tw-text-center tw-text-vm-slate-500")} />
        <input className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-900 tw-outline-none" value={value} readOnly />
        {actionIcon ? <i className={cn(actionIcon, "tw-text-vm-primary")} /> : null}
      </span>
    </label>
  );
}

export function ParkingOperationForm({ laneId, mode, onLaneChange }: ParkingOperationFormProps) {
  const isCheckIn = mode === "check-in";

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

        <FieldShell actionIcon="fas fa-expand" icon="far fa-id-badge" label="Mã thẻ / RFID" value="04A1B2C3D4E5F6" />
        <FieldShell actionIcon="fas fa-car" icon="fas fa-car-side" label="Biển số nhận diện" value="30A-123.45" />

        <label className="tw-m-0 tw-grid tw-gap-2">
          <span className="tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">Làn xe</span>
          <div className="tw-flex tw-h-[42px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5">
            <span className="tw-inline-flex tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-vm-slate-700">
              <i className="fas fa-road" />
            </span>
            <SelectMenu
              className="tw-min-w-0 tw-flex-1"
              ariaLabel="Chọn làn xe"
              options={laneOptions}
              value={laneId}
              clearValue="lane-a1-in"
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
            />
            <span className="tw-absolute tw-bottom-3 tw-right-4 tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">0 / 200</span>
          </span>
        </label>

        <Button className="tw-h-[48px] tw-w-full tw-text-[0.96rem] tw-font-extrabold" size="lg">
          <i className="far fa-check-circle tw-text-[1.18rem]" />
          {isCheckIn ? "Xác nhận check-in" : "Xác nhận check-out"}
        </Button>
      </CardContent>
    </Card>
  );
}
