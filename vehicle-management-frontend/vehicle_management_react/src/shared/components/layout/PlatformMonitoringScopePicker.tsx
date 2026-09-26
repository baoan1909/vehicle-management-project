import { useLocation, useNavigate } from "react-router-dom";

import { platformAdminNavigation } from "@/config/navigation";
import { usePlatformMonitoringScope } from "@/shared/monitoring/PlatformMonitoringScope";

const selectClassName = "tw-h-9 tw-w-full tw-min-w-0 tw-rounded-vm-sm tw-border tw-border-solid tw-border-brand-100 tw-bg-white tw-px-2 tw-text-xs tw-font-semibold tw-text-slate-900 focus:tw-outline-brand-300";

export function PlatformMonitoringScopePicker() {
  const location = useLocation();
  const navigate = useNavigate();
  const { scope, setScope, organizations, parkingLots, loading, error } = usePlatformMonitoringScope();
  const organizationId = scope.level === "ALL" ? "" : scope.organizationId;
  const partnerLots = parkingLots.filter((parkingLot) => parkingLot.organizationId === organizationId);
  const updateScope = (nextScope: typeof scope) => {
    setScope(nextScope);
    const isMonitoringPage = platformAdminNavigation.some((entry) => entry.kind === "link"
      && entry.monitoring
      && entry.matches.some((match) => location.pathname === match || location.pathname.startsWith(`${match}/`)));
    if (isMonitoringPage && nextScope.level !== "ALL"
      && !["/admin/parking-lots", "/admin/swipe/sessions", "/admin/card"].includes(location.pathname)) {
      navigate("/admin/parking-lots");
    }
  };

  return (
    <section className="tw-mb-5 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-p-3" aria-label="Phạm vi giám sát dữ liệu">
      <p className="tw-m-0 tw-text-[0.7rem] tw-font-black tw-uppercase tw-tracking-[0.1em] tw-text-vm-primary">Phạm vi giám sát</p>
      {error ? (
        <p className="tw-m-0 tw-mt-2 tw-text-xs tw-text-red-600">Chưa tải được đối tác và bãi xe.</p>
      ) : (
        <div className="tw-mt-2 tw-grid tw-gap-2">
          <select
            aria-label="Cấp độ giám sát"
            className={selectClassName}
            disabled={loading}
            value={scope.level}
            onChange={(event) => {
              const level = event.target.value;
              if (level === "ALL") updateScope({ level: "ALL" });
              else if (level === "PARTNER" && organizations[0]) updateScope({ level: "PARTNER", organizationId: organizations[0].organizationId });
              else if (level === "PARKING_LOT" && parkingLots[0]) {
                const firstLot = parkingLots[0];
                updateScope({ level: "PARKING_LOT", organizationId: firstLot.organizationId, parkingLotId: firstLot.parkingLotId });
              }
            }}
          >
            <option value="ALL">Toàn sàn</option>
            <option value="PARTNER" disabled={organizations.length === 0}>Theo đối tác</option>
            <option value="PARKING_LOT" disabled={parkingLots.length === 0}>Theo bãi xe</option>
          </select>
          {scope.level !== "ALL" ? (
            <select
              aria-label="Chọn đối tác giám sát"
              className={selectClassName}
              value={organizationId}
              onChange={(event) => {
                const nextOrganizationId = event.target.value;
                if (scope.level === "PARTNER") updateScope({ level: "PARTNER", organizationId: nextOrganizationId });
                else {
                  const firstLot = parkingLots.find((parkingLot) => parkingLot.organizationId === nextOrganizationId);
                  if (firstLot) updateScope({ level: "PARKING_LOT", organizationId: nextOrganizationId, parkingLotId: firstLot.parkingLotId });
                }
              }}
            >
              {organizations.filter((organization) => scope.level === "PARTNER" || parkingLots.some((lot) => lot.organizationId === organization.organizationId)).map((organization) => (
                <option key={organization.organizationId} value={organization.organizationId}>{organization.name}</option>
              ))}
            </select>
          ) : null}
          {scope.level === "PARKING_LOT" ? (
            <select
              aria-label="Chọn bãi xe giám sát"
              className={selectClassName}
              value={scope.parkingLotId}
              onChange={(event) => updateScope({ level: "PARKING_LOT", organizationId, parkingLotId: event.target.value })}
            >
              {partnerLots.map((lot) => <option key={lot.parkingLotId} value={lot.parkingLotId}>{lot.name}</option>)}
            </select>
          ) : null}
        </div>
      )}
      <p className="tw-m-0 tw-mt-2 tw-text-[0.7rem] tw-font-medium tw-leading-4 tw-text-vm-slate-500">
        Chỉ áp dụng cho Giám sát dữ liệu. Quản trị nền tảng luôn ở phạm vi toàn sàn.
      </p>
    </section>
  );
}
