import { createContext, type ReactNode, useContext, useEffect, useMemo, useState } from "react";

import { getPlatformFeeConfiguration } from "@/features/billing/api/platformFeeApi";

type PlatformFeeConfigurationValue = {
  loading: boolean;
  maxPartnerTotalRate: number | null;
};

const PlatformFeeConfigurationContext = createContext<PlatformFeeConfigurationValue>({
  loading: true,
  maxPartnerTotalRate: null,
});

export function PlatformFeeConfigurationProvider({ children }: { children: ReactNode }) {
  const [maxPartnerTotalRate, setMaxPartnerTotalRate] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    getPlatformFeeConfiguration()
      .then((response) => {
        if (!cancelled) setMaxPartnerTotalRate(response.data.maxPartnerTotalRate);
      })
      .catch(() => {
        if (!cancelled) setMaxPartnerTotalRate(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo(() => ({ loading, maxPartnerTotalRate }), [loading, maxPartnerTotalRate]);
  return <PlatformFeeConfigurationContext.Provider value={value}>{children}</PlatformFeeConfigurationContext.Provider>;
}

export function usePlatformFeeConfiguration() {
  return useContext(PlatformFeeConfigurationContext);
}
