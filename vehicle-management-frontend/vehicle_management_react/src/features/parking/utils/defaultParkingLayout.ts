import type { LayoutElementApi, LayoutSpaceItemApi } from "@/features/parking/api/parkingLayoutApi";

type DefaultLayout = {
  items: LayoutSpaceItemApi[];
  elements: LayoutElementApi[];
};

function element(
  elementType: LayoutElementApi["elementType"],
  geometry: Record<string, unknown>,
  displayOrder: number,
  label: string | null = null,
  style: Record<string, unknown> | null = null,
): LayoutElementApi {
  return { layoutElementId: null, elementType, geometry, style, label, displayOrder };
}

function row(
  layoutVersionId: string | null,
  startCode: number,
  count: number,
  startX: number,
  y: number,
): LayoutSpaceItemApi[] {
  return Array.from({ length: count }, (_, index) => ({
    layoutItemId: null,
    layoutVersionId,
    parkingSpaceId: null,
    spaceCode: `A-${String(startCode + index).padStart(2, "0")}`,
    vehicleTypeId: null,
    x: startX + index * 62,
    y,
    width: 50,
    height: 100,
    rotation: 0,
  }));
}

export function createDefaultParkingLayout(
  layoutVersionId: string | null,
  options: { spaceCount?: number; vehicleTypeId?: string | null } = {},
): DefaultLayout {
  const spaceCount = Math.max(1, Math.min(28, Math.round(options.spaceCount ?? 28)));
  const items = [
    ...row(layoutVersionId, 1, 14, 50, 70),
    ...row(layoutVersionId, 15, 7, 50, 390),
    ...row(layoutVersionId, 22, 7, 548, 390),
  ].slice(0, spaceCount).map((item) => ({ ...item, vehicleTypeId: options.vehicleTypeId ?? null }));

  const elements: LayoutElementApi[] = [
    element("BACKGROUND", { x: 20, y: 30, width: 960, height: 620 }, 0, "Mặt bằng bãi", {
      fill: "#f8fafc",
      stroke: "#dbe4ef",
    }),
    element("DRIVE_LANE", { x: 40, y: 190, width: 920, height: 180, rotation: 0 }, 1, "Làn lưu thông", {
      fill: "#e5e9ed",
      stroke: "#cbd5e1",
      variant: "ROAD",
    }),
    element("DRIVE_LANE", { x: 239, y: 247, width: 60, height: 28, rotation: 0 }, 2, "Hướng đi", { fill: "#ffffff", variant: "ARROW" }),
    element("DRIVE_LANE", { x: 709, y: 247, width: 60, height: 28, rotation: 180 }, 3, "Hướng đi", { fill: "#ffffff", variant: "ARROW" }),
    element("DRIVE_LANE", { x: 475, y: 302, width: 60, height: 28, rotation: 90 }, 4, "Hướng đi", { fill: "#ffffff", variant: "ARROW" }),
    element("DRIVE_LANE", { x: 468, y: 340, width: 64, height: 24, rotation: 0 }, 5, "Vạch qua đường", { fill: "#ffffff", variant: "CROSSWALK" }),
    element("WALL", { x: 28, y: 40, width: 944, height: 12 }, 10),
    element("WALL", { x: 28, y: 40, width: 12, height: 470 }, 11),
    element("WALL", { x: 960, y: 40, width: 12, height: 470 }, 12),
    element("WALL", { x: 28, y: 510, width: 330, height: 12 }, 13),
    element("WALL", { x: 642, y: 510, width: 330, height: 12 }, 14),
    element("WALL", { x: 358, y: 510, width: 12, height: 70 }, 15),
    element("WALL", { x: 630, y: 510, width: 12, height: 70 }, 16),
    ...[40, 200, 420, 650, 870, 948].map((x, index) =>
      element("COLUMN", { x, y: 170, width: 20, height: 20 }, 30 + index)),
    ...[40, 200, 420, 650, 870, 948].map((x, index) =>
      element("COLUMN", { x, y: 370, width: 20, height: 20 }, 40 + index)),
    element("ENTRY", { x: 390, y: 520, width: 95, height: 75, rotation: 0 }, 60, "LỐI VÀO"),
    element("EXIT", { x: 520, y: 520, width: 95, height: 75, rotation: 0 }, 61, "LỐI RA"),
  ];

  return { items, elements };
}
