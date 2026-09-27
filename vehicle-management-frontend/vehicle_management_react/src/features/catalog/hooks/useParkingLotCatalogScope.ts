import { useEffect, useMemo, useState } from "react";
import {
  getParkingLotCatalogAvailability,
  setParkingLotTicketTypeEnabled,
  setParkingLotVehicleTypeEnabled,
  type ParkingLotCatalogAvailability,
} from "@/features/catalog/api/parkingLotCatalogAvailabilityApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";

export function useParkingLotCatalogScope() {
  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [selectedLotId, setSelectedLotId] = useState(() => sessionStorage.getItem("catalog.selectedParkingLotId") ?? "");
  const [availability, setAvailability] = useState<ParkingLotCatalogAvailability | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    void getParkingLots()
      .then((response) => {
        if (!mounted) return;
        const nextLots = response.data ?? [];
        setLots(nextLots);
        setSelectedLotId((current) => nextLots.some((lot) => lot.parkingLotId === current)
          ? current : nextLots[0]?.parkingLotId ?? "");
        if (nextLots.length === 0) setLoading(false);
      })
      .catch((reason: unknown) => {
        if (!mounted) return;
        setError(reason instanceof Error ? reason.message : "Không thể tải danh sách bãi xe.");
        setLoading(false);
      });
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    if (!selectedLotId) return;
    sessionStorage.setItem("catalog.selectedParkingLotId", selectedLotId);
    let mounted = true;
    setAvailability(null);
    setLoading(true);
    setError("");
    void getParkingLotCatalogAvailability(selectedLotId)
      .then((response) => { if (mounted) setAvailability(response.data); })
      .catch((reason: unknown) => {
        if (mounted) setError(reason instanceof Error ? reason.message : "Không thể tải cấu hình danh mục của bãi xe.");
      })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [selectedLotId]);

  const selectedLot = useMemo(
    () => lots.find((lot) => lot.parkingLotId === selectedLotId) ?? null,
    [lots, selectedLotId],
  );

  const updateAvailability = async (kind: "vehicle" | "ticket", id: string, enabled: boolean) => {
    if (!selectedLot) throw new Error("Vui lòng chọn bãi xe trước.");
    const response = kind === "vehicle"
      ? await setParkingLotVehicleTypeEnabled(selectedLot.parkingLotId, id, enabled)
      : await setParkingLotTicketTypeEnabled(selectedLot.parkingLotId, id, enabled);
    setAvailability(response.data);
  };

  return {
    availability: availability?.parkingLotId === selectedLotId ? availability : null,
    error,
    loading,
    lots,
    selectedLot,
    selectedLotId,
    setSelectedLotId,
    updateAvailability,
  };
}
