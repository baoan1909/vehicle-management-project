import { useState } from "react";

import { SelectMenu } from "@/components/ui";
import { OperationModeTabs, type ParkingOperationMode } from "@/features/parking/components/OperationModeTabs";
import { ParkingCameraPanel } from "@/features/parking/components/ParkingCameraPanel";
import { ParkingOperationForm } from "@/features/parking/components/ParkingOperationForm";
import { ParkingSessionSummary } from "@/features/parking/components/ParkingSessionSummary";

const laneOptions = [
  { label: "LANE-A1-IN", value: "lane-a1-in" },
  { label: "LANE-A1-OUT", value: "lane-a1-out" },
  { label: "LANE-B1-IN", value: "lane-b1-in" },
];

const cameraOptions = [
  { label: "CAM-A1-01", value: "cam-a1-01" },
  { label: "CAM-A1-02", value: "cam-a1-02" },
  { label: "CAM-B1-01", value: "cam-b1-01" },
];

function FilterSelect({
  ariaLabel,
  icon,
  label,
  onChange,
  options,
  value,
}: {
  ariaLabel: string;
  icon?: string;
  label: string;
  onChange: (value: string) => void;
  options: Array<{ label: string; value: string }>;
  value: string;
}) {
  return (
    <div className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]">
      <span className="tw-whitespace-nowrap tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-700">{label}</span>
      <div className="tw-flex tw-min-w-[168px] tw-items-center tw-gap-2">
        {icon ? (
          <span className="tw-inline-flex tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-text-vm-slate-700">
            <i className={icon} />
          </span>
        ) : null}
        <SelectMenu
          ariaLabel={ariaLabel}
          options={options}
          value={value}
          clearValue={options[0]?.value}
          onChange={onChange}
          menuClassName="tw-min-w-[190px]"
          triggerClassName="!tw-h-8 !tw-border-0 !tw-px-0 !tw-shadow-none tw-text-[0.84rem]"
        />
      </div>
    </div>
  );
}

export function SwipeListPage() {
  const [mode, setMode] = useState<ParkingOperationMode>("check-in");
  const [laneId, setLaneId] = useState("lane-a1-in");
  const [cameraId, setCameraId] = useState("cam-a1-01");

  return (
    <main className="tw-px-4 tw-pb-5 tw-pt-3 lg:tw-px-5">
      <section className="tw-mx-auto tw-grid tw-min-h-[calc(100vh-124px)] tw-w-[min(100%,1660px)] tw-grid-rows-[auto_minmax(0,1fr)] tw-gap-3">
        <div className="tw-grid tw-grid-cols-[minmax(260px,470px)_minmax(320px,1fr)_auto] tw-items-end tw-gap-3 max-[1280px]:tw-grid-cols-1">
          <div className="tw-grid tw-gap-2">
            <h1 className="tw-m-0 tw-text-[1.36rem] tw-font-black tw-leading-tight tw-text-slate-950">Vận hành vào / ra bãi</h1>
            <OperationModeTabs mode={mode} onChange={setMode} />
          </div>

          <div className="tw-flex tw-flex-wrap tw-items-end tw-justify-end tw-gap-2.5 max-[1280px]:tw-justify-start">
            <FilterSelect ariaLabel="Chọn làn xe" label="Làn xe" options={laneOptions} value={laneId} onChange={setLaneId} />
            <FilterSelect ariaLabel="Chọn camera" icon="fas fa-video" label="Camera" options={cameraOptions} value={cameraId} onChange={setCameraId} />
          </div>

          <div className="tw-flex tw-h-10 tw-items-center tw-gap-2 tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-emerald-200 tw-bg-emerald-50 tw-px-3 tw-text-[0.84rem] tw-font-extrabold tw-text-emerald-700 tw-shadow-[0_8px_18px_rgba(15,23,42,0.035)]">
            <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-emerald-500" />
            {cameraOptions.find((option) => option.value === cameraId)?.label ?? "CAM-A1-01"} • Đang kết nối
          </div>
        </div>

        <div className="tw-grid tw-min-h-0 tw-grid-cols-[minmax(430px,1.08fr)_minmax(340px,0.88fr)_minmax(360px,0.98fr)] tw-gap-3 max-[1380px]:tw-grid-cols-[minmax(390px,1fr)_minmax(330px,0.9fr)] max-[980px]:tw-grid-cols-1">
          <ParkingCameraPanel />
          <ParkingOperationForm laneId={laneId} mode={mode} onLaneChange={setLaneId} />
          <ParkingSessionSummary mode={mode} />
        </div>
      </section>
    </main>
  );
}
