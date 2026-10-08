import { useEffect, useState } from "react";
import { getFinancialOverview } from "@/features/billing/api/walletApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { useAuth } from "@/core/auth/useAuth";

export function WalletManagementPage() {
  const { user } = useAuth();
  const [overview, setOverview] = useState<Record<string, number>>({});

  useEffect(() => {
    getFinancialOverview().then((res) => setOverview(res.data ?? {})).catch(() => undefined);
  }, []);

  if (!hasAnyPermission(user, ["WALLET_READ_ALL"])) {
    return <main className="tw-p-6">Bạn không có quyền quản trị ví.</main>;
  }

  const imbalance = overview["LEDGER_IMBALANCE"] ?? 0;

  return (
    <main className="tw-p-6 tw-space-y-6">
      <h1 className="tw-text-xl tw-font-bold">Quản trị ví & đối soát</h1>
      {imbalance !== 0 && (
        <p className="tw-rounded tw-bg-red-100 tw-p-3 tw-text-red-700">
          Cảnh báo nghiêm trọng: ledger imbalance = {imbalance}
        </p>
      )}
      <div className="tw-grid tw-grid-cols-1 md:tw-grid-cols-3 tw-gap-4">
        {Object.entries(overview).map(([key, value]) => (
          <div key={key} className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">{key}</p>
            <p className="tw-text-xl tw-font-bold">{Number(value)?.toLocaleString("vi-VN")}</p>
          </div>
        ))}
      </div>
    </main>
  );
}
