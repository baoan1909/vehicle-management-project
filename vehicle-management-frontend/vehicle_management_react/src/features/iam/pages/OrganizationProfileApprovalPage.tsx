import { useCallback, useEffect, useMemo, useState } from "react";

import { Badge, Button, Card, InfoBanner, Modal, SelectMenu, useToast } from "@/components/ui";
import { formatDateTimeLabel } from "@/components/ui";
import {
  fetchOrganizationProfileUpdateRequests,
  reviewOrganizationProfileUpdate,
  type ApprovalStatus,
  type OrganizationProfileUpdateRequest,
} from "@/features/iam/api/organizationProfileApi";

const options = [
  { label: "Tất cả trạng thái", value: "all" },
  { label: "Chờ duyệt", value: "PENDING" },
  { label: "Đã duyệt", value: "APPROVED" },
  { label: "Từ chối", value: "REJECTED" },
];

function label(status: ApprovalStatus) { return ({ PENDING: "Chờ duyệt", APPROVED: "Đã duyệt", REJECTED: "Từ chối", CANCELLED: "Đã hủy" })[status]; }
function tone(status: ApprovalStatus): "success" | "warning" | "danger" {
  return status === "APPROVED" ? "success" : status === "PENDING" ? "warning" : "danger";
}

export function OrganizationProfileApprovalPage() {
  const toast = useToast();
  const [status, setStatus] = useState<ApprovalStatus | "all">("PENDING");
  const [requests, setRequests] = useState<OrganizationProfileUpdateRequest[]>([]);
  const [selected, setSelected] = useState<OrganizationProfileUpdateRequest | null>(null);
  const [note, setNote] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setIsLoading(true); setError("");
    try { setRequests(await fetchOrganizationProfileUpdateRequests(status === "all" ? undefined : status)); }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể tải yêu cầu cập nhật hồ sơ."); }
    finally { setIsLoading(false); }
  }, [status]);
  useEffect(() => { void load(); }, [load]);
  const summary = useMemo(() => ({ pending: requests.filter((item) => item.status === "PENDING").length, total: requests.length }), [requests]);

  async function review(decision: "approve" | "reject") {
    if (!selected) return;
    if (decision === "reject" && !note.trim()) { setError("Cần nhập lý do từ chối để Partner Admin biết cách cập nhật lại."); return; }
    setIsSaving(true); setError("");
    try {
      await reviewOrganizationProfileUpdate(selected.approvalRequestId, decision, note);
      toast.success(decision === "approve" ? "Hồ sơ đối tác đã được cập nhật." : "Đã từ chối yêu cầu cập nhật.", decision === "approve" ? "Duyệt thành công" : "Đã từ chối");
      setSelected(null); setNote(""); await load();
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể xử lý yêu cầu."); }
    finally { setIsSaving(false); }
  }

  return <div className="tw-px-4 tw-py-4 lg:tw-px-5"><section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1400px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
    <div className="tw-flex tw-items-start tw-justify-between tw-gap-4 max-[640px]:tw-flex-col"><div><p className="tw-m-0 tw-text-[0.74rem] tw-font-black tw-uppercase tw-tracking-[0.12em] tw-text-vm-primary">Quản trị nền tảng</p><h1 className="tw-m-0 tw-mt-1 tw-text-vm-page-title tw-text-vm-slate-900">Duyệt cập nhật hồ sơ đối tác</h1><p className="tw-m-0 tw-mt-2 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-500">Hồ sơ đang hoạt động chỉ thay đổi sau khi System Admin duyệt.</p></div><Button variant="secondary" disabled={isLoading} onClick={() => void load()}><i className="fas fa-sync-alt" />Làm mới</Button></div>
    <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-4 max-[560px]:tw-grid-cols-1"><Card className="tw-p-4"><p className="tw-m-0 tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">Yêu cầu đang hiển thị</p><strong className="tw-mt-2 tw-block tw-text-[1.8rem] tw-font-black">{summary.total}</strong></Card><Card className="tw-p-4"><p className="tw-m-0 tw-text-[0.8rem] tw-font-bold tw-text-vm-slate-500">Chờ System Admin duyệt</p><strong className="tw-mt-2 tw-block tw-text-[1.8rem] tw-font-black tw-text-amber-600">{summary.pending}</strong></Card></div>
    <Card className="tw-mt-4 tw-p-4"><div className="tw-flex tw-flex-wrap tw-items-end tw-justify-between tw-gap-3"><div className="tw-w-[230px]"><p className="tw-m-0 tw-mb-2 tw-text-[0.74rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Trạng thái</p><SelectMenu ariaLabel="Trạng thái" value={status} options={options} onChange={(value) => setStatus(value as ApprovalStatus | "all")} /></div><Button variant="secondary" onClick={() => setStatus("PENDING")}><i className="fas fa-undo" />Đặt lại</Button></div></Card>
    <Card className="tw-mt-4 tw-overflow-hidden"><div className="tw-grid tw-grid-cols-[minmax(250px,1fr)_minmax(250px,1fr)_130px_110px] tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-px-4 tw-py-3 tw-text-[0.72rem] tw-font-black tw-uppercase tw-text-vm-slate-500 max-[900px]:tw-hidden"><span>Đối tác</span><span>Thông tin đề xuất</span><span>Trạng thái</span><span className="tw-text-right">Thao tác</span></div>
      {isLoading ? <div className="tw-p-4"><InfoBanner tone="info" title="Đang tải yêu cầu" description="Vui lòng chờ trong giây lát." /></div> : null}{error && !selected ? <div className="tw-p-4"><InfoBanner tone="warning" title="Không thể tải yêu cầu" description={error} /></div> : null}{!isLoading && !error && requests.length === 0 ? <div className="tw-p-4"><InfoBanner tone="success" title="Không có yêu cầu phù hợp" description="Không có yêu cầu cập nhật hồ sơ theo bộ lọc hiện tại." /></div> : null}
      {!isLoading && !error ? requests.map((item) => <article key={item.approvalRequestId} className="tw-grid tw-grid-cols-[minmax(250px,1fr)_minmax(250px,1fr)_130px_110px] tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-4 tw-py-3 last:tw-border-b-0 max-[900px]:tw-grid-cols-1"><div><strong className="tw-block tw-text-vm-slate-900">{item.organizationName}</strong><span className="tw-text-[0.78rem] tw-font-bold tw-text-vm-slate-500">{item.organizationCode}</span></div><div className="tw-min-w-0"><strong className="tw-block tw-truncate tw-text-[0.86rem]">{item.addressDisplay}</strong><span className="tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">Gửi {formatDateTimeLabel(item.submittedAt)}</span></div><Badge tone={tone(item.status)} className="tw-w-fit tw-rounded-full tw-px-3">{label(item.status)}</Badge><div className="tw-justify-self-end max-[900px]:tw-justify-self-start"><Button size="sm" variant={item.status === "PENDING" ? "primary" : "secondary"} onClick={() => { setSelected(item); setNote(""); setError(""); }}><i className="fas fa-eye" />{item.status === "PENDING" ? "Xét duyệt" : "Chi tiết"}</Button></div></article>) : null}</Card>
  </section><Modal open={selected !== null} onClose={() => { if (!isSaving) setSelected(null); }} title="Yêu cầu cập nhật hồ sơ" description="So sánh thông tin đề xuất trước khi cập nhật hồ sơ đang hoạt động." width="lg" actions={<div className="tw-flex tw-justify-end tw-gap-3"><Button variant="secondary" disabled={isSaving} onClick={() => setSelected(null)}>Đóng</Button>{selected?.status === "PENDING" ? <><Button variant="danger" disabled={isSaving} onClick={() => void review("reject")}><i className="fas fa-times" />Từ chối</Button><Button loading={isSaving} onClick={() => void review("approve")}>{!isSaving ? <i className="fas fa-check" /> : null}Duyệt cập nhật</Button></> : null}</div>}>
    {selected ? <div className="tw-grid tw-gap-4"><Field label="Mã đối tác" value={selected.organizationCode} /><div className="tw-grid tw-gap-3"><ComparisonField label="Tên đối tác" currentValue={selected.currentOrganizationName || "Không có dữ liệu hiện hành"} proposedValue={selected.organizationName} /><ComparisonField label="Địa chỉ" currentValue={selected.currentAddressDisplay || "Không có dữ liệu hiện hành"} proposedValue={selected.addressDisplay} /></div><Field label="Thời điểm gửi yêu cầu" value={formatDateTimeLabel(selected.submittedAt)} /><label className="tw-grid tw-gap-2"><span className="tw-text-[0.75rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Ghi chú xét duyệt</span><textarea disabled={selected.status !== "PENDING" || isSaving} value={selected.status === "PENDING" ? note : selected.note ?? ""} onChange={(event) => setNote(event.target.value)} maxLength={500} className="tw-min-h-[90px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-p-3 tw-font-semibold tw-outline-none focus:tw-border-vm-primary" placeholder="Bắt buộc khi từ chối..." /></label>{error ? <InfoBanner tone="warning" title="Không thể xử lý yêu cầu" description={error} /> : null}</div> : null}
  </Modal></div>;
}

function Field({ label: fieldLabel, value }: { label: string; value: string }) { return <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3"><p className="tw-m-0 tw-text-[0.7rem] tw-font-black tw-uppercase tw-text-vm-slate-500">{fieldLabel}</p><strong className="tw-mt-1 tw-block tw-break-words tw-text-[0.9rem] tw-text-vm-slate-900">{value || "-"}</strong></div>; }

function ComparisonField({ label: fieldLabel, currentValue, proposedValue }: { label: string; currentValue: string; proposedValue: string }) { return <div className="tw-grid tw-grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] tw-items-stretch tw-gap-3 max-[620px]:tw-grid-cols-1"><div className="tw-min-w-0 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3"><p className="tw-m-0 tw-text-[0.68rem] tw-font-black tw-uppercase tw-text-vm-slate-400">Hiện hành</p><strong className="tw-mt-1 tw-block tw-break-words tw-text-[0.9rem] tw-text-vm-slate-900">{currentValue}</strong></div><div className="tw-flex tw-min-w-[112px] tw-flex-col tw-items-center tw-justify-center tw-gap-1 tw-px-1 tw-text-center max-[620px]:tw-flex-row max-[620px]:tw-py-1"><span className="tw-text-[0.68rem] tw-font-black tw-uppercase tw-text-vm-slate-500">{fieldLabel}</span><i className="fas fa-arrow-right tw-text-vm-primary max-[620px]:tw-rotate-90" aria-hidden="true" /></div><div className="tw-min-w-0 tw-rounded-vm-md tw-border tw-border-solid tw-border-brand-100 tw-bg-brand-50 tw-p-3"><p className="tw-m-0 tw-text-[0.68rem] tw-font-black tw-uppercase tw-text-vm-primary">Đề xuất</p><strong className="tw-mt-1 tw-block tw-break-words tw-text-[0.9rem] tw-text-vm-slate-900">{proposedValue}</strong></div></div>; }
