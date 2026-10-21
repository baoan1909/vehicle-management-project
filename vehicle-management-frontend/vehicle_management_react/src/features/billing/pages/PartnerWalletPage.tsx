import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { Modal } from "@/shared/components/ui/Modal";
import { DateRangeInput } from "@/shared/components/ui/DateRangeInput";
import { cn } from "@/lib/cn";
import {
  getPartnerAllocations,
  getPartnerPayouts,
  getPartnerWallet,
  requestPartnerPayout,
  type PayoutRequestResponse,
  type RevenueAllocationResponse,
  type WalletResponse,
} from "@/features/billing/api/walletApi";

type AllocationRow = {
  date: string;
  txCount: number;
  lotName: string;
  gross: number;
  platformFee: number;
  voucherCommission: number;
  net: number;
  settled: boolean;
};

type PayoutRow = {
  requestedAt: string;
  amount: number;
  status: "PENDING" | "COMPLETED" | "REJECTED" | "FAILED";
};

const FALLBACK_WALLET: WalletResponse = {
  walletId: "",
  ownerType: "ORGANIZATION",
  currency: "VND",
  availableBalance: 28450000,
  pendingBalance: 12580000,
  heldBalance: 5320000,
  status: "ACTIVE",
};

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseIsoDay(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function defaultChartRange() {
  const today = new Date();
  const start = new Date(today);
  start.setDate(today.getDate() - 29);
  return `${toIsoDate(start)}|${toIsoDate(today)}`;
}

function splitDateRange(value: string) {
  const [effectiveFrom = "", effectiveTo = ""] = value.split("|");
  return { effectiveFrom, effectiveTo };
}

function eachDayIso(fromIso: string, toIso: string) {
  const days: string[] = [];
  const start = parseIsoDay(fromIso);
  const end = parseIsoDay(toIso);
  if (!start || !end || start.getTime() > end.getTime()) return days;
  const cursor = new Date(start);
  while (cursor.getTime() <= end.getTime() && days.length < 62) {
    days.push(toIsoDate(cursor));
    cursor.setDate(cursor.getDate() + 1);
  }
  return days;
}

function formatDayMonthLabel(isoDay: string) {
  const date = parseIsoDay(isoDay);
  if (!date) return "";
  return `${`${date.getDate()}`.padStart(2, "0")}/${`${date.getMonth() + 1}`.padStart(2, "0")}`;
}

// Giá trị minh họa theo ngày khi backend chưa có allocation; xác định theo ngày nên lọc khoảng nào cũng có cột.
function demoValueForDay(isoDay: string) {
  let hash = 0;
  for (const char of isoDay) hash = (hash * 31 + char.charCodeAt(0)) % 1000003;
  return 2000000 + (hash % 51) * 100000;
}

const FALLBACK_ALLOCATION_ROWS: AllocationRow[] = [
  { date: "12/04", txCount: 1245, lotName: "Bãi xe Thành Công", gross: 8750000, platformFee: 875000, voucherCommission: 437500, net: 7437500, settled: true },
  { date: "11/04", txCount: 980, lotName: "Bãi xe Thành Công", gross: 6320000, platformFee: 632000, voucherCommission: 316000, net: 5372000, settled: true },
  { date: "10/04", txCount: 1102, lotName: "Bãi xe Thành Công", gross: 7980000, platformFee: 798000, voucherCommission: 399000, net: 6783000, settled: true },
  { date: "09/04", txCount: 876, lotName: "Bãi xe Thành Công", gross: 6150000, platformFee: 615000, voucherCommission: 307500, net: 5227500, settled: true },
  { date: "08/04", txCount: 1310, lotName: "Bãi xe Thành Công", gross: 9420000, platformFee: 942000, voucherCommission: 471000, net: 8007000, settled: true },
];

const FALLBACK_PAYOUTS: PayoutRow[] = [
  { requestedAt: "10/04/2025 09:12", amount: 20000000, status: "PENDING" },
  { requestedAt: "03/04/2025 14:25", amount: 15000000, status: "COMPLETED" },
  { requestedAt: "27/03/2025 10:08", amount: 10000000, status: "COMPLETED" },
];

function formatVnd(value: number) {
  return `${new Intl.NumberFormat("vi-VN").format(Math.round(value))} đ`;
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const day = `${date.getDate()}`.padStart(2, "0");
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const year = date.getFullYear();
  const hours = `${date.getHours()}`.padStart(2, "0");
  const minutes = `${date.getMinutes()}`.padStart(2, "0");
  return `${day}/${month}/${year} ${hours}:${minutes}`;
}

function formatDayMonth(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${`${date.getDate()}`.padStart(2, "0")}/${`${date.getMonth() + 1}`.padStart(2, "0")}`;
}

function mapPayoutStatus(status: string): PayoutRow["status"] {
  if (status === "COMPLETED") return "COMPLETED";
  if (status === "REJECTED") return "REJECTED";
  if (status === "FAILED") return "FAILED";
  return "PENDING";
}

function groupAllocationsByDay(allocations: RevenueAllocationResponse[]): AllocationRow[] {
  const groups = new Map<string, AllocationRow>();
  for (const allocation of allocations) {
    const day = formatDayMonth(allocation.createdAt);
    if (!day) continue;
    const current = groups.get(day) ?? {
      date: day,
      txCount: 0,
      lotName: "Bãi xe Thành Công",
      gross: 0,
      platformFee: 0,
      voucherCommission: 0,
      net: 0,
      settled: true,
    };
    current.txCount += 1;
    current.gross += allocation.grossAmount ?? 0;
    current.platformFee += allocation.platformFeeAmount ?? 0;
    current.voucherCommission += allocation.voucherCommissionAmount ?? 0;
    current.net += allocation.partnerPayableAmount ?? 0;
    if (!allocation.settledAt) current.settled = false;
    groups.set(day, current);
  }
  return Array.from(groups.values()).reverse().slice(0, 5);
}

function toPayoutRows(payouts: PayoutRequestResponse[]): PayoutRow[] {
  return payouts.map((payout) => ({
    requestedAt: formatDateTime(payout.requestedAt),
    amount: payout.amount ?? 0,
    status: mapPayoutStatus(payout.status),
  }));
}

type ChartPoint = {
  isoDay: string;
  label: string;
  value: number;
};

function DailyRevenueChart({ points }: { points: ChartPoint[] }) {
  const viewWidth = 760;
  const chartHeight = 148;
  const axisWidth = 40;
  const bottomPad = 30;
  const plotWidth = viewWidth - axisWidth - 8;
  const maxValue = Math.max(...points.map((point) => point.value), 1);
  const yMax = Math.max(Math.ceil(maxValue / 2000000) * 2000000, 2000000);
  const slotWidth = points.length > 0 ? plotWidth / points.length : plotWidth;
  const barWidth = Math.max(Math.min(slotWidth * 0.62, 16), 3);
  const labelEvery = Math.max(Math.ceil(points.length / 10), 1);

  return (
    <div>
      <svg
        className="tw-block tw-h-auto tw-w-full"
        viewBox={`0 0 ${viewWidth} ${chartHeight + bottomPad}`}
        role="img"
        aria-label="Doanh thu theo ngày"
      >
        {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
          const tick = yMax * (1 - ratio);
          const y = chartHeight - ratio * chartHeight;
          return (
            <g key={ratio}>
              <text x="0" y={y + 4} fill="#94A3B8" fontSize="11" fontWeight="600">
                {tick === 0 ? "0" : `${Math.round(tick / 1000000)}M`}
              </text>
            </g>
          );
        })}
        {points.map((point, index) => {
          const height = Math.max((point.value / yMax) * chartHeight, 3);
          const slotX = axisWidth + index * slotWidth;
          const x = slotX + (slotWidth - barWidth) / 2;
          const y = chartHeight - height;
          return (
            <g key={point.isoDay}>
              <rect
                x={x}
                y={y}
                width={barWidth}
                height={height}
                rx={2.5}
                fill={index % 2 === 0 ? "#60A5FA" : "#2563EB"}
              />
              {index % labelEvery === 0 ? (
                <text x={slotX + slotWidth / 2} y={chartHeight + 18} fill="#64748B" fontSize="11" fontWeight="600" textAnchor="middle">
                  {point.label}
                </text>
              ) : null}
            </g>
          );
        })}
      </svg>
    </div>
  );
}

function VietcombankMark() {
  return (
    <svg width="52" height="36" viewBox="0 0 52 36" role="img" aria-label="Vietcombank">
      <path d="M4 4 L18 32 L26 32 L14 4 Z" fill="#009344" />
      <path d="M20 4 L34 32 L42 32 L30 4 Z" fill="#006B3F" opacity="0.85" />
      <path d="M34 4 L44 22 L48 22 L40 4 Z" fill="#009344" opacity="0.65" />
    </svg>
  );
}

export function PartnerWalletPage() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [allocations, setAllocations] = useState<RevenueAllocationResponse[]>([]);
  const [payouts, setPayouts] = useState<PayoutRequestResponse[]>([]);
  const [showAllPayouts, setShowAllPayouts] = useState(false);
  const [chartDateRange, setChartDateRange] = useState(() => defaultChartRange());
  const [payoutModalOpen, setPayoutModalOpen] = useState(false);
  const [bankModalOpen, setBankModalOpen] = useState(false);
  const [payoutAmount, setPayoutAmount] = useState("");
  const [bankAccountId, setBankAccountId] = useState("");
  const [payoutSubmitting, setPayoutSubmitting] = useState(false);
  const [payoutError, setPayoutError] = useState("");

  const canRead = hasAnyPermission(user, ["WALLET_READ_PARTNER"]);
  const canPayout = hasAnyPermission(user, ["WALLET_PAYOUT_CREATE_PARTNER"]);
  const canManageBank = hasAnyPermission(user, ["WALLET_BANK_ACCOUNT_MANAGE_PARTNER"]);

  useEffect(() => {
    if (!canRead) return;
    getPartnerWallet().then((res) => setWallet(res.data)).catch(() => undefined);
    getPartnerAllocations(0, 100).then((res) => setAllocations(res.data ?? [])).catch(() => undefined);
    getPartnerPayouts(0, 20).then((res) => setPayouts(res.data ?? [])).catch(() => undefined);
  }, [canRead]);

  const balances = wallet ?? FALLBACK_WALLET;
  const allocationRows = useMemo(
    () => (allocations.length > 0 ? groupAllocationsByDay(allocations) : FALLBACK_ALLOCATION_ROWS),
    [allocations],
  );
  const payoutRows = useMemo(
    () => (payouts.length > 0 ? toPayoutRows(payouts) : FALLBACK_PAYOUTS),
    [payouts],
  );
  const dailyRevenue = useMemo<ChartPoint[]>(() => {
    const { effectiveFrom, effectiveTo } = splitDateRange(chartDateRange);
    const days = eachDayIso(effectiveFrom, effectiveTo);
    if (days.length === 0) return [];
    const liveByDay = new Map<string, number>();
    for (const allocation of allocations) {
      const parsed = new Date(allocation.createdAt);
      if (Number.isNaN(parsed.getTime())) continue;
      const isoDay = toIsoDate(parsed);
      liveByDay.set(isoDay, (liveByDay.get(isoDay) ?? 0) + (allocation.grossAmount ?? 0));
    }
    const useLive = allocations.length > 0;
    return days.map((isoDay) => ({
      isoDay,
      label: formatDayMonthLabel(isoDay),
      value: useLive ? (liveByDay.get(isoDay) ?? 0) : demoValueForDay(isoDay),
    }));
  }, [allocations, chartDateRange]);
  const totalGross = useMemo(() => dailyRevenue.reduce((sum, point) => sum + point.value, 0), [dailyRevenue]);
  const visiblePayouts = showAllPayouts ? payoutRows : payoutRows.slice(0, 3);

  async function handlePayoutSubmit() {
    const amount = Number(payoutAmount.replace(/[^0-9]/g, ""));
    if (!Number.isFinite(amount) || amount < 50000) {
      setPayoutError("Số tiền rút tối thiểu là 50.000 đ");
      return;
    }
    if (amount > balances.availableBalance) {
      setPayoutError("Số tiền rút vượt quá số dư khả dụng");
      return;
    }
    if (!bankAccountId.trim()) {
      setPayoutError("Vui lòng nhập tài khoản ngân hàng đã xác minh");
      return;
    }
    setPayoutSubmitting(true);
    setPayoutError("");
    try {
      await requestPartnerPayout(bankAccountId.trim(), Math.round(amount));
      setPayoutModalOpen(false);
      setPayoutAmount("");
      const [walletRes, payoutRes] = await Promise.all([getPartnerWallet(), getPartnerPayouts(0, 20)]);
      setWallet(walletRes.data);
      setPayouts(payoutRes.data ?? []);
    } catch (error) {
      setPayoutError(error instanceof Error ? error.message : "Gửi yêu cầu rút tiền thất bại");
    } finally {
      setPayoutSubmitting(false);
    }
  }

  if (!canRead) {
    return <main className="tw-p-6">Bạn không có quyền xem ví đối tác.</main>;
  }

  return (
    <main className="tw-mx-auto tw-w-full tw-max-w-[1480px] tw-px-5 tw-py-5 max-[768px]:tw-px-4">
      <div className="tw-flex tw-flex-col tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/90 tw-bg-white tw-p-5 tw-shadow-[0_16px_34px_rgba(15,23,42,0.04)]">
      <div>
        <h1 className="tw-m-0 tw-text-[22px] tw-font-black tw-text-[#0F172A]">Ví đối tác</h1>
        <p className="tw-mb-0 tw-mt-1 tw-text-[13px] tw-font-medium tw-text-[#64748B]">
          Theo dõi doanh thu, đối soát và rút tiền về tài khoản ngân hàng
        </p>
      </div>

      <section className="tw-mt-4 tw-grid tw-grid-cols-3 tw-gap-4 max-[900px]:tw-grid-cols-1">
        <div className="tw-col-span-2 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card max-[900px]:tw-col-span-1">
          <div className="tw-flex tw-items-center tw-gap-4 max-[640px]:tw-flex-col max-[640px]:tw-items-stretch">
            <h2 className="tw-m-0 tw-shrink-0 tw-text-[15px] tw-font-extrabold tw-text-[#0F172A]">
              Doanh thu theo ngày ({dailyRevenue.length} ngày gần nhất)
            </h2>
            <div className="tw-w-[300px] tw-max-w-full">
              <DateRangeInput ariaLabel="Khoảng ngày doanh thu" value={chartDateRange} onChange={setChartDateRange} />
            </div>
            <div className="tw-ml-auto tw-shrink-0 tw-text-right max-[640px]:tw-ml-0">
              <p className="tw-m-0 tw-text-[12px] tw-font-medium tw-text-[#64748B]">Tổng doanh thu gộp</p>
              <p className="tw-m-0 tw-mt-1 tw-text-[18px] tw-font-black tw-text-[#0F172A]">
                {formatVnd(totalGross)}{" "}
                <span className="tw-text-[13px] tw-font-bold tw-text-[#16A34A]">
                  <i className="fas fa-arrow-up" /> +12%
                </span>
              </p>
            </div>
          </div>
          <div className="tw-mt-16">
            {dailyRevenue.length > 0 ? (
              <DailyRevenueChart points={dailyRevenue} />
            ) : (
              <p className="tw-m-0 tw-py-10 tw-text-center tw-text-[13px] tw-font-semibold tw-text-[#64748B]">
                Chọn khoảng ngày để xem doanh thu
              </p>
            )}
          </div>
        </div>

        <div className="tw-grid tw-gap-4">
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#FEF3C7] tw-text-[22px] tw-text-[#F59E0B]">
              <i className="fas fa-user-clock" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Chờ đối soát</span>
              <strong className="tw-mt-1 tw-block tw-text-[20px] tw-font-black tw-text-[#0F172A]">{formatVnd(balances.pendingBalance)}</strong>
            </span>
          </div>
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-flex-shrink-0 tw-text-[40px] tw-leading-none tw-text-[#16A34A]">
              <i className="fas fa-check-circle" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Khả dụng</span>
              <strong className="tw-mt-1 tw-block tw-text-[20px] tw-font-black tw-text-[#0F172A]">{formatVnd(balances.availableBalance)}</strong>
            </span>
          </div>
          <div className="tw-flex tw-items-center tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
            <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#EFF6FF] tw-text-[22px] tw-text-[#2563EB]">
              <i className="fas fa-lock" />
            </span>
            <span>
              <span className="tw-block tw-text-[13px] tw-font-semibold tw-text-[#64748B]">Đang giữ</span>
              <strong className="tw-mt-1 tw-block tw-text-[20px] tw-font-black tw-text-[#0F172A]">{formatVnd(balances.heldBalance)}</strong>
            </span>
          </div>
        </div>
      </section>

      <section className="tw-mt-4 tw-flex tw-items-center tw-justify-between tw-gap-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card max-[640px]:tw-flex-col max-[640px]:tw-items-stretch">
        <div className="tw-flex tw-items-center tw-gap-4">
          <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-[#EFF6FF] tw-text-[22px] tw-text-[#2563EB]">
            <i className="fas fa-handshake" />
          </span>
          <span>
            <strong className="tw-block tw-text-[15px] tw-font-extrabold tw-text-[#0F172A]">Yêu cầu rút tiền</strong>
            <span className="tw-mt-1 tw-block tw-text-[13px] tw-text-[#64748B]">
              Số dư khả dụng: <strong className="tw-font-extrabold tw-text-[#0F172A]">{formatVnd(balances.availableBalance)}</strong>
            </span>
          </span>
        </div>
        {canPayout ? (
          <button
            type="button"
            onClick={() => setPayoutModalOpen(true)}
            className="tw-inline-flex tw-min-h-[48px] tw-items-center tw-justify-center tw-gap-2 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-8 tw-text-[15px] tw-font-extrabold tw-text-white tw-transition hover:tw-bg-vm-primary-hover max-[640px]:tw-w-full"
          >
            <i className="fas fa-landmark" /> Yêu cầu rút tiền
          </button>
        ) : null}
      </section>

      <section className="tw-mt-4 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
        <div className="tw-flex tw-items-center tw-justify-between tw-gap-3">
          <h2 className="tw-m-0 tw-text-[15px] tw-font-extrabold tw-text-[#0F172A]">Bảng phân bổ doanh thu</h2>
          <span className="tw-inline-flex tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-px-3 tw-py-2 tw-text-[12px] tw-font-bold tw-text-[#334155]">
            <i className="far fa-calendar-alt tw-text-[#2563EB]" /> 13/03/2025 - 12/04/2025 <i className="fas fa-chevron-down tw-text-[10px] tw-text-[#64748B]" />
          </span>
        </div>
        <div className="tw-mt-3 tw-overflow-x-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7]">
          <table className="tw-w-full tw-min-w-[860px] tw-border-collapse tw-text-[13px]">
            <thead>
              <tr className="tw-bg-[#F1F5FB] tw-text-[#334155]">
                {["Ngày", "Giao dịch", "Bãi xe", "Doanh thu gộp", "Phí nền tảng", "Hoa hồng voucher", "Thực nhận", "Trạng thái"].map((header) => (
                  <th key={header} className="tw-px-4 tw-py-3 tw-text-center tw-text-[12px] tw-font-extrabold">{header}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {allocationRows.map((row) => (
                <tr key={row.date} className="tw-border-0 tw-border-t tw-border-solid tw-border-[#EDF1F7]">
                  <td className="tw-px-4 tw-py-3 tw-text-center tw-font-semibold tw-text-[#334155]">{row.date}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-center tw-font-semibold tw-text-[#334155]">{new Intl.NumberFormat("vi-VN").format(row.txCount)}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-center tw-font-medium tw-text-[#334155]">{row.lotName}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-right tw-font-semibold tw-text-[#0F172A]">{new Intl.NumberFormat("vi-VN").format(Math.round(row.gross))}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-right tw-font-semibold tw-text-[#334155]">-{new Intl.NumberFormat("vi-VN").format(Math.round(row.platformFee))}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-right tw-font-semibold tw-text-[#334155]">-{new Intl.NumberFormat("vi-VN", { minimumFractionDigits: 1 }).format(row.voucherCommission)}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-right tw-font-extrabold tw-text-[#0F172A]">{new Intl.NumberFormat("vi-VN").format(Math.round(row.net))}</td>
                  <td className="tw-px-4 tw-py-3 tw-text-center">
                    <span className="tw-inline-block tw-rounded-full tw-bg-[#DCFCE7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#15803D]">
                      {row.settled ? "Đã đối soát" : "Chờ đối soát"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-4 max-[900px]:tw-grid-cols-1">
        <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-flex tw-items-center tw-justify-between">
            <h2 className="tw-m-0 tw-text-[15px] tw-font-extrabold tw-text-[#0F172A]">Trạng thái rút tiền gần đây</h2>
            <button
              type="button"
              onClick={() => setShowAllPayouts((value) => !value)}
              className="tw-border-0 tw-bg-transparent tw-p-0 tw-text-[13px] tw-font-bold tw-text-vm-primary hover:tw-underline"
            >
              {showAllPayouts ? "Thu gọn" : "Xem tất cả"}
            </button>
          </div>
          <div className="tw-mt-3 tw-overflow-x-auto tw-rounded-vm-md tw-border tw-border-solid tw-border-[#E8EEF7]">
            <table className="tw-w-full tw-border-collapse tw-text-[13px]">
              <thead>
                <tr className="tw-bg-[#F1F5FB] tw-text-[#334155]">
                  <th className="tw-px-4 tw-py-3 tw-text-left tw-text-[12px] tw-font-extrabold">Ngày yêu cầu</th>
                  <th className="tw-px-4 tw-py-3 tw-text-left tw-text-[12px] tw-font-extrabold">Số tiền</th>
                  <th className="tw-px-4 tw-py-3 tw-text-left tw-text-[12px] tw-font-extrabold">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {visiblePayouts.map((payout) => (
                  <tr key={`${payout.requestedAt}-${payout.amount}`} className="tw-border-0 tw-border-t tw-border-solid tw-border-[#EDF1F7]">
                    <td className="tw-px-4 tw-py-3 tw-font-medium tw-text-[#334155]">{payout.requestedAt}</td>
                    <td className="tw-px-4 tw-py-3 tw-font-extrabold tw-text-[#0F172A]">{formatVnd(payout.amount)}</td>
                    <td className="tw-px-4 tw-py-3">
                      {payout.status === "PENDING" ? (
                        <span className="tw-inline-flex tw-items-center tw-gap-1 tw-rounded-full tw-bg-[#FEF3C7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#D97706]">
                          <i className="fas fa-fire" /> Đang xử lý
                        </span>
                      ) : payout.status === "COMPLETED" ? (
                        <span className="tw-inline-flex tw-items-center tw-gap-1 tw-rounded-full tw-bg-[#DCFCE7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#15803D]">
                          <i className="fas fa-check" /> Thành công
                        </span>
                      ) : (
                        <span className="tw-inline-flex tw-items-center tw-gap-1 tw-rounded-full tw-bg-[#FEE2E2] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#B91C1C]">
                          <i className="fas fa-times" /> {payout.status === "REJECTED" ? "Từ chối" : "Thất bại"}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-p-5 tw-shadow-vm-card">
          <div className="tw-flex tw-items-center tw-justify-between">
            <h2 className="tw-m-0 tw-text-[15px] tw-font-extrabold tw-text-[#0F172A]">Tài khoản ngân hàng nhận tiền</h2>
            {canManageBank ? (
              <button
                type="button"
                onClick={() => setBankModalOpen(true)}
                className="tw-inline-flex tw-items-center tw-gap-1 tw-border-0 tw-bg-transparent tw-p-0 tw-text-[13px] tw-font-bold tw-text-vm-primary hover:tw-underline"
              >
                <i className="fas fa-pen tw-text-[11px]" /> Chỉnh sửa
              </button>
            ) : null}
          </div>
          <div className="tw-mt-3 tw-flex tw-items-center tw-justify-between tw-gap-3">
            <div className="tw-flex tw-items-center tw-gap-3">
              <VietcombankMark />
              <strong className="tw-text-[15px] tw-font-extrabold tw-text-[#0F172A]">Vietcombank</strong>
            </div>
            <span className="tw-rounded-full tw-bg-[#DCFCE7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#15803D]">
              Đã xác thực
            </span>
          </div>
          <dl className="tw-mb-0 tw-mt-4 tw-grid tw-gap-3">
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
              <dt className="tw-text-[13px] tw-text-[#64748B]">Số tài khoản</dt>
              <dd className="tw-m-0 tw-text-[13px] tw-font-bold tw-text-[#0F172A]">1023 4567 8901</dd>
            </div>
            <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
              <dt className="tw-text-[13px] tw-text-[#64748B]">Chủ tài khoản</dt>
              <dd className="tw-m-0 tw-text-right tw-text-[13px] tw-font-bold tw-text-[#0F172A]">CÔNG TY TNHH THÀNH CÔNG</dd>
            </div>
          </dl>
          <p className="tw-mb-0 tw-mt-4 tw-text-[12px] tw-text-[#94A3B8]">Tài khoản này được dùng để nhận tiền rút từ CoParking</p>
        </div>
      </section>
      </div>

      <Modal
        open={payoutModalOpen}
        onClose={() => (payoutSubmitting ? undefined : setPayoutModalOpen(false))}
        title="Yêu cầu rút tiền"
        description={`Số dư khả dụng: ${formatVnd(balances.availableBalance)}. Tiền được chuyển về tài khoản ngân hàng đã xác minh.`}
        actions={
          <div className="tw-flex tw-justify-end tw-gap-2">
            <button
              type="button"
              disabled={payoutSubmitting}
              onClick={() => setPayoutModalOpen(false)}
              className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-bg-white tw-px-4 tw-py-2 tw-text-[13px] tw-font-bold tw-text-slate-700"
            >
              Hủy
            </button>
            <button
              type="button"
              disabled={payoutSubmitting}
              onClick={handlePayoutSubmit}
              className={cn(
                "tw-inline-flex tw-items-center tw-gap-2 tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-py-2 tw-text-[13px] tw-font-extrabold tw-text-white hover:tw-bg-vm-primary-hover",
                payoutSubmitting ? "tw-opacity-60" : "",
              )}
            >
              <i className="fas fa-landmark" /> {payoutSubmitting ? "Đang gửi..." : "Gửi yêu cầu"}
            </button>
          </div>
        }
      >
        <div className="tw-grid tw-gap-4">
          <label className="tw-grid tw-gap-2 tw-text-[13px] tw-font-bold tw-text-slate-900">
            Số tiền rút (đ)
            <input
              value={payoutAmount}
              onChange={(event) => setPayoutAmount(event.target.value.replace(/[^0-9]/g, ""))}
              inputMode="numeric"
              placeholder="Ví dụ: 20000000"
              className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-px-3 tw-text-[14px] tw-font-semibold"
            />
          </label>
          <label className="tw-grid tw-gap-2 tw-text-[13px] tw-font-bold tw-text-slate-900">
            Tài khoản ngân hàng đã xác minh (ID)
            <input
              value={bankAccountId}
              onChange={(event) => setBankAccountId(event.target.value)}
              placeholder="Nhập ID tài khoản ngân hàng đã xác minh"
              className="tw-h-11 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-border tw-px-3 tw-text-[14px] tw-font-semibold"
            />
          </label>
          {payoutError ? <p className="tw-m-0 tw-text-[13px] tw-font-bold tw-text-red-600">{payoutError}</p> : null}
        </div>
      </Modal>

      <Modal
        open={bankModalOpen}
        onClose={() => setBankModalOpen(false)}
        title="Tài khoản ngân hàng nhận tiền"
        description="Tài khoản đã xác minh đang nhận tiền rút từ CoParking. Mọi thay đổi cần được quản trị viên xác minh lại trước khi rút tiền."
        actions={
          <div className="tw-flex tw-justify-end">
            <button
              type="button"
              onClick={() => setBankModalOpen(false)}
              className="tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-py-2 tw-text-[13px] tw-font-extrabold tw-text-white hover:tw-bg-vm-primary-hover"
            >
              Đã hiểu
            </button>
          </div>
        }
      >
        <dl className="tw-grid tw-gap-3 tw-text-[13px]">
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
            <dt className="tw-text-[#64748B]">Ngân hàng</dt>
            <dd className="tw-m-0 tw-font-extrabold tw-text-[#0F172A]">Vietcombank</dd>
          </div>
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
            <dt className="tw-text-[#64748B]">Số tài khoản</dt>
            <dd className="tw-m-0 tw-font-extrabold tw-text-[#0F172A]">1023 4567 8901</dd>
          </div>
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
            <dt className="tw-text-[#64748B]">Chủ tài khoản</dt>
            <dd className="tw-m-0 tw-text-right tw-font-extrabold tw-text-[#0F172A]">CÔNG TY TNHH THÀNH CÔNG</dd>
          </div>
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-4">
            <dt className="tw-text-[#64748B]">Trạng thái</dt>
            <dd className="tw-m-0 tw-rounded-full tw-bg-[#DCFCE7] tw-px-3 tw-py-1 tw-text-[12px] tw-font-bold tw-text-[#15803D]">Đã xác thực</dd>
          </div>
        </dl>
      </Modal>
    </main>
  );
}
