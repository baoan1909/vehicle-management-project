import type { MouseEvent } from "react";
import { CardStateBadge } from "@/features/cards/components/CardStateBadge";
import type { CardManageRecord } from "@/features/cards/components/cardManageData";
import { cn } from "@/lib/cn";
import { PaginationFooter } from "@/shared/components/ui/PaginationFooter";

interface CardListTableProps {
  checkedIds: string[];
  currentPage: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onSelectRow: (id: string) => void;
  onToggleAllRows: () => void;
  onToggleRowCheck: (id: string) => void;
  pageSize: number;
  rows: CardManageRecord[];
  selectedId: string | null;
  totalRecords: number;
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

export function CardListTable({
  checkedIds,
  currentPage,
  onPageChange,
  onPageSizeChange,
  onSelectRow,
  onToggleAllRows,
  onToggleRowCheck,
  pageSize,
  rows,
  selectedId,
  totalRecords
}: CardListTableProps) {
  const startIndex = totalRecords === 0 ? 0 : (currentPage - 1) * pageSize + 1;
  const endIndex = totalRecords === 0 ? 0 : startIndex + rows.length - 1;
  const totalPages = Math.max(1, Math.ceil(totalRecords / pageSize));
  const checkedCount = rows.filter((row) => checkedIds.includes(row.id)).length;
  const allRowsChecked = rows.length > 0 && checkedCount === rows.length;
  const someRowsChecked = checkedCount > 0 && checkedCount < rows.length;

  return (
    <div className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100">
      <div className="table-responsive">
        <table className="table tw-m-0 tw-border-separate tw-border-spacing-0 [&_td]:tw-text-[0.9rem] [&_thead_th]:tw-bg-white [&_thead_th]:tw-text-[0.82rem] [&_thead_th]:tw-font-bold [&_thead_th]:tw-normal-case [&_thead_th]:tw-tracking-normal [&_thead_th]:tw-text-slate-900">
          <thead>
            <tr>
              <th className="tw-w-10 !tw-pl-[0.8rem]">
                <CheckButton checked={allRowsChecked} partial={someRowsChecked} label="Chọn tất cả dòng trong trang" onClick={() => onToggleAllRows()} />
              </th>
              <th>Mã thẻ <HeaderSort /></th>
              <th>Loại thẻ <HeaderSort /></th>
              <th>Loại xe</th>
              <th>Khách hàng</th>
              <th>Biển số</th>
              <th>Trạng thái</th>
              <th>Vé tháng</th>
              <th>Báo mất <HeaderSort /></th>
              <th>Cập nhật</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const isSelected = row.id === selectedId;
              const isChecked = checkedIds.includes(row.id);

              return (
                <tr className={isSelected ? "tw-shadow-[inset_3px_0_0_#2563eb] [&>td]:tw-bg-brand-50" : ""} key={row.id} onClick={() => onSelectRow(row.id)}>
                  <td className="tw-w-10 !tw-pl-[0.8rem]">
                    <CheckButton
                      checked={isChecked}
                      label={`Chọn dòng ${row.cardCode}`}
                      onClick={(event) => {
                        event.stopPropagation();
                        onToggleRowCheck(row.id);
                      }}
                    />
                  </td>
                  <td className="tw-font-bold tw-text-vm-primary">{row.cardCode}</td>
                  <td>{row.cardTypeLabel}</td>
                  <td>{row.vehicleType}</td>
                  <td>{row.customerName ?? "-"}</td>
                  <td>{row.licensePlate ?? "-"}</td>
                  <td>
                    <CardStateBadge kind="inventory" label={row.inventoryStatusLabel} value={row.inventoryStatus} />
                  </td>
                  <td className={row.subscriptionState !== "none" ? "tw-text-[0.88rem] tw-font-bold tw-text-green-600" : "tw-text-[0.88rem] tw-font-medium tw-text-vm-slate-700"}>
                    {row.subscriptionStateLabel}
                  </td>
                  <td>
                    {row.lostCardState === "open" ? (
                      <span className="tw-text-[0.88rem] tw-font-bold tw-text-red-500">{row.lostCardStateLabel}</span>
                    ) : (
                      <span className="tw-inline-flex tw-min-h-6 tw-items-center tw-justify-center tw-rounded-full tw-bg-slate-100 tw-px-[0.6rem] tw-py-[0.2rem] tw-text-[0.78rem] tw-font-bold tw-text-slate-600">{row.lostCardStateLabel}</span>
                    )}
                  </td>
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
        ariaLabel="Card pagination"
        className="tw-bg-white"
        currentPage={currentPage}
        endIndex={endIndex}
        onPageChange={onPageChange}
        onPageSizeChange={onPageSizeChange}
        pageSize={pageSize}
        startIndex={startIndex}
        totalPages={totalPages}
        totalRecords={totalRecords}
      />
    </div>
  );
}
