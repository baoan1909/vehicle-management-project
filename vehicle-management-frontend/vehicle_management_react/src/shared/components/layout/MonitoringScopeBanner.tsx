import { useLocation } from "react-router-dom";

import { usePlatformMonitoringScope } from "@/shared/monitoring/PlatformMonitoringScope";

const scopedPages = new Set(["/admin/parking-lots", "/admin/swipe/sessions", "/admin/card"]);

export function MonitoringScopeBanner() {
  const { pathname } = useLocation();
  const { scope, organizations, parkingLots, loading } = usePlatformMonitoringScope();
  if (scope.level === "ALL" || !scopedPages.has(pathname)) return null;

  const organizationName = organizations.find((organization) => organization.organizationId === scope.organizationId)?.name;
  const parkingLotName = scope.level === "PARKING_LOT"
    ? parkingLots.find((parkingLot) => parkingLot.parkingLotId === scope.parkingLotId)?.name
    : null;
  const scopeName = scope.level === "PARTNER" ? organizationName : parkingLotName;

  return (
    <div className="tw-mx-4 tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-px-4 tw-py-2 tw-text-sm tw-font-semibold tw-text-vm-primary" role="status">
      <i className="fas fa-eye tw-mr-2" aria-hidden="true" />
      {loading ? "Đang xác định phạm vi giám sát..." : `Đang xem dữ liệu ${scope.level === "PARTNER" ? "đối tác" : "bãi xe"}: ${scopeName ?? "không xác định"} · Chỉ xem`}
    </div>
  );
}
