import { useState } from "react";
import { Drawer } from "@/shared/components/ui/Drawer";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import type { ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";

type CatalogItem = { id: string; name: string; code: string; status: "active" | "inactive" };

type Props = {
  canConfigure: boolean;
  excludedIds: string[];
  items: CatalogItem[];
  kind: "vehicle" | "ticket";
  lots: ParkingLotApiResponse[];
  onLotChange: (parkingLotId: string) => void;
  onSetEnabled: (id: string, enabled: boolean) => Promise<void>;
  selectedLot: ParkingLotApiResponse | null;
};

export function ParkingLotCatalogControls({ canConfigure, excludedIds, items, kind, lots, onLotChange, onSetEnabled, selectedLot }: Props) {
  const [open, setOpen] = useState(false);
  const [busyId, setBusyId] = useState("");
  const [error, setError] = useState("");
  const kindLabel = kind === "vehicle" ? "loại xe" : "loại vé";
  const options = lots.map((lot) => ({ label: lot.name, value: lot.parkingLotId }));

  const handleToggle = async (item: CatalogItem) => {
    setBusyId(item.id);
    setError("");
    try {
      await onSetEnabled(item.id, excludedIds.includes(item.id));
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
        {error ? <p className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-red-700" role="alert">{error}</p> : null}
        <div className="tw-grid tw-gap-2">
          {items.map((item) => {
            const enabled = !excludedIds.includes(item.id);
            return (
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3" key={item.id}>
                <span className="tw-min-w-0"><strong className="tw-block tw-text-vm-slate-900">{item.name}</strong><small className="tw-text-vm-slate-500">{item.code}{item.status === "inactive" ? " · Ngừng dùng toàn Partner" : ""}</small></span>
                <button aria-pressed={enabled} className={enabled ? "tw-rounded-vm-md tw-border-0 tw-bg-green-50 tw-px-3 tw-py-2 tw-font-bold tw-text-green-700" : "tw-rounded-vm-md tw-border-0 tw-bg-vm-slate-100 tw-px-3 tw-py-2 tw-font-bold tw-text-vm-slate-600"} disabled={Boolean(busyId) || item.status === "inactive"} onClick={() => void handleToggle(item)} type="button">
                  {busyId === item.id ? "Đang lưu..." : enabled ? "Đang áp dụng" : "Chưa áp dụng"}
                </button>
              </div>
            );
          })}
        </div>
      </Drawer>
    </>
  );
}
