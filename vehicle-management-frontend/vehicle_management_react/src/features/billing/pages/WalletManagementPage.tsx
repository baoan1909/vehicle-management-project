import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { DateRangeInput } from "@/shared/components/ui/DateRangeInput";
import { Drawer } from "@/shared/components/ui/Drawer";
import { Modal } from "@/shared/components/ui/Modal";
import { SearchInput } from "@/shared/components/ui/SearchInput";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { cn } from "@/lib/cn";
import {
  getAdminWalletTransactions,
  listAdminWallets,
  lockWallet,
  requestWalletAdjustment,
  unlockWallet,
  type AdminWalletResponse,
  type WalletTransactionResponse,
} from "@/features/billing/api/walletApi";

type WalletKind = "CUSTOMER" | "PARTNER";

type WalletRow = {
  id: string;
  code: string;
  owner: string;
  kind: WalletKind;
  kindLabel: string;
  available: number;
  held: number;
  locked: boolean;
  createdAt: string;
  lastActive: string;
};

type DetailTab = "overview" | "transactions" | "ledger" | "risks";

const FALLBACK_ROWS: WalletRow[] = [
  { id: "1", code: "WAL-CUS-7F3A", owner: "Nguyễn Văn Dũng", kind: "CUSTOMER", kindLabel: "Khách hàng", available: 2450000, held: 0, locked: false, createdAt: "02/01/2024 09:10", lastActive: "12/04/2025 14:02" },
  { id: "2", code: "WAL-CUS-9182", owner: "Trần Thị Mai", kind: "CUSTOMER", kindLabel: "Khách hàng", available: 120000, held: 0, locked: false, createdAt: "18/02/2024 11:44", lastActive: "11/04/2025 16:20" },
  { id: "3", code: "WAL-PAR-3K9D", owner: "Bãi xe Thành Công", kind: "PARTNER", kindLabel: "Đối tác", available: 28450000, held: 5320000, locked: false, createdAt: "15/01/2024 10:24", lastActive: "12/04/2025 14:32" },
  { id: "4", code: "WAL-PAR-8H7F", owner: "Bãi xe Long Biên", kind: "PARTNER", kindLabel: "Đối tác", available: 12800000, held: 2100000, locked: false, createdAt: "20/03/2024 08:15", lastActive: "12/04/2025 11:05" },
  { id: "5", code: "WAL-CUS-47ZX", owner: "Lê Minh Khoa", kind: "CUSTOMER", kindLabel: "Khách hàng", available: 0, held: 0, locked: true, createdAt: "05/05/2024 14:30", lastActive: "09/04/2025 09:12" },
  { id: "6", code: "WAL-CUS-6D8H", owner: "Phạm Thu Hà", kind: "CUSTOMER", kindLabel: "Khách hàng", available: 320000, held: 0, locked: false, createdAt: "11/06/2024 10:02", lastActive: "10/04/2025 18:40" },
  { id: "7", code: "WAL-PAR-9L1M", owner: "Bãi xe Mỹ Đình", kind: "PARTNER", kindLabel: "Đối tác", available: 6750000, held: 1200000, locked: false, createdAt: "09/09/2024 15:26", lastActive: "11/04/2025 08:55" },
];

const FALLBACK_KPIS = {
  customerBalance: 12450320000,
  partnerDebt: 3280450000,
  held: 986340000,
  anomalies: 24,
};

function formatVnd(value: number) {
  return `${new Intl.NumberFormat("vi-VN").format(Math.round(value))} đ`;
}

function formatNum(value: number) {
  return new Intl.NumberFormat("vi-VN").format(Math.round(value));
}

function toRow(wallet: AdminWalletResponse): WalletRow {
  const kind: WalletKind = wallet.ownerType === "ORGANIZATION" ? "PARTNER" : "CUSTOMER";
  const shortId = wallet.walletId.replaceAll("-", "").slice(0, 4).toUpperCase();
  return {
    id: wallet.walletId,
    code: `WAL-${kind === "PARTNER" ? "PAR" : "CUS"}-${shortId}`,
    owner: wallet.organizationId ?? wallet.customerId ?? "—",
    kind,
    kindLabel: kind === "PARTNER" ? "Đối tác" : "Khách hàng",
    available: wallet.availableBalance ?? 0,
    held: wallet.heldBalance ?? 0,
    locked: wallet.status !== "ACTIVE",
    createdAt: "—",
    lastActive: "—",
  };
}

function StatusPill({ locked }: { locked: boolean }) {
  if (locked) {
    return (
      <span className="tw-inline-flex tw-items-center tw-gap-1 tw-rounded-full tw-bg-[#FEE2E2] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#DC2626]">
        <i className="fas fa-fire" /> Tạm khóa
      </span>
    );
  }
  return (
    <span className="tw-inline-flex tw-items-center tw-gap-1 tw-rounded-full tw-bg-[#DCFCE7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#15803D]">
      Đang hoạt động
    </span>
  );
}

export function WalletManagementPage() {
  const { user } = useAuth();
  const [wallets, setWallets] = useState<AdminWalletResponse[]>([]);
  const [keyword, setKeyword] = useState("");
  const [kindFilter, setKindFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [dateRange, setDateRange] = useState("2025-04-01|2025-04-12");
  const [selectedId, setSelectedId] = useState<string | null>("3");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [detailTab, setDetailTab] = useState<DetailTab>("overview");
  const [transactions, setTransactions] = useState<WalletTransactionResponse[]>([]);
  const [adjustModalOpen, setAdjustModalOpen] = useState(false);
  const [adjustAmount, setAdjustAmount] = useState("");
  const [adjustDirection, setAdjustDirection] = useState<"CREDIT" | "DEBIT">("CREDIT");
  const [adjustReason, setAdjustReason] = useState("");
  const [adjustSubmitting, setAdjustSubmitting] = useState(false);
  const [adjustError, setAdjustError] = useState("");
  const [actionError, setActionError] = useState("");

  const canRead = hasAnyPermission(user, ["WALLET_READ_ALL"]);
  const canLock = hasAnyPermission(user, ["WALLET_LOCK_ALL"]);
  const canUnlock = hasAnyPermission(user, ["WALLET_UNLOCK_ALL"]);
  const canAdjust = hasAnyPermission(user, ["WALLET_ADJUST_REQUEST_ALL"]);

  useEffect(() => {
    if (!canRead) return;
    listAdminWallets(0, 50).then((res) => setWallets(res.data ?? [])).catch(() => undefined);
  }, [canRead]);

  const rows = useMemo<WalletRow[]>(() => {
    const live = wallets.map(toRow);
    const base = live.length > 0 ? live : FALLBACK_ROWS;
    const normalizedKeyword = keyword.trim().toLowerCase();
    return base.filter((row) => {
      if (kindFilter !== "all" && row.kind !== kindFilter) return false;
      if (statusFilter !== "all" && (statusFilter === "locked") !== row.locked) return false;
      if (normalizedKeyword && !`${row.code} ${row.owner}`.toLowerCase().includes(normalizedKeyword)) return false;
      return true;
    });
  }, [wallets, keyword, kindFilter, statusFilter]);

  const kpis = useMemo(() => {
    if (wallets.length === 0) return FALLBACK_KPIS;
    return {
      customerBalance: wallets.filter((w) => w.ownerType !== "ORGANIZATION").reduce((sum, w) => sum + (w.availableBalance ?? 0), 0),
      partnerDebt: wallets.filter((w) => w.ownerType === "ORGANIZATION").reduce((sum, w) => sum + (w.pendingBalance ?? 0) + (w.availableBalance ?? 0), 0),
      held: wallets.reduce((sum, w) => sum + (w.heldBalance ?? 0), 0),
      anomalies: wallets.filter((w) => w.status !== "ACTIVE").length,
    };
  }, [wallets]);

  const selected = rows.find((row) => row.id === selectedId) ?? rows[0] ?? null;

  function openDetail(row: WalletRow) {
    setSelectedId(row.id);
    setDetailTab("overview");
    setDrawerOpen(true);
    if (wallets.length > 0) {
      getAdminWalletTransactions(row.id, 0, 20).then((res) => setTransactions(res.data ?? [])).catch(() => setTransactions([]));
    } else {
      setTransactions([]);
    }
  }

  async function refreshWallets() {
    try {
      const res = await listAdminWallets(0, 50);
      setWallets(res.data ?? []);
    } catch {
      // Giữ dữ liệu hiện tại khi tải lại thất bại.
    }
  }

  async function handleLockToggle() {
    if (!selected) return;
    setActionError("");
    try {
      if (selected.locked) {
        await unlockWallet(selected.id);
      } else {
        await lockWallet(selected.id);
      }
      await refreshWallets();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : "Thao tác khóa/mở khóa thất bại");
    }
  }

  async function handleAdjustSubmit() {
    if (!selected) return;
    const amount = Number(adjustAmount.replace(/[^0-9]/g, ""));
    if (!Number.isFinite(amount) || amount <= 0) {
      setAdjustError("Vui lòng nhập số tiền hợp lệ");
      return;
    }
    if (!adjustReason.trim()) {
      setAdjustError("Vui lòng nhập lý do điều chỉnh");
      return;
    }
    setAdjustSubmitting(true);
    setAdjustError("");
    try {
      await requestWalletAdjustment(selected.id, Math.round(amount), adjustDirection, adjustReason.trim());
      setAdjustModalOpen(false);
      setAdjustAmount("");
      setAdjustReason("");
      await refreshWallets();
    } catch (error) {
      setAdjustError(error instanceof Error ? error.message : "Tạo yêu cầu điều chỉnh thất bại");
    } finally {
      setAdjustSubmitting(false);
    }
  }

  if (!canRead) {
    return <main className="tw-p-6">Bạn không có quyền quản trị ví.</main>;
  }

  const detailTabs: Array<{ key: DetailTab; label: string }> = [
    { key: "overview", label: "Tổng quan" },
    { key: "transactions", label: "Giao dịch gần đây" },
    { key: "ledger", label: "Xem sổ cái" },
    { key: "risks", label: "Cảnh báo rủi ro" },
  ];

  return (
    <main className="tw-mx-auto tw-w-full tw-max-w-[1480px] tw-px-5 tw-py-5 max-[768px]:tw-px-4">
      <div className="tw-flex tw-flex-col tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-white tw-p-5 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
        <div>
          <h1 className="tw-m-0 tw-text-[22px] tw-font-black tw-text-[#0F172A]">Quản lý ví & giao dịch</h1>
          <p className="tw-mb-0 tw-mt-1 tw-text-[13px] tw-font-medium tw-text-[#64748B]">
            Giám sát số dư, giao dịch và xử lý các trường hợp bất thường
          </p>
        </div>

        <section className="tw-grid tw-grid-cols-4 tw-gap-4 max-[1100px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#EFF6FF] tw-text-[22px] tw-text-[#2563EB]">
              <i className="fas fa-wallet" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Tổng số dư khách hàng</span>
              <strong className="tw-mt-1 tw-block tw-text-[19px] tw-font-black tw-text-[#0F172A]">{formatVnd(kpis.customerBalance)}</strong>
              <small className="tw-mt-1 tw-flex tw-items-center tw-gap-1 tw-text-[12px] tw-font-bold tw-text-[#16A34A]">
                <i className="fas fa-arrow-up" /> +5.2% <span className="tw-font-medium tw-text-[#64748B]">so với tháng trước</span>
              </small>
            </span>
          </div>
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#DCFCE7] tw-text-[22px] tw-text-[#16A34A]">
              <i className="fas fa-landmark" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Công nợ đối tác</span>
              <strong className="tw-mt-1 tw-block tw-text-[19px] tw-font-black tw-text-[#0F172A]">{formatVnd(kpis.partnerDebt)}</strong>
              <small className="tw-mt-1 tw-flex tw-items-center tw-gap-1 tw-text-[12px] tw-font-bold tw-text-[#16A34A]">
                <i className="fas fa-arrow-up" /> +8.1% <span className="tw-font-medium tw-text-[#64748B]">so với tháng trước</span>
              </small>
            </span>
          </div>
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#FEF3C7] tw-text-[22px] tw-text-[#F59E0B]">
              <i className="fas fa-lock" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Tiền đang giữ</span>
              <strong className="tw-mt-1 tw-block tw-text-[19px] tw-font-black tw-text-[#0F172A]">{formatVnd(kpis.held)}</strong>
              <small className="tw-mt-1 tw-flex tw-items-center tw-gap-1 tw-text-[12px] tw-font-bold tw-text-[#16A34A]">
                <i className="fas fa-arrow-up" /> +12.5% <span className="tw-font-medium tw-text-[#64748B]">so với tháng trước</span>
              </small>
            </span>
          </div>
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#FEE2E2] tw-text-[22px] tw-text-[#DC2626]">
              <i className="fas fa-exclamation-triangle" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Giao dịch bất thường</span>
              <strong className="tw-mt-1 tw-block tw-text-[19px] tw-font-black tw-text-[#DC2626]">{formatNum(kpis.anomalies)}</strong>
              <small className="tw-mt-1 tw-flex tw-items-center tw-gap-1 tw-text-[12px] tw-font-bold tw-text-[#DC2626]">
                <i className="fas fa-arrow-up" /> +11 <span className="tw-font-medium">trường hợp mới</span>
              </small>
            </span>
          </div>
        </section>

        <section className="tw-grid tw-gap-3">
          <SearchInput
            aria-label="Tìm kiếm ví"
            placeholder="Tìm kiếm theo mã ví, tên chủ sở hữu..."
            value={keyword}
            onChange={setKeyword}
          />
          <div className="tw-grid tw-grid-cols-3 tw-gap-4 max-[900px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-1.5 tw-text-[13px] tw-font-bold tw-text-[#334155]">
              Loại chủ sở hữu
              <SelectMenu
                ariaLabel="Loại chủ sở hữu"
                value={kindFilter}
                onChange={setKindFilter}
                options={[
                  { label: "Tất cả", value: "all" },
                  { label: "Khách hàng", value: "CUSTOMER" },
                  { label: "Đối tác", value: "PARTNER" },
                ]}
              />
            </label>
            <label className="tw-grid tw-gap-1.5 tw-text-[13px] tw-font-bold tw-text-[#334155]">
              Trạng thái
              <SelectMenu
                ariaLabel="Trạng thái ví"
                value={statusFilter}
                onChange={setStatusFilter}
                options={[
                  { label: "Tất cả", value: "all" },
                  { label: "Đang hoạt động", value: "active" },
                  { label: "Tạm khóa", value: "locked" },
                ]}
              />
            </label>
            <label className="tw-grid tw-gap-1.5 tw-text-[13px] tw-font-bold tw-text-[#334155]">
              Khoảng thời gian
              <DateRangeInput ariaLabel="Khoảng thời gian" value={dateRange} onChange={setDateRange} />
            </label>
          </div>
        </section>

        <section className="tw-overflow-x-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7]">
          <table className="tw-w-full tw-min-w-[920px] tw-border-collapse tw-text-[13px]">
            <thead>
              <tr className="tw-bg-[#F1F5FB] tw-text-[#334155]">
                <th className="tw-px-4 tw-py-3 tw-text-left tw-text-[12px] tw-font-extrabold">Mã ví</th>
                <th className="tw-px-4 tw-py-3 tw-text-left tw-text-[12px] tw-font-extrabold">Chủ sở hữu</th>
                <th className="tw-px-4 tw-py-3 tw-text-left tw-text-[12px] tw-font-extrabold">Loại ví</th>
                <th className="tw-px-4 tw-py-3 tw-text-right tw-text-[12px] tw-font-extrabold">Số dư khả dụng</th>
                <th className="tw-px-4 tw-py-3 tw-text-right tw-text-[12px] tw-font-extrabold">Đang giữ</th>
                <th className="tw-px-4 tw-py-3 tw-text-center tw-text-[12px] tw-font-extrabold">Trạng thái</th>
                <th className="tw-w-[52px] tw-px-2 tw-py-3" />
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr
                  key={row.id}
                  onClick={() => openDetail(row)}
                  className={cn(
                    "tw-cursor-pointer tw-border-0 tw-border-t tw-border-solid tw-border-[#EDF1F7] tw-transition hover:tw-bg-[#F8FAFF]",
                    row.id === selectedId ? "tw-bg-[#DBEAFE]" : "tw-bg-white",
                  )}
                >
                  <td className="tw-px-4 tw-py-3 tw-font-semibold tw-text-[#334155]">{row.code}</td>
                  <td className="tw-px-4 tw-py-3 tw-font-medium tw-text-[#334155]">{row.owner}</td>
                  <td className="tw-px-4 tw-py-3 tw-font-medium tw-text-[#334155]">{row.kindLabel}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-right tw-font-semibold tw-text-[#0F172A]">{formatNum(row.available)}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-right tw-font-semibold tw-text-[#0F172A]">{formatNum(row.held)}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-center"><StatusPill locked={row.locked} /></td>
                  <td className="tw-px-2 tw-py-3 tw-text-center">
                    <span className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-sm tw-bg-[#EFF6FF] tw-text-[14px] tw-text-[#2563EB]">
                      <i className="fas fa-ellipsis-v" />
                    </span>
                  </td>
                </tr>
              ))}
              {rows.length === 0 && (
                <tr>
                  <td colSpan={7} className="tw-px-4 tw-py-8 tw-text-center tw-font-semibold tw-text-[#64748B]">
                    Không tìm thấy ví phù hợp
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
      </div>

      <Drawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        title={selected ? `${selected.code} - ${selected.owner}` : "Chi tiết ví"}
        width="xl"
      >
        {selected && (
          <div className="tw-grid tw-gap-4">
            <div className="tw-flex tw-gap-6 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100">
              {detailTabs.map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => setDetailTab(tab.key)}
                  className={cn(
                    "tw-border-0 tw-border-b-2 tw-border-solid tw-bg-transparent tw-px-1 tw-pb-2 tw-text-[13px] tw-font-extrabold tw-transition",
                    detailTab === tab.key
                      ? "tw-border-vm-primary tw-text-vm-primary"
                      : "tw-border-transparent tw-text-[#64748B] hover:tw-text-vm-primary",
                  )}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {detailTab === "overview" && (
              <>
                <div className="tw-grid tw-grid-cols-3 tw-gap-4 max-[640px]:tw-grid-cols-1">
                  <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7] tw-p-4">
                    <h4 className="tw-m-0 tw-text-[13px] tw-font-extrabold tw-text-[#0F172A]">Thông tin ví</h4>
                    <dl className="tw-mb-0 tw-mt-3 tw-grid tw-gap-2 tw-text-[12px]">
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Mã ví</dt>
                        <dd className="tw-m-0 tw-font-bold tw-text-[#0F172A]">{selected.code}</dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Loại chủ sở hữu</dt>
                        <dd className="tw-m-0 tw-font-bold tw-text-[#0F172A]">{selected.kindLabel}</dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">{selected.kind === "PARTNER" ? "Tên đối tác" : "Tên khách hàng"}</dt>
                        <dd className="tw-m-0 tw-text-right tw-font-bold tw-text-[#0F172A]">{selected.owner}</dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Trạng thái</dt>
                        <dd className="tw-m-0"><StatusPill locked={selected.locked} /></dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Ngày tạo</dt>
                        <dd className="tw-m-0 tw-font-bold tw-text-[#0F172A]">{selected.createdAt}</dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Hoạt động gần nhất</dt>
                        <dd className="tw-m-0 tw-font-bold tw-text-[#0F172A]">{selected.lastActive}</dd>
                      </div>
                    </dl>
                  </div>
                  <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7] tw-p-4">
                    <h4 className="tw-m-0 tw-text-[13px] tw-font-extrabold tw-text-[#0F172A]">Số dư (VND)</h4>
                    <dl className="tw-mb-0 tw-mt-3 tw-grid tw-gap-3 tw-text-[12px]">
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Khả dụng</dt>
                        <dd className="tw-m-0 tw-font-black tw-text-[#0F172A]">{formatNum(selected.available)}</dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3">
                        <dt className="tw-text-[#64748B]">Đang giữ</dt>
                        <dd className="tw-m-0 tw-font-black tw-text-[#0F172A]">{formatNum(selected.held)}</dd>
                      </div>
                      <div className="tw-flex tw-justify-between tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-[#EDF1F7] tw-pt-3">
                        <dt className="tw-text-[#64748B]">Tổng</dt>
                        <dd className="tw-m-0 tw-font-black tw-text-[#0F172A]">{formatNum(selected.available + selected.held)}</dd>
                      </div>
                    </dl>
                  </div>
                  <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7] tw-p-4">
                    <h4 className="tw-m-0 tw-text-[13px] tw-font-extrabold tw-text-[#0F172A]">Thao tác quản trị</h4>
                    <div className="tw-mt-3 tw-grid tw-gap-2">
                      {(selected.locked ? canUnlock : canLock) && (
                        <button
                          type="button"
                          onClick={handleLockToggle}
                          className="tw-inline-flex tw-min-h-[40px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#FCA5A5] tw-bg-white tw-px-3 tw-text-[13px] tw-font-extrabold tw-text-[#DC2626] tw-transition hover:tw-bg-[#FEF2F2]"
                        >
                          <i className="fas fa-lock" /> {selected.locked ? "Mở khóa ví" : "Khóa ví"}
                        </button>
                      )}
                      {canAdjust && (
                        <button
                          type="button"
                          onClick={() => { setAdjustError(""); setAdjustModalOpen(true); }}
                          className="tw-inline-flex tw-min-h-[40px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#93C5FD] tw-bg-white tw-px-3 tw-text-[13px] tw-font-extrabold tw-text-[#2563EB] tw-transition hover:tw-bg-[#EFF6FF]"
                        >
                          <i className="fas fa-exchange-alt" /> Điều chỉnh số dư
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setDetailTab("ledger")}
                        className="tw-inline-flex tw-min-h-[40px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#93C5FD] tw-bg-white tw-px-3 tw-text-[13px] tw-font-extrabold tw-text-[#2563EB] tw-transition hover:tw-bg-[#EFF6FF]"
                      >
                        <i className="far fa-bookmark" /> Xem sổ cái
                      </button>
                    </div>
                    {actionError ? <p className="tw-mb-0 tw-mt-2 tw-text-[12px] tw-font-bold tw-text-red-600">{actionError}</p> : null}
                  </div>
                </div>

                <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                  <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7] tw-p-4">
                    <h4 className="tw-m-0 tw-text-[13px] tw-font-extrabold tw-text-[#0F172A]">Quy trình phê duyệt (Maker - Checker)</h4>
                    <ol className="tw-mb-0 tw-mt-4 tw-grid tw-list-none tw-grid-cols-3 tw-gap-2 tw-p-0 tw-text-center">
                      <li>
                        <span className="tw-mx-auto tw-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-bg-[#2563EB] tw-text-[14px] tw-text-white">
                          <i className="fas fa-check" />
                        </span>
                        <strong className="tw-mt-2 tw-block tw-text-[12px] tw-font-extrabold tw-text-[#0F172A]">Tạo yêu cầu</strong>
                        <span className="tw-block tw-text-[11px] tw-text-[#64748B]">Nguyễn Văn A</span>
                        <span className="tw-block tw-text-[11px] tw-text-[#64748B]">12/04/2025 09:15</span>
                      </li>
                      <li>
                        <span className="tw-mx-auto tw-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-bg-[#2563EB] tw-text-[14px] tw-text-white">
                          <i className="fas fa-check" />
                        </span>
                        <strong className="tw-mt-2 tw-block tw-text-[12px] tw-font-extrabold tw-text-[#0F172A]">Phê duyệt</strong>
                        <span className="tw-block tw-text-[11px] tw-text-[#64748B]">Trần Thị B</span>
                        <span className="tw-block tw-text-[11px] tw-text-[#64748B]">12/04/2025 10:20</span>
                      </li>
                      <li>
                        <span className="tw-mx-auto tw-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-border-2 tw-border-solid tw-border-[#CBD5E1] tw-text-[14px] tw-text-[#94A3B8]">
                          <i className="fas fa-check" />
                        </span>
                        <strong className="tw-mt-2 tw-block tw-text-[12px] tw-font-extrabold tw-text-[#0F172A]">Hoàn tất</strong>
                        <span className="tw-block tw-text-[11px] tw-text-[#64748B]">Hệ thống</span>
                        <span className="tw-block tw-text-[11px] tw-text-[#64748B]">12/04/2025 10:21</span>
                      </li>
                    </ol>
                  </div>
                  <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7] tw-p-4">
                    <div className="tw-flex tw-items-center tw-justify-between">
                      <h4 className="tw-m-0 tw-text-[13px] tw-font-extrabold tw-text-[#0F172A]">Lịch sử xử lý</h4>
                      <button type="button" className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[12px] tw-font-bold tw-text-vm-primary hover:tw-underline">
                        Xem tất cả
                      </button>
                    </div>
                    <ul className="tw-mb-0 tw-mt-3 tw-grid tw-list-none tw-gap-3 tw-p-0">
                      {[
                        { time: "12/04/2025 14:32", title: "Điều chỉnh số dư +500.000 đ", by: "Bởi Nguyễn Văn A" },
                        { time: "10/04/2025 09:15", title: "Gỡ cảnh báo rủi ro", by: "Bởi Trần Thị B" },
                        { time: "08/04/2025 16:20", title: "Tạo cảnh báo rủi ro", by: "Bởi Hệ thống (phát hiện giao dịch bất thường)" },
                      ].map((item) => (
                        <li key={item.time} className="tw-flex tw-gap-3">
                          <span className="tw-flex tw-flex-col tw-items-center">
                            <span className="tw-h-2.5 tw-w-2.5 tw-rounded-full tw-bg-[#2563EB]" />
                            <span className="tw-w-px tw-flex-1 tw-bg-[#DBEAFE]" />
                          </span>
                          <span className="tw-text-[12px]">
                            <span className="tw-font-bold tw-text-[#334155]">{item.time}</span>
                            <strong className="tw-ml-3 tw-font-extrabold tw-text-[#0F172A]">{item.title}</strong>
                            <span className="tw-block tw-text-[#64748B]">{item.by}</span>
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              </>
            )}

            {detailTab === "transactions" && (
              <ul className="tw-mb-0 tw-grid tw-list-none tw-gap-2 tw-p-0">
                {(transactions.length > 0
                  ? transactions.map((tx) => ({
                      id: tx.financialTransactionId,
                      code: tx.transactionCode,
                      title: tx.transactionType,
                      time: tx.occurredAt,
                      status: tx.status,
                    }))
                  : [
                      { id: "t1", code: "TXN-88412", title: "Nạp tiền qua VNPAY", time: "12/04/2025 14:32", status: "POSTED" },
                      { id: "t2", code: "TXN-87901", title: "Thanh toán hóa đơn", time: "11/04/2025 09:05", status: "POSTED" },
                      { id: "t3", code: "TXN-87110", title: "Đối soát doanh thu", time: "10/04/2025 16:44", status: "POSTED" },
                    ]
                ).map((tx) => (
                  <li key={tx.id} className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7] tw-px-4 tw-py-3">
                    <span>
                      <strong className="tw-block tw-text-[13px] tw-font-extrabold tw-text-[#0F172A]">{tx.code} · {tx.title}</strong>
                      <span className="tw-block tw-text-[12px] tw-text-[#64748B]">{tx.time}</span>
                    </span>
                    <span className="tw-rounded-full tw-bg-[#DCFCE7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#15803D]">{tx.status}</span>
                  </li>
                ))}
              </ul>
            )}

            {detailTab === "ledger" && (
              <div className="tw-overflow-x-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7]">
                <table className="tw-w-full tw-min-w-[520px] tw-border-collapse tw-text-[12px]">
                  <thead>
                    <tr className="tw-bg-[#F1F5FB] tw-text-[#334155]">
                      <th className="tw-px-4 tw-py-3 tw-text-left tw-font-extrabold">Mã bút toán</th>
                      <th className="tw-px-4 tw-py-3 tw-text-left tw-font-extrabold">Nội dung</th>
                      <th className="tw-px-4 tw-py-3 tw-text-right tw-font-extrabold">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(transactions.length > 0
                      ? transactions.map((tx) => ({ id: tx.financialTransactionId, code: tx.transactionCode, title: tx.transactionType, status: tx.status }))
                      : [{ id: "t1", code: "TXN-88412", title: "Nạp tiền qua VNPAY", status: "POSTED" }]
                    ).map((tx) => (
                      <tr key={tx.id} className="tw-border-0 tw-border-t tw-border-solid tw-border-[#EDF1F7]">
                        <td className="tw-px-4 tw-py-3 tw-font-bold tw-text-[#0F172A]">{tx.code}</td>
                        <td className="tw-px-4 tw-py-3 tw-text-[#334155]">{tx.title}</td>
                        <td className="tw-px-4 tw-py-3 tw-text-right tw-font-bold tw-text-[#15803D]">{tx.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {detailTab === "risks" && (
              <ul className="tw-mb-0 tw-grid tw-list-none tw-gap-2 tw-p-0">
                <li className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-[#FECACA] tw-bg-[#FEF2F2] tw-px-4 tw-py-3">
                  <i className="fas fa-exclamation-triangle tw-text-[#DC2626]" />
                  <span className="tw-text-[13px]">
                    <strong className="tw-font-extrabold tw-text-[#0F172A]">Giao dịch bất thường</strong>
                    <span className="tw-block tw-text-[#64748B]">Phát hiện bởi hệ thống — cần rà soát trước khi phê duyệt payout.</span>
                  </span>
                </li>
              </ul>
            )}
          </div>
        )}
      </Drawer>

      <Modal
        open={adjustModalOpen}
        onClose={() => (adjustSubmitting ? undefined : setAdjustModalOpen(false))}
        title="Điều chỉnh số dư"
        description={selected ? `Ví ${selected.code} — yêu cầu cần người khác phê duyệt (Maker - Checker).` : undefined}
        actions={
          <div className="tw-flex tw-justify-end tw-gap-2">
            <button
              type="button"
              disabled={adjustSubmitting}
              onClick={() => setAdjustModalOpen(false)}
              className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-px-4 tw-py-2 tw-text-[13px] tw-font-bold tw-text-slate-700"
            >
              Hủy
            </button>
            <button
              type="button"
              disabled={adjustSubmitting}
              onClick={handleAdjustSubmit}
              className={cn(
                "tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-py-2 tw-text-[13px] tw-font-extrabold tw-text-white hover:tw-bg-vm-primary-hover",
                adjustSubmitting ? "tw-opacity-60" : "",
              )}
            >
              {adjustSubmitting ? "Đang gửi..." : "Gửi yêu cầu"}
            </button>
          </div>
        }
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-2 tw-text-[13px] tw-font-bold tw-text-slate-900">
            Số tiền (đ)
            <input
              value={adjustAmount}
              onChange={(event) => setAdjustAmount(event.target.value.replace(/[^0-9]/g, ""))}
              inputMode="numeric"
              placeholder="Ví dụ: 500000"
              className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-px-3 tw-text-[14px] tw-font-semibold"
            />
          </label>
          <label className="tw-grid tw-gap-2 tw-text-[13px] tw-font-bold tw-text-slate-900">
            Hướng điều chỉnh
            <SelectMenu
              ariaLabel="Hướng điều chỉnh"
              value={adjustDirection}
              onChange={(value) => setAdjustDirection(value as "CREDIT" | "DEBIT")}
              options={[
                { label: "Cộng tiền (CREDIT)", value: "CREDIT" },
                { label: "Trừ tiền (DEBIT)", value: "DEBIT" },
              ]}
            />
          </label>
          <label className="tw-grid tw-gap-2 tw-text-[13px] tw-font-bold tw-text-slate-900">
            Lý do
            <input
              value={adjustReason}
              onChange={(event) => setAdjustReason(event.target.value)}
              placeholder="Nhập lý do điều chỉnh"
              className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-px-3 tw-text-[14px] tw-font-semibold"
            />
          </label>
          {adjustError ? <p className="tw-m-0 tw-text-[13px] tw-font-bold tw-text-red-600">{adjustError}</p> : null}
        </div>
      </Modal>
    </main>
  );
}
