import { useEffect, useMemo, useState } from "react";

import { Button, Drawer } from "@/components/ui";
import type { EligibleParkingSpace } from "@/features/parking/api/parkingSessionApi";
import { cn } from "@/lib/cn";

type ParkingSpaceSelectionDrawerProps = {
  confirmedParkingSpaceId?: string;
  levelLabel?: string;
  onClose: () => void;
  onConfirm: (space: EligibleParkingSpace) => void;
  open: boolean;
  spaces: EligibleParkingSpace[];
  zoneLabel?: string;
};

export function ParkingSpaceSelectionDrawer({
  confirmedParkingSpaceId,
  levelLabel,
  onClose,
  onConfirm,
  open,
  spaces,
  zoneLabel,
}: ParkingSpaceSelectionDrawerProps) {
  const availableSpaces = useMemo(
    () => spaces.filter((space) => space.status === "AVAILABLE"),
    [spaces],
  );
  const validConfirmedId = availableSpaces.some(
    (space) => space.parkingSpaceId === confirmedParkingSpaceId,
  )
    ? confirmedParkingSpaceId
    : undefined;
  const [candidateId, setCandidateId] = useState<string | undefined>(validConfirmedId);

  useEffect(() => {
    if (open) setCandidateId(validConfirmedId);
  }, [open, validConfirmedId]);

  const candidate = availableSpaces.find((space) => space.parkingSpaceId === candidateId);
  const location = [zoneLabel, levelLabel].filter(Boolean).join(" • ");

  return (
    <Drawer
      actions={(
        <div className="tw-grid tw-grid-cols-2 tw-gap-2">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button disabled={!candidate} onClick={() => candidate && onConfirm(candidate)}>
            {candidate ? `Dùng ô ${candidate.code}` : "Chọn một ô"}
          </Button>
        </div>
      )}
      description={`${location || "Khu vực hiện tại"} • ${availableSpaces.length} ô hợp lệ`}
      onClose={onClose}
      open={open}
      title="Chọn ô đỗ"
      width="lg"
    >
      <div className="tw-grid tw-grid-cols-2 tw-gap-3 sm:tw-grid-cols-3">
        {availableSpaces.map((space) => {
          const selected = space.parkingSpaceId === candidateId;
          return (
            <button
              aria-pressed={selected}
              className={cn(
                "tw-grid tw-min-h-[92px] tw-gap-1 tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left tw-transition focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus",
                selected
                  ? "tw-border-brand-300 tw-bg-brand-50 tw-text-vm-primary"
                  : "tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-800 hover:tw-border-brand-200",
              )}
              key={space.parkingSpaceId}
              type="button"
              onClick={() => setCandidateId(space.parkingSpaceId)}
            >
              <strong className="tw-text-[1rem] tw-font-black">{space.code}</strong>
              <span className="tw-text-[0.7rem] tw-font-bold tw-text-emerald-600">Sẵn sàng</span>
              {selected ? <span className="tw-text-[0.68rem] tw-font-extrabold">Đang chọn</span> : null}
            </button>
          );
        })}
      </div>
      {!availableSpaces.length ? (
        <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-4 tw-text-[0.82rem] tw-font-bold tw-text-amber-700">
          Không còn ô khả dụng. Vui lòng đóng drawer và kiểm tra lại availability.
        </div>
      ) : null}
    </Drawer>
  );
}
