import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import type {
  LayoutElementApi,
  LayoutSpaceItemApi,
} from "@/features/parking/api/parkingLayoutApi";
import { normalizeSpaceCode } from "@/features/parking/utils/layoutValidation";
import { cn } from "@/lib/cn";

export type CanvasSelection =
  | { kind: "space"; code: string }
  | { kind: "element"; key: string };

export type LayoutPaletteTool =
  | "SPACE"
  | LayoutElementApi["elementType"]
  | "LANE_ROAD"
  | "LANE_ARROW"
  | "LANE_CROSSWALK";

export function spaceKey(code: string) {
  return `space:${normalizeSpaceCode(code)}`;
}

export function elementKey(element: LayoutElementApi, index: number) {
  return `element:${element.layoutElementId ?? `local-${index}`}`;
}

type CanvasProps = {
  items: LayoutSpaceItemApi[];
  elements: LayoutElementApi[];
  canvasWidth: number;
  canvasHeight: number;
  selected: CanvasSelection[];
  onSelect: (selection: CanvasSelection[], options?: { openProperties?: boolean }) => void;
  onItemsChange: (items: LayoutSpaceItemApi[]) => void;
  onElementsChange: (elements: LayoutElementApi[]) => void;
  snapGrid?: number;
  readOnly?: boolean;
  toolbarTarget?: Element | null;
  onPaletteDrop?: (
    tool: LayoutPaletteTool,
    position: { x: number; y: number },
  ) => void;
  vehicleTypes?: Array<{ vehicleTypeId: string; code: string; name: string }>;
  onHistoryCheckpoint?: () => void;
  onUndo?: () => void;
  onRedo?: () => void;
  canUndo?: boolean;
  canRedo?: boolean;
  spaceVisuals?: ReadonlyMap<string, LayoutSpaceVisualState>;
};

export type LayoutSpaceVisualState = {
  status: "AVAILABLE" | "OCCUPIED" | "RESERVED" | "MAINTENANCE";
  allocated?: boolean;
};

const LIVE_SPACE_VISUALS: Record<LayoutSpaceVisualState["status"], {
  fill: string;
  stroke: string;
  glyph: string;
  label: string;
}> = {
  AVAILABLE: { fill: "#dcfce7", stroke: "#16a34a", glyph: "✓", label: "Còn trống" },
  OCCUPIED: { fill: "#fee2e2", stroke: "#dc2626", glyph: "●", label: "Đang có xe" },
  RESERVED: { fill: "#fef3c7", stroke: "#d97706", glyph: "◐", label: "Đặt trước" },
  MAINTENANCE: { fill: "#e2e8f0", stroke: "#64748b", glyph: "■", label: "Bảo trì" },
};

const ALLOCATED_SPACE_VISUAL = {
  fill: "#ede9fe",
  stroke: "#7c3aed",
  glyph: "◆",
  label: "Đã phân bổ",
};

type DragState =
  | { mode: "move"; startX: number; startY: number; origins: Map<string, { x: number; y: number }> }
  | { mode: "resize"; code: string; handle: SpaceResizeHandle; startX: number; startY: number; origin: LayoutSpaceItemApi }
  | { mode: "rotate"; code: string; centerX: number; centerY: number }
  | { mode: "resize-element"; key: string; handle: ResizeHandle; startX: number; startY: number; origin: { x: number; y: number; width: number; height: number; rotation: number } }
  | { mode: "rotate-element"; key: string; centerX: number; centerY: number }
  | { mode: "pan"; startClientX: number; startClientY: number; originX: number; originY: number }
  | null;

const MIN_SIZE = 10;
const SPACE_MIN_WIDTH = 30;
const SPACE_MIN_HEIGHT = 50;
type ResizeHandle = "nw" | "n" | "ne" | "e" | "se" | "s" | "sw" | "w";
type SpaceResizeHandle = ResizeHandle;

function resizeHandles(x: number, y: number, width: number, height: number): Array<{
  handle: ResizeHandle;
  x: number;
  y: number;
  cursor: string;
}> {
  const centerX = x + width / 2;
  const centerY = y + height / 2;
  return [
    { handle: "nw", x, y, cursor: "nwse-resize" },
    { handle: "n", x: centerX, y, cursor: "ns-resize" },
    { handle: "ne", x: x + width, y, cursor: "nesw-resize" },
    { handle: "e", x: x + width, y: centerY, cursor: "ew-resize" },
    { handle: "se", x: x + width, y: y + height, cursor: "nwse-resize" },
    { handle: "s", x: centerX, y: y + height, cursor: "ns-resize" },
    { handle: "sw", x, y: y + height, cursor: "nesw-resize" },
    { handle: "w", x, y: centerY, cursor: "ew-resize" },
  ];
}

function toLocal(svg: SVGSVGElement, clientX: number, clientY: number) {
  const point = new DOMPoint(clientX, clientY);
  const matrix = svg.getScreenCTM();
  if (!matrix) return { x: 0, y: 0 };
  const local = point.matrixTransform(matrix.inverse());
  return { x: local.x, y: local.y };
}

function toCanvas(
  svg: SVGSVGElement,
  clientX: number,
  clientY: number,
  zoom: number,
  pan: { x: number; y: number },
) {
  const local = toLocal(svg, clientX, clientY);
  return {
    x: local.x / zoom - pan.x,
    y: local.y / zoom - pan.y,
  };
}

function elementPosition(element: LayoutElementApi): { x: number; y: number } | null {
  const geometry = element.geometry as Record<string, unknown>;
  const x = Number(geometry?.["x"]);
  const y = Number(geometry?.["y"]);
  if (!Number.isFinite(x) || !Number.isFinite(y)) return null;
  return { x, y };
}

function elementSize(element: LayoutElementApi): { width: number; height: number } {
  const geometry = element.geometry as Record<string, unknown>;
  const width = Number(geometry?.["width"]);
  const height = Number(geometry?.["height"]);
  return {
    width: Number.isFinite(width) && width > 0 ? width : 60,
    height: Number.isFinite(height) && height > 0 ? height : 30,
  };
}

export function LayoutCanvas({
  canvasHeight,
  canvasWidth,
  elements,
  items,
  onElementsChange,
  onItemsChange,
  onSelect,
  selected,
  snapGrid = 5,
  readOnly = false,
  toolbarTarget,
  onPaletteDrop,
  vehicleTypes = [],
  onHistoryCheckpoint,
  onUndo,
  onRedo,
  canUndo = false,
  canRedo = false,
  spaceVisuals,
}: CanvasProps) {
  const svgRef = useRef<SVGSVGElement>(null);
  const viewportRef = useRef<HTMLDivElement>(null);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [showGrid, setShowGrid] = useState(true);
  const [snap, setSnap] = useState(true);
  const [clipboard, setClipboard] = useState<LayoutSpaceItemApi[]>([]);
  const dragRef = useRef<DragState>(null);
  const spacePressedRef = useRef(false);

  const selectedKeys = useMemo(
    () => new Set(selected.map((entry) => (entry.kind === "space" ? spaceKey(entry.code) : `element:${entry.key}`))),
    [selected],
  );

  const snapValue = useCallback(
    (value: number) => {
      if (!snap || snapGrid <= 0) return Math.round(value * 100) / 100;
      return Math.round(value / snapGrid) * snapGrid;
    },
    [snap, snapGrid],
  );

  function resetViewportScroll() {
    window.requestAnimationFrame(() => {
      viewportRef.current?.scrollTo({ left: 0, top: 0, behavior: "auto" });
    });
  }

  function fitToScreen() {
    const container = svgRef.current?.parentElement;
    if (!container) return;
    const rect = container.getBoundingClientRect();
    const next = Math.min((rect.width - 48) / canvasWidth, (rect.height - 48) / canvasHeight, 2);
    setZoom(Math.max(0.2, next));
    setPan({ x: 0, y: 0 });
    resetViewportScroll();
  }

  function constrainPan(nextPan: { x: number; y: number }, atZoom = zoom) {
    const viewport = viewportRef.current;
    if (!viewport) {
      return { x: Math.min(0, nextPan.x), y: Math.min(0, nextPan.y) };
    }
    const visibleWidth = viewport.clientWidth / atZoom;
    const visibleHeight = viewport.clientHeight / atZoom;
    const minX = Math.min(0, visibleWidth - canvasWidth);
    const minY = Math.min(0, visibleHeight - canvasHeight);
    const maxX = Math.max(0, visibleWidth - canvasWidth);
    const maxY = Math.max(0, visibleHeight - canvasHeight);
    return {
      x: Math.min(maxX, Math.max(minX, nextPan.x)),
      y: Math.min(maxY, Math.max(minY, nextPan.y)),
    };
  }

  function changeZoom(nextZoom: number) {
    const constrainedZoom = Math.min(3, Math.max(0.2, nextZoom));
    setZoom(constrainedZoom);
    setPan((current) => constrainPan(current, constrainedZoom));
  }

  function centerDiagram() {
    const bounds = [
      ...items.map((item) => ({ x: item.x, y: item.y, width: item.width, height: item.height })),
      ...elements.flatMap((element) => {
        const position = elementPosition(element);
        if (!position) return [];
        return [{ ...position, ...elementSize(element) }];
      }),
    ];
    if (bounds.length === 0) {
      setZoom(1);
      setPan({ x: 0, y: 0 });
      resetViewportScroll();
      return;
    }
    const minX = Math.min(...bounds.map((bound) => bound.x));
    const minY = Math.min(...bounds.map((bound) => bound.y));
    const maxX = Math.max(...bounds.map((bound) => bound.x + bound.width));
    const maxY = Math.max(...bounds.map((bound) => bound.y + bound.height));
    const viewport = viewportRef.current;
    if (!viewport) return;
    const padding = 48;
    const contentWidth = Math.max(1, maxX - minX);
    const contentHeight = Math.max(1, maxY - minY);
    const targetZoom = Math.min(
      2,
      Math.max(
        0.2,
        Math.min(
          Math.max(1, viewport.clientWidth - padding) / contentWidth,
          Math.max(1, viewport.clientHeight - padding) / contentHeight,
        ),
      ),
    );
    setZoom(targetZoom);
    setPan(constrainPan({
      x: viewport.clientWidth / (2 * targetZoom) - (minX + maxX) / 2,
      y: viewport.clientHeight / (2 * targetZoom) - (minY + maxY) / 2,
    }, targetZoom));
    resetViewportScroll();
  }

  useEffect(() => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  }, [canvasWidth, canvasHeight]);

  useEffect(() => {
    function handleSpaceDown(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      if (target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.tagName === "SELECT")) return;
      if (event.code === "Space") {
        spacePressedRef.current = true;
        event.preventDefault();
      }
    }
    function handleSpaceUp(event: KeyboardEvent) {
      if (event.code === "Space") spacePressedRef.current = false;
    }
    function clearSpace() {
      spacePressedRef.current = false;
    }
    window.addEventListener("keydown", handleSpaceDown);
    window.addEventListener("keyup", handleSpaceUp);
    window.addEventListener("blur", clearSpace);
    return () => {
      window.removeEventListener("keydown", handleSpaceDown);
      window.removeEventListener("keyup", handleSpaceUp);
      window.removeEventListener("blur", clearSpace);
    };
  }, []);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const listener = (event: WheelEvent) => handleWheel(event);
    viewport.addEventListener("wheel", listener, { passive: false });
    return () => viewport.removeEventListener("wheel", listener);
  }, [zoom, pan]);

  function toggleSelection(entry: CanvasSelection, additive: boolean) {
    if (!additive) {
      onSelect([entry], { openProperties: true });
      return;
    }
    const key = entry.kind === "space" ? spaceKey(entry.code) : `element:${entry.key}`;
    const exists = selectedKeys.has(key);
    if (exists) {
      onSelect(
        selected.filter(
          (current) =>
            (current.kind === "space" ? spaceKey(current.code) : `element:${current.key}`) !== key,
        ),
        { openProperties: true },
      );
    } else {
      onSelect([...selected, entry], { openProperties: true });
    }
  }

  function handleBackgroundPointerDown(event: React.PointerEvent) {
    if (event.button === 1 || event.shiftKey || (event.button === 0 && spacePressedRef.current)) {
      const svg = svgRef.current;
      if (!svg) return;
      dragRef.current = {
        mode: "pan",
        startClientX: event.clientX,
        startClientY: event.clientY,
        originX: pan.x,
        originY: pan.y,
      };
      (event.target as Element).setPointerCapture?.(event.pointerId);
      return;
    }
    onSelect([]);
  }

  function handleSpacePointerDown(event: React.PointerEvent, item: LayoutSpaceItemApi) {
    event.stopPropagation();
    if (event.button === 0 && spacePressedRef.current) {
      dragRef.current = {
        mode: "pan",
        startClientX: event.clientX,
        startClientY: event.clientY,
        originX: pan.x,
        originY: pan.y,
      };
      (event.target as Element).setPointerCapture?.(event.pointerId);
      return;
    }
    toggleSelection({ kind: "space", code: item.spaceCode }, event.shiftKey);
    if (readOnly) return;
    onHistoryCheckpoint?.();
    const svg = svgRef.current;
    if (!svg) return;
    const local = toCanvas(svg, event.clientX, event.clientY, zoom, pan);
    const codes = event.shiftKey
      ? [...selected.filter((entry) => entry.kind === "space").map((entry) => normalizeSpaceCode(entry.code)), normalizeSpaceCode(item.spaceCode)]
      : [normalizeSpaceCode(item.spaceCode)];
    const origins = new Map<string, { x: number; y: number }>();
    for (const candidate of items) {
      if (codes.includes(normalizeSpaceCode(candidate.spaceCode))) {
        origins.set(normalizeSpaceCode(candidate.spaceCode), { x: candidate.x, y: candidate.y });
      }
    }
    dragRef.current = { mode: "move", startX: local.x, startY: local.y, origins };
    (event.target as Element).setPointerCapture?.(event.pointerId);
  }

  function handleElementPointerDown(event: React.PointerEvent, element: LayoutElementApi, index: number) {
    event.stopPropagation();
    if (event.button === 0 && spacePressedRef.current) {
      dragRef.current = {
        mode: "pan",
        startClientX: event.clientX,
        startClientY: event.clientY,
        originX: pan.x,
        originY: pan.y,
      };
      (event.target as Element).setPointerCapture?.(event.pointerId);
      return;
    }
    const key = element.layoutElementId ?? `local-${index}`;
    toggleSelection({ kind: "element", key }, event.shiftKey);
    if (readOnly) return;
    onHistoryCheckpoint?.();
    const position = elementPosition(element);
    if (!position) return;
    const svg = svgRef.current;
    if (!svg) return;
    const local = toCanvas(svg, event.clientX, event.clientY, zoom, pan);
    dragRef.current = {
      mode: "move",
      startX: local.x,
      startY: local.y,
      origins: new Map([[`element:${key}`, position]]),
    };
    (event.target as Element).setPointerCapture?.(event.pointerId);
  }

  function beginSpaceResize(
    event: React.PointerEvent<SVGRectElement>,
    item: LayoutSpaceItemApi,
    handle: SpaceResizeHandle,
  ) {
    event.stopPropagation();
    onHistoryCheckpoint?.();
    const svg = svgRef.current;
    if (!svg) return;
    const local = toCanvas(svg, event.clientX, event.clientY, zoom, pan);
    dragRef.current = {
      mode: "resize",
      code: item.spaceCode,
      handle,
      startX: local.x,
      startY: local.y,
      origin: item,
    };
    event.currentTarget.setPointerCapture?.(event.pointerId);
  }

  function beginElementResize(
    event: React.PointerEvent<SVGRectElement>,
    key: string,
    handle: ResizeHandle,
    origin: { x: number; y: number; width: number; height: number; rotation: number },
  ) {
    event.stopPropagation();
    onHistoryCheckpoint?.();
    const svg = svgRef.current;
    if (!svg) return;
    const local = toCanvas(svg, event.clientX, event.clientY, zoom, pan);
    dragRef.current = { mode: "resize-element", key, handle, startX: local.x, startY: local.y, origin };
    event.currentTarget.setPointerCapture?.(event.pointerId);
  }

  function handlePointerMove(event: React.PointerEvent) {
    const drag = dragRef.current;
    const svg = svgRef.current;
    if (!drag || !svg) return;
    const local = toCanvas(svg, event.clientX, event.clientY, zoom, pan);

    if (drag.mode === "pan") {
      const scale = zoom;
      setPan(constrainPan({
        x: drag.originX + (event.clientX - drag.startClientX) / scale,
        y: drag.originY + (event.clientY - drag.startClientY) / scale,
      }));
      return;
    }

    if (readOnly) return;

    if (drag.mode === "move") {
      const dx = local.x - drag.startX;
      const dy = local.y - drag.startY;
      const nextItems = items.map((item) => {
        const origin = drag.origins.get(normalizeSpaceCode(item.spaceCode));
        if (!origin) return item;
        return { ...item, x: snapValue(origin.x + dx), y: snapValue(origin.y + dy) };
      });
      let elementsChanged = false;
      const nextElements = elements.map((element, index) => {
        const key = element.layoutElementId ?? `local-${index}`;
        const origin = drag.origins.get(`element:${key}`);
        if (!origin) return element;
        elementsChanged = true;
        return {
          ...element,
          geometry: { ...(element.geometry as Record<string, unknown>), x: snapValue(origin.x + dx), y: snapValue(origin.y + dy) },
        };
      });
      onItemsChange(nextItems);
      if (elementsChanged) onElementsChange(nextElements);
      return;
    }

    if (drag.mode === "resize") {
      const worldDx = local.x - drag.startX;
      const worldDy = local.y - drag.startY;
      const angle = (-drag.origin.rotation * Math.PI) / 180;
      const dx = worldDx * Math.cos(angle) - worldDy * Math.sin(angle);
      const dy = worldDx * Math.sin(angle) + worldDy * Math.cos(angle);
      const movesLeft = drag.handle.includes("w");
      const movesRight = drag.handle.includes("e");
      const movesTop = drag.handle.includes("n");
      const movesBottom = drag.handle.includes("s");
      const rawWidth = drag.origin.width + (movesRight ? dx : 0) - (movesLeft ? dx : 0);
      const rawHeight = drag.origin.height + (movesBottom ? dy : 0) - (movesTop ? dy : 0);
      const width = Math.max(SPACE_MIN_WIDTH, snapValue(rawWidth));
      const height = Math.max(SPACE_MIN_HEIGHT, snapValue(rawHeight));
      const shiftX = movesRight
        ? (width - drag.origin.width) / 2
        : movesLeft
          ? (drag.origin.width - width) / 2
          : 0;
      const shiftY = movesBottom
        ? (height - drag.origin.height) / 2
        : movesTop
          ? (drag.origin.height - height) / 2
          : 0;
      const rotation = (drag.origin.rotation * Math.PI) / 180;
      const worldShiftX = shiftX * Math.cos(rotation) - shiftY * Math.sin(rotation);
      const worldShiftY = shiftX * Math.sin(rotation) + shiftY * Math.cos(rotation);
      const originCenterX = drag.origin.x + drag.origin.width / 2;
      const originCenterY = drag.origin.y + drag.origin.height / 2;
      onItemsChange(
        items.map((item) =>
          normalizeSpaceCode(item.spaceCode) === normalizeSpaceCode(drag.code)
            ? {
                ...item,
                x: snapValue(originCenterX + worldShiftX - width / 2),
                y: snapValue(originCenterY + worldShiftY - height / 2),
                width,
                height,
              }
            : item,
        ),
      );
      return;
    }

    if (drag.mode === "resize-element") {
      const worldDx = local.x - drag.startX;
      const worldDy = local.y - drag.startY;
      const inverseAngle = (-drag.origin.rotation * Math.PI) / 180;
      const dx = worldDx * Math.cos(inverseAngle) - worldDy * Math.sin(inverseAngle);
      const dy = worldDx * Math.sin(inverseAngle) + worldDy * Math.cos(inverseAngle);
      const movesLeft = drag.handle.includes("w");
      const movesRight = drag.handle.includes("e");
      const movesTop = drag.handle.includes("n");
      const movesBottom = drag.handle.includes("s");
      const rawWidth = drag.origin.width + (movesRight ? dx : 0) - (movesLeft ? dx : 0);
      const rawHeight = drag.origin.height + (movesBottom ? dy : 0) - (movesTop ? dy : 0);
      const width = Math.max(MIN_SIZE, snapValue(rawWidth));
      const height = Math.max(MIN_SIZE, snapValue(rawHeight));
      const shiftX = movesRight ? (width - drag.origin.width) / 2 : movesLeft ? (drag.origin.width - width) / 2 : 0;
      const shiftY = movesBottom ? (height - drag.origin.height) / 2 : movesTop ? (drag.origin.height - height) / 2 : 0;
      const rotation = (drag.origin.rotation * Math.PI) / 180;
      const worldShiftX = shiftX * Math.cos(rotation) - shiftY * Math.sin(rotation);
      const worldShiftY = shiftX * Math.sin(rotation) + shiftY * Math.cos(rotation);
      const originCenterX = drag.origin.x + drag.origin.width / 2;
      const originCenterY = drag.origin.y + drag.origin.height / 2;
      onElementsChange(
        elements.map((element, index) => {
          const key = element.layoutElementId ?? `local-${index}`;
          if (key !== drag.key) return element;
          return {
            ...element,
            geometry: {
              ...(element.geometry as Record<string, unknown>),
              x: snapValue(originCenterX + worldShiftX - width / 2),
              y: snapValue(originCenterY + worldShiftY - height / 2),
              width,
              height,
            },
          };
        }),
      );
      return;
    }

    if (drag.mode === "rotate") {
      const angle = (Math.atan2(local.y - drag.centerY, local.x - drag.centerX) * 180) / Math.PI;
      const normalized = ((angle + 90) % 360 + 360) % 360;
      onItemsChange(
        items.map((item) =>
          normalizeSpaceCode(item.spaceCode) === normalizeSpaceCode(drag.code)
            ? { ...item, rotation: Math.round(normalized * 100) / 100 }
            : item,
        ),
      );
      return;
    }

    if (drag.mode === "rotate-element") {
      const angle = (Math.atan2(local.y - drag.centerY, local.x - drag.centerX) * 180) / Math.PI;
      const rotation = Math.round((((angle + 90) % 360 + 360) % 360) * 100) / 100;
      onElementsChange(
        elements.map((element, index) => {
          const key = element.layoutElementId ?? `local-${index}`;
          return key === drag.key
            ? { ...element, geometry: { ...(element.geometry as Record<string, unknown>), rotation } }
            : element;
        }),
      );
    }
  }

  function handlePointerUp() {
    dragRef.current = null;
  }

  function handleWheel(event: WheelEvent) {
    if (!event.ctrlKey || !event.altKey) return;
    event.preventDefault();
    event.stopPropagation();
    const svg = svgRef.current;
    if (!svg) return;
    const cursor = toLocal(svg, event.clientX, event.clientY);
    const world = {
      x: cursor.x / zoom - pan.x,
      y: cursor.y / zoom - pan.y,
    };
    const factor = Math.exp(-event.deltaY * 0.002);
    const nextZoom = Math.min(3, Math.max(0.2, zoom * factor));
    setZoom(nextZoom);
    setPan(constrainPan({
      x: cursor.x / nextZoom - world.x,
      y: cursor.y / nextZoom - world.y,
    }, nextZoom));
  }

  function handleDrop(event: React.DragEvent<HTMLElement>) {
    event.preventDefault();
    if (readOnly || !onPaletteDrop) return;
    const tool = event.dataTransfer.getData("application/x-parking-layout-tool") as LayoutPaletteTool;
    if (!tool) return;
    const svg = svgRef.current;
    if (!svg) return;
    onHistoryCheckpoint?.();
    onPaletteDrop(tool, toCanvas(svg, event.clientX, event.clientY, zoom, pan));
  }

  function deleteSelection() {
    if (readOnly) return;
    const spaceCodes = new Set(
      selected.filter((entry) => entry.kind === "space").map((entry) => normalizeSpaceCode(entry.code)),
    );
    const elementKeys = new Set(
      selected.filter((entry) => entry.kind === "element").map((entry) => entry.key),
    );
    if (spaceCodes.size === 0 && elementKeys.size === 0) return;
    onHistoryCheckpoint?.();
    onItemsChange(items.filter((item) => !spaceCodes.has(normalizeSpaceCode(item.spaceCode))));
    onElementsChange(
      elements.filter((element, index) => !elementKeys.has(element.layoutElementId ?? `local-${index}`)),
    );
    onSelect([]);
  }

  function duplicateSelection() {
    if (readOnly) return;
    const spaceCodes = new Set(
      selected.filter((entry) => entry.kind === "space").map((entry) => normalizeSpaceCode(entry.code)),
    );
    const copies = items
      .filter((item) => spaceCodes.has(normalizeSpaceCode(item.spaceCode)))
      .map((item, index) => ({
        ...item,
        layoutItemId: null,
        parkingSpaceId: null,
        spaceCode: `${normalizeSpaceCode(item.spaceCode)}-C${index + 1}`,
        x: item.x + 20,
        y: item.y + 20,
      }));
    if (copies.length === 0) return;
    onHistoryCheckpoint?.();
    onItemsChange([...items, ...copies]);
    onSelect(copies.map((item) => ({ kind: "space" as const, code: item.spaceCode })));
  }

  function copySelection() {
    setClipboard(
      items.filter((item) =>
        selected.some(
          (entry) => entry.kind === "space" && normalizeSpaceCode(entry.code) === normalizeSpaceCode(item.spaceCode),
        ),
      ),
    );
  }

  function pasteClipboard() {
    if (readOnly) return;
    if (clipboard.length === 0) return;
    onHistoryCheckpoint?.();
    const copies = clipboard.map((item, index) => ({
      ...item,
      layoutItemId: null,
      parkingSpaceId: null,
      spaceCode: `${normalizeSpaceCode(item.spaceCode)}-P${index + 1}`,
      x: item.x + 30,
      y: item.y + 30,
    }));
    onItemsChange([...items, ...copies]);
    onSelect(copies.map((item) => ({ kind: "space" as const, code: item.spaceCode })));
  }

  function alignSelection(mode: "left" | "centerX" | "top" | "middle") {
    if (readOnly) return;
    const targets = items.filter((item) =>
      selected.some(
        (entry) => entry.kind === "space" && normalizeSpaceCode(entry.code) === normalizeSpaceCode(item.spaceCode),
      ),
    );
    if (targets.length < 2) return;
    let reference = 0;
    if (mode === "left") reference = Math.min(...targets.map((item) => item.x));
    if (mode === "centerX") {
      const centers = targets.map((item) => item.x + item.width / 2);
      reference = centers.reduce((sum, value) => sum + value, 0) / centers.length;
    }
    if (mode === "top") reference = Math.min(...targets.map((item) => item.y));
    if (mode === "middle") {
      const centers = targets.map((item) => item.y + item.height / 2);
      reference = centers.reduce((sum, value) => sum + value, 0) / centers.length;
    }
    onItemsChange(
      items.map((item) => {
        if (
          !selected.some(
            (entry) => entry.kind === "space" && normalizeSpaceCode(entry.code) === normalizeSpaceCode(item.spaceCode),
          )
        ) {
          return item;
        }
        if (mode === "left") return { ...item, x: snapValue(reference) };
        if (mode === "centerX") return { ...item, x: snapValue(reference - item.width / 2) };
        if (mode === "top") return { ...item, y: snapValue(reference) };
        return { ...item, y: snapValue(reference - item.height / 2) };
      }),
    );
  }

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      if (target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.tagName === "SELECT")) {
        return;
      }
      if (readOnly) {
        if (event.key === "Escape") onSelect([]);
        return;
      }
      if (event.key === "Delete" || event.key === "Backspace") {
        event.preventDefault();
        deleteSelection();
      } else if (event.key === "Escape") {
        onSelect([]);
      } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "z") {
        event.preventDefault();
        if (event.shiftKey) onRedo?.();
        else onUndo?.();
      } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "y") {
        event.preventDefault();
        onRedo?.();
      } else if (event.key.startsWith("Arrow")) {
        const step = event.shiftKey ? 10 : 1;
        const dx = event.key === "ArrowLeft" ? -step : event.key === "ArrowRight" ? step : 0;
        const dy = event.key === "ArrowUp" ? -step : event.key === "ArrowDown" ? step : 0;
        if (dx !== 0 || dy !== 0) {
          event.preventDefault();
          onHistoryCheckpoint?.();
          onItemsChange(
            items.map((item) =>
              selected.some(
                (entry) => entry.kind === "space" && normalizeSpaceCode(entry.code) === normalizeSpaceCode(item.spaceCode),
              )
                ? { ...item, x: item.x + dx, y: item.y + dy }
                : item,
            ),
          );
          const elementKeys = new Set(
            selected.filter((entry) => entry.kind === "element").map((entry) => entry.key),
          );
          onElementsChange(
            elements.map((element, index) => {
              const key = element.layoutElementId ?? `local-${index}`;
              if (!elementKeys.has(key)) return element;
              const position = elementPosition(element);
              if (!position) return element;
              return {
                ...element,
                geometry: {
                  ...(element.geometry as Record<string, unknown>),
                  x: position.x + dx,
                  y: position.y + dy,
                },
              };
            }),
          );
        }
      } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "c") {
        copySelection();
      } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "v") {
        event.preventDefault();
        pasteClipboard();
      } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "d") {
        event.preventDefault();
        duplicateSelection();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items, elements, selected, readOnly]);

  const gridLines = useMemo(() => {
    if (!showGrid) return null;
    const step = 50;
    const vertical: number[] = [];
    const horizontal: number[] = [];
    for (let x = 0; x <= canvasWidth; x += step) vertical.push(x);
    for (let y = 0; y <= canvasHeight; y += step) horizontal.push(y);
    return { vertical, horizontal };
  }, [showGrid, canvasWidth, canvasHeight]);

  const primarySelection = selected.length === 1 ? selected[0] : null;
  const selectedSpace =
    primarySelection?.kind === "space"
      ? items.find((item) => normalizeSpaceCode(item.spaceCode) === normalizeSpaceCode(primarySelection.code))
      : undefined;

  const toolbar = (
    <div className="tw-flex tw-min-h-10 tw-flex-wrap tw-items-center tw-justify-end tw-gap-2" role="toolbar" aria-label="Công cụ sơ đồ">
      {!readOnly ? (
        <>
          <button className="tw-size-9 tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700 disabled:tw-opacity-40" type="button" disabled={!canUndo} onClick={onUndo} title="Lùi lại (Ctrl+Z)"><i className="fas fa-undo" /></button>
          <button className="tw-size-9 tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700 disabled:tw-opacity-40" type="button" disabled={!canRedo} onClick={onRedo} title="Tiến lại (Ctrl+Shift+Z / Ctrl+Y)"><i className="fas fa-redo" /></button>
        </>
      ) : null}
      <div className="tw-flex tw-h-9 tw-overflow-hidden tw-rounded-md tw-border tw-border-solid tw-border-slate-200">
        <button className="tw-w-9 tw-border-0 tw-border-r tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700" type="button" onClick={() => changeZoom(zoom - .1)} aria-label="Thu nhỏ">−</button>
        <button className="tw-w-16 tw-border-0 tw-bg-white tw-text-xs tw-font-bold tw-text-slate-700" type="button" onClick={() => { setZoom(1); setPan({ x: 0, y: 0 }); }}>{Math.round(zoom * 100)}%</button>
        <button className="tw-w-9 tw-border-0 tw-border-l tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700" type="button" onClick={() => changeZoom(zoom + .1)} aria-label="Phóng to">+</button>
      </div>
      <button className="tw-size-9 tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700" type="button" onClick={fitToScreen} title="Vừa màn hình"><i className="fas fa-expand-arrows-alt" /></button>
      <button className="tw-size-9 tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-text-slate-700" type="button" onClick={centerDiagram} title="Về trung tâm sơ đồ"><i className="fas fa-crosshairs" /></button>
      <button className={cn("tw-h-9 tw-rounded-md tw-border tw-border-solid tw-px-3 tw-text-xs tw-font-bold", showGrid ? "tw-border-blue-200 tw-bg-blue-50 tw-text-blue-700" : "tw-border-slate-200 tw-bg-white tw-text-slate-600")} type="button" onClick={() => setShowGrid((v) => !v)}><i className="fas fa-th tw-mr-2" />Grid</button>
      {!readOnly ? <button className={cn("tw-h-9 tw-rounded-md tw-border tw-border-solid tw-px-3 tw-text-xs tw-font-bold", snap ? "tw-border-blue-200 tw-bg-blue-50 tw-text-blue-700" : "tw-border-slate-200 tw-bg-white tw-text-slate-600")} type="button" onClick={() => setSnap((v) => !v)}><i className="fas fa-magnet tw-mr-2" />Snap</button> : null}
    </div>
  );

  return (
    <div className="tw-flex tw-h-full tw-min-h-0 tw-w-full tw-flex-col tw-gap-2">
      {toolbarTarget ? createPortal(toolbar, toolbarTarget) : toolbar}

      <div
        ref={viewportRef}
        className="tw-min-h-[590px] tw-flex-1 tw-overflow-auto tw-rounded-[4px] tw-border tw-border-solid tw-border-slate-200 tw-bg-[#fbfdff]"
        style={{ touchAction: "none" }}
        onDragOver={(event) => {
          if (!readOnly && onPaletteDrop) event.preventDefault();
        }}
        onDrop={handleDrop}
      >
        <svg
          ref={svgRef}
          role="application"
          aria-label="Khung vẽ sơ đồ bãi xe"
          width={Math.max(canvasWidth * zoom + 48, viewportRef.current?.clientWidth ?? 0)}
          height={Math.max(canvasHeight * zoom + 48, viewportRef.current?.clientHeight ?? 0)}
          onPointerDown={handleBackgroundPointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerLeave={handlePointerUp}
        >
          <g transform={`translate(${pan.x * zoom},${pan.y * zoom}) scale(${zoom})`}>
            <rect x={0} y={0} width={canvasWidth} height={canvasHeight} fill="#ffffff" stroke="#cbd5e1" strokeWidth={1.5} />
            {gridLines && (
              <g stroke="#e2e8f0" strokeWidth={0.5}>
                {gridLines.vertical.map((x) => (
                  <line key={`v${x}`} x1={x} y1={0} x2={x} y2={canvasHeight} />
                ))}
                {gridLines.horizontal.map((y) => (
                  <line key={`h${y}`} x1={0} y1={y} x2={canvasWidth} y2={y} />
                ))}
              </g>
            )}

            {[...elements]
              .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
              .map((element, index) => {
                const key = element.layoutElementId ?? `local-${index}`;
                const position = elementPosition(element) ?? { x: 0, y: 0 };
                const size = elementSize(element);
                const rotation = Number((element.geometry as Record<string, unknown>)?.["rotation"] ?? 0);
                const centerX = position.x + size.width / 2;
                const centerY = position.y + size.height / 2;
                const isSelected = selectedKeys.has(`element:${key}`);
                const elementResizeHandles = resizeHandles(position.x, position.y, size.width, size.height);
                const elementHandleSize = Math.max(7, Math.min(10, Math.min(size.width, size.height) * 0.18));
                return (
                  <g key={elementKey(element, index)} transform={`rotate(${rotation} ${centerX} ${centerY})`}>
                    <LayoutElementShape
                      element={element}
                      selected={isSelected}
                      onPointerDown={(event) => handleElementPointerDown(event, element, index)}
                    />
                    {isSelected && !readOnly && (
                      <g>
                        <rect x={position.x} y={position.y} width={size.width} height={size.height} fill="none" stroke="#2563eb" strokeWidth={1.5} strokeDasharray="4 3" pointerEvents="none" />
                        {elementResizeHandles.map((handle) => (
                          <rect
                            key={handle.handle}
                            x={handle.x - elementHandleSize / 2}
                            y={handle.y - elementHandleSize / 2}
                            width={elementHandleSize}
                            height={elementHandleSize}
                            rx={1.5}
                            fill="#ffffff"
                            stroke="#2563eb"
                            strokeWidth={2}
                            style={{ cursor: handle.cursor }}
                            onPointerDown={(event) => beginElementResize(event, key, handle.handle, { x: position.x, y: position.y, width: size.width, height: size.height, rotation })}
                          >
                            <title>Đổi kích thước {handle.handle}</title>
                          </rect>
                        ))}
                        <line x1={centerX} y1={position.y} x2={centerX} y2={position.y - 12} stroke="#2563eb" strokeWidth={1.5} pointerEvents="none" />
                        <circle
                          cx={centerX}
                          cy={position.y - 18}
                          r={7}
                          fill="#fff"
                          stroke="#2563eb"
                          strokeWidth={2}
                          style={{ cursor: "grab" }}
                          onPointerDown={(event) => {
                            event.stopPropagation();
                            onHistoryCheckpoint?.();
                            dragRef.current = { mode: "rotate-element", key, centerX, centerY };
                            (event.target as Element).setPointerCapture?.(event.pointerId);
                          }}
                        />
                      </g>
                    )}
                  </g>
                );
              })}

            {items.map((item) => {
              const key = spaceKey(item.spaceCode);
              const isSelected = selectedKeys.has(key);
              const centerX = item.x + item.width / 2;
              const centerY = item.y + item.height / 2;
              const vehicleType = vehicleTypes.find((type) => type.vehicleTypeId === item.vehicleTypeId);
              const liveVisual = item.parkingSpaceId ? spaceVisuals?.get(item.parkingSpaceId) : undefined;
              const liveMeta = liveVisual
                ? liveVisual.allocated && liveVisual.status === "AVAILABLE"
                  ? ALLOCATED_SPACE_VISUAL
                  : LIVE_SPACE_VISUALS[liveVisual.status]
                : null;
              const vehicleIdentity = `${vehicleType?.code ?? ""} ${vehicleType?.name ?? ""}`.toLocaleUpperCase("vi");
              const isMotorcycle = /MOTOR|MOTO|XE MÁY|XE MAY/.test(vehicleIdentity);
              const carWidth = Math.max(12, Math.min(34, item.width * 0.44));
              const carHeight = Math.max(24, Math.min(item.height * 0.54, item.height - 28));
              const carX = centerX - carWidth / 2;
              const carY = item.y + Math.max(8, item.height * 0.14);
              const handleSize = Math.max(6, Math.min(10, Math.min(item.width, item.height) * 0.14));
              const spaceResizeHandles = resizeHandles(item.x, item.y, item.width, item.height);
              return (
                <g key={key} transform={`rotate(${item.rotation} ${centerX} ${centerY})`}>
                  <rect
                    x={item.x}
                    y={item.y}
                    width={item.width}
                    height={item.height}
                    rx={Math.max(3, Math.min(7, item.width * 0.07))}
                    fill={liveMeta?.fill ?? (isSelected ? "#dceaff" : "#f8fafc")}
                    stroke={isSelected ? "#1769e8" : liveMeta?.stroke ?? "#aeb7c2"}
                    strokeWidth={isSelected ? 2.5 : liveVisual?.allocated ? 2.5 : 1.25}
                    strokeDasharray={liveVisual?.allocated ? "6 3" : undefined}
                    style={{ cursor: "move" }}
                    onPointerDown={(event) => handleSpacePointerDown(event, item)}
                  >
                    <title>{liveMeta ? `${item.spaceCode} — ${liveMeta.label}${liveVisual?.allocated && liveVisual.status !== "AVAILABLE" ? " — Đã phân bổ" : ""}` : `${item.spaceCode} (${item.width}×${item.height})`}</title>
                  </rect>
                  <text
                    x={centerX}
                    y={item.y + item.height - Math.max(9, Math.min(16, item.height * .1))}
                    textAnchor="middle"
                    dominantBaseline="central"
                    fontFamily="Inter, ui-sans-serif, system-ui, sans-serif"
                    fontSize={Math.max(7.5, Math.min(13, Math.min(item.width, item.height) / 4))}
                    fontWeight={700}
                    letterSpacing={0.15}
                    fill="#0f172a"
                    stroke="none"
                    pointerEvents="none"
                  >
                    {normalizeSpaceCode(item.spaceCode)}
                  </text>
                  <g pointerEvents="none">
                    {isMotorcycle ? (() => {
                      const bikeWidth = Math.max(8, Math.min(18, item.width * 0.27));
                      const bikeX = centerX - bikeWidth / 2;
                      return (
                        <>
                          <ellipse cx={centerX} cy={carY + carHeight * 0.08} rx={bikeWidth * 0.32} ry={carHeight * 0.09} fill="#334155" />
                          <ellipse cx={centerX} cy={carY + carHeight * 0.9} rx={bikeWidth * 0.34} ry={carHeight * 0.1} fill="#334155" />
                          <rect x={bikeX + bikeWidth * 0.18} y={carY + carHeight * 0.13} width={bikeWidth * 0.64} height={carHeight * 0.72} rx={bikeWidth * 0.3} fill={isSelected ? "#6e9fec" : "#c3c9d0"} stroke={isSelected ? "#2d72df" : "#929ca7"} strokeWidth={1.3} />
                          <rect x={bikeX + bikeWidth * 0.24} y={carY + carHeight * 0.38} width={bikeWidth * 0.52} height={carHeight * 0.31} rx={bikeWidth * 0.22} fill={isSelected ? "#315f9f" : "#697584"} />
                          <path d={`M ${bikeX - bikeWidth * 0.18} ${carY + carHeight * 0.23} H ${bikeX + bikeWidth * 1.18}`} stroke={isSelected ? "#2d72df" : "#64748b"} strokeWidth={2} strokeLinecap="round" />
                          <circle cx={centerX} cy={carY + carHeight * 0.2} r={bikeWidth * 0.16} fill={isSelected ? "#dceaff" : "#eef1f4"} stroke={isSelected ? "#5b8fe1" : "#aab2bb"} strokeWidth={1} />
                        </>
                      );
                    })() : (
                      <>
                        <rect x={carX} y={carY} width={carWidth} height={carHeight} rx={carWidth * 0.3} fill={isSelected ? "#79a7ef" : "#c8cdd3"} stroke={isSelected ? "#2d72df" : "#9aa3ad"} strokeWidth={1.5} />
                        <rect x={carX + carWidth * 0.13} y={carY + carHeight * 0.12} width={carWidth * 0.74} height={carHeight * 0.18} rx={carWidth * 0.16} fill={isSelected ? "#dceaff" : "#eef1f4"} stroke={isSelected ? "#5b8fe1" : "#b5bdc5"} strokeWidth={1} />
                        <rect x={carX + carWidth * 0.12} y={carY + carHeight * 0.34} width={carWidth * 0.76} height={carHeight * 0.34} rx={carWidth * 0.12} fill={isSelected ? "#70a0eb" : "#bdc3ca"} stroke={isSelected ? "#4b83dc" : "#a7afb8"} strokeWidth={1} />
                        <rect x={carX + carWidth * 0.13} y={carY + carHeight * 0.72} width={carWidth * 0.74} height={carHeight * 0.15} rx={carWidth * 0.14} fill={isSelected ? "#dceaff" : "#eef1f4"} stroke={isSelected ? "#5b8fe1" : "#b5bdc5"} strokeWidth={1} />
                        <line x1={carX + carWidth * 0.08} y1={carY + carHeight * 0.34} x2={carX + carWidth * 0.08} y2={carY + carHeight * 0.68} stroke={isSelected ? "#d8e8ff" : "#f8fafc"} strokeWidth={1.4} />
                        <line x1={carX + carWidth * 0.92} y1={carY + carHeight * 0.34} x2={carX + carWidth * 0.92} y2={carY + carHeight * 0.68} stroke={isSelected ? "#d8e8ff" : "#f8fafc"} strokeWidth={1.4} />
                        <rect x={carX - carWidth * 0.07} y={carY + carHeight * 0.31} width={carWidth * 0.09} height={carHeight * 0.12} rx={1} fill={isSelected ? "#5e91e4" : "#aab2bb"} />
                        <rect x={carX + carWidth * 0.98} y={carY + carHeight * 0.31} width={carWidth * 0.09} height={carHeight * 0.12} rx={1} fill={isSelected ? "#5e91e4" : "#aab2bb"} />
                      </>
                    )}
                  </g>
                  {liveMeta ? (
                    <g pointerEvents="none" aria-hidden="true">
                      <circle
                        cx={item.x + item.width - Math.max(7, Math.min(11, item.width * 0.13))}
                        cy={item.y + Math.max(7, Math.min(11, item.width * 0.13))}
                        r={Math.max(5, Math.min(8, item.width * 0.1))}
                        fill="#ffffff"
                        stroke={liveMeta.stroke}
                        strokeWidth={1.25}
                      />
                      <text
                        x={item.x + item.width - Math.max(7, Math.min(11, item.width * 0.13))}
                        y={item.y + Math.max(7, Math.min(11, item.width * 0.13)) + 0.5}
                        textAnchor="middle"
                        dominantBaseline="central"
                        fontFamily="Inter, ui-sans-serif, system-ui, sans-serif"
                        fontSize={Math.max(6, Math.min(9, item.width * 0.11))}
                        fontWeight={800}
                        fill={liveMeta.stroke}
                        stroke="none"
                      >
                        {liveMeta.glyph}
                      </text>
                    </g>
                  ) : null}
                  {isSelected && selectedSpace && normalizeSpaceCode(selectedSpace.spaceCode) === normalizeSpaceCode(item.spaceCode) && (
                    <g>
                      {spaceResizeHandles.map((handle) => (
                        <rect
                          key={handle.handle}
                          x={handle.x - handleSize / 2}
                          y={handle.y - handleSize / 2}
                          width={handleSize}
                          height={handleSize}
                          rx={1.5}
                          fill="#ffffff"
                          stroke="#1769e8"
                          strokeWidth={2}
                          style={{ cursor: handle.cursor }}
                          onPointerDown={(event) => beginSpaceResize(event, item, handle.handle)}
                        >
                          <title>Đổi kích thước {handle.handle}</title>
                        </rect>
                      ))}
                      <line x1={centerX} y1={item.y} x2={centerX} y2={item.y - 22} stroke="#1769e8" strokeWidth={2} pointerEvents="none" />
                      <circle
                        cx={centerX}
                        cy={item.y - 31}
                        r={10}
                        fill="#ffffff"
                        stroke="#1769e8"
                        strokeWidth={2}
                        style={{ cursor: "grab" }}
                        onPointerDown={(event) => {
                          event.stopPropagation();
                          onHistoryCheckpoint?.();
                          dragRef.current = { mode: "rotate", code: item.spaceCode, centerX, centerY };
                          (event.target as Element).setPointerCapture?.(event.pointerId);
                        }}
                      >
                        <title>Xoay</title>
                      </circle>
                      <text x={centerX} y={item.y - 30.5} textAnchor="middle" dominantBaseline="central" fontFamily="Inter, ui-sans-serif, system-ui, sans-serif" fontSize={9} fontWeight={700} fill="#1769e8" stroke="none" pointerEvents="none">↻</text>
                    </g>
                  )}
                </g>
              );
            })}
          </g>
        </svg>
      </div>
      <p className="tw-m-0 tw-text-[0.68rem] tw-font-semibold tw-text-vm-slate-500">
        {readOnly
          ? "Nhấn vào ô để xem chi tiết • Space + chuột trái để di chuyển mặt bằng • Ctrl + Alt + lăn chuột để zoom tại con trỏ."
          : "Kéo để di chuyển • Kéo công cụ từ palette để đặt vào sơ đồ • Space + chuột trái để di chuyển mặt bằng • Ctrl + Alt + lăn chuột để zoom tại con trỏ."}
      </p>
    </div>
  );
}

function LayoutElementShape({
  element,
  onPointerDown,
  selected,
}: {
  element: LayoutElementApi;
  selected: boolean;
  onPointerDown: (event: React.PointerEvent) => void;
}) {
  const geometry = (element.geometry ?? {}) as Record<string, number | string>;
  const style = (element.style ?? {}) as Record<string, string | number>;
  const stroke = selected ? "#2563eb" : String(style["stroke"] ?? "#94a3b8");
  const fill = String(style["fill"] ?? "transparent");
  const strokeWidth = selected ? 2.5 : Number(style["strokeWidth"] ?? 1.5);
  const common = {
    stroke,
    strokeWidth,
    style: { cursor: "move" },
    onPointerDown,
  } as const;

  switch (element.elementType) {
    case "WALL": {
      const x = Number(geometry["x"] ?? 0);
      const y = Number(geometry["y"] ?? 0);
      const { width, height } = elementSize(element);
      return (
        <g {...common}>
          <title>Tường</title>
          <rect x={x} y={y} width={width} height={height} fill="#596168" stroke="#3f474d" rx={1} />
          <line
            x1={x + (width >= height ? 0 : width * 0.25)}
            y1={y + (width >= height ? height * 0.24 : 0)}
            x2={x + (width >= height ? width : width * 0.25)}
            y2={y + (width >= height ? height * 0.24 : height)}
            stroke="#899198"
            strokeWidth={Math.max(1, Math.min(width, height) * 0.12)}
            pointerEvents="none"
          />
        </g>
      );
    }
    case "COLUMN": {
      const x = Number(geometry["x"] ?? 0);
      const y = Number(geometry["y"] ?? 0);
      const { width, height } = elementSize(element);
      return (
        <g {...common}>
          <title>Cột</title>
          <rect x={x} y={y} width={width} height={height} rx={1} fill="#6b7379" stroke="#41484d" />
          <rect x={x + 2} y={y + 2} width={Math.max(0, width - 4)} height={Math.max(0, height - 4)} fill="none" stroke="#8f989e" strokeWidth={1} pointerEvents="none" />
        </g>
      );
    }
    case "DRIVE_LANE": {
      const x = Number(geometry["x"] ?? 0);
      const y = Number(geometry["y"] ?? 0);
      const { width, height } = elementSize(element);
      const variant = String(style["variant"] ?? "ROAD");
      if (variant === "ARROW") {
        return (
          <g {...common}>
            <title>Mũi tên chỉ đường</title>
            <path
              d={`M ${x} ${y + height * 0.36}
                  H ${x + width * 0.62}
                  V ${y + height * 0.12}
                  L ${x + width} ${y + height * 0.5}
                  L ${x + width * 0.62} ${y + height * 0.88}
                  V ${y + height * 0.64}
                  H ${x} Z`}
              fill={String(style["fill"] ?? "#ffffff")}
              stroke="none"
            />
          </g>
        );
      }
      if (variant === "CROSSWALK") {
        return (
          <g {...common}>
            <title>Vạch qua đường</title>
            {Array.from({ length: 7 }, (_, index) => (
              <rect
                key={index}
                x={x + index * (width / 7)}
                y={y}
                width={width / 12}
                height={height}
                fill={String(style["fill"] ?? "#ffffff")}
                stroke="none"
              />
            ))}
          </g>
        );
      }
      return (
        <g {...common}>
          <title>Đường đi</title>
          <rect x={x} y={y} width={width} height={height} rx={2} fill={fill === "transparent" ? "#e1e5e8" : fill} stroke={stroke} />
          <line x1={x + 22} y1={y + height / 2} x2={x + width - 22} y2={y + height / 2} stroke="#ffffff" strokeWidth={2} strokeDasharray="16 11" pointerEvents="none" />
        </g>
      );
    }
    case "ENTRY":
    case "EXIT": {
      const x = Number(geometry["x"] ?? 0);
      const y = Number(geometry["y"] ?? 0);
      const { width, height } = elementSize(element);
      const isEntry = element.elementType === "ENTRY";
      const label = element.label ?? (isEntry ? "LỐI VÀO" : "LỐI RA");
      return (
        <g {...common}>
          <title>{isEntry ? "Cửa vào" : "Cửa ra"}</title>
          {isEntry ? (
            <>
              <rect x={x + width * 0.07} y={y + height * 0.47} width={width * 0.14} height={height * 0.22} fill="#35404b" stroke="#202a35" strokeWidth={1} />
              <rect x={x + width * 0.09} y={y + height * 0.37} width={width * 0.1} height={height * 0.15} fill="#f4c51d" stroke="#35404b" strokeWidth={1} />
              <rect x={x + width * 0.105} y={y + height * 0.06} width={width * 0.07} height={height * 0.43} fill="#f8fafc" stroke="#46515c" strokeWidth={1} />
              {Array.from({ length: 5 }, (_, index) => (
                <line
                  key={index}
                  x1={x + width * 0.105}
                  y1={y + height * (0.1 + index * 0.075)}
                  x2={x + width * 0.175}
                  y2={y + height * (0.16 + index * 0.075)}
                  stroke="#ef4444"
                  strokeWidth={Math.max(2, width * 0.025)}
                  pointerEvents="none"
                />
              ))}
              <path
                d={`M ${x + width * 0.64} ${y + height * 0.27}
                    L ${x + width * 0.76} ${y + height * 0.42}
                    L ${x + width * 0.69} ${y + height * 0.42}
                    L ${x + width * 0.69} ${y + height * 0.65}
                    L ${x + width * 0.59} ${y + height * 0.65}
                    L ${x + width * 0.59} ${y + height * 0.42}
                    L ${x + width * 0.52} ${y + height * 0.42} Z`}
                fill="#334155"
                stroke="none"
                pointerEvents="none"
              />
            </>
          ) : (
            <>
              <rect x={x + width * 0.07} y={y + height * 0.23} width={width * 0.14} height={height * 0.27} fill="#35404b" stroke="#202a35" strokeWidth={1} />
              <rect x={x + width * 0.085} y={y + height * 0.08} width={width * 0.11} height={height * 0.2} fill="#f4c51d" stroke="#35404b" strokeWidth={1} />
              <rect x={x + width * 0.18} y={y + height * 0.24} width={width * 0.67} height={height * 0.075} fill="#f8fafc" stroke="#46515c" strokeWidth={1} />
              {Array.from({ length: 6 }, (_, index) => (
                <line
                  key={index}
                  x1={x + width * (0.21 + index * 0.1)}
                  y1={y + height * 0.24}
                  x2={x + width * (0.25 + index * 0.1)}
                  y2={y + height * 0.315}
                  stroke="#ef4444"
                  strokeWidth={Math.max(2, height * 0.04)}
                  pointerEvents="none"
                />
              ))}
              <path
                d={`M ${x + width * 0.49} ${y + height * 0.42}
                    L ${x + width * 0.59} ${y + height * 0.42}
                    L ${x + width * 0.59} ${y + height * 0.62}
                    L ${x + width * 0.66} ${y + height * 0.62}
                    L ${x + width * 0.54} ${y + height * 0.76}
                    L ${x + width * 0.42} ${y + height * 0.62}
                    L ${x + width * 0.49} ${y + height * 0.62} Z`}
                fill="#334155"
                stroke="none"
                pointerEvents="none"
              />
            </>
          )}
          <text
            x={x + width / 2}
            y={y + height * 0.91}
            textAnchor="middle"
            dominantBaseline="central"
            fontFamily="Inter, ui-sans-serif, system-ui, sans-serif"
            fontSize={Math.max(7.5, Math.min(11, width * 0.105))}
            fontWeight={700}
            letterSpacing={0.1}
            fill="#334155"
            stroke="none"
            pointerEvents="none"
          >
            {label}
          </text>
        </g>
      );
    }
    case "TEXT":
    case "LABEL": {
      const x = Number(geometry["x"] ?? 0);
      const y = Number(geometry["y"] ?? 0);
      return (
        <text
          {...common}
          x={x}
          y={y}
          fontFamily="Inter, ui-sans-serif, system-ui, sans-serif"
          fontSize={Number(style["fontSize"] ?? 12)}
          fontWeight={700}
          letterSpacing={0.1}
          fill={String(style["color"] ?? "#334155")}
          stroke="none"
          strokeWidth={0}
        >
          {element.label ?? "Nhãn"}
        </text>
      );
    }
    case "BACKGROUND":
    case "IMAGE_OVERLAY": {
      const x = Number(geometry["x"] ?? 0);
      const y = Number(geometry["y"] ?? 0);
      const { width, height } = elementSize(element);
      const opacity = Number(style["opacity"] ?? (element.elementType === "IMAGE_OVERLAY" ? 0.5 : 1));
      return (
        <rect x={x} y={y} width={width} height={height} fill={fill === "transparent" ? "#f8fafc" : fill} opacity={opacity} {...common}>
          <title>{element.label ?? "Nền"}</title>
        </rect>
      );
    }
    default:
      return null;
  }
}
