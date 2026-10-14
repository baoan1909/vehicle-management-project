import type { LayoutElementApi, LayoutSpaceItemApi } from "@/features/parking/api/parkingLayoutApi";

export const ALLOWED_ELEMENT_TYPES = [
  "WALL",
  "COLUMN",
  "DRIVE_LANE",
  "ENTRY",
  "EXIT",
  "TEXT",
  "LABEL",
  "BACKGROUND",
  "IMAGE_OVERLAY",
] as const;

const MAX_GEOMETRY_CHARS = 8192;
const MAX_STYLE_CHARS = 4096;
const MAX_LABEL_CHARS = 200;

const UNSAFE_TOKENS = [
  "<script",
  "javascript:",
  "expression(",
  "onload=",
  "onerror=",
  "onclick=",
  "<svg",
  "<img",
  "<iframe",
];

export function normalizeSpaceCode(code: string): string {
  return code.trim().toUpperCase();
}

export type LayoutIssue = {
  code: string;
  message: string;
};

type Corner = [number, number];

function rotatedCorners(item: {
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
}): Corner[] {
  const radians = (item.rotation * Math.PI) / 180;
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);
  const centerX = item.x + item.width / 2;
  const centerY = item.y + item.height / 2;
  const local: Corner[] = [
    [-item.width / 2, -item.height / 2],
    [item.width / 2, -item.height / 2],
    [item.width / 2, item.height / 2],
    [-item.width / 2, item.height / 2],
  ];
  return local.map(([lx, ly]) => [centerX + lx * cos - ly * sin, centerY + lx * sin + ly * cos]);
}

function edgeNormal(from: Corner, to: Corner): Corner {
  const dx = to[0] - from[0];
  const dy = to[1] - from[1];
  const length = Math.hypot(dx, dy);
  if (length === 0) return [1, 0];
  return [-dy / length, dx / length];
}

function project(corners: Corner[], axis: Corner): [number, number] {
  let min = Number.POSITIVE_INFINITY;
  let max = Number.NEGATIVE_INFINITY;
  for (const corner of corners) {
    const dot = corner[0] * axis[0] + corner[1] * axis[1];
    if (dot < min) min = dot;
    if (dot > max) max = dot;
  }
  return [min, max];
}

export function rectanglesOverlap(
  a: { x: number; y: number; width: number; height: number; rotation: number },
  b: { x: number; y: number; width: number; height: number; rotation: number },
): boolean {
  const cornersA = rotatedCorners(a);
  const cornersB = rotatedCorners(b);
  const axes: Corner[] = [
    edgeNormal(cornersA[0], cornersA[1]),
    edgeNormal(cornersA[1], cornersA[2]),
    edgeNormal(cornersB[0], cornersB[1]),
    edgeNormal(cornersB[1], cornersB[2]),
  ];
  for (const axis of axes) {
    const [minA, maxA] = project(cornersA, axis);
    const [minB, maxB] = project(cornersB, axis);
    if (maxA <= minB || maxB <= minA) return false;
  }
  return true;
}

export function validateDraftItems(
  items: LayoutSpaceItemApi[],
  canvas: { width: number; height: number },
  supportedVehicleTypeIds?: Set<string> | null,
): LayoutIssue[] {
  const issues: LayoutIssue[] = [];
  const seen = new Map<string, number>();

  items.forEach((item, index) => {
    const code = normalizeSpaceCode(item.spaceCode ?? "");
    if (!code) {
      issues.push({ code: `items[${index}].spaceCode`, message: "Mã ô không được để trống." });
    } else {
      const first = seen.get(code);
      if (first !== undefined) {
        issues.push({
          code: `items[${index}].spaceCode`,
          message: `Trùng mã ô ${code} với dòng ${first + 1}.`,
        });
      } else {
        seen.set(code, index);
      }
    }

    for (const field of ["x", "y", "width", "height", "rotation"] as const) {
      const value = item[field];
      if (!Number.isFinite(value)) {
        issues.push({ code: `items[${index}].${field}`, message: `Tọa độ ${field} phải là số hữu hạn.` });
      }
    }
    if (!(item.width > 0) || !(item.height > 0)) {
      issues.push({ code: `items[${index}].size`, message: "Rộng và cao phải lớn hơn 0." });
    }
    if (
      Number.isFinite(item.x) &&
      Number.isFinite(item.y) &&
      Number.isFinite(item.width) &&
      Number.isFinite(item.height)
    ) {
      for (const [cx, cy] of rotatedCorners(item)) {
        if (cx < 0 || cy < 0 || cx > canvas.width || cy > canvas.height) {
          issues.push({
            code: `items[${index}].position`,
            message: `Ô ${code || index + 1} nằm ngoài khung vẽ.`,
          });
          break;
        }
      }
    }
    if (supportedVehicleTypeIds && item.vehicleTypeId && !supportedVehicleTypeIds.has(item.vehicleTypeId)) {
      issues.push({
        code: `items[${index}].vehicleTypeId`,
        message: `Ô ${code || index + 1} dùng loại xe không thuộc bãi.`,
      });
    }
  });

  for (let i = 0; i < items.length; i += 1) {
    for (let j = i + 1; j < items.length; j += 1) {
      const a = items[i];
      const b = items[j];
      if (
        [a.x, a.y, a.width, a.height, a.rotation, b.x, b.y, b.width, b.height, b.rotation].every(
          Number.isFinite,
        ) &&
        a.width > 0 &&
        a.height > 0 &&
        b.width > 0 &&
        b.height > 0 &&
        rectanglesOverlap(a, b)
      ) {
        issues.push({
          code: `items[${i}]/items[${j}]`,
          message: `Ô ${normalizeSpaceCode(a.spaceCode)} đè lên ô ${normalizeSpaceCode(b.spaceCode)}.`,
        });
      }
    }
  }

  return issues;
}

export function validateLayoutElements(elements: LayoutElementApi[]): LayoutIssue[] {
  const issues: LayoutIssue[] = [];
  elements.forEach((element, index) => {
    const type = (element.elementType ?? "").toUpperCase();
    if (!(ALLOWED_ELEMENT_TYPES as readonly string[]).includes(type)) {
      issues.push({
        code: `elements[${index}].elementType`,
        message: `Loại phần tử không hỗ trợ: ${element.elementType}.`,
      });
      return;
    }
    const geometryText = JSON.stringify(element.geometry ?? null);
    if (geometryText.length > MAX_GEOMETRY_CHARS) {
      issues.push({
        code: `elements[${index}].geometry`,
        message: "Hình học phần tử quá lớn.",
      });
    }
    if (/NaN|Infinity/.test(geometryText)) {
      issues.push({
        code: `elements[${index}].geometry`,
        message: "Hình học chỉ được chứa số hữu hạn.",
      });
    }
    const styleText = JSON.stringify(element.style ?? null);
    if (styleText.length > MAX_STYLE_CHARS) {
      issues.push({ code: `elements[${index}].style`, message: "Style phần tử quá lớn." });
    }
    const lowered = styleText.toLowerCase();
    const unsafe = UNSAFE_TOKENS.find((token) => lowered.includes(token));
    if (unsafe) {
      issues.push({
        code: `elements[${index}].style`,
        message: "Style chứa nội dung không an toàn.",
      });
    }
    if (element.label && element.label.length > MAX_LABEL_CHARS) {
      issues.push({ code: `elements[${index}].label`, message: "Nhãn tối đa 200 ký tự." });
    }
    if (/[<>]/.test(element.label ?? "")) {
      issues.push({ code: `elements[${index}].label`, message: "Nhãn không được chứa ký tự < >." });
    }
    if ((element.displayOrder ?? 0) < 0) {
      issues.push({
        code: `elements[${index}].displayOrder`,
        message: "Thứ tự lớp phải >= 0.",
      });
    }
    const imageUrl = readImageUrl(element);
    if (imageUrl && !isSafeImageSource(imageUrl)) {
      issues.push({
        code: `elements[${index}].image`,
        message: "Ảnh nền chỉ chấp nhận object key/URL http(s) an toàn.",
      });
    }
  });
  return issues;
}

function readImageUrl(element: LayoutElementApi): string | null {
  const geometry = element.geometry as Record<string, unknown> | null;
  const style = element.style as Record<string, unknown> | null;
  const candidate =
    (geometry?.["src"] as string | undefined) ??
    (geometry?.["url"] as string | undefined) ??
    (style?.["backgroundImage"] as string | undefined);
  return typeof candidate === "string" && candidate.trim() ? candidate.trim() : null;
}

export function isSafeImageSource(value: string): boolean {
  const trimmed = value.trim();
  if (!trimmed || trimmed.length > 2000) return false;
  if (/^data:/i.test(trimmed)) return false;
  if (/^javascript:/i.test(trimmed)) return false;
  if (/^https?:\/\//i.test(trimmed)) return true;
  if (/^[\w\-./]+$/.test(trimmed) && !trimmed.includes("..")) return true;
  return false;
}

export function summarizeDiff(
  draftItems: LayoutSpaceItemApi[],
  publishedItems: LayoutSpaceItemApi[],
): { added: number; updated: number; removed: number } {
  const publishedByCode = new Map(publishedItems.map((item) => [normalizeSpaceCode(item.spaceCode), item]));
  const draftByCode = new Map(draftItems.map((item) => [normalizeSpaceCode(item.spaceCode), item]));
  let added = 0;
  let updated = 0;
  for (const [code] of draftByCode) {
    if (publishedByCode.has(code)) updated += 1;
    else added += 1;
  }
  let removed = 0;
  for (const [code] of publishedByCode) {
    if (!draftByCode.has(code)) removed += 1;
  }
  return { added, updated, removed };
}
