import { SelectMenu } from "@/shared/components/ui/SelectMenu";

interface CardToolbarProps {
  cardTypeValue: string;
  vehicleTypeValue: string;
  inventoryStatusValue: string;
  subscriptionStatusValue: string;
  lostStatusValue: string;
  searchValue: string;
  onCardTypeChange: (value: string) => void;
  onInventoryStatusChange: (value: string) => void;
  onLostStatusChange: (value: string) => void;
  onReset: () => void;
  onSearchChange: (value: string) => void;
  onSubscriptionStatusChange: (value: string) => void;
  onVehicleTypeChange: (value: string) => void;
}

export function CardToolbar({
  cardTypeValue,
  vehicleTypeValue,
  inventoryStatusValue,
  subscriptionStatusValue,
  lostStatusValue,
  searchValue,
  onCardTypeChange,
  onInventoryStatusChange,
  onLostStatusChange,
  onReset,
  onSearchChange,
  onSubscriptionStatusChange,
  onVehicleTypeChange
}: CardToolbarProps) {
  return (
    <div className="tw-flex tw-flex-col tw-items-stretch tw-gap-[0.85rem] tw-p-[1.1rem]">
      <div className="tw-flex tw-w-full tw-items-stretch tw-gap-3">
        <label className="tw-m-0 tw-flex tw-min-h-10 tw-w-[min(100%,760px)] tw-flex-[0_1_760px] tw-items-center tw-gap-[0.7rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-[0.95rem] tw-text-vm-slate-500">
          <i className="fas fa-search" />
          <input
            className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.94rem] tw-text-[#111827] tw-outline-none placeholder:tw-text-vm-slate-500"
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Mã thẻ, UID, biển số, khách hàng..."
            type="search"
            value={searchValue}
          />
        </label>
      </div>

      <div className="tw-flex tw-flex-col tw-items-start tw-gap-[0.85rem]">
        <div className="tw-grid tw-w-auto tw-max-w-full tw-grid-cols-[repeat(4,minmax(140px,168px))] tw-gap-3 max-[900px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
          <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
            <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Loại thẻ</span>
            <SelectMenu
              ariaLabel="Loại thẻ"
              value={cardTypeValue}
              onChange={onCardTypeChange}
              options={[
                { label: "Tất cả", value: "all" },
                { label: "Đăng ký", value: "Đăng ký" },
                { label: "Vãng lai", value: "Vãng lai" }
              ]}
            />
          </label>

          <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
            <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Loại xe</span>
            <SelectMenu
              ariaLabel="Loại xe"
              value={vehicleTypeValue}
              onChange={onVehicleTypeChange}
              options={[
                { label: "Tất cả", value: "all" },
                { label: "Xe máy", value: "Xe máy" },
                { label: "Xe ô tô", value: "Xe ô tô" },
                { label: "Xe khác", value: "Xe khác" }
              ]}
            />
          </label>

          <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
            <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Trạng thái</span>
            <SelectMenu
              ariaLabel="Trạng thái"
              value={inventoryStatusValue}
              onChange={onInventoryStatusChange}
              options={[
                { label: "Tất cả", value: "all" },
                { label: "Sẵn sàng", value: "available" },
                { label: "Đã gán", value: "assigned" },
                { label: "Trong bãi", value: "in_use" },
                { label: "Chờ xử lý", value: "pending" },
                { label: "Mất thẻ", value: "lost" },
                { label: "Khóa", value: "blocked" }
              ]}
            />
          </label>

          <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
            <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Vé tháng</span>
            <SelectMenu
              ariaLabel="Vé tháng"
              value={subscriptionStatusValue}
              onChange={onSubscriptionStatusChange}
              options={[
                { label: "Tất cả", value: "all" },
                { label: "Đang hiệu lực", value: "active" },
                { label: "Chờ duyệt", value: "pending" },
                { label: "Hết hạn", value: "expired" },
                { label: "Không", value: "none" }
              ]}
            />
          </label>
        </div>

        <div className="tw-grid tw-w-auto tw-max-w-full tw-grid-cols-[minmax(140px,168px)_auto] tw-items-end tw-justify-start tw-gap-3 max-[640px]:tw-grid-cols-1">
          <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
            <span className="tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-500">Báo mất</span>
            <SelectMenu
              ariaLabel="Báo mất"
              value={lostStatusValue}
              onChange={onLostStatusChange}
              options={[
                { label: "Tất cả", value: "all" },
                { label: "Mở", value: "open" },
                { label: "Không", value: "none" }
              ]}
            />
          </label>

          <button
            className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-[0.7rem] tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-transition-colors hover:tw-bg-vm-slate-25"
            onClick={onReset}
            type="button"
          >
            <i className="fas fa-redo-alt" />
            <span>Xóa bộ lọc</span>
          </button>
        </div>
      </div>
    </div>
  );
}
