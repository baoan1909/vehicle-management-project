import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { apiClient } from "@/core/api/apiClient";
import { cn } from "@/lib/cn";
import { ClientPage } from "@/shared/components/layout/ClientPage";

import { PublicFooter } from "./PortalShared";

const initialForm = {
  organizationCode: "",
  organizationName: "",
  representativeName: "",
  email: "",
  phoneNumber: "",
  address: "",
  expectedParkingLotCount: "1",
  parkingOperationDescription: "",
};

type PartnerForm = typeof initialForm;
type FormField = keyof PartnerForm;
type FieldErrors = Partial<Record<FormField, string>>;

function validateForm(form: PartnerForm): FieldErrors {
  const errors: FieldErrors = {};
  if (!/^[A-Za-z0-9_-]+$/.test(form.organizationCode.trim())) {
    errors.organizationCode = "Mã đơn vị chỉ được chứa chữ, số, dấu gạch dưới hoặc dấu gạch ngang.";
  }
  if (!/^\+?\d+$/.test(form.phoneNumber.trim())) {
    errors.phoneNumber = "Số điện thoại chỉ được chứa chữ số và có thể bắt đầu bằng dấu +.";
  }
  const parkingLotCount = Number(form.expectedParkingLotCount);
  if (!Number.isInteger(parkingLotCount) || parkingLotCount < 1 || parkingLotCount > 1000) {
    errors.expectedParkingLotCount = "Số bãi dự kiến quản lý phải nằm trong khoảng từ 1 đến 1.000.";
  }
  return errors;
}

function errorFieldsForMessage(message: string): FieldErrors {
  if (message.includes("Email này đã có hồ sơ") || message.includes("Email liên hệ không đúng")) return { email: message };
  if (message.includes("Mã đơn vị này đã có hồ sơ") || message.includes("Mã đơn vị chỉ được")) return { organizationCode: message };
  if (message.includes("Số điện thoại")) return { phoneNumber: message };
  if (message.includes("Số bãi dự kiến")) return { expectedParkingLotCount: message };
  return {};
}

export function PartnerRegistrationPage() {
  const [form, setForm] = useState<PartnerForm>(initialForm);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  function updateField(field: FormField, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
    setError(null);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const validationErrors = validateForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setNotice(null);
      setError("Vui lòng kiểm tra các thông tin được đánh dấu bên dưới.");
      setFieldErrors(validationErrors);
      return;
    }

    setSaving(true);
    setNotice(null);
    setError(null);
    setFieldErrors({});
    try {
      await apiClient("/public/partner-registrations", {
        method: "POST",
        skipAuth: true,
        body: {
          ...form,
          organizationCode: form.organizationCode.trim().toUpperCase(),
          expectedParkingLotCount: Number(form.expectedParkingLotCount),
        },
      });
      setNotice("Hồ sơ đã được gửi. Đội ngũ CoParking sẽ liên hệ để xác minh và hướng dẫn triển khai.");
      setForm(initialForm);
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : "Không thể gửi hồ sơ. Vui lòng thử lại.";
      setError(message);
      setFieldErrors(errorFieldsForMessage(message));
    } finally {
      setSaving(false);
    }
  }

  function field(key: FormField, label: string, type = "text", required = true) {
    const fieldError = fieldErrors[key];
    return (
      <label className="tw-grid tw-gap-2 tw-text-[.88rem] tw-font-bold tw-text-[#173252]">
        <span>{label}</span>
        <input
          aria-invalid={Boolean(fieldError)}
          required={required}
          type={type}
          value={form[key]}
          onChange={(event) => updateField(key, event.target.value)}
          className={cn(
            "tw-h-11 tw-rounded-[9px] tw-border tw-border-solid tw-bg-white tw-px-3 tw-outline-none focus:tw-ring-4",
            fieldError
              ? "tw-border-red-400 focus:tw-border-red-500 focus:tw-ring-red-500/10"
              : "tw-border-[#d7e3f2] focus:tw-border-[#176fff] focus:tw-ring-[#176fff]/10",
          )}
        />
        {fieldError ? <span className="tw-text-[.78rem] tw-font-semibold tw-leading-5 tw-text-red-600">{fieldError}</span> : null}
      </label>
    );
  }

  const parkingLotCountError = fieldErrors.expectedParkingLotCount;

  return (
    <ClientPage>
      <main className="tw-bg-[#f5f9ff] tw-py-10">
        <section className="tw-mx-auto tw-grid tw-w-[min(980px,calc(100%_-_32px))] tw-grid-cols-[.8fr_1.2fr] tw-overflow-hidden tw-rounded-[22px] tw-bg-white tw-shadow-[0_24px_60px_rgba(18,59,110,.14)] max-[760px]:tw-grid-cols-1">
          <aside className="tw-bg-[linear-gradient(145deg,#061d42,#1267df)] tw-p-9 tw-text-white">
            <p className="tw-m-0 tw-text-sm tw-font-black tw-uppercase tw-tracking-[.14em] tw-text-[#8fc0ff]">CoParking for business</p>
            <h1 className="tw-m-0 tw-mt-4 tw-font-[Cambria] tw-text-4xl tw-font-bold">Trở thành đối tác CoParking</h1>
            <p className="tw-mt-5 tw-leading-7 tw-text-[#d4e5ff]">Đưa bãi xe của bạn lên một nền tảng quản lý tập trung, linh hoạt và sẵn sàng mở rộng.</p>
            <ul className="tw-mt-8 tw-grid tw-gap-4 tw-list-none tw-p-0 tw-text-sm tw-font-semibold">
              {["Tư vấn mô hình vận hành phù hợp", "Hỗ trợ cấu hình bãi và thiết bị", "Đào tạo đội ngũ vận hành"].map((item) => (
                <li key={item}><i className="fas fa-check-circle tw-mr-3 tw-text-[#67b4ff]" />{item}</li>
              ))}
            </ul>
          </aside>

          <section className="tw-p-8 max-[520px]:tw-p-5">
            <div className="tw-flex tw-items-center tw-justify-between">
              <div>
                <h2 className="tw-m-0 tw-text-2xl tw-font-black tw-text-[#102b50]">Đăng ký đối tác</h2>
                <p className="tw-m-0 tw-mt-1 tw-text-sm tw-text-[#65809f]">Thông tin sẽ được CoParking xác minh trước khi cấp tài khoản.</p>
              </div>
              <Link to="/" className="tw-text-sm tw-font-bold tw-text-[#176fff]">Trang chủ</Link>
            </div>
            {notice ? <p className="tw-mt-5 tw-rounded-[9px] tw-bg-emerald-50 tw-p-3 tw-text-sm tw-font-semibold tw-text-emerald-700">{notice}</p> : null}
            {error ? <p className="tw-mt-5 tw-rounded-[9px] tw-bg-red-50 tw-p-3 tw-text-sm tw-font-semibold tw-text-red-700">{error}</p> : null}

            <form onSubmit={submit} className="tw-mt-6 tw-grid tw-grid-cols-2 tw-gap-4 max-[520px]:tw-grid-cols-1">
              {field("organizationName", "Tên đơn vị / bãi xe")}
              {field("organizationCode", "Mã đơn vị dự kiến (VD: PARKING_A)")}
              {field("representativeName", "Họ tên người đại diện")}
              {field("phoneNumber", "Số điện thoại", "tel")}
              {field("email", "Email liên hệ", "email")}
              <label className="tw-grid tw-gap-2 tw-text-[.88rem] tw-font-bold tw-text-[#173252]">
                <span>Số bãi dự kiến quản lý</span>
                <input
                  aria-invalid={Boolean(parkingLotCountError)}
                  required
                  min="1"
                  max="1000"
                  type="number"
                  value={form.expectedParkingLotCount}
                  onChange={(event) => updateField("expectedParkingLotCount", event.target.value)}
                  className={cn(
                    "tw-h-11 tw-rounded-[9px] tw-border tw-border-solid tw-px-3 tw-outline-none focus:tw-ring-4",
                    parkingLotCountError ? "tw-border-red-400 focus:tw-border-red-500 focus:tw-ring-red-500/10" : "tw-border-[#d7e3f2] focus:tw-border-[#176fff] focus:tw-ring-[#176fff]/10",
                  )}
                />
                {parkingLotCountError ? <span className="tw-text-[.78rem] tw-font-semibold tw-leading-5 tw-text-red-600">{parkingLotCountError}</span> : null}
              </label>
              <label className="tw-col-span-2 tw-grid tw-gap-2 tw-text-[.88rem] tw-font-bold tw-text-[#173252] max-[520px]:tw-col-span-1">
                <span>Địa chỉ</span>
                <input required value={form.address} onChange={(event) => updateField("address", event.target.value)} className="tw-h-11 tw-rounded-[9px] tw-border tw-border-solid tw-border-[#d7e3f2] tw-px-3" />
              </label>
              <label className="tw-col-span-2 tw-grid tw-gap-2 tw-text-[.88rem] tw-font-bold tw-text-[#173252] max-[520px]:tw-col-span-1">
                <span>Mô tả bãi xe <small className="tw-font-normal">(tuỳ chọn)</small></span>
                <textarea value={form.parkingOperationDescription} onChange={(event) => updateField("parkingOperationDescription", event.target.value)} className="tw-min-h-24 tw-rounded-[9px] tw-border tw-border-solid tw-border-[#d7e3f2] tw-p-3" placeholder="Loại xe phục vụ, số tầng/hầm, thiết bị hiện có..." />
              </label>
              <button disabled={saving} className="tw-col-span-2 tw-mt-2 tw-min-h-12 tw-rounded-[9px] tw-bg-[#176fff] tw-font-black tw-text-white tw-shadow-[0_12px_22px_rgba(23,111,255,.24)] disabled:tw-opacity-60 max-[520px]:tw-col-span-1">
                {saving ? "Đang gửi hồ sơ..." : "Gửi đăng ký đối tác"} <i className="fas fa-arrow-right tw-ml-2" />
              </button>
            </form>
          </section>
        </section>
      </main>
      <PublicFooter />
    </ClientPage>
  );
}
