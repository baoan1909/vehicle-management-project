import { UnderlineTabs } from "@/shared/components/ui/UnderlineTabs";

export type ParkingOperationMode = "check-in" | "check-out";

type OperationModeTabsProps = {
  mode: ParkingOperationMode;
  onChange: (mode: ParkingOperationMode) => void;
};

const modes: Array<{ label: string; value: ParkingOperationMode }> = [
  { label: "Check-in", value: "check-in" },
  { label: "Check-out", value: "check-out" },
];

export function OperationModeTabs({ mode, onChange }: OperationModeTabsProps) {
  return (
    <UnderlineTabs value={mode} onChange={onChange} tabs={modes} ariaLabel="Chế độ vận hành bãi xe" />
  );
}
