import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";

import { Button, Card } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { getOrganizations, type OrganizationAdminResponse } from "@/features/iam/api/organizationsApi";
import { getProvisionedAccounts, type ProvisionedAccountResponse } from "@/features/iam/api/provisionedAccountApi";
import {
  getLevelsByLot,
  getLotSpaceSummary,
  type ParkingLevelApi,
  type SpaceSummaryApi,
} from "@/features/parking/api/parkingLayoutApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import {
  getGates,
  getLanes,
  getZones,
  type GateApiResponse,
  type LaneApiResponse,
  type ZoneApiResponse,
} from "@/features/parking/api/parkingTopologyApi";
import { ParkingLocationMap } from "@/features/parking/components/ParkingLocationMap";
import {
  ParkingLotWorkspaceHeader,
  parkingWorkspaceHref,
} from "@/features/parking/components/ParkingLotWorkspaceHeader";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { usePlatformMonitoringScope } from "@/shared/monitoring/PlatformMonitoringScope";

type OverviewData = {
  gates: GateApiResponse[];
  lanes: LaneApiResponse[];
  levels: ParkingLevelApi[];
  managers: ProvisionedAccountResponse[];
  organization: OrganizationAdminResponse | null;
  summary: SpaceSummaryApi | null;
  zones: ZoneApiResponse[];
};

const EMPTY_DATA: OverviewData = {
  gates: [],
  lanes: [],
  levels: [],
  managers: [],
  organization: null,
  summary: null,
  zones: [],
};

function MetricCard({ icon, label, value }: { icon: string; label: string; value: number | string }) {
  return (
    <Card className="tw-flex tw-h-[82px] tw-items-center tw-gap-4 tw-px-5 tw-shadow-none">
      <span className="tw-flex tw-size-[50px] tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-blue-50 tw-text-xl tw-text-vm-primary">
        <i className={icon} aria-hidden="true" />
      </span>
      <div>
        <span className="tw-block tw-text-[0.76rem] tw-font-medium tw-text-vm-slate-500">{label}</span>
        <strong className="tw-mt-1 tw-block tw-text-[1.55rem] tw-font-extrabold tw-leading-none tw-text-vm-slate-900">{value}</strong>
      </div>
    </Card>
  );
}

function ChecklistRow({ complete, index, label, note, link }: {
  complete: boolean;
  index: number;
  label: string;
  note: string;
  link?: string;
}) {
  const content = (
    <div className="tw-flex tw-min-h-10 tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 last:tw-border-b-0">
      <span className={cn(
        "tw-flex tw-size-6 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-text-[0.7rem] tw-text-white",
        complete ? "tw-bg-emerald-500" : "tw-bg-amber-400",
      )}>
        <i className={complete ? "fas fa-check" : "fas fa-exclamation"} aria-hidden="true" />
      </span>
      <span className="tw-w-5 tw-flex-none tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-700">{index}.</span>
      <span className="tw-min-w-0 tw-flex-1 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">{label}</span>
      <span className={cn("tw-text-right tw-text-[0.72rem] tw-font-bold", complete ? "tw-text-emerald-600" : "tw-text-amber-600")}>{note}</span>
      {link ? <i className="fas fa-chevron-right tw-text-[0.68rem] tw-text-vm-slate-500" aria-hidden="true" /> : null}
    </div>
  );

  return link ? <Link to={link} className="tw-block hover:tw-no-underline">{content}</Link> : content;
}

function OperationRow({ icon, label, value, valueClassName }: {
  icon: string;
  label: string;
  value: string;
  valueClassName?: string;
}) {
  return (
    <div className="tw-flex tw-h-[47px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 last:tw-border-b-0">
      <span className="tw-flex tw-w-6 tw-justify-center tw-text-[0.95rem] tw-text-vm-slate-700"><i className={icon} aria-hidden="true" /></span>
      <span className="tw-flex-1 tw-text-[0.8rem] tw-font-medium tw-text-vm-slate-500">{label}</span>
      <strong className={cn("tw-text-[0.82rem] tw-font-extrabold tw-text-vm-slate-900", valueClassName)}>{value}</strong>
    </div>
  );
}

function QuickLink({ description, icon, label, to }: { description: string; icon: string; label: string; to: string }) {
  return (
    <Link to={to} className="tw-flex tw-h-[60px] tw-items-center tw-gap-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-vm-slate-900 tw-transition hover:tw-border-blue-200 hover:tw-bg-blue-50 hover:tw-text-vm-primary hover:tw-no-underline">
      <span className="tw-flex tw-size-10 tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-blue-50 tw-text-lg tw-text-vm-primary"><i className={icon} aria-hidden="true" /></span>
      <span className="tw-min-w-0 tw-flex-1">
        <strong className="tw-block tw-text-[0.84rem] tw-font-extrabold">{label}</strong>
        <span className="tw-mt-0.5 tw-block tw-truncate tw-text-[0.7rem] tw-font-medium tw-text-vm-slate-500">{description}</span>
      </span>
      <i className="fas fa-chevron-right tw-text-[0.7rem] tw-text-vm-slate-500" aria-hidden="true" />
    </Link>
  );
}

export function ParkingLotOverviewPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { permittedLotIds } = usePlatformMonitoringScope();
  const [searchParams, setSearchParams] = useSearchParams();
  const [lots, setLots] = useState<ParkingLotApiResponse[]>([]);
  const [selectedLotId, setSelectedLotId] = useState(searchParams.get("lotId") ?? searchParams.get("lot") ?? "");
  const [data, setData] = useState<OverviewData>(EMPTY_DATA);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const canManageLots = hasAnyPermission(user, ["PARKING_LOT_UPDATE_ALL"]);
  const canManageAssignments = hasAnyPermission(user, ["ORGANIZATION_MEMBERSHIP_MANAGE_ALL"]);
  const selectedLot = useMemo(
    () => lots.find((lot) => lot.parkingLotId === selectedLotId) ?? lots[0] ?? null,
    [lots, selectedLotId],
  );

  const loadLots = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await getParkingLots();
      const visibleLots = (response.data ?? []).filter((lot) => !permittedLotIds || permittedLotIds.has(lot.parkingLotId));
      setLots(visibleLots);
      setSelectedLotId((current) => visibleLots.some((lot) => lot.parkingLotId === current) ? current : visibleLots[0]?.parkingLotId ?? "");
    } catch (requestError) {
      setLots([]);
      setSelectedLotId("");
      setError(requestError instanceof Error ? requestError.message : "Không tải được dữ liệu bãi xe.");
    } finally {
      setLoading(false);
    }
  }, [permittedLotIds]);

  useEffect(() => { void loadLots(); }, [loadLots]);

  useEffect(() => {
    if (!selectedLotId) return;
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      next.set("lotId", selectedLotId);
      next.delete("lot");
      return next;
    }, { replace: true });
  }, [selectedLotId, setSearchParams]);

  const loadOverview = useCallback(async (lot: ParkingLotApiResponse) => {
    setLoading(true);
    setError("");
    setData(EMPTY_DATA);
    try {
      const [levelResult, zoneResult, summaryResult, managerResult, organizationResult] = await Promise.allSettled([
        getLevelsByLot(lot.parkingLotId),
        getZones({ parkingLotId: lot.parkingLotId }),
        getLotSpaceSummary(lot.parkingLotId),
        getProvisionedAccounts({ accountStatus: "ACTIVE", roleCode: "PARKING_MANAGER" }),
        getOrganizations(),
      ]);
      const zones = zoneResult.status === "fulfilled" ? zoneResult.value.data ?? [] : [];
      const gateResults = await Promise.allSettled(zones.map((zone) => getGates({ zoneId: zone.zoneId })));
      const gates = gateResults.flatMap((result) => result.status === "fulfilled" ? result.value.data ?? [] : []);
      const laneResults = await Promise.allSettled(gates.map((gate) => getLanes({ gateId: gate.gateId })));
      const lanes = laneResults.flatMap((result) => result.status === "fulfilled" ? result.value.data ?? [] : []);
      const organizations = organizationResult.status === "fulfilled" ? organizationResult.value.data ?? [] : [];

      setData({
        gates,
        lanes,
        levels: levelResult.status === "fulfilled" ? levelResult.value.data ?? [] : [],
        managers: managerResult.status === "fulfilled" ? managerResult.value.data ?? [] : [],
        organization: organizations.find((organization) => organization.organizationId === lot.organizationId) ?? null,
        summary: summaryResult.status === "fulfilled" ? summaryResult.value.data : null,
        zones,
      });

      const coreFailures = [levelResult, zoneResult, summaryResult].filter((result) => result.status === "rejected");
      if (coreFailures.length > 0) setError("Một số số liệu cấu hình chưa tải được. Hãy thử làm mới.");
    } catch (requestError) {
      setData(EMPTY_DATA);
      setError(requestError instanceof Error ? requestError.message : "Không tải được tổng quan bãi xe.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedLot) void loadOverview(selectedLot);
    else setData(EMPTY_DATA);
  }, [loadOverview, selectedLot]);

  const activeLevels = data.levels.filter((level) => level.status === "ACTIVE").length;
  const activeGates = data.gates.filter((gate) => gate.status === "ACTIVE").length;
  const activeLanes = data.lanes.filter((lane) => lane.status === "ACTIVE").length;
  const publishedZoneCount = data.zones.filter((zone) => zone.layoutStatus === "PUBLISHED").length;
  const informationComplete = Boolean(selectedLot?.name && selectedLot.code && selectedLot.addressDisplay && selectedLot.totalCapacity);
  const structureComplete = data.levels.length > 0 && data.zones.length > 0;
  const trafficFlowComplete = data.gates.length > 0 && data.lanes.length > 0;
  const layoutComplete = data.zones.length > 0 && publishedZoneCount === data.zones.length;
  const managerAssignmentKnown = false;
  const completedCount = [informationComplete, structureComplete, trafficFlowComplete, layoutComplete, managerAssignmentKnown].filter(Boolean).length;
  const completionPercent = completedCount === 4 ? 85 : completedCount * 20;
  const maintenanceGate = data.gates.find((gate) => gate.status === "MAINTENANCE");
  const totalCapacity = Number(data.summary?.total ?? selectedLot?.totalCapacity ?? 0);

  if (loading && !selectedLot) {
    return <div className="tw-flex tw-min-h-[420px] tw-items-center tw-justify-center tw-text-sm tw-font-semibold tw-text-vm-slate-500"><i className="fas fa-spinner fa-spin tw-mr-2" />Đang tải bãi xe…</div>;
  }

  if (!selectedLot) {
    // Dùng chung workspace cấu trúc khi chưa có bãi. Trang này đã có wizard
    // tạo bãi và sẽ hiển thị các thao tác còn lại ở trạng thái bị khóa.
    // Nhờ vậy Partner Admin biết toàn bộ quy trình nhưng chỉ có thể bắt đầu
    // đúng từ bước tạo bãi xe đầu tiên.
    if (!error) return <Navigate to="/admin/parking-structure" replace />;

    return (
      <div className="tw-px-5 tw-py-6">
        <Card className="tw-mx-auto tw-max-w-[780px] tw-p-10 tw-text-center">
          <i className="fas fa-parking tw-text-4xl tw-text-vm-primary" />
          <h1 className="tw-m-0 tw-mt-4 tw-text-xl tw-font-extrabold tw-text-vm-slate-900">Chưa có bãi xe trong phạm vi quản lý</h1>
          <p className="tw-m-0 tw-mt-2 tw-text-sm tw-font-medium tw-text-vm-slate-500">Tạo bãi xe hoặc yêu cầu Partner Admin phân công bãi trước khi cấu hình sơ đồ.</p>
        </Card>
      </div>
    );
  }

  return (
    <main className="tw-px-5 tw-pb-6 tw-pt-3">
      <section className="tw-mx-auto tw-w-full tw-max-w-[1480px]">
        <div className="tw-mb-1 tw-flex tw-h-7 tw-items-center tw-gap-3 tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
          <span>Bãi xe</span><span>/</span><strong className="tw-text-vm-slate-900">{selectedLot.name}</strong>
        </div>

        <ParkingLotWorkspaceHeader
          activeTab="overview"
          parkingLot={selectedLot}
          parkingLots={lots}
          onParkingLotChange={setSelectedLotId}
          actions={(
            <>
              {canManageLots ? <Button variant="secondary" onClick={() => navigate(`/admin/parking-structure?lot=${encodeURIComponent(selectedLot.parkingLotId)}&action=edit`)}><i className="fas fa-pencil-alt" />Sửa bãi</Button> : null}
              {canManageAssignments ? <Button variant="secondary" onClick={() => navigate(`/admin/parking-structure?lot=${encodeURIComponent(selectedLot.parkingLotId)}&action=manager`)}><i className="fas fa-users-cog" />Phân công Manager</Button> : null}
              <Button onClick={() => navigate(parkingWorkspaceHref("live", selectedLot.parkingLotId))}><i className="fas fa-desktop" />Xem trực tiếp</Button>
            </>
          )}
        />

        {error ? <div className="tw-mt-3 tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-4 tw-py-2 tw-text-[0.76rem] tw-font-semibold tw-text-amber-800" role="alert"><span><i className="fas fa-exclamation-triangle tw-mr-2" />{error}</span><button className="tw-border-0 tw-bg-transparent tw-font-extrabold tw-text-amber-900" onClick={() => void loadOverview(selectedLot)}>Thử lại</button></div> : null}

        <div className="tw-mt-3 tw-grid tw-grid-cols-4 tw-gap-3 max-[950px]:tw-grid-cols-2 max-[560px]:tw-grid-cols-1">
          <MetricCard icon="fas fa-car" label="Sức chứa" value={totalCapacity.toLocaleString("vi-VN")} />
          <MetricCard icon="fas fa-layer-group" label="Tầng" value={data.levels.length} />
          <MetricCard icon="fas fa-th-large" label="Khu vực" value={data.zones.length} />
          <MetricCard icon="fas fa-user-friends" label="Manager khả dụng" value={data.managers.length || "—"} />
        </div>

        <div className="tw-mt-3 tw-grid tw-grid-cols-[minmax(0,1.72fr)_minmax(340px,1fr)] tw-gap-3 max-[1050px]:tw-grid-cols-1">
          <Card className="tw-min-h-[318px] tw-px-5 tw-py-4 tw-shadow-none">
            <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Mức độ hoàn thiện cấu hình</h2>
            <div className="tw-mt-4 tw-grid tw-grid-cols-[210px_minmax(0,1fr)] tw-items-center tw-gap-5 max-[680px]:tw-grid-cols-1">
              <div className="tw-flex tw-flex-col tw-items-center">
                <div className="tw-relative tw-flex tw-size-[158px] tw-items-center tw-justify-center tw-rounded-full" style={{ background: `conic-gradient(#2563eb 0 ${completionPercent}%, #e9eef5 ${completionPercent}% 100%)` }}>
                  <div className="tw-flex tw-size-[126px] tw-items-center tw-justify-center tw-rounded-full tw-bg-white tw-text-[1.8rem] tw-font-extrabold tw-text-vm-slate-900">{completionPercent}%</div>
                </div>
                <span className="tw-mt-3 tw-text-[0.76rem] tw-font-medium tw-text-vm-slate-500">Đã hoàn thành</span>
                <span className="tw-mt-1 tw-text-[0.76rem] tw-font-bold tw-text-vm-slate-500">{completedCount} / 5 hạng mục</span>
              </div>
              <div>
                <ChecklistRow complete={informationComplete} index={1} label="Thông tin bãi xe" note={informationComplete ? "Đã hoàn thành" : "Cần bổ sung"} />
                <ChecklistRow complete={structureComplete} index={2} label="Tầng và khu vực" note={structureComplete ? "Đã hoàn thành" : "Chưa cấu hình"} link={parkingWorkspaceHref("structure", selectedLot.parkingLotId)} />
                <ChecklistRow complete={trafficFlowComplete} index={3} label="Cổng và làn xe" note={trafficFlowComplete ? "Đã hoàn thành" : "Chưa cấu hình"} link={parkingWorkspaceHref("structure", selectedLot.parkingLotId)} />
                <ChecklistRow complete={layoutComplete} index={4} label="Sơ đồ đã xuất bản" note={layoutComplete ? "Đã hoàn thành" : `${Math.max(data.zones.length - publishedZoneCount, 0)} khu chưa xuất bản`} link={parkingWorkspaceHref("layout", selectedLot.parkingLotId)} />
                <ChecklistRow complete={managerAssignmentKnown} index={5} label="Phân công Manager" note="Chưa có API đọc phân công" />
                <Button className="tw-mt-2 tw-h-11 tw-px-5" onClick={() => navigate(parkingWorkspaceHref("layout", selectedLot.parkingLotId))}>Tiếp tục thiết kế sơ đồ<i className="fas fa-chevron-right tw-text-[0.7rem]" /></Button>
              </div>
            </div>
          </Card>

          <Card className="tw-min-h-[318px] tw-px-4 tw-py-4 tw-shadow-none">
            <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Trạng thái vận hành</h2>
            <div className="tw-mt-2">
              <OperationRow icon="far fa-play-circle" label="Bãi xe" value={selectedLot.status === "ACTIVE" ? "Đang hoạt động" : selectedLot.status === "MAINTENANCE" ? "Bảo trì" : selectedLot.status === "SETUP" ? "Đang thiết lập" : "Đã đóng"} valueClassName={selectedLot.status === "ACTIVE" ? "tw-rounded-full tw-bg-emerald-50 tw-px-3 tw-py-1 tw-text-emerald-700" : undefined} />
              <OperationRow icon="fas fa-layer-group" label="Tầng hoạt động" value={`${activeLevels} / ${data.levels.length}`} />
              <OperationRow icon="fas fa-door-open" label="Cổng hoạt động" value={`${activeGates} / ${data.gates.length}`} />
              <OperationRow icon="fas fa-road" label="Làn hoạt động" value={`${activeLanes} / ${data.lanes.length}`} />
            </div>
            {maintenanceGate ? (
              <div className="tw-mt-3 tw-flex tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-px-4 tw-py-3">
                <i className="fas fa-exclamation-triangle tw-mt-0.5 tw-text-lg tw-text-amber-500" />
                <div><strong className="tw-block tw-text-[0.8rem] tw-font-extrabold tw-text-amber-700">{maintenanceGate.name} đang bảo trì</strong><span className="tw-mt-1 tw-block tw-text-[0.7rem] tw-font-medium tw-text-vm-slate-500">Tạm ngưng hoạt động cho đến khi được kích hoạt lại.</span></div>
              </div>
            ) : null}
          </Card>
        </div>

        <div className="tw-mt-3 tw-grid tw-grid-cols-[minmax(0,1.72fr)_minmax(340px,1fr)] tw-gap-3 max-[1050px]:tw-grid-cols-1">
          <Card className="tw-min-h-[258px] tw-px-5 tw-py-4 tw-shadow-none">
            <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin bãi xe</h2>
            <div className="tw-mt-3 tw-grid tw-grid-cols-[300px_minmax(0,1fr)] tw-gap-5 max-[720px]:tw-grid-cols-1">
              <dl className="tw-m-0 tw-grid tw-content-start tw-grid-cols-[128px_1fr] tw-gap-y-3 tw-text-[0.78rem]">
                <dt className="tw-font-medium tw-text-vm-slate-500">Mã bãi</dt><dd className="tw-m-0 tw-font-extrabold tw-text-vm-slate-900">{selectedLot.code}</dd>
                <dt className="tw-font-medium tw-text-vm-slate-500">Đơn vị quản lý</dt><dd className="tw-m-0 tw-truncate tw-font-extrabold tw-text-vm-slate-900">{data.organization?.name ?? "Chưa xác định"}</dd>
                <dt className="tw-font-medium tw-text-vm-slate-500">Giờ hoạt động</dt><dd className="tw-m-0 tw-font-extrabold tw-text-vm-slate-900">24/7</dd>
                <dt className="tw-font-medium tw-text-vm-slate-500">Trạng thái vị trí</dt><dd className="tw-m-0"><span className="tw-inline-flex tw-items-center tw-gap-1.5 tw-rounded-full tw-bg-emerald-50 tw-px-2.5 tw-py-1 tw-text-[0.7rem] tw-font-bold tw-text-emerald-700"><i className="fas fa-check-circle" />{selectedLot.latitude !== null && selectedLot.longitude !== null ? "Đã xác nhận" : "Chưa xác nhận"}</span></dd>
                <dt className="tw-font-medium tw-text-vm-slate-500">Tọa độ</dt><dd className="tw-m-0 tw-font-medium tw-text-vm-slate-700">{selectedLot.latitude !== null && selectedLot.longitude !== null ? `${Number(selectedLot.latitude).toFixed(4)}, ${Number(selectedLot.longitude).toFixed(4)}` : "—"}</dd>
              </dl>
              {selectedLot.latitude !== null && selectedLot.longitude !== null ? (
                <ParkingLocationMap className="!tw-h-[194px]" interactive={false} latitude={Number(selectedLot.latitude)} longitude={Number(selectedLot.longitude)} />
              ) : (
                <div className="tw-flex tw-h-[194px] tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-dashed tw-border-vm-slate-200 tw-bg-vm-slate-25 tw-text-center tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500"><span><i className="fas fa-map-marked-alt tw-mb-2 tw-block tw-text-2xl tw-text-vm-primary" />Chưa có tọa độ để hiển thị bản đồ</span></div>
              )}
            </div>
          </Card>

          <Card className="tw-min-h-[258px] tw-px-4 tw-py-4 tw-shadow-none">
            <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Người phụ trách</h2>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.68rem] tw-font-medium tw-text-vm-slate-500">Manager đang hoạt động và có thể được phân công</p>
            <div className="tw-mt-2">
              {data.managers.slice(0, 2).map((manager, index) => (
                <div key={manager.account.accountId} className="tw-flex tw-h-[62px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 last:tw-border-b-0">
                  <span className={cn("tw-flex tw-size-11 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-text-sm tw-font-extrabold tw-text-white", index % 2 === 0 ? "tw-bg-gradient-to-br tw-from-slate-500 tw-to-blue-600" : "tw-bg-gradient-to-br tw-from-purple-500 tw-to-pink-500")}>{manager.account.username.slice(0, 2).toUpperCase()}</span>
                  <div className="tw-min-w-0 tw-flex-1"><strong className="tw-block tw-truncate tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-900">{manager.account.username}</strong><span className="tw-mt-1 tw-block tw-truncate tw-text-[0.7rem] tw-font-medium tw-text-vm-slate-500">Parking Manager</span></div>
                  <i className="fas fa-ellipsis-v tw-text-vm-slate-500" />
                </div>
              ))}
              {data.managers.length === 0 ? <div className="tw-flex tw-h-[124px] tw-items-center tw-justify-center tw-text-center tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">Không có dữ liệu Manager khả dụng.</div> : null}
            </div>
            {canManageAssignments ? <Button variant="secondary" className="tw-mt-2 tw-w-full" onClick={() => navigate(`/admin/parking-structure?lot=${encodeURIComponent(selectedLot.parkingLotId)}&action=manager`)}><i className="fas fa-users-cog" />Quản lý phân công</Button> : null}
          </Card>
        </div>

        <Card className="tw-mt-3 tw-px-5 tw-py-3 tw-shadow-none">
          <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Đi nhanh</h2>
          <div className="tw-mt-2 tw-grid tw-grid-cols-3 tw-gap-4 max-[820px]:tw-grid-cols-1">
            <QuickLink icon="fas fa-layer-group" label="Cấu trúc" description="Quản lý tầng, khu, cổng và làn" to={parkingWorkspaceHref("structure", selectedLot.parkingLotId)} />
            <QuickLink icon="far fa-map" label="Thiết kế sơ đồ" description="Chỉnh bố cục và xuất bản" to={parkingWorkspaceHref("layout", selectedLot.parkingLotId)} />
            <QuickLink icon="fas fa-desktop" label="Sơ đồ trực tiếp" description="Theo dõi trạng thái từng ô" to={parkingWorkspaceHref("live", selectedLot.parkingLotId)} />
          </div>
        </Card>
      </section>
    </main>
  );
}
