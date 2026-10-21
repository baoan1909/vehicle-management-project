import { useCallback, useEffect, useState } from "react";

import { Badge, Button, Card, InfoBanner, Input, Modal, VietnamAddressPicker, useToast, type VietnamAddressValue } from "@/components/ui";
import { formatDateTimeLabel } from "@/components/ui";
import { fetchMyOrganizationProfile, requestOrganizationProfileUpdate, type OrganizationProfile } from "@/features/iam/api/organizationProfileApi";

const emptyAddress: VietnamAddressValue = { provinceCode: "", districtCode: null, wardCode: "", addressDetail: "" };

export function OrganizationProfilePage() {
  const toast = useToast();
  const [profile, setProfile] = useState<OrganizationProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [editOpen, setEditOpen] = useState(false);
  const [name, setName] = useState("");
  const [address, setAddress] = useState<VietnamAddressValue>(emptyAddress);
  const [isSaving, setIsSaving] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true); setError("");
    try { setProfile((await fetchMyOrganizationProfile()).data ?? null); }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Không thể tải hồ sơ đối tác."); }
    finally { setIsLoading(false); }
  }, []);

  useEffect(() => { void load(); }, [load]);

  function openEditor() {
    if (!profile) return;
    setName(profile.name);
    // Hồ sơ đối tác chỉ sử dụng đơn vị hành chính hiện hành: Tỉnh/Thành phố → Phường/Xã.
    // Không gửi lại mã quận/huyện cũ khi Partner Admin tạo yêu cầu cập nhật mới.
    setAddress({ provinceCode: profile.provinceCode ?? "", districtCode: null, wardCode: profile.wardCode ?? "", addressDetail: profile.addressDetail ?? "" });
    setEditOpen(true);
  }

  async function submit() {
    if (!profile) return;
    setIsSaving(true);
    try {
      await requestOrganizationProfileUpdate({ name, structuredAddress: address });
      toast.success("Thông tin đề xuất đã được gửi System Admin xét duyệt.", "Đã gửi yêu cầu");
      setEditOpen(false);
      await load();
    } catch (requestError) {
      toast.error(requestError instanceof Error ? requestError.message : "Không thể gửi yêu cầu cập nhật.", "Gửi yêu cầu thất bại");
    } finally { setIsSaving(false); }
  }

  const latest = profile?.latestUpdateRequest;
  const pending = latest?.status === "PENDING";
  return <div className="tw-px-4 tw-py-4 lg:tw-px-5"><section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1120px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
    <div className="tw-flex tw-items-start tw-justify-between tw-gap-4 max-[640px]:tw-flex-col"><div><p className="tw-m-0 tw-text-[0.74rem] tw-font-black tw-uppercase tw-tracking-[0.12em] tw-text-vm-primary">Đối tác CoParking</p><h1 className="tw-m-0 tw-mt-1 tw-text-vm-page-title tw-text-vm-slate-900">Hồ sơ đối tác</h1><p className="tw-m-0 tw-mt-2 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-500">Thông tin có hiệu lực do System Admin đã duyệt.</p></div><Button variant="secondary" disabled={isLoading} onClick={() => void load()}><i className="fas fa-sync-alt" />Làm mới</Button></div>
    {isLoading ? <div className="tw-mt-5"><InfoBanner tone="info" title="Đang tải hồ sơ" description="Vui lòng chờ trong giây lát." icon={<i className="fas fa-spinner fa-spin" />} /></div> : null}
    {error ? <div className="tw-mt-5"><InfoBanner tone="warning" title="Không thể tải hồ sơ" description={error} icon={<i className="fas fa-exclamation-circle" />} /></div> : null}
    {profile ? <><Card className="tw-mt-5 tw-p-5"><div className="tw-flex tw-flex-wrap tw-items-start tw-justify-between tw-gap-4"><div><p className="tw-m-0 tw-text-[0.76rem] tw-font-black tw-uppercase tw-text-vm-slate-500">Đơn vị</p><h2 className="tw-m-0 tw-mt-1 tw-text-[1.35rem] tw-font-black tw-text-vm-slate-900">{profile.name}</h2></div><Badge tone={profile.status === "ACTIVE" ? "success" : "warning"} className="tw-rounded-full tw-px-3">{profile.status === "ACTIVE" ? "Đang hoạt động" : profile.status}</Badge></div>
      <div className="tw-mt-5 tw-grid tw-grid-cols-2 tw-gap-3 max-[620px]:tw-grid-cols-1"><Field label="Mã đối tác" value={profile.code} /><Field label="Trạng thái" value={profile.status} /><Field label="Địa chỉ đã duyệt" value={profile.addressDisplay || "Chưa cập nhật"} wide /><Field label="Tài khoản đại diện" value={profile.representativeUsername || "-"} /><Field label="Email liên hệ" value={profile.representativeEmail || "-"} /><Field label="Ngày tạo hồ sơ" value={profile.createdAt ? formatDateTimeLabel(profile.createdAt) : "-"} /><Field label="Cập nhật lần cuối" value={profile.updatedAt ? formatDateTimeLabel(profile.updatedAt) : "-"} /></div>
      <div className="tw-mt-5 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5"><p className="tw-m-0 tw-text-[0.76rem] tw-font-black tw-uppercase tw-tracking-[0.08em] tw-text-vm-slate-500">Tóm tắt vận hành</p><div className="tw-mt-3 tw-grid tw-grid-cols-4 tw-gap-3 max-[900px]:tw-grid-cols-2 max-[460px]:tw-grid-cols-1"><Metric label="Tổng bãi xe" value={profile.totalParkingLots ?? 0} tone="primary" /><Metric label="Đang hoạt động" value={profile.activeParkingLots ?? 0} tone="success" /><Metric label="Đang thiết lập" value={profile.setupParkingLots ?? 0} tone="warning" /><Metric label="Đang bảo trì" value={profile.maintenanceParkingLots ?? 0} tone="danger" /></div></div>
      <div className="tw-mt-5"><Button disabled={pending} onClick={openEditor}><i className="fas fa-pen" />{pending ? "Đang chờ duyệt" : "Yêu cầu cập nhật hồ sơ"}</Button></div></Card>
      {latest ? <InfoBanner className="tw-mt-4" tone={pending ? "info" : latest.status === "REJECTED" ? "warning" : "success"} title={pending ? "Yêu cầu cập nhật đang chờ duyệt" : latest.status === "REJECTED" ? "Yêu cầu gần nhất bị từ chối" : "Yêu cầu cập nhật gần nhất đã được duyệt"} description={`${latest.organizationName} · ${latest.addressDisplay || "Chưa có địa chỉ"}${latest.note ? ` · Phản hồi: ${latest.note}` : ""}`} icon={<i className={pending ? "far fa-clock" : latest.status === "REJECTED" ? "fas fa-exclamation-circle" : "fas fa-check-circle"} />} /> : null}
    </> : null}
  </section>
  <Modal open={editOpen} onClose={() => { if (!isSaving) setEditOpen(false); }} title="Yêu cầu cập nhật hồ sơ" description="Thay đổi chỉ có hiệu lực sau khi System Admin duyệt." width="lg" actions={<div className="tw-flex tw-justify-end tw-gap-3"><Button variant="secondary" disabled={isSaving} onClick={() => setEditOpen(false)}>Hủy</Button><Button loading={isSaving} onClick={() => void submit()}>{!isSaving ? <i className="fas fa-paper-plane" /> : null}Gửi duyệt</Button></div>}><div className="tw-grid tw-gap-5"><label className="tw-grid tw-gap-2"><span className="tw-text-[0.78rem] tw-font-black tw-uppercase tw-text-vm-slate-700">Tên đối tác</span><Input value={name} maxLength={150} onChange={(event) => setName(event.target.value)} /></label><VietnamAddressPicker label="Địa chỉ đối tác" required mode="current" value={address} onChange={setAddress} /></div></Modal>
  </div>;
}

function Field({ label, value, wide = false }: { label: string; value: string; wide?: boolean }) { return <div className={`tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3 ${wide ? "sm:tw-col-span-2" : ""}`}><p className="tw-m-0 tw-text-[0.7rem] tw-font-black tw-uppercase tw-text-vm-slate-500">{label}</p><strong className="tw-mt-1 tw-block tw-break-words tw-text-[0.92rem] tw-text-vm-slate-900">{value || "-"}</strong></div>; }

function Metric({ label, value, tone }: { label: string; value: number; tone: "primary" | "success" | "warning" | "danger" }) { const color = { primary: "tw-text-vm-primary", success: "tw-text-emerald-600", warning: "tw-text-amber-600", danger: "tw-text-rose-600" }[tone]; return <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3"><p className="tw-m-0 tw-text-[0.73rem] tw-font-bold tw-text-vm-slate-500">{label}</p><strong className={`tw-mt-1 tw-block tw-text-[1.5rem] tw-font-black ${color}`}>{value}</strong></div>; }
