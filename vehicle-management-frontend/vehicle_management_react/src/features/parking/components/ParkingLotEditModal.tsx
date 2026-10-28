import { useEffect, useState } from "react";

import { Modal } from "@/components/ui";
import {
  confirmParkingLotLocation,
  geocodeParkingLot,
  updateParkingLot,
  type ParkingLotApiResponse,
} from "@/features/parking/api/parkingLotsApi";
import { ParkingLotInfoForm, type ParkingLotInfoValue } from "@/features/parking/components/ParkingLotInfoForm";

function toInitialValue(lot: ParkingLotApiResponse): ParkingLotInfoValue {
  return {
    addressDisplay: lot.addressDisplay ?? "",
    addressInputScheme: lot.addressInputScheme ?? "CURRENT",
    currentWardCode: lot.currentWardCode,
    latitude: lot.latitude,
    legacyWardCode: lot.legacyWardCode,
    locationResolution: lot.geocodingStatus === "MANUAL_CONFIRMED"
      ? "MANUAL"
      : lot.geocodingStatus === "RESOLVED"
        ? "GEOCODED"
        : "NONE",
    longitude: lot.longitude,
    name: lot.name,
    totalCapacity: Number(lot.totalCapacity ?? 0),
  };
}

export function ParkingLotEditModal({ lot, open, onClose, onCompleted }: {
  lot: ParkingLotApiResponse | null;
  open: boolean;
  onClose: () => void;
  onCompleted: (lot: ParkingLotApiResponse) => void;
}) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setSaving(false);
    setError(null);
  }, [open, lot?.parkingLotId]);

  async function handleSubmit(value: ParkingLotInfoValue) {
    if (!lot || saving) return;
    setSaving(true);
    setError(null);
    try {
      let saved = (await updateParkingLot(lot.parkingLotId, {
        addressDisplay: value.addressDisplay,
        addressInputScheme: value.addressInputScheme,
        currentWardCode: value.currentWardCode,
        latitude: value.latitude,
        legacyWardCode: value.legacyWardCode,
        longitude: value.longitude,
        name: value.name,
        totalCapacity: value.totalCapacity,
      })).data;

      if (value.latitude !== null && value.longitude !== null) {
        if (value.locationResolution === "MANUAL") {
          saved = (await confirmParkingLotLocation(lot.parkingLotId, value.latitude, value.longitude)).data;
        } else if (value.locationResolution === "GEOCODED") {
          saved = (await geocodeParkingLot(lot.parkingLotId)).data;
        }
      }
      onCompleted(saved);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể cập nhật bãi xe.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open && Boolean(lot)}
      width="lg"
      title="Sửa thông tin bãi xe"
      description={lot ? `${lot.code} — ${lot.name}` : undefined}
      onClose={() => { if (!saving) onClose(); }}
    >
      {lot ? (
        <ParkingLotInfoForm
          formKey={`edit-${lot.parkingLotId}`}
          initial={toInitialValue(lot)}
          submitLabel="Lưu thay đổi"
          submitting={saving}
          serverError={error}
          statusMeta={{ geocodingStatus: lot.geocodingStatus, status: lot.status }}
          onSubmit={(value) => void handleSubmit(value)}
          onClose={onClose}
        />
      ) : null}
    </Modal>
  );
}
