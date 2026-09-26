import { useEffect, useMemo, useState, type ReactNode } from "react";
import { DatePicker, Modal, useToast } from "@/components/ui";
import {
  activateVoucher,
  createVoucher,
  getVouchers,
  pauseVoucher,
  updateVoucher,
  type VoucherPayload,
  type VoucherResponse,
} from "@/features/pricing/api/voucherApi";
import { endOfApplicationDayIso, getApplicationTimeZone, startOfApplicationDayIso } from "@/shared/time/applicationTime";

type VoucherForm = {
  code: string;
  name: string;
  description: string;
  discountType: "PERCENTAGE" | "FIXED_AMOUNT";
  discountValue: string;
  maxDiscountAmount: string;
  minimumSubscriptionAmount: string;
  maxRedemptions: string;
  maxRedemptionsPerCustomer: string;
  validFrom: string;
  validTo: string;
  showOnDashboard: boolean;
  showOnSubscriptionPage: boolean;
  bannerTitle: string;
  bannerDescription: string;
  bannerPriority: string;
};

const emptyForm = (): VoucherForm => ({
  code: "",
  name: "",
  description: "",
  discountType: "PERCENTAGE",
  discountValue: "",
  maxDiscountAmount: "",
  minimumSubscriptionAmount: "0",
  maxRedemptions: "",
  maxRedemptionsPerCustomer: "1",
  validFrom: toDateInputValue(new Date()),
  validTo: "",
  showOnDashboard: false,
  showOnSubscriptionPage: false,
  bannerTitle: "",
  bannerDescription: "",
  bannerPriority: "0",
});

function money(value: number | null | undefined) {
  return `${new Intl.NumberFormat("vi-VN").format(Number(value ?? 0))} VNĐ`;
}

function formatCurrencyInput(value: string) {
  if (!value) return "";
  return new Intl.NumberFormat("vi-VN").format(Number(value));
}

function normalizeCurrencyInput(value: string) {
  return value.replace(/\D/g, "");
}

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatDate(value: string) {
  const parsed = parseVoucherDate(value);
  return Number.isNaN(parsed.getTime()) ? value || "--" : new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeZone: getApplicationTimeZone() }).format(parsed);
}

function parseVoucherDate(value: string) {
  const matched = value.match(/^(\d{2}):(\d{2})\s+(\d{2})-(\d{2})-(\d{4})$/);
  if (matched) {
    const [, hour, minute, day, month, year] = matched;
    return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute));
  }

  return new Date(value);
}

function validateVoucherForm(form: VoucherForm) {
  if (!form.code.trim()) return "Vui lòng nhập mã voucher.";
  if (!form.name.trim()) return "Vui lòng nhập tên chương trình.";

  const discountValue = Number(form.discountValue);
  if (!Number.isFinite(discountValue) || discountValue <= 0) {
    return "Mức giảm phải là một số lớn hơn 0.";
  }
  if (form.discountType === "PERCENTAGE" && discountValue > 100) {
    return "Mức giảm theo phần trăm không được vượt quá 100%.";
  }

  const maximumDiscount = form.maxDiscountAmount ? Number(form.maxDiscountAmount) : null;
  if (maximumDiscount != null && (!Number.isFinite(maximumDiscount) || maximumDiscount < 0)) {
    return "Giảm tối đa phải là số tiền từ 0 trở lên.";
  }

  const minimumAmount = Number(form.minimumSubscriptionAmount || 0);
  if (!Number.isFinite(minimumAmount) || minimumAmount < 0) {
    return "Đơn tối thiểu phải là số tiền từ 0 trở lên.";
  }

  const maximumRedemptions = form.maxRedemptions ? Number(form.maxRedemptions) : null;
  if (maximumRedemptions != null && (!Number.isInteger(maximumRedemptions) || maximumRedemptions <= 0)) {
    return "Tổng lượt dùng phải là số nguyên lớn hơn 0, hoặc để trống nếu không giới hạn.";
  }

  const perCustomerLimit = Number(form.maxRedemptionsPerCustomer);
  if (!Number.isInteger(perCustomerLimit) || perCustomerLimit <= 0) {
    return "Lượt dùng tối đa mỗi khách phải là số nguyên lớn hơn 0.";
  }

  if (!form.validFrom || !form.validTo) return "Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc hiệu lực.";
  if (new Date(form.validTo).getTime() <= new Date(form.validFrom).getTime()) {
    return "Ngày kết thúc hiệu lực phải sau ngày bắt đầu.";
  }

  if ((form.showOnDashboard || form.showOnSubscriptionPage) && !form.bannerTitle.trim()) {
    return "Vui lòng nhập tiêu đề khi bật hiển thị banner cho khách hàng.";
  }

  const bannerPriority = Number(form.bannerPriority || 0);
  if (!Number.isInteger(bannerPriority) || bannerPriority < 0) {
    return "Độ ưu tiên banner phải là số nguyên từ 0 trở lên.";
  }

  return null;
}

function statusTone(status: VoucherResponse["status"]) {
  return status === "ACTIVE" ? "tw-bg-emerald-50 tw-text-emerald-700" : status === "PAUSED" ? "tw-bg-amber-50 tw-text-amber-700" : "tw-bg-slate-100 tw-text-slate-600";
}

function toForm(voucher: VoucherResponse): VoucherForm {
  const toInput = (value: string) => {
    const parsed = parseVoucherDate(value);
    return Number.isNaN(parsed.getTime()) ? "" : toDateInputValue(parsed);
  };
  return {
    code: voucher.code,
    name: voucher.name,
    description: voucher.description ?? "",
    discountType: voucher.discountType,
    discountValue: String(voucher.discountValue),
    maxDiscountAmount: voucher.maxDiscountAmount == null ? "" : String(voucher.maxDiscountAmount),
    minimumSubscriptionAmount: String(voucher.minimumSubscriptionAmount ?? 0),
    maxRedemptions: voucher.maxRedemptions == null ? "" : String(voucher.maxRedemptions),
    maxRedemptionsPerCustomer: String(voucher.maxRedemptionsPerCustomer ?? 1),
    validFrom: toInput(voucher.validFrom),
    validTo: toInput(voucher.validTo),
    showOnDashboard: voucher.showOnDashboard,
    showOnSubscriptionPage: voucher.showOnSubscriptionPage,
    bannerTitle: voucher.bannerTitle ?? "",
    bannerDescription: voucher.bannerDescription ?? "",
    bannerPriority: String(voucher.bannerPriority ?? 0),
  };
}

export function VoucherManagementPage() {
  const toast = useToast();
  const [vouchers, setVouchers] = useState<VoucherResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<VoucherResponse | null>(null);
  const [form, setForm] = useState<VoucherForm>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  const filtered = useMemo(() => {
    const search = keyword.trim().toLowerCase();
    return search ? vouchers.filter((item) => `${item.code} ${item.name}`.toLowerCase().includes(search)) : vouchers;
  }, [keyword, vouchers]);

  const load = async () => {
    setLoading(true);
    try {
      const response = await getVouchers();
      setVouchers(response.data ?? []);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không thể tải danh sách voucher.", "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const openCreate = () => { setEditing(null); setForm(emptyForm()); setFormError(""); setModalOpen(true); };
  const openEdit = (voucher: VoucherResponse) => { setEditing(voucher); setForm(toForm(voucher)); setFormError(""); setModalOpen(true); };
  const updateForm = <K extends keyof VoucherForm>(key: K, value: VoucherForm[K]) => {
    setFormError("");
    setForm((current) => ({ ...current, [key]: value }));
  };

  const save = async () => {
    const validationMessage = validateVoucherForm(form);
    if (validationMessage) {
      setFormError(validationMessage);
      toast.error(validationMessage, "Dữ liệu voucher chưa hợp lệ");
      return;
    }
    const payload: VoucherPayload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      discountType: form.discountType,
      discountValue: Number(form.discountValue),
      maxDiscountAmount: form.maxDiscountAmount ? Number(form.maxDiscountAmount) : null,
      minimumSubscriptionAmount: Number(form.minimumSubscriptionAmount || 0),
      maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
      maxRedemptionsPerCustomer: Number(form.maxRedemptionsPerCustomer || 1),
      validFrom: startOfApplicationDayIso(form.validFrom)!,
      validTo: endOfApplicationDayIso(form.validTo)!,
      showOnDashboard: form.showOnDashboard,
      showOnSubscriptionPage: form.showOnSubscriptionPage,
      bannerTitle: form.bannerTitle.trim() || null,
      bannerDescription: form.bannerDescription.trim() || null,
      bannerPriority: Number(form.bannerPriority || 0),
    };
    setSaving(true);
    try {
      if (editing) await updateVoucher(editing.voucherId, payload); else await createVoucher(payload);
      toast.success(editing ? "Đã cập nhật voucher." : "Đã tạo voucher ở trạng thái nháp.", "Thành công");
      setModalOpen(false);
      await load();
    } catch (error) {
      const message = error instanceof Error ? error.message : "Không thể lưu voucher.";
      setFormError(message);
      toast.error(message, "Lưu không thành công");
    } finally { setSaving(false); }
  };

  const changeStatus = async (voucher: VoucherResponse) => {
    try {
      if (voucher.status === "ACTIVE") await pauseVoucher(voucher.voucherId); else await activateVoucher(voucher.voucherId);
      toast.success(voucher.status === "ACTIVE" ? "Voucher đã tạm dừng." : "Voucher đã được kích hoạt.", "Đã cập nhật trạng thái");
      await load();
    } catch (error) { toast.error(error instanceof Error ? error.message : "Không thể đổi trạng thái voucher.", "Thao tác không thành công"); }
  };

  return <main className="tw-space-y-5 tw-p-5">
    <header className="tw-flex tw-flex-wrap tw-items-end tw-justify-between tw-gap-3">
      <div><p className="tw-m-0 tw-text-xs tw-font-semibold tw-uppercase tw-tracking-[.14em] tw-text-[#1263e9]">Khách đăng ký</p><h1 className="tw-m-0 tw-mt-1 tw-text-2xl tw-font-bold tw-text-slate-900">Voucher vé đăng ký</h1><p className="tw-m-0 tw-mt-1 tw-text-sm tw-text-slate-500">Giảm giá chỉ dành cho vé tháng, quý và năm; không áp dụng cho vé lượt hoặc phí mất thẻ.</p></div>
      <button type="button" onClick={openCreate} className="tw-h-10 tw-rounded-md tw-border-0 tw-bg-[#1263e9] tw-px-4 tw-text-sm tw-font-semibold tw-text-white"><i className="fas fa-plus tw-mr-2" />Tạo voucher</button>
    </header>
    <section className="tw-rounded-xl tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-sm">
      <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-p-4"><strong className="tw-text-slate-800">Danh sách voucher</strong><label className="tw-relative"><i className="fas fa-search tw-pointer-events-none tw-absolute tw-left-3 tw-top-1/2 -tw-translate-y-1/2 tw-text-slate-400" /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} className="tw-h-9 tw-w-64 tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-pl-9 tw-pr-3 tw-text-sm" placeholder="Tìm theo mã, tên..." /></label></div>
      <div className="tw-overflow-x-auto"><table className="tw-w-full tw-min-w-[940px] tw-border-collapse tw-text-left tw-text-sm"><thead className="tw-bg-slate-50 tw-text-xs tw-uppercase tw-text-slate-500"><tr><th className="tw-px-4 tw-py-3">Voucher</th><th className="tw-px-4 tw-py-3">Ưu đãi</th><th className="tw-px-4 tw-py-3">Điều kiện</th><th className="tw-px-4 tw-py-3">Hiệu lực</th><th className="tw-px-4 tw-py-3">Trạng thái</th><th className="tw-px-4 tw-py-3 tw-text-right">Thao tác</th></tr></thead><tbody>{loading ? <tr><td colSpan={6} className="tw-p-8 tw-text-center tw-text-slate-500">Đang tải...</td></tr> : filtered.length === 0 ? <tr><td colSpan={6} className="tw-p-8 tw-text-center tw-text-slate-500">Chưa có voucher phù hợp.</td></tr> : filtered.map((voucher) => <tr key={voucher.voucherId} className="tw-border-0 tw-border-t tw-border-solid tw-border-slate-100"><td className="tw-px-4 tw-py-3"><strong className="tw-block tw-text-slate-800">{voucher.code}</strong><span className="tw-text-xs tw-text-slate-500">{voucher.name}</span>{voucher.showOnDashboard || voucher.showOnSubscriptionPage ? <span className="tw-mt-1 tw-inline-flex tw-rounded-full tw-bg-blue-50 tw-px-2 tw-py-0.5 tw-text-[0.65rem] tw-font-semibold tw-text-blue-700"><i className="fas fa-bullhorn tw-mr-1" />Đang quảng bá</span> : null}</td><td className="tw-px-4 tw-py-3 tw-font-semibold tw-text-slate-700">{voucher.discountType === "PERCENTAGE" ? `${voucher.discountValue}%${voucher.maxDiscountAmount ? ` · tối đa ${money(voucher.maxDiscountAmount)}` : ""}` : money(voucher.discountValue)}</td><td className="tw-px-4 tw-py-3 tw-text-xs tw-text-slate-600">Đơn tối thiểu: {money(voucher.minimumSubscriptionAmount)}<br />Tổng: {voucher.maxRedemptions ?? "Không giới hạn"} ·/khách: {voucher.maxRedemptionsPerCustomer}</td><td className="tw-px-4 tw-py-3 tw-text-xs tw-text-slate-600">{formatDate(voucher.validFrom)}<br />đến {formatDate(voucher.validTo)}</td><td className="tw-px-4 tw-py-3"><span className={`tw-inline-flex tw-rounded-full tw-px-2.5 tw-py-1 tw-text-xs tw-font-semibold ${statusTone(voucher.status)}`}>{voucher.status === "ACTIVE" ? "Đang áp dụng" : voucher.status === "PAUSED" ? "Tạm dừng" : "Nháp"}</span></td><td className="tw-space-x-2 tw-px-4 tw-py-3 tw-text-right"><button type="button" onClick={() => openEdit(voucher)} className="tw-rounded tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-2.5 tw-py-1.5 tw-text-xs tw-font-semibold tw-text-slate-700">Sửa</button><button type="button" onClick={() => void changeStatus(voucher)} className="tw-rounded tw-border tw-border-solid tw-border-blue-200 tw-bg-blue-50 tw-px-2.5 tw-py-1.5 tw-text-xs tw-font-semibold tw-text-blue-700">{voucher.status === "ACTIVE" ? "Tạm dừng" : "Kích hoạt"}</button></td></tr>)}</tbody></table></div>
    </section>
    <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? "Cập nhật voucher" : "Tạo voucher"} description="Voucher chỉ giảm giá hóa đơn của vé đăng ký." actions={<div className="tw-flex tw-justify-end tw-gap-2"><button type="button" onClick={() => setModalOpen(false)} className="tw-h-10 tw-rounded-md tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-px-4 tw-font-semibold tw-text-slate-700">Hủy</button><button type="button" disabled={saving} onClick={() => void save()} className="tw-h-10 tw-rounded-md tw-border-0 tw-bg-[#1263e9] tw-px-4 tw-font-semibold tw-text-white disabled:tw-opacity-60">{saving ? "Đang lưu..." : "Lưu voucher"}</button></div>}>
      <div className="tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1">
        {formError ? <div className="tw-col-span-2 tw-flex tw-gap-2 tw-rounded-md tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-medium tw-text-red-700 max-[640px]:tw-col-span-1" role="alert"><i className="fas fa-exclamation-circle tw-mt-0.5" /><span>{formError}</span></div> : null}
        <Field label="Mã voucher"><input value={form.code} maxLength={50} onChange={(event) => updateForm("code", event.target.value.toUpperCase())} placeholder="WELCOME10" /></Field><Field label="Tên chương trình"><input value={form.name} maxLength={150} onChange={(event) => updateForm("name", event.target.value)} placeholder="Ưu đãi khách mới" /></Field>
        <Field label="Hình thức"><select value={form.discountType} onChange={(event) => updateForm("discountType", event.target.value as VoucherForm["discountType"])}><option value="PERCENTAGE">Giảm theo phần trăm</option><option value="FIXED_AMOUNT">Giảm số tiền cố định</option></select></Field><Field label={form.discountType === "PERCENTAGE" ? "Mức giảm (%)" : "Mức giảm (VNĐ)"}><input inputMode={form.discountType === "FIXED_AMOUNT" ? "numeric" : "decimal"} type="text" value={form.discountType === "FIXED_AMOUNT" ? formatCurrencyInput(form.discountValue) : form.discountValue} onChange={(event) => updateForm("discountValue", form.discountType === "FIXED_AMOUNT" ? normalizeCurrencyInput(event.target.value) : event.target.value)} /></Field>
        <Field label="Giảm tối đa (VNĐ)"><input inputMode="numeric" type="text" value={formatCurrencyInput(form.maxDiscountAmount)} onChange={(event) => updateForm("maxDiscountAmount", normalizeCurrencyInput(event.target.value))} disabled={form.discountType === "FIXED_AMOUNT"} /></Field><Field label="Đơn tối thiểu (VNĐ)"><input inputMode="numeric" type="text" value={formatCurrencyInput(form.minimumSubscriptionAmount)} onChange={(event) => updateForm("minimumSubscriptionAmount", normalizeCurrencyInput(event.target.value))} /></Field>
        <Field className="tw-grid-rows-[40px_auto] max-[640px]:tw-grid-rows-[auto_auto]" label="Tổng lượt dùng"><input min="1" placeholder="Để trống = không giới hạn" type="number" value={form.maxRedemptions} onChange={(event) => updateForm("maxRedemptions", event.target.value)} /></Field><Field className="tw-grid-rows-[40px_auto] max-[640px]:tw-grid-rows-[auto_auto]" label="Lượt dùng tối đa/khách"><input min="1" type="number" value={form.maxRedemptionsPerCustomer} onChange={(event) => updateForm("maxRedemptionsPerCustomer", event.target.value)} /></Field>
        <Field label="Bắt đầu hiệu lực"><DatePicker ariaLabel="Chọn ngày bắt đầu hiệu lực" placeholder="Chọn ngày bắt đầu" value={form.validFrom} onChange={(value) => updateForm("validFrom", value)} /></Field><Field label="Kết thúc hiệu lực"><DatePicker ariaLabel="Chọn ngày kết thúc hiệu lực" menuAlign="right" placeholder="Chọn ngày kết thúc" value={form.validTo} onChange={(value) => updateForm("validTo", value)} /></Field>
        <Field label="Mô tả"><textarea className="tw-min-h-20" value={form.description} onChange={(event) => updateForm("description", event.target.value)} placeholder="Điều kiện hoặc nội dung hiển thị cho nhân viên." /></Field>
        <div className="tw-col-span-2 tw-rounded-md tw-border tw-border-solid tw-border-blue-100 tw-bg-blue-50/50 tw-p-3 max-[640px]:tw-col-span-1">
          <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-2"><strong className="tw-text-sm tw-text-slate-800"><i className="fas fa-bullhorn tw-mr-2 tw-text-blue-600" />Banner khách hàng</strong><Field label="Độ ưu tiên"><input className="!tw-h-9 !tw-w-20" type="number" min="0" step="1" value={form.bannerPriority} onChange={(event) => updateForm("bannerPriority", event.target.value)} /></Field></div>
          <div className="tw-mt-3 tw-flex tw-flex-wrap tw-gap-x-5 tw-gap-y-2"><label className="tw-inline-flex tw-items-center tw-gap-2 tw-text-sm tw-font-medium tw-text-slate-700"><input className="tw-h-4 tw-w-4" type="checkbox" checked={form.showOnDashboard} onChange={(event) => updateForm("showOnDashboard", event.target.checked)} />Hiển thị ở trang tổng quan và vé tháng</label><label className="tw-inline-flex tw-items-center tw-gap-2 tw-text-sm tw-font-medium tw-text-slate-700"><input className="tw-h-4 tw-w-4" type="checkbox" checked={form.showOnSubscriptionPage} onChange={(event) => updateForm("showOnSubscriptionPage", event.target.checked)} />Chỉ hiển thị ở trang vé tháng</label></div>
          <div className="tw-mt-3 tw-grid tw-grid-cols-2 tw-gap-4 max-[640px]:tw-grid-cols-1"><Field label="Tiêu đề banner"><input value={form.bannerTitle} maxLength={150} onChange={(event) => updateForm("bannerTitle", event.target.value)} placeholder="Ưu đãi chào mừng" /></Field><Field label="Nội dung banner"><input value={form.bannerDescription} maxLength={500} onChange={(event) => updateForm("bannerDescription", event.target.value)} placeholder="Giảm 10% vé tháng với mã WELCOME10" /></Field></div>
        </div>
      </div>
    </Modal>
  </main>;
}

function Field({ label, children, className = "" }: { className?: string; label: string; children: ReactNode }) {
  return <label className={`tw-grid tw-gap-1 tw-text-sm tw-font-semibold tw-text-slate-700 [&_input]:tw-h-10 [&_input]:tw-w-full [&_input]:tw-rounded-md [&_input]:tw-border [&_input]:tw-border-solid [&_input]:tw-border-slate-200 [&_input]:tw-px-3 [&_select]:tw-h-10 [&_select]:tw-w-full [&_select]:tw-rounded-md [&_select]:tw-border [&_select]:tw-border-solid [&_select]:tw-border-slate-200 [&_select]:tw-px-3 [&_textarea]:tw-w-full [&_textarea]:tw-rounded-md [&_textarea]:tw-border [&_textarea]:tw-border-solid [&_textarea]:tw-border-slate-200 [&_textarea]:tw-p-3 ${className}`}>{label}{children}</label>;
}
