import { useEffect, useMemo, useState, type MouseEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { DateRangeInput } from "@/components/ui";
import { cn } from "@/lib/cn";
import { PaginationFooter } from "@/shared/components/ui/PaginationFooter";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

import {
  getLostCardReports,
  getLostCardReportSummary,
  type LostCardReportResponse,
  type LostCardReportSummaryResponse,
  type LostCardReportStatus,
} from "@/features/cards/api/lostCardReportsApi";

type LostCardStatus = "open" | "resolved" | "cancelled";
type LostCardContext = "visitor" | "registered";
type LostCardInvoiceStatus = "unpaid" | "paid" | "cancelled";

type LostCardReportRow = {
  id: string;
  licensePlate: string;
  context: LostCardContext;
  reporterName: string;
  reporterPhone: string;
  lostDate: string;
  lostTime: string;
  totalAmount: number;
  invoiceStatus: LostCardInvoiceStatus;
  status: LostCardStatus;
  updatedDate: string;
  updatedTime: string;
};

type LostCardSummaryMetric = {
  accent: "red" | "amber" | "green" | "blue";
  icon: string;
  label: string;
  value: string;
  hint: string;
};

function buildSummaryMetrics(summary: LostCardReportSummaryResponse | null): LostCardSummaryMetric[] {
  return [
    { accent: "red", icon: "!", label: "Phiếu đang mở", value: String(summary?.openCount ?? 0), hint: "Cần theo dõi thanh toán" },
    { accent: "amber", icon: "đ", label: "Chờ thanh toán", value: String(summary?.unpaidInvoiceCount ?? 0), hint: "Invoice UNPAID" },
    { accent: "green", icon: "✓", label: "Đã xử lý", value: String(summary?.resolvedCount ?? 0), hint: "RESOLVED trong kỳ" },
    { accent: "blue", icon: "▣", label: "Thẻ đã khóa", value: String(summary?.lostCardCount ?? 0), hint: "Card status LOST" }
  ];
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("vi-VN").format(value) + " đ";
}

function matchesSearch(row: LostCardReportRow, searchValue: string) {
  if (!searchValue.trim()) return true;

  const search = searchValue.trim().toLowerCase();
  return [row.licensePlate, row.reporterName, row.reporterPhone].some((value) => value.toLowerCase().includes(search));
}

function getContextLabel(context: LostCardContext) {
  return context === "registered" ? "Đăng ký" : "Vãng lai";
}

function getInvoiceLabel(status: LostCardInvoiceStatus) {
  switch (status) {
    case "paid":
      return "PAID";
    case "cancelled":
      return "CANCELLED";
    case "unpaid":
      return "UNPAID";
  }
}

function getStatusLabel(status: LostCardStatus) {
  switch (status) {
    case "open":
      return "OPEN";
    case "resolved":
      return "RESOLVED";
    case "cancelled":
      return "CANCELLED";
  }
}

function HeaderSort() {
  return <i className="fas fa-sort tw-ml-[0.2rem] tw-text-[0.72rem] tw-text-slate-400" aria-hidden="true" />;
}

function CheckButton({
  checked,
  label,
  onClick,
  partial
}: {
  checked: boolean;
  label: string;
  onClick: (event: MouseEvent<HTMLButtonElement>) => void;
  partial?: boolean;
}) {
  return (
    <button
      className={cn(
        "tw-inline-flex tw-h-[18px] tw-w-[18px] tw-items-center tw-justify-center tw-rounded tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-text-[0.66rem] tw-text-white",
        checked || partial ? "tw-border-vm-primary tw-bg-vm-primary" : "",
      )}
      type="button"
      aria-label={label}
      onClick={onClick}
    >
      {checked ? <i className="fas fa-check" /> : partial ? <i className="fas fa-minus" /> : null}
    </button>
  );
}

function SummaryMetricCard({ item }: { item: LostCardSummaryMetric }) {
  const accentClass = {
    red: "tw-bg-red-50 tw-text-red-500",
    amber: "tw-bg-amber-50 tw-text-amber-500",
    green: "tw-bg-emerald-50 tw-text-emerald-600",
    blue: "tw-bg-blue-50 tw-text-vm-primary"
  }[item.accent];

  return (
    <div className="tw-grid tw-min-h-[104px] tw-grid-cols-[56px_minmax(0,1fr)] tw-items-center tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-p-4 tw-shadow-[0_10px_28px_rgba(15,23,42,0.045)]">
      <div className={cn("tw-inline-flex tw-h-[52px] tw-w-[52px] tw-items-center tw-justify-center tw-rounded-vm-lg tw-text-[1.45rem] tw-font-extrabold", accentClass)}>{item.icon}</div>
      <div className="tw-min-w-0">
        <p className="tw-m-0 tw-text-[0.9rem] tw-font-extrabold tw-text-slate-900">{item.label}</p>
        <strong className="tw-mt-1 tw-block tw-text-[1.6rem] tw-font-extrabold tw-leading-none tw-text-slate-900">{item.value}</strong>
        <span className="tw-mt-2 tw-block tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-600">{item.hint}</span>
      </div>
    </div>
  );
}

function ContextPill({ context }: { context: LostCardContext }) {
  return (
    <span className="tw-inline-flex tw-min-h-6 tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-px-[0.65rem] tw-py-[0.2rem] tw-text-[0.78rem] tw-font-extrabold tw-text-vm-primary">
      {getContextLabel(context)}
    </span>
  );
}

function StatusBadge({ status }: { status: LostCardStatus }) {
  const className = {
    open: "tw-bg-red-500/10 tw-text-red-500",
    resolved: "tw-bg-green-500/10 tw-text-green-600",
    cancelled: "tw-bg-slate-400/20 tw-text-slate-500"
  }[status];

  return (
    <span className={cn("tw-inline-flex tw-min-h-6 tw-items-center tw-justify-center tw-gap-[0.35rem] tw-rounded-full tw-px-[0.56rem] tw-py-[0.22rem] tw-text-[0.74rem] tw-font-extrabold tw-tracking-[0.01em]", className)}>
      <span className="tw-h-1.5 tw-w-1.5 tw-rounded-full tw-bg-current" />
      <span>{getStatusLabel(status)}</span>
    </span>
  );
}

function InvoiceText({ status }: { status: LostCardInvoiceStatus }) {
  const className = {
    unpaid: "tw-text-orange-500",
    paid: "tw-text-green-600",
    cancelled: "tw-text-orange-500"
  }[status];

  return <span className={cn("tw-text-[0.88rem] tw-font-extrabold", className)}>{getInvoiceLabel(status)}</span>;
}

function toComparableDate(value: string) {
  const [day, month, year] = value.split("/").map(Number);
  if (!day || !month || !year) return "";

  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function matchesDateRange(rowDate: string, fromDate: string, toDate: string) {
  const comparableDate = toComparableDate(rowDate);

  if (fromDate && comparableDate < fromDate) return false;
  if (toDate && comparableDate > toDate) return false;

  return true;
}

function splitDateRange(value: string) {
  const [fromDate = "", toDate = ""] = value.split("|");
  return { fromDate, toDate };
}

function toStartOfDayInstant(date: string) {
  if (!date) return undefined;
  return new Date(`${date}T00:00:00+07:00`).toISOString();
}

function toEndOfDayInstant(date: string) {
  if (!date) return undefined;
  return new Date(`${date}T23:59:59.999+07:00`).toISOString();
}

function getDateTimeParts(value: string) {
  if (!value) {
    return { date: "", time: "" };
  }

  const vietnamDateTime = value.match(/^(\d{2}):(\d{2}) (\d{2})-(\d{2})-(\d{4})$/);
  if (vietnamDateTime) {
    const [, hour, minute, day, month, year] = vietnamDateTime;
    return {
      date: `${day}/${month}/${year}`,
      time: `${hour}:${minute}`,
    };
  }

  const isoDateTime = value.match(/^(\d{4})-(\d{2})-(\d{2})T?(\d{2})?:?(\d{2})?/);
  if (isoDateTime) {
    const [, year, month, day, hour = "", minute = ""] = isoDateTime;
    return {
      date: `${day}/${month}/${year}`,
      time: hour && minute ? `${hour}:${minute}` : "",
    };
  }

  return { date: value, time: "" };
}

function mapLostCardReportToRow(item: LostCardReportResponse): LostCardReportRow {
  const lostDateTime = getDateTimeParts(item.timeOfLost);
  const updatedDateTime = getDateTimeParts(item.updatedAt || item.createdAt);

  return {
    id: item.lostCardReportId,
    licensePlate: item.licensePlate || "-",
    context: item.context === "VISITOR_IN_PARKING" ? "visitor" : "registered",
    reporterName: item.reporterName,
    reporterPhone: item.reporterPhone,
    lostDate: lostDateTime.date,
    lostTime: lostDateTime.time,
    totalAmount: Number(item.totalAmount ?? 0),
    invoiceStatus: (item.invoiceStatus?.toLowerCase() as LostCardInvoiceStatus | undefined) ?? "unpaid",
    status: item.status.toLowerCase() as LostCardStatus,
    updatedDate: updatedDateTime.date,
    updatedTime: updatedDateTime.time,
  };
}

const getDetailPaymentState = (row: LostCardReportRow) => {
  if (row.status === "resolved") return "resolved";
  if (row.invoiceStatus === "paid") return "paid";
  return "unpaid";
};

export function LostCardListPage() {
  const [searchValue, setSearchValue] = useState("");
  const [statusValue, setStatusValue] = useState("all");
  const [dateRange, setDateRange] = useState("");
  const [reports, setReports] = useState<LostCardReportResponse[]>([]);
  const [summary, setSummary] = useState<LostCardReportSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const navigate = useNavigate();
  const [checkedIds, setCheckedIds] = useState<string[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  const openLostCardDetail = (row: LostCardReportRow) => {
    navigate(`/admin/lost/detail?reportId=${row.id}&payment=${getDetailPaymentState(row)}`);
  };

  const rows = useMemo(() => reports.map(mapLostCardReportToRow), [reports]);
  const summaryMetrics = useMemo(() => buildSummaryMetrics(summary), [summary]);
  const { fromDate, toDate } = useMemo(() => splitDateRange(dateRange), [dateRange]);

  const filteredRecords = useMemo(
    () =>
      rows.filter((row) => {
        const matchesStatus = statusValue === "all" ? true : row.status === statusValue;
        const matchesDate = matchesDateRange(row.lostDate, fromDate, toDate);

        return matchesStatus && matchesSearch(row, searchValue) && matchesDate;
      }),
    [rows, searchValue, statusValue, fromDate, toDate],
  );

  const totalPages = Math.max(1, Math.ceil(filteredRecords.length / pageSize));
  const safeCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (safeCurrentPage - 1) * pageSize;
  const pagedRecords = filteredRecords.slice(startIndex, startIndex + pageSize);
  const checkedCount = pagedRecords.filter((row) => checkedIds.includes(row.id)).length;
  const allRowsChecked = pagedRecords.length > 0 && checkedCount === pagedRecords.length;
  const someRowsChecked = checkedCount > 0 && checkedCount < pagedRecords.length;
  const effectiveSelectedId = filteredRecords.some((row) => row.id === selectedId) ? selectedId : null;
  const pageStartIndex = filteredRecords.length === 0 ? 0 : startIndex + 1;
  const pageEndIndex = filteredRecords.length === 0 ? 0 : pageStartIndex + pagedRecords.length - 1;

  useEffect(() => {
    if (currentPage !== safeCurrentPage) {
      setCurrentPage(safeCurrentPage);
    }
  }, [currentPage, safeCurrentPage]);

  useEffect(() => {
    setCheckedIds((prev) => {
      const next = prev.filter((id) => filteredRecords.some((row) => row.id === id));
      return next.length === prev.length ? prev : next;
    });
  }, [filteredRecords]);

  useEffect(() => {
    let cancelled = false;

    async function fetchReports() {
      setIsLoading(true);
      setErrorMessage("");

      try {
        const filter = {
          status: statusValue === "all" ? undefined : (statusValue.toUpperCase() as LostCardReportStatus),
          keyword: searchValue || undefined,
          fromDate: toStartOfDayInstant(fromDate),
          toDate: toEndOfDayInstant(toDate),
        };

        const [reportsResponse, summaryResponse] = await Promise.all([
          getLostCardReports(filter),
          getLostCardReportSummary({
            fromDate: filter.fromDate,
            toDate: filter.toDate,
          }),
        ]);

        if (!cancelled) {
          setReports(reportsResponse.data);
          setSummary(summaryResponse.data);
        }
      } catch (error) {
        if (!cancelled) {
          setErrorMessage(error instanceof Error ? error.message : "Không tải được danh sách phiếu mất thẻ");
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    fetchReports();

    return () => {
      cancelled = true;
    };
  }, [searchValue, statusValue, fromDate, toDate]);

  const toggleRowCheck = (id: string) => {
    setCheckedIds((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]));
  };

  const toggleAllVisibleRows = () => {
    const visibleIds = pagedRecords.map((row) => row.id);
    setCheckedIds(allRowsChecked ? [] : visibleIds);
  };

  const resetFilters = () => {
    setSearchValue("");
    setStatusValue("all");
    setDateRange("");
    setCurrentPage(1);
  };

  return (
    <div className="content-header tw-px-0 tw-pb-4 tw-pt-3">
      <section className="content tw-pb-8">
        <div className="container-fluid tw-max-w-[1480px]">
          <div className="tw-flex tw-flex-col tw-gap-[1.1rem]">
            <div className="tw-flex tw-flex-wrap tw-items-start tw-justify-between tw-gap-4">
              <div>
                <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-extrabold tw-leading-tight tw-text-slate-900">Quản lý thẻ bị mất</h1>
                <p className="tw-mb-0 tw-mt-2 tw-text-[0.92rem] tw-font-semibold tw-text-vm-slate-600">
                  Theo dõi phiếu báo mất thẻ, trạng thái thanh toán và xử lý sau khi khách hoàn tất thanh toán.
                </p>
              </div>

              <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-3">
                <button
                  className="tw-inline-flex tw-min-h-11 tw-items-center tw-gap-[0.7rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-shadow-[0_8px_20px_rgba(15,23,42,0.04)] tw-transition-colors hover:tw-bg-vm-slate-25"
                  type="button"
                >
                  <i className="fas fa-download" />
                  <span>Xuất dữ liệu</span>
                </button>

                <Link
                  className="tw-inline-flex tw-min-h-11 tw-items-center tw-gap-[0.7rem] tw-rounded-vm-lg tw-bg-vm-primary tw-px-4 tw-text-[0.92rem] tw-font-extrabold tw-text-white tw-shadow-[0_12px_22px_rgba(37,99,235,0.2)] tw-transition-colors hover:tw-bg-brand-700 hover:tw-text-white"
                  to="/admin/lost/form"
                >
                  <i className="fas fa-plus" />
                  <span>Tạo phiếu báo mất</span>
                </Link>
              </div>
            </div>

            <div className="tw-grid tw-grid-cols-4 tw-gap-[0.9rem] max-[1360px]:tw-grid-cols-2 max-[900px]:tw-grid-cols-1">
              {summaryMetrics.map((item) => (
                <SummaryMetricCard item={item} key={item.label} />
              ))}
            </div>

            <section className="tw-min-w-0 tw-overflow-hidden tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200/95 tw-bg-white tw-shadow-[0_14px_36px_rgba(15,23,42,0.05)]">
              <div className="tw-grid tw-grid-cols-[minmax(260px,1fr)_170px_250px_auto] tw-items-center tw-gap-3 tw-p-[1.1rem] max-[1200px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
                <label className="tw-m-0 tw-flex tw-min-h-10 tw-min-w-0 tw-items-center tw-gap-[0.7rem] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-[0.95rem] tw-text-vm-slate-500">
                  <i className="fas fa-search" />
                  <input
                    className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.94rem] tw-text-[#111827] tw-outline-none placeholder:tw-text-vm-slate-500"
                    onChange={(event) => {
                      setSearchValue(event.target.value);
                      setCurrentPage(1);
                    }}
                    placeholder="Tìm theo biển số, người báo, SĐT..."
                    type="search"
                    value={searchValue}
                  />
                </label>

                <SelectMenu
                  ariaLabel="Trạng thái"
                  className="tw-w-full"
                  value={statusValue}
                  onChange={(value) => {
                    setStatusValue(value);
                    setCurrentPage(1);
                  }}
                  options={[
                    { label: "Tất cả", value: "all" },
                    { label: "OPEN", value: "open" },
                    { label: "RESOLVED", value: "resolved" },
                    { label: "CANCELLED", value: "cancelled" }
                  ]}
                />

                <DateRangeInput
                  ariaLabel="Khoảng ngày báo mất"
                  value={dateRange}
                  onChange={(value) => {
                    setDateRange(value);
                    setCurrentPage(1);
                  }}
                />

                <button
                  className="tw-inline-flex tw-min-h-10 tw-items-center tw-justify-center tw-gap-[0.55rem] tw-whitespace-nowrap tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-4 tw-py-[0.7rem] tw-text-[0.92rem] tw-font-bold tw-text-vm-slate-700 tw-transition-colors hover:tw-bg-vm-slate-25 max-[1200px]:tw-w-full"
                  onClick={resetFilters}
                  type="button"
                >
                  <i className="fas fa-redo-alt" />
                  <span>Xóa lọc</span>
                </button>
              </div>

              {errorMessage ? (
                <div className="tw-mx-[1.1rem] tw-mb-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-4 tw-py-3 tw-text-[0.9rem] tw-font-semibold tw-text-red-600">
                  {errorMessage}
                </div>
              ) : null}

              <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100">
                <div className="table-responsive">
                  <table className="table tw-m-0 tw-border-separate tw-border-spacing-0 [&_td]:tw-text-[0.9rem] [&_thead_th]:tw-bg-white [&_thead_th]:tw-text-[0.82rem] [&_thead_th]:tw-font-bold [&_thead_th]:tw-normal-case [&_thead_th]:tw-tracking-normal [&_thead_th]:tw-text-slate-900">
                    <thead>
                      <tr>
                        <th className="tw-w-10 !tw-pl-[0.8rem]">
                          <CheckButton checked={allRowsChecked} partial={someRowsChecked} label="Chọn tất cả dòng trong trang" onClick={() => toggleAllVisibleRows()} />
                        </th>
                        <th>Biển số <HeaderSort /></th>
                        <th>Loại khách</th>
                        <th>Người báo</th>
                        <th>Thời gian mất</th>
                        <th>Tổng tiền</th>
                        <th>Invoice</th>
                        <th>Trạng thái</th>
                        <th>Cập nhật</th>
                      </tr>
                    </thead>
                    <tbody>
                      {isLoading ? (
                        <tr>
                          <td className="tw-py-8 tw-text-center tw-font-semibold tw-text-vm-slate-500" colSpan={9}>
                            Đang tải danh sách phiếu mất thẻ...
                          </td>
                        </tr>
                      ) : null}

                      {!isLoading && pagedRecords.length === 0 ? (
                        <tr>
                          <td className="tw-py-8 tw-text-center tw-font-semibold tw-text-vm-slate-500" colSpan={9}>
                            Không có phiếu mất thẻ phù hợp.
                          </td>
                        </tr>
                      ) : null}

                      {!isLoading && pagedRecords.map((row) => {
                        const isSelected = row.id === effectiveSelectedId;
                        const isChecked = checkedIds.includes(row.id);

                        return (
                          <tr
                            className={cn(
                              "tw-cursor-pointer",
                              isSelected ? "tw-shadow-[inset_3px_0_0_#2563eb] [&>td]:tw-bg-brand-50" : "",
                            )}
                            key={row.id}
                            onClick={() => openLostCardDetail(row)}
                          >
                            <td className="tw-w-10 !tw-pl-[0.8rem]">
                              <CheckButton
                                checked={isChecked}
                                label={`Chọn dòng ${row.licensePlate}`}
                                onClick={(event) => {
                                  event.stopPropagation();
                                  toggleRowCheck(row.id);
                                }}
                              />
                            </td>
                            <td>
                              <Link
                                className="tw-font-bold tw-text-vm-primary hover:tw-text-brand-700 hover:tw-no-underline"
                                to={`/admin/lost/detail?reportId=${row.id}&payment=${getDetailPaymentState(row)}`}
                                onClick={(event) => event.stopPropagation()}
                              >
                                {row.licensePlate}
                              </Link>
                            </td>
                            <td><ContextPill context={row.context} /></td>
                            <td>
                              <div className="tw-grid tw-gap-[0.15rem]">
                                <span className="tw-text-[0.88rem] tw-font-medium tw-text-slate-900">{row.reporterName}</span>
                                <span className="tw-text-[0.78rem] tw-font-medium tw-text-vm-slate-500">{row.reporterPhone}</span>
                              </div>
                            </td>
                            <td>
                              <div className="tw-grid tw-gap-[0.15rem]">
                                <span className="tw-text-[0.88rem] tw-font-medium tw-text-slate-900">{row.lostDate}</span>
                                <strong className="tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-700">{row.lostTime}</strong>
                              </div>
                            </td>
                            <td className="tw-font-bold tw-text-red-500">{formatCurrency(row.totalAmount)}</td>
                            <td><InvoiceText status={row.invoiceStatus} /></td>
                            <td><StatusBadge status={row.status} /></td>
                            <td>
                              <div className="tw-grid tw-gap-[0.15rem]">
                                <span className="tw-text-[0.88rem] tw-text-vm-slate-700">{row.updatedDate}</span>
                                <strong className="tw-text-[0.88rem] tw-font-medium tw-text-slate-900">{row.updatedTime}</strong>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <PaginationFooter
                  ariaLabel="Lost card pagination"
                  className="tw-bg-white"
                  currentPage={safeCurrentPage}
                  endIndex={pageEndIndex}
                  onPageChange={setCurrentPage}
                  onPageSizeChange={(value) => {
                    setPageSize(value);
                    setCurrentPage(1);
                  }}
                  pageSize={pageSize}
                  startIndex={pageStartIndex}
                  totalPages={totalPages}
                  totalRecords={filteredRecords.length}
                />
              </div>
            </section>
          </div>
        </div>
      </section>
    </div>
  );
}
