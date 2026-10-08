import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyWallet, getMyWalletTransactions, type WalletResponse } from "@/features/billing/api/walletApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { useAuth } from "@/core/auth/useAuth";

export function CustomerWalletPage() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [transactions, setTransactions] = useState<{ financialTransactionId: string; transactionCode: string; transactionType: string; status: string }[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getMyWallet()
      .then((res) => setWallet(res.data))
      .catch((err) => setError(err?.message ?? "Không tải được ví"));
    getMyWalletTransactions(0, 20)
      .then((res) => setTransactions(res.data ?? []))
      .catch(() => undefined);
  }, []);

  if (!hasAnyPermission(user, ["WALLET_READ_OWN"])) {
    return <main className="tw-p-6">Bạn không có quyền xem ví.</main>;
  }

  return (
    <main className="tw-p-6 tw-space-y-6">
      <div className="tw-flex tw-items-center tw-justify-between">
        <h1 className="tw-text-xl tw-font-bold">Ví của tôi</h1>
        <Link to="/customer/wallet/topup" className="tw-rounded tw-bg-vm-primary tw-px-4 tw-py-2 tw-text-white">
          Nạp tiền
        </Link>
      </div>
      {error && <p className="tw-text-red-600">{error}</p>}
      {wallet && (
        <div className="tw-grid tw-grid-cols-1 md:tw-grid-cols-3 tw-gap-4">
          <div className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">Số dư khả dụng</p>
            <p className="tw-text-2xl tw-font-bold">{wallet.availableBalance?.toLocaleString("vi-VN")} ₫</p>
          </div>
          <div className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">Trạng thái</p>
            <p className="tw-font-bold">{wallet.status}</p>
          </div>
          <div className="tw-rounded tw-border tw-p-4">
            <p className="tw-text-sm">Tiền tệ</p>
            <p className="tw-font-bold">{wallet.currency}</p>
          </div>
        </div>
      )}
      <section>
        <h2 className="tw-font-bold tw-mb-2">Lịch sử giao dịch</h2>
        <ul className="tw-space-y-2">
          {transactions.map((tx) => (
            <li key={tx.financialTransactionId} className="tw-rounded tw-border tw-p-3 tw-flex tw-justify-between">
              <span>{tx.transactionCode} · {tx.transactionType}</span>
              <span>{tx.status}</span>
            </li>
          ))}
          {transactions.length === 0 && <li>Chưa có giao dịch.</li>}
        </ul>
      </section>
    </main>
  );
}
