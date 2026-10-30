import { useEffect, useMemo } from "react";
import L from "leaflet";
import { Circle, MapContainer, Marker, TileLayer, Tooltip, useMap, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";

import type { NearbyParkingLot } from "@/features/parking/api/publicParkingApi";

export type MapPoint = {
  latitude: number;
  longitude: number;
  accuracy?: number;
};

type Props = {
  lots: NearbyParkingLot[];
  onMapError: () => void;
  onPickLocation: (point: MapPoint) => void;
  onSelectLot: (parkingLotId: string) => void;
  pickingEnabled: boolean;
  radiusKm: number;
  selectedLotId: string | null;
  userLocation: MapPoint | null;
};

const VIETNAM_CENTER: [number, number] = [16.047079, 108.20623];
const METERS_PER_KILOMETER = 1_000;

function getLotMarkerColor(lot: NearbyParkingLot, selectedLotId: string | null) {
  if (lot.parkingLotId === selectedLotId) return "#dc2626";
  if (lot.status === "ACTIVE") return "#0f766e";
  if (lot.status === "SETUP") return "#d97706";
  return "#475569";
}

function getLotStatusLabel(status: NearbyParkingLot["status"]) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "SETUP") return "Đang thiết lập";
  if (status === "MAINTENANCE") return "Đang bảo trì";
  return "Đã đóng";
}

function ViewportController({
  lots,
  radiusKm,
  selectedLotId,
  userLocation,
}: Pick<Props, "lots" | "radiusKm" | "selectedLotId" | "userLocation">) {
  const map = useMap();

  useEffect(() => {
    const selectedLot = lots.find((lot) => lot.parkingLotId === selectedLotId);
    if (selectedLot) {
      map.flyTo([selectedLot.latitude, selectedLot.longitude], Math.max(map.getZoom(), 16), { duration: 0.5 });
      return;
    }

    if (userLocation) {
      const searchBounds = L.latLng(
        userLocation.latitude,
        userLocation.longitude,
      ).toBounds(radiusKm * METERS_PER_KILOMETER * 2);
      map.fitBounds(searchBounds, { padding: [36, 36] });
    }
  }, [lots, map, radiusKm, selectedLotId, userLocation]);

  return null;
}

function PointPicker({ enabled, onPick }: { enabled: boolean; onPick: Props["onPickLocation"] }) {
  useMapEvents({
    click(event) {
      if (enabled) {
        onPick({ latitude: event.latlng.lat, longitude: event.latlng.lng });
      }
    },
  });
  return null;
}

export function NearbyParkingMap({
  lots,
  onMapError,
  onPickLocation,
  onSelectLot,
  pickingEnabled,
  radiusKm,
  selectedLotId,
  userLocation,
}: Props) {
  const userIcon = useMemo(() => L.divIcon({
    className: "",
    html: '<div aria-hidden="true" style="display:grid;place-items:center;width:34px;height:34px;border:3px solid white;border-radius:999px;background:#2563eb;color:white;font-size:17px;box-shadow:0 3px 12px rgba(15,23,42,.35)">●</div>',
    iconAnchor: [17, 17],
  }), []);

  const lotIcons = useMemo(() => new Map(lots.map((lot, index) => [
    lot.parkingLotId,
    L.divIcon({
      className: "",
      html: `<div aria-hidden="true" style="display:grid;place-items:center;width:36px;height:42px;border:3px solid white;border-radius:18px 18px 18px 4px;transform:rotate(-45deg);background:${getLotMarkerColor(lot, selectedLotId)};color:white;font-weight:900;box-shadow:0 3px 12px rgba(15,23,42,.35)"><span style="transform:rotate(45deg)">${index + 1}</span></div>`,
      iconAnchor: [18, 40],
    }),
  ])), [lots, selectedLotId]);

  return (
    <MapContainer
      center={userLocation ? [userLocation.latitude, userLocation.longitude] : VIETNAM_CENTER}
      zoom={userLocation ? 15 : 6}
      scrollWheelZoom
      className={`tw-h-full tw-min-h-[360px] tw-w-full ${pickingEnabled ? "tw-cursor-crosshair" : ""}`}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        eventHandlers={{ tileerror: onMapError }}
      />
      <ViewportController
        lots={lots}
        radiusKm={radiusKm}
        selectedLotId={selectedLotId}
        userLocation={userLocation}
      />
      <PointPicker enabled={pickingEnabled} onPick={onPickLocation} />
      {userLocation ? (
        <>
          <Circle
            center={[userLocation.latitude, userLocation.longitude]}
            radius={radiusKm * METERS_PER_KILOMETER}
            pathOptions={{ color: "#2563eb", fillColor: "#93c5fd", fillOpacity: 0.15, weight: 1 }}
          />
          <Marker
            alt="Vị trí tìm kiếm của bạn"
            icon={userIcon}
            keyboard
            position={[userLocation.latitude, userLocation.longitude]}
            title="Vị trí tìm kiếm của bạn"
          >
            <Tooltip>Vị trí tìm kiếm của bạn</Tooltip>
          </Marker>
        </>
      ) : null}
      {lots.map((lot, index) => (
        <Marker
          alt={`Bãi xe ${index + 1}: ${lot.name}`}
          icon={lotIcons.get(lot.parkingLotId)}
          keyboard
          key={lot.parkingLotId}
          position={[lot.latitude, lot.longitude]}
          title={`Bãi xe ${index + 1}: ${lot.name}`}
          eventHandlers={{ click: () => onSelectLot(lot.parkingLotId) }}
        >
          <Tooltip>{index + 1}. {lot.name} · {getLotStatusLabel(lot.status)}</Tooltip>
        </Marker>
      ))}
    </MapContainer>
  );
}
