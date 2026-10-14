import { useMemo } from "react";

import {
  LayoutCanvas,
  type CanvasSelection,
  type LayoutSpaceVisualState,
} from "@/features/parking/components/LayoutCanvas";
import type {
  LayoutElementApi,
  LayoutSpaceItemApi,
  LevelMapApi,
  ParkingSpaceApi,
} from "@/features/parking/api/parkingLayoutApi";
import { normalizeSpaceCode } from "@/features/parking/utils/layoutValidation";
import { cn } from "@/lib/cn";

export type LiveMapFilters = {
  status: string;
  vehicleTypeId: string;
  zoneId: string;
  occupancy: string;
  allocation: string;
  keyword: string;
};

const STATUS_META: Record<ParkingSpaceApi["status"], {
  label: string;
  fill: string;
  stroke: string;
  glyph: string;
}> = {
  AVAILABLE: { label: "Còn trống", fill: "#dcfce7", stroke: "#16a34a", glyph: "✓" },
  OCCUPIED: { label: "Đang có xe", fill: "#fee2e2", stroke: "#dc2626", glyph: "●" },
  RESERVED: { label: "Đặt trước", fill: "#fef3c7", stroke: "#d97706", glyph: "◐" },
  MAINTENANCE: { label: "Bảo trì", fill: "#e2e8f0", stroke: "#64748b", glyph: "■" },
};

function occupancyOf(space: ParkingSpaceApi) {
  return space.manualOccupancyType === "VISITOR" || space.manualOccupancyType === "REGISTERED"
    ? space.manualOccupancyType
    : "UNKNOWN";
}

export function LiveOpsMap({
  allocatedSpaceIds,
  canvasHeight,
  canvasWidth,
  elements,
  filters,
  items,
  levelMap,
  onSelectSpace,
  selectedSpaceId,
  vehicleTypes,
}: {
  levelMap: LevelMapApi;
  items: LayoutSpaceItemApi[];
  elements: LayoutElementApi[];
  allocatedSpaceIds: Set<string>;
  filters: LiveMapFilters;
  canvasWidth: number;
  canvasHeight: number;
  selectedSpaceId: string | null;
  onSelectSpace: (space: ParkingSpaceApi | null) => void;
  vehicleTypes: Array<{ vehicleTypeId: string; code: string; name: string }>;
}) {
  const visibleSpaces = useMemo(() => {
    const keyword = normalizeSpaceCode(filters.keyword);
    return levelMap.spaces.filter((space) => {
      if (filters.status !== "all" && space.status !== filters.status) return false;
      if (filters.vehicleTypeId !== "all" && (space.vehicleTypeId ?? "") !== filters.vehicleTypeId) return false;
      if (filters.zoneId !== "all" && space.zoneId !== filters.zoneId) return false;
      if (filters.occupancy !== "all" && occupancyOf(space) !== filters.occupancy) return false;
      if (filters.allocation === "allocated" && !allocatedSpaceIds.has(space.parkingSpaceId)) return false;
      if (filters.allocation === "unallocated" && allocatedSpaceIds.has(space.parkingSpaceId)) return false;
      return !keyword || normalizeSpaceCode(space.code).includes(keyword);
    });
  }, [allocatedSpaceIds, filters, levelMap.spaces]);

  const visibleSpaceIds = useMemo(
    () => new Set(visibleSpaces.map((space) => space.parkingSpaceId)),
    [visibleSpaces],
  );

  const visibleCodes = useMemo(
    () => new Set(visibleSpaces.map((space) => normalizeSpaceCode(space.code))),
    [visibleSpaces],
  );

  const visibleItems = useMemo(
    () => items.filter((item) => (
      item.parkingSpaceId
        ? visibleSpaceIds.has(item.parkingSpaceId)
        : visibleCodes.has(normalizeSpaceCode(item.spaceCode))
    )),
    [items, visibleCodes, visibleSpaceIds],
  );

  const spaceVisuals = useMemo(() => {
    const values = new Map<string, LayoutSpaceVisualState>();
    for (const space of levelMap.spaces) {
      values.set(space.parkingSpaceId, {
        status: space.status,
        allocated: allocatedSpaceIds.has(space.parkingSpaceId),
      });
    }
    return values;
  }, [allocatedSpaceIds, levelMap.spaces]);

  const selectedSpace = selectedSpaceId
    ? levelMap.spaces.find((space) => space.parkingSpaceId === selectedSpaceId) ?? null
    : null;

  const selection: CanvasSelection[] = selectedSpace
    ? [{ kind: "space", code: selectedSpace.code }]
    : [];

  function handleSelection(next: CanvasSelection[]) {
    const selected = [...next].reverse().find((entry) => entry.kind === "space");
    if (!selected || selected.kind !== "space") {
      onSelectSpace(null);
      return;
    }
    const normalizedCode = normalizeSpaceCode(selected.code);
    const selectedItem = visibleItems.find(
      (item) => normalizeSpaceCode(item.spaceCode) === normalizedCode,
    );
    const space = selectedItem?.parkingSpaceId
      ? levelMap.spaces.find((candidate) => candidate.parkingSpaceId === selectedItem.parkingSpaceId)
      : levelMap.spaces.find((candidate) => normalizeSpaceCode(candidate.code) === normalizedCode);
    onSelectSpace(space ?? null);
  }

  return (
    <div className="tw-flex tw-min-h-0 tw-flex-col tw-gap-3">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-3" aria-label="Chú thích trạng thái ô đỗ">
        <span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">
          Hiển thị {visibleSpaces.length}/{levelMap.spaces.length} ô
        </span>
        <span className="tw-ml-auto tw-flex tw-flex-wrap tw-gap-3">
          {Object.entries(STATUS_META).map(([status, meta]) => (
            <span key={status} className="tw-inline-flex tw-items-center tw-gap-1.5 tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-600">
              <span
                className={cn("tw-inline-flex tw-h-4 tw-w-4 tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid")}
                style={{ backgroundColor: meta.fill, borderColor: meta.stroke, color: meta.stroke }}
                aria-hidden="true"
              >
                {meta.glyph}
              </span>
              {meta.label}
            </span>
          ))}
          <span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-text-[0.72rem] tw-font-bold tw-text-vm-slate-600">
            <span className="tw-h-4 tw-w-4 tw-rounded tw-border-2 tw-border-dashed tw-border-purple-500 tw-bg-white" aria-hidden="true" />
            Đã phân bổ
          </span>
        </span>
      </div>

      <LayoutCanvas
        canvasHeight={canvasHeight}
        canvasWidth={canvasWidth}
        elements={elements}
        items={visibleItems}
        onElementsChange={() => undefined}
        onItemsChange={() => undefined}
        onSelect={handleSelection}
        readOnly
        selected={selection}
        spaceVisuals={spaceVisuals}
        vehicleTypes={vehicleTypes}
      />
    </div>
  );
}
