import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { getTopupOrder } from "@/features/billing/api/walletApi";

export function WalletTopupResultPage() {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState<string>("Đang kiểm tra...");
  const transactionRef = searchParams.get("transactionRef") ?? searchParams.get("vnp_TxnRef");

  useEffect(() => {
    const pendingInvoice = sessionStorage.getItem("wallet-topup-pending-invoice");
    if (pendingInvoice) {
      sessionStorage.removeItem("wallet-topup-pending-invoice");
    }
    const responseCode = searchParams.get("vnp_ResponseCode");
    if (responseCode === "00") {
      setStatus("Nạp tiền thành công. Số dư sẽ được cập nhật sau khi IPN xác nhận.");
    } else if (responseCode) {
      setStatus(`Nạp tiền chưa thành công (mã ${responseCode}). Vui lòng thử lại.`);
    } else {
      setStatus("Đang chờ xác nhận từ VNPAY. Vui lòng tải lại sau ít phút.");
    }
    void getTopupOrder;
    void transactionRef;
  }, [searchParams, transactionRef]);

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
