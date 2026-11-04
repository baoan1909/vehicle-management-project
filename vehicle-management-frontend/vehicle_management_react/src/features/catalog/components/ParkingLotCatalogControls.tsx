import { useState } from "react";
import { Drawer } from "@/shared/components/ui/Drawer";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import type { ParkingLotCatalogAvailability } from "@/features/catalog/api/parkingLotCatalogAvailabilityApi";
import type { ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";

type CatalogItem = { id: string; name: string; code: string; status: "active" | "inactive" };

type Props = {
  canConfigureAllLots?: boolean;
  canConfigure: boolean;
  availabilities?: ParkingLotCatalogAvailability[];
  drainingIds?: string[];
  excludedIds: string[];
  items: CatalogItem[];
  kind: "vehicle" | "ticket";
  lots: ParkingLotApiResponse[];
  onLotChange: (parkingLotId: string) => void;
  onSetEnabled: (id: string, enabled: boolean) => Promise<void>;
  onSetEnabledForAllLots?: (id: string, enabled: boolean) => Promise<void>;
  selectedLot: ParkingLotApiResponse | null;
};

export function ParkingLotCatalogControls({ availabilities = [], canConfigureAllLots = false, canConfigure, drainingIds = [], excludedIds, items, kind, lots, onLotChange, onSetEnabled, onSetEnabledForAllLots, selectedLot }: Props) {
  const [open, setOpen] = useState(false);
  const [applyScope, setApplyScope] = useState<"selected" | "all">("selected");
  const [busyId, setBusyId] = useState("");
  const [error, setError] = useState("");
  const kindLabel = kind === "vehicle" ? "loại xe" : "loại vé";
  const options = lots.map((lot) => ({ label: lot.name, value: lot.parkingLotId }));
  const appliesToAllLots = kind === "vehicle" && canConfigureAllLots && applyScope === "all";

  const getItemScopeState = (itemId: string) => {
    if (!appliesToAllLots) {
      return {
        draining: drainingIds.includes(itemId),
        enabled: !excludedIds.includes(itemId),
        partial: false,
      };
    }
    const scopedAvailabilities = lots
      .map((lot) => availabilities.find((availability) => availability.parkingLotId === lot.parkingLotId))
      .filter((availability): availability is ParkingLotCatalogAvailability => Boolean(availability));
    if (scopedAvailabilities.length === 0) {
      return { draining: false, enabled: true, partial: false };
    }
    const excludedCount = scopedAvailabilities.filter(
      (availability) => availability.excludedVehicleTypeIds.includes(itemId),
    ).length;
    return {
      draining: scopedAvailabilities.some((availability) => availability.drainingVehicleTypeIds.includes(itemId)),
      enabled: excludedCount < scopedAvailabilities.length,
      partial: excludedCount > 0 && excludedCount < scopedAvailabilities.length,
    };
  };

  const handleToggle = async (item: CatalogItem, currentlyEnabled: boolean) => {
    if (currentlyEnabled && !window.confirm(
      appliesToAllLots
        ? `Ngừng phục vụ ${item.name} tại tất cả ${lots.length} bãi? Đăng ký mới và khách vãng lai sẽ bị chặn; vé còn hiệu lực và phiên đang gửi vẫn được phục vụ đến khi kết thúc.`
        : `Ngừng phục vụ ${item.name} tại ${selectedLot?.name ?? "bãi xe này"}? Đăng ký mới và khách vãng lai sẽ bị chặn; vé còn hiệu lực và phiên đang gửi vẫn được phục vụ đến khi kết thúc.`,
    )) return;
    setBusyId(item.id);
    setError("");
    try {
      if (appliesToAllLots && onSetEnabledForAllLots) {
        await onSetEnabledForAllLots(item.id, !currentlyEnabled);
      } else {
        await onSetEnabled(item.id, !currentlyEnabled);
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : `Không thể cập nhật ${kindLabel}.`);
    } finally {
      setBusyId("");
    }
  };

  return (
    <>
      <div className="tw-flex tw-flex-wrap tw-items-end tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-4">
        <label className="tw-m-0 tw-grid tw-w-[min(100%,360px)] tw-gap-1.5">
          <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Bãi xe đang xem</span>
          <SelectMenu ariaLabel="Chọn bãi xe để xem danh mục" options={options} value={selectedLot?.parkingLotId ?? ""} onChange={onLotChange} disabled={lots.length === 0} />
        </label>
        {canConfigure && selectedLot ? (
          <button className="tw-min-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-white tw-px-4 tw-font-bold tw-text-vm-primary hover:tw-bg-blue-50" type="button" onClick={() => setOpen(true)}>
            <i className="fas fa-sliders-h tw-mr-2" />Cấu hình {kindLabel} áp dụng
          </button>
        ) : null}
      </div>
      <Drawer open={open && Boolean(selectedLot)} onClose={() => setOpen(false)} title={`Cấu hình ${kindLabel} tại ${selectedLot?.name ?? "bãi xe"}`} description={`Bật những ${kindLabel} được phục vụ tại bãi này. Loại mới mặc định áp dụng cho các bãi của Partner cho đến khi được tắt riêng.`}>
        {kind === "vehicle" ? (
          <div className="tw-grid tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
            <strong className="tw-text-sm tw-text-vm-slate-800">Phạm vi áp dụng</strong>
            {canConfigureAllLots ? (
              <div className="tw-grid tw-grid-cols-2 tw-gap-2" role="group" aria-label="Chọn phạm vi bãi áp dụng">
                <button aria-pressed={applyScope === "selected"} className={applyScope === "selected" ? "tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-blue-50 tw-px-3 tw-py-2 tw-font-bold tw-text-vm-primary" : "tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-bg-white tw-px-3 tw-py-2 tw-font-bold tw-text-vm-slate-600"} onClick={() => setApplyScope("selected")} type="button">
                  Bãi đang chọn
                </button>
                <button aria-pressed={applyScope === "all"} className={applyScope === "all" ? "tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-primary tw-bg-blue-50 tw-px-3 tw-py-2 tw-font-bold tw-text-vm-primary" : "tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-bg-white tw-px-3 tw-py-2 tw-font-bold tw-text-vm-slate-600"} onClick={() => setApplyScope("all")} type="button">
                  Tất cả bãi thuộc Partner
                </button>
              </div>
            ) : (
              <span className="tw-text-sm tw-text-vm-slate-600">Bãi được phân công đang chọn: <strong>{selectedLot?.name}</strong></span>
            )}
            <small className="tw-text-vm-slate-500">{appliesToAllLots ? `Thao tác sẽ áp dụng đồng thời cho ${lots.length} bãi trong phạm vi Partner.` : `Thao tác chỉ áp dụng tại ${selectedLot?.name ?? "bãi đang chọn"}.`}</small>
          </div>
        ) : null}
        {error ? <p className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-red-700" role="alert">{error}</p> : null}
        <div className="tw-grid tw-gap-2">
          {items.map((item) => {
            const { draining, enabled, partial } = getItemScopeState(item.id);
            return (
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3" key={item.id}>
                <span className="tw-min-w-0"><strong className="tw-block tw-text-vm-slate-900">{item.name}</strong><small className="tw-text-vm-slate-500">{item.code}{item.status === "inactive" ? " · Ngừng dùng toàn Partner" : draining ? " · Đang phục vụ nốt vé/phiên hiện hữu" : ""}</small></span>
                <span className="tw-flex tw-shrink-0 tw-flex-wrap tw-justify-end tw-gap-2">
                  <button aria-pressed={enabled} className={partial || draining ? "tw-rounded-vm-md tw-border-0 tw-bg-amber-50 tw-px-3 tw-py-2 tw-font-bold tw-text-amber-700" : enabled ? "tw-rounded-vm-md tw-border-0 tw-bg-green-50 tw-px-3 tw-py-2 tw-font-bold tw-text-green-700" : "tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-100 tw-px-3 tw-py-2 tw-font-bold tw-text-vm-slate-600"} disabled={Boolean(busyId) || item.status === "inactive"} onClick={() => void handleToggle(item, enabled)} type="button">
                    {busyId === item.id ? "Đang lưu..." : partial ? "Áp dụng một phần" : enabled ? "Đang áp dụng" : draining ? "Đang ngừng dần" : "Đã ngừng"}
                  </button>
                </span>
              </div>
            );
          })}
        </div>
      </Drawer>
    </>
  );
}
