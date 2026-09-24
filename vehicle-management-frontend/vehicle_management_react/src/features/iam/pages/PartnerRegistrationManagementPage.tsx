import { useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, InfoBanner, Modal, SearchInput, SelectMenu, useToast } from "@/components/ui";
import {
  fetchPartnerRegistrations,
  reviewPartnerRegistration,
  type PartnerRegistration,
  type PartnerRegistrationStatus,
} from "@/features/iam/api/partnerRegistrationApi";
import { cn } from "@/lib/cn";

type BadgeTone = "primary" | "success" | "warning" | "danger" | "neutral";
type ReviewDecision = "approve" | "reject";

const statusOptions: Array<{ label: string; value: PartnerRegistrationStatus | "all" }> = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Chờ duyệt", value: "PENDING" },
  { label: "Đã duyệt", value: "APPROVED" },
  { label: "Từ chối", value: "REJECTED" },
];

function statusLabel(status: PartnerRegistrationStatus) {
  const labels: Record<PartnerRegistrationStatus, string> = {
    APPROVED: "Đã duyệt",
    CANCELLED: "Đã hủy",
    PENDING: "Chờ duyệt",
    REJECTED: "Từ chối",
  };
  return labels[status];
}

function statusTone(status: PartnerRegistrationStatus): BadgeTone {
  if (status === "APPROVED") return "success";
  if (status === "PENDING") return "warning";
  if (status === "REJECTED" || status === "CANCELLED") return "danger";
  return "neutral";
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function DetailField({ label, value, wide = false }: { label: string; value?: string | number | null; wide?: boolean }) {
  const displayValue = value == null || String(value).trim() === "" ? "-" : value;
  return (
    <div className={cn("tw-min-w-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-2.5", wide ? "sm:tw-col-span-2" : "")}>
      <p className="tw-m-0 tw-text-[0.7rem] tw-font-black tw-uppercase tw-text-vm-slate-500">{label}</p>
      <strong className="tw-mt-1 tw-block tw-break-words tw-text-[0.88rem] tw-font-bold tw-leading-5 tw-text-vm-slate-900">{displayValue}</strong>
    </div>
  );
}

function RegistrationModal({
  error,
  isSaving,
  item,
  note,
  onClose,
  onNoteChange,
  onReview,
}: {
  error: string;
  isSaving: boolean;
  item: PartnerRegistration | null;
  note: string;
  onClose: () => void;
  onNoteChange: (value: string) => void;
  onReview: (decision: ReviewDecision) => void;
}) {
  if (!item) return null;
  const canReview = item.status === "PENDING";

  return (
    <Modal
      open
      onClose={isSaving ? () => undefined : onClose}
      title="Hồ sơ đăng ký đối tác"
      description="Kiểm tra thông tin đơn vị trước khi cấp quyền quản trị đối tác."
      width="lg"
      actions={(
        <div className="tw-flex tw-flex-wrap tw-justify-end tw-gap-3">
          <Button disabled={isSaving} variant="secondary" onClick={onClose}>Đóng</Button>
          {canReview ? (
            <>
              <Button disabled={isSaving} variant="danger" onClick={() => onReview("reject")}>
                <i className="fas fa-times" />
                Từ chối
              </Button>
              <Button loading={isSaving} onClick={() => onReview("approve")}>
                {!isSaving ? <i className="fas fa-check" /> : null}
                Duyệt & cấp Partner Admin
              </Button>
            </>
          ) : null}
        </div>
      )}
    >
      <div className="tw-grid tw-gap-5">
        <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3">
          <div>
            <p className="tw-m-0 tw-text-[0.75rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Đơn vị đăng ký</p>
            <h4 className="tw-m-0 tw-mt-1 tw-text-[1.05rem] tw-font-black tw-text-vm-slate-900">{item.organizationName}</h4>
          </div>
          <Badge tone={statusTone(item.status)} className="tw-rounded-full tw-px-3">{statusLabel(item.status)}</Badge>
        </div>

        <section>
          <h5 className="tw-m-0 tw-mb-3 tw-text-[0.8rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Thông tin đối tác</h5>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[560px]:tw-grid-cols-1">
            <DetailField label="Mã đối tác" value={item.organizationCode} />
            <DetailField label="Số bãi dự kiến" value={item.expectedParkingLotCount} />
            <DetailField label="Địa chỉ" value={item.address} wide />
            <DetailField label="Mô tả vận hành" value={item.parkingOperationDescription} wide />
          </div>
        </section>

        <section>
          <h5 className="tw-m-0 tw-mb-3 tw-text-[0.8rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Người đại diện & tài khoản cấp mới</h5>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[560px]:tw-grid-cols-1">
            <DetailField label="Người đại diện" value={item.representativeName} />
            <DetailField label="Số điện thoại" value={item.phoneNumber} />
            <DetailField label="Email / tên đăng nhập" value={item.email} wide />
            <DetailField label="Thời điểm gửi yêu cầu" value={formatDateTime(item.createdAt)} wide />
          </div>
        </section>

        <label className="tw-grid tw-gap-1.5">
          <span className="tw-text-[0.75rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Ghi chú xét duyệt{canReview ? "" : ""}</span>
          <textarea
            className="tw-min-h-[88px] tw-resize-none tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)] disabled:tw-bg-vm-slate-25"
            disabled={isSaving || !canReview}
            maxLength={255}
            onChange={(event) => onNoteChange(event.target.value)}
            placeholder={canReview ? "Ghi chú nội bộ hoặc lý do từ chối..." : "Không có ghi chú."}
            value={canReview ? note : item.note ?? ""}
          />
        </label>
        {error ? <InfoBanner tone="warning" title="Không thể cập nhật hồ sơ" description={error} icon={<i className="fas fa-exclamation-circle" />} /> : null}
      </div>
    </Modal>
  );
}

export function PartnerRegistrationManagementPage() {
  const toast = useToast();
  const [registrations, setRegistrations] = useState<PartnerRegistration[]>([]);
  const [status, setStatus] = useState<PartnerRegistrationStatus | "all">("PENDING");
  const [keyword, setKeyword] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedRegistration, setSelectedRegistration] = useState<PartnerRegistration | null>(null);
  const [reviewNote, setReviewNote] = useState("");
  const [reviewError, setReviewError] = useState("");
  const [isReviewSaving, setIsReviewSaving] = useState(false);

  async function loadRegistrations() {
    setIsLoading(true);
    setError("");
    try {
      setRegistrations(await fetchPartnerRegistrations(status === "all" ? undefined : status));
    } catch (requestError) {
      setRegistrations([]);
      setError(requestError instanceof Error ? requestError.message : "Không thể tải danh sách đăng ký đối tác.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadRegistrations();
  }, [status]);

  const visibleRegistrations = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase("vi-VN");
    if (!normalizedKeyword) return registrations;
    return registrations.filter((item) => [
      item.organizationCode,
      item.organizationName,
      item.representativeName,
      item.email,
      item.phoneNumber,
    ].some((value) => value.toLocaleLowerCase("vi-VN").includes(normalizedKeyword)));
  }, [keyword, registrations]);

  const metrics = useMemo(() => ({
    total: registrations.length,
    pending: registrations.filter((item) => item.status === "PENDING").length,
    approved: registrations.filter((item) => item.status === "APPROVED").length,
    rejected: registrations.filter((item) => item.status === "REJECTED").length,
  }), [registrations]);

  function openRegistration(item: PartnerRegistration) {
    setSelectedRegistration(item);
    setReviewNote("");
    setReviewError("");
  }

  async function handleReview(decision: ReviewDecision) {
    if (!selectedRegistration) return;
    if (decision === "reject" && !reviewNote.trim()) {
      setReviewError("Vui lòng nhập lý do từ chối để đối tác biết cách xử lý tiếp theo.");
      return;
    }

    setIsReviewSaving(true);
    setReviewError("");
    try {
      await reviewPartnerRegistration(selectedRegistration.approvalRequestId, decision, reviewNote);
      toast.success(
        decision === "approve" ? "Đã tạo Partner và cấp tài khoản Partner Admin." : "Đã từ chối yêu cầu đăng ký đối tác.",
        decision === "approve" ? "Duyệt thành công" : "Đã từ chối",
      );
      setSelectedRegistration(null);
      setReviewNote("");
      await loadRegistrations();
    } catch (requestError) {
      setReviewError(requestError instanceof Error ? requestError.message : "Không thể cập nhật hồ sơ đối tác.");
    } finally {
      setIsReviewSaving(false);
    }
  }

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1500px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-4 max-[760px]:tw-flex-col max-[760px]:tw-items-stretch">
          <div>
            <p className="tw-m-0 tw-text-[0.74rem] tw-font-black tw-uppercase tw-tracking-[0.12em] tw-text-vm-primary">Nền tảng CoParking</p>
            <h1 className="tw-m-0 tw-mt-1 tw-text-vm-page-title tw-text-vm-slate-900">Đăng ký đối tác</h1>
            <p className="tw-m-0 tw-mt-2 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-500">Xét duyệt đơn vị vận hành trước khi cấp quyền Partner Admin.</p>
          </div>
          <Button variant="secondary" disabled={isLoading} onClick={() => void loadRegistrations()}>
            <i className="fas fa-sync-alt" />
            Làm mới
          </Button>
        </div>

        <div className="tw-mt-5 tw-grid tw-grid-cols-4 tw-gap-4 max-[1100px]:tw-grid-cols-2 max-[580px]:tw-grid-cols-1">
          {[
            ["fas fa-layer-group", "Tổng hồ sơ", metrics.total, "tw-bg-brand-50 tw-text-vm-primary"],
            ["far fa-clock", "Chờ duyệt", metrics.pending, "tw-bg-amber-50 tw-text-amber-600"],
            ["fas fa-check-circle", "Đã duyệt", metrics.approved, "tw-bg-emerald-50 tw-text-emerald-600"],
            ["fas fa-times-circle", "Từ chối", metrics.rejected, "tw-bg-red-50 tw-text-red-600"],
          ].map(([icon, label, value, colorClass]) => (
            <Card key={String(label)} className="tw-min-h-[82px] tw-p-4">
              <div className="tw-flex tw-items-center tw-gap-3">
                <span className={cn("tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-vm-md", String(colorClass))}><i className={String(icon)} /></span>
                <div>
                  <p className="tw-m-0 tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-500">{label}</p>
                  <strong className="tw-mt-1 tw-block tw-text-[1.55rem] tw-font-black tw-leading-none tw-text-vm-slate-900">{value}</strong>
                </div>
              </div>
            </Card>
          ))}
        </div>

        <Card className="tw-mt-5 tw-p-4">
          <div className="tw-grid tw-grid-cols-[minmax(260px,1fr)_220px_auto] tw-gap-3 max-[780px]:tw-grid-cols-1">
            <SearchInput
              aria-label="Tìm hồ sơ đối tác"
              containerClassName="tw-h-[42px]"
              onChange={setKeyword}
              placeholder="Tìm mã, tên đối tác, người đại diện..."
              value={keyword}
            />
            <SelectMenu ariaLabel="Trạng thái hồ sơ" value={status} options={statusOptions} onChange={(value) => setStatus(value as PartnerRegistrationStatus | "all")} />
            <Button className="tw-h-[42px]" variant="secondary" onClick={() => { setKeyword(""); setStatus("PENDING"); }}>
              <i className="fas fa-undo" />
              Đặt lại
            </Button>
          </div>
        </Card>

        <Card className="tw-mt-4 tw-overflow-hidden">
          <div className="tw-grid tw-grid-cols-[minmax(230px,1.1fr)_minmax(220px,1fr)_155px_135px_110px] tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3 tw-text-[0.72rem] tw-font-extrabold tw-uppercase tw-text-vm-slate-500 max-[1080px]:tw-hidden">
            <span>Đơn vị đối tác</span>
            <span>Người đại diện</span>
            <span>Quy mô dự kiến</span>
            <span>Trạng thái</span>
            <span className="tw-text-right">Thao tác</span>
          </div>
          {isLoading ? <div className="tw-p-4"><InfoBanner tone="info" title="Đang tải hồ sơ" description="Vui lòng chờ trong giây lát." icon={<i className="fas fa-spinner fa-spin" />} /></div> : null}
          {!isLoading && error ? <div className="tw-p-4"><InfoBanner tone="warning" title="Không thể tải hồ sơ" description={error} icon={<i className="fas fa-exclamation-circle" />} /></div> : null}
          {!isLoading && !error && visibleRegistrations.length === 0 ? <div className="tw-p-4"><InfoBanner tone="success" title="Không có hồ sơ phù hợp" description="Không có đăng ký đối tác nào theo bộ lọc hiện tại." icon={<i className="fas fa-check-circle" />} /></div> : null}
          {!isLoading && !error ? visibleRegistrations.map((item) => (
            <article key={item.approvalRequestId} className="tw-grid tw-grid-cols-[minmax(230px,1.1fr)_minmax(220px,1fr)_155px_135px_110px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3 last:tw-border-b-0 max-[1080px]:tw-grid-cols-1">
              <div className="tw-min-w-0">
                <div className="tw-flex tw-flex-wrap tw-items-center tw-gap-2"><strong className="tw-truncate tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">{item.organizationName}</strong><Badge tone="neutral" className="tw-rounded-full tw-px-2.5">{item.organizationCode}</Badge></div>
                <p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.77rem] tw-font-semibold tw-text-vm-slate-500">Gửi lúc {formatDateTime(item.createdAt)}</p>
              </div>
              <div className="tw-min-w-0"><strong className="tw-block tw-truncate tw-text-[0.88rem] tw-font-bold tw-text-vm-slate-900">{item.representativeName}</strong><p className="tw-m-0 tw-mt-1 tw-truncate tw-text-[0.77rem] tw-font-semibold tw-text-vm-slate-500">{item.email} · {item.phoneNumber}</p></div>
              <span className="tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-700">{item.expectedParkingLotCount} bãi xe</span>
              <Badge tone={statusTone(item.status)} className="tw-w-fit tw-rounded-full tw-px-3">{statusLabel(item.status)}</Badge>
              <div className="tw-justify-self-end max-[1080px]:tw-justify-self-start"><Button className="tw-w-[104px] tw-gap-1.5 tw-px-2 tw-text-[0.78rem]" size="sm" variant={item.status === "PENDING" ? "primary" : "secondary"} onClick={() => openRegistration(item)}><i className="fas fa-eye" />{item.status === "PENDING" ? "Xét duyệt" : "Chi tiết"}</Button></div>
            </article>
          )) : null}
        </Card>
      </section>

      <RegistrationModal
        error={reviewError}
        isSaving={isReviewSaving}
        item={selectedRegistration}
        note={reviewNote}
        onClose={() => { if (!isReviewSaving) setSelectedRegistration(null); }}
        onNoteChange={setReviewNote}
        onReview={(decision) => void handleReview(decision)}
      />
    </div>
  );
}
