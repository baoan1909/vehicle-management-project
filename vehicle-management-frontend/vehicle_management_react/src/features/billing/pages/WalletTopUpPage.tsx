import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { QUICK_TOPUP_AMOUNTS, createWalletTopup, getMyWallet } from "@/features/billing/api/walletApi";
import { CustomerPortalLayout } from "@/features/customer-portal/pages/PortalShared";

const MINIMUM_TOPUP_AMOUNT = 10_000;

function formatCurrency(value: number | null) {
  if (value == null) return "—";
  return `${new Intl.NumberFormat("vi-VN").format(value)} ₫`;
}

export function WalletTopUpPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [amount, setAmount] = useState<number>(100_000);
  const [customAmount, setCustomAmount] = useState("");
  const [balance, setBalance] = useState<number | null>(null);
  const [balanceLoading, setBalanceLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const invoiceId = searchParams.get("invoiceId");
  const finalAmount = useMemo(
    () => customAmount ? Number(customAmount) : amount,
    [amount, customAmount],
  );

  useEffect(() => {
    let active = true;
    getMyWallet()
      .then((res) => {
        if (active) setBalance(res.data?.availableBalance ?? null);
      })
      .catch(() => undefined)
      .finally(() => {
        if (active) setBalanceLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function selectQuickAmount(value: number) {
    setAmount(value);
    setCustomAmount("");
    setError(null);
  }

  function changeCustomAmount(value: string) {
    const digits = value.replace(/[^0-9]/g, "");
    setCustomAmount(digits.replace(/^0+(?=\d)/, ""));
    setError(null);
  }

  async function handleTopup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!Number.isFinite(finalAmount) || finalAmount < MINIMUM_TOPUP_AMOUNT) {
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
    <CustomerPortalLayout>
      <section className="tw-mx-auto tw-w-full tw-max-w-[1060px] tw-pb-5">
        <div className="tw-mb-5 tw-flex tw-items-start tw-justify-between tw-gap-5 max-[640px]:tw-flex-col">
          <div>
            <Link className="tw-mb-3 tw-inline-flex tw-items-center tw-gap-2 tw-text-[0.75rem] tw-font-semibold tw-text-[#587091] hover:tw-text-[#0868ee] hover:tw-no-underline" to="/customer/wallet">
              <i className="fas fa-arrow-left tw-text-[0.65rem]" /> Quay lại Ví của tôi
            </Link>
            <h1 className="tw-m-0 tw-font-[Cambria,Georgia,serif] tw-text-[1.75rem] tw-font-bold tw-leading-tight tw-text-[#071738]">Nạp tiền vào ví</h1>
            <p className="tw-mb-0 tw-mt-1 tw-text-[0.82rem] tw-text-[#6f809b]">Chọn số tiền và thanh toán an toàn qua cổng VNPay</p>
          </div>
          <div className="tw-flex tw-min-w-[210px] tw-items-center tw-gap-3 tw-rounded-xl tw-border tw-border-solid tw-border-[#dce6f3] tw-bg-white tw-px-4 tw-py-3 tw-shadow-[0_4px_15px_rgba(15,55,100,0.04)]">
            <span className="tw-grid tw-h-10 tw-w-10 tw-place-items-center tw-rounded-lg tw-bg-blue-50 tw-text-[#0868ee]"><i className="fas fa-wallet" /></span>
            <span>
              <small className="tw-block tw-text-[0.68rem] tw-font-medium tw-text-[#71819a]">Số dư hiện tại</small>
              <strong className="tw-mt-0.5 tw-block tw-text-[0.95rem] tw-font-bold tw-text-[#071738]">{balanceLoading ? "Đang tải..." : formatCurrency(balance)}</strong>
            </span>
          </div>
        </div>

        {invoiceId ? (
          <div className="tw-mb-4 tw-flex tw-items-start tw-gap-3 tw-rounded-xl tw-border tw-border-solid tw-border-blue-200 tw-bg-blue-50 tw-px-4 tw-py-3 tw-text-[0.78rem] tw-text-[#24517f]">
            <i className="fas fa-info-circle tw-mt-0.5 tw-text-[#0868ee]" />
            <p className="tw-m-0"><strong>Nạp tiền để hoàn tất thanh toán.</strong> Sau khi nạp thành công, bạn sẽ được đưa về hóa đơn <span className="tw-font-semibold">{invoiceId}</span>.</p>
          </div>
        ) : null}

        <form className="tw-grid tw-grid-cols-[minmax(0,1.45fr)_minmax(280px,0.85fr)] tw-overflow-hidden tw-rounded-2xl tw-border tw-border-solid tw-border-[#dce6f3] tw-bg-white tw-shadow-[0_12px_35px_rgba(17,58,105,0.08)] max-[860px]:tw-grid-cols-1" onSubmit={handleTopup}>
          <div className="tw-p-7 max-[640px]:tw-p-5">
            <div className="tw-mb-6 tw-flex tw-items-center tw-gap-3">
              <span className="tw-grid tw-h-10 tw-w-10 tw-place-items-center tw-rounded-xl tw-bg-[#eaf3ff] tw-text-[#0868ee]"><i className="fas fa-coins" /></span>
              <div>
                <h2 className="tw-m-0 tw-text-[1rem] tw-font-bold tw-text-[#0b1d3d]">Chọn số tiền muốn nạp</h2>
                <p className="tw-mb-0 tw-mt-1 tw-text-[0.71rem] tw-text-[#7a8aa2]">Tối thiểu {formatCurrency(MINIMUM_TOPUP_AMOUNT)} mỗi giao dịch</p>
              </div>
            </div>

            <div aria-label="Mệnh giá nạp nhanh" className="tw-grid tw-grid-cols-3 tw-gap-3 max-[560px]:tw-grid-cols-2" role="radiogroup">
              {QUICK_TOPUP_AMOUNTS.map((value) => {
                const selected = amount === value && !customAmount;
                return (
                  <button
                    aria-checked={selected}
                    className={`tw-relative tw-min-h-[62px] tw-rounded-xl tw-border tw-border-solid tw-px-3 tw-py-3 tw-text-[0.84rem] tw-font-semibold tw-transition ${selected ? "tw-border-[#0868ee] tw-bg-[#edf5ff] tw-text-[#075bd6] tw-shadow-[0_0_0_1px_#0868ee]" : "tw-border-[#dce5f1] tw-bg-white tw-text-[#2f4261] hover:tw-border-blue-300 hover:tw-bg-blue-50/50"}`}
                    key={value}
                    onClick={() => selectQuickAmount(value)}
                    role="radio"
                    type="button"
                  >
                    {formatCurrency(value)}
                    {selected ? <i className="fas fa-check-circle tw-absolute tw-right-2 tw-top-2 tw-text-[0.68rem] tw-text-[#0868ee]" /> : null}
                  </button>
                );
              })}
            </div>

            <label className="tw-mt-6 tw-block">
              <span className="tw-mb-2 tw-block tw-text-[0.76rem] tw-font-semibold tw-text-[#344967]">Hoặc nhập số tiền khác</span>
              <span className={`tw-flex tw-h-[54px] tw-items-center tw-rounded-xl tw-border tw-border-solid tw-bg-white tw-transition focus-within:tw-border-[#0868ee] focus-within:tw-shadow-[0_0_0_3px_rgba(8,104,238,0.1)] ${customAmount ? "tw-border-[#86b9f8]" : "tw-border-[#dce5f1]"}`}>
                <span className="tw-grid tw-h-full tw-w-12 tw-place-items-center tw-border-0 tw-border-r tw-border-solid tw-border-[#e5ebf3] tw-text-[#7c8ba1]"><i className="fas fa-pen" /></span>
                <input
                  aria-describedby="custom-amount-help"
                  className="tw-h-full tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-px-4 tw-text-[0.9rem] tw-font-semibold tw-text-[#102442] tw-outline-none placeholder:tw-font-normal placeholder:tw-text-[#9aa7ba]"
                  inputMode="numeric"
                  onChange={(event) => changeCustomAmount(event.target.value)}
                  placeholder="Nhập số tiền"
                  value={customAmount ? new Intl.NumberFormat("vi-VN").format(Number(customAmount)) : ""}
                />
                <strong className="tw-pr-4 tw-text-[0.78rem] tw-font-bold tw-text-[#526681]">VND</strong>
              </span>
              <small className="tw-mt-2 tw-block tw-text-[0.67rem] tw-text-[#8391a6]" id="custom-amount-help">Chỉ nhập số nguyên, không bao gồm dấu chấm hoặc ký tự đặc biệt.</small>
            </label>

            {error ? (
              <div className="tw-mt-4 tw-flex tw-items-start tw-gap-2 tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2.5 tw-text-[0.75rem] tw-font-medium tw-text-red-700" role="alert">
                <i className="fas fa-exclamation-circle tw-mt-0.5" /><span>{error}</span>
              </div>
            ) : null}
          </div>

          <aside className="tw-flex tw-flex-col tw-border-0 tw-border-l tw-border-solid tw-border-[#e2e9f2] tw-bg-[#f8faff] tw-p-7 max-[860px]:tw-border-l-0 max-[860px]:tw-border-t max-[640px]:tw-p-5">
            <h2 className="tw-m-0 tw-text-[1rem] tw-font-bold tw-text-[#0b1d3d]">Chi tiết nạp tiền</h2>
            <div className="tw-mt-5 tw-rounded-xl tw-border tw-border-solid tw-border-[#dce6f3] tw-bg-white tw-p-4">
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-[#edf1f6] tw-pb-3">
                <span className="tw-text-[0.72rem] tw-text-[#71819a]">Số tiền nạp</span>
                <strong className="tw-text-[0.9rem] tw-font-bold tw-text-[#132747]">{formatCurrency(Number.isFinite(finalAmount) ? finalAmount : 0)}</strong>
              </div>
              <div className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-py-3">
                <span className="tw-text-[0.72rem] tw-text-[#71819a]">Phí giao dịch</span>
                <strong className="tw-text-[0.78rem] tw-font-semibold tw-text-emerald-600">Miễn phí</strong>
              </div>
              <div className="tw-flex tw-items-end tw-justify-between tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-[#edf1f6] tw-pt-3">
                <span className="tw-text-[0.74rem] tw-font-semibold tw-text-[#405473]">Tổng thanh toán</span>
                <strong className="tw-text-[1.25rem] tw-font-bold tw-text-[#075fdc]">{formatCurrency(Number.isFinite(finalAmount) ? finalAmount : 0)}</strong>
              </div>
            </div>

            <div className="tw-mt-5">
              <span className="tw-mb-2 tw-block tw-text-[0.72rem] tw-font-semibold tw-text-[#405473]">Phương thức thanh toán</span>
              <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-xl tw-border tw-border-solid tw-border-[#91bdf5] tw-bg-white tw-p-3 tw-shadow-[0_0_0_1px_rgba(8,104,238,0.08)]">
                <span className="tw-grid tw-h-10 tw-w-12 tw-place-items-center tw-rounded-lg tw-bg-[#eaf3ff] tw-text-[#075fdc]"><i className="fas fa-university" /></span>
                <span className="tw-min-w-0 tw-flex-1"><strong className="tw-block tw-text-[0.8rem] tw-font-bold tw-text-[#173252]">VNPay</strong><small className="tw-mt-0.5 tw-block tw-text-[0.64rem] tw-text-[#7b8aa0]">ATM, QR và ngân hàng điện tử</small></span>
                <i className="fas fa-check-circle tw-text-[#0875ea]" />
              </div>
            </div>

            <div className="tw-mt-auto tw-pt-6">
              <button
                className="tw-flex tw-min-h-[48px] tw-w-full tw-items-center tw-justify-center tw-gap-2 tw-rounded-xl tw-border-0 tw-bg-[linear-gradient(100deg,#075de6,#087cf3)] tw-px-5 tw-text-[0.84rem] tw-font-bold tw-text-white tw-shadow-[0_9px_20px_rgba(8,104,238,0.24)] tw-transition hover:tw-brightness-105 disabled:tw-cursor-not-allowed disabled:tw-opacity-60"
                disabled={loading}
                type="submit"
              >
                {loading ? <><i className="fas fa-spinner fa-spin" /> Đang tạo giao dịch...</> : <>Tiếp tục với VNPay <i className="fas fa-arrow-right tw-text-[0.7rem]" /></>}
              </button>
              <button className="tw-mt-3 tw-w-full tw-border-0 tw-bg-transparent tw-py-2 tw-text-[0.74rem] tw-font-semibold tw-text-[#657791] hover:tw-text-[#075fdc]" disabled={loading} onClick={() => navigate(-1)} type="button">Hủy và quay lại</button>
              <p className="tw-mb-0 tw-mt-4 tw-flex tw-items-start tw-justify-center tw-gap-2 tw-text-center tw-text-[0.62rem] tw-leading-5 tw-text-[#8794a8]"><i className="fas fa-lock tw-mt-1 tw-text-emerald-600" />Giao dịch được mã hóa và xử lý bảo mật bởi VNPay.</p>
            </div>
          </aside>
        </form>
      </section>
    </CustomerPortalLayout>
  );
}
