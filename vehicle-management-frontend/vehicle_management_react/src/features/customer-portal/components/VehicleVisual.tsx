import { useId } from "react";
import { VehiclePaintMask } from "./VehiclePaintMask";
import { paintChannelRamp, resolveVehicleKind, resolveVehiclePaint } from "./vehicleAppearance";
import { vehicleModels } from "./vehicleModels";

type VehicleVisualProps = {
  color?: string | null;
  size?: "compact" | "hero" | "card" | "dashboard";
  typeName?: string | null;
};

export function isMotorcycle(typeName?: string | null) {
  return resolveVehicleKind(typeName) === "motorcycle";
}

// One source image and one paint mask per category, independent of make/model/color.
export function VehicleVisual({ color, size = "compact", typeName }: VehicleVisualProps) {
  const id = useId();
  const kind = resolveVehicleKind(typeName);
  const paint = resolveVehiclePaint(color);
  const model = kind === "other" ? null : vehicleModels[kind];
  const sizeClass = size === "hero" ? "tw-h-[238px] tw-w-[430px]"
    : size === "dashboard" ? "tw-h-[158px] tw-w-[320px]"
    : size === "card" ? "tw-h-[115px] tw-w-[210px]" : "tw-h-[88px] tw-w-[160px]";
  const label = model?.label ?? typeName ?? "Phương tiện";
  const description = `Hình minh họa phương tiện${paint.recognized ? "" : " · Màu minh họa trung tính"}`;

  return (
    <span aria-label={`${label}${color ? ` màu ${color}` : ""}`} className={`tw-inline-grid tw-grid-rows-[minmax(0,1fr)] tw-max-w-full tw-min-w-0 tw-place-items-center ${sizeClass}`} role="img" title={description}>
      {model ? (
        <svg aria-hidden="true" className={`tw-min-h-0 tw-h-full tw-w-full ${size === "dashboard" ? "[transform:scale(1.35)]" : ""}`} viewBox="0 0 1536 1024" preserveAspectRatio="xMidYMid meet">
          <defs>
            <mask id={`${id}-paint-mask`} maskUnits="userSpaceOnUse" x="0" y="0" width="1536" height="1024">
              <VehiclePaintMask kind={kind} />
            </mask>
            <filter id={`${id}-paint`} x="0" y="0" width="100%" height="100%" colorInterpolationFilters="sRGB">
              <feColorMatrix type="saturate" values="0" />
              <feComponentTransfer>
                <feFuncR type="linear" slope={model.luminanceGain} />
                <feFuncG type="linear" slope={model.luminanceGain} />
                <feFuncB type="linear" slope={model.luminanceGain} />
              </feComponentTransfer>
              <feComponentTransfer>
                <feFuncR type="table" tableValues={paintChannelRamp(paint.hex, 0)} />
                <feFuncG type="table" tableValues={paintChannelRamp(paint.hex, 1)} />
                <feFuncB type="table" tableValues={paintChannelRamp(paint.hex, 2)} />
              </feComponentTransfer>
            </filter>
            {model.whiteBackground ? (
              <filter id={`${id}-cutout`} x="0" y="0" width="100%" height="100%" colorInterpolationFilters="sRGB">
                <feColorMatrix type="matrix" values="1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  -10 -10 -10 0 29" />
              </filter>
            ) : null}
          </defs>
          <image href={model.image} width="1536" height="1024" filter={model.whiteBackground ? `url(#${id}-cutout)` : undefined} />
          <g mask={`url(#${id}-paint-mask)`}>
            <image href={model.image} width="1536" height="1024" filter={`url(#${id}-paint)`} />
          </g>
        </svg>
      ) : <i aria-hidden="true" className="fas fa-parking tw-text-5xl tw-text-slate-400" />}
    </span>
  );
}
