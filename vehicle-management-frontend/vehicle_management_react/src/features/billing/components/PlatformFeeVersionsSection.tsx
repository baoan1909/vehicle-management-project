import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";

import { Button, Card, ConfirmDialog, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";
import {
  PLATFORM_FEE_MAX_TOTAL_RATE,
  activatePlatformFeeVersion,
  archivePlatformFeeVersion,
  createPlatformFeeVersion,
  deactivatePlatformFeeVersion,
  getPlatformFeeTypeDetail,
  getPlatformFeeVersionHistory,
  previewPlatformFees,
  updateDraftPlatformFeeVersion,
  type PlatformFeeTypeDetailResponse,
  type PlatformFeeVersionResponse,
  type SavePlatformFeeVersionRequest,
} from "@/features/billing/api/platformFeeApi";
import { PlatformFeeVersionDrawer } from "@/features/billing/components/PlatformFeeVersionDrawer";
import {
  formatFeeDate,
  formatFeeDateTime,
  formatFeePercent,
  getActorLabel,
  getErrorMessage,
  isVersionEditable,
  platformFeePermissions,
  resolveVersionDisplay,
} from "@/features/billing/components/platformFeeMeta";
import type { CurrentUser } from "@/shared/types/common";

type ConfirmAction = {
  kind: "activate" | "archive" | "deactivate";
  version: PlatformFeeVersionResponse;
};

type VersionsSectionProps = {
  bare?: boolean;
  feeTypeId: string;
  onBack: () => void;
};

const ACTION_MENU_WIDTH = 208;

function ActorCell({ time, user, userId }: { time: string | null; user: CurrentUser | null | undefined; userId: string | null }) {
  const label = getActorLabel(userId, user);
  const formatted = formatFeeDateTime(time);
  return (
    <div className="tw-grid tw-gap-0.5">
      <strong className="tw-block tw-text-vm-slate-900" title={userId ?? undefined}>
        {label}
      </strong>
      <span className="tw-block tw-text-[0.74rem] tw-font-semibold tw-text-vm-slate-500">{formatted ?? "—"}</span>
    </div>
  );
}

export function PlatformFeeVersionsSection({ bare = false, feeTypeId, onBack }: VersionsSectionProps) {
  const toast = useToast();
  const { user } = useAuth();
  const [detail, setDetail] = useState<PlatformFeeTypeDetailResponse | null>(null);
  const [versions, setVersions] = useState<PlatformFeeVersionResponse[]>([]);
  const [totalRate, setTotalRate] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingVersion, setEditingVersion] = useState<PlatformFeeVersionResponse | null>(null);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);
  const [menuPosition, setMenuPosition] = useState<{ left: number; top: number } | null>(null);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction | null>(null);
  const [confirming, setConfirming] = useState(false);
  const menuRootRef = useRef<HTMLDivElement | null>(null);

  const canUpdate = hasAnyPermission(user, platformFeePermissions.update);
  const canActivate = hasAnyPermission(user, platformFeePermissions.activate);
  const canArchive = hasAnyPermission(user, platformFeePermissions.archive);
  const canPreview = hasAnyPermission(user, platformFeePermissions.preview);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [detailResponse, versionsResponse] = await Promise.all([
        getPlatformFeeTypeDetail(feeTypeId),
        getPlatformFeeVersionHistory(feeTypeId),
      ]);
      setDetail(detailResponse.data);
      setVersions(versionsResponse.data);
      if (canPreview) {
        try {
          const previewResponse = await previewPlatformFees({
            customerPaidAmount: 1000000,
            effectiveAt: new Date().toISOString(),
            grossAmount: 1000000,
            transactionType: detailResponse.data.feeType.transactionType,
          });
          setTotalRate(previewResponse.data.effectiveTotalRate);
        } catch {
          setTotalRate(null);
        }
      } else {
        setTotalRate(null);
      }
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể tải phiên bản tỷ lệ."), "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [canPreview, feeTypeId, toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

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

  const sortedVersions = useMemo(
    () => [...versions].sort((left, right) => right.version - left.version),
    [versions],
  );

  const now = useMemo(() => Date.now(), [versions]);

  function openActionMenu(versionId: string, trigger: HTMLButtonElement) {
    if (openMenuId === versionId) {
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
    setOpenMenuId(versionId);
  }

  function getRowActions(version: PlatformFeeVersionResponse) {
    const actions: Array<{ action: "activate" | "archive" | "deactivate" | "edit"; icon: string; label: string }> = [];
    if (isVersionEditable(version) && canUpdate) {
      actions.push({ action: "edit", icon: "far fa-edit", label: "Sửa" });
    }
    if ((version.status === "DRAFT" || version.status === "INACTIVE") && canActivate) {
      actions.push({ action: "activate", icon: "fas fa-check-circle", label: "Kích hoạt" });
    }
    if (version.status === "ACTIVE" && canActivate) {
      actions.push({ action: "deactivate", icon: "fas fa-ban", label: "Ngừng áp dụng" });
    }
    if ((version.status === "DRAFT" || version.status === "INACTIVE") && canArchive) {
      actions.push({ action: "archive", icon: "far fa-trash-alt", label: "Lưu trữ" });
    }
    return actions;
  }

  function handleMenuAction(version: PlatformFeeVersionResponse, action: "activate" | "archive" | "deactivate" | "edit") {
    setOpenMenuId(null);
    setMenuPosition(null);
    if (action === "edit") {
      setEditingVersion(version);
      setDrawerOpen(true);
      return;
    }
    setConfirmAction({ kind: action, version });
  }

  async function handleCreateDraft(payload: SavePlatformFeeVersionRequest) {
    await createPlatformFeeVersion(feeTypeId, payload);
    setDrawerOpen(false);
    toast.success("Đã lưu phiên bản tỷ lệ dưới dạng bản nháp.", "Tạo phiên bản thành công");
    await loadData();
  }

  async function handleCreateAndActivate(payload: SavePlatformFeeVersionRequest) {
    const created = await createPlatformFeeVersion(feeTypeId, payload);
    try {
      await activatePlatformFeeVersion(feeTypeId, created.data.platformFeeRuleVersionId);
    } catch (error) {
      toast.warning("Đã tạo bản nháp nhưng kích hoạt thất bại. Vui lòng kích hoạt lại từ danh sách.", "Kích hoạt thất bại");
      setDrawerOpen(false);
      await loadData();
      throw error;
    }
    setDrawerOpen(false);
    toast.success("Phiên bản tỷ lệ mới đã được kích hoạt.", "Kích hoạt thành công");
    await loadData();
  }

  async function handleUpdate(versionId: string, payload: SavePlatformFeeVersionRequest) {
    await updateDraftPlatformFeeVersion(feeTypeId, versionId, payload);
    setDrawerOpen(false);
    setEditingVersion(null);
    toast.success("Đã cập nhật phiên bản tỷ lệ.", "Lưu thay đổi thành công");
    await loadData();
  }

  async function handleConfirm() {
    if (!confirmAction) return;
    setConfirming(true);
    try {
      if (confirmAction.kind === "activate") {
        await activatePlatformFeeVersion(feeTypeId, confirmAction.version.platformFeeRuleVersionId);
        toast.success(`Đã kích hoạt phiên bản v${confirmAction.version.version}.`, "Kích hoạt thành công");
      } else if (confirmAction.kind === "deactivate") {
        await deactivatePlatformFeeVersion(feeTypeId, confirmAction.version.platformFeeRuleVersionId);
        toast.success(`Đã ngừng áp dụng phiên bản v${confirmAction.version.version}.`, "Ngừng áp dụng thành công");
      } else {
        await archivePlatformFeeVersion(feeTypeId, confirmAction.version.platformFeeRuleVersionId);
        toast.success(`Đã lưu trữ phiên bản v${confirmAction.version.version}.`, "Lưu trữ thành công");
      }
      setConfirmAction(null);
      await loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể thực hiện thao tác."), "Thao tác thất bại");
    } finally {
      setConfirming(false);
    }
  }

  const openMenuRow = openMenuId ? versions.find((version) => version.platformFeeRuleVersionId === openMenuId) ?? null : null;
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
            className="tw-flex tw-min-h-9 tw-w-full tw-items-center tw-gap-2.5 tw-border-0 tw-bg-transparent tw-px-3 tw-text-left tw-text-[0.86rem] tw-font-bold tw-text-vm-slate-700 tw-transition hover:tw-bg-vm-slate-25 hover:tw-text-vm-primary"
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

  const feeType = detail?.feeType ?? null;
  const effectiveVersion = detail?.effectiveVersion ?? null;
  const upcomingVersion = detail?.upcomingVersion ?? null;
  const maxTotalPercent = PLATFORM_FEE_MAX_TOTAL_RATE * 100;

  return (
    <div className="tw-grid tw-gap-4">
      {bare ? (
        canUpdate ? (
          <div className="tw-flex tw-justify-end">
            <Button
              onClick={() => {
                setEditingVersion(null);
                setDrawerOpen(true);
              }}
            >
              <i className="fas fa-plus" aria-hidden="true" /> Thêm phiên bản
            </Button>
          </div>
        ) : null
      ) : (
        <>
          <nav aria-label="Breadcrumb" className="tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
            <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-font-semibold tw-text-vm-slate-500 tw-transition hover:tw-text-vm-primary" type="button" onClick={onBack}>
              Phí sàn
            </button>
            <span aria-hidden="true">/</span>
            <span className="tw-rounded-vm-sm tw-bg-vm-slate-100 tw-px-2 tw-py-0.5 tw-font-bold tw-text-vm-slate-700">
              {feeType?.feeCode ?? "..."}
            </span>
            <span aria-hidden="true">/</span>
            <span className="tw-font-bold tw-text-vm-slate-900">Phiên bản tỷ lệ</span>
          </nav>

          <header className="tw-flex tw-items-start tw-justify-between tw-gap-4">
            <div className="tw-grid tw-gap-1">
              <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Phiên bản tỷ lệ</h1>
              <p className="tw-m-0 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-500">
                {feeType ? `Quản lý tỷ lệ và thời gian hiệu lực của ${feeType.name}` : "Quản lý tỷ lệ và thời gian hiệu lực của phí nền tảng"}
              </p>
            </div>
            {canUpdate ? (
              <Button
                onClick={() => {
                  setEditingVersion(null);
                  setDrawerOpen(true);
                }}
              >
                <i className="fas fa-plus" aria-hidden="true" /> Thêm phiên bản
              </Button>
            ) : null}
          </header>
        </>
      )}

      <Card className="tw-grid tw-grid-cols-3 tw-divide-x tw-divide-solid tw-divide-vm-slate-100 max-[900px]:tw-grid-cols-1 max-[900px]:tw-divide-x-0 max-[900px]:tw-divide-y">
        <div className="tw-flex tw-items-center tw-gap-4 tw-p-5">
          <span className="tw-flex tw-h-12 tw-w-12 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-[1.15rem] tw-text-vm-primary">
            <i className="fas fa-percent" aria-hidden="true" />
          </span>
          <span className="tw-grid tw-gap-1">
            <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">Tỷ lệ đang áp dụng</span>
            <strong className="tw-text-[1.5rem] tw-font-black tw-leading-none tw-text-vm-primary">
              {loading ? "—" : formatFeePercent(effectiveVersion?.feeRate)}
            </strong>
          </span>
        </div>
        <div className="tw-flex tw-items-center tw-gap-4 tw-p-5">
          <span className="tw-flex tw-h-12 tw-w-12 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-[1.15rem] tw-text-vm-primary">
            <i className="fas fa-layer-group" aria-hidden="true" />
          </span>
          <span className="tw-grid tw-gap-1">
            <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">Tổng phí toàn sàn</span>
            <strong className="tw-text-[1.5rem] tw-font-black tw-leading-none">
              <span className="tw-text-green-600">{loading || totalRate === null ? "—" : formatFeePercent(totalRate)}</span>
              <span className="tw-text-[1.1rem] tw-font-bold tw-text-vm-slate-500"> / {maxTotalPercent}%</span>
            </strong>
          </span>
        </div>
        <div className="tw-flex tw-items-center tw-gap-4 tw-p-5">
          <span className="tw-flex tw-h-12 tw-w-12 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-bg-brand-50 tw-text-[1.15rem] tw-text-vm-primary">
            <i className="far fa-calendar-alt" aria-hidden="true" />
          </span>
          <span className="tw-grid tw-gap-1">
            <span className="tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">Phiên bản kế tiếp</span>
            <strong className="tw-text-[1.5rem] tw-font-black tw-leading-none tw-text-vm-primary">
              {loading ? "—" : formatFeePercent(upcomingVersion?.feeRate)}
            </strong>
            {upcomingVersion?.effectiveFrom ? (
              <span className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">
                từ {formatFeeDate(upcomingVersion.effectiveFrom)}
              </span>
            ) : null}
          </span>
        </div>
      </Card>

      <Card className="tw-overflow-hidden">
        <div className="tw-overflow-x-auto">
          <table className="tw-w-full tw-min-w-[880px] tw-border-collapse tw-text-left tw-text-[0.8rem]">
            <thead className="tw-bg-vm-slate-25 tw-text-vm-slate-600">
              <tr>
                {["Phiên bản", "Tỷ lệ", "Hiệu lực từ", "Hiệu lực đến", "Trạng thái", "Người kích hoạt", "Thao tác"].map((label) => (
                  <th className="tw-whitespace-nowrap tw-px-4 tw-py-3 tw-font-black" key={label}>{label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td className="tw-px-4 tw-py-10 tw-text-center tw-font-bold tw-text-vm-slate-500" colSpan={7}>
                    <i className="fas fa-spinner fa-spin tw-mr-2 tw-text-vm-primary" aria-hidden="true" />
                    Đang tải phiên bản tỷ lệ...
                  </td>
                </tr>
              ) : null}
              {!loading && sortedVersions.length === 0 ? (
                <tr>
                  <td className="tw-px-4 tw-py-10 tw-text-center tw-font-semibold tw-text-vm-slate-500" colSpan={7}>
                    Chưa có phiên bản tỷ lệ nào.
                  </td>
                </tr>
              ) : null}
              {!loading && sortedVersions.map((version) => {
                const display = resolveVersionDisplay(version, now);
                const rowActions = getRowActions(version);
                const actorId = version.activatedBy ?? version.createdBy;
                const actorTime = version.activatedAt ?? version.createdAt;
                return (
                  <tr className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-transition hover:tw-bg-brand-50" key={version.platformFeeRuleVersionId}>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-black tw-text-vm-slate-900">v{version.version}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-black tw-text-vm-slate-900">{formatFeePercent(version.feeRate)}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-semibold tw-text-vm-slate-700">{formatFeeDateTime(version.effectiveFrom) ?? "—"}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-font-semibold tw-text-vm-slate-700">{formatFeeDateTime(version.effectiveTo) ?? "Không giới hạn"}</td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5">
                      <span className={display.pillClassName}>
                        <span className={display.dotClassName} aria-hidden="true" />
                        {display.label}
                      </span>
                    </td>
                    <td className="tw-whitespace-nowrap tw-px-4 tw-py-3.5 tw-text-[0.8rem]">
                      <ActorCell time={actorTime} user={user} userId={actorId} />
                    </td>
                    <td className="tw-px-4 tw-py-3.5">
                      <div className="tw-flex tw-items-center tw-justify-center">
                        <button
                          aria-expanded={openMenuId === version.platformFeeRuleVersionId}
                          aria-label={`Mở menu thao tác phiên bản v${version.version}`}
                          className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-500 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-900 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus disabled:tw-cursor-not-allowed disabled:tw-text-slate-300"
                          disabled={rowActions.length === 0}
                          title={rowActions.length === 0 ? "Bạn chỉ có quyền xem." : undefined}
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            openActionMenu(version.platformFeeRuleVersionId, event.currentTarget);
                          }}
                        >
                          <i className="fas fa-ellipsis-h" aria-hidden="true" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {actionMenu}
      </Card>

      <PlatformFeeVersionDrawer
        editingVersion={editingVersion}
        onClose={() => {
          setDrawerOpen(false);
          setEditingVersion(null);
        }}
        open={drawerOpen}
        onSaveAndActivate={(payload) => handleCreateAndActivate(payload)}
        onSaveDraft={(payload) => handleCreateDraft(payload)}
        onUpdate={(versionId, payload) => handleUpdate(versionId, payload)}
      />

      <ConfirmDialog
        cancelLabel="Hủy"
        confirmLabel={confirmAction?.kind === "archive" ? "Lưu trữ" : confirmAction?.kind === "deactivate" ? "Ngừng áp dụng" : "Kích hoạt"}
        loading={confirming}
        message={confirmAction?.kind === "archive"
          ? (<>Lưu trữ phiên bản <strong>v{confirmAction.version.version}</strong> ({formatFeePercent(confirmAction.version.feeRate)})? Phiên bản đã lưu trữ không thể khôi phục.</>)
          : confirmAction?.kind === "deactivate"
            ? (<>Ngừng áp dụng phiên bản <strong>v{confirmAction.version.version}</strong> ({formatFeePercent(confirmAction.version.feeRate)})?</>)
            : (<>Kích hoạt phiên bản <strong>v{confirmAction?.version.version}</strong> ({confirmAction ? formatFeePercent(confirmAction.version.feeRate) : ""})? Sau khi kích hoạt sẽ không thể chỉnh sửa.</>)}
        open={confirmAction !== null}
        title={confirmAction?.kind === "archive" ? "Lưu trữ phiên bản" : confirmAction?.kind === "deactivate" ? "Ngừng áp dụng" : "Kích hoạt phiên bản"}
        tone={confirmAction?.kind === "archive" ? "danger" : "default"}
        onCancel={() => {
          if (!confirming) setConfirmAction(null);
        }}
        onConfirm={() => void handleConfirm()}
      />
    </div>
  );
}
