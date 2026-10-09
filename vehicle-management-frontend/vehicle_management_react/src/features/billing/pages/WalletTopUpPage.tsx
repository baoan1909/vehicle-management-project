import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { QUICK_TOPUP_AMOUNTS, createWalletTopup, getMyWallet } from "@/features/billing/api/walletApi";
import { useEffect } from "react";

export function WalletTopUpPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [amount, setAmount] = useState<number>(100000);
  const [customAmount, setCustomAmount] = useState<string>("");
  const [balance, setBalance] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const invoiceId = searchParams.get("invoiceId");

  useEffect(() => {
    getMyWallet().then((res) => setBalance(res.data?.availableBalance ?? null)).catch(() => undefined);
  }, []);

  async function handleTopup() {
    setError(null);
    const finalAmount = customAmount ? Number(customAmount) : amount;
    if (!Number.isFinite(finalAmount) || finalAmount < 10000) {
      setError("Số tiền nạp tối thiểu là 10.000 ₫");
      return;
    }
    setLoading(true);
    try {
      const res = await createWalletTopup(Math.round(finalAmount));
      const paymentUrl = res.data?.paymentUrl;
      if (paymentUrl) {
        sessionStorage.setItem("wallet-topup-pending-order", res.data.topupOrderId);
        if (invoiceId) {
          sessionStorage.setItem("wallet-topup-pending-invoice", invoiceId);
        } else {
          sessionStorage.removeItem("wallet-topup-pending-invoice");
        }
        window.location.href = paymentUrl;
      } else {
        setError("Không tạo được đơn nạp tiền");
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Nạp tiền thất bại");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="tw-p-6 tw-max-w-xl tw-mx-auto tw-space-y-4">
      <h1 className="tw-text-xl tw-font-bold">Nạp tiền vào ví</h1>
      {balance !== null && <p>Số dư hiện tại: <strong>{balance.toLocaleString("vi-VN")} ₫</strong></p>}
      {invoiceId && <p className="tw-text-sm">Nạp tiền để thanh toán hóa đơn {invoiceId}. Sau khi nạp xong bạn sẽ được đưa về tiếp tục thanh toán.</p>}
      <div className="tw-grid tw-grid-cols-3 tw-gap-2">
        {QUICK_TOPUP_AMOUNTS.map((value) => (
          <button
            key={value}
            onClick={() => { setAmount(value); setCustomAmount(""); }}
            className={`tw-rounded tw-border tw-p-3 ${amount === value && !customAmount ? "tw-border-vm-primary tw-font-bold" : ""}`}
          >
            {value.toLocaleString("vi-VN")} ₫
          </button>
        ))}
      </div>
      <input
        value={customAmount}
        onChange={(e) => setCustomAmount(e.target.value.replace(/[^0-9]/g, ""))}
        placeholder="Hoặc nhập số tiền tùy chọn (VND)"
        inputMode="numeric"
        className="tw-w-full tw-rounded tw-border tw-p-3"
      />
      {error && <p className="tw-text-red-600">{error}</p>}
      <button
        onClick={handleTopup}
        disabled={loading}
        className="tw-w-full tw-rounded tw-bg-vm-primary tw-p-3 tw-text-white disabled:tw-opacity-50"
      >
        {loading ? "Đang tạo đơn..." : "Tiếp tục sang VNPAY"}
      </button>
      <button onClick={() => navigate(-1)} className="tw-w-full tw-p-2 tw-text-sm">Quay lại</button>
    </main>
  );
}
