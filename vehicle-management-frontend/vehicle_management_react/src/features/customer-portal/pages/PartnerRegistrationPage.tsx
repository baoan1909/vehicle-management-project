import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { Button, useToast } from "@/components/ui";
import { resendVerificationEmail } from "@/features/auth/api/authApi";
import {
  AuthFormField,
  AuthFormSectionTitle,
  AuthInlineNotice,
  AuthPasswordInput,
} from "@/features/auth/components/AuthFormControls";
import {
  authFieldLimits,
  validateEmail,
  validateRegisterValues,
  type RegisterFieldErrors,
} from "@/features/auth/utils/authValidation";
import { submitPartnerRegistration } from "@/features/iam/api/partnerRegistrationApi";

const initialForm = {
  fullName: "",
  username: "",
  email: "",
  password: "",
  confirmPassword: "",
  organizationCode: "",
  organizationName: "",
  phoneNumber: "",
};

type PartnerForm = typeof initialForm;
type FormField = keyof PartnerForm;
type FieldErrors = Partial<Record<FormField, string>>;

function validatePartnerForm(form: PartnerForm): FieldErrors {
  const accountErrors: RegisterFieldErrors = validateRegisterValues({
    confirmPassword: form.confirmPassword,
    email: form.email,
    fullName: form.fullName,
    password: form.password,
    username: form.username,
  });
  const errors: FieldErrors = { ...accountErrors };
  const organizationCode = form.organizationCode.trim();
  if (!organizationCode) {
    errors.organizationCode = "Vui lòng nhập mã đơn vị.";
  } else if (!/^[A-Za-z0-9_-]+$/.test(organizationCode)) {
    errors.organizationCode = "Mã đơn vị chỉ gồm chữ, số, dấu gạch dưới hoặc dấu gạch ngang.";
  } else if (organizationCode.length > 50) {
    errors.organizationCode = "Mã đơn vị không được vượt quá 50 ký tự.";
  }
  if (!form.organizationName.trim()) errors.organizationName = "Vui lòng nhập tên đơn vị.";
  if (!form.fullName.trim()) errors.fullName = "Vui lòng nhập họ và tên.";
  if (!/^\+?\d+$/.test(form.phoneNumber.trim())) {
    errors.phoneNumber = "Số điện thoại chỉ gồm chữ số và có thể bắt đầu bằng dấu +.";
  }
  return errors;
}

function errorFieldsForMessage(message: string): FieldErrors {
  const normalized = message.toLocaleLowerCase("vi-VN");
  if (normalized.includes("tên đăng nhập")) return { username: message };
  if (normalized.includes("email")) return { email: message };
  if (normalized.includes("organization code") || normalized.includes("mã đơn vị")) return { organizationCode: message };
  if (normalized.includes("số điện thoại")) return { phoneNumber: message };
  return {};
}

export function PartnerRegistrationPage() {
  const toast = useToast();
  const [form, setForm] = useState<PartnerForm>(initialForm);
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [saving, setSaving] = useState(false);
  const [resending, setResending] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const resendTargetEmail = (registeredEmail || form.email).trim().toLocaleLowerCase("en-US");

  function updateField(field: FormField, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
    setError(null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    const validationErrors = validatePartnerForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setError("Vui lòng kiểm tra các thông tin được đánh dấu bên dưới.");
      setFieldErrors(validationErrors);
      return;
    }
    if (!acceptedTerms) {
      setError("Vui lòng đồng ý Điều khoản sử dụng và Chính sách bảo mật.");
      return;
    }

    setSaving(true);
    setError(null);
    setFieldErrors({});
    const normalizedEmail = form.email.trim().toLocaleLowerCase("en-US");
    try {
      await submitPartnerRegistration({
        email: normalizedEmail,
        fullName: form.fullName.trim(),
        organizationCode: form.organizationCode.trim().toUpperCase(),
        organizationName: form.organizationName.trim(),
        password: form.password,
        phoneNumber: form.phoneNumber.trim(),
        username: form.username.trim(),
      });
      setRegisteredEmail(normalizedEmail);
      toast.success("Tài khoản đối tác đã được tạo. Vui lòng kiểm tra email để xác thực.");
      setForm(initialForm);
      setAcceptedTerms(false);
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "Không thể tạo tài khoản đối tác. Vui lòng thử lại.";
      setError(message);
      setFieldErrors(errorFieldsForMessage(message));
    } finally {
      setSaving(false);
    }
  }

  async function resendVerification() {
    if (resending) return;
    const validationError = validateEmail(resendTargetEmail);
    if (validationError) {
      setError(validationError);
      setFieldErrors((current) => ({ ...current, email: validationError }));
      return;
    }
    setResending(true);
    setError(null);
    try {
      const response = await resendVerificationEmail({ email: resendTargetEmail });
      setRegisteredEmail(resendTargetEmail);
      toast.success(response.message || "Đã gửi lại email xác thực.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể gửi lại email xác thực.");
    } finally {
      setResending(false);
    }
  }

  return (
    <div className="tw-fixed tw-inset-0 tw-flex tw-min-h-screen tw-min-h-[100dvh] tw-w-screen tw-overflow-y-auto tw-bg-[linear-gradient(180deg,#f8fbff_0%,#eef5ff_100%)]">
      <main className="tw-my-auto tw-w-full tw-py-10">
        <section className="tw-mx-auto tw-grid tw-w-[min(1120px,calc(100%_-_32px))] tw-grid-cols-[0.68fr_1.32fr] tw-overflow-hidden tw-rounded-vm-lg tw-bg-white tw-shadow-[0_24px_60px_rgba(18,59,110,.14)] max-[880px]:tw-grid-cols-1">
          <aside className="tw-bg-[linear-gradient(145deg,#061d42,#1267df)] tw-p-9 tw-text-white">
            <p className="tw-m-0 tw-text-sm tw-font-black tw-uppercase tw-tracking-[.14em] tw-text-[#8fc0ff]">CoParking for business</p>
            <h1 className="tw-m-0 tw-mt-4 tw-font-[Cambria] tw-text-4xl tw-font-bold">Trở thành đối tác CoParking</h1>
            <p className="tw-mt-5 tw-leading-7 tw-text-[#d4e5ff]">Tự tạo tài khoản quản trị đối tác, xác minh email và theo dõi quá trình xét duyệt trên một luồng duy nhất.</p>
            <ol className="tw-mt-8 tw-grid tw-gap-4 tw-pl-5 tw-text-sm tw-font-semibold tw-leading-6">
              <li>Tạo tài khoản và hồ sơ đơn vị.</li>
              <li>Xác minh địa chỉ email đăng ký.</li>
              <li>Chờ CoParking xét duyệt và kích hoạt quyền.</li>
            </ol>
            <p className="tw-mt-8 tw-rounded-vm-md tw-bg-white/10 tw-p-4 tw-text-sm tw-leading-6 tw-text-[#d4e5ff]">
              Trong thời gian chờ duyệt, tài khoản có thể đăng nhập để xem trạng thái nhưng chưa sử dụng chức năng quản trị bãi xe.
            </p>
          </aside>

          <section className="tw-p-8 max-[560px]:tw-p-5">
            <div>
              <h2 className="tw-m-0 tw-text-2xl tw-font-black tw-text-[#102b50]">Đăng ký tài khoản đối tác</h2>
              <p className="tw-m-0 tw-mt-1 tw-text-sm tw-text-[#65809f]">Dùng email chính chủ để nhận liên kết xác minh và kết quả xét duyệt.</p>
            </div>

            <form className="tw-mt-6 tw-grid tw-gap-5" noValidate onSubmit={submit}>
              {error ? <AuthInlineNotice tone="error">{error}</AuthInlineNotice> : null}

              <section className="tw-grid tw-gap-3">
                <AuthFormSectionTitle>Thông tin tài khoản</AuthFormSectionTitle>
                <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[680px]:tw-grid-cols-1">
                  <AuthFormField autoComplete="name" id="partnerFullName" icon="far fa-user" label="Họ và tên" error={fieldErrors.fullName} maxLength={authFieldLimits.fullNameMaxLength} required value={form.fullName} onChange={(value) => updateField("fullName", value)} />
                  <AuthFormField autoComplete="username" id="partnerUsername" icon="far fa-user-circle" label="Tên đăng nhập" error={fieldErrors.username} maxLength={authFieldLimits.usernameMaxLength} required value={form.username} onChange={(value) => updateField("username", value)} />
                  <div className="tw-col-span-2 max-[680px]:tw-col-span-1">
                    <AuthFormField autoComplete="email" id="partnerEmail" icon="far fa-envelope" label="Email" error={fieldErrors.email} maxLength={authFieldLimits.emailMaxLength} required type="email" value={form.email} onChange={(value) => updateField("email", value)} />
                  </div>
                  <AuthPasswordInput autoComplete="new-password" id="partnerPassword" label="Mật khẩu" error={fieldErrors.password} maxLength={authFieldLimits.passwordMaxLength} required value={form.password} onChange={(value) => updateField("password", value)} />
                  <AuthPasswordInput autoComplete="new-password" id="partnerConfirmPassword" label="Xác nhận mật khẩu" error={fieldErrors.confirmPassword} maxLength={authFieldLimits.passwordMaxLength} required value={form.confirmPassword} onChange={(value) => updateField("confirmPassword", value)} />
                </div>
              </section>

              <section className="tw-grid tw-gap-3">
                <AuthFormSectionTitle>Thông tin cá nhân</AuthFormSectionTitle>
                <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[680px]:tw-grid-cols-1">
                  <AuthFormField id="phoneNumber" icon="fas fa-phone-alt" label="Số điện thoại" error={fieldErrors.phoneNumber} maxLength={20} required type="tel" value={form.phoneNumber} onChange={(value) => updateField("phoneNumber", value)} />
                </div>
              </section>

              <section className="tw-grid tw-gap-3">
                <AuthFormSectionTitle>Thông tin đối tác</AuthFormSectionTitle>
                <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[680px]:tw-grid-cols-1">
                  <AuthFormField id="organizationCode" icon="fas fa-fingerprint" label="Mã đơn vị" error={fieldErrors.organizationCode} maxLength={50} placeholder="VD: PARKING_ABC" required value={form.organizationCode} onChange={(value) => updateField("organizationCode", value.toUpperCase())} />
                  <AuthFormField id="organizationName" icon="far fa-building" label="Tên đơn vị" error={fieldErrors.organizationName} maxLength={150} required value={form.organizationName} onChange={(value) => updateField("organizationName", value)} />
                </div>
              </section>

              <AuthInlineNotice>
                <div className="tw-flex tw-min-w-0 tw-flex-1 tw-items-center tw-gap-3 max-[680px]:tw-flex-col max-[680px]:tw-items-start">
                  <span className="tw-min-w-0 tw-flex-1">Sau khi tạo tài khoản, email xác thực sẽ được gửi đến địa chỉ email của bạn. Vui lòng kiểm tra email để kích hoạt tài khoản.</span>
                  <Button
                    className="tw-ml-auto tw-mr-2 tw-h-8 tw-flex-shrink-0 tw-rounded-vm-sm tw-px-3 tw-text-[0.78rem] tw-font-extrabold max-[680px]:tw-ml-0 max-[680px]:tw-mr-0"
                    disabled={!resendTargetEmail}
                    loading={resending}
                    size="sm"
                    type="button"
                    variant="secondary"
                    onClick={resendVerification}
                  >
                    {resending ? "Đang gửi..." : "Gửi lại email"}
                  </Button>
                </div>
              </AuthInlineNotice>

              <label className="tw-flex tw-items-center tw-gap-2.5 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-700">
                <input
                  checked={acceptedTerms}
                  className="tw-h-4 tw-w-4 tw-rounded-vm-sm tw-border tw-border-solid tw-border-[#cbd5e1] tw-accent-vm-primary"
                  type="checkbox"
                  onChange={(event) => setAcceptedTerms(event.target.checked)}
                />
                <span>
                  Tôi đã đọc và đồng ý với{" "}
                  <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/pricing">Điều khoản sử dụng</Link>
                  {" "}và{" "}
                  <Link className="tw-font-extrabold tw-text-vm-primary tw-no-underline hover:tw-text-vm-primary-hover" to="/pricing">Chính sách bảo mật</Link>
                </span>
              </label>

              <Button className="tw-h-11 tw-w-full tw-rounded-vm-md tw-font-extrabold" disabled={saving} loading={saving} type="submit" variant="primary">
                {saving ? "Đang tạo tài khoản..." : "Đăng ký trở thành đối tác"}
              </Button>
              <p className="tw-m-0 tw-text-center tw-text-sm tw-font-semibold tw-text-vm-slate-600">Đã có tài khoản? <Link className="tw-font-extrabold tw-text-vm-primary" to="/login">Đăng nhập</Link></p>
            </form>
          </section>
        </section>
      </main>
    </div>
  );
}
