import { useEffect, useMemo, useState } from "react";

import {
  getCustomerPortalProfile,
  updateCustomerPortalProfile,
  type CustomerPortalProfile,
} from "@/features/customer-portal/api/customerPortalApi";
import { requestPasswordReset } from "@/features/auth/api/authApi";
import { Modal } from "@/shared/components/ui/Modal";

import { CustomerPageHeader, CustomerPortalLayout, Field, StatusPill } from "./PortalShared";

type ProfileForm = {
  address: string;
  dateOfBirth: string;
  fullName: string;
  gender: string;
  identifyCard: string;
  phoneNumber: string;
};

const emptyForm: ProfileForm = {
  address: "",
  dateOfBirth: "",
  fullName: "",
  gender: "",
  identifyCard: "",
  phoneNumber: "",
};

function initials(name?: string | null) {
  const words = name?.trim().split(/\s+/).filter(Boolean) ?? [];
  if (words.length === 0) return "KH";
  return words.slice(-2).map((word) => word[0]).join("").toUpperCase();
}

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "--";
  return new Intl.DateTimeFormat("vi-VN").format(date);
}

function compactCode(value?: string | null) {
  if (!value) return "--";
  return value.length > 18 ? `${value.slice(0, 8)}...${value.slice(-6)}` : value;
}

function profileToForm(profile: CustomerPortalProfile): ProfileForm {
  return {
    address: profile.profile?.address ?? "",
    dateOfBirth: profile.profile?.dateOfBirth ?? "",
    fullName: profile.profile?.fullName ?? "",
    gender: profile.profile?.gender ?? "",
    identifyCard: profile.profile?.identifyCard ?? "",
    phoneNumber: profile.profile?.phoneNumber ?? "",
  };
}

export function ProfilePage() {
  const [profile, setProfile] = useState<CustomerPortalProfile | null>(null);
  const [form, setForm] = useState<ProfileForm>(emptyForm);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordSending, setPasswordSending] = useState(false);
  const [passwordError, setPasswordError] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let ignore = false;

    async function loadProfile() {
      setLoading(true);
      setError("");
      try {
        const nextProfile = await getCustomerPortalProfile();
        if (ignore) return;
        setProfile(nextProfile);
        setForm(profileToForm(nextProfile));
      } catch (requestError) {
        if (!ignore) setError(requestError instanceof Error ? requestError.message : "Không thể tải hồ sơ khách hàng.");
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    void loadProfile();
    return () => {
      ignore = true;
    };
  }, []);

  const displayName = form.fullName || profile?.account?.username || "Khách hàng";
  const email = profile?.account?.email ?? "--";
  const customerCode = profile?.customer?.customerCode ?? "--";
  const displayCustomerCode = compactCode(customerCode);
  const customerStatus = profile?.customer?.customerStatus ?? "--";
  const approvalStatus = profile?.customer?.customerApprovalStatus ?? "--";
  const accountStatus = profile?.account?.accountStatus ?? "--";
  const isApproved = approvalStatus === "APPROVED";
  const formChanged = useMemo(() => {
    if (!profile) return false;
    return JSON.stringify(form) !== JSON.stringify(profileToForm(profile));
  }, [form, profile]);

  const handleSave = async () => {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const updatedProfile = await updateCustomerPortalProfile({
        address: form.address.trim() || undefined,
        dateOfBirth: form.dateOfBirth || undefined,
        fullName: form.fullName.trim() || undefined,
        gender: form.gender.trim() || undefined,
        identifyCard: form.identifyCard.trim() || undefined,
        phoneNumber: form.phoneNumber.trim() || undefined,
      });
      setProfile(updatedProfile);
      setForm(profileToForm(updatedProfile));
      setNotice("Đã cập nhật hồ sơ.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể cập nhật hồ sơ.");
    } finally {
      setSaving(false);
    }
  };

  const closePasswordModal = () => {
    setPasswordOpen(false);
    setPasswordError("");
  };

  const handleSendPasswordReset = async () => {
    if (!profile?.account?.email) {
      setPasswordError("Tài khoản hiện tại chưa có email để gửi liên kết đổi mật khẩu.");
      return;
    }

    setPasswordSending(true);
    setPasswordError("");
    try {
      await requestPasswordReset({ email: profile.account.email });
      closePasswordModal();
      setNotice("Đã gửi liên kết đổi mật khẩu đến email đăng nhập.");
    } catch (requestError) {
      setPasswordError(requestError instanceof Error ? requestError.message : "Không thể gửi liên kết đổi mật khẩu.");
    } finally {
      setPasswordSending(false);
    }
  };

  return (
    <CustomerPortalLayout>
      <CustomerPageHeader
        title="Hồ sơ cá nhân"
        subtitle="Quản lý thông tin tài khoản và hồ sơ khách hàng"
        action={<button className="vm-outline-action" type="button" onClick={() => setPasswordOpen(true)}><i className="fas fa-lock" /> Đổi mật khẩu</button>}
      />

      {error ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {error}</div> : null}
      {notice ? <div className="vm-info-note tw-bg-green-50 tw-text-green-700"><i className="fas fa-check-circle" /> {notice}</div> : null}

      <div className="vm-profile-top">
        <article className="vm-customer-card vm-profile-summary">
          <div className="vm-large-avatar">{initials(displayName)}</div>
          <div>
            <h2>{loading ? "Đang tải..." : displayName}</h2>
            <div className="vm-pill-row">
              <StatusPill tone="blue">Khách hàng</StatusPill>
              <StatusPill tone={customerStatus === "ACTIVE" ? "green" : "gray"}>{customerStatus}</StatusPill>
              <StatusPill tone={isApproved ? "green" : "orange"}>{approvalStatus}</StatusPill>
            </div>
            <dl className="vm-info-list vm-profile-info-list">
              <dt><i className="far fa-id-card" /> Mã khách hàng:</dt><dd title={customerCode}>{displayCustomerCode}</dd>
              <dt><i className="far fa-calendar-alt" /> Ngày sinh:</dt><dd>{formatDate(profile?.profile?.dateOfBirth)}</dd>
              <dt><i className="fas fa-shield-alt" /> Trạng thái hồ sơ:</dt><dd>{profile?.profile?.userProfileStatus ?? "--"}</dd>
            </dl>
          </div>
        </article>

        <article className="vm-customer-card">
          <h2>Thông tin tài khoản</h2>
          <dl className="vm-info-list vm-profile-info-list">
            <dt><i className="far fa-envelope" /> Email đăng nhập:</dt><dd>{email}</dd>
            <dt><i className="fas fa-phone-alt" /> Số điện thoại:</dt><dd>{form.phoneNumber || "--"}</dd>
            <dt><i className="far fa-user" /> Tên đăng nhập:</dt><dd>{profile?.account?.username ?? "--"}</dd>
            <dt><i className="fas fa-shield-alt" /> Trạng thái:</dt><dd className="vm-green-text">{accountStatus}</dd>
          </dl>
        </article>
      </div>

      <section className="vm-customer-card">
        <h2>Cập nhật hồ sơ</h2>
        <div className="vm-form-grid">
          <Field label="Họ và tên"><input value={form.fullName} onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))} /></Field>
          <Field label="Email"><input value={email} readOnly /></Field>
          <Field label="Số điện thoại"><input value={form.phoneNumber} onChange={(event) => setForm((current) => ({ ...current, phoneNumber: event.target.value }))} /></Field>
          <Field label="CCCD/CMND"><input value={form.identifyCard} onChange={(event) => setForm((current) => ({ ...current, identifyCard: event.target.value }))} /></Field>
          <Field label="Ngày sinh"><input type="date" value={form.dateOfBirth} onChange={(event) => setForm((current) => ({ ...current, dateOfBirth: event.target.value }))} /></Field>
          <Field label="Giới tính">
            <select value={form.gender} onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}>
              <option value="">Chưa chọn</option>
              <option value="Nam">Nam</option>
              <option value="Nữ">Nữ</option>
              <option value="Khác">Khác</option>
            </select>
          </Field>
          <Field label="Địa chỉ"><input value={form.address} onChange={(event) => setForm((current) => ({ ...current, address: event.target.value }))} /></Field>
          <Field label="Ghi chú"><textarea placeholder="Thông tin bổ sung..." /></Field>
        </div>
        <div className="vm-form-actions vm-profile-form-actions">
          <button className="vm-outline-btn" type="button" disabled={!profile || saving} onClick={() => profile && setForm(profileToForm(profile))}>Hủy thay đổi</button>
          <button type="button" disabled={!formChanged || saving} onClick={handleSave}>{saving ? "Đang lưu..." : "Lưu thay đổi"}</button>
        </div>
      </section>

      <Modal
        open={passwordOpen}
        title="Đổi mật khẩu"
        description="Hệ thống sẽ gửi liên kết đổi mật khẩu đến email đăng nhập của tài khoản."
        width="sm"
        onClose={closePasswordModal}
        actions={
          <div className="vm-password-modal-actions">
            <button className="vm-outline-btn" type="button" onClick={closePasswordModal}>Đóng</button>
            <button type="button" disabled={passwordSending || !profile?.account?.email} onClick={handleSendPasswordReset}>
              {passwordSending ? "Đang gửi..." : "Gửi liên kết"}
            </button>
          </div>
        }
      >
        <div className="vm-password-form">
          {passwordError ? <div className="vm-info-note tw-bg-red-50 tw-text-red-600"><i className="fas fa-exclamation-circle" /> {passwordError}</div> : null}
          <div className="vm-password-reset-card">
            <span><i className="fas fa-envelope-open-text" /></span>
            <div>
              <p>Email nhận liên kết</p>
              <strong>{profile?.account?.email ?? "--"}</strong>
            </div>
          </div>
          <div className="vm-info-note">
            <i className="fas fa-info-circle" /> Sau khi xác nhận, vui lòng kiểm tra email và làm theo hướng dẫn từ hệ thống xác thực.
          </div>
        </div>
      </Modal>
    </CustomerPortalLayout>
  );
}
