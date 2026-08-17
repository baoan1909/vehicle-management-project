import { useEffect, useRef, useState, type MouseEvent as ReactMouseEvent } from "react";
import { createPortal } from "react-dom";

import { CardStateBadge } from "@/features/cards/components/CardStateBadge";
import type { CardManageRecord } from "@/features/cards/components/cardManageData";
import { cn } from "@/lib/cn";
import { PaginationFooter } from "@/shared/components/ui/PaginationFooter";

export type CardTableAction = "block" | "retire" | "reclassify";

type ActionMenuPosition = {
  left: number;
  top: number;
};

const ACTION_MENU_WIDTH = 176;
const ACTION_MENU_GAP = 6;

interface CardListTableProps {
  checkedIds: string[];
  currentPage: number;
  isLoading?: boolean;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRequestAction: (row: CardManageRecord, action: CardTableAction) => void;
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
  onClick: (event: ReactMouseEvent<HTMLButtonElement>) => void;
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
  isLoading = false,
  onPageChange,
  onPageSizeChange,
  onRequestAction,
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
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [menuPosition, setMenuPosition] = useState<ActionMenuPosition | null>(null);
  const menuRootRef = useRef<HTMLDivElement | null>(null);
  const openMenuRow = openMenuId ? rows.find((row) => row.id === openMenuId) ?? null : null;

  useEffect(() => {
    if (!openMenuId) return undefined;

    const closeMenu = () => {
      setOpenMenuId(null);
      setMenuPosition(null);
    };

    const handlePointerDown = (event: globalThis.MouseEvent) => {
      if (!menuRootRef.current?.contains(event.target as Node)) {
        closeMenu();
      }
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        closeMenu();
      }
    };

    window.addEventListener("mousedown", handlePointerDown);
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("resize", closeMenu);
    window.addEventListener("scroll", closeMenu, true);
    return () => {
      window.removeEventListener("mousedown", handlePointerDown);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("resize", closeMenu);
      window.removeEventListener("scroll", closeMenu, true);
    };
  }, [openMenuId]);

  const openActionMenu = (rowId: string, trigger: HTMLButtonElement) => {
    if (openMenuId === rowId) {
      setOpenMenuId(null);
      setMenuPosition(null);
      return;
    }

    const rect = trigger.getBoundingClientRect();
    const maxLeft = window.innerWidth - ACTION_MENU_WIDTH - 8;
    const menuHeight = rows.find((row) => row.id === rowId)?.inventoryStatus === "available" ? 118 : 82;
    const shouldOpenAbove = rect.bottom + ACTION_MENU_GAP + menuHeight > window.innerHeight - 8;
    setMenuPosition({
      left: Math.max(8, Math.min(rect.right - ACTION_MENU_WIDTH, maxLeft)),
      top: shouldOpenAbove ? Math.max(8, rect.top - menuHeight - ACTION_MENU_GAP) : rect.bottom + ACTION_MENU_GAP,
    });
    setOpenMenuId(rowId);
  };

  const actionMenu = openMenuRow && menuPosition
    ? createPortal(
        <div
          className="tw-fixed tw-z-[2600] tw-w-44 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-1.5 tw-text-left tw-shadow-[0_16px_36px_rgba(15,23,42,0.16)]"
          ref={menuRootRef}
          style={{ left: menuPosition.left, top: menuPosition.top }}
        >
          {[
            { action: "block" as const, icon: openMenuRow.inventoryStatus === "blocked" ? "fas fa-unlock" : "fas fa-lock", label: openMenuRow.inventoryStatus === "blocked" ? "Mở khóa thẻ" : "Khóa thẻ" },
            { action: "retire" as const, icon: "far fa-trash-alt", label: "Ngưng sử dụng" },
            ...(openMenuRow.inventoryStatus === "available"
              ? [{ action: "reclassify" as const, icon: "fas fa-right-left", label: "Phân loại lại" }]
              : []),
          ].map((item) => (
            <button
              className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-transparent tw-px-3 tw-text-left tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary"
              key={item.action}
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                setOpenMenuId(null);
                setMenuPosition(null);
                onRequestAction(openMenuRow, item.action);
              }}
            >
              <i className={`${item.icon} tw-w-4 tw-text-center tw-text-[0.82rem]`} />
              <span>{item.label}</span>
            </button>
          ))}
        </div>,
        document.body,
      )
    : null;

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
              <th>Khách hàng</th>
              <th>Biển số</th>
              <th>Trạng thái</th>
              <th>Vé tháng</th>
              <th>Báo mất <HeaderSort /></th>
              <th className="tw-w-[96px] tw-text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td className="tw-py-10 tw-text-center tw-font-bold tw-text-vm-slate-500" colSpan={9}>
                  Đang tải danh sách thẻ...
                </td>
              </tr>
            ) : null}
            {!isLoading && rows.length === 0 ? (
              <tr>
                <td className="tw-py-10 tw-text-center tw-font-bold tw-text-vm-slate-500" colSpan={9}>
                  Không có thẻ phù hợp với bộ lọc hiện tại.
                </td>
              </tr>
            ) : null}
            {!isLoading && rows.map((row) => {
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
                  <td className="tw-w-[96px]">
                    <div className="tw-flex tw-items-center tw-justify-center tw-gap-1.5">
                      <button
                        aria-expanded={openMenuId === row.id}
                        aria-label={`Mở menu thao tác thẻ ${row.cardCode}`}
                        className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-text-[0.86rem] tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25"
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation();
                          openActionMenu(row.id, event.currentTarget);
                        }}
                      >
                        <i className="fas fa-ellipsis-v" />
                      </button>
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
      {actionMenu}
    </div>
  );
}
