import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";

import { Badge, Button, Card, SelectMenu, useToast } from "@/components/ui";
import { getVehicleTypes, type VehicleTypeApiResponse } from "@/features/catalog/api/vehicleTypesApi";
import {
  activateParkingLot,
  closeParkingLot,
  createParkingLot,
  getParkingLots,
  markParkingLotMaintenance,
  updateParkingLot,
  type ParkingLotApiResponse,
  type ParkingLotStatusApi,
} from "@/features/parking/api/parkingLotsApi";
import {
  activateGate,
  activateLane,
  activateZone,
  closeGate,
  closeLane,
  closeZone,
  getGates,
  getLanes,
  getZones,
  markGateMaintenance,
  markLaneMaintenance,
  markZoneMaintenance,
  updateGate,
  updateLane,
  updateZone,
  type GateApiResponse,
  type GateStatusApi,
  type LaneApiResponse,
  type LaneDirectionApi,
  type LaneStatusApi,
  type ZoneApiResponse,
  type ZoneStatusApi,
} from "@/features/parking/api/parkingTopologyApi";
import { cn } from "@/lib/cn";

type ParkingLotStatus = ParkingLotStatusApi;
type ZoneStatus = ZoneStatusApi;
type GateStatus = GateStatusApi;
type LaneStatus = LaneStatusApi | "OVERLOAD";
type LaneDirection = LaneDirectionApi | "VIP";
type DrawerPhase = "opening" | "open" | "closing";

type ParkingLot = {
  address: string;
  code: string;
  id: string;
  name: string;
  source: "api" | "mock";
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
  vehicleType: string;
  vehicleTypeId: string | null;
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

const mockParkingLots: ParkingLot[] = [
  {
    address: "12 Nguyễn Văn Linh, Quận 7, TP. HCM",
    code: "CP-LOT-A",
    id: "lot-a",
    source: "mock",
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
    source: "mock",
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
    source: "mock",
    name: "CP-Lot C - Sân bay",
    sessions: 0,
    status: "CLOSED",
    totalCapacity: 260,
    used: 0,
  },
];

const mockZones: Zone[] = [
  { capacity: 500, code: "ZONE-A", id: "zone-a", name: "Khu A", parkingLotId: "lot-a", status: "ACTIVE", used: 342, vehicleType: "Xe máy", vehicleTypeId: null },
  { capacity: 400, code: "ZONE-B", id: "zone-b", name: "Khu B", parkingLotId: "lot-a", status: "ACTIVE", used: 265, vehicleType: "Ô tô", vehicleTypeId: null },
  { capacity: 300, code: "ZONE-C", id: "zone-c", name: "Khu C", parkingLotId: "lot-a", status: "MAINTENANCE", used: 120, vehicleType: "Hỗn hợp", vehicleTypeId: null },
  { capacity: 200, code: "ZONE-D", id: "zone-d", name: "Khu D", parkingLotId: "lot-a", status: "CLOSED", used: 0, vehicleType: "Xe máy", vehicleTypeId: null },
];

const mockGates: Gate[] = [
  { code: "GATE-A1", id: "gate-a1", name: "Cổng A1", status: "ACTIVE", zoneId: "zone-a" },
  { code: "GATE-A2", id: "gate-a2", name: "Cổng A2", status: "ACTIVE", zoneId: "zone-a" },
  { code: "GATE-B1", id: "gate-b1", name: "Cổng B1", status: "ACTIVE", zoneId: "zone-b" },
  { code: "GATE-B2", id: "gate-b2", name: "Cổng B2", status: "ACTIVE", zoneId: "zone-b" },
  { code: "GATE-C1", id: "gate-c1", name: "Cổng C1", status: "MAINTENANCE", zoneId: "zone-c" },
  { code: "GATE-C2", id: "gate-c2", name: "Cổng C2", status: "MAINTENANCE", zoneId: "zone-c" },
  { code: "GATE-D1", id: "gate-d1", name: "Cổng D1", status: "CLOSED", zoneId: "zone-d" },
];

const mockLanes: Lane[] = [
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

const statusOptions = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Đang hoạt động", value: "ACTIVE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
  { label: "Đã đóng", value: "CLOSED" },
];
function toParkingLotView(lot: ParkingLotApiResponse): ParkingLot {
  return {
    address: lot.address ?? "",
    code: lot.code,
    id: lot.parkingLotId,
    name: lot.name,
    sessions: 0,
    source: "api",
    status: lot.status,
    totalCapacity: Number(lot.totalCapacity ?? 0),
    used: 0,
  };
}

function toParkingLotPayload(lot: ParkingLot) {
  return {
    address: lot.address,
    code: lot.code,
    name: lot.name,
    totalCapacity: lot.totalCapacity,
  };
}

function getVehicleTypeLabel(vehicleTypeId: string | null, vehicleTypeLookup: Map<string, VehicleTypeApiResponse>) {
  if (!vehicleTypeId) return "Hỗn hợp";
  const vehicleType = vehicleTypeLookup.get(vehicleTypeId);
  return vehicleType?.name || vehicleType?.code || "Hỗn hợp";
}

function toZoneView(zone: ZoneApiResponse, vehicleTypeLookup: Map<string, VehicleTypeApiResponse>): Zone {
  return {
    capacity: Number(zone.capacity ?? 0),
    code: zone.code,
    id: zone.zoneId,
    name: zone.name,
    parkingLotId: zone.parkingLotId,
    status: zone.status,
    used: 0,
    vehicleType: getVehicleTypeLabel(zone.vehicleTypeId, vehicleTypeLookup),
    vehicleTypeId: zone.vehicleTypeId,
  };
}

function toGateView(gate: GateApiResponse): Gate {
  return {
    code: gate.code,
    id: gate.gateId,
    name: gate.name,
    status: gate.status,
    zoneId: gate.zoneId,
  };
}

function toLaneView(lane: LaneApiResponse): Lane {
  return {
    activeSessions: 0,
    camera: "--",
    code: lane.code,
    deviceHealth: "Ổn định",
    direction: lane.direction,
    gateId: lane.gateId,
    id: lane.laneId,
    name: lane.name,
    rfid: "--",
    status: lane.status,
    throughput: 0,
    updatedAt: lane.updatedAt ?? lane.createdAt ?? "--",
  };
}

function getVehicleTypeFilterValue(vehicleType: VehicleTypeApiResponse) {
  const text = `${vehicleType.code} ${vehicleType.name}`.toLowerCase();
  if (text.includes("moto") || text.includes("motor") || text.includes("xe máy") || text.includes("xe may")) return "motorbike";
  if (text.includes("car") || text.includes("oto") || text.includes("ô tô") || text.includes("o to")) return "car";
  return vehicleType.vehicleTypeId;
}

function zoneMatchesVehicleFilter(zone: Zone, selectedVehicleType: string) {
  if (selectedVehicleType === "all") return true;
  if (zone.vehicleTypeId === selectedVehicleType) return true;

  const text = zone.vehicleType.toLowerCase();
  if (selectedVehicleType === "motorbike") return text.includes("moto") || text.includes("motor") || text.includes("xe máy") || text.includes("xe may");
  if (selectedVehicleType === "car") return text.includes("car") || text.includes("oto") || text.includes("ô tô") || text.includes("o to");

  return false;
}

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
  const normalized = vehicleType.toLowerCase();
  if (normalized.includes("moto") || normalized.includes("motor") || normalized.includes("xe máy") || normalized.includes("xe may")) return "fas fa-motorcycle";
  if (normalized.includes("car") || normalized.includes("oto") || normalized.includes("ô tô") || normalized.includes("o to")) return "fas fa-car";
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
  const used = Math.min(capacity, lane.status === "CLOSED" || lane.status === "MAINTENANCE" ? 0 : Math.round(lane.throughput * 0.75));
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
  const inLaneCount = lanes.filter((lane) => lane.direction === "IN").length;
  const outLaneCount = lanes.filter((lane) => lane.direction === "OUT").length;

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
        <span title="Làn vào"><i className="fas fa-sign-in-alt tw-mr-1.5" />{inLaneCount}</span>
        <span title="Làn ra"><i className="fas fa-sign-out-alt tw-mr-1.5" />{outLaneCount}</span>
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
            {zonesForLot.length ? (
              <>
                <div className="tw-absolute tw-left-[11%] tw-right-[11%] tw-top-[66px] tw-h-px tw-bg-slate-300" />
                <div className="tw-grid tw-grid-cols-4 tw-gap-8">
                  {zonesForLot.map((zone) => (
                    <ZoneCard key={zone.id} zone={zone} gatesByZone={gatesByZone} lanesByGate={lanesByGate} selectedNode={selectedNode} onSelect={onSelect} />
                  ))}
                </div>
              </>
            ) : (
              <div className="tw-flex tw-h-[360px] tw-w-[720px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-6 tw-text-center">
                <div>
                  <i className="fas fa-parking tw-text-[2rem] tw-text-vm-slate-400" />
                  <p className="tw-m-0 tw-mt-3 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">Chưa có dữ liệu khu, cổng và làn cho bãi này.</p>
                  <p className="tw-m-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">API đã được gọi nhưng bãi này chưa có cấu hình topology phù hợp với bộ lọc hiện tại.</p>
                </div>
              </div>
            )}
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

function OperationSummary({ gates, lanes, selectedParkingLot }: { gates: Gate[]; lanes: Lane[]; selectedParkingLot: ParkingLot }) {
  const totalCapacity = Math.max(selectedParkingLot.totalCapacity, 0);
  const percent = totalCapacity ? Math.round((selectedParkingLot.used / totalCapacity) * 100) : 0;
  const free = Math.max(totalCapacity - selectedParkingLot.used, 0);
  const activeGateCount = gates.filter((gate) => gate.status === "ACTIVE").length;
  const activeLaneCount = lanes.filter((lane) => lane.status === "ACTIVE").length;
  const maintenanceLaneCount = lanes.filter((lane) => lane.status === "MAINTENANCE").length;
  const overloadLaneCount = lanes.filter((lane) => lane.status === "OVERLOAD").length;

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
                ["Tổng sức chứa", totalCapacity.toLocaleString("vi-VN"), "tw-bg-vm-slate-400"],
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
            ["Phiên đang hoạt động", selectedParkingLot.sessions.toLocaleString("vi-VN"), "far fa-id-badge", "tw-text-vm-primary"],
            ["Cổng hoạt động", `${activeGateCount.toLocaleString("vi-VN")} / ${gates.length.toLocaleString("vi-VN")}`, "fas fa-archway", "tw-text-vm-primary"],
            ["Làn đang hoạt động", `${activeLaneCount.toLocaleString("vi-VN")} / ${lanes.length.toLocaleString("vi-VN")}`, "fas fa-road", "tw-text-vm-primary"],
            ["Làn bảo trì", maintenanceLaneCount.toLocaleString("vi-VN"), "fas fa-tools", "tw-text-red-500"],
            ["Làn quá tải", overloadLaneCount.toLocaleString("vi-VN"), "fas fa-exclamation-triangle", "tw-text-red-500"],
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

function ParkingLotDrawer({
  error,
  isOpen,
  lot,
  onClose,
  onSubmit,
  saving,
}: {
  error: string;
  isOpen: boolean;
  lot: ParkingLot | null;
  onClose: () => void;
  onSubmit: (payload: ParkingLot) => Promise<void> | void;
  saving: boolean;
}) {
  const [form, setForm] = useState<ParkingLot>(() => lot ?? {
    address: "",
    code: "",
    id: "",
    name: "",
    sessions: 0,
    source: "api",
    status: "ACTIVE",
    totalCapacity: 0,
    used: 0,
  });
  const [formError, setFormError] = useState("");

  useEffect(() => {
    setForm(lot ?? {
      address: "",
      code: "",
      id: "",
      name: "",
      sessions: 0,
      source: "api",
      status: "ACTIVE",
      totalCapacity: 0,
      used: 0,
    });
    setFormError("");
  }, [lot, isOpen]);

  if (!isOpen) return null;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError("");

    if (!form.code.trim() || !form.name.trim() || form.totalCapacity <= 0) {
      setFormError("Vui lòng nhập mã bãi, tên bãi và sức chứa hợp lệ.");
      return;
    }

    await onSubmit({
      ...form,
      address: form.address.trim(),
      code: form.code.trim(),
      name: form.name.trim(),
      totalCapacity: Number(form.totalCapacity),
    });
  }

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2300] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="parking-lot-drawer-title">
      <button className="tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0" type="button" aria-label="Đóng form bãi xe" onClick={onClose} />
      <aside className="tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,460px)] tw-flex-col tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-shadow-vm-drawer">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-px-6 tw-py-5">
          <div>
            <h2 id="parking-lot-drawer-title" className="tw-m-0 tw-text-[1.22rem] tw-font-extrabold tw-text-vm-slate-900">{lot ? "Sửa bãi xe" : "Thêm bãi xe"}</h2>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">Dữ liệu được lưu trực tiếp qua API bãi xe.</p>
          </div>
          <button className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 hover:tw-bg-vm-slate-100" type="button" aria-label="Đóng" onClick={onClose}>
            <i className="fas fa-times" />
          </button>
        </header>

        <form className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col" onSubmit={(event) => void handleSubmit(event)}>
          <div className="tw-grid tw-gap-4 tw-overflow-y-auto tw-px-6 tw-pb-5">
            {[
              ["Mã bãi xe", "code", "LOT-HCMUTE"],
              ["Tên bãi xe", "name", "Bãi xe HCMUTE"],
              ["Địa chỉ", "address", "Số 1 Võ Văn Ngân"],
            ].map(([label, key, placeholder]) => (
              <label className="tw-m-0 tw-grid tw-gap-2" key={key}>
                <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">{label}</span>
                <input
                  className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]"
                  placeholder={placeholder}
                  value={String(form[key as "code" | "name" | "address"])}
                  onChange={(event) => setForm((current) => ({ ...current, [key]: event.target.value }))}
                />
              </label>
            ))}

            <label className="tw-m-0 tw-grid tw-gap-2">
              <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Tổng sức chứa</span>
              <input
                className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]"
                min={1}
                type="number"
                value={form.totalCapacity || ""}
                onChange={(event) => setForm((current) => ({ ...current, totalCapacity: Number(event.target.value) }))}
              />
            </label>

            {lot ? (
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
                <span className="tw-text-[0.74rem] tw-font-extrabold tw-text-vm-slate-500">Trạng thái hiện tại</span>
                <div className="tw-mt-2">
                  <Badge tone={statusTone(form.status)} className="tw-rounded-full tw-px-3">{statusLabel(form.status)}</Badge>
                </div>
              </div>
            ) : null}

            {formError || error ? <div className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{formError || error}</div> : null}
          </div>

          <footer className="tw-grid tw-grid-cols-2 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6 tw-py-4">
            <Button variant="secondary" onClick={onClose}>Hủy</Button>
            <Button loading={saving} type="submit">{saving ? "Đang lưu..." : lot ? "Cập nhật" : "Tạo bãi xe"}</Button>
          </footer>
        </form>
      </aside>
    </div>
  );
}

type ParkingNodeFormPayload = {
  capacity: number;
  code: string;
  direction: LaneDirectionApi;
  name: string;
  vehicleTypeId: string | null;
};

type ParkingNodeStatusAction = "ACTIVE" | "MAINTENANCE" | "CLOSED";

function ParkingNodeDrawer({
  error,
  gate,
  lanes,
  isOpen,
  lane,
  node,
  onClose,
  onSave,
  onStatusChange,
  saving,
  vehicleTypeOptions,
  zone,
}: {
  error: string;
  gate?: Gate;
  lanes: Lane[];
  isOpen: boolean;
  lane?: Lane;
  node: SelectedNode | null;
  onClose: () => void;
  onSave: (node: SelectedNode, payload: ParkingNodeFormPayload) => Promise<void> | void;
  onStatusChange: (node: SelectedNode, action: ParkingNodeStatusAction) => Promise<void> | void;
  saving: boolean;
  vehicleTypeOptions: Array<{ label: string; value: string }>;
  zone?: Zone;
}) {
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");
  const [form, setForm] = useState<ParkingNodeFormPayload>({
    capacity: 0,
    code: "",
    direction: "IN",
    name: "",
    vehicleTypeId: null,
  });
  const [formError, setFormError] = useState("");
  const [confirmAction, setConfirmAction] = useState<ParkingNodeStatusAction | null>(null);

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

  useEffect(() => {
    setForm({
      capacity: zone?.capacity ?? 0,
      code: lane?.code ?? gate?.code ?? zone?.code ?? "",
      direction: lane?.direction === "OUT" ? "OUT" : "IN",
      name: lane?.name ?? gate?.name ?? zone?.name ?? "",
      vehicleTypeId: zone?.vehicleTypeId ?? null,
    });
    setFormError("");
    setConfirmAction(null);
  }, [gate, lane, node, zone]);

  if (!isRendered || !node) return null;
  const currentNode = node;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError("");

    if (!form.code.trim() || !form.name.trim()) {
      setFormError("Vui lòng nhập mã và tên.");
      return;
    }

    if (currentNode.kind === "zone" && form.capacity <= 0) {
      setFormError("Sức chứa khu phải lớn hơn 0.");
      return;
    }

    await onSave(currentNode, {
      ...form,
      code: form.code.trim(),
      name: form.name.trim(),
    });
  }

  async function handleConfirmStatus() {
    if (!confirmAction) return;
    await onStatusChange(currentNode, confirmAction);
    setConfirmAction(null);
  }

  const title = currentNode.kind === "lane" ? "Chi tiết làn" : currentNode.kind === "gate" ? "Chi tiết cổng" : "Chi tiết khu vực";
  const status = lane?.status ?? gate?.status ?? zone?.status ?? "ACTIVE";
  const code = lane?.code ?? gate?.code ?? zone?.code ?? "";
  const canEdit = !currentNode.id.startsWith("zone-") && !currentNode.id.startsWith("gate-") && !currentNode.id.startsWith("lane-");
  const nodeLabel = currentNode.kind === "lane" ? "làn" : currentNode.kind === "gate" ? "cổng" : "khu";
  const confirmTitle = confirmAction === "MAINTENANCE" ? `Xác nhận đưa ${nodeLabel} vào bảo trì?` : `Xác nhận đóng ${nodeLabel}?`;
  const confirmMessage =
    confirmAction === "MAINTENANCE"
      ? `${nodeLabel.charAt(0).toUpperCase()}${nodeLabel.slice(1)} sẽ tạm ngưng hoạt động cho đến khi được kích hoạt lại.`
      : `Sau khi đóng, ${nodeLabel} sẽ không còn được sử dụng trong luồng vận hành. Chỉ thực hiện khi chắc chắn không còn phiên hoặc thiết bị đang phụ thuộc.`;
  const inLaneCount = gate ? lanes.filter((item) => item.gateId === gate.id && item.direction === "IN").length : 0;
  const outLaneCount = gate ? lanes.filter((item) => item.gateId === gate.id && item.direction === "OUT").length : 0;

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

        <form className="tw-flex tw-min-h-0 tw-flex-1 tw-flex-col" onSubmit={(event) => void handleSubmit(event)}>
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
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin cấu hình</h3>
            <div className="tw-mt-4 tw-grid tw-gap-3">
              <label className="tw-m-0 tw-grid tw-gap-2">
                <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Mã</span>
                <input
                  className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-bg-vm-slate-25"
                  disabled={!canEdit || saving}
                  value={form.code}
                  onChange={(event) => setForm((current) => ({ ...current, code: event.target.value }))}
                />
              </label>
              <label className="tw-m-0 tw-grid tw-gap-2">
                <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Tên</span>
                <input
                  className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-bg-vm-slate-25"
                  disabled={!canEdit || saving}
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                />
              </label>
              {node.kind === "zone" ? (
                <div className="tw-grid tw-grid-cols-2 tw-gap-3">
                  <label className="tw-m-0 tw-grid tw-gap-2">
                    <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Sức chứa</span>
                    <input
                      className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-bg-vm-slate-25"
                      disabled={!canEdit || saving}
                      min={1}
                      type="number"
                      value={form.capacity || ""}
                      onChange={(event) => setForm((current) => ({ ...current, capacity: Number(event.target.value) }))}
                    />
                  </label>
                  <label className="tw-m-0 tw-grid tw-gap-2">
                    <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Loại xe</span>
                    <select
                      className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-bg-vm-slate-25"
                      disabled={!canEdit || saving}
                      value={form.vehicleTypeId ?? ""}
                      onChange={(event) => setForm((current) => ({ ...current, vehicleTypeId: event.target.value || null }))}
                    >
                      <option value="">Chưa chọn</option>
                      {vehicleTypeOptions.filter((option) => option.value !== "all").map((option) => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </label>
                </div>
              ) : null}
              {node.kind === "lane" ? (
                <label className="tw-m-0 tw-grid tw-gap-2">
                  <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-600">Hướng lưu thông</span>
                  <select
                    className="tw-h-[42px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-bg-vm-slate-25"
                    disabled={!canEdit || saving}
                    value={form.direction}
                    onChange={(event) => setForm((current) => ({ ...current, direction: event.target.value as LaneDirectionApi }))}
                  >
                    <option value="IN">Vào bãi</option>
                    <option value="OUT">Ra bãi</option>
                  </select>
                </label>
              ) : null}
              {!canEdit ? <div className="tw-rounded-vm-md tw-bg-amber-50 tw-p-3 tw-text-[0.78rem] tw-font-bold tw-text-amber-700">Dữ liệu mẫu chỉ dùng để xem giao diện, không thể cập nhật.</div> : null}
              {formError || error ? <div className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{formError || error}</div> : null}
            </div>
          </section>

          <section className="tw-mt-4 tw-rounded-vm-lg tw-bg-white tw-p-0">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Trạng thái vận hành</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-2">
              <button className="tw-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-emerald-100 tw-bg-emerald-50 tw-text-[0.78rem] tw-font-extrabold tw-text-emerald-700 tw-transition hover:tw-translate-y-[-1px] disabled:tw-opacity-50" disabled={!canEdit || saving || status === "ACTIVE"} type="button" onClick={() => void onStatusChange(node, "ACTIVE")}>
                <i className="fas fa-check" />
                Kích hoạt
              </button>
              <button className="tw-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-100 tw-bg-amber-50 tw-text-[0.78rem] tw-font-extrabold tw-text-amber-700 tw-transition hover:tw-translate-y-[-1px] disabled:tw-opacity-50" disabled={!canEdit || saving || status === "MAINTENANCE"} type="button" onClick={() => setConfirmAction("MAINTENANCE")}>
                <i className="fas fa-tools" />
                Bảo trì
              </button>
              <button className="tw-flex tw-min-h-[46px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-text-[0.78rem] tw-font-extrabold tw-text-red-700 tw-transition hover:tw-translate-y-[-1px] disabled:tw-opacity-50" disabled={!canEdit || saving || status === "CLOSED"} type="button" onClick={() => setConfirmAction("CLOSED")}>
                <i className="fas fa-ban" />
                Đóng
              </button>
            </div>
          </section>

          <section className="tw-mt-6 tw-rounded-vm-lg tw-bg-white">
            <h3 className="tw-m-0 tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">Luồng xe hiện tại</h3>
            <div className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-3">
              {[
                ["Làn vào", node.kind === "gate" ? inLaneCount.toString() : lane?.direction === "IN" ? "1" : "0", "fas fa-sign-in-alt"],
                ["Làn ra", node.kind === "gate" ? outLaneCount.toString() : lane?.direction === "OUT" ? "1" : "0", "fas fa-sign-out-alt"],
                ["Phiên đang mở", (lane?.activeSessions ?? 0).toString(), "far fa-clock"],
              ].map(([label, value, icon]) => (
                <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3" key={label}>
                  <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.72rem] tw-font-extrabold tw-text-vm-slate-500"><i className={cn(icon, "tw-text-vm-primary")} />{label}</span>
                  <strong className="tw-mt-1 tw-block tw-text-[1.1rem] tw-font-extrabold tw-text-vm-slate-900">{value}</strong>
                </div>
              ))}
            </div>
          </section>

          {node.kind === "lane" ? <section className="tw-mt-6 tw-rounded-vm-lg tw-bg-white">
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
          </section> : null}

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

        {confirmAction ? (
          <div className="tw-fixed tw-inset-0 tw-z-[2600] tw-flex tw-items-center tw-justify-center tw-bg-slate-900/35 tw-p-4">
            <div className="tw-w-[min(100%,380px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-[0_24px_70px_rgba(15,23,42,0.22)]">
              <div className="tw-flex tw-gap-3">
                <span className={cn("tw-inline-flex tw-h-10 tw-w-10 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full", confirmAction === "CLOSED" ? "tw-bg-red-50 tw-text-red-500" : "tw-bg-amber-50 tw-text-amber-500")}>
                  <i className="fas fa-exclamation-triangle" />
                </span>
                <div>
                  <strong className="tw-text-[0.98rem] tw-font-extrabold tw-text-vm-slate-900">{confirmTitle}</strong>
                  <p className="tw-m-0 tw-mt-2 tw-text-[0.82rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{confirmMessage}</p>
                </div>
              </div>
              <div className="tw-mt-5 tw-flex tw-justify-end tw-gap-3">
                <Button variant="secondary" disabled={saving} onClick={() => setConfirmAction(null)}>Không</Button>
                <Button className={confirmAction === "CLOSED" ? "tw-bg-red-500 hover:tw-bg-red-600" : "tw-bg-orange-500 hover:tw-bg-orange-600"} loading={saving} onClick={() => void handleConfirmStatus()}>
                  {saving ? "Đang xử lý..." : "Xác nhận"}
                </Button>
              </div>
            </div>
          </div>
        ) : null}

        <footer className="tw-grid tw-grid-cols-[1fr_1.35fr] tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-6 tw-py-4">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button variant="primary" type="submit" disabled={!canEdit} loading={saving}>
            {!saving ? <i className="far fa-save" /> : null}
            {saving ? "Đang lưu" : "Lưu thay đổi"}
          </Button>
        </footer>
        </form>
      </aside>
    </div>
  );
}

export function ParkingOperationsPage() {
  const toast = useToast();
  const [parkingLots, setParkingLots] = useState<ParkingLot[]>(mockParkingLots);
  const [selectedLotId, setSelectedLotId] = useState(mockParkingLots[0].id);
  const [selectedStatus, setSelectedStatus] = useState("all");
  const [selectedVehicleType, setSelectedVehicleType] = useState("all");
  const [selectedNode, setSelectedNode] = useState<SelectedNode | null>({ id: "lane-a1-in", kind: "lane", label: "Làn vào" });
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [lotDrawerOpen, setLotDrawerOpen] = useState(false);
  const [editingLot, setEditingLot] = useState<ParkingLot | null>(null);
  const [loadingLots, setLoadingLots] = useState(false);
  const [savingLot, setSavingLot] = useState(false);
  const [lotError, setLotError] = useState("");
  const [zones, setZones] = useState<Zone[]>(mockZones);
  const [gates, setGates] = useState<Gate[]>(mockGates);
  const [lanes, setLanes] = useState<Lane[]>(mockLanes);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeApiResponse[]>([]);
  const [loadingTopology, setLoadingTopology] = useState(false);
  const [savingNode, setSavingNode] = useState(false);
  const [topologyError, setTopologyError] = useState("");

  const selectedParkingLot = parkingLots.find((lot) => lot.id === selectedLotId) ?? parkingLots[0];
  const selectedLotCanMutate = selectedParkingLot?.source === "api";
  const dynamicLotOptions = useMemo(() => parkingLots.map((lot) => ({ label: `Bãi xe: ${lot.name}`, value: lot.id })), [parkingLots]);
  const activeLotCount = parkingLots.filter((lot) => lot.status === "ACTIVE").length;
  const maintenanceLotCount = parkingLots.filter((lot) => lot.status === "MAINTENANCE").length;
  const vehicleTypeLookup = useMemo(() => new Map(vehicleTypes.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType])), [vehicleTypes]);
  const dynamicVehicleTypeOptions = useMemo(() => {
    const options = vehicleTypes.map((vehicleType) => ({
      label: vehicleType.name || vehicleType.code,
      value: vehicleType.vehicleTypeId,
    }));
    return [{ label: "Tất cả loại xe", value: "all" }, ...options];
  }, [vehicleTypes]);
  const activeGateCount = gates.filter((gate) => gate.status === "ACTIVE").length;
  const activeLaneCount = lanes.filter((lane) => lane.status === "ACTIVE").length;
  const maintenanceLaneCount = lanes.filter((lane) => lane.status === "MAINTENANCE").length;

  const loadParkingLots = useCallback(async (preferredLotId?: string) => {
    setLoadingLots(true);
    setLotError("");

    try {
      const response = await getParkingLots();
      const nextLots = (response.data ?? []).map(toParkingLotView);
      setParkingLots(nextLots.length ? nextLots : mockParkingLots);
      setSelectedLotId((current) => {
        const candidate = preferredLotId ?? current;
        if (nextLots.some((lot) => lot.id === candidate)) return candidate;
        return nextLots[0]?.id ?? mockParkingLots[0].id;
      });
    } catch (error) {
      setLotError(error instanceof Error ? error.message : "Không thể tải dữ liệu bãi xe.");
      setParkingLots(mockParkingLots);
      setSelectedLotId(mockParkingLots[0].id);
    } finally {
      setLoadingLots(false);
    }
  }, []);

  useEffect(() => {
    void loadParkingLots();
  }, [loadParkingLots]);

  const loadParkingTopology = useCallback(async (parkingLotId: string) => {
    if (!parkingLotId) return;

    setLoadingTopology(true);
    setTopologyError("");

    try {
      const [vehicleTypeResponse, zoneResponse] = await Promise.all([
        getVehicleTypes({ isActive: true }),
        getZones({ parkingLotId }),
      ]);
      const nextVehicleTypes = vehicleTypeResponse.data ?? [];
      const nextVehicleTypeLookup = new Map(nextVehicleTypes.map((vehicleType) => [vehicleType.vehicleTypeId, vehicleType]));
      const nextZones = (zoneResponse.data ?? []).map((zone) => toZoneView(zone, nextVehicleTypeLookup));
      const gateResponses = await Promise.all(nextZones.map((zone) => getGates({ zoneId: zone.id })));
      const nextGates = gateResponses.flatMap((response) => (response.data ?? []).map(toGateView));
      const laneResponses = await Promise.all(nextGates.map((gate) => getLanes({ gateId: gate.id })));
      const nextLanes = laneResponses.flatMap((response) => (response.data ?? []).map(toLaneView));

      setVehicleTypes(nextVehicleTypes);
      setZones(nextZones);
      setGates(nextGates);
      setLanes(nextLanes);
      setSelectedNode((current) => {
        if (!current) return null;
        if (current.kind === "zone" && nextZones.some((zone) => zone.id === current.id)) return current;
        if (current.kind === "gate" && nextGates.some((gate) => gate.id === current.id)) return current;
        if (current.kind === "lane" && nextLanes.some((lane) => lane.id === current.id)) return current;
        return null;
      });
    } catch (error) {
      setTopologyError(error instanceof Error ? error.message : "Không thể tải sơ đồ bãi xe.");
      setZones(mockZones);
      setGates(mockGates);
      setLanes(mockLanes);
    } finally {
      setLoadingTopology(false);
    }
  }, []);

  useEffect(() => {
    void loadParkingTopology(selectedLotId);
  }, [loadParkingTopology, selectedLotId]);

  async function handleSubmitParkingLot(payload: ParkingLot) {
    setSavingLot(true);
    setLotError("");

    try {
      if (editingLot) {
        await updateParkingLot(editingLot.id, toParkingLotPayload(payload));
        toast.success("Đã cập nhật bãi xe.");
        await loadParkingLots(editingLot.id);
      } else {
        const created = await createParkingLot(toParkingLotPayload(payload));
        toast.success("Đã tạo bãi xe.");
        await loadParkingLots(created.data.parkingLotId);
      }

      setLotDrawerOpen(false);
      setEditingLot(null);
    } catch (error) {
      setLotError(error instanceof Error ? error.message : "Không thể lưu bãi xe.");
    } finally {
      setSavingLot(false);
    }
  }

  async function handleChangeLotStatus(status: ParkingLotStatus) {
    if (!selectedParkingLot || !selectedLotCanMutate) return;

    setSavingLot(true);
    setLotError("");

    try {
      if (status === "ACTIVE") {
        await activateParkingLot(selectedParkingLot.id);
      } else if (status === "MAINTENANCE") {
        await markParkingLotMaintenance(selectedParkingLot.id);
      } else {
        await closeParkingLot(selectedParkingLot.id);
      }

      toast.success("Đã cập nhật trạng thái bãi xe.");
      await loadParkingLots();
    } catch (error) {
      setLotError(error instanceof Error ? error.message : "Không thể cập nhật trạng thái bãi xe.");
    } finally {
      setSavingLot(false);
    }
  }

  async function handleSaveNode(node: SelectedNode, payload: ParkingNodeFormPayload) {
    setSavingNode(true);
    setTopologyError("");

    try {
      if (node.kind === "zone") {
        await updateZone(node.id, {
          capacity: payload.capacity,
          code: payload.code,
          name: payload.name,
          vehicleTypeId: payload.vehicleTypeId,
        });
      } else if (node.kind === "gate") {
        await updateGate(node.id, {
          code: payload.code,
          name: payload.name,
        });
      } else {
        await updateLane(node.id, {
          code: payload.code,
          direction: payload.direction,
          name: payload.name,
        });
      }

      toast.success("Đã cập nhật thông tin.");
      await loadParkingTopology(selectedLotId);
    } catch (error) {
      setTopologyError(error instanceof Error ? error.message : "Không thể cập nhật thông tin.");
    } finally {
      setSavingNode(false);
    }
  }

  async function handleChangeNodeStatus(node: SelectedNode, action: ParkingNodeStatusAction) {
    setSavingNode(true);
    setTopologyError("");

    try {
      if (node.kind === "zone") {
        if (action === "ACTIVE") await activateZone(node.id);
        else if (action === "MAINTENANCE") await markZoneMaintenance(node.id);
        else await closeZone(node.id);
      } else if (node.kind === "gate") {
        if (action === "ACTIVE") await activateGate(node.id);
        else if (action === "MAINTENANCE") await markGateMaintenance(node.id);
        else await closeGate(node.id);
      } else if (action === "ACTIVE") {
        await activateLane(node.id);
      } else if (action === "MAINTENANCE") {
        await markLaneMaintenance(node.id);
      } else {
        await closeLane(node.id);
      }

      toast.success("Đã cập nhật trạng thái.");
      await loadParkingTopology(selectedLotId);
    } catch (error) {
      setTopologyError(error instanceof Error ? error.message : "Không thể cập nhật trạng thái.");
    } finally {
      setSavingNode(false);
    }
  }

  const zonesForLot = useMemo(() => {
    return zones.filter((zone) => {
      const matchesLot = zone.parkingLotId === selectedLotId || zone.parkingLotId === mockParkingLots[0].id;
      const matchesVehicle = zoneMatchesVehicleFilter(zone, selectedVehicleType);
      const matchesStatus = selectedStatus === "all" || zone.status === selectedStatus;
      return matchesLot && matchesVehicle && matchesStatus;
    });
  }, [selectedLotId, selectedStatus, selectedVehicleType, zones]);

  const gatesByZone = useMemo(() => {
    return gates.reduce((map, gate) => {
      const current = map.get(gate.zoneId) ?? [];
      current.push(gate);
      map.set(gate.zoneId, current);
      return map;
    }, new Map<string, Gate[]>());
  }, [gates]);

  const lanesByGate = useMemo(() => {
    return lanes.reduce((map, lane) => {
      const current = map.get(lane.gateId) ?? [];
      current.push(lane);
      map.set(lane.gateId, current);
      return map;
    }, new Map<string, Lane[]>());
  }, [lanes]);

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
              <Button
                size="lg"
                variant="primary"
                onClick={() => {
                  setLotError("");
                  setEditingLot(null);
                  setLotDrawerOpen(true);
                }}
              >
                <i className="fas fa-plus" />
                Thêm bãi xe
              </Button>
              <Button
                size="lg"
                variant="secondary"
                disabled={!selectedLotCanMutate}
                onClick={() => {
                  setLotError("");
                  setEditingLot(selectedParkingLot);
                  setLotDrawerOpen(true);
                }}
              >
                <i className="far fa-edit" />
                Sửa bãi
              </Button>
              <Button
                size="lg"
                variant="secondary"
                disabled={loadingLots || loadingTopology}
                onClick={() => {
                  void loadParkingLots();
                  void loadParkingTopology(selectedLotId);
                }}
              >
                <i className="fas fa-sync-alt" />
                Làm mới
              </Button>
              <Button size="lg" variant="secondary">
                <i className="fas fa-download" />
                Xuất dữ liệu
                <i className="fas fa-chevron-down tw-text-[0.72rem]" />
              </Button>
            </div>
          </div>

          <div className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
            <ParkingMetricCard delta={`${activeLotCount} đang hoạt động`} icon="fas fa-parking" label="Tổng bãi xe" tone="blue" value={parkingLots.length.toLocaleString("vi-VN")} />
            <ParkingMetricCard delta={`${maintenanceLotCount} đang bảo trì`} icon="fas fa-layer-group" label="Bãi đang mở" tone="green" value={activeLotCount.toLocaleString("vi-VN")} />
            <ParkingMetricCard delta={`${gates.length} tổng cổng`} icon="fas fa-door-open" label="Cổng hoạt động" tone="amber" value={activeGateCount.toLocaleString("vi-VN")} />
            <ParkingMetricCard delta={`${lanes.length} tổng làn`} icon="fas fa-road" label="Làn bảo trì" tone="red" value={maintenanceLaneCount.toLocaleString("vi-VN")} />
          </div>

          <Card className="tw-mt-4 tw-p-4">
            <div className="tw-grid tw-grid-cols-[260px_minmax(260px,1fr)_180px_180px_auto] tw-items-start tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
              <SelectMenu
                className="tw-self-start"
                ariaLabel="Chọn bãi xe"
                options={dynamicLotOptions}
                value={selectedLotId}
                clearValue={parkingLots[0]?.id}
                onChange={(value) => {
                  setSelectedLotId(value);
                  setDrawerOpen(false);
                  setSelectedNode(null);
                }}
              />
              <label className="tw-m-0 tw-box-border tw-flex tw-h-[42px] tw-self-start tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3.5 tw-shadow-[0_4px_10px_rgba(15,23,42,0.025)] tw-transition focus-within:tw-border-brand-200 focus-within:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]">
                <i className="fas fa-search tw-flex-shrink-0 tw-text-[0.92rem] tw-leading-none tw-text-vm-slate-500" />
                <input
                  className="tw-m-0 tw-h-full tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.92rem] tw-font-semibold tw-leading-[42px] tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500"
                  placeholder="Tìm khu, cổng, làn..."
                />
              </label>
              <SelectMenu className="tw-self-start" ariaLabel="Trạng thái" options={statusOptions} value={selectedStatus} onChange={setSelectedStatus} />
              <SelectMenu className="tw-self-start" ariaLabel="Loại phương tiện" options={dynamicVehicleTypeOptions} value={selectedVehicleType} onChange={setSelectedVehicleType} />
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
            <div className="tw-mt-3 tw-flex tw-flex-wrap tw-items-center tw-gap-2">
              <Button
                size="sm"
                variant="secondary"
                disabled={!selectedLotCanMutate || savingLot || selectedParkingLot.status === "ACTIVE"}
                onClick={() => void handleChangeLotStatus("ACTIVE")}
              >
                Kích hoạt
              </Button>
              <Button
                size="sm"
                variant="secondary"
                disabled={!selectedLotCanMutate || savingLot || selectedParkingLot.status === "MAINTENANCE"}
                onClick={() => void handleChangeLotStatus("MAINTENANCE")}
              >
                Bảo trì
              </Button>
              <Button
                size="sm"
                className="tw-border-red-200 tw-bg-white tw-text-red-600 hover:tw-bg-red-50"
                variant="secondary"
                disabled={!selectedLotCanMutate || savingLot || selectedParkingLot.status === "CLOSED"}
                onClick={() => void handleChangeLotStatus("CLOSED")}
              >
                Đóng bãi
              </Button>
              {selectedLotCanMutate ? null : (
                <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">Dữ liệu mẫu chỉ dùng để xem giao diện, không thể cập nhật.</span>
              )}
            </div>
            {lotError ? (
              <div className="tw-mt-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-red-700">
                {lotError}
              </div>
            ) : null}
            {topologyError ? (
              <div className="tw-mt-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-100 tw-bg-amber-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-amber-700">
                {topologyError}
              </div>
            ) : null}
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
            <OperationSummary gates={gates} lanes={lanes} selectedParkingLot={selectedParkingLot} />
          </div>
        </section>
      </div>

      <ParkingNodeDrawer
        error={topologyError}
        gate={selectedGate}
        isOpen={drawerOpen}
        lane={selectedLane}
        lanes={lanes}
        node={selectedNode}
        onClose={() => setDrawerOpen(false)}
        onSave={handleSaveNode}
        onStatusChange={handleChangeNodeStatus}
        saving={savingNode}
        vehicleTypeOptions={dynamicVehicleTypeOptions}
        zone={selectedZone}
      />
      <ParkingLotDrawer
        error={lotError}
        isOpen={lotDrawerOpen}
        lot={editingLot}
        saving={savingLot}
        onClose={() => {
          setLotDrawerOpen(false);
          setEditingLot(null);
          setLotError("");
        }}
        onSubmit={handleSubmitParkingLot}
      />
    </>
  );
}
