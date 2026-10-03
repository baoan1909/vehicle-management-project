import { useEffect, useMemo } from "react";
import L from "leaflet";
import { MapContainer, Marker, TileLayer, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";

type Props = {
  latitude: number;
  longitude: number;
  onMarkerMoved: (latitude: number, longitude: number) => void;
};

function Recenter({ latitude, longitude }: Omit<Props, "onMarkerMoved">) {
  const map = useMap();
  useEffect(() => { map.setView([latitude, longitude], map.getZoom()); }, [latitude, longitude, map]);
  return null;
}

export function ParkingLocationMap({ latitude, longitude, onMarkerMoved }: Props) {
  const icon = useMemo(() => L.divIcon({
    className: "",
    html: '<div style="font-size:30px;filter:drop-shadow(0 2px 2px rgba(0,0,0,.35))">📍</div>',
    iconAnchor: [15, 30],
  }), []);

  return (
    <div className="tw-h-64 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-200">
      <MapContainer center={[latitude, longitude]} zoom={17} scrollWheelZoom className="tw-h-full tw-w-full">
        <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <Recenter latitude={latitude} longitude={longitude} />
        <Marker draggable icon={icon} position={[latitude, longitude]} eventHandlers={{
          dragend: (event) => {
            const point = event.target.getLatLng();
            onMarkerMoved(point.lat, point.lng);
          },
        }} />
      </MapContainer>
    </div>
  );
}
