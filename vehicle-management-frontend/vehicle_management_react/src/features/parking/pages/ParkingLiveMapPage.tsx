import { isApiError } from "@/core/api/apiError";
import { getOperationAllocatedSpaceIds, getOperationMap as getLevelMap, getOperationLevels as getLevelsByLot, getOperationSummary as getLotSpaceSummary, getOperationPublishedLayout as getPublishedLayout, getOperationZones as getZones } from "@/features/parking/api/parkingOperationReadApi";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { Button, Card, Modal, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { getVehicleTypes, type VehicleTypeApiResponse } from "@/features/catalog/api/vehicleTypesApi";
import {
  createSpaceAllocation,
  getSpaceAllocations,
  getSpaceOpenSession,
  updateSpaceStatus,
  type LayoutElementApi,
  type LayoutSpaceItemApi,
  type LevelMapApi,
  type OpenSessionApi,
  type ParkingLevelApi,
  type ParkingSpaceAllocationApi,
  type ParkingSpaceApi,
  type SpaceSummaryApi,
} from "@/features/parking/api/parkingLayoutApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { type ZoneApiResponse } from "@/features/parking/api/parkingTopologyApi";
import { LiveOpsMap } from "@/features/parking/components/LiveOpsMap";
import { ParkingLotWorkspaceHeader } from "@/features/parking/components/ParkingLotWorkspaceHeader";
import { useParkingRealtime } from "@/features/parking/api/parkingRealtime";
import { SearchInput } from "@/shared/components/ui/SearchInput";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";

type SpotStatus = "AVAILABLE" | "OCCUPIED" | "RESERVED" | "MAINTENANCE";

type SpotView = {
  id: string;
  code: string;
  status: SpotStatus;
  allocated: boolean;
  plate: string | null;
  customer: string | null;
  since: string | null;
  zoneName: string;
  vehicleType: string;
};

const STATUS_BADGE: Record<SpotStatus, { label: string; className: string }> = {
  AVAILABLE: { label: "Còn trống", className: "tw-bg-emerald-50 tw-text-emerald-700" },
  OCCUPIED: { label: "Đang có xe", className: "tw-bg-red-50 tw-text-red-600" },
  RESERVED: { label: "Đặt trước", className: "tw-bg-amber-50 tw-text-amber-700" },
  MAINTENANCE: { label: "Bảo trì", className: "tw-bg-slate-100 tw-text-slate-600" },
};

const API_STATUS_OPTIONS = [
  { label: "Đang có xe", value: "OCCUPIED" },
  { label: "Còn trống", value: "AVAILABLE" },
  { label: "Đặt trước", value: "RESERVED" },
  { label: "Bảo trì", value: "MAINTENANCE" },
];

function toSpotView(space: ParkingSpaceApi, allocated: boolean, zoneName: string): SpotView {
  const status = (["AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE"].includes(space.status) ? space.status : "AVAILABLE") as SpotStatus;
  return {
    id: space.parkingSpaceId,
    code: space.code,
    status,
    allocated,
    plate: null,
    customer: space.manualOccupancyType === "REGISTERED" ? "Khách đăng ký" : space.manualOccupancyType === "VISITOR" ? "Khách vãng lai" : null,
    since: space.updatedAt,
    zoneName,
    vehicleType: space.vehicleTypeId ?? "Ô tô",
  };
}

/* ------------------------------------------------------------------ */
/* Khối trình bày dùng chung                                          */
/* ------------------------------------------------------------------ */

function KpiCard({ icon, iconWrap, label, value }: { icon: string; iconWrap: string; label: string; value: string }) {
  return (
    <Card className="tw-flex tw-items-center tw-gap-3 tw-px-4 tw-py-3.5">
      <span className={cn("tw-inline-flex tw-h-11 tw-w-11 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-[10px] tw-text-[1.15rem]", iconWrap)}>
        <i className={icon} aria-hidden="true" />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-truncate tw-text-[0.8rem] tw-font-medium tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-0.5 tw-block tw-text-[1.4rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
      </span>
    </Card>
  );
}

/* ------------------------------------------------------------------ */
/* Trang                                                              */
/* ------------------------------------------------------------------ */

export function ParkingLiveMapPage() {
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [levels, setLevels] = useState<ParkingLevelApi[]>([]);
  const [zones, setZones] = useState<ZoneApiResponse[]>([]);
  const [lotId, setLotId] = useState(searchParams.get("lotId") ?? searchParams.get("lot") ?? "");
  const [levelId, setLevelId] = useState(searchParams.get("levelId") ?? "");
  const [levelMap, setLevelMap] = useState<LevelMapApi | null>(null);
  const [publishedItems, setPublishedItems] = useState<LayoutSpaceItemApi[]>([]);
  const [publishedElements, setPublishedElements] = useState<LayoutElementApi[]>([]);
  const [vehicleTypes, setVehicleTypes] = useState<VehicleTypeApiResponse[]>([]);
  const [summary, setSummary] = useState<SpaceSummaryApi | null>(null);
  const [allocatedSpaceIds, setAllocatedSpaceIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);
  const [topologyLoading, setTopologyLoading] = useState(true);
  const [topologyError, setTopologyError] = useState("");
  const [mapError, setMapError] = useState("");
  const [reloadVersion, setReloadVersion] = useState(0);
  const mapRequest = useRef(0);
  const selection = useRef({ lotId, levelId });
  selection.current = { lotId, levelId };
  const canReadAllocations = hasAnyPermission(user, ["PARKING_SPACE_READ_ALL"]);

  const [zoneFilter, setZoneFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [vehicleFilter, setVehicleFilter] = useState("all");
  const [keyword, setKeyword] = useState("");

  const [selectedId, setSelectedId] = useState("");
  const [fullscreen, setFullscreen] = useState(false);
  const [realtimeOk, setRealtimeOk] = useState(true);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(() => Date.now());
  const [, setTick] = useState(0);

  const [detailAllocations, setDetailAllocations] = useState<ParkingSpaceAllocationApi[]>([]);
  const [detailSession, setDetailSession] = useState<OpenSessionApi | null>(null);
  const [manualStatus, setManualStatus] = useState<ParkingSpaceApi["status"]>("OCCUPIED");
  const [statusReason, setStatusReason] = useState("");
  const [operationBusy, setOperationBusy] = useState(false);
  const [assignmentOpen, setAssignmentOpen] = useState(false);
  const [subscriptionId, setSubscriptionId] = useState("");

  const canUpdateStatus = hasAnyPermission(user, ["PARKING_SPACE_STATUS_UPDATE_ALL"]);
  const canAssignSpace = hasAnyPermission(user, ["PARKING_SPACE_ASSIGN_ALL"]);

  const lot = useMemo(
    () => lots.find((item) => item.parkingLotId === lotId) ?? null,
    [lots, lotId],
  );

  /* Nạp chức năng hiện có: bãi xe -> tầng -> sơ đồ + tổng hợp */
  useEffect(() => {
    getParkingLots()
      .then((response) => {
        const list = response.data ?? [];
        setLots(list);
        if (!lotId && list.length > 0) setLotId(list[0].parkingLotId);
      })
      .catch(() => { setLots([]); setTopologyError("Không tải được danh sách bãi xe."); setTopologyLoading(false); });
    getVehicleTypes({ isActive: true })
      .then((response) => setVehicleTypes(response.data ?? []))
      .catch(() => setVehicleTypes([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    let alive = true;
    mapRequest.current += 1;
    setLevelId("");
    setLevels([]);
    setZones([]);
    setSelectedId("");
    setLevelMap(null);
    setPublishedItems([]);
    setPublishedElements([]);
    setSummary(null);
    setAllocatedSpaceIds(new Set());
    setTopologyError("");
    setMapError("");
    if (!lotId) { setTopologyLoading(false); return; }
    setTopologyLoading(true);
    setSearchParams((params) => { params.set("lotId", lotId); params.delete("lot"); params.delete("levelId"); return params; });
    Promise.all([getLevelsByLot(lotId), getZones({ parkingLotId: lotId }), getLotSpaceSummary(lotId)])
      .then(([levelResponse, zoneResponse, summaryResponse]) => {
        if (!alive) return;
        const list = levelResponse.data ?? [];
        setLevels(list);
        setZones(zoneResponse.data ?? []);
        setSummary(summaryResponse.data);
        setLevelId(list[0]?.parkingLevelId ?? "");
      })
      .catch((error) => {
        if (alive) setTopologyError(error instanceof Error ? error.message : "Không tải được tầng và khu vực của bãi.");
      })
      .finally(() => { if (alive) setTopologyLoading(false); });
    return () => { alive = false; mapRequest.current += 1; };
  }, [lotId, reloadVersion]);

  const refresh = useCallback(async (reloadPublishedLayout = true) => {
    if (!lotId || !levelId) {
      setLastUpdatedAt(Date.now());
      return;
    }
    const request = ++mapRequest.current;
    const isCurrent = () => request === mapRequest.current && selection.current.lotId === lotId && selection.current.levelId === levelId;
    setLoading(true);
    setMapError("");
    try {
      const [mapResponse, summaryResponse] = await Promise.all([getLevelMap(levelId), getLotSpaceSummary(lotId)]);
      if (!isCurrent()) return;
      const map = mapResponse.data;
      setLevelMap(map);
      setSummary(summaryResponse.data);
      const zoneIds = (map.zones ?? []).map((zone) => zone.zoneId);
      const [allocationLists, publishedLayouts] = await Promise.all([
        Promise.all(
          zoneIds.map((zoneId) => getOperationAllocatedSpaceIds(zoneId).then((response) => response.data)),
        ),
        reloadPublishedLayout
          ? Promise.all(
            zoneIds.map((zoneId) => getPublishedLayout(zoneId)
              .then((response) => ({ zoneId, layout: response.data }))
              .catch((error) => { if (isApiError(error) && error.status === 404) return null; throw error; })),
          )
          : Promise.resolve([]),
      ]);

      if (!isCurrent()) return;
      if (reloadPublishedLayout) {
        const layoutByZone = new Map(
          publishedLayouts
            .filter((entry): entry is NonNullable<typeof entry> => Boolean(entry))
            .map((entry) => [entry.zoneId, entry.layout]),
        );
        const nextItems = map.spaces.map((space): LayoutSpaceItemApi => {
          const layout = layoutByZone.get(space.zoneId);
          const publishedItem = layout?.items.find((item) => (
            item.parkingSpaceId === space.parkingSpaceId
            || (!item.parkingSpaceId && item.spaceCode === space.code)
          ));
          return publishedItem
            ? { ...publishedItem, parkingSpaceId: space.parkingSpaceId }
            : {
              layoutItemId: null,
              layoutVersionId: null,
              parkingSpaceId: space.parkingSpaceId,
              spaceCode: space.code,
              vehicleTypeId: space.vehicleTypeId,
              x: space.x ?? 0,
              y: space.y ?? 0,
              width: space.width ?? 60,
              height: space.height ?? 120,
              rotation: space.rotation ?? 0,
            };
        });
        const elementById = new Map<string, LayoutElementApi>();
        for (const entry of publishedLayouts) {
          if (!entry) continue;
          entry.layout.elements.forEach((element, index) => {
            const key = element.layoutElementId ?? `${entry.zoneId}:local:${index}`;
            elementById.set(key, element);
          });
        }
        setPublishedItems(nextItems);
        setPublishedElements([...elementById.values()]);
      }
      setAllocatedSpaceIds(new Set(allocationLists.flat()));
      setLastUpdatedAt(Date.now());
    } catch (error) {
      if (!isCurrent()) return;
      setMapError(error instanceof Error ? error.message : "Không tải được sơ đồ trực tiếp.");
      setLevelMap(null);
      setPublishedItems([]);
      setPublishedElements([]);
      setSummary(null);
      setAllocatedSpaceIds(new Set());
      toast.error("Không tải được sơ đồ trực tiếp của tầng đã chọn.");
    } finally {
      if (isCurrent()) setLoading(false);
    }
  }, [lotId, levelId, canReadAllocations]);

  useEffect(() => {
    setLevelMap(null);
    setPublishedItems([]);
    setPublishedElements([]);
    setSelectedId("");
    if (levelId) {
      setSearchParams((params) => {
        params.set("levelId", levelId);
        return params;
      });
      void refresh();
    } else {
      setAllocatedSpaceIds(new Set());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    return () => { mapRequest.current += 1; };
  }, [levelId, refresh]);

  useEffect(() => {
    const timer = window.setInterval(() => setTick((value) => value + 1), 5000);
    return () => window.clearInterval(timer);
  }, []);

  useParkingRealtime({
    parkingLotId: lotId || null,
    onEvent: (event) => {
      setRealtimeOk(true);
      if (event.parkingSpaceId && event.status && ["AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE"].includes(event.status)) {
        setLevelMap((current) => current ? {
          ...current,
          spaces: current.spaces.map((space) => (
            space.parkingSpaceId === event.parkingSpaceId
              ? { ...space, status: event.status as ParkingSpaceApi["status"], updatedAt: event.occurredAt }
              : space
          )),
        } : current);
      }
      if (!event.levelId || event.levelId === levelId) {
        void refresh(event.eventType === "PARKING_LAYOUT_PUBLISHED");
      }
    },
    onReconnect: () => {
      setRealtimeOk(true);
      void refresh();
    },
    onError: () => {
      setRealtimeOk(false);
      toast.error("Mất kết nối realtime, đang dùng dữ liệu snapshot.");
    },
  });

  const zoneNameOf = useCallback(
    (zoneId: string) => zones.find((zone) => zone.zoneId === zoneId)?.name ?? "—",
    [zones],
  );

  /* Chỉ hiển thị dữ liệu thật của tầng đang chọn; bãi chưa cấu hình dùng empty state. */
  const spots: SpotView[] = useMemo(() => {
    if (levelMap && levelMap.spaces.length > 0) {
      const sorted = [...levelMap.spaces].sort((a, b) => (a.y ?? 0) - (b.y ?? 0) || (a.x ?? 0) - (b.x ?? 0));
      return sorted.map((space) => toSpotView(space, allocatedSpaceIds.has(space.parkingSpaceId), zoneNameOf(space.zoneId)));
    }
    return [];
  }, [levelMap, allocatedSpaceIds, zoneNameOf]);

  const selected: SpotView | null = useMemo(
    () => spots.find((spot) => spot.id === selectedId) ?? spots.find((spot) => spot.code === selectedId) ?? null,
    [spots, selectedId],
  );

  const totals = useMemo(() => {
    if (summary) {
      return {
        total: summary.total,
        available: summary.available,
        occupied: summary.occupied,
        reserved: summary.reserved,
        maintenance: summary.maintenance,
      };
    }
    return { total: 0, available: 0, occupied: 0, reserved: 0, maintenance: 0 };
  }, [summary]);

  /* Chi tiết phiên mở + allocation của ô đang chọn (chức năng hiện có) */
  useEffect(() => {
    let alive = true;
    setDetailAllocations([]);
    setDetailSession(null);
    if (!selected || selected.id.startsWith("s-")) return;
    if (canReadAllocations) getSpaceAllocations(selected.id)
      .then((response) => {
        if (alive) setDetailAllocations(response.data ?? []);
      })
      .catch(() => undefined);
    getSpaceOpenSession(selected.id)
      .then((response) => {
        if (alive) setDetailSession(response.data ?? null);
      })
      .catch(() => undefined);
    return () => {
      alive = false;
    };
  }, [selected, canReadAllocations]);

  useEffect(() => {
    if (selected) setManualStatus(selected.status);
  }, [selected]);

  async function handleManualStatusUpdate() {
    if (!canUpdateStatus || !selected || !statusReason.trim() || selected.id.startsWith("s-")) {
      if (selected?.id.startsWith("s-")) toast.error("Dữ liệu demo — kết nối API để cập nhật trạng thái.");
      return;
    }
    setOperationBusy(true);
    try {
      await updateSpaceStatus(selected.id, {
        targetStatus: manualStatus,
        occupancyType: manualStatus === "OCCUPIED" ? "VISITOR" : null,
        reason: statusReason.trim(),
      });
      setStatusReason("");
      await refresh(false);
      toast.success("Đã cập nhật trạng thái ô đỗ.");
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Cập nhật trạng thái thất bại.");
    } finally {
      setOperationBusy(false);
    }
  }

  function handleAssignSpot() {
    if (!canAssignSpace) return;
    if (!selected || selected.id.startsWith("s-")) {
      navigate("/admin/subscription-approvals");
      return;
    }
    setSubscriptionId("");
    setAssignmentOpen(true);
  }

  async function handleConfirmAssignment() {
    if (!canAssignSpace || !selected || !subscriptionId.trim() || operationBusy) return;
    setOperationBusy(true);
    try {
      const from = new Date().toISOString().slice(0, 10);
      await createSpaceAllocation(selected.id, {
        subscriptionId: subscriptionId.trim(),
        effectiveFrom: from,
        effectiveTo: from,
      });
      await refresh(false);
      setAssignmentOpen(false);
      setSubscriptionId("");
      toast.success("Đã tạo giữ chỗ cho ô đỗ.");
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Gán chỗ thất bại.");
    } finally {
      setOperationBusy(false);
    }
  }

  const secondsAgo = Math.max(0, Math.round((Date.now() - lastUpdatedAt) / 1000));
  const badge = selected
    ? selected.allocated && selected.status === "AVAILABLE"
      ? { label: "Đã phân bổ", className: "tw-bg-purple-50 tw-text-purple-700" }
      : STATUS_BADGE[selected.status]
    : null;
  const detailPlate = detailSession?.licensePlateIn ?? selected?.plate ?? "—";
  const detailCustomer = selected?.allocated
    ? selected.customer ?? "Khách đăng ký"
    : selected?.status === "OCCUPIED"
      ? selected.customer ?? "Khách vãng lai"
      : selected?.customer ?? "—";
  const detailSince = detailSession?.checkInTime ?? selected?.since ?? "—";

  const levelOptions = levels.map((level) => ({
    value: level.parkingLevelId,
    label: `${level.name} (${level.code})`,
  }));
  const zoneOptions = [
    { label: "Tất cả khu", value: "all" },
    ...zones
      .filter((zone) => !levelMap || levelMap.zones.some((candidate) => candidate.zoneId === zone.zoneId))
      .map((zone) => ({ label: `${zone.code} — ${zone.name}`, value: zone.zoneId })),
  ];

  if (!lot) {
    return <main className="tw-p-6"><Card className="tw-p-6"><h1 className="tw-text-xl tw-font-bold">Sơ đồ trực tiếp</h1>
      <p role="status">{topologyError || "Chưa có bãi xe trong phạm vi truy cập hoặc đang tải danh sách bãi."}</p>
    </Card></main>;
  }

  return (
    <main className="tw-mx-auto tw-w-full tw-max-w-[1480px] tw-px-5 tw-py-5 max-[768px]:tw-px-4">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="tw-flex tw-items-center tw-gap-2 tw-text-[0.84rem]">
        <Link to="/admin/parking-lots" className="tw-font-medium tw-text-vm-slate-500 hover:tw-text-vm-primary hover:tw-no-underline">
          Bãi xe
        </Link>
        <span className="tw-text-vm-slate-200">/</span>
        <span className="tw-font-semibold tw-text-vm-slate-900">{lot.name}</span>
      </nav>

      <ParkingLotWorkspaceHeader
        activeTab="live"
        parkingLot={lot}
        parkingLots={lots}
        onParkingLotChange={(nextLotId) => {
          setLotId(nextLotId);
          setLevelId("");
          setZoneFilter("all");
          setSelectedId("");
        }}
      />

      {/* Realtime banner */}
      <Card className="tw-mt-4 tw-flex tw-flex-wrap tw-items-center tw-gap-3 tw-px-4 tw-py-3">
        <span className="tw-relative tw-inline-flex tw-h-4 tw-w-4">
          <span className={cn("tw-absolute tw-inline-flex tw-h-full tw-w-full tw-animate-ping tw-rounded-full tw-opacity-60", realtimeOk ? "tw-bg-emerald-400" : "tw-bg-red-400")} />
          <span className={cn("tw-relative tw-inline-flex tw-h-4 tw-w-4 tw-items-center tw-justify-center tw-rounded-full tw-border-2 tw-border-solid tw-border-white", realtimeOk ? "tw-bg-emerald-500" : "tw-bg-red-500")} />
        </span>
        <strong className={cn("tw-text-[0.9rem] tw-font-extrabold", realtimeOk ? "tw-text-emerald-700" : "tw-text-red-600")}>
          {realtimeOk ? "Realtime đang kết nối" : "Mất kết nối realtime"}
        </strong>
        <span className="tw-hidden tw-h-4 tw-w-px tw-bg-vm-slate-100 sm:tw-block" />
        <span className="tw-text-[0.84rem] tw-font-medium tw-text-vm-slate-500">{summary && !mapError && !topologyError ? `Cập nhật ${secondsAgo} giây trước` : "Chưa có dữ liệu sơ đồ"}</span>
        <Button variant="secondary" size="sm" className="tw-ml-auto tw-rounded-vm-md tw-font-bold tw-text-vm-primary" loading={loading || topologyLoading} onClick={() => setReloadVersion((version) => version + 1)}>
          <i className="fas fa-sync-alt" aria-hidden="true" />
          Làm mới
        </Button>
      </Card>

      {topologyError || mapError ? <div role="alert" className="tw-mt-4 tw-rounded-lg tw-bg-amber-50 tw-p-4 tw-text-amber-900">Không tải được dữ liệu sơ đồ: {topologyError || mapError}. Vui lòng làm mới để thử lại.</div> : null}
      {/* KPI */}
      <div className="tw-mt-4 tw-grid tw-grid-cols-5 tw-gap-3.5 max-[1200px]:tw-grid-cols-3 max-[720px]:tw-grid-cols-2">
        <KpiCard icon="fas fa-th-large" iconWrap="tw-bg-brand-50 tw-text-vm-primary" label="Tổng số ô" value={summary && !topologyError && !mapError ? String(totals.total) : "—"} />
        <KpiCard icon="fas fa-check-circle" iconWrap="tw-bg-[#dcfce7] tw-text-[#16a34a]" label="Còn trống" value={summary && !topologyError && !mapError ? String(totals.available) : "—"} />
        <KpiCard icon="fas fa-car" iconWrap="tw-bg-[#fee2e2] tw-text-[#dc2626]" label="Đang có xe" value={summary && !topologyError && !mapError ? String(totals.occupied) : "—"} />
        <KpiCard icon="far fa-clock" iconWrap="tw-bg-[#fef3c7] tw-text-[#d97706]" label="Đặt trước" value={summary && !topologyError && !mapError ? String(totals.reserved) : "—"} />
        <KpiCard icon="fas fa-wrench" iconWrap="tw-bg-vm-slate-50 tw-text-vm-slate-700" label="Bảo trì" value={summary && !topologyError && !mapError ? String(totals.maintenance) : "—"} />
      </div>

      {/* Bộ lọc */}
      <div className="tw-mt-4 tw-grid tw-grid-cols-[190px_170px_180px_150px_minmax(0,1fr)] tw-gap-3 max-[1200px]:tw-grid-cols-3 max-[720px]:tw-grid-cols-1">
        <SelectMenu
          ariaLabel="Chọn tầng"
          value={levelId}
          onChange={(nextLevelId) => {
            setLevelId(nextLevelId);
            setZoneFilter("all");
            setSelectedId("");
          }}
          options={[{ label: topologyLoading ? "Đang tải tầng…" : topologyError ? "Không tải được tầng" : levels.length > 0 ? "Chọn tầng" : "Bãi chưa có tầng", value: "" }, ...levelOptions]}
          clearValue=""
        />
        <SelectMenu ariaLabel="Lọc khu" value={zoneFilter} onChange={setZoneFilter} options={zoneOptions} />
        <SelectMenu
          ariaLabel="Lọc trạng thái"
          value={statusFilter}
          onChange={setStatusFilter}
          options={[
            { label: "Tất cả trạng thái", value: "all" },
            { label: "Còn trống", value: "AVAILABLE" },
            { label: "Đang có xe", value: "OCCUPIED" },
            { label: "Đặt trước", value: "RESERVED" },
            { label: "Bảo trì", value: "MAINTENANCE" },
            { label: "Đã phân bổ", value: "allocated" },
          ]}
        />
        <SelectMenu
          ariaLabel="Lọc loại xe"
          value={vehicleFilter}
          onChange={setVehicleFilter}
          options={[
            { label: "Tất cả loại xe", value: "all" },
            ...vehicleTypes.map((vehicleType) => ({
              label: `${vehicleType.code} — ${vehicleType.name}`,
              value: vehicleType.vehicleTypeId,
            })),
          ]}
        />
        <SearchInput value={keyword} onChange={setKeyword} placeholder="Tìm mã ô..." aria-label="Tìm mã ô" />
      </div>

      {/* Sơ đồ + chi tiết */}
      <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_350px] tw-items-start tw-gap-4 max-[1200px]:tw-grid-cols-1">
        <Card className={cn("tw-overflow-hidden tw-p-3", fullscreen && "tw-fixed tw-inset-4 tw-z-[1100] tw-flex tw-flex-col tw-overflow-auto")}>
          <div className="tw-mb-2 tw-flex tw-justify-end">
            <button
              type="button"
              aria-label={fullscreen ? "Thoát toàn màn hình" : "Toàn màn hình"}
              onClick={() => setFullscreen((value) => !value)}
              className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-vm-slate-700 hover:tw-bg-vm-slate-25"
            >
              <i className={cn("tw-text-[0.8rem]", fullscreen ? "fas fa-compress" : "fas fa-expand")} aria-hidden="true" />
            </button>
          </div>
          {levelMap ? (
            <LiveOpsMap
              key={levelId}
              allocatedSpaceIds={allocatedSpaceIds}
              canvasHeight={Number(levelMap.level.canvasHeight ?? 700) || 700}
              canvasWidth={Number(levelMap.level.canvasWidth ?? 1000) || 1000}
              elements={publishedElements}
              filters={{
                status: statusFilter === "allocated" ? "all" : statusFilter,
                vehicleTypeId: vehicleFilter,
                zoneId: zoneFilter,
                occupancy: "all",
                allocation: statusFilter === "allocated" ? "allocated" : "all",
                keyword,
              }}
              items={publishedItems}
              levelMap={levelMap}
              onSelectSpace={(space) => setSelectedId(space?.parkingSpaceId ?? "")}
              selectedSpaceId={selected?.id ?? null}
              vehicleTypes={vehicleTypes}
            />
          ) : (
            <div className="tw-grid tw-min-h-[590px] tw-place-items-center tw-rounded-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-p-8 tw-text-center">
              <div>
                <i className="fas fa-drafting-compass tw-text-4xl tw-text-vm-primary" aria-hidden="true" />
                <p className="tw-mb-0 tw-mt-3 tw-text-sm tw-font-bold tw-text-vm-slate-600">
                  {levelId ? "Không tải được sơ đồ đã phát hành của tầng này." : "Hãy chọn tầng để xem sơ đồ trực tiếp."}
                </p>
              </div>
            </div>
          )}
        </Card>

        {/* Chi tiết ô */}
        <Card className="tw-p-5">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <h2 className="tw-m-0 tw-text-[1.08rem] tw-font-extrabold tw-text-vm-slate-900">Chi tiết ô {selected?.code ?? "—"}</h2>
            {badge ? (
              <span className={cn("tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.72rem] tw-font-bold", badge.className)}>
                <span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-current" />
                {badge.label}
              </span>
            ) : null}
          </div>

          <div className="tw-mt-3 tw-grid tw-gap-2.5">
            <p className="tw-m-0 tw-flex tw-items-center tw-gap-2.5 tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">
              <i className="far fa-id-card tw-w-5 tw-text-center tw-text-vm-slate-500" aria-hidden="true" />
              {detailPlate}
            </p>
            <p className="tw-m-0 tw-flex tw-items-center tw-gap-2.5 tw-text-[0.86rem] tw-text-vm-slate-700">
              <i className="far fa-user tw-w-5 tw-text-center tw-text-vm-slate-500" aria-hidden="true" />
              {detailCustomer}
            </p>
            <p className="tw-m-0 tw-flex tw-items-center tw-gap-2.5 tw-text-[0.86rem] tw-text-vm-slate-700">
              <i className="far fa-clock tw-w-5 tw-text-center tw-text-vm-slate-500" aria-hidden="true" />
              {detailSince}
            </p>
            <p className="tw-m-0 tw-flex tw-items-center tw-gap-2.5 tw-text-[0.86rem] tw-text-vm-slate-700">
              <i className="far fa-map tw-w-5 tw-text-center tw-text-vm-slate-500" aria-hidden="true" />
              {selected?.zoneName ?? "—"}
            </p>
            <p className="tw-m-0 tw-flex tw-items-center tw-gap-2.5 tw-text-[0.86rem] tw-text-vm-slate-700">
              <i className="fas fa-car tw-w-5 tw-text-center tw-text-vm-slate-500" aria-hidden="true" />
              {selected?.vehicleType ?? "Ô tô"}
            </p>
            {detailAllocations.length > 0 ? (
              <p className="tw-m-0 tw-text-[0.8rem] tw-font-semibold tw-text-vm-primary">
                Đang giữ {detailAllocations.length} allocation hiệu lực.
              </p>
            ) : null}
          </div>

          <div className="tw-my-4 tw-h-px tw-bg-vm-slate-100" />

          {canUpdateStatus ? <>
          <p className="tw-m-0 tw-text-[0.86rem] tw-font-extrabold tw-text-vm-slate-900">Cập nhật trạng thái</p>
          <div className="tw-mt-2">
            <SelectMenu
              ariaLabel="Trạng thái mới"
              value={manualStatus}
              onChange={(value) => setManualStatus(value as ParkingSpaceApi["status"])}
              options={API_STATUS_OPTIONS}
              disabled={!canUpdateStatus}
            />
          </div>
          <p className="tw-m-0 tw-mt-3 tw-text-[0.82rem] tw-text-vm-slate-500">Lý do (bắt buộc)</p>
          <input
            value={statusReason}
            onChange={(event) => setStatusReason(event.target.value)}
            maxLength={500}
            placeholder="Nhập lý do cập nhật..."
            aria-label="Lý do cập nhật trạng thái"
            disabled={!canUpdateStatus}
            className="tw-mt-1.5 tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-500 focus:tw-border-brand-200 focus:tw-shadow-vm-focus disabled:tw-bg-vm-slate-25"
          />
          <Button className="tw-mt-3 tw-h-11 tw-w-full tw-rounded-vm-md tw-font-bold" loading={operationBusy} disabled={!canUpdateStatus || !statusReason.trim()} onClick={() => void handleManualStatusUpdate()}>
            Cập nhật trạng thái
          </Button>
          </> : null}
          {canAssignSpace ? <Button variant="secondary" className="tw-mt-2.5 tw-h-11 tw-w-full tw-rounded-vm-md tw-border-vm-primary tw-font-bold tw-text-vm-primary hover:tw-bg-brand-50" disabled={!canAssignSpace} onClick={handleAssignSpot}>
            <i className="far fa-user-plus tw-text-[0.9rem]" aria-hidden="true" />
            Gán chỗ
          </Button> : null}
          <Link to="/admin/swipe/sessions" className="tw-mt-2.5 tw-block tw-text-center tw-text-[0.86rem] tw-font-bold tw-text-vm-primary hover:tw-underline">
            Xem phiên gửi xe <i className="fas fa-external-link-alt tw-text-[0.72rem]" aria-hidden="true" />
          </Link>

          <div className="tw-my-4 tw-h-px tw-bg-vm-slate-100" />

          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
            <h3 className="tw-m-0 tw-text-[0.92rem] tw-font-extrabold tw-text-vm-slate-900">Hoạt động gần đây</h3>
            <Link to="/admin/swipe/sessions" className="tw-text-[0.82rem] tw-font-bold tw-text-vm-primary hover:tw-underline">
              Xem tất cả
            </Link>
          </div>
          <div className="tw-mt-3 tw-grid tw-gap-3">
            <div className="tw-flex tw-items-center tw-gap-3">
              <span className="tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-[#dcfce7] tw-text-[#16a34a]">
                <i className="fas fa-check tw-text-[0.8rem]" aria-hidden="true" />
              </span>
              <span className="tw-min-w-0 tw-flex-1">
                <span className="tw-block tw-truncate tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-900">A-08 vừa check-out</span>
                <span className="tw-block tw-text-[0.76rem] tw-text-vm-slate-500">Vừa rời bãi</span>
              </span>
              <span className="tw-text-[0.78rem] tw-font-medium tw-text-vm-slate-500">10:24</span>
            </div>
            <div className="tw-flex tw-items-center tw-gap-3">
              <span className="tw-inline-flex tw-h-9 tw-w-9 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-50 tw-text-vm-slate-700">
                <i className="fas fa-wrench tw-text-[0.8rem]" aria-hidden="true" />
              </span>
              <span className="tw-min-w-0 tw-flex-1">
                <span className="tw-block tw-truncate tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-900">A-21 chuyển bảo trì</span>
                <span className="tw-block tw-text-[0.76rem] tw-text-vm-slate-500">Bảo trì định kỳ</span>
              </span>
              <span className="tw-text-[0.78rem] tw-font-medium tw-text-vm-slate-500">09:17</span>
            </div>
          </div>
        </Card>
      </div>
      <Modal
        open={assignmentOpen && Boolean(selected)}
        title="Gán chỗ đăng ký"
        description={selected ? `Tạo phân bổ cho ô ${selected.code}. Ô đã phân bổ sẽ không được tính là còn trống cho khách khác.` : undefined}
        onClose={() => { if (!operationBusy) setAssignmentOpen(false); }}
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-2">
            <Button variant="secondary" disabled={operationBusy} onClick={() => setAssignmentOpen(false)}>Hủy</Button>
            <Button loading={operationBusy} disabled={!subscriptionId.trim()} onClick={() => void handleConfirmAssignment()}>
              <i className="fas fa-user-check" aria-hidden="true" /> Xác nhận gán
            </Button>
          </div>
        )}
      >
        <label className="tw-grid tw-gap-1.5 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">
          Mã subscription (UUID)
          <input
            autoFocus
            value={subscriptionId}
            onChange={(event) => setSubscriptionId(event.target.value)}
            placeholder="Nhập mã subscription"
            aria-label="Mã subscription"
            className="tw-h-11 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-medium tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-shadow-vm-focus"
          />
        </label>
      </Modal>
    </main>
  );
}
