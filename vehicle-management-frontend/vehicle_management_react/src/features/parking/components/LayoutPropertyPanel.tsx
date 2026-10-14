import type {
  LayoutElementApi,
  LayoutSpaceItemApi,
} from "@/features/parking/api/parkingLayoutApi";
import { normalizeSpaceCode } from "@/features/parking/utils/layoutValidation";
import type { CanvasSelection } from "@/features/parking/components/LayoutCanvas";
import { SelectMenu } from "@/components/ui";

type PropertyPanelProps = {
  selection: CanvasSelection[];
  items: LayoutSpaceItemApi[];
  elements: LayoutElementApi[];
  vehicleTypes: Array<{ vehicleTypeId: string; code: string; name: string }>;
  onItemsChange: (items: LayoutSpaceItemApi[]) => void;
  onElementsChange: (elements: LayoutElementApi[]) => void;
  onClose?: () => void;
};

function PanelHeader({ onClose, title }: { onClose?: () => void; title: string }) {
  return (
    <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
      <h3 className="tw-m-0 tw-text-[0.95rem] tw-font-black tw-text-slate-900">{title}</h3>
      {onClose ? (
        <button
          type="button"
          aria-label="Đóng thuộc tính"
          title="Đóng"
          onClick={onClose}
          className="tw-flex tw-size-8 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-lg tw-font-bold tw-leading-none tw-text-slate-600 hover:tw-border-blue-300 hover:tw-bg-blue-50 hover:tw-text-blue-700"
        >
          −
        </button>
      ) : null}
    </div>
  );
}

function NumberField({
  label,
  onChange,
  value,
}: {
  label: string;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label className="tw-m-0 tw-grid tw-gap-1">
      <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">{label}</span>
      <input
        className="tw-h-10 tw-rounded-[6px] tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-px-3 tw-text-[0.82rem] tw-font-semibold tw-text-slate-900"
        type="number"
        step="any"
        value={Number.isFinite(value) ? value : 0}
        onChange={(event) => {
          const next = Number(event.target.value);
          if (Number.isFinite(next)) onChange(next);
        }}
      />
    </label>
  );
}

export function LayoutPropertyPanel({
  elements,
  items,
  onElementsChange,
  onItemsChange,
  onClose,
  selection,
  vehicleTypes,
}: PropertyPanelProps) {
  if (selection.length === 0) {
    return (
      <div className="tw-p-4 tw-text-[0.8rem] tw-font-semibold tw-text-slate-500"><PanelHeader title="Thuộc tính" onClose={onClose} />
        <div className="tw-mt-5">
        Chọn một ô đỗ hoặc phần tử để chỉnh thuộc tính.
        </div>
        </div>
    );
  }
  if (selection.length > 1) {
    return (
      <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-700">
        <PanelHeader title="Thuộc tính" onClose={onClose} />
        <p className="tw-mb-0 tw-mt-5">Đang chọn {selection.length} đối tượng. Dùng thanh công cụ để căn chỉnh, nhân bản hoặc xóa hàng loạt.</p>
      </div>
    );
  }

  const entry = selection[0];
  if (entry.kind === "space") {
    const index = items.findIndex(
      (item) => normalizeSpaceCode(item.spaceCode) === normalizeSpaceCode(entry.code),
    );
    if (index < 0) return null;
    const item = items[index];
    function update(patch: Partial<LayoutSpaceItemApi>) {
      onItemsChange(items.map((candidate, position) => (position === index ? { ...candidate, ...patch } : candidate)));
    }
    return (
      <div className="tw-grid tw-gap-4 tw-bg-white tw-p-4">
        <PanelHeader title="Thuộc tính ô đỗ" onClose={onClose} />
        <label className="tw-m-0 tw-grid tw-gap-1">
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Mã ô</span>
          <input
            className="tw-h-9 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.85rem] tw-font-bold tw-uppercase tw-text-vm-slate-900"
            value={item.spaceCode}
            onChange={(event) => update({ spaceCode: event.target.value.toUpperCase() })}
          />
        </label>
        <div className="tw-m-0 tw-grid tw-gap-1">
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Loại xe</span>
          <SelectMenu
            ariaLabel="Loại xe của ô đỗ"
            className="tw-w-full"
            triggerClassName="!tw-h-9 !tw-text-[0.85rem] !tw-shadow-none"
            value={item.vehicleTypeId ?? ""}
            onChange={(value) => update({ vehicleTypeId: value || null })}
            options={[{ value: "", label: "Chung (mọi loại xe)" }, ...vehicleTypes.map((type) => ({ value: type.vehicleTypeId, label: `${type.code} — ${type.name}` }))]}
            clearValue=""
            portal
          />
        </div>
        <div className="tw-grid tw-gap-3">
          <NumberField label="X (px)" value={item.x} onChange={(value) => update({ x: value })} />
          <NumberField label="Y (px)" value={item.y} onChange={(value) => update({ y: value })} />
          <NumberField label="Rộng (px)" value={item.width} onChange={(value) => update({ width: value })} />
          <NumberField label="Cao (px)" value={item.height} onChange={(value) => update({ height: value })} />
        </div>
        <NumberField label="Xoay (°)" value={item.rotation} onChange={(value) => update({ rotation: ((value % 360) + 360) % 360 })} />
      </div>
    );
  }

  const elementIndex = elements.findIndex(
    (element, position) => (element.layoutElementId ?? `local-${position}`) === entry.key,
  );
  if (elementIndex < 0) return null;
  const element = elements[elementIndex];
  const laneVariant = String((element.style as Record<string, unknown> | null)?.["variant"] ?? "");
  const elementTitle = element.elementType === "DRIVE_LANE"
    ? laneVariant === "ARROW"
      ? "Mũi tên chỉ đường"
      : laneVariant === "CROSSWALK"
        ? "Vạch qua đường"
        : "Đường đi"
    : element.elementType;
  function updateElement(patch: Partial<LayoutElementApi>) {
    onElementsChange(elements.map((candidate, position) => (position === elementIndex ? { ...candidate, ...patch } : candidate)));
  }
  function updateGeometry(patch: Record<string, number>) {
    updateElement({ geometry: { ...(element.geometry as Record<string, unknown>), ...patch } });
  }
  const geometry = (element.geometry ?? {}) as Record<string, number>;
  return (
    <div className="tw-grid tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
      <PanelHeader title={elementTitle} onClose={onClose} />
      {(element.elementType === "TEXT" || element.elementType === "LABEL") && (
        <label className="tw-m-0 tw-grid tw-gap-1">
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-600">Nội dung</span>
          <input
            className="tw-h-9 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-2 tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-900"
            value={element.label ?? ""}
            maxLength={200}
            onChange={(event) => updateElement({ label: event.target.value })}
          />
        </label>
      )}
      <div className="tw-grid tw-gap-3">
        <NumberField label="X (px)" value={Number(geometry["x"] ?? 0)} onChange={(value) => updateGeometry({ x: value })} />
        <NumberField label="Y (px)" value={Number(geometry["y"] ?? 0)} onChange={(value) => updateGeometry({ y: value })} />
        <NumberField label="Rộng (px)" value={Number(geometry["width"] ?? 60)} onChange={(value) => updateGeometry({ width: Math.max(10, value) })} />
        <NumberField label="Cao (px)" value={Number(geometry["height"] ?? 30)} onChange={(value) => updateGeometry({ height: Math.max(10, value) })} />
        <NumberField label="Xoay (°)" value={Number(geometry["rotation"] ?? 0)} onChange={(value) => updateGeometry({ rotation: ((value % 360) + 360) % 360 })} />
      </div>
      <NumberField
        label="Thứ tự lớp"
        value={element.displayOrder ?? 0}
        onChange={(value) => updateElement({ displayOrder: Math.max(0, Math.round(value)) })}
      />
    </div>
  );
}
