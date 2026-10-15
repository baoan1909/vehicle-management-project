import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import {
  getMyWallet,
  getMyWalletTopups,
  getMyWalletTransactions,
  type WalletResponse,
  type WalletTopupResponse,
  type WalletTransactionResponse,
} from "@/features/billing/api/walletApi";
import { CustomerPortalLayout } from "@/features/customer-portal/pages/PortalShared";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";

const PAGE_SIZE = 6;
type TransactionFilter = "ALL" | "TOPUP" | "PAYMENT" | "REFUND";

const filters: Array<{ key: TransactionFilter; label: string }> = [
  { key: "ALL", label: "Tất cả" },
  { key: "TOPUP", label: "Nạp tiền" },
  { key: "PAYMENT", label: "Thanh toán" },
  { key: "REFUND", label: "Hoàn tiền" },
];

function formatCurrency(value?: number | null) {
  if (value == null || !Number.isFinite(Number(value))) return "—";
  return `${new Intl.NumberFormat("vi-VN").format(Number(value))} ₫`;
}

function formatDateTime(value?: string | null) {
  if (!value) return { date: "--/--/----", time: "--:--" };
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return { date: "--/--/----", time: "--:--" };
  return {
    date: new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date),
    time: new Intl.DateTimeFormat("vi-VN", { hour: "2-digit", minute: "2-digit", hour12: false }).format(date),
  };
}

function isCurrentMonth(value?: string | null) {
  if (!value) return false;
  const date = new Date(value);
  const now = new Date();
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
}

function compactWalletId(walletId?: string | null) {
  if (!walletId) return "Đang khởi tạo";
  const value = walletId.replaceAll("-", "").toUpperCase();
  return `WAL-CUS-${value.slice(0, 4)}-${value.slice(-4)}`;
}

function walletStatusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    ACTIVE: "Đang hoạt động",
    CLOSED: "Đã đóng",
    DEBIT_BLOCKED: "Tạm ngưng chi",
    LOCKED: "Đã khóa",
  };
  return status ? labels[status] ?? status : "Đang khởi tạo";
}

function transactionKind(type: string): Exclude<TransactionFilter, "ALL"> | "OTHER" {
  if (["TOP_UP", "WALLET_TOP_UP", "WALLET_ADJUSTMENT"].includes(type)) return "TOPUP";
  if (type === "INVOICE_PAYMENT") return "PAYMENT";
  if (type === "REFUND") return "REFUND";
  return "OTHER";
}

function transactionStatus(status: string) {
  if (["POSTED", "COMPLETED"].includes(status)) return { icon: "fas fa-check-circle", label: "Thành công", style: "tw-bg-emerald-50 tw-text-emerald-600" };
  if (status === "PENDING") return { icon: "fas fa-clock", label: "Đang xử lý", style: "tw-bg-amber-50 tw-text-amber-600" };
  if (status === "REVERSED") return { icon: "fas fa-undo-alt", label: "Đã hoàn", style: "tw-bg-blue-50 tw-text-blue-600" };
  return { icon: "fas fa-times-circle", label: status === "FAILED" ? "Thất bại" : status, style: "tw-bg-rose-50 tw-text-rose-600" };
}

function transactionMeta(transaction: WalletTransactionResponse, topupById: Map<string, WalletTopupResponse>) {
  const kind = transactionKind(transaction.transactionType);
  const topup = transaction.referenceId ? topupById.get(transaction.referenceId) : undefined;
  if (kind === "TOPUP") return {
    amount: topup?.amount ?? null, direction: "credit",
    icon: "fas fa-wallet", iconStyle: "tw-bg-emerald-50 tw-text-emerald-600",
    title: transaction.transactionType === "WALLET_ADJUSTMENT" ? "Điều chỉnh số dư" : "Nạp tiền vào ví",
    subtitle: topup?.transactionRef ? `VNPAY · ${topup.transactionRef}` : "Cộng tiền vào ví",
  };
  if (kind === "PAYMENT") return {
    amount: null, direction: "debit", icon: "fas fa-receipt", iconStyle: "tw-bg-rose-50 tw-text-rose-500",
    title: "Thanh toán vé gửi xe", subtitle: "Thanh toán hóa đơn dịch vụ",
  };
  if (kind === "REFUND") return {
    amount: null, direction: "credit", icon: "fas fa-undo-alt", iconStyle: "tw-bg-amber-50 tw-text-amber-500",
    title: "Hoàn tiền", subtitle: "Hoàn tiền về ví cá nhân",
  };
  return {
    amount: null, direction: "neutral", icon: "fas fa-exchange-alt", iconStyle: "tw-bg-blue-50 tw-text-blue-600",
    title: "Biến động số dư", subtitle: "Giao dịch ví",
  };
}

function WalletStatCard({ icon, iconStyle, label, note, noteStyle, value }: {
  icon: string; iconStyle: string; label: string; note: string; noteStyle: string; value: string;
}) {
  return (
    <article className="tw-min-h-[144px] tw-rounded-[9px] tw-border tw-border-solid tw-border-[#dfe7f2] tw-bg-white tw-px-5 tw-py-4 tw-shadow-[0_4px_14px_rgba(15,55,100,0.035)]">
      <span className={`tw-grid tw-h-10 tw-w-10 tw-place-items-center tw-rounded-lg ${iconStyle}`}><i className={icon} /></span>
      <p className="tw-m-0 tw-mt-3 tw-text-[0.78rem] tw-font-medium tw-text-[#435473]">{label}</p>
      <strong className="tw-mt-1 tw-block tw-text-[1.28rem] tw-font-bold tw-leading-tight tw-text-[#071738]">{value}</strong>
      <small className={`tw-mt-2 tw-flex tw-items-center tw-gap-1.5 tw-text-[0.69rem] tw-font-semibold ${noteStyle}`}><i className="fas fa-arrow-up tw-text-[0.58rem]" />{note}</small>
    </article>
  );
}

function WalletPagination({ currentPage, onPageChange, totalPages, totalRecords }: {
  currentPage: number; onPageChange: (page: number) => void; totalPages: number; totalRecords: number;
}) {
  const start = totalRecords === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1;
  const end = Math.min(currentPage * PAGE_SIZE, totalRecords);
  const visible = Array.from(new Set([1, currentPage - 1, currentPage, currentPage + 1, totalPages]))
    .filter((item) => item > 0 && item <= totalPages)
    .sort((left, right) => left - right);
  const buttonClass = "tw-grid tw-h-8 tw-min-w-8 tw-place-items-center tw-rounded-md tw-border tw-border-solid tw-border-[#dfe7f2] tw-bg-white tw-px-2 tw-text-[0.75rem] tw-font-medium tw-text-[#536680] disabled:tw-cursor-not-allowed disabled:tw-opacity-40";
  return (
    <footer className="tw-flex tw-items-center tw-justify-between tw-gap-4 tw-px-5 tw-pb-4 tw-pt-4 max-[640px]:tw-flex-col max-[640px]:tw-items-start">
      <p className="tw-m-0 tw-text-[0.7rem] tw-font-normal tw-text-[#71819b]">{totalRecords === 0 ? "Không có giao dịch" : `Hiển thị ${start} - ${end} của ${totalRecords} giao dịch`}</p>
      <nav aria-label="Phân trang giao dịch" className="tw-flex tw-items-center tw-gap-1.5">
        <button aria-label="Trang trước" className={buttonClass} disabled={currentPage === 1} type="button" onClick={() => onPageChange(currentPage - 1)}><i className="fas fa-chevron-left tw-text-[0.62rem]" /></button>
        {visible.map((item, index) => (
          <span className="tw-flex tw-items-center tw-gap-1.5" key={item}>
            {index > 0 && item - visible[index - 1] > 1 ? <span className="tw-px-1 tw-text-slate-400">…</span> : null}
            <button aria-current={item === currentPage ? "page" : undefined} className={`${buttonClass} ${item === currentPage ? "!tw-border-[#0868ee] !tw-bg-[#0868ee] !tw-text-white" : ""}`} type="button" onClick={() => onPageChange(item)}>{item}</button>
          </span>
        ))}
        <button aria-label="Trang sau" className={buttonClass} disabled={currentPage === totalPages} type="button" onClick={() => onPageChange(currentPage + 1)}><i className="fas fa-chevron-right tw-text-[0.62rem]" /></button>
      </nav>
    </footer>
  );
}

export function CustomerWalletPage() {
  const { user } = useAuth();
  const canTopUp = hasAnyPermission(user, ["WALLET_TOP_UP_OWN"]);
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [transactions, setTransactions] = useState<WalletTransactionResponse[]>([]);
  const [topups, setTopups] = useState<WalletTopupResponse[]>([]);
  const [filter, setFilter] = useState<TransactionFilter>("ALL");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    let mounted = true;
    getMyWallet()
      .then(async (walletResult) => {
        if (!mounted) return;
        setWallet(walletResult.data);
        const transactionResult = await getMyWalletTransactions(0, 100);
        if (!mounted) return;
        setTransactions(transactionResult.data ?? []);
        if (canTopUp) {
          try {
            const topupResult = await getMyWalletTopups(0, 100);
            if (mounted) setTopups(topupResult.data ?? []);
          } catch {
            if (mounted) setTopups([]);
          }
        }
        setError(null);
      })
      .catch((requestError) => {
        if (mounted) setError(requestError instanceof Error ? requestError.message : "Không tải được dữ liệu ví.");
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => { mounted = false; };
  }, [canTopUp]);

  const topupById = useMemo(() => new Map(topups.map((item) => [item.topupOrderId, item])), [topups]);
  const completedTopups = useMemo(
    () => topups.filter((item) => item.status === "COMPLETED" && isCurrentMonth(item.completedAt ?? item.expiresAt)),
    [topups],
  );
  const deposited = completedTopups.reduce((sum, item) => sum + Number(item.amount || 0), 0);
  const paymentsThisMonth = transactions.filter((item) => transactionKind(item.transactionType) === "PAYMENT" && isCurrentMonth(item.occurredAt)).length;
  const refundsThisMonth = transactions.filter((item) => transactionKind(item.transactionType) === "REFUND" && isCurrentMonth(item.occurredAt)).length;
  const counts = useMemo(() => ({
    ALL: transactions.length,
    TOPUP: transactions.filter((item) => transactionKind(item.transactionType) === "TOPUP").length,
    PAYMENT: transactions.filter((item) => transactionKind(item.transactionType) === "PAYMENT").length,
    REFUND: transactions.filter((item) => transactionKind(item.transactionType) === "REFUND").length,
  }), [transactions]);
  const filtered = useMemo(
    () => filter === "ALL" ? transactions : transactions.filter((item) => transactionKind(item.transactionType) === filter),
    [filter, transactions],
  );
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const visibleTransactions = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  useEffect(() => { setPage(1); }, [filter]);

  async function copyWalletId() {
    if (!wallet?.walletId) return;
    await navigator.clipboard.writeText(wallet.walletId);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  }

  if (!hasAnyPermission(user, ["WALLET_READ_OWN"])) {
    return (
      <CustomerPortalLayout>
        <section className="tw-grid tw-min-h-[420px] tw-place-content-center tw-justify-items-center tw-gap-3 tw-rounded-xl tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-8 tw-text-center">
          <i className="fas fa-lock tw-text-3xl tw-text-slate-400" />
          <h1 className="tw-m-0 tw-text-2xl tw-font-bold tw-text-[#071738]">Bạn không có quyền xem ví</h1>
          <p className="tw-m-0 tw-text-sm tw-text-slate-500">Vui lòng liên hệ quản trị viên để được cấp quyền truy cập.</p>
        </section>
      </CustomerPortalLayout>
    );
  }

  return (
    <CustomerPortalLayout>
      <section className="tw-mx-auto tw-w-full tw-max-w-[1120px] tw-pb-2">
        <header className="tw-mb-4">
          <h1 className="tw-m-0 tw-text-[1.65rem] tw-font-bold tw-leading-tight tw-text-[#071738]" style={{ fontFamily: 'Inter, "Segoe UI", Arial, sans-serif' }}>Ví của tôi</h1>
          <p className="tw-m-0 tw-mt-1 tw-text-[0.82rem] tw-font-normal tw-text-[#71809b]">Quản lý số dư, nạp tiền và theo dõi giao dịch của bạn</p>
        </header>

        {error ? (
          <div className="tw-mb-4 tw-flex tw-items-center tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-sm tw-font-medium tw-text-red-700">
            <i className="fas fa-exclamation-circle" />{error}
          </div>
        ) : null}

        <section className="tw-relative tw-overflow-hidden tw-rounded-[10px] tw-bg-[linear-gradient(115deg,#075cf3_0%,#0879f6_56%,#39b8f5_100%)] tw-px-7 tw-py-6 tw-text-white tw-shadow-[0_14px_28px_rgba(5,99,232,0.18)] max-[640px]:tw-px-5">
          <div aria-hidden="true" className="tw-pointer-events-none tw-absolute -tw-right-20 -tw-top-24 tw-h-64 tw-w-64 tw-rounded-full tw-bg-white/10 tw-blur-2xl" />
          <div className="tw-relative tw-flex tw-items-start tw-justify-between tw-gap-5">
            <div>
              <p className="tw-m-0 tw-flex tw-items-center tw-gap-3 tw-text-[0.92rem] tw-font-medium"><span>Số dư khả dụng</span><i className="far fa-eye tw-text-[0.92rem]" /></p>
              <strong className="tw-mt-2 tw-block tw-text-[2.65rem] tw-font-bold tw-leading-none tw-tracking-[-0.035em] max-[640px]:tw-text-[2rem]">{loading ? "—" : formatCurrency(wallet?.availableBalance)}</strong>
              <button className="tw-mt-3 tw-flex tw-items-center tw-gap-2 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[0.82rem] tw-font-medium tw-text-white" type="button" onClick={() => void copyWalletId()}>
                <span>Mã ví: {compactWalletId(wallet?.walletId)}</span><i className={copied ? "fas fa-check" : "far fa-copy"} /><span className="tw-sr-only">{copied ? "Đã sao chép" : "Sao chép mã ví"}</span>
              </button>
            </div>
            <span className="tw-inline-flex tw-items-center tw-gap-2 tw-rounded-md tw-bg-[#d9f8e8] tw-px-3 tw-py-1.5 tw-text-[0.76rem] tw-font-bold tw-text-[#157348]">
              <span className="tw-h-2 tw-w-2 tw-rounded-full tw-bg-[#1cb875]" />{walletStatusLabel(wallet?.status)}
            </span>
          </div>
          <div className="tw-relative tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-3 max-[640px]:tw-grid-cols-1">
            {canTopUp ? <Link className="tw-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-white/85 tw-bg-white/5 tw-px-5 tw-text-[0.93rem] tw-font-semibold tw-text-white tw-transition hover:tw-bg-white/10 hover:tw-text-white hover:tw-no-underline" to="/customer/wallet/topup"><i className="fas fa-plus-circle tw-text-[1.05rem]" />Nạp tiền</Link> : <span aria-disabled="true" className="tw-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-white/35 tw-bg-white/5 tw-px-5 tw-text-[0.93rem] tw-font-semibold tw-text-white/60"><i className="fas fa-plus-circle tw-text-[1.05rem]" />Nạp tiền</span>}
            <Link className="tw-flex tw-min-h-12 tw-items-center tw-justify-center tw-gap-3 tw-rounded-lg tw-border tw-border-solid tw-border-white tw-bg-white tw-px-5 tw-text-[0.93rem] tw-font-semibold tw-text-[#0969eb] tw-shadow-sm tw-transition hover:tw-bg-blue-50 hover:tw-text-[#0759d8] hover:tw-no-underline" to="/customer/subscriptions"><i className="fas fa-receipt" />Thanh toán vé</Link>
          </div>
        </section>

        <section className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-4 max-[760px]:tw-grid-cols-1">
          <WalletStatCard icon="fas fa-wallet" iconStyle="tw-bg-emerald-50 tw-text-emerald-600" label="Đã nạp tháng này" value={formatCurrency(deposited)} note={`${completedTopups.length} giao dịch thành công`} noteStyle="tw-text-emerald-600" />
          <WalletStatCard icon="fas fa-receipt" iconStyle="tw-bg-rose-50 tw-text-rose-500" label="Đã chi tháng này" value={paymentsThisMonth === 0 ? formatCurrency(0) : "—"} note={`${paymentsThisMonth} giao dịch thanh toán`} noteStyle="tw-text-rose-500 [&>i]:tw-rotate-180" />
          <WalletStatCard icon="fas fa-undo-alt" iconStyle="tw-bg-amber-50 tw-text-amber-500" label="Hoàn tiền" value={refundsThisMonth === 0 ? formatCurrency(0) : "—"} note={`${refundsThisMonth} giao dịch hoàn tiền`} noteStyle="tw-text-emerald-600" />
        </section>

        <section className="tw-mt-4 tw-overflow-hidden tw-rounded-[10px] tw-border tw-border-solid tw-border-[#dfe7f2] tw-bg-white tw-shadow-[0_4px_16px_rgba(15,55,100,0.035)]">
          <div className="tw-px-5 tw-pt-4">
            <h2 className="tw-m-0 tw-text-[1.02rem] tw-font-bold tw-text-[#071738]">Lịch sử giao dịch</h2>
            <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-2" role="tablist" aria-label="Lọc giao dịch ví">
              {filters.map((item) => {
                const active = filter === item.key;
                return (
                  <button aria-selected={active} className={`tw-min-h-9 tw-rounded-full tw-border tw-border-solid tw-px-4 tw-text-[0.74rem] tw-font-medium tw-transition ${active ? "tw-border-[#0868ee] tw-bg-[#0868ee] tw-text-white tw-shadow-[0_5px_12px_rgba(8,104,238,0.2)]" : "tw-border-[#dce5f1] tw-bg-[#f8fafd] tw-text-[#344766] hover:tw-border-blue-300"}`} key={item.key} role="tab" type="button" onClick={() => setFilter(item.key)}>
                    {item.label} ({counts[item.key]})
                  </button>
                );
              })}
            </div>
          </div>

          <div className="vm-wallet-desktop-table tw-mx-5 tw-mt-3 tw-overflow-x-auto tw-rounded-t-md tw-border tw-border-solid tw-border-[#e2e9f2]">
            <table className="tw-w-full tw-min-w-[790px] tw-border-collapse tw-text-left">
              <thead className="tw-bg-[#f6f8fb] tw-text-[0.7rem] tw-font-semibold tw-text-[#405170]">
                <tr>
                  <th className="tw-w-[18%] tw-px-3 tw-py-2.5">Ngày</th>
                  <th className="tw-w-[36%] tw-px-3 tw-py-2.5">Nội dung giao dịch</th>
                  <th className="tw-w-[17%] tw-px-3 tw-py-2.5">Số tiền</th>
                  <th className="tw-w-[15%] tw-px-3 tw-py-2.5">Trạng thái</th>
                  <th className="tw-w-[18%] tw-px-3 tw-py-2.5">Mã tham chiếu</th>
                </tr>
              </thead>
              <tbody className="tw-divide-y tw-divide-[#e7edf5]">
                {visibleTransactions.map((transaction) => {
                  const meta = transactionMeta(transaction, topupById);
                  const status = transactionStatus(transaction.status);
                  const date = formatDateTime(transaction.occurredAt);
                  const prefix = meta.amount == null ? "" : meta.direction === "credit" ? "+" : meta.direction === "debit" ? "−" : "";
                  const amountStyle = meta.direction === "credit" ? "tw-text-emerald-600" : meta.direction === "debit" ? "tw-text-rose-500" : "tw-text-slate-600";
                  return (
                    <tr className="tw-bg-white tw-text-[0.73rem] tw-text-[#233654] hover:tw-bg-blue-50/40" key={transaction.financialTransactionId}>
                      <td className="tw-whitespace-nowrap tw-px-3 tw-py-2"><span className="tw-block">{date.date}</span><small className="tw-mt-0.5 tw-block tw-text-[0.67rem] tw-text-[#6f819e]">{date.time}</small></td>
                      <td className="tw-px-3 tw-py-2">
                        <div className="tw-flex tw-items-center tw-gap-3">
                          <span className={`tw-grid tw-h-8 tw-w-8 tw-flex-none tw-place-items-center tw-rounded-md ${meta.iconStyle}`}><i className={meta.icon} /></span>
                          <span className="tw-min-w-0"><strong className="tw-block tw-truncate tw-font-semibold tw-text-[#122544]">{meta.title}</strong><small className="tw-mt-0.5 tw-block tw-truncate tw-text-[0.66rem] tw-text-[#71819b]">{meta.subtitle}</small></span>
                        </div>
                      </td>
                      <td className={`tw-whitespace-nowrap tw-px-3 tw-py-2 tw-font-bold ${amountStyle}`}>{prefix}{formatCurrency(meta.amount)}</td>
                      <td className="tw-px-3 tw-py-2"><span className={`tw-inline-flex tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.66rem] tw-font-semibold ${status.style}`}><i className={status.icon} />{status.label}</span></td>
                      <td className="tw-max-w-[170px] tw-truncate tw-whitespace-nowrap tw-px-3 tw-py-2 tw-font-medium tw-text-[#2f4363]" title={transaction.transactionCode}>{transaction.transactionCode}</td>
                    </tr>
                  );
                })}
                {!loading && visibleTransactions.length === 0 ? <tr><td className="tw-px-4 tw-py-12 tw-text-center tw-text-sm tw-text-slate-500" colSpan={5}><i className="far fa-folder-open tw-mb-2 tw-block tw-text-2xl tw-text-slate-300" />Chưa có giao dịch phù hợp.</td></tr> : null}
                {loading ? <tr><td className="tw-px-4 tw-py-12 tw-text-center tw-text-sm tw-text-slate-500" colSpan={5}><i className="fas fa-spinner fa-spin tw-mr-2 tw-text-blue-600" />Đang tải lịch sử giao dịch...</td></tr> : null}
              </tbody>
            </table>
          </div>
          <ul className="vm-wallet-mobile-cards" aria-label="Lịch sử giao dịch">
            {visibleTransactions.map((transaction) => {
              const meta = transactionMeta(transaction, topupById);
              const status = transactionStatus(transaction.status);
              const date = formatDateTime(transaction.occurredAt);
              const prefix = meta.amount == null ? "" : meta.direction === "credit" ? "+" : meta.direction === "debit" ? "−" : "";
              const amountStyle = meta.direction === "credit" ? "tw-text-emerald-600" : meta.direction === "debit" ? "tw-text-rose-500" : "tw-text-slate-600";
              return (
                <li className="vm-support-mobile-card" key={transaction.financialTransactionId}>
                  <div className="tw-flex tw-items-center tw-justify-between tw-gap-2">
                    <strong>{date.date} · {date.time}</strong>
                    <span className={`tw-inline-flex tw-items-center tw-gap-1.5 tw-whitespace-nowrap tw-rounded-full tw-px-2.5 tw-py-1 tw-text-[0.66rem] tw-font-semibold ${status.style}`}><i className={status.icon} />{status.label}</span>
                  </div>
                  <span className="tw-text-[0.9rem] tw-font-bold tw-text-slate-900">{meta.title}</span>
                  <span className="tw-truncate tw-text-[0.78rem] tw-font-semibold tw-text-slate-500" title={transaction.transactionCode}>{meta.subtitle} · {transaction.transactionCode}</span>
                  <strong className={`tw-text-right tw-text-[1.05rem] ${amountStyle}`}>{prefix}{formatCurrency(meta.amount)}</strong>
                </li>
              );
            })}
            {!loading && visibleTransactions.length === 0 ? <li className="vm-support-mobile-card">Chưa có giao dịch phù hợp.</li> : null}
            {loading ? <li className="vm-support-mobile-card">Đang tải lịch sử giao dịch...</li> : null}
          </ul>
          <WalletPagination currentPage={safePage} totalPages={totalPages} totalRecords={filtered.length} onPageChange={setPage} />
        </section>
      </section>
    </CustomerPortalLayout>
  );
}
