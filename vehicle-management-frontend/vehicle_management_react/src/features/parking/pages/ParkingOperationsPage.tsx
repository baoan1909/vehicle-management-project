import { useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, SelectMenu } from "@/components/ui";
import { cn } from "@/lib/cn";

type ParkingLotStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";
type ZoneStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";
type GateStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED";
type LaneStatus = "ACTIVE" | "MAINTENANCE" | "CLOSED" | "OVERLOAD";
type LaneDirection = "IN" | "OUT" | "VIP";
type DrawerPhase = "opening" | "open" | "closing";

type ParkingLot = {
  address: string;
  code: string;
  id: string;
  name: string;
  sessions: number;
  status: ParkingLotStatus;
  totalCapacity: number;
  used: number;
};

type Zone = {
  capacity: number;
  code: string;
  id: string;
  name: string;
  parkingLotId: string;
  status: ZoneStatus;
  used: number;
  vehicleType: "Xe máy" | "Ô tô" | "Hỗn hợp";
};

type Gate = {
  code: string;
  id: string;
  name: string;
  status: GateStatus;
  zoneId: string;
};

type Lane = {
  activeSessions: number;
  camera: string;
  code: string;
  deviceHealth: "Ổn định" | "Cần kiểm tra" | "Mất kết nối";
  direction: LaneDirection;
  gateId: string;
  id: string;
  name: string;
  rfid: string;
  status: LaneStatus;
  throughput: number;
  updatedAt: string;
};

type SelectedNode =
  | { id: string; kind: "zone"; label: string }
  | { id: string; kind: "gate"; label: string }
  | { id: string; kind: "lane"; label: string };

const DRAWER_ANIMATION_MS = 280;

const parkingLots: ParkingLot[] = [
  {
    address: "12 Nguyễn Văn Linh, Quận 7, TP. HCM",
    code: "CP-LOT-A",
    id: "lot-a",
    name: "CP-Lot A - Trung tâm",
    sessions: 184,
    status: "ACTIVE",
    totalCapacity: 640,
    used: 428,
  },
  {
    address: "88 Võ Chí Công, TP. Thủ Đức",
    code: "CP-LOT-B",
    id: "lot-b",
    name: "CP-Lot B - Thủ Đức",
    sessions: 97,
    status: "MAINTENANCE",
    totalCapacity: 420,
    used: 231,
  },
  {
    address: "22 Cộng Hòa, Tân Bình",
    code: "CP-LOT-C",
    id: "lot-c",
    name: "CP-Lot C - Sân bay",
    sessions: 0,
    status: "CLOSED",
    totalCapacity: 260,
    used: 0,
  },
];

const zones: Zone[] = [
  { capacity: 500, code: "ZONE-A", id: "zone-a", name: "Khu A", parkingLotId: "lot-a", status: "ACTIVE", used: 342, vehicleType: "Xe máy" },
  { capacity: 400, code: "ZONE-B", id: "zone-b", name: "Khu B", parkingLotId: "lot-a", status: "ACTIVE", used: 265, vehicleType: "Ô tô" },
  { capacity: 300, code: "ZONE-C", id: "zone-c", name: "Khu C", parkingLotId: "lot-a", status: "MAINTENANCE", used: 120, vehicleType: "Hỗn hợp" },
  { capacity: 200, code: "ZONE-D", id: "zone-d", name: "Khu D", parkingLotId: "lot-a", status: "CLOSED", used: 0, vehicleType: "Xe máy" },
];

const gates: Gate[] = [
  { code: "GATE-A1", id: "gate-a1", name: "Cổng A1", status: "ACTIVE", zoneId: "zone-a" },
  { code: "GATE-A2", id: "gate-a2", name: "Cổng A2", status: "ACTIVE", zoneId: "zone-a" },
  { code: "GATE-B1", id: "gate-b1", name: "Cổng B1", status: "ACTIVE", zoneId: "zone-b" },
  { code: "GATE-B2", id: "gate-b2", name: "Cổng B2", status: "ACTIVE", zoneId: "zone-b" },
  { code: "GATE-C1", id: "gate-c1", name: "Cổng C1", status: "MAINTENANCE", zoneId: "zone-c" },
  { code: "GATE-C2", id: "gate-c2", name: "Cổng C2", status: "MAINTENANCE", zoneId: "zone-c" },
  { code: "GATE-D1", id: "gate-d1", name: "Cổng D1", status: "CLOSED", zoneId: "zone-d" },
];

const lanes: Lane[] = [
  {
    activeSessions: 42,
    camera: "CAM-A1-01",
    code: "LANE-A1-IN",
    deviceHealth: "Ổn định",
    direction: "IN",
    gateId: "gate-a1",
    id: "lane-a1-in",
    name: "Làn vào",
    rfid: "RFID-A1-IN",
    status: "ACTIVE",
    throughput: 126,
    updatedAt: "30/06/2026 09:15",
  },
  {
    activeSessions: 38,
    camera: "CAM-A1-02",
    code: "LANE-A1-OUT",
    deviceHealth: "Ổn định",
    direction: "OUT",
    gateId: "gate-a1",
    id: "lane-a1-out",
    name: "Làn ra",
    rfid: "RFID-A1-OUT",
    status: "ACTIVE",
    throughput: 118,
    updatedAt: "30/06/2026 09:12",
  },
  {
    activeSessions: 16,
    camera: "CAM-A2-01",
    code: "LANE-A2-VIP",
    deviceHealth: "Ổn định",
    direction: "VIP",
    gateId: "gate-a2",
    id: "lane-a2-vip",
    name: "Làn VIP",
    rfid: "RFID-A2-VIP",
    status: "ACTIVE",
    throughput: 54,
    updatedAt: "30/06/2026 09:10",
  },
  {
    activeSessions: 33,
    camera: "CAM-B1-01",
    code: "LANE-B1-IN",
    deviceHealth: "Cần kiểm tra",
    direction: "IN",
    gateId: "gate-b1",
    id: "lane-b1-in",
    name: "Làn vào",
    rfid: "RFID-B1-IN",
    status: "OVERLOAD",
    throughput: 164,
    updatedAt: "30/06/2026 09:08",
  },
  {
    activeSessions: 29,
    camera: "CAM-B1-02",
    code: "LANE-B1-OUT",
    deviceHealth: "Ổn định",
    direction: "OUT",
    gateId: "gate-b1",
    id: "lane-b1-out",
    name: "Làn ra",
    rfid: "RFID-B1-OUT",
    status: "ACTIVE",
    throughput: 139,
    updatedAt: "30/06/2026 09:05",
  },
  {
    activeSessions: 0,
    camera: "CAM-C1-01",
    code: "LANE-C1-IN",
    deviceHealth: "Mất kết nối",
    direction: "IN",
    gateId: "gate-c1",
    id: "lane-c1-in",
    name: "Làn vào",
    rfid: "RFID-C1-IN",
    status: "MAINTENANCE",
    throughput: 0,
    updatedAt: "30/06/2026 08:45",
  },
  {
    activeSessions: 26,
    camera: "CAM-D1-01",
    code: "LANE-D1-IN",
    deviceHealth: "Mất kết nối",
    direction: "IN",
    gateId: "gate-d1",
    id: "lane-d1-in",
    name: "Làn vào",
    rfid: "RFID-D1-IN",
    status: "CLOSED",
    throughput: 0,
    updatedAt: "30/06/2026 09:00",
  },
  {
    activeSessions: 0,
    camera: "CAM-D1-02",
    code: "LANE-D1-OUT",
    deviceHealth: "Mất kết nối",
    direction: "OUT",
    gateId: "gate-d1",
    id: "lane-d1-out",
    name: "Làn ra",
    rfid: "RFID-D1-OUT",
    status: "CLOSED",
    throughput: 0,
    updatedAt: "30/06/2026 09:00",
  },
  {
    activeSessions: 18,
    camera: "CAM-B2-01",
    code: "LANE-B2-IN",
    deviceHealth: "Ổn định",
    direction: "IN",
    gateId: "gate-b2",
    id: "lane-b2-in",
    name: "Làn vào",
    rfid: "RFID-B2-IN",
    status: "ACTIVE",
    throughput: 88,
    updatedAt: "30/06/2026 09:02",
  },
  {
    activeSessions: 14,
    camera: "CAM-B2-02",
    code: "LANE-B2-OUT",
    deviceHealth: "Ổn định",
    direction: "OUT",
    gateId: "gate-b2",
    id: "lane-b2-out",
    name: "Làn ra",
    rfid: "RFID-B2-OUT",
    status: "ACTIVE",
    throughput: 76,
    updatedAt: "30/06/2026 09:01",
  },
  {
    activeSessions: 0,
    camera: "CAM-C2-01",
    code: "LANE-C2-IN",
    deviceHealth: "Cần kiểm tra",
    direction: "IN",
    gateId: "gate-c2",
    id: "lane-c2-in",
    name: "Làn vào",
    rfid: "RFID-C2-IN",
    status: "MAINTENANCE",
    throughput: 0,
    updatedAt: "30/06/2026 08:45",
  },
  {
    activeSessions: 0,
    camera: "CAM-C2-02",
    code: "LANE-C2-OUT",
    deviceHealth: "Cần kiểm tra",
    direction: "OUT",
    gateId: "gate-c2",
    id: "lane-c2-out",
    name: "Làn ra",
    rfid: "RFID-C2-OUT",
    status: "MAINTENANCE",
    throughput: 0,
    updatedAt: "30/06/2026 08:45",
  },
];

const lotOptions = parkingLots.map((lot) => ({ label: `Bãi xe: ${lot.name}`, value: lot.id }));
const statusOptions = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Đang hoạt động", value: "ACTIVE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
  { label: "Đã đóng", value: "CLOSED" },
  { label: "Quá tải", value: "OVERLOAD" },
];
const vehicleTypeOptions = [
  { label: "Tất cả loại xe", value: "all" },
  { label: "Xe máy", value: "motorbike" },
  { label: "Ô tô", value: "car" },
  { label: "Hỗn hợp", value: "mixed" },
];

function statusTone(status: ParkingLotStatus | ZoneStatus | GateStatus | LaneStatus) {
  if (status === "ACTIVE") return "success";
  if (status === "MAINTENANCE" || status === "OVERLOAD") return "warning";
  return "neutral";
}

function statusLabel(status: ParkingLotStatus | ZoneStatus | GateStatus | LaneStatus) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "MAINTENANCE") return "Bảo trì";
  if (status === "OVERLOAD") return "Quá tải";
  return "Đã đóng";
}

function statusDotClassName(status: ParkingLotStatus | ZoneStatus | GateStatus | LaneStatus) {
  if (status === "ACTIVE") return "tw-bg-emerald-500";
  if (status === "MAINTENANCE") return "tw-bg-amber-500";
  if (status === "OVERLOAD") return "tw-bg-orange-500";
  return "tw-bg-red-500";
}

function topologyTone(status: ParkingLotStatus | ZoneStatus | GateStatus | LaneStatus) {
  if (status === "ACTIVE") {
    return {
      border: "tw-border-emerald-200",
      dashed: "tw-border-emerald-200",
      icon: "tw-bg-emerald-50 tw-text-emerald-600",
      progress: "tw-bg-vm-primary",
      soft: "tw-bg-emerald-50",
      text: "tw-text-emerald-600",
    };
  }

  if (status === "MAINTENANCE" || status === "OVERLOAD") {
    return {
      border: "tw-border-amber-200",
      dashed: "tw-border-amber-200",
      icon: "tw-bg-amber-50 tw-text-orange-500",
      progress: "tw-bg-orange-400",
      soft: "tw-bg-amber-50",
      text: "tw-text-orange-500",
    };
  }

  return {
    border: "tw-border-red-200",
    dashed: "tw-border-red-200",
    icon: "tw-bg-red-50 tw-text-red-500",
    progress: "tw-bg-slate-300",
    soft: "tw-bg-red-50",
    text: "tw-text-red-500",
  };
}

function laneIcon(direction: LaneDirection) {
  if (direction === "IN") return "fas fa-sign-in-alt";
  if (direction === "OUT") return "fas fa-sign-out-alt";
  return "fas fa-star";
}

function vehicleIcon(vehicleType: Zone["vehicleType"]) {
  if (vehicleType === "Xe máy") return "fas fa-motorcycle";
  if (vehicleType === "Ô tô") return "fas fa-car";
  return "fas fa-shuttle-van";
}

function ParkingMetricCard({
  delta,
  icon,
  label,
  tone,
  value,
}: {
  delta: string;
  icon: string;
  label: string;
  tone: "blue" | "green" | "amber" | "red";
  value: string;
}) {
  const toneClassName = {
    amber: "tw-bg-amber-50 tw-text-amber-600",
    blue: "tw-bg-brand-50 tw-text-vm-primary",
    green: "tw-bg-emerald-50 tw-text-emerald-600",
    red: "tw-bg-red-50 tw-text-red-600",
  }[tone];

  const sparklineClassName = {
    amber: "tw-stroke-amber-500",
    blue: "tw-stroke-vm-primary",
    green: "tw-stroke-emerald-500",
    red: "tw-stroke-red-500",
  }[tone];

  return (
    <Card className="tw-min-h-[116px] tw-p-4">
      <div className="tw-flex tw-items-start tw-gap-4">
        <span className={cn("tw-inline-flex tw-h-14 tw-w-14 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.28rem]", toneClassName)}>
          <i className={icon} />
        </span>
        <div className="tw-min-w-0">
          <p className="tw-m-0 tw-text-[0.88rem] tw-font-extrabold tw-text-vm-slate-700">{label}</p>
          <strong className="tw-mt-2 tw-block tw-text-[1.8rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
        </div>
      </div>
      <div className="tw-mt-4 tw-flex tw-items-end tw-justify-between tw-gap-3">
        <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-emerald-600">{delta}</span>
        <svg className="tw-h-8 tw-w-[96px]" viewBox="0 0 96 32" aria-hidden="true">
          <polyline
            className={cn("tw-fill-none tw-stroke-[2.5]", sparklineClassName)}
            points="2,24 12,18 22,21 32,13 42,17 52,10 62,15 72,7 82,12 94,4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </div>
    </Card>
  );
}

function LaneChip({ lane, onSelect, selected }: { lane: Lane; onSelect: () => void; selected: boolean }) {
  const tone = topologyTone(lane.status);
  const capacity = lane.status === "CLOSED" || lane.status === "MAINTENANCE" ? 40 : lane.direction === "VIP" ? 50 : 120;
  const used = Math.min(capacity, lane.status === "CLOSED" || lane.status === "MAINTENANCE" ? 0 : Math.max(10, Math.round(lane.throughput * 0.75)));
  const percent = Math.round((used / capacity) * 100);

  return (
    <button
      className={cn(
        "tw-flex tw-min-h-[118px] tw-w-full tw-min-w-0 tw-flex-col tw-items-center tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-center tw-transition",
        selected ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[0_0_0_3px_rgba(37,99,235,0.1)]" : "tw-border-vm-slate-100 hover:tw-border-brand-100 hover:tw-bg-brand-50/40",
      )}
      type="button"
      onClick={onSelect}
    >
      <i className={cn(laneIcon(lane.direction), "tw-text-[1.05rem]", lane.status === "CLOSED" ? "tw-text-red-500" : lane.status === "MAINTENANCE" ? "tw-text-orange-500" : lane.direction === "OUT" ? "tw-text-emerald-600" : "tw-text-vm-primary")} />
      <strong className="tw-mt-2 tw-text-[0.76rem] tw-font-extrabold tw-text-vm-slate-900">{lane.name}</strong>
      <span className={cn("tw-mt-1 tw-inline-flex tw-items-center tw-gap-1 tw-text-[0.58rem] tw-font-extrabold", tone.text)}>
        <span className={cn("tw-h-1.5 tw-w-1.5 tw-rounded-full", statusDotClassName(lane.status))} />
        {statusLabel(lane.status)}
      </span>
      <span className="tw-mt-auto tw-text-[0.7rem] tw-font-extrabold tw-text-vm-slate-900">{used} / {capacity}</span>
      <span className="tw-mt-2 tw-h-1.5 tw-w-full tw-overflow-hidden tw-rounded-full tw-bg-vm-slate-100">
        <span className={cn("tw-block tw-h-full tw-rounded-full", tone.progress)} style={{ width: `${percent}%` }} />
      </span>
    </button>
  );
}

function GateNode({
  gate,
  lanes,
  onSelect,
  selectedNode,
}: {
  gate: Gate;
  lanes: Lane[];
  onSelect: (node: SelectedNode) => void;
  selectedNode: SelectedNode | null;
}) {
  const selected = selectedNode?.kind === "gate" && selectedNode.id === gate.id;
  const tone = topologyTone(gate.status);
  const motorbikeCount = lanes.filter((lane) => lane.direction !== "VIP").length;
  const carCount = Math.max(0, lanes.length - motorbikeCount + (gate.id.endsWith("1") ? 1 : 0));

  return (
    <button
      className={cn(
        "tw-relative tw-min-h-[84px] tw-rounded-vm-md tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-shadow-[0_10px_22px_rgba(15,23,42,0.045)] tw-transition before:tw-absolute before:tw-left-1/2 before:tw-top-[-18px] before:tw-h-[18px] before:tw-w-px before:tw-bg-slate-300",
        selected ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[0_0_0_3px_rgba(37,99,235,0.1)]" : "tw-border-vm-slate-100 hover:tw-border-brand-100 hover:tw-bg-vm-slate-25",
      )}
      type="button"
      onClick={() => onSelect({ id: gate.id, kind: "gate", label: gate.name })}
    >
      <span className="tw-flex tw-items-center tw-gap-2.5">
        <span className={cn("tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-sm tw-text-[0.92rem]", tone.icon)}>
          <i className="fas fa-archway" />
        </span>
        <span className="tw-min-w-0">
          <strong className="tw-block tw-truncate tw-text-[0.83rem] tw-font-extrabold tw-text-vm-slate-900">{gate.name}</strong>
          <small className={cn("tw-inline-flex tw-items-center tw-gap-1 tw-text-[0.62rem] tw-font-extrabold", tone.text)}>
            <span className={cn("tw-h-1.5 tw-w-1.5 tw-rounded-full", statusDotClassName(gate.status))} />
            {statusLabel(gate.status)}
          </small>
        </span>
      </span>
      <span className="tw-mt-3 tw-flex tw-items-center tw-gap-4 tw-text-[0.8rem] tw-font-extrabold tw-text-vm-primary">
        <span><i className="fas fa-motorcycle tw-mr-1.5" />{motorbikeCount}</span>
        <span><i className="fas fa-car tw-mr-1.5" />{carCount}</span>
      </span>
    </button>
  );
}

function ZoneCard({
  gatesByZone,
  lanesByGate,
  onSelect,
  selectedNode,
  zone,
}: {
  gatesByZone: Map<string, Gate[]>;
  lanesByGate: Map<string, Lane[]>;
  onSelect: (node: SelectedNode) => void;
  selectedNode: SelectedNode | null;
  zone: Zone;
}) {
  const zoneGates = gatesByZone.get(zone.id) ?? [];
  const zoneLanes = zoneGates.flatMap((gate) => lanesByGate.get(gate.id) ?? []);
  const selected = selectedNode?.kind === "zone" && selectedNode.id === zone.id;
  const percent = Math.round((zone.used / zone.capacity) * 100);
  const tone = topologyTone(zone.status);

  return (
    <article className="tw-relative tw-min-w-[300px]">
      <button
        className={cn(
          "tw-relative tw-z-[2] tw-flex tw-min-h-[132px] tw-w-full tw-flex-col tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-4 tw-text-left tw-shadow-[0_16px_34px_rgba(15,23,42,0.05)] tw-transition",
          selected ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[0_0_0_3px_rgba(37,99,235,0.1)]" : "tw-border-vm-slate-100 hover:tw-border-brand-100",
        )}
        type="button"
        onClick={() => onSelect({ id: zone.id, kind: "zone", label: zone.name })}
      >
        <span className="tw-flex tw-items-start tw-gap-3">
          <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-sm tw-text-[1rem]", tone.icon)}>
            <i className={vehicleIcon(zone.vehicleType)} />
          </span>
          <span className="tw-min-w-0">
            <strong className="tw-block tw-text-[0.95rem] tw-font-extrabold tw-text-vm-slate-900">{zone.name}</strong>
            <small className={cn("tw-mt-1 tw-inline-flex tw-items-center tw-gap-1 tw-text-[0.62rem] tw-font-extrabold", tone.text)}>
              <span className={cn("tw-h-1.5 tw-w-1.5 tw-rounded-full", statusDotClassName(zone.status))} />
              {statusLabel(zone.status)}
            </small>
          </span>
        </span>

        <span className="tw-mt-4 tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">Sức chứa</span>
        <span className="tw-mt-2 tw-flex tw-items-center tw-gap-3">
          <span className="tw-h-2 tw-flex-1 tw-overflow-hidden tw-rounded-full tw-bg-vm-slate-100">
            <span className={cn("tw-block tw-h-full tw-rounded-full", tone.progress)} style={{ width: `${percent}%` }} />
          </span>
          <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-900">{zone.used} / {zone.capacity}</span>
        </span>
      </button>

      <div className="tw-relative tw-mx-auto tw-h-10 tw-w-px tw-bg-slate-300" />

      <div className="tw-relative tw-grid tw-grid-cols-2 tw-gap-3 before:tw-absolute before:tw-left-[25%] before:tw-right-[25%] before:tw-top-[-20px] before:tw-h-px before:tw-bg-slate-300 max-[720px]:tw-grid-cols-1">
        {zoneGates.map((gate) => (
          <GateNode key={gate.id} gate={gate} lanes={lanesByGate.get(gate.id) ?? []} selectedNode={selectedNode} onSelect={onSelect} />
        ))}
      </div>

      <div className="tw-relative tw-mx-auto tw-h-8 tw-w-px tw-bg-slate-300" />

      <div className={cn("tw-grid tw-grid-cols-[repeat(3,minmax(0,1fr))] tw-gap-2 tw-rounded-vm-lg tw-border tw-border-dashed tw-bg-white/80 tw-p-3", tone.dashed)}>
        {zoneLanes.map((lane) => (
          <LaneChip
            key={lane.id}
            lane={lane}
            selected={selectedNode?.kind === "lane" && selectedNode.id === lane.id}
            onSelect={() => onSelect({ id: lane.id, kind: "lane", label: lane.name })}
          />
        ))}
      </div>
    </article>
  );
}

function ParkingTopologyMap({
  gatesByZone,
  lanesByGate,
  onSelect,
  selectedNode,
  selectedParkingLot,
  zonesForLot,
}: {
  gatesByZone: Map<string, Gate[]>;
  lanesByGate: Map<string, Lane[]>;
  onSelect: (node: SelectedNode) => void;
  selectedNode: SelectedNode | null;
  selectedParkingLot: ParkingLot;
  zonesForLot: Zone[];
}) {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [zoom, setZoom] = useState(100);
  const zoomScale = zoom / 100;

  function zoomOut() {
    setZoom((current) => Math.max(80, current - 10));
  }

  function zoomIn() {
    setZoom((current) => Math.min(140, current + 10));
  }

  return (
    <Card
      className={cn(
        "tw-min-h-[690px] tw-overflow-hidden",
        isFullscreen ? "tw-fixed tw-inset-4 tw-z-[2100] tw-flex tw-flex-col tw-shadow-[0_24px_70px_rgba(15,23,42,0.22)]" : "",
      )}
    >
      <div className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-px-5 tw-py-5">
        <div>
          <h2 className="tw-m-0 tw-text-[1.18rem] tw-font-extrabold tw-text-vm-slate-900">Sơ đồ bãi {selectedParkingLot.name.split(" - ")[0]}</h2>
        </div>
        <div className="tw-flex tw-items-center tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white">
          <button
            className={cn("tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25", isFullscreen ? "tw-text-vm-primary" : "")}
            type="button"
            aria-label={isFullscreen ? "Thoát toàn màn hình" : "Phóng toàn màn hình"}
            aria-pressed={isFullscreen}
            onClick={() => setIsFullscreen((current) => !current)}
          >
            <i className={isFullscreen ? "fas fa-compress-arrows-alt" : "fas fa-expand-arrows-alt"} />
          </button>
          <button
            className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25 disabled:tw-opacity-40"
            type="button"
            aria-label="Thu nhỏ"
            disabled={zoom <= 80}
            onClick={zoomOut}
          >
            <i className="fas fa-minus" />
          </button>
          <span className="tw-inline-flex tw-h-9 tw-min-w-[62px] tw-items-center tw-justify-center tw-border-0 tw-border-r tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-900">{zoom}%</span>
          <button
            className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-border-0 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25 disabled:tw-opacity-40"
            type="button"
            aria-label="Phóng to"
            disabled={zoom >= 140}
            onClick={zoomIn}
          >
            <i className="fas fa-plus" />
          </button>
        </div>
      </div>

      <div className={cn("tw-relative tw-min-h-[620px] tw-overflow-auto tw-bg-white tw-px-5 tw-pb-5 tw-pt-1 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden", isFullscreen ? "tw-flex-1" : "")}>
        <div className="tw-relative" style={{ height: `${620 * zoomScale}px`, width: `${1320 * zoomScale}px` }}>
          <div
            className="tw-absolute tw-left-0 tw-top-0 tw-w-[1320px] tw-origin-top-left tw-transition-transform tw-duration-200"
            style={{ transform: `scale(${zoomScale})` }}
          >
            <div className="tw-absolute tw-left-[11%] tw-right-[11%] tw-top-[66px] tw-h-px tw-bg-slate-300" />
            <div className="tw-grid tw-grid-cols-4 tw-gap-8">
            {zonesForLot.map((zone) => (
              <ZoneCard key={zone.id} zone={zone} gatesByZone={gatesByZone} lanesByGate={lanesByGate} selectedNode={selectedNode} onSelect={onSelect} />
            ))}
            </div>
          </div>
        </div>
      </div>

      <div className="tw-mx-5 tw-mb-5 tw-flex tw-w-fit tw-flex-wrap tw-items-center tw-gap-8 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-3">
        {[
          ["Đang hoạt động", "tw-bg-emerald-500"],
          ["Bảo trì", "tw-bg-orange-500"],
          ["Đã đóng", "tw-bg-red-500"],
          ["Quá tải", "tw-bg-red-600"],
        ].map(([label, color]) => (
          <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-700" key={label}>
            <span className={cn("tw-h-2.5 tw-w-2.5 tw-rounded-full", color)} />
            {label}
          </span>
        ))}
      </div>
    </Card>
  );
}

function OperationSummary({ selectedParkingLot }: { selectedParkingLot: ParkingLot }) {
  const percent = Math.round((selectedParkingLot.used / selectedParkingLot.totalCapacity) * 100);
  const free = selectedParkingLot.totalCapacity - selectedParkingLot.used;

  return (
    <aside className="tw-grid tw-gap-4">
      <Card className="tw-flex tw-min-h-full tw-flex-col tw-p-5">
        <h2 className="tw-m-0 tw-text-[1.05rem] tw-font-extrabold tw-text-vm-slate-900">Tóm tắt vận hành</h2>

        <section className="tw-mt-5">
          <h3 className="tw-m-0 tw-text-[0.76rem] tw-font-extrabold tw-text-vm-slate-700">Tình trạng sử dụng</h3>
          <div className="tw-mt-4 tw-flex tw-items-center tw-gap-5">
            <div
              className="tw-relative tw-flex tw-h-[118px] tw-w-[118px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full"
              style={{ background: `conic-gradient(#2563EB 0 ${percent}%, #E2E8F0 ${percent}% 100%)` }}
            >
              <div className="tw-flex tw-h-[82px] tw-w-[82px] tw-flex-col tw-items-center tw-justify-center tw-rounded-full tw-bg-white">
                <strong className="tw-text-[1.28rem] tw-font-extrabold tw-text-vm-slate-900">{percent.toString().replace(".", ",")}%</strong>
                <span className="tw-text-[0.68rem] tw-font-extrabold tw-text-vm-slate-700">Đang sử dụng</span>
              </div>
            </div>
            <div className="tw-grid tw-flex-1 tw-gap-3">
              {[
                ["Đang dùng", selectedParkingLot.used.toLocaleString("vi-VN"), "tw-bg-vm-primary"],
                ["Còn trống", free.toLocaleString("vi-VN"), "tw-bg-vm-slate-300"],
                ["Tổng sức chứa", selectedParkingLot.totalCapacity.toLocaleString("vi-VN"), "tw-bg-vm-slate-400"],
              ].map(([label, value, color]) => (
                <div className="tw-flex tw-items-center tw-justify-between tw-gap-3" key={label}>
                  <span className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-700">
                    <span className={cn("tw-h-2 tw-w-2 tw-rounded-full", color)} />
                    {label}
                  </span>
                  <strong className="tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
                </div>
              ))}
            </div>
          </div>
        </section>

        <div className="tw-mt-5 tw-grid tw-gap-0 tw-divide-y tw-divide-vm-slate-100">
          {[
            ["Phiên đang hoạt động", "156", "far fa-id-badge", "tw-text-vm-primary"],
            ["Cổng hoạt động", "8 / 10", "fas fa-archway", "tw-text-vm-primary"],
            ["Làn đang hoạt động", "28 / 36", "fas fa-road", "tw-text-vm-primary"],
            ["Làn bảo trì", "2", "fas fa-tools", "tw-text-red-500"],
            ["Làn quá tải", "1", "fas fa-exclamation-triangle", "tw-text-red-500"],
          ].map(([label, value, icon]) => (
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-py-3" key={label}>
              <span className="tw-flex tw-items-center tw-gap-3 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">
                <i className={cn(icon, "tw-w-5 tw-text-center tw-text-[1rem]")} />
                {label}
              </span>
              <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
            </div>
          ))}
        </div>

        <section className="tw-mt-5">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">Cảnh báo gần đây</h3>
            <button className="tw-border-0 tw-bg-transparent tw-text-[0.72rem] tw-font-extrabold tw-text-vm-primary" type="button">Xem tất cả</button>
          </div>
          <div className="tw-mt-4 tw-grid tw-gap-4">
          {[
            ["Hôm nay 09:15", "Làn vào Cổng A1 đạt 90% công suất", "Cảnh báo", "tw-bg-orange-500", "primary"],
            ["Hôm nay 08:47", "Cổng C1 đang ở trạng thái bảo trì", "Bảo trì", "tw-bg-red-500", "danger"],
            ["Hôm qua 16:20", "Làn VIP Khu B hoạt động trở lại", "Thông báo", "tw-bg-emerald-500", "primary"],
          ].map(([time, text, badge, color, tone]) => (
            <div className="tw-flex tw-gap-3" key={text}>
              <span className={cn("tw-mt-1.5 tw-h-2 tw-w-2 tw-flex-shrink-0 tw-rounded-full", color)} />
              <div className="tw-min-w-0 tw-flex-1">
                <span className="tw-text-[0.7rem] tw-font-extrabold tw-text-vm-slate-500">{time}</span>
                <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-700">{text}</p>
              </div>
              <Badge tone={tone === "danger" ? "danger" : "primary"} className="tw-h-fit tw-rounded-vm-sm tw-px-2">{badge}</Badge>
            </div>
          ))}
          </div>
        </section>

        <div className="tw-mt-auto tw-flex tw-gap-3 tw-pt-5">
          <Button className="tw-flex-1" variant="secondary">
            <i className="fas fa-sync-alt" />
            Cập nhật
          </Button>
          <Button className="tw-flex-1 tw-border-red-200 tw-bg-white tw-text-red-600 hover:tw-bg-red-50" variant="secondary">
            <i className="fas fa-tools" />
            Đưa bảo trì
          </Button>
        </div>
      </Card>
    </aside>
  );
}

function ParkingNodeDrawer({
  gate,
  isOpen,
  lane,
  node,
  onClose,
  zone,
}: {
  gate?: Gate;
  isOpen: boolean;
  lane?: Lane;
  node: SelectedNode | null;
  onClose: () => void;
  zone?: Zone;
}) {
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");

  useEffect(() => {
    if (isOpen) {
      setIsRendered(true);
      setPhase("opening");
      const openTimer = window.setTimeout(() => setPhase("open"), DRAWER_ANIMATION_MS);
      return () => window.clearTimeout(openTimer);
    }

    if (!isRendered) return undefined;

    setPhase("closing");
    const closeTimer = window.setTimeout(() => setIsRendered(false), DRAWER_ANIMATION_MS);
    return () => window.clearTimeout(closeTimer);
  }, [isOpen, isRendered]);

  useEffect(() => {
    if (!isRendered) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isRendered, onClose]);

  if (!isRendered || !node) return null;

  const title = node.kind === "lane" ? "Chi tiết làn" : node.kind === "gate" ? "Chi tiết cổng" : "Chi tiết khu";
  const status = lane?.status ?? gate?.status ?? zone?.status ?? "ACTIVE";
  const code = lane?.code ?? gate?.code ?? zone?.code ?? "";

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="parking-node-drawer-title">
      <button
        type="button"
        aria-label="Đóng drawer chi tiết"
        className={cn(
          "tw-absolute tw-inset-0 tw-border-0 tw-bg-transparent tw-p-0 tw-will-change-opacity",
          phase === "opening" ? "tw-animate-vm-drawer-backdrop-in" : "",
          phase === "closing" ? "tw-animate-vm-drawer-backdrop-out" : "",
        )}
        onClick={onClose}
      />

      <aside
        className={cn(
          "tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,520px)] tw-transform-gpu tw-flex-col tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-vm-drawer tw-will-change-transform [backface-visibility:hidden] max-[768px]:tw-w-full",
          phase === "opening" ? "tw-animate-vm-drawer-panel-in" : "",
          phase === "closing" ? "tw-animate-vm-drawer-panel-out" : "",
        )}
      >
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-px-6 tw-py-5">
          <div className="tw-min-w-0">
            <h2 id="parking-node-drawer-title" className="tw-m-0 tw-text-[1.28rem] tw-font-extrabold tw-text-vm-slate-900">{title}</h2>
          </div>
          <button
            className="tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-900 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
            type="button"
            aria-label="Đóng"
            onClick={onClose}
          >
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-min-h-0 tw-flex-1 tw-overflow-y-auto tw-px-6 tw-pb-5 tw-pt-0 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
          <section className="tw-mb-4 tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            <span className={cn("tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1.1rem]", topologyTone(status).icon)}>
              <i className={lane ? laneIcon(lane.direction) : node.kind === "gate" ? "fas fa-archway" : "fas fa-parking"} />
            </span>
            <div className="tw-min-w-0 tw-flex-1">
              <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2">
                <strong className="tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">{code}</strong>
                <Badge tone={statusTone(status)} className="tw-rounded-full tw-px-3">{statusLabel(status)}</Badge>
              </div>
            </div>
          </section>

          <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin cơ bản</h3>
            <dl className="tw-m-0 tw-mt-4 tw-grid tw-gap-3">
              {[
                ["Tên", node.label],
                ["Mã", code],
                ["Loại node", node.kind === "lane" ? "Làn xe" : node.kind === "gate" ? "Cổng" : "Khu"],
                ["Hướng lưu thông", lane?.direction === "IN" ? "Vào bãi" : lane?.direction === "OUT" ? "Ra bãi" : lane?.direction === "VIP" ? "Làn ưu tiên" : "-"],
              ].map(([label, value]) => (
                <div className="tw-grid tw-grid-cols-[120px_1fr] tw-gap-3" key={label}>
                  <dt className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">{label}</dt>
                  <dd className="tw-m-0 tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-900">{value}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="tw-mt-4 tw-rounded-vm-lg tw-bg-white tw-p-0">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Trạng thái vận hành</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-2">
              {[
                ["Kích hoạt", "fas fa-check", "tw-border-emerald-100 tw-bg-emerald-50 tw-text-emerald-700"],
                ["Bảo trì", "fas fa-tools", "tw-border-amber-100 tw-bg-amber-50 tw-text-amber-700"],
                ["Đóng làn", "fas fa-ban", "tw-border-red-100 tw-bg-red-50 tw-text-red-700"],
              ].map(([label, icon, className]) => (
                <button className={cn("tw-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-text-[0.78rem] tw-font-extrabold tw-transition hover:tw-translate-y-[-1px]", className)} key={label} type="button">
                  <i className={icon} />
                  {label}
                </button>
              ))}
            </div>
          </section>

          <section className="tw-mt-6 tw-rounded-vm-lg tw-bg-white">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Luồng xe hiện tại</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-3">
              {[
                ["Xe máy", "28", "fas fa-motorcycle"],
                ["Ô tô", "16", "fas fa-car"],
                ["Phiên đang mở", (lane?.activeSessions ?? 6).toString(), "far fa-clock"],
              ].map(([label, value, icon]) => (
                <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3" key={label}>
                  <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-500"><i className={cn(icon, "tw-text-vm-primary")} />{label}</span>
                  <strong className="tw-mt-1 tw-block tw-text-[1.1rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
                </div>
              ))}
            </div>
          </section>

          <section className="tw-mt-6 tw-rounded-vm-lg tw-bg-white">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thiết bị liên kết</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-3">
              {[
                ["Camera", lane?.camera ?? "CAM-ZONE-01", "fas fa-video"],
                ["RFID reader", lane?.rfid ?? "RFID-GATE-01", "fas fa-wifi"],
                ["Barrier", "BAR-" + code.replace("LANE-", ""), "fas fa-road"],
              ].map(([label, value, icon]) => (
                <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3" key={label}>
                  <span className="tw-flex tw-items-start tw-gap-2">
                    <span className="tw-inline-flex tw-h-8 tw-w-8 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
                      <i className={icon} />
                    </span>
                    <span className="tw-min-w-0">
                      <strong className="tw-block tw-text-[0.84rem] tw-font-extrabold tw-text-vm-slate-900">{label}</strong>
                      <small className="tw-block tw-truncate tw-text-[0.7rem] tw-font-semibold tw-text-vm-slate-500">{value}</small>
                      <small className="tw-mt-1 tw-inline-flex tw-items-center tw-gap-1 tw-text-[0.62rem] tw-font-extrabold tw-text-emerald-600"><span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-emerald-500" />Hoạt động</small>
                    </span>
                  </span>
                </div>
              ))}
            </div>
          </section>

          <section className="tw-mt-6 tw-rounded-vm-lg tw-bg-white">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Lịch sử gần đây</h3>
            <div className="tw-mt-4 tw-grid tw-gap-3">
              {[
                [lane?.updatedAt ?? "30/06/2026 09:15", "Cập nhật trạng thái vận hành"],
                ["30/06/2026 08:45", "Đồng bộ thiết bị thành công"],
                ["29/06/2026 17:30", "Kiểm tra bảo trì định kỳ"],
              ].map(([time, text]) => (
                <div className="tw-flex tw-gap-3" key={`${time}-${text}`}>
                  <span className="tw-mt-1.5 tw-h-2 tw-w-2 tw-rounded-full tw-bg-vm-primary" />
                  <div>
                    <span className="tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-500">{time}</span>
                    <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">{text}</p>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        <div className="tw-absolute tw-bottom-[84px] tw-right-5 tw-w-[220px] tw-rounded-vm-lg tw-border tw-border-solid tw-border-amber-100 tw-bg-white tw-p-4 tw-shadow-[0_18px_42px_rgba(15,23,42,0.16)]">
          <div className="tw-flex tw-gap-2">
            <i className="fas fa-exclamation-triangle tw-mt-0.5 tw-text-amber-500" />
            <div>
              <strong className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-900">Xác nhận đưa làn vào bảo trì?</strong>
              <p className="tw-m-0 tw-mt-1 tw-text-[0.68rem] tw-font-semibold tw-leading-4 tw-text-vm-slate-500">Làn sẽ tạm ngưng hoạt động cho đến khi kích hoạt lại.</p>
            </div>
          </div>
          <div className="tw-mt-3 tw-flex tw-justify-end tw-gap-2">
            <Button size="sm" variant="secondary">Không</Button>
            <Button size="sm" className="tw-bg-red-500 hover:tw-bg-red-600">Xác nhận</Button>
          </div>
        </div>

        <footer className="tw-grid tw-grid-cols-[1fr_1.35fr_1.35fr] tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6 tw-py-4">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button variant="primary">
            <i className="far fa-save" />
            Lưu thay đổi
          </Button>
          <Button className="tw-border-orange-200 tw-bg-white tw-text-orange-600 hover:tw-bg-orange-50" variant="secondary">
            <i className="fas fa-tools" />
            Đưa bảo trì
          </Button>
        </footer>
      </aside>
    </div>
  );
}

export function ParkingOperationsPage() {
  const [selectedLotId, setSelectedLotId] = useState(parkingLots[0].id);
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [selectedVehicleType, setSelectedVehicleType] = useState("all");
  const [selectedNode, setSelectedNode] = useState<SelectedNode | null>({ id: "lane-a1-in", kind: "lane", label: "Làn vào" });
  const [drawerOpen, setDrawerOpen] = useState(false);

  const selectedParkingLot = parkingLots.find((lot) => lot.id === selectedLotId) ?? parkingLots[0];

  const zonesForLot = useMemo(() => {
    return zones.filter((zone) => {
      const matchesLot = zone.parkingLotId === selectedLotId;
      const matchesVehicle =
        selectedVehicleType === "all" ||
        (selectedVehicleType === "motorbike" && zone.vehicleType === "Xe máy") ||
        (selectedVehicleType === "car" && zone.vehicleType === "Ô tô") ||
        (selectedVehicleType === "mixed" && zone.vehicleType === "Hỗn hợp");
      const matchesStatus = selectedStatus === "all" || zone.status === selectedStatus;
      return matchesLot && matchesVehicle && matchesStatus;
    });
  }, [selectedLotId, selectedStatus, selectedVehicleType]);

  const gatesByZone = useMemo(() => {
    return gates.reduce((map, gate) => {
      const current = map.get(gate.zoneId) ?? [];
      current.push(gate);
      map.set(gate.zoneId, current);
      return map;
    }, new Map<string, Gate[]>());
  }, []);

  const lanesByGate = useMemo(() => {
    return lanes.reduce((map, lane) => {
      const current = map.get(lane.gateId) ?? [];
      current.push(lane);
      map.set(lane.gateId, current);
      return map;
    }, new Map<string, Lane[]>());
  }, []);

  const selectedLane = selectedNode?.kind === "lane" ? lanes.find((lane) => lane.id === selectedNode.id) : undefined;
  const selectedGate = selectedNode?.kind === "gate" ? gates.find((gate) => gate.id === selectedNode.id) : selectedLane ? gates.find((gate) => gate.id === selectedLane.gateId) : undefined;
  const selectedZone = selectedNode?.kind === "zone" ? zones.find((zone) => zone.id === selectedNode.id) : selectedGate ? zones.find((zone) => zone.id === selectedGate.zoneId) : undefined;

  function handleSelectNode(node: SelectedNode) {
    setSelectedNode(node);
    setDrawerOpen(true);
  }

  return (
    <>
      <div className="tw-px-4 tw-py-4 lg:tw-px-5">
        <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1560px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-mb-5 tw-flex tw-items-center tw-justify-between tw-gap-4">
            <div className="tw-flex tw-min-w-0 tw-items-center tw-gap-4">
              <h1 className="tw-m-0 tw-text-vm-page-title tw-tracking-[-0.03em] tw-text-vm-slate-900">Bãi xe & Sơ đồ vận hành</h1>
              <a className="tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-primary hover:tw-text-vm-primary-hover hover:tw-no-underline" href="#parking-help">
                <i className="far fa-question-circle tw-text-[1rem]" />
                Hướng dẫn & Trợ giúp
              </a>
            </div>
            <div className="tw-flex tw-flex-shrink-0 tw-items-center tw-gap-3">
              <Button size="lg" variant="primary">
                <i className="fas fa-plus" />
                Thêm bãi xe
              </Button>
              <Button size="lg" variant="secondary">
                <i className="fas fa-download" />
                Xuất dữ liệu
                <i className="fas fa-chevron-down tw-text-[0.72rem]" />
              </Button>
            </div>
          </div>

          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
            <ParkingMetricCard delta="+1 so với tháng trước" icon="fas fa-parking" label="Tổng bãi xe" tone="blue" value="3" />
            <ParkingMetricCard delta="+4 khu đang mở" icon="fas fa-layer-group" label="Khu đang mở" tone="green" value="12" />
            <ParkingMetricCard delta="+2 cổng hôm nay" icon="fas fa-door-open" label="Cổng hoạt động" tone="amber" value="8" />
            <ParkingMetricCard delta="-1 so với hôm qua" icon="fas fa-road" label="Làn bảo trì" tone="red" value="2" />
          </div>

          <Card className="tw-mt-4 tw-p-4">
            <div className="tw-grid tw-grid-cols-[260px_minmax(260px,1fr)_180px_180px_auto] tw-items-start tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <SelectMenu className="tw-self-start" ariaLabel="Chọn bãi xe" options={lotOptions} value={selectedLotId} clearValue={parkingLots[0].id} onChange={setSelectedLotId} />
              <label className="tw-m-0 tw-box-border tw-flex tw-h-[42px] tw-self-start tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)] tw-transition focus-within:tw-border-brand-200 focus-within:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]">
                <i className="fas fa-search tw-flex-shrink-0 tw-text-[0.92rem] tw-leading-none tw-text-vm-slate-500" />
                <input
                  className="tw-m-0 tw-h-full tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-font-semibold tw-leading-[42px] tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
                  placeholder="Tìm khu, cổng, làn..."
                />
              </label>
              <SelectMenu className="tw-self-start" ariaLabel="Trạng thái" options={statusOptions} value={selectedStatus} onChange={setSelectedStatus} />
              <SelectMenu className="tw-self-start" ariaLabel="Loại phương tiện" options={vehicleTypeOptions} value={selectedVehicleType} onChange={setSelectedVehicleType} />
              <Button
                className="tw-h-[42px] tw-self-start tw-whitespace-nowrap"
                variant="secondary"
                onClick={() => {
                  setSelectedStatus("all");
                  setSelectedVehicleType("all");
                }}
              >
                <i className="fas fa-sync-alt" />
                Xóa bộ lọc
              </Button>
            </div>
          </Card>

          <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_330px] tw-gap-4 max-[1280px]:tw-grid-cols-1">
            <ParkingTopologyMap
              gatesByZone={gatesByZone}
              lanesByGate={lanesByGate}
              onSelect={handleSelectNode}
              selectedNode={selectedNode}
              selectedParkingLot={selectedParkingLot}
              zonesForLot={zonesForLot}
            />
            <OperationSummary selectedParkingLot={selectedParkingLot} />
          </div>
        </section>
      </div>

      <ParkingNodeDrawer
        gate={selectedGate}
        isOpen={drawerOpen}
        lane={selectedLane}
        node={selectedNode}
        onClose={() => setDrawerOpen(false)}
        zone={selectedZone}
      />
    </>
  );
}
