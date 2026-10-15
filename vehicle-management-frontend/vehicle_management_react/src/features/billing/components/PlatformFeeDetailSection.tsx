import { useCallback, useEffect, useMemo, useState } from "react";

import { Button, Card, ConfirmDialog, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { cn } from "@/lib/cn";
import {
  PLATFORM_FEE_MAX_TOTAL_RATE,
  activatePlatformFeeType,
  archivePlatformFeeType,
  deactivatePlatformFeeType,
  getPlatformFeeTypeDetail,
  getPlatformFeeVersionHistory,
  previewPlatformFees,
  updateDraftPlatformFeeType,
  type PlatformFeeBasis,
  type PlatformFeeTypeDetailResponse,
  type PlatformFeeVersionResponse,
} from "@/features/billing/api/platformFeeApi";
import { FeeFieldLabel, FeeLockedInput, FeeToggle } from "@/features/billing/components/PlatformFeeControls";
import { PlatformFeeStatusPill } from "@/features/billing/components/PlatformFeeStatusPill";
import { PlatformFeeVersionsSection } from "@/features/billing/components/PlatformFeeVersionsSection";
import {
  chargedToLabels,
  feeTypePillMeta,
  formatFeeDateTime,
  formatFeePercent,
  getActorLabel,
  getErrorMessage,
  platformFeeInputClassName,
  platformFeePermissions,
  platformFeeTextareaClassName,
  transactionTypeLabels,
} from "@/features/billing/components/platformFeeMeta";

export type FeeDetailTab = "history" | "info" | "versions";

type DetailSectionProps = {
  feeTypeId: string;
  initialTab?: FeeDetailTab;
  onBack: () => void;
};

type HistoryEvent = {
  actorId: string | null;
  key: string;
  time: string;
  title: string;
  tone: "amber" | "blue" | "gray" | "green" | "red";
};

const NOMINAL_PREVIEW_AMOUNT = 1000000;

function periodsOverlap(
  leftFrom: string | null,
  leftTo: string | null,
  rightFrom: string | null,
  rightTo: string | null,
) {
  const startLeft = leftFrom ? new Date(leftFrom).getTime() : Number.NEGATIVE_INFINITY;
  const endLeft = leftTo ? new Date(leftTo).getTime() : Number.POSITIVE_INFINITY;
  const startRight = rightFrom ? new Date(rightFrom).getTime() : Number.NEGATIVE_INFINITY;
  const endRight = rightTo ? new Date(rightTo).getTime() : Number.POSITIVE_INFINITY;
  if (!Number.isFinite(startLeft) || !Number.isFinite(endLeft) || !Number.isFinite(startRight) || !Number.isFinite(endRight)) {
    return false;
  }
  return startLeft < endRight && startRight < endLeft;
}

function ConditionRow({ description, met, title }: { description: string; met: boolean; title: string }) {
  return (
    <div className="tw-flex tw-items-start tw-gap-3">
      <span
        className={cn(
          "tw-flex tw-h-6 tw-w-6 tw-flex-none tw-items-center tw-justify-center tw-rounded-full tw-text-[0.72rem] tw-text-white",
          met ? "tw-bg-green-500" : "tw-bg-red-500",
        )}
      >
        <i className={met ? "fas fa-check" : "fas fa-exclamation"} aria-hidden="true" />
      </span>
      <span className="tw-grid tw-min-w-0 tw-gap-0.5">
        <strong className="tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">{title}</strong>
        <span className="tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{description}</span>
      </span>
    </div>
  );
}

export function PlatformFeeDetailSection({ feeTypeId, initialTab = "info", onBack }: DetailSectionProps) {
  const toast = useToast();
  const { user } = useAuth();

  const canUpdate = hasAnyPermission(user, platformFeePermissions.update);
  const canActivate = hasAnyPermission(user, platformFeePermissions.activate);
  const canArchive = hasAnyPermission(user, platformFeePermissions.archive);
  const canPreview = hasAnyPermission(user, platformFeePermissions.preview);

  const [tab, setTab] = useState<FeeDetailTab>(initialTab);
  const [detail, setDetail] = useState<PlatformFeeTypeDetailResponse | null>(null);
  const [versions, setVersions] = useState<PlatformFeeVersionResponse[]>([]);
  const [totalRate, setTotalRate] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [formName, setFormName] = useState("");
  const [formDescription, setFormDescription] = useState("");
  const [formFeeBasis, setFormFeeBasis] = useState("GROSS_AMOUNT");
  const [formStackable, setFormStackable] = useState(true);
  const [saving, setSaving] = useState(false);
  const [confirmKind, setConfirmKind] = useState<"activate" | "archive" | "deactivate" | null>(null);
  const [confirming, setConfirming] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [detailResponse, versionsResponse] = await Promise.all([
        getPlatformFeeTypeDetail(feeTypeId),
        getPlatformFeeVersionHistory(feeTypeId),
      ]);
      setDetail(detailResponse.data);
      setVersions(versionsResponse.data);
      const feeType = detailResponse.data.feeType;
      setFormName(feeType.name);
      setFormDescription(feeType.description ?? "");
      setFormFeeBasis(feeType.feeBasis);
      setFormStackable(feeType.stackable);
      if (canPreview) {
        try {
          const previewResponse = await previewPlatformFees({
            customerPaidAmount: NOMINAL_PREVIEW_AMOUNT,
            effectiveAt: new Date().toISOString(),
            grossAmount: NOMINAL_PREVIEW_AMOUNT,
            transactionType: feeType.transactionType,
          });
          setTotalRate(previewResponse.data.effectiveTotalRate);
        } catch {
          setTotalRate(null);
        }
      } else {
        setTotalRate(null);
      }
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể tải chi tiết phí sàn."), "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [canPreview, feeTypeId, toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const feeType = detail?.feeType ?? null;
  const editable = feeType?.status === "DRAFT" && canUpdate;
  const statusPill = feeType ? feeTypePillMeta[feeType.status] : null;

  const hasVersion = (detail?.totalVersions ?? 0) > 0;
  const hasOverlap = useMemo(() => {
    const active = versions.filter((version) => version.status === "ACTIVE");
    for (let left = 0; left < active.length; left += 1) {
      for (let right = left + 1; right < active.length; right += 1) {
        if (periodsOverlap(
          active[left].effectiveFrom,
          active[left].effectiveTo,
          active[right].effectiveFrom,
          active[right].effectiveTo,
        )) {
          return true;
        }
      }
    }
    return false;
  }, [versions]);
  const withinLimit = totalRate !== null && totalRate <= PLATFORM_FEE_MAX_TOTAL_RATE;

  const historyEvents = useMemo<HistoryEvent[]>(() => {
    if (!detail) return [];
    const events: HistoryEvent[] = [];
    if (feeType?.createdAt) {
      events.push({
        actorId: feeType.createdBy,
        key: `type-created-${feeType.platformFeeTypeId}`,
        time: feeType.createdAt,
        title: `Tạo loại phí ${feeType.feeCode}`,
        tone: "blue",
      });
    }
    if (feeType?.archivedAt) {
      events.push({
        actorId: feeType.archivedBy,
        key: `type-archived-${feeType.platformFeeTypeId}`,
        time: feeType.archivedAt,
        title: `Lưu trữ loại phí ${feeType.feeCode}`,
        tone: "gray",
      });
    }
    versions.forEach((version) => {
      if (version.createdAt) {
        events.push({
          actorId: version.createdBy,
          key: `v-created-${version.platformFeeRuleVersionId}`,
          time: version.createdAt,
          title: `Tạo phiên bản v${version.version} (${formatFeePercent(version.feeRate)})`,
          tone: "blue",
        });
      }
      if (version.activatedAt) {
        events.push({
          actorId: version.activatedBy,
          key: `v-activated-${version.platformFeeRuleVersionId}`,
          time: version.activatedAt,
          title: `Kích hoạt phiên bản v${version.version}`,
          tone: "green",
        });
      }
      if (version.deactivatedAt) {
        events.push({
          actorId: version.deactivatedBy,
          key: `v-deactivated-${version.platformFeeRuleVersionId}`,
          time: version.deactivatedAt,
          title: `Ngừng áp dụng phiên bản v${version.version}`,
          tone: "amber",
        });
      }
      if (version.archivedAt) {
        events.push({
          actorId: version.archivedBy,
          key: `v-archived-${version.platformFeeRuleVersionId}`,
          time: version.archivedAt,
          title: `Lưu trữ phiên bản v${version.version}`,
          tone: "gray",
        });
      }
    });
    return events.sort((left, right) => new Date(right.time).getTime() - new Date(left.time).getTime());
  }, [detail, feeType, versions]);

  async function handleSave() {
    if (!feeType || !editable) return;
    if (!formName.trim()) {
      toast.error("Vui lòng nhập tên phí.", "Thiếu thông tin");
      return;
    }
    setSaving(true);
    try {
      await updateDraftPlatformFeeType(feeType.platformFeeTypeId, {
        description: formDescription.trim() || null,
        feeBasis: formFeeBasis as PlatformFeeBasis,
        name: formName.trim(),
        stackable: formStackable,
      });
      toast.success("Đã cập nhật thông tin phí.", "Lưu thay đổi thành công");
      await loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể lưu thay đổi."), "Lưu thất bại");
    } finally {
      setSaving(false);
    }
  }

  async function handleConfirm() {
    if (!feeType || !confirmKind) return;
    setConfirming(true);
    try {
      if (confirmKind === "activate") {
        await activatePlatformFeeType(feeType.platformFeeTypeId);
        toast.success(`Đã kích hoạt phí ${feeType.feeCode}.`, "Kích hoạt thành công");
      } else if (confirmKind === "deactivate") {
        await deactivatePlatformFeeType(feeType.platformFeeTypeId);
        toast.success(`Đã ngừng áp dụng phí ${feeType.feeCode}.`, "Ngừng áp dụng thành công");
      } else {
        await archivePlatformFeeType(feeType.platformFeeTypeId);
        toast.success(`Đã lưu trữ phí ${feeType.feeCode}.`, "Lưu trữ thành công");
        setConfirmKind(null);
        onBack();
        return;
      }
      setConfirmKind(null);
      await loadData();
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể thực hiện thao tác."), "Thao tác thất bại");
    } finally {
      setConfirming(false);
    }
  }

  const showSave = editable;
  const showArchive = feeType && (feeType.status === "DRAFT" || feeType.status === "INACTIVE") && canArchive;

  return (
    <div className="tw-grid tw-gap-4">
      <nav aria-label="Breadcrumb" className="tw-flex tw-flex-wrap tw-items-center tw-gap-2 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-500">
        <button className="tw-border-0 tw-bg-transparent tw-p-0 tw-font-semibold tw-text-vm-slate-500 tw-transition hover:tw-text-vm-primary" type="button" onClick={onBack}>
          Phí sàn
        </button>
        <span aria-hidden="true">/</span>
        <span className="tw-font-bold tw-text-vm-slate-700">{feeType?.feeCode ?? "..."}</span>
      </nav>

      <header className="tw-flex tw-items-center tw-justify-between tw-gap-4 max-[700px]:tw-flex-col max-[700px]:tw-items-stretch">
        <div className="tw-flex tw-items-center tw-gap-3">
          <h1 className="tw-m-0 tw-text-[1.75rem] tw-font-black tw-leading-tight tw-text-vm-slate-900">Chi tiết phí sàn</h1>
          {statusPill ? <PlatformFeeStatusPill label={statusPill.label} tone={statusPill.tone} /> : null}
        </div>
        <div className="tw-flex tw-gap-2">
          {showSave ? (
            <Button loading={saving} onClick={() => void handleSave()}>
              <i className="far fa-save" aria-hidden="true" /> Lưu thay đổi
            </Button>
          ) : null}
          {showArchive ? (
            <Button variant="danger" onClick={() => setConfirmKind("archive")}>
              <i className="far fa-trash-alt" aria-hidden="true" /> Lưu trữ
            </Button>
          ) : null}
        </div>
      </header>

      <Card className="tw-flex tw-items-center tw-gap-4 tw-p-5">
        <span className="tw-flex tw-h-14 tw-w-14 tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-lg tw-bg-brand-50 tw-text-[1.5rem] tw-text-vm-primary">
          <i className="fas fa-database" aria-hidden="true" />
        </span>
        <span className="tw-grid tw-min-w-0 tw-gap-1">
          <strong className="tw-truncate tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">{feeType?.feeCode ?? "..."}</strong>
          <span className="tw-truncate tw-text-[0.88rem] tw-font-semibold tw-text-vm-slate-500">{feeType?.name ?? ""}</span>
        </span>
        <span className="tw-ml-auto tw-grid tw-flex-none tw-gap-1 tw-border-0 tw-border-l tw-border-solid tw-border-vm-slate-100 tw-pl-5">
          <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.84rem]">
            <span className="tw-font-semibold tw-text-vm-slate-500">Tạo lúc:</span>
            <strong className="tw-font-bold tw-text-vm-slate-900">{formatFeeDateTime(feeType?.createdAt) ?? "—"}</strong>
          </span>
          <span className="tw-flex tw-items-center tw-gap-2 tw-text-[0.84rem]">
            <span className="tw-font-semibold tw-text-vm-slate-500">Người tạo:</span>
            <strong className="tw-font-bold tw-text-vm-slate-900" title={feeType?.createdBy ?? undefined}>
              {feeType?.createdBy ? getActorLabel(feeType.createdBy, user) : "—"}
            </strong>
          </span>
        </span>
      </Card>

      <div className="tw-flex tw-items-center tw-gap-7 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 max-[640px]:tw-gap-4">
        {([
          { label: "Thông tin chung", value: "info" as FeeDetailTab },
          { label: `Phiên bản tỷ lệ (${detail?.totalVersions ?? 0})`, value: "versions" as FeeDetailTab },
          { label: "Lịch sử thay đổi", value: "history" as FeeDetailTab },
        ]).map((item) => {
          const active = tab === item.value;
          return (
            <button
              aria-selected={active}
              className={cn(
                "tw-relative tw-border-0 tw-bg-transparent tw-px-0 tw-pb-2.5 tw-text-[0.92rem] tw-font-bold tw-transition",
                active ? "tw-text-vm-primary" : "tw-text-vm-slate-500 hover:tw-text-vm-slate-700",
              )}
              key={item.value}
              role="tab"
              type="button"
              onClick={() => setTab(item.value)}
            >
              {item.label}
              {active ? <span className="tw-absolute tw-inset-x-0 tw--bottom-px tw-h-[3px] tw-rounded-full tw-bg-vm-primary" /> : null}
            </button>
          );
        })}
      </div>

      {tab === "info" ? (
        <>
          <section className="tw-grid tw-grid-cols-[minmax(0,1fr)_340px] tw-items-start tw-gap-4 max-[1000px]:tw-grid-cols-1">
            <Card className="tw-p-5">
              <h2 className="tw-m-0 tw-mb-4 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Thông tin chung</h2>
              {loading || !feeType ? (
                <p className="tw-m-0 tw-flex tw-items-center tw-gap-2 tw-py-8 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
                  <i className="fas fa-spinner fa-spin tw-text-vm-primary" aria-hidden="true" />
                  Đang tải thông tin phí...
                </p>
              ) : (
                <div className="tw-grid tw-gap-4">
                  <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                    <label className="tw-grid tw-gap-1.5 tw-content-start">
                      <FeeFieldLabel required>Tên phí</FeeFieldLabel>
                      <input
                        aria-label="Tên phí"
                        className={platformFeeInputClassName}
                        disabled={!editable || saving}
                        value={formName}
                        onChange={(event) => setFormName(event.target.value)}
                      />
                    </label>
                    <div className="tw-grid tw-gap-1.5 tw-content-start">
                      <FeeFieldLabel>Mã phí</FeeFieldLabel>
                      <FeeLockedInput ariaLabel="Mã phí" value={feeType.feeCode} />
                    </div>
                  </div>
                  <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                    <label className="tw-grid tw-gap-1.5 tw-content-start">
                      <FeeFieldLabel>Mô tả</FeeFieldLabel>
                      <textarea
                        aria-label="Mô tả"
                        className={platformFeeTextareaClassName}
                        disabled={!editable || saving}
                        rows={4}
                        value={formDescription}
                        onChange={(event) => setFormDescription(event.target.value)}
                      />
                    </label>
                    <div className="tw-grid tw-gap-1.5 tw-content-start">
                      <FeeFieldLabel>Loại giao dịch</FeeFieldLabel>
                      <FeeLockedInput ariaLabel="Loại giao dịch" value={transactionTypeLabels[feeType.transactionType]} />
                    </div>
                  </div>
                  <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
                    <label className="tw-grid tw-gap-1.5 tw-content-start">
                      <FeeFieldLabel required>Cơ sở tính phí</FeeFieldLabel>
                      <SelectMenu
                        ariaLabel="Cơ sở tính phí"
                        disabled={!editable || saving}
                        options={[
                          { label: "Giá gốc", value: "GROSS_AMOUNT" },
                          { label: "Sau giảm giá", value: "AFTER_DISCOUNT" },
                        ]}
                        portal
                        value={formFeeBasis}
                        onChange={setFormFeeBasis}
                      />
                    </label>
                    <div className="tw-grid tw-gap-1.5 tw-content-start">
                      <FeeFieldLabel>Bên chịu phí</FeeFieldLabel>
                      <FeeLockedInput ariaLabel="Bên chịu phí" value={chargedToLabels[feeType.chargedTo]} />
                    </div>
                  </div>
                  <div className="tw-grid tw-gap-1.5 tw-content-start">
                    <FeeFieldLabel hint="Chỉ áp dụng khi phí được kích hoạt và cho phép cộng dồn">Cho phép cộng dồn</FeeFieldLabel>
                    <FeeToggle checked={formStackable} disabled={!editable || saving} label="Cho phép cộng dồn" onChange={setFormStackable} />
                  </div>
                </div>
              )}
            </Card>

            <Card className="tw-grid tw-gap-4 tw-p-5">
              <h2 className="tw-m-0 tw-text-[1.1rem] tw-font-black tw-text-vm-slate-900">Điều kiện kích hoạt</h2>
              <ConditionRow
                description={hasVersion ? `Hiện có ${detail?.totalVersions ?? 0} phiên bản tỷ lệ.` : "Chưa có phiên bản tỷ lệ nào."}
                met={hasVersion}
                title="Có ít nhất một phiên bản tỷ lệ"
              />
              <ConditionRow
                description="Các phiên bản tỷ lệ không bị trùng thời gian hiệu lực."
                met={!hasOverlap}
                title="Không trùng khoảng hiệu lực"
              />
              <ConditionRow
                description={totalRate === null ? "Chưa xác định tổng phí hiện tại." : `Tổng phí đối tác đang chịu: ${formatFeePercent(totalRate)} (≤ 30%).`}
                met={withinLimit}
                title="Tổng phí không vượt 30%"
              />
              {feeType && feeType.status === "ACTIVE" && canActivate ? (
                <Button variant="secondary" onClick={() => setConfirmKind("deactivate")}>
                  <i className="fas fa-ban" aria-hidden="true" /> Ngừng áp dụng
                </Button>
              ) : null}
              {feeType && feeType.status !== "ACTIVE" && feeType.status !== "ARCHIVED" && canActivate ? (
                <Button onClick={() => setConfirmKind("activate")}>
                  <i className="fas fa-play" aria-hidden="true" /> Kích hoạt loại phí
                </Button>
              ) : null}
            </Card>
          </section>

          <div className="tw-flex tw-items-start tw-gap-2.5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50/70 tw-px-4 tw-py-3">
            <i className="far fa-info-circle tw-mt-0.5 tw-text-[1rem] tw-text-vm-primary" aria-hidden="true" />
            <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
              Thay đổi chỉ được phép khi phí đang ở trạng thái Bản nháp.
            </p>
          </div>
        </>
      ) : null}

      {tab === "versions" ? <PlatformFeeVersionsSection bare feeTypeId={feeTypeId} onBack={onBack} /> : null}

      {tab === "history" ? (
        <Card className="tw-p-5">
          {historyEvents.length === 0 ? (
            <p className="tw-m-0 tw-py-6 tw-text-center tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
              Chưa có thay đổi nào được ghi nhận.
            </p>
          ) : (
            <ol className="tw-m-0 tw-grid tw-gap-0 tw-p-0" style={{ listStyle: "none" }}>
              {historyEvents.map((event, index) => (
                <li className="tw-flex tw-gap-3" key={event.key}>
                  <span className="tw-flex tw-flex-col tw-items-center">
                    <span
                      className={cn(
                        "tw-mt-1 tw-h-2.5 tw-w-2.5 tw-flex-none tw-rounded-full",
                        event.tone === "green" ? "tw-bg-green-500"
                          : event.tone === "amber" ? "tw-bg-amber-500"
                            : event.tone === "red" ? "tw-bg-red-500"
                              : event.tone === "blue" ? "tw-bg-vm-primary"
                                : "tw-bg-vm-slate-200",
                      )}
                    />
                    {index === historyEvents.length - 1 ? null : (
                      <span className="tw-w-px tw-flex-1 tw-bg-vm-slate-100" />
                    )}
                  </span>
                  <span className="tw-grid tw-gap-0.5 tw-pb-5">
                    <strong className="tw-text-[0.88rem] tw-font-bold tw-text-vm-slate-900">{event.title}</strong>
                    <span className="tw-text-[0.78rem] tw-font-semibold tw-text-vm-slate-500">
                      {getActorLabel(event.actorId, user)} • {formatFeeDateTime(event.time) ?? "—"}
                    </span>
                  </span>
                </li>
              ))}
            </ol>
          )}
        </Card>
      ) : null}

      <ConfirmDialog
        cancelLabel="Hủy"
        confirmLabel={confirmKind === "archive" ? "Lưu trữ" : confirmKind === "deactivate" ? "Ngừng áp dụng" : "Kích hoạt"}
        loading={confirming}
        message={confirmKind === "archive"
          ? (<>Lưu trữ phí <strong>{feeType?.feeCode}</strong>? Phí đã lưu trữ không thể khôi phục.</>)
          : confirmKind === "deactivate"
            ? (<>Ngừng áp dụng phí <strong>{feeType?.feeCode}</strong>?</>)
            : (<>Kích hoạt phí <strong>{feeType?.feeCode}</strong>? Phí cần thỏa mãn các điều kiện kích hoạt.</>)}
        open={confirmKind !== null}
        title={confirmKind === "archive" ? "Lưu trữ phí" : confirmKind === "deactivate" ? "Ngừng áp dụng" : "Kích hoạt phí"}
        tone={confirmKind === "archive" ? "danger" : "default"}
        onCancel={() => {
          if (!confirming) setConfirmKind(null);
        }}
        onConfirm={() => void handleConfirm()}
      />
    </div>
  );
}
