import { type ChangeEvent, useCallback, useEffect, useRef, useState } from "react";
import { Navigate } from "react-router-dom";

import { Button, useToast } from "@/components/ui";
import { VietnamAddressPicker, type VietnamAddressValue } from "@/components/ui/VietnamAddressPicker";
import {
  AuthFormField,
  AuthFormSectionTitle,
  AuthInlineNotice,
} from "@/features/auth/components/AuthFormControls";
import { resendVerificationEmail } from "@/features/auth/api/authApi";
import { getMyAvatarModerationStatus, uploadMyAccountAvatar, type AvatarModerationStatus } from "@/features/iam/api/accountProfileApi";
import { AvatarModerationStatusCard } from "@/features/iam/components/AvatarModerationStatusCard";
import { getMyPartnerRegistrationStatus, completePartnerProfile, type PartnerApplicationStatus } from "@/features/iam/api/partnerRegistrationApi";
import { useAuth } from "@/core/auth/useAuth";
import { subscribeNotificationReceived } from "@/features/notifications/utils/notificationEvents";
import { ClientPage } from "@/shared/components/layout/ClientPage";

import { PublicFooter } from "@/features/customer-portal/pages/PortalShared";

export function PartnerProfileCompletionPage() {
  const { user, setUser } = useAuth();
  const toast = useToast();
  const avatarInputRef = useRef<HTMLInputElement>(null);

  const [status, setStatus] = useState<PartnerApplicationStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [resending, setResending] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarModeration, setAvatarModeration] = useState<AvatarModerationStatus | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [form, setForm] = useState<{
    fullName: string;
    dateOfBirth: string;
    gender: string;
    phoneNumber: string;
    identifyCard: string;
    personalAddress: VietnamAddressValue;
    organizationCode: string;
    organizationName: string;
    representativeName: string;
    representativePhoneNumber: string;
    organizationAddress: VietnamAddressValue;
  }>({
    fullName: "",
    dateOfBirth: "",
    gender: "",
    phoneNumber: "",
    identifyCard: "",
    personalAddress: { provinceCode: "", districtCode: null, wardCode: "", addressDetail: "" },
    organizationCode: "",
    organizationName: "",
    representativeName: "",
    representativePhoneNumber: "",
    organizationAddress: { provinceCode: "", districtCode: null, wardCode: "", addressDetail: "" },
  });

  const [personalAddressMode, setPersonalAddressMode] = useState<"current" | "legacy">("current");
  const [organizationAddressMode, setOrganizationAddressMode] = useState<"current" | "legacy">("current");

  const loadStatus = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await getMyPartnerRegistrationStatus();
      const nextStatus = response.data;
      setStatus(nextStatus);
      try {
        setAvatarModeration((await getMyAvatarModerationStatus()).data);
      } catch {
        setAvatarModeration(null);
      }
      if (nextStatus) {
        setForm((prev) => ({
          ...prev,
          fullName: nextStatus.fullName ?? "",
          dateOfBirth: nextStatus.dateOfBirth ?? "",
          gender: nextStatus.gender ?? "",
          phoneNumber: nextStatus.phoneNumber ?? "",
          identifyCard: nextStatus.identifyCard ?? "",
          personalAddress: nextStatus.personalAddress ?? prev.personalAddress,
          organizationCode: nextStatus.organizationCode ?? "",
          organizationName: nextStatus.organizationName ?? "",
          representativeName: nextStatus.representativeName ?? "",
          representativePhoneNumber: nextStatus.representativePhoneNumber ?? "",
          organizationAddress: nextStatus.organizationAddress ?? prev.organizationAddress,
        }));
        setPersonalAddressMode(nextStatus.personalAddress?.districtCode ? "legacy" : "current");
        setOrganizationAddressMode(nextStatus.organizationAddress?.districtCode ? "legacy" : "current");
      }
      setUser((currentUser) => currentUser ? {
        ...currentUser,
        accountStatus: nextStatus.accountStatus,
        partnerApplicationStatus: nextStatus.approvalStatus,
        partnerNextAction: nextStatus.nextAction,
        avatarUrl: nextStatus.avatarUrl ?? currentUser.avatarUrl,
        permissionCodes: nextStatus.accountStatus === "ACTIVE" ? undefined : currentUser.permissionCodes,
      } : currentUser);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải trạng thái hồ sơ đối tác.");
    } finally {
      setLoading(false);
    }
  }, [setUser]);

  useEffect(() => {
    if (user) void loadStatus();
  }, [loadStatus, user?.id]);

  useEffect(() => subscribeNotificationReceived((notification) => {
    if (!["AVATAR_APPROVED", "AVATAR_REJECTED"].includes(notification.notificationType)) return;
    void loadStatus();
  }), [loadStatus]);

  if (!user) return <Navigate to="/login" replace />;

  // If nextAction is not COMPLETE_PROFILE, redirect
  if (status && status.nextAction !== "COMPLETE_PROFILE") {
    return <Navigate to="/partner/application-status" replace />;
  }

  async function resendEmail() {
    const email = user?.email;
    if (!email || resending) return;
    setResending(true);
    setMessage("");
    setError("");
    try {
      const response = await resendVerificationEmail({ email });
      setMessage(response.message || "Đã gửi lại email xác thực.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể gửi lại email xác thực.");
    } finally {
      setResending(false);
    }
  }

  async function handleAvatarChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || avatarUploading) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setError("Ảnh đại diện chỉ hỗ trợ JPG, PNG hoặc WebP.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Ảnh đại diện không được vượt quá 5 MB.");
      return;
    }
    setAvatarUploading(true);
    setError("");
    try {
      await uploadMyAccountAvatar(file);
      const moderation = await getMyAvatarModerationStatus();
      setAvatarModeration(moderation.data);
      toast.success(moderation.data?.approvalStatus === "APPROVED" ? "Ảnh đại diện đã được tự động duyệt." : "Ảnh đại diện đã được gửi duyệt.");
      await loadStatus();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể cập nhật ảnh đại diện.");
    } finally {
      setAvatarUploading(false);
    }
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError("");
    setMessage("");

    try {
      await completePartnerProfile({
        fullName: form.fullName.trim(),
        dateOfBirth: form.dateOfBirth,
        gender: form.gender,
        phoneNumber: form.phoneNumber.trim(),
        identifyCard: form.identifyCard.trim(),
        personalAddress: {
          ...form.personalAddress,
          districtCode: personalAddressMode === "legacy" ? form.personalAddress.districtCode : null,
        },
        organizationCode: form.organizationCode.trim().toUpperCase(),
        organizationName: form.organizationName.trim(),
        representativeName: form.representativeName.trim(),
        representativePhoneNumber: form.representativePhoneNumber.trim(),
        organizationAddress: {
          ...form.organizationAddress,
          districtCode: organizationAddressMode === "legacy" ? form.organizationAddress.districtCode : null,
        },
      });
      toast.success("Hồ sơ đã được cập nhật. Hệ thống đang kiểm tra để duyệt tự động.");
      await loadStatus();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể cập nhật hồ sơ. Vui lòng thử lại.");
    } finally {
      setSaving(false);
    }
  }

  function updateField(field: keyof typeof form, value: string | VietnamAddressValue) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  return (
    <ClientPage>
      <main className="tw-min-h-[calc(100vh-72px)] tw-bg-[#f5f9ff] tw-py-12">
        <section className="tw-mx-auto tw-w-[min(920px,calc(100%_-_32px))] tw-grid tw-gap-5 tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-8 tw-shadow-[0_20px_50px_rgba(15,55,105,.12)] max-[560px]:tw-p-5">
          <header className="tw-flex tw-items-start tw-justify-between tw-gap-4">
            <div>
              <p className="tw-m-0 tw-text-xs tw-font-black tw-uppercase tw-tracking-[.12em] tw-text-vm-primary">Đăng ký đối tác</p>
              <h1 className="tw-m-0 tw-mt-2 tw-text-2xl tw-font-black tw-text-vm-slate-900">Hoàn thiện hồ sơ đối tác</h1>
            </div>
            <Button loading={loading} size="sm" type="button" variant="secondary" onClick={() => void loadStatus()}>Làm mới</Button>
          </header>

          {error ? <AuthInlineNotice tone="error">{error}</AuthInlineNotice> : null}
          {message ? <AuthInlineNotice tone="success">{message}</AuthInlineNotice> : null}
          {loading && !status ? <div className="tw-py-10 tw-text-center tw-font-semibold tw-text-vm-slate-500">Đang tải trạng thái hồ sơ...</div> : null}

          {status && (
            <form className="tw-mt-4 tw-grid tw-gap-5" noValidate onSubmit={handleSubmit}>
              <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-4">
                <div className="tw-flex tw-items-center tw-gap-2 tw-text-sm tw-font-black tw-uppercase tw-text-vm-slate-500">
                  <span>Trạng thái:</span>
                  <span className="tw-font-bold tw-text-vm-slate-900">
                    {status.nextAction === "COMPLETE_PROFILE" ? "Cần hoàn thiện hồ sơ" :
                     status.nextAction === "VERIFY_EMAIL" ? "Chờ xác minh email" :
                     status.nextAction === "WAIT_FOR_REVIEW" ? "Đang chờ xét duyệt" :
                     status.nextAction === "ACCESS_PARTNER_PORTAL" ? "Đã duyệt" : status.nextAction}
                  </span>
                </div>
                {!status.emailVerified && (
                  <div className="tw-mt-3 tw-flex tw-items-center tw-gap-3">
                    <span className="tw-flex-1 tw-text-sm tw-text-vm-slate-600">Email chưa được xác minh. Vui lòng kiểm tra hộp thư.</span>
                    <Button loading={resending} size="sm" type="button" variant="secondary" onClick={resendEmail}>Gửi lại email</Button>
                  </div>
                )}
              </div>

              <section className="tw-grid tw-gap-3">
                <AuthFormSectionTitle>Thông tin cá nhân</AuthFormSectionTitle>
                <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[680px]:tw-grid-cols-1">
                  <AuthFormField
                    autoComplete="name"
                    id="fullName"
                    icon="far fa-user"
                    label="Họ và tên"
                    maxLength={150}
                    required
                    value={form.fullName}
                    onChange={(value) => updateField("fullName", value)}
                  />
                  <AuthFormField
                    autoComplete="bday"
                    id="dateOfBirth"
                    icon="far fa-calendar-alt"
                    label="Ngày sinh"
                    type="date"
                    required
                    value={form.dateOfBirth}
                    onChange={(value) => updateField("dateOfBirth", value)}
                  />
                  <label className="tw-grid tw-gap-2 tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">
                    Giới tính
                    <select
                      className="tw-h-[46px] tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-font-semibold tw-text-vm-slate-900"
                      required
                      value={form.gender}
                      onChange={(event) => updateField("gender", event.target.value)}
                    >
                      <option value="">Chọn giới tính</option>
                      <option value="Nam">Nam</option>
                      <option value="Nữ">Nữ</option>
                      <option value="Khác">Khác</option>
                    </select>
                  </label>
                  <AuthFormField
                    autoComplete="tel"
                    id="phoneNumber"
                    icon="fas fa-phone-alt"
                    label="Số điện thoại"
                    maxLength={20}
                    required
                    type="tel"
                    value={form.phoneNumber}
                    onChange={(value) => updateField("phoneNumber", value)}
                  />
                  <div className="tw-grid tw-gap-2">
                    <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Ảnh đại diện (tùy chọn)</span>
                    <div className="tw-flex tw-items-center tw-gap-3">
                      <div className="tw-flex tw-h-14 tw-w-14 tw-items-center tw-justify-center tw-overflow-hidden tw-rounded-full tw-bg-vm-slate-100">
                        {status.avatarUrl
                          ? <img alt="Ảnh đại diện" className="tw-h-full tw-w-full tw-object-cover" src={status.avatarUrl} />
                          : <i className="far fa-user tw-text-xl tw-text-vm-slate-500" />}
                      </div>
                      <input
                        ref={avatarInputRef}
                        accept="image/jpeg,image/png,image/webp"
                        className="tw-sr-only"
                        type="file"
                        onChange={handleAvatarChange}
                      />
                      <Button
                        loading={avatarUploading}
                        size="sm"
                        type="button"
                        variant="secondary"
                        onClick={() => avatarInputRef.current?.click()}
                      >
                        {status.hasAvatar ? "Đổi ảnh" : "Tải ảnh lên"}
                      </Button>
                    </div>
                    <span className="tw-text-xs tw-font-medium tw-text-vm-slate-500">JPG, PNG hoặc WebP; tối đa 5 MB. Ảnh được duyệt theo luồng riêng và không chặn xét duyệt hồ sơ.</span>
                    <AvatarModerationStatusCard status={avatarModeration} />
                  </div>
                  <AuthFormField
                    id="identifyCard"
                    icon="fas fa-id-card"
                    label="CCCD/CMND"
                    maxLength={20}
                    required
                    value={form.identifyCard}
                    onChange={(value) => updateField("identifyCard", value)}
                  />
                  <div className="tw-col-span-2">
                    <VietnamAddressPicker
                      label="Địa chỉ liên hệ"
                      value={form.personalAddress}
                      onChange={(value) => updateField("personalAddress", value)}
                      onModeChange={setPersonalAddressMode}
                      mode="auto"
                      required
                    />
                  </div>
                </div>
              </section>

              <section className="tw-grid tw-gap-3">
                <AuthFormSectionTitle>Thông tin đơn vị (để xét duyệt)</AuthFormSectionTitle>
                <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[680px]:tw-grid-cols-1">
                  <AuthFormField
                    id="organizationCode"
                    icon="fas fa-fingerprint"
                    label="Mã đơn vị"
                    maxLength={50}
                    required
                    value={form.organizationCode}
                    onChange={(value) => updateField("organizationCode", value.toUpperCase())}
                    disabled={!!status?.organizationCode}
                  />
                  <AuthFormField
                    id="organizationName"
                    icon="far fa-building"
                    label="Tên đơn vị"
                    maxLength={150}
                    required
                    value={form.organizationName}
                    onChange={(value) => updateField("organizationName", value)}
                  />
                  <AuthFormField
                    id="representativeName"
                    icon="far fa-address-card"
                    label="Người đại diện"
                    maxLength={150}
                    required
                    value={form.representativeName}
                    onChange={(value) => updateField("representativeName", value)}
                  />
                  <AuthFormField
                    autoComplete="tel"
                    id="representativePhoneNumber"
                    icon="fas fa-phone-alt"
                    label="Số điện thoại người đại diện"
                    maxLength={20}
                    required
                    type="tel"
                    value={form.representativePhoneNumber}
                    onChange={(value) => updateField("representativePhoneNumber", value)}
                  />
                  <div className="tw-col-span-2">
                    <VietnamAddressPicker
                      label="Địa chỉ đơn vị"
                      value={form.organizationAddress}
                      onChange={(value) => updateField("organizationAddress", value)}
                      onModeChange={setOrganizationAddressMode}
                      mode="auto"
                      required
                    />
                  </div>
                </div>
              </section>

              <AuthInlineNotice>
                <p className="tw-text-sm tw-text-vm-slate-600">Sau khi lưu hồ sơ, nếu đầy đủ thông tin, hệ thống sẽ tự động xét duyệt. Nếu không, hồ sơ sẽ chuyển sang trạng thái "Đang chờ xét duyệt" để đội ngũ CoParking kiểm tra.</p>
              </AuthInlineNotice>

              <Button className="tw-h-11 tw-w-full tw-rounded-vm-md tw-font-extrabold" disabled={saving} loading={saving} type="submit" variant="primary">
                {saving ? "Đang lưu..." : "Lưu và gửi xét duyệt"}
              </Button>
            </form>
          )}
        </section>
      </main>
      <PublicFooter />
    </ClientPage>
  );
}
