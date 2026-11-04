import { useEffect, useMemo, useState } from "react";
import {
  getParkingLotCatalogAvailability,
  setParkingLotTicketTypeEnabled,
  setParkingLotVehicleTypeEnabled,
  setParkingLotVehicleTypeEnabledForLots,
  type ParkingLotCatalogAvailability,
} from "@/features/catalog/api/parkingLotCatalogAvailabilityApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";

export function useParkingLotCatalogScope() {
  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [selectedLotId, setSelectedLotId] = useState(() => sessionStorage.getItem("catalog.selectedParkingLotId") ?? "");
  const [availabilityByLotId, setAvailabilityByLotId] = useState<Record<string, ParkingLotCatalogAvailability>>({});
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
    if (selectedLotId) sessionStorage.setItem("catalog.selectedParkingLotId", selectedLotId);
  }, [selectedLotId]);

  useEffect(() => {
    if (lots.length === 0) return;
    let mounted = true;
    setLoading(true);
    setError("");
    void Promise.all(lots.map((lot) => getParkingLotCatalogAvailability(lot.parkingLotId)))
      .then((responses) => {
        if (!mounted) return;
        setAvailabilityByLotId(Object.fromEntries(
          responses.map((response) => [response.data.parkingLotId, response.data]),
        ));
      })
      .catch((reason: unknown) => {
        if (mounted) setError(reason instanceof Error ? reason.message : "Không thể tải cấu hình danh mục của các bãi xe.");
      })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [lots]);

  const selectedLot = useMemo(
    () => lots.find((lot) => lot.parkingLotId === selectedLotId) ?? null,
    [lots, selectedLotId],
  );

  const updateAvailability = async (kind: "vehicle" | "ticket", id: string, enabled: boolean) => {
    if (!selectedLot) throw new Error("Vui lòng chọn bãi xe trước.");
    const response = kind === "vehicle"
      ? await setParkingLotVehicleTypeEnabled(selectedLot.parkingLotId, id, enabled)
      : await setParkingLotTicketTypeEnabled(selectedLot.parkingLotId, id, enabled);
    setAvailabilityByLotId((current) => ({
      ...current,
      [response.data.parkingLotId]: response.data,
    }));
  };

  const updateVehicleAvailabilityAtLot = async (parkingLotId: string, id: string, enabled: boolean) => {
    if (!lots.some((lot) => lot.parkingLotId === parkingLotId)) {
      throw new Error("Bãi xe không thuộc phạm vi bạn được phép quản lý.");
    }
    const response = await setParkingLotVehicleTypeEnabled(parkingLotId, id, enabled);
    setAvailabilityByLotId((current) => ({
      ...current,
      [response.data.parkingLotId]: response.data,
    }));
  };

  const updateVehicleAvailabilityForAllLots = async (id: string, enabled: boolean) => {
    if (!selectedLot) throw new Error("Vui lòng chọn bãi xe trước.");
    const parkingLotIds = lots.map((lot) => lot.parkingLotId);
    if (parkingLotIds.length === 0) throw new Error("Không có bãi xe nào trong phạm vi áp dụng.");
    const response = await setParkingLotVehicleTypeEnabledForLots(parkingLotIds, id, enabled);
    setAvailabilityByLotId((current) => ({
      ...current,
      ...Object.fromEntries(response.data.map((item) => [item.parkingLotId, item])),
    }));
  };

  const availability = selectedLotId ? availabilityByLotId[selectedLotId] ?? null : null;

  return {
    availability,
    availabilities: Object.values(availabilityByLotId),
    error,
    loading,
    lots,
    selectedLot,
    selectedLotId,
    setSelectedLotId,
    updateAvailability,
    updateVehicleAvailabilityAtLot,
    updateVehicleAvailabilityForAllLots,
  };
}
