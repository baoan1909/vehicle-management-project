import { useCallback, useEffect, useState } from "react";

import { Badge, Button, Card, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { fetchAvatarApprovals, reviewAvatarApproval } from "@/features/iam/api/avatarApprovalApi";
import type { AvatarModerationStatus } from "@/features/iam/api/accountProfileApi";
import { subscribeNotificationReceived } from "@/features/notifications/utils/notificationEvents";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { formatInApplicationTime } from "@/shared/time/applicationTime";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

type FilterStatus = AvatarModerationStatus["approvalStatus"] | "ALL";

const statusOptions = [
  { label: "Chờ duyệt", value: "PENDING" },
  { label: "Tất cả", value: "ALL" },
  { label: "Đã duyệt", value: "APPROVED" },
  { label: "Từ chối", value: "REJECTED" },
  { label: "Đã hủy", value: "CANCELLED" },
] satisfies Array<{ label: string; value: FilterStatus }>;

function statusLabel(status: AvatarModerationStatus["approvalStatus"]) {
  if (status === "APPROVED") return "Đã duyệt";
  if (status === "REJECTED") return "Từ chối";
  if (status === "CANCELLED") return "Đã hủy";
  return "Chờ duyệt";
}

function statusTone(status: AvatarModerationStatus["approvalStatus"]) {
  if (status === "APPROVED") return "success" as const;
  if (status === "PENDING") return "warning" as const;
  if (status === "REJECTED") return "danger" as const;
  return "neutral" as const;
}

export function AvatarApprovalManagementPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [items, setItems] = useState<AvatarModerationStatus[]>([]);
  const [filter, setFilter] = useState<FilterStatus>("PENDING");
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [error, setError] = useState("");
  const canReview = hasAnyPermission(user, ["USER_PROFILE_UPDATE_ALL"]);

  const loadItems = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setItems(await fetchAvatarApprovals(filter));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải danh sách ảnh chờ duyệt.");
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => { void loadItems(); }, [loadItems]);

  useEffect(() => subscribeNotificationReceived((notification) => {
    if (notification.notificationType === "AVATAR_APPROVAL_SUBMITTED") void loadItems();
  }), [loadItems]);

  const review = async (item: AvatarModerationStatus, decision: "approve" | "reject") => {
    const note = notes[item.approvalRequestId]?.trim();
    if (decision === "reject" && !note) {
      setError("Vui lòng nhập lý do từ chối ảnh đại diện.");
      return;
    }
    setSavingId(item.approvalRequestId);
    setError("");
    try {
      await reviewAvatarApproval(item.approvalRequestId, decision, note);
      toast.success(decision === "approve" ? "Đã duyệt ảnh đại diện." : "Đã từ chối ảnh đại diện.");
      await loadItems();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể cập nhật trạng thái ảnh đại diện.");
    } finally {
      setSavingId(null);
    }
  };

  return (
    <section className="content tw-pb-8 tw-pt-4">
      <div className="container-fluid tw-max-w-[1480px]">
        <Card className="tw-rounded-vm-lg tw-border tw-border-solid !tw-border-vm-slate-100 tw-p-5">
          <header className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-4">
            <div>
              <p className="tw-m-0 tw-text-xs tw-font-black tw-uppercase tw-tracking-[.12em] tw-text-vm-primary">Quản trị hồ sơ</p>
              <h1 className="tw-m-0 tw-mt-1 tw-text-vm-page-title tw-font-black tw-text-vm-slate-900">Duyệt ảnh đại diện</h1>
              <p className="tw-mb-0 tw-mt-2 tw-text-sm tw-font-semibold tw-text-vm-slate-500">Ảnh cũ tiếp tục hiển thị cho đến khi ảnh mới được phê duyệt.</p>
            </div>
            <div className="tw-flex tw-items-center tw-gap-3">
              <div className="tw-w-48"><SelectMenu ariaLabel="Lọc trạng thái duyệt ảnh" options={statusOptions} value={filter} onChange={(value) => setFilter(value as FilterStatus)} /></div>
              <Button loading={loading} type="button" variant="secondary" onClick={() => void loadItems()}><i className="fas fa-sync-alt" /> Làm mới</Button>
            </div>
          </header>

          {error ? <div className="tw-mt-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-font-semibold tw-text-red-700">{error}</div> : null}
          {!loading && items.length === 0 ? <div className="tw-mt-5 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-10 tw-text-center tw-font-semibold tw-text-vm-slate-500">Không có ảnh đại diện ở trạng thái này.</div> : null}

          <div className="tw-mt-5 tw-grid tw-grid-cols-3 tw-gap-4 max-[1150px]:tw-grid-cols-2 max-[720px]:tw-grid-cols-1">
            {items.map((item) => {
              const candidateUrl = resolvePublicMediaUrl(item.candidatePreviewUrl);
              const displayedUrl = resolvePublicMediaUrl(item.displayedAvatarUrl);
              return (
                <article key={item.approvalRequestId} className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4 tw-shadow-[0_12px_30px_rgba(15,23,42,.06)]">
                  <div className="tw-flex tw-items-start tw-justify-between tw-gap-3"><Badge tone={statusTone(item.approvalStatus)}>{statusLabel(item.approvalStatus)}</Badge><small className="tw-text-vm-slate-500">{formatInApplicationTime(item.submittedAt, "vi-VN", { dateStyle: "short", timeStyle: "short" })}</small></div>
                  <div className="tw-mt-4 tw-grid tw-grid-cols-2 tw-gap-3">
                    <div className="tw-text-center"><span className="tw-mb-2 tw-block tw-text-xs tw-font-black tw-uppercase tw-text-vm-slate-500">Đang hiển thị</span>{displayedUrl ? <img alt="Avatar hiện tại" className="tw-mx-auto tw-h-28 tw-w-28 tw-rounded-full tw-object-cover" src={displayedUrl} /> : <div className="tw-mx-auto tw-grid tw-h-28 tw-w-28 tw-place-items-center tw-rounded-full tw-bg-vm-slate-100"><i className="far fa-user tw-text-3xl tw-text-vm-slate-400" /></div>}</div>
                    <div className="tw-text-center"><span className="tw-mb-2 tw-block tw-text-xs tw-font-black tw-uppercase tw-text-vm-slate-500">Ảnh đề nghị</span>{candidateUrl ? <img alt="Avatar đề nghị" className="tw-mx-auto tw-h-28 tw-w-28 tw-rounded-full tw-object-cover" src={candidateUrl} /> : <div className="tw-mx-auto tw-grid tw-h-28 tw-w-28 tw-place-items-center tw-rounded-full tw-bg-vm-slate-100"><i className="far fa-image tw-text-3xl tw-text-vm-slate-400" /></div>}</div>
                  </div>
                  <p className="tw-mb-0 tw-mt-4 tw-break-all tw-text-xs tw-font-semibold tw-text-vm-slate-500">Tài khoản: {item.ownerAccountId}</p>
                  {item.approvalStatus === "PENDING" && canReview ? <><textarea className="tw-mt-3 tw-min-h-20 tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-200 tw-p-3 tw-text-sm" placeholder="Ghi chú duyệt hoặc lý do từ chối" value={notes[item.approvalRequestId] ?? ""} onChange={(event) => setNotes((current) => ({ ...current, [item.approvalRequestId]: event.target.value }))} /><div className="tw-mt-3 tw-grid tw-grid-cols-2 tw-gap-3"><Button loading={savingId === item.approvalRequestId} type="button" onClick={() => void review(item, "approve")}><i className="fas fa-check" /> Duyệt</Button><Button disabled={savingId === item.approvalRequestId} type="button" variant="danger" onClick={() => void review(item, "reject")}><i className="fas fa-times" /> Từ chối</Button></div></> : null}
                  {item.reviewNote ? <p className="tw-mb-0 tw-mt-3 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3 tw-text-sm tw-font-semibold tw-text-vm-slate-700">{item.reviewNote}</p> : null}
                </article>
              );
            })}
          </div>
        </Card>
      </div>
    </section>
  );
}
