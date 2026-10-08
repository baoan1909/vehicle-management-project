import { useEffect, useState } from "react";
import { getPartnerAllocations, getPartnerWallet, type WalletResponse } from "@/features/billing/api/walletApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { useAuth } from "@/core/auth/useAuth";

export function PartnerWalletPage() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [allocations, setAllocations] = useState<unknown[]>([]);

  useEffect(() => {
    getPartnerWallet().then((res) => setWallet(res.data)).catch(() => undefined);
    getPartnerAllocations(0, 20).then((res) => setAllocations(res.data ?? [])).catch(() => undefined);
  }, []);

  if (!hasAnyPermission(user, ["WALLET_READ_PARTNER"])) {
    return <main className="tw-p-6">Bạn không có quyền xem ví đối tác.</main>;
  }

  return (
    <main className="tw-p-6 tw-space-y-6">
      <h1 className="tw-text-xl tw-font-bold">Ví đối tác</h1>
      {wallet && (
        <div className="tw-grid tw-grid-cols-1 md:tw-grid-cols-3 tw-gap-4">
          <div className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">Chờ settlement (pending)</p>
            <p className="tw-text-2xl tw-font-bold">{wallet.pendingBalance?.toLocaleString("vi-VN")} ₫</p>
          </div>
          <div className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">Khả dụng (available)</p>
            <p className="tw-text-2xl tw-font-bold">{wallet.availableBalance?.toLocaleString("vi-VN")} ₫</p>
          </div>
          <div className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">Đang giữ (held)</p>
            <p className="tw-text-2xl tw-font-bold">{wallet.heldBalance?.toLocaleString("vi-VN")} ₫</p>
          </div>
        </div>
      )}
      <section>
        <h2 className="tw-font-bold tw-mb-2">Doanh thu theo phân bổ</h2>
        <pre className="tw-rounded tw-border tw-p-3 tw-text-xs tw-overflow-auto">{JSON.stringify(allocations, null, 2)}</pre>
      </section>
    </main>
  );
}
