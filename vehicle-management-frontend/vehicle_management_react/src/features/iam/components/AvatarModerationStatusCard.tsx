import type { AvatarModerationStatus } from "@/features/iam/api/accountProfileApi";
import { resolvePublicMediaUrl } from "@/shared/utils/mediaUrl";

const statusMeta = {
  PENDING: { label: "Ảnh mới đang chờ duyệt", tone: "tw-border-amber-200 tw-bg-amber-50 tw-text-amber-900", icon: "far fa-clock" },
  APPROVED: { label: "Ảnh đại diện đã được duyệt", tone: "tw-border-green-200 tw-bg-green-50 tw-text-green-800", icon: "fas fa-check-circle" },
  REJECTED: { label: "Ảnh đại diện bị từ chối", tone: "tw-border-red-200 tw-bg-red-50 tw-text-red-800", icon: "fas fa-times-circle" },
  CANCELLED: { label: "Yêu cầu duyệt ảnh đã hủy", tone: "tw-border-slate-200 tw-bg-slate-50 tw-text-slate-700", icon: "fas fa-ban" },
} as const;

export function AvatarModerationStatusCard({ status }: { status: AvatarModerationStatus | null }) {
  if (!status) return null;
  const meta = statusMeta[status.approvalStatus];
  const candidateUrl = resolvePublicMediaUrl(status.candidatePreviewUrl);

  return (
    <div className={`tw-mt-3 tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left ${meta.tone}`}>
      {candidateUrl ? <img alt="Ảnh đang chờ duyệt" className="tw-h-12 tw-w-12 tw-flex-none tw-rounded-full tw-object-cover" src={candidateUrl} /> : <i className={`${meta.icon} tw-text-lg`} />}
      <div className="tw-min-w-0">
        <strong className="tw-block tw-text-[0.82rem] tw-font-black">{meta.label}</strong>
        <small className="tw-mt-1 tw-block tw-text-[0.74rem] tw-font-semibold">
          {status.approvalStatus === "PENDING" ? "Avatar hiện tại vẫn được giữ cho đến khi ảnh mới được duyệt." : status.reviewNote || "Trạng thái ảnh đại diện đã được cập nhật."}
        </small>
      </div>
    </div>
  );
}
