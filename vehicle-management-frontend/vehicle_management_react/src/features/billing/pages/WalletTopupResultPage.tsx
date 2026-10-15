import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getTopupOrder, payInvoiceByWallet } from "@/features/billing/api/walletApi";
import { CustomerPortalLayout } from "@/features/customer-portal/pages/PortalShared";

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
    <CustomerPortalLayout>
      <main className="vm-wallet-topup-result tw-mx-auto tw-grid tw-w-full tw-max-w-xl tw-justify-items-center tw-gap-4 tw-rounded-2xl tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-8 tw-text-center tw-shadow-[0_14px_36px_rgba(15,55,100,0.08)]">
        <span className="tw-grid tw-h-16 tw-w-16 tw-place-items-center tw-rounded-full tw-bg-blue-50 tw-text-2xl tw-text-vm-primary" aria-hidden="true"><i className="fas fa-wallet" /></span>
        <div>
          <span className="tw-text-xs tw-font-black tw-uppercase tw-tracking-[0.08em] tw-text-vm-primary">Ví CoParking</span>
          <h1 className="tw-m-0 tw-mt-2 tw-text-2xl tw-font-bold tw-text-slate-900">Kết quả nạp tiền</h1>
        </div>
        <p className="tw-m-0 tw-max-w-md tw-text-sm tw-font-semibold tw-leading-6 tw-text-slate-600" role="status" aria-live="polite">{status}</p>
        <div className="vm-wallet-topup-result-actions tw-flex tw-w-full tw-justify-center tw-gap-3">
          <Link to="/customer/wallet" className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-rounded-lg tw-border tw-border-solid tw-border-blue-200 tw-bg-white tw-px-5 tw-font-bold tw-text-blue-700 hover:tw-no-underline">Về ví</Link>
          <Link to="/customer/wallet/topup" className="tw-inline-flex tw-min-h-11 tw-items-center tw-justify-center tw-rounded-lg tw-bg-vm-primary tw-px-5 tw-font-bold tw-text-white hover:tw-text-white hover:tw-no-underline">Nạp tiếp</Link>
        </div>
      </main>
    </CustomerPortalLayout>
  );
}
