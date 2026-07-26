import { useEffect, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import {
  clearVnpayReturnTarget,
  readVnpayReturnTarget,
} from "@/features/billing/utils/vnpayReturnTarget";

export function VnpayReturnPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const target = useMemo(
    () => readVnpayReturnTarget() || "/customer/subscriptions",
    [],
  );

  useEffect(() => {
    const separator = target.includes("?") ? "&" : "?";
    const callbackQuery = location.search.replace(/^\?/, "");
    clearVnpayReturnTarget();
    navigate(
      callbackQuery ? `${target}${separator}${callbackQuery}` : target,
      { replace: true },
    );
  }, [location.search, navigate, target]);

  return (
    <main className="tw-grid tw-min-h-[60vh] tw-place-items-center tw-p-6">
      <div className="tw-text-center">
        <i className="fas fa-spinner fa-spin tw-text-2xl tw-text-vm-primary" />
        <p className="tw-mb-0 tw-mt-3 tw-font-bold tw-text-vm-slate-600">
          Đang xác nhận kết quả thanh toán...
        </p>
      </div>
    </main>
  );
}
