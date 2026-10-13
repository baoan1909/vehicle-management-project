import { useEffect, useMemo } from "react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";

import { cn } from "@/lib/cn";

type Props = {
  className?: string;
  interactive?: boolean;
  latitude: number;
  longitude: number;
  onMarkerMoved?: (latitude: number, longitude: number) => void;
};

function Recenter({ latitude, longitude }: Omit<Props, "onMarkerMoved">) {
  const map = useMap();
  useEffect(() => { map.setView([latitude, longitude], map.getZoom()); }, [latitude, longitude, map]);
  return null;
}

export function ParkingLocationMap({ className, interactive = true, latitude, longitude, onMarkerMoved }: Props) {
  const icon = useMemo(() => L.divIcon({
    className: "",
    html: '<div style="display:flex;width:36px;height:44px;align-items:center;justify-content:center;border-radius:18px 18px 18px 4px;background:#2563eb;color:#fff;box-shadow:0 8px 18px rgba(37,99,235,.32);font:800 18px Inter,sans-serif;transform:rotate(-45deg)"><span style="transform:rotate(45deg)">P</span></div>',
    iconSize: [36, 44],
    iconAnchor: [18, 42],
  }), []);

  return (
    <div className={cn("tw-h-64 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200", className)}>
      <MapContainer
        center={[latitude, longitude]}
        zoom={17}
        dragging={interactive}
        doubleClickZoom={interactive}
        scrollWheelZoom={interactive}
        touchZoom={interactive}
        zoomControl={interactive}
        className="tw-h-full tw-w-full"
      >
        <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <Recenter latitude={latitude} longitude={longitude} />
        <Marker draggable={interactive} icon={icon} position={[latitude, longitude]} eventHandlers={{
          dragend: (event) => {
            const point = event.target.getLatLng();
            onMarkerMoved?.(point.lat, point.lng);
          },
        }} />
      </MapContainer>
    </div>
  );
}
