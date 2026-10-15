import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { Button, Card, ConfirmDialog, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";
import {
  PLATFORM_FEE_MAX_TOTAL_RATE,
  activatePlatformFeeType,
  archivePlatformFeeType,
  deactivatePlatformFeeType,
  getPlatformFeeVersionHistory,
  previewPlatformFees,
  searchPlatformFeeTypes,
  type PlatformFeeTypeResponse,
  type PlatformFeeTypeStatus,
} from "@/features/billing/api/platformFeeApi";
import { PlatformFeePager } from "@/features/billing/components/PlatformFeePager";
import { PlatformFeeStatusPill } from "@/features/billing/components/PlatformFeeStatusPill";
import {
  feeBasisLabels,
  feeTypePillMeta,
  formatFeeDate,
  formatFeePercent,
  getErrorMessage,
  platformFeePermissions,
} from "@/features/billing/components/platformFeeMeta";

type ConfirmAction = {
  feeType: PlatformFeeTypeResponse;
  kind: "activate" | "archive" | "deactivate";
};

type RowEnrichment = {
  rateText: string;
  validityText: string;
};

type ListSectionProps = {
  onCreate: () => void;
  onOpenDetail: (feeTypeId: string, tab?: "history" | "info" | "versions") => void;
  onPreview: () => void;
};

const ACTION_MENU_WIDTH = 208;
const SUMMARY_PAGE_SIZE = 100;
const NOMINAL_PREVIEW_AMOUNT = 1000000;

const statusOptions = [
  { label: "Trạng thái", value: "all" },
  { label: "Bản nháp", value: "DRAFT" },
  { label: "Đang áp dụng", value: "ACTIVE" },
  { label: "Ngừng áp dụng", value: "INACTIVE" },
  { label: "Đã lưu trữ", value: "ARCHIVED" },
];

const feeBasisOptions = [
  { label: "Cơ sở tính", value: "all" },
  { label: "Giá gốc", value: "GROSS_AMOUNT" },
  { label: "Sau giảm giá", value: "AFTER_DISCOUNT" },
];

function KpiCard({ icon, iconClassName, label, value }: { icon: string; iconClassName: string; label: string; value: string }) {
  return (
    <Card className="tw-flex tw-items-center tw-gap-3.5 tw-p-4">
      <span className={cn("tw-flex tw-h-12 tw-w-12 tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-md tw-text-[1.2rem]", iconClassName)}>
        <i className={icon} aria-hidden="true" />
      </span>
      <span className="tw-grid tw-min-w-0 tw-gap-1">
        <span className="tw-truncate tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">{label}</span>
        <strong className="tw-text-[1.6rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
      </span>
    </Card>
  );
}

export function PlatformFeeListSection({ onCreate, onOpenDetail, onPreview }: ListSectionProps) {
  const toast = useToast();
  const { user } = useAuth();

  const canCreate = hasAnyPermission(user, platformFeePermissions.create);
  const canUpdate = hasAnyPermission(user, platformFeePermissions.update);
  const canActivate = hasAnyPermission(user, platformFeePermissions.activate);
  const canArchive = hasAnyPermission(user, platformFeePermissions.archive);
  const canPreview = hasAnyPermission(user, platformFeePermissions.preview);

  const [items, setItems] = useState<PlatformFeeTypeResponse[]>([]);
  const [status, setStatus] = useState("all");
  const [feeBasis, setFeeBasis] = useState("all");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [totalRecords, setTotalRecords] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [enrichment, setEnrichment] = useState<Record<string, RowEnrichment>>({});
  const [activeCount, setActiveCount] = useState(0);
  const [draftCount, setDraftCount] = useState(0);
  const [totalRate, setTotalRate] = useState<number | null>(null);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [menuPosition, setMenuPosition] = useState<{ left: number; top: number } | null>(null);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction | null>(null);
  const [confirming, setConfirming] = useState(false);
  const menuRootRef = useRef<HTMLDivElement | null>(null);

  const pageSize = 10;

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setKeyword(keywordInput.trim());
      setPage(1);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [keywordInput]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    searchPlatformFeeTypes({
      feeBasis: feeBasis === "all" ? undefined : (feeBasis as "AFTER_DISCOUNT" | "GROSS_AMOUNT"),
      keyword: keyword || undefined,
      page: page - 1,
      size: pageSize,
      status: status === "all" ? undefined : (status as PlatformFeeTypeStatus),
      transactionType: "SUBSCRIPTION_PURCHASE",
    })
      .then((response) => {
        if (cancelled) return;
        setItems(response.data.items);
        setTotalRecords(response.data.totalElements);
        setTotalPages(Math.max(1, response.data.totalPages));
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        toast.error(getErrorMessage(error, "Không thể tải danh sách phí sàn."), "Tải dữ liệu thất bại");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [feeBasis, keyword, page, status, toast]);

  useEffect(() => {
    let cancelled = false;
    setSummaryLoading(true);
    searchPlatformFeeTypes({ page: 0, size: SUMMARY_PAGE_SIZE, transactionType: "SUBSCRIPTION_PURCHASE" })
      .then((response) => {
        if (cancelled) return;
        setActiveCount(response.data.items.filter((item) => item.status === "ACTIVE").length);
        setDraftCount(response.data.items.filter((item) => item.status === "DRAFT").length);
      })
      .catch(() => {
        if (!cancelled) {
          setActiveCount(0);
          setDraftCount(0);
        }
      })
      .finally(() => {
        if (!cancelled) setSummaryLoading(false);
      });
    if (canPreview) {
      previewPlatformFees({
        customerPaidAmount: NOMINAL_PREVIEW_AMOUNT,
        effectiveAt: new Date().toISOString(),
        grossAmount: NOMINAL_PREVIEW_AMOUNT,
        transactionType: "SUBSCRIPTION_PURCHASE",
      })
        .then((response) => {
          if (!cancelled) setTotalRate(response.data.effectiveTotalRate);
        })
        .catch(() => {
          if (!cancelled) setTotalRate(null);
        });
    } else {
      setTotalRate(null);
    }
    return () => {
      cancelled = true;
    };
  }, [canPreview]);

  useEffect(() => {
    let cancelled = false;
    const missing = items.filter((item) => !enrichment[item.platformFeeTypeId]);
    if (missing.length === 0) return undefined;
    void Promise.all(
      missing.map(async (item) => {
        try {
          const response = await getPlatformFeeVersionHistory(item.platformFeeTypeId);
          const now = Date.now();
          const effective = response.data
            .filter((version) => {
              if (version.status !== "ACTIVE") return false;
              const from = version.effectiveFrom ? new Date(version.effectiveFrom).getTime() : Number.NaN;
              const to = version.effectiveTo ? new Date(version.effectiveTo).getTime() : Number.NaN;
              if (!Number.isNaN(from) && now < from) return false;
              if (!Number.isNaN(to) && now >= to) return false;
              return true;
            })
            .sort((left, right) => right.version - left.version)[0];
          return {
            id: item.platformFeeTypeId,
            rateText: effective ? formatFeePercent(effective.feeRate) : "—",
            validityText: effective
              ? `${formatFeeDate(effective.effectiveFrom) ?? "—"} - ${effective.effectiveTo ? formatFeeDate(effective.effectiveTo) : "Không giới hạn"}`
              : "Chưa kích hoạt",
          };
        } catch {
          return { id: item.platformFeeTypeId, rateText: "—", validityText: "Chưa kích hoạt" };
        }
      }),
    ).then((results) => {
      if (cancelled) return;
      setEnrichment((current) => {
        const next = { ...current };
        results.forEach((result) => {
          next[result.id] = { rateText: result.rateText, validityText: result.validityText };
        });
        return next;
      });
    });
    return () => {
      cancelled = true;
    };
  }, [enrichment, items]);

  useEffect(() => {
    if (!openMenuId) return undefined;
    const closeMenu = () => {
      setOpenMenuId(null);
      setMenuPosition(null);
    };
    const handlePointerDown = (event: globalThis.MouseEvent) => {
      if (!menuRootRef.current?.contains(event.target as Node)) closeMenu();
    };
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeMenu();
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

  const startIndex = totalRecords === 0 ? 0 : (page - 1) * pageSize + 1;
  const endIndex = Math.min(page * pageSize, totalRecords);

  function reloadAll() {
    setEnrichment({});
    searchPlatformFeeTypes({
      feeBasis: feeBasis === "all" ? undefined : (feeBasis as "AFTER_DISCOUNT" | "GROSS_AMOUNT"),
      keyword: keyword || undefined,
      page: page - 1,
      size: pageSize,
      status: status === "all" ? undefined : (status as PlatformFeeTypeStatus),
      transactionType: "SUBSCRIPTION_PURCHASE",
    })
      .then((response) => {
        setItems(response.data.items);
        setTotalRecords(response.data.totalElements);
        setTotalPages(Math.max(1, response.data.totalPages));
      })
      .catch((error: unknown) => {
        toast.error(getErrorMessage(error, "Không thể tải danh sách phí sàn."), "Tải dữ liệu thất bại");
      });
    searchPlatformFeeTypes({ page: 0, size: SUMMARY_PAGE_SIZE, transactionType: "SUBSCRIPTION_PURCHASE" })
      .then((response) => {
        setActiveCount(response.data.items.filter((item) => item.status === "ACTIVE").length);
        setDraftCount(response.data.items.filter((item) => item.status === "DRAFT").length);
      })
      .catch(() => undefined);
  }

  function openActionMenu(feeTypeId: string, trigger: HTMLButtonElement) {
    if (openMenuId === feeTypeId) {
      setOpenMenuId(null);
      setMenuPosition(null);
      return;
    }
    const rect = trigger.getBoundingClientRect();
    const maxLeft = window.innerWidth - ACTION_MENU_WIDTH - 8;
    setMenuPosition({
      left: Math.max(8, Math.min(rect.right - ACTION_MENU_WIDTH, maxLeft)),
      top: rect.bottom + 8,
    });
    setOpenMenuId(feeTypeId);
  }

  function getRowActions(feeType: PlatformFeeTypeResponse) {
    const actions: Array<{ action: "activate" | "archive" | "deactivate" | "preview" | "versions"; danger?: boolean; icon: string; label: string }> = [
      { action: "versions", icon: "fas fa-list-ul", label: "Xem phiên bản" },
    ];
    if ((feeType.status === "DRAFT" || feeType.status === "INACTIVE") && canActivate) {
      actions.push({ action: "activate", icon: "fas fa-check-circle", label: "Kích hoạt" });
    }
    if (feeType.status === "ACTIVE" && canActivate) {
      actions.push({ action: "deactivate", icon: "fas fa-ban", label: "Ngừng áp dụng" });
    }
    if ((feeType.status === "DRAFT" || feeType.status === "INACTIVE") && canArchive) {
      actions.push({ action: "archive", danger: true, icon: "far fa-trash-alt", label: "Lưu trữ" });
    }
    if (canPreview) {
      actions.push({ action: "preview", icon: "fas fa-calculator", label: "Xem trước tính phí" });
    }
    return actions;
  }

  function handleMenuAction(feeType: PlatformFeeTypeResponse, action: "activate" | "archive" | "deactivate" | "preview" | "versions") {
    setOpenMenuId(null);
    setMenuPosition(null);
    if (action === "versions") {
      onOpenDetail(feeType.platformFeeTypeId, "versions");
      return;
    }
    if (action === "preview") {
      onPreview();
      return;
    }
    setConfirmAction({ feeType, kind: action });
  }

  async function handleConfirm() {
    if (!confirmAction) return;
    setConfirming(true);
    try {
      if (confirmAction.kind === "activate") {
        await activatePlatformFeeType(confirmAction.feeType.platformFeeTypeId);
        toast.success(`Đã kích hoạt phí ${confirmAction.feeType.feeCode}.`, "Kích hoạt thành công");
      } else if (confirmAction.kind === "deactivate") {
        await deactivatePlatformFeeType(confirmAction.feeType.platformFeeTypeId);
        toast.success(`Đã ngừng áp dụng phí ${confirmAction.feeType.feeCode}.`, "Ngừng áp dụng thành công");
      } else {
        await archivePlatformFeeType(confirmAction.feeType.platformFeeTypeId);
        toast.success(`Đã lưu trữ phí ${confirmAction.feeType.feeCode}.`, "Lưu trữ thành công");
      }
      setConfirmAction(null);
      reloadAll();
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể thực hiện thao tác."), "Thao tác thất bại");
    } finally {
      setConfirming(false);
    }
  }

  const openMenuRow = openMenuId ? items.find((item) => item.platformFeeTypeId === openMenuId) ?? null : null;
  const openMenuActions = openMenuRow ? getRowActions(openMenuRow) : [];

  const actionMenu = openMenuRow && menuPosition
    ? createPortal(
      <div
        className="tw-fixed tw-z-[2600] tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-py-1.5 tw-text-left tw-shadow-vm-dropdown"
        ref={menuRootRef}
        style={{ left: menuPosition.left, top: menuPosition.top, width: ACTION_MENU_WIDTH }}
      >
        {openMenuActions.map((item) => (
          <button
            className={cn(
              "tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-transparent tw-px-3 tw-text-left tw-text-[0.86rem] tw-font-bold tw-transition",
              item.danger
                ? "tw-text-red-600 hover:tw-bg-red-50"
                : "tw-text-vm-slate-700 hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary",
            )}
            key={item.action}
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              handleMenuAction(openMenuRow, item.action);
            }}
          >
            <i className={`${item.icon} tw-w-4 tw-text-center tw-text-[0.82rem]`} aria-hidden="true" />
            <span>{item.label}</span>
          </button>
        ))}
      </div>,
      document.body,
    )
    : null;

  return (
    <div className="tw-grid tw-gap-4">
      <header className="tw-grid tw-gap-1">
        <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Quản lý phí sàn</h1>
        <p className="tw-m-0 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-500">
          Thiết lập các khoản phí áp dụng cho toàn bộ đơn mua vé tháng trên sàn.
        </p>
      </header>

      <section className="tw-grid tw-grid-cols-4 tw-gap-3 max-[1100px]:tw-grid-cols-2 max-[600px]:tw-grid-cols-1">
        <KpiCard icon="fas fa-database" iconClassName="tw-bg-brand-50 tw-text-vm-primary" label="Phí đang áp dụng" value={summaryLoading ? "—" : String(activeCount)} />
        <KpiCard icon="far fa-file-alt" iconClassName="tw-bg-amber-50 tw-text-amber-500" label="Bản nháp" value={summaryLoading ? "—" : String(draftCount)} />
        <KpiCard icon="fas fa-chart-pie" iconClassName="tw-bg-green-50 tw-text-green-500" label="Tổng tỷ lệ hiện tại" value={totalRate === null ? "—" : formatFeePercent(totalRate)} />
        <KpiCard icon="fas fa-exclamation-triangle" iconClassName="tw-bg-red-50 tw-text-red-500" label="Giới hạn tối đa" value={`${PLATFORM_FEE_MAX_TOTAL_RATE * 100}%`} />
      </section>

      <Card className="tw-overflow-hidden">
        <div className="tw-grid tw-grid-cols-[minmax(220px,1fr)_200px_200px_auto] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4 max-[1000px]:tw-grid-cols-2 max-[640px]:tw-grid-cols-1">
          <span className="tw-relative tw-block max-[1000px]:tw-col-span-2 max-[640px]:tw-col-span-1">
            <i className="fas fa-search tw-absolute tw-left-3 tw-top-1/2 tw--translate-y-1/2 tw-text-vm-slate-400" aria-hidden="true" />
            <input
              aria-label="Tìm theo mã hoặc tên phí"
              className="tw-h-[42px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-pl-9 tw-pr-3 tw-text-[0.86rem] tw-text-vm-slate-900 placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-outline-none"
              placeholder="Tìm theo mã hoặc tên phí"
              value={keywordInput}
              onChange={(event) => setKeywordInput(event.target.value)}
            />
          </span>
          <SelectMenu
            ariaLabel="Lọc trạng thái phí sàn"
            options={statusOptions}
            portal
            value={status}
            onChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
          />
          <SelectMenu
            ariaLabel="Lọc cơ sở tính phí sàn"
            options={feeBasisOptions}
            portal
            value={feeBasis}
            onChange={(value) => {
              setFeeBasis(value);
              setPage(1);
            }}
          />
          {canCreate ? (
            <Button onClick={onCreate}>
              <i className="fas fa-plus" aria-hidden="true" /> Tạo phí sàn
            </Button>
          ) : null}
        </div>

        <div className="tw-overflow-x-auto">
          <table className="tw-w-full tw-min-w-[960px] tw-border-collapse tw-text-left tw-text-[0.82rem]">
            <thead className="tw-bg-vm-slate-50 tw-text-vm-slate-600">
              <tr>
                {["Mã phí", "Tên phí", "Cơ sở tính", "Tỷ lệ hiện tại", "Hiệu lực", "Cộng dồn", "Trạng thái", "Thao tác"].map((label) => (
                  <th className="tw-whitespace-nowrap tw-px-4 tw-py-3 tw-font-black" key={label}>{label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className="tw-px-4 tw-py-10 tw-text-center tw-font-bold tw-text-vm-slate-500" colSpan={8}>
                    <i className="fas fa-spinner fa-spin tw-mr-2 tw-text-vm-primary" aria-hidden="true" />
                    Đang tải danh sách phí...
                  </td>
                </tr>
              ) : null}
              {!loading && items.length === 0 ? (
                <tr>
                  <td className="tw-px-4 tw-py-10 tw-text-center tw-font-semibold tw-text-vm-slate-500" colSpan={8}>
                    Không có khoản phí nào phù hợp.
                  </td>
                </tr>
              ) : null}
              {!loading && items.map((item) => {
                const pill = feeTypePillMeta[item.status];
                const extra = enrichment[item.platformFeeTypeId];
                const rowActions = getRowActions(item);
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-transition hover:tw-bg-brand-50" key={item.platformFeeTypeId}>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-bold tw-text-vm-slate-900">{item.feeCode}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-semibold tw-text-vm-slate-700">{item.name}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-semibold tw-text-vm-slate-700">{feeBasisLabels[item.feeBasis]}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-bold tw-text-vm-slate-900">{extra ? extra.rateText : "…"}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-semibold tw-text-vm-slate-700">{extra ? extra.validityText : "…"}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-semibold tw-text-vm-slate-700">{item.stackable ? "Có" : "Không"}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5">
                      <PlatformFeeStatusPill label={pill.label} tone={pill.tone} />
                    </td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5">
                      <div className="tw-flex tw-items-center tw-gap-1">
                        <button
                          aria-label={`Xem chi tiết phí ${item.feeCode}`}
                          className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-[0.9rem] tw-text-vm-slate-500 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                          type="button"
                          onClick={() => onOpenDetail(item.platformFeeTypeId, "info")}
                        >
                          <i className="far fa-eye" aria-hidden="true" />
                        </button>
                        {canUpdate ? (
                          <button
                            aria-label={`Sửa phí ${item.feeCode}`}
                            className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-[0.9rem] tw-text-vm-slate-500 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary"
                            type="button"
                            onClick={() => onOpenDetail(item.platformFeeTypeId, "info")}
                          >
                            <i className="fas fa-pencil-alt" aria-hidden="true" />
                          </button>
                        ) : null}
                        <button
                          aria-expanded={openMenuId === item.platformFeeTypeId}
                          aria-label={`Mở menu thao tác phí ${item.feeCode}`}
                          className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-[0.9rem] tw-text-vm-slate-500 tw-transition hover:tw-bg-brand-50 hover:tw-text-vm-primary disabled:tw-cursor-not-allowed disabled:tw-opacity-40"
                          disabled={rowActions.length === 0}
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            openActionMenu(item.platformFeeTypeId, event.currentTarget);
                          }}
                        >
                          <i className="fas fa-ellipsis-v" aria-hidden="true" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <PlatformFeePager
          currentPage={page}
          endIndex={endIndex}
          onPageChange={setPage}
          startIndex={startIndex}
          totalPages={totalPages}
          totalRecords={totalRecords}
        />
        {actionMenu}
      </Card>

      <ConfirmDialog
        cancelLabel="Hủy"
        confirmLabel={confirmAction?.kind === "archive" ? "Lưu trữ" : confirmAction?.kind === "deactivate" ? "Ngừng áp dụng" : "Kích hoạt"}
        loading={confirming}
        message={confirmAction?.kind === "archive"
          ? (<>Lưu trữ phí <strong>{confirmAction.feeType.feeCode}</strong>? Phí đã lưu trữ không thể khôi phục.</>)
          : confirmAction?.kind === "deactivate"
            ? (<>Ngừng áp dụng phí <strong>{confirmAction.feeType.feeCode}</strong>?</>)
            : (<>Kích hoạt phí <strong>{confirmAction?.feeType.feeCode}</strong>? Phí cần có phiên bản tỷ lệ đã kích hoạt để tính phí.</>)}
        open={confirmAction !== null}
        title={confirmAction?.kind === "archive" ? "Lưu trữ phí" : confirmAction?.kind === "deactivate" ? "Ngừng áp dụng" : "Kích hoạt phí"}
        tone={confirmAction?.kind === "archive" ? "danger" : "default"}
        onCancel={() => {
          if (!confirming) setConfirmAction(null);
        }}
        onConfirm={() => void handleConfirm()}
      />
    </div>
  );
}
