import { FilterToolbar } from "@/shared/components/ui/FilterToolbar";
import { SelectMenu, type SelectMenuOption } from "@/shared/components/ui/SelectMenu";

interface CardToolbarProps {
  cardTypeOptions: SelectMenuOption[];
  cardTypeValue: string;
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
}

export function CardToolbar({
  cardTypeOptions,
  cardTypeValue,
  inventoryStatusValue,
  subscriptionStatusValue,
  lostStatusValue,
  searchValue,
  onCardTypeChange,
  onInventoryStatusChange,
  onLostStatusChange,
  onReset,
  onSearchChange,
  onSubscriptionStatusChange
}: CardToolbarProps) {
  return (
    <FilterToolbar
      className="tw-grid tw-grid-cols-[minmax(280px,1fr)_repeat(4,minmax(150px,174px))_auto] tw-items-end tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 max-[1320px]:tw-grid-cols-3 max-[820px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1"
      onReset={onReset}
      onSearchChange={onSearchChange}
      searchPlaceholder="Mã thẻ, UID, biển số, khách hàng..."
      searchValue={searchValue}
    >
      <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
        <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Loại thẻ</span>
        <SelectMenu ariaLabel="Loại thẻ" value={cardTypeValue} onChange={onCardTypeChange} options={cardTypeOptions} />
      </label>

      <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
        <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Trạng thái</span>
        <SelectMenu
          ariaLabel="Trạng thái"
          value={inventoryStatusValue}
          onChange={onInventoryStatusChange}
          options={[
            { label: "Tất cả", value: "all" },
            { label: "Sẵn sàng", value: "available" },
            { label: "Đã gán", value: "assigned" },
            { label: "Trong bãi", value: "in_use" },
            { label: "Đã giữ", value: "reserved" },
            { label: "Mất thẻ", value: "lost" },
            { label: "Khóa", value: "blocked" },
            { label: "Hỏng", value: "damaged" },
            { label: "Ngừng dùng", value: "retired" },
          ]}
        />
      </label>

      <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
        <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Vé tháng</span>
        <SelectMenu
          ariaLabel="Vé tháng"
          value={subscriptionStatusValue}
          onChange={onSubscriptionStatusChange}
          options={[
            { label: "Tất cả", value: "all" },
            { label: "Đang hiệu lực", value: "active" },
            { label: "Chờ duyệt", value: "pending" },
            { label: "Hết hạn", value: "expired" },
            { label: "Không", value: "none" },
          ]}
        />
      </label>

      <label className="tw-m-0 tw-grid tw-gap-[0.35rem]">
        <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">Báo mất</span>
        <SelectMenu
          ariaLabel="Báo mất"
          value={lostStatusValue}
          onChange={onLostStatusChange}
          options={[
            { label: "Tất cả", value: "all" },
            { label: "Mở", value: "open" },
            { label: "Không", value: "none" },
          ]}
        />
      </label>
    </FilterToolbar>
  );
}
