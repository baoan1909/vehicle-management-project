import { SelectMenu } from "@/shared/components/ui/SelectMenu";
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
      && !["/admin/parking-lots", "/admin/parking-structure", "/admin/parking-layouts", "/admin/parking-live", "/admin/swipe/sessions", "/admin/card"].includes(location.pathname)) {
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
          <SelectMenu ariaLabel={"Cấp độ giám sát"}
                  triggerClassName={selectClassName}
                  disabled={loading}
                  value={scope.level}
                  onChange={(selectedValue) => {
              const level = selectedValue;
              if (level === "ALL") updateScope({ level: "ALL" });
              else if (level === "PARTNER" && organizations[0]) updateScope({ level: "PARTNER", organizationId: organizations[0].organizationId });
              else if (level === "PARKING_LOT" && parkingLots[0]) {
                const firstLot = parkingLots[0];
                updateScope({ level: "PARKING_LOT", organizationId: firstLot.organizationId, parkingLotId: firstLot.parkingLotId });
              }
            }}
                  options={[{ label: "Toàn sàn", value: "ALL" }, { label: "Theo đối tác", value: "PARTNER", disabled: organizations.length === 0 }, { label: "Theo bãi xe", value: "PARKING_LOT", disabled: parkingLots.length === 0 }]} portal clearValue="" />
          {scope.level !== "ALL" ? (
            <SelectMenu ariaLabel={"Chọn đối tác giám sát"}
                  triggerClassName={selectClassName}
                  value={organizationId}
                  onChange={(selectedValue) => {
                const nextOrganizationId = selectedValue;
                if (scope.level === "PARTNER") updateScope({ level: "PARTNER", organizationId: nextOrganizationId });
                else {
                  const firstLot = parkingLots.find((parkingLot) => parkingLot.organizationId === nextOrganizationId);
                  if (firstLot) updateScope({ level: "PARKING_LOT", organizationId: nextOrganizationId, parkingLotId: firstLot.parkingLotId });
                }
              }}
                  options={[...organizations.filter((organization) => scope.level === "PARTNER" || parkingLots.some((lot) => lot.organizationId === organization.organizationId)).map((organization) => ({ label: organization.name, value: organization.organizationId }))]} portal clearValue="" />
          ) : null}
          {scope.level === "PARKING_LOT" ? (
            <SelectMenu ariaLabel={"Chọn bãi xe giám sát"}
                  triggerClassName={selectClassName}
                  value={scope.parkingLotId}
                  onChange={(selectedValue) => updateScope({ level: "PARKING_LOT", organizationId, parkingLotId: selectedValue })}
                  options={[...partnerLots.map((lot) => ({ label: lot.name, value: lot.parkingLotId }))]} portal clearValue="" />
          ) : null}
        </div>
      )}
      <p className="tw-m-0 tw-mt-2 tw-text-[0.7rem] tw-font-medium tw-leading-4 tw-text-vm-slate-500">
        Chỉ áp dụng cho Giám sát dữ liệu. Quản trị nền tảng luôn ở phạm vi toàn sàn.
      </p>
    </section>
  );
}
