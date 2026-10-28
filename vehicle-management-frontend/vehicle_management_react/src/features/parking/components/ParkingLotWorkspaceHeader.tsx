import type { ReactNode } from "react";
import { Link } from "react-router-dom";

import type { ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { cn } from "@/lib/cn";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

export type ParkingWorkspaceTab = "overview" | "structure" | "layout" | "live";

type Props = {
  activeTab: ParkingWorkspaceTab;
  parkingLot: ParkingLotApiResponse;
  parkingLots?: ParkingLotApiResponse[];
  onParkingLotChange?: (parkingLotId: string) => void;
  actions?: ReactNode;
};

const statusLabel = {
  SETUP: "Đang thiết lập",
  ACTIVE: "Đang hoạt động",
  MAINTENANCE: "Bảo trì",
  CLOSED: "Đã đóng",
} as const;

const statusClassName = {
  SETUP: "tw-bg-blue-50 tw-text-blue-700",
  ACTIVE: "tw-bg-emerald-50 tw-text-emerald-700",
  MAINTENANCE: "tw-bg-amber-50 tw-text-amber-700",
  CLOSED: "tw-bg-slate-100 tw-text-slate-600",
} as const;

export function parkingWorkspaceHref(tab: ParkingWorkspaceTab, parkingLotId: string) {
  const encodedLotId = encodeURIComponent(parkingLotId);
  if (tab === "structure") return `/admin/parking-structure?lot=${encodedLotId}`;
  if (tab === "layout") return `/admin/parking-layouts?lotId=${encodedLotId}`;
  if (tab === "live") return `/admin/parking-live?lotId=${encodedLotId}`;
  return `/admin/parking-lots?lotId=${encodedLotId}`;
}

export function ParkingLotWorkspaceHeader({ activeTab, parkingLot, parkingLots = [], onParkingLotChange, actions }: Props) {
  const tabs: Array<{ id: ParkingWorkspaceTab; label: string }> = [
    { id: "overview", label: "Tổng quan" },
    { id: "structure", label: "Cấu trúc" },
    { id: "layout", label: "Thiết kế sơ đồ" },
    { id: "live", label: "Trực tiếp" },
  ];

  return (
    <>
      <div className="tw-flex tw-min-h-[86px] tw-items-center tw-justify-between tw-gap-5 max-[850px]:tw-items-start max-[850px]:tw-flex-col">
        <div className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-4">
          <div className="tw-flex tw-size-[62px] tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-solid tw-border-blue-100 tw-bg-gradient-to-br tw-from-blue-50 tw-to-white tw-shadow-sm">
            <span className="tw-flex tw-size-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-vm-primary tw-text-xl tw-font-black tw-text-white tw-shadow-[0_8px_18px_rgba(37,99,235,.24)]">P</span>
          </div>
          <div className="tw-min-w-0 tw-flex-1">
            <div className="tw-flex tw-min-w-0 tw-flex-nowrap tw-items-center tw-gap-3">
              <h1 className="tw-m-0 tw-min-w-0 tw-truncate tw-text-[27px] tw-font-extrabold tw-leading-tight tw-tracking-[-0.025em] tw-text-vm-slate-900" title={`${parkingLot.name} · ${parkingLot.code}`}>
                {parkingLot.name} · {parkingLot.code}
              </h1>
              <span className={cn("tw-inline-flex tw-flex-none tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-full tw-px-3 tw-py-1 tw-text-[0.76rem] tw-font-extrabold", statusClassName[parkingLot.status])}>
                <i className="fas fa-circle tw-text-[0.48rem]" aria-hidden="true" />
                {statusLabel[parkingLot.status]}
              </span>
            </div>
            <p className="tw-m-0 tw-mt-1.5 tw-flex tw-items-center tw-gap-2 tw-truncate tw-text-[0.84rem] tw-font-medium tw-text-vm-slate-500">
              <i className="fas fa-map-marker-alt tw-text-vm-slate-500" aria-hidden="true" />
              {parkingLot.addressDisplay || "Chưa cập nhật địa chỉ"}
            </p>
          </div>
        </div>
        {parkingLots.length > 1 || actions ? (
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-end tw-gap-2.5">
            {parkingLots.length > 1 && onParkingLotChange ? (
              <SelectMenu
                ariaLabel="Chọn bãi xe"
                className="!tw-w-[260px]"
                triggerClassName="!tw-h-10 !tw-rounded-[8px] !tw-border-[#dbe3f0] !tw-text-[0.82rem] !tw-font-bold !tw-shadow-none"
                value={parkingLot.parkingLotId}
                onChange={onParkingLotChange}
                options={parkingLots.map((lot) => ({ label: `${lot.code} — ${lot.name}`, value: lot.parkingLotId }))}
                portal
              />
            ) : null}
            {actions}
          </div>
        ) : null}
      </div>

      <nav className="tw-mt-1 tw-flex tw-h-[52px] tw-items-end tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100" aria-label="Điều hướng quản lý bãi xe">
        {tabs.map((tab) => (
          <Link
            key={tab.id}
            to={parkingWorkspaceHref(tab.id, parkingLot.parkingLotId)}
            className={cn(
              "tw-relative tw-inline-flex tw-h-[52px] tw-items-center tw-px-4 tw-text-[0.9rem] tw-font-bold tw-transition hover:tw-text-vm-primary hover:tw-no-underline",
              activeTab === tab.id ? "tw-text-vm-primary" : "tw-text-vm-slate-500",
            )}
            aria-current={activeTab === tab.id ? "page" : undefined}
          >
            {tab.label}
            {activeTab === tab.id ? <span className="tw-absolute tw-inset-x-0 tw-bottom-[-1px] tw-h-0.5 tw-bg-vm-primary" /> : null}
          </Link>
        ))}
      </nav>
    </>
  );
}
