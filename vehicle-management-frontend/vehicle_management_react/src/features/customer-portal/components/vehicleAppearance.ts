export type VehicleKind = "car" | "motorcycle" | "truck" | "bicycle" | "other";

export function normalizeVehicleText(value?: string | null) {
  return value?.trim().toLocaleLowerCase("vi-VN").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").replace(/[_-]+/g, " ").replace(/\s+/g, " ") ?? "";
}

/** Match specific categories first: "ô tô tải" must never become a passenger car. */
export function resolveVehicleKind(typeName?: string | null): VehicleKind {
  const type = normalizeVehicleText(typeName);
  if (/\b(xe tai|tai nho|o to tai|light truck|truck|pickup|ban tai)\b/.test(type)) return "truck";
  if (/\b(xe dap|bicycle|cycle)\b/.test(type)) return "bicycle";
  if (/\b(xe may|xe ga|tay ga|xe so|mo to|motorbike|motorcycle|scooter|motor)\b/.test(type)) return "motorcycle";
  if (/\b(o to|xe hoi|car|sedan|suv)\b/.test(type)) return "car";
  return "other";
}

const paints = [
  { names: ["do do", "burgundy", "maroon"], hex: "#782431" },
  { names: ["xanh den", "xanh navy", "navy"], hex: "#213553" },
  { names: ["xanh la", "xanh luc", "green"], hex: "#26834c" },
  { names: ["xanh ngoc", "turquoise", "teal"], hex: "#259c9b" },
  { names: ["do", "red"], hex: "#ce2436" },
  { names: ["xanh duong", "xanh bien", "xanh lam", "xanh", "blue"], hex: "#216fce" },
  { names: ["vang", "yellow", "gold"], hex: "#e9b323" },
  { names: ["tim", "purple", "violet"], hex: "#8748ae" },
  { names: ["cam", "orange"], hex: "#e47826" },
  { names: ["hong", "pink"], hex: "#dc699a" },
  { names: ["den", "black"], hex: "#252930" },
  { names: ["trang", "white"], hex: "#e7eaee" },
  { names: ["bac", "silver"], hex: "#b8c1ca" },
  { names: ["xam", "ghi", "gray", "grey"], hex: "#808995" },
  { names: ["nau", "brown"], hex: "#845839" },
  { names: ["be", "kem", "beige", "cream"], hex: "#d6c6a4" },
];

export function resolveVehiclePaint(color?: string | null) {
  const raw = color?.trim() ?? "";
  if (/^#[\da-f]{6}$/i.test(raw)) return { hex: raw.toLowerCase(), recognized: true };
  if (/^#[\da-f]{3}$/i.test(raw)) return { hex: `#${raw.slice(1).split("").map((value) => value + value).join("")}`.toLowerCase(), recognized: true };
  const value = normalizeVehicleText(raw);
  // Compound descriptions use their first named color, with the longest matching name winning.
  const matches = paints.flatMap((paint) => paint.names.flatMap((name) => {
    const match = new RegExp(`\\b${name}\\b`).exec(value);
    return match ? [{ ...paint, index: match.index, length: name.length }] : [];
  })).sort((a, b) => a.index - b.index || b.length - a.length);
  return { hex: matches[0]?.hex ?? "#aab3be", recognized: matches.length > 0 };
}

/** Preserve shading and neutral highlights; never rotate the colors of glass or tires. */
export function paintChannelRamp(hex: string, channel: number) {
  const value = parseInt(hex.slice(1 + channel * 2, 3 + channel * 2), 16) / 255;
  return [value * .04, value * .18, value * .44, value * .75, value * .96, value + (1 - value) * .72].map((n) => n.toFixed(4)).join(" ");
}
