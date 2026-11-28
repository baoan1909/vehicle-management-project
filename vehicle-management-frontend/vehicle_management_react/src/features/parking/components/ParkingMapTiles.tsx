import { useEffect, useState } from "react";
import { TileLayer, useMap } from "react-leaflet";

const primaryUrl = import.meta.env.VITE_MAP_TILE_URL || "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const primaryAttribution = import.meta.env.VITE_MAP_TILE_ATTRIBUTION || '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const fallbackUrl = import.meta.env.VITE_MAP_TILE_FALLBACK_URL;
const fallbackAttribution = import.meta.env.VITE_MAP_TILE_FALLBACK_ATTRIBUTION;

/** Optional fallback must be configured together with the provider's attribution. */
export function ParkingMapTiles() {
  const map = useMap();
  const [fallback, setFallback] = useState(false);
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const observer = new ResizeObserver(() => map.invalidateSize());
    observer.observe(map.getContainer());
    return () => observer.disconnect();
  }, [map]);
  return <>
    <TileLayer
      key={`${fallback}-${attempt}`}
      url={fallback ? fallbackUrl! : primaryUrl}
      attribution={fallback ? fallbackAttribution! : primaryAttribution}
      eventHandlers={{
        tileerror: () => {
          if (!fallback && fallbackUrl && fallbackAttribution) { setFallback(true); setFailed(false); }
          else setFailed(true);
        },
      }}
    />
    {failed ? <div role="alert" className="tw-absolute tw-left-2 tw-right-2 tw-top-2 tw-z-[1000] tw-rounded-md tw-bg-white/95 tw-p-3 tw-text-sm tw-text-slate-800 tw-shadow">
      Không tải được nền bản đồ. Vui lòng kiểm tra kết nối mạng hoặc thử lại.
      <button type="button" className="tw-ml-2 tw-font-bold tw-text-blue-600" onClick={(event) => {
        event.stopPropagation(); setFailed(false); setFallback(false); setAttempt((value) => value + 1);
      }}>Thử lại bản đồ</button>
    </div> : null}
  </>;
}
