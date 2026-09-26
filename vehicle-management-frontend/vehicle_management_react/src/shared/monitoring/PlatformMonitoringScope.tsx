import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from "react";

import { useAuth } from "@/core/auth/useAuth";
import { getOrganizations, type OrganizationAdminResponse } from "@/features/iam/api/organizationsApi";
import { getParkingLots, type ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { hasAnyPermission } from "@/shared/auth/permissions";

export type MonitoringScope =
  | { level: "ALL" }
  | { level: "PARTNER"; organizationId: string }
  | { level: "PARKING_LOT"; organizationId: string; parkingLotId: string };

type PlatformMonitoringScopeValue = {
  scope: MonitoringScope;
  setScope: (scope: MonitoringScope) => void;
  organizations: OrganizationAdminResponse[];
  parkingLots: ParkingLotApiResponse[];
  loading: boolean;
  error: boolean;
  permittedLotIds: Set<string> | null;
};

const PlatformMonitoringScopeContext = createContext<PlatformMonitoringScopeValue | null>(null);

export function PlatformMonitoringScopeProvider({ children }: PropsWithChildren) {
  const { user } = useAuth();
  const canSelectScope = hasAnyPermission(user, ["ORGANIZATION_CREATE_ALL"])
    && hasAnyPermission(user, ["ORGANIZATION_READ_ALL"])
    && hasAnyPermission(user, ["PARKING_LOT_READ_ALL"]);
  const [scope, setScope] = useState<MonitoringScope>({ level: "ALL" });
  const [organizations, setOrganizations] = useState<OrganizationAdminResponse[]>([]);
  const [parkingLots, setParkingLots] = useState<ParkingLotApiResponse[]>([]);
  const [loading, setLoading] = useState(canSelectScope);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!canSelectScope) {
      setScope({ level: "ALL" });
      setOrganizations([]);
      setParkingLots([]);
      setLoading(false);
      setError(false);
      return;
    }
    let active = true;
    setLoading(true);
    setError(false);

    Promise.all([getOrganizations(), getParkingLots()])
      .then(([organizationResponse, parkingLotResponse]) => {
        if (!active) return;
        setOrganizations(organizationResponse.data ?? []);
        setParkingLots(parkingLotResponse.data ?? []);
      })
      .catch(() => {
        if (active) setError(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [canSelectScope]);

  const permittedLotIds = useMemo(() => {
    if (scope.level === "ALL") return null;
    if (scope.level === "PARKING_LOT") return new Set([scope.parkingLotId]);
    return new Set(parkingLots
      .filter((parkingLot) => parkingLot.organizationId === scope.organizationId)
      .map((parkingLot) => parkingLot.parkingLotId));
  }, [parkingLots, scope]);

  const value = useMemo(() => ({
    scope: canSelectScope ? scope : { level: "ALL" } as const,
    setScope,
    organizations,
    parkingLots,
    loading,
    error,
    permittedLotIds: canSelectScope ? permittedLotIds : null,
  }), [canSelectScope, scope, organizations, parkingLots, loading, error, permittedLotIds]);

  return <PlatformMonitoringScopeContext.Provider value={value}>{children}</PlatformMonitoringScopeContext.Provider>;
}

export function usePlatformMonitoringScope() {
  const context = useContext(PlatformMonitoringScopeContext);
  if (!context) throw new Error("usePlatformMonitoringScope must be used inside PlatformMonitoringScopeProvider");
  return context;
}
