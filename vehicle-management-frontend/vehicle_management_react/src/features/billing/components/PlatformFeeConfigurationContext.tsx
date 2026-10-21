import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useState } from "react";

import {
  getPlatformFeeConfiguration,
  updatePlatformFeeConfiguration,
  type PlatformFeeConfigurationResponse,
} from "@/features/billing/api/platformFeeApi";

type PlatformFeeConfigurationValue = {
  configuration: PlatformFeeConfigurationResponse | null;
  loading: boolean;
  maxPartnerTotalRate: number | null;
  updateConfiguration: (maxPartnerTotalRate: number) => Promise<PlatformFeeConfigurationResponse>;
};

const PlatformFeeConfigurationContext = createContext<PlatformFeeConfigurationValue>({
  configuration: null,
  loading: true,
  maxPartnerTotalRate: null,
  updateConfiguration: async () => {
    throw new Error("Platform fee configuration provider is unavailable");
  },
});

export function PlatformFeeConfigurationProvider({ children }: { children: ReactNode }) {
  const [configuration, setConfiguration] = useState<PlatformFeeConfigurationResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getPlatformFeeConfiguration()
      .then((response) => {
        if (!cancelled) setConfiguration(response.data);
      })
      .catch(() => {
        if (!cancelled) setConfiguration(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const updateConfiguration = useCallback(async (maxPartnerTotalRate: number) => {
    if (!configuration) {
      throw new Error("Chưa tải được cấu hình phí sàn. Vui lòng tải lại trang.");
    }
    const response = await updatePlatformFeeConfiguration({
      maxPartnerTotalRate,
      version: configuration.version,
    });
    setConfiguration(response.data);
    return response.data;
  }, [configuration]);

  const value = useMemo(() => ({
    configuration,
    loading,
    maxPartnerTotalRate: configuration?.maxPartnerTotalRate ?? null,
    updateConfiguration,
  }), [configuration, loading, updateConfiguration]);
  return <PlatformFeeConfigurationContext.Provider value={value}>{children}</PlatformFeeConfigurationContext.Provider>;
}

export function usePlatformFeeConfiguration() {
  return useContext(PlatformFeeConfigurationContext);
}
