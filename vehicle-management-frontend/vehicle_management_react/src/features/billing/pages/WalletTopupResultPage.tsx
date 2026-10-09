import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getTopupOrder, payInvoiceByWallet } from "@/features/billing/api/walletApi";

const POLL_INTERVAL_MS = 2_000;
const MAX_POLL_ATTEMPTS = 30;

export function WalletTopupResultPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<string>("Đang kiểm tra...");
  const paymentStartedRef = useRef(false);

  useEffect(() => {
    const pendingTopupOrderId = sessionStorage.getItem("wallet-topup-pending-order");
    if (!pendingTopupOrderId) {
      setStatus("Không tìm thấy đơn nạp tiền để đối chiếu. Vui lòng kiểm tra lịch sử ví.");
      return;
    }
    const topupOrderId: string = pendingTopupOrderId;

    let cancelled = false;
    let timer: number | undefined;
    let attempts = 0;

    async function pollOrder() {
      try {
        const response = await getTopupOrder(topupOrderId);
        if (cancelled) return;
        const order = response.data;

        if (order.status === "COMPLETED") {
          sessionStorage.removeItem("wallet-topup-pending-order");
          const pendingInvoice = sessionStorage.getItem("wallet-topup-pending-invoice");
          if (!pendingInvoice) {
            setStatus(`Đã nạp ${order.amount.toLocaleString("vi-VN")} ₫ vào ví thành công.`);
            return;
          }

          if (paymentStartedRef.current) return;
          paymentStartedRef.current = true;
          setStatus("Nạp tiền thành công. Đang tiếp tục thanh toán hóa đơn bằng ví...");
          const keyName = `wallet-payment-idempotency:${pendingInvoice}`;
          const paymentKey = sessionStorage.getItem(keyName) ?? crypto.randomUUID();
          sessionStorage.setItem(keyName, paymentKey);
          try {
            await payInvoiceByWallet(pendingInvoice, paymentKey);
            sessionStorage.removeItem("wallet-topup-pending-invoice");
            sessionStorage.removeItem(keyName);
            navigate("/customer/subscriptions?walletPayment=success", { replace: true });
          } catch (error: unknown) {
            paymentStartedRef.current = false;
            setStatus(error instanceof Error
              ? `Ví đã được nạp nhưng chưa thanh toán được hóa đơn: ${error.message}`
              : "Ví đã được nạp nhưng chưa thanh toán được hóa đơn. Vui lòng thử lại từ trang vé tháng.");
          }
          return;
        }

        if (["FAILED", "EXPIRED", "CANCELLED"].includes(order.status)) {
          sessionStorage.removeItem("wallet-topup-pending-order");
          setStatus(`Đơn nạp tiền ở trạng thái ${order.status}. Số dư ví không thay đổi.`);
          return;
        }

        attempts += 1;
        if (attempts >= MAX_POLL_ATTEMPTS) {
          setStatus("VNPAY chưa xác nhận giao dịch. Bạn có thể kiểm tra lại trong lịch sử ví.");
          return;
        }
        setStatus("Đang chờ VNPAY xác nhận giao dịch qua IPN...");
        timer = window.setTimeout(pollOrder, POLL_INTERVAL_MS);
      } catch (error: unknown) {
        if (cancelled) return;
        attempts += 1;
        if (attempts >= MAX_POLL_ATTEMPTS) {
          setStatus(error instanceof Error ? error.message : "Không thể kiểm tra trạng thái nạp tiền.");
          return;
        }
        timer = window.setTimeout(pollOrder, POLL_INTERVAL_MS);
      }
    }

    void pollOrder();
    return () => {
      cancelled = true;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [navigate]);

  return (
    <main className="tw-p-6 tw-max-w-xl tw-mx-auto tw-text-center tw-space-y-4">
      <h1 className="tw-text-xl tw-font-bold">Kết quả nạp tiền</h1>
      <p>{status}</p>
      <div className="tw-flex tw-gap-2 tw-justify-center">
        <Link to="/customer/wallet" className="tw-rounded tw-border tw-px-4 tw-py-2">Về ví</Link>
        <Link to="/customer/wallet/topup" className="tw-rounded tw-bg-vm-primary tw-px-4 tw-py-2 tw-text-white">Nạp tiếp</Link>
      </div>
    </main>
  );
}
