import { SelectMenu } from "@/shared/components/ui/SelectMenu";
import { useEffect, useId, useRef, useState, type FormEvent } from "react";

import { Button, Modal } from "@/components/ui";
import {
  accessGuestSupportRequest, createGuestSupportRequest, getPublicSupportCategories,
  type GuestSupportRequest, type PublicSupportCategory, type PublicSupportStatus,
} from "@/features/support/api/guestSupportApi";
import { CaptchaChallenge } from "@/features/support/components/CaptchaChallenge";
import {
  classifyGuestSupportError, readGuestCredential, saveGuestCredential, type GuestSupportError,
} from "@/features/support/guestSupportState";
import { getApplicationTimeZone } from "@/shared/time/applicationTime";

const emptyForm = { categoryId: "", title: "", content: "", email: "", phone: "" };
const fieldClass = "tw-w-full tw-min-w-0 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-bg-white tw-p-2.5 tw-text-slate-900";
const statusLabels = { OPEN: "Đã tiếp nhận", IN_PROGRESS: "Đang xử lý", RESOLVED: "Đã giải quyết", CLOSED: "Đã đóng" };

export function GuestSupportRequests({ mode, onClose, sessionId, supportStatus, suggestedContent }: {
  mode: "create" | "lookup" | null;
  onClose: () => void;
  sessionId: string;
  supportStatus: PublicSupportStatus | null;
  suggestedContent: string;
}) {
  const formId = useId();
  const [form, setForm] = useState(emptyForm);
  const [categories, setCategories] = useState<PublicSupportCategory[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(false);
  const [categoriesAttempt, setCategoriesAttempt] = useState(0);
  const [categoriesError, setCategoriesError] = useState<GuestSupportError | null>(null);
  const [proof, setProof] = useState<string | null>(null);
  const [captchaReset, setCaptchaReset] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<GuestSupportError | null>(null);
  const [createdRequest, setCreatedRequest] = useState<GuestSupportRequest | null>(null);
  const [credential] = useState(readGuestCredential);
  const [referenceCode, setReferenceCode] = useState(credential?.referenceCode ?? "");
  const [capabilityToken, setCapabilityToken] = useState(credential?.capabilityToken ?? "");
  const [lookupResult, setLookupResult] = useState<GuestSupportRequest | null>(null);
  const idempotencyRef = useRef<{ payload: string; key: string } | null>(null);
  const requestRef = useRef<AbortController | null>(null);

  useEffect(() => () => requestRef.current?.abort(), []);

  useEffect(() => {
    if (mode !== "create" || categories.length) return;
    let active = true;
    setCategoriesLoading(true);
    setCategoriesError(null);
    void getPublicSupportCategories()
      .then((response) => { if (active) { setCategories(response.data); setCategoriesLoading(false); } })
      .catch((caught) => { if (active) { setCategoriesError(classifyGuestSupportError(caught)); setCategoriesLoading(false); } });
    return () => { active = false; };
  }, [mode, categories.length, categoriesAttempt]);

  useEffect(() => {
    setError(null);
    if (mode === "create" && suggestedContent) {
      setForm((current) => current.content ? current : { ...current, content: suggestedContent.slice(0, 4000) });
    }
  }, [mode, suggestedContent]);

  async function submitRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (requestRef.current) return;
    const payload = {
      categoryId: form.categoryId, title: form.title.trim(), content: form.content.trim(),
      email: form.email.trim(), phone: form.phone.trim(), sessionId,
    };
    if (!payload.categoryId) {
      setError({ kind: "generic", message: "Vui lòng chọn danh mục hỗ trợ." });
      return;
    }
    if (!payload.email && !payload.phone) {
      setError({ kind: "generic", message: "Vui lòng cung cấp email hoặc số điện thoại hợp lệ." });
      return;
    }
    const fingerprint = JSON.stringify(payload);
    if (idempotencyRef.current?.payload !== fingerprint) {
      idempotencyRef.current = { payload: fingerprint, key: crypto.randomUUID() };
    }
    const controller = new AbortController();
    requestRef.current = controller;
    setBusy(true);
    setError(null);
    try {
      const response = await createGuestSupportRequest(
        { ...payload, captchaProof: proof ?? undefined }, idempotencyRef.current.key, controller.signal,
      );
      if (controller.signal.aborted) return;
      setCreatedRequest(response.data);
      if (response.data.capabilityToken) {
        const saved = { referenceCode: response.data.referenceCode, capabilityToken: response.data.capabilityToken };
        saveGuestCredential(saved);
        setReferenceCode(saved.referenceCode);
        setCapabilityToken(saved.capabilityToken);
      }
      setForm(emptyForm);
      idempotencyRef.current = null;
    } catch (caught) {
      if (!controller.signal.aborted) setError(classifyGuestSupportError(caught));
    } finally {
      if (!controller.signal.aborted) {
        requestRef.current = null;
        setBusy(false);
        if (supportStatus?.captchaRequiredForIntake) {
          setProof(null);
          setCaptchaReset((value) => value + 1);
        }
      }
    }
  }

  async function lookupRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (requestRef.current) return;
    const controller = new AbortController();
    requestRef.current = controller;
    setBusy(true);
    setError(null);
    setLookupResult(null);
    try {
      const response = await accessGuestSupportRequest(referenceCode.trim(), capabilityToken.trim(), controller.signal);
      if (!controller.signal.aborted) setLookupResult(response.data);
    } catch (caught) {
      if (controller.signal.aborted) return;
      const classified = classifyGuestSupportError(caught);
      setError(classified);
      if (classified.kind === "expired") saveGuestCredential(null);
    } finally {
      if (!controller.signal.aborted) { requestRef.current = null; setBusy(false); }
    }
  }

  function updateField(field: keyof typeof emptyForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setCreatedRequest(null);
  }

  const createMode = mode === "create";
  return (
    <Modal
      title={createMode ? "Tạo phiếu hỗ trợ" : "Tra cứu phiếu hỗ trợ"}
      description={createMode ? "Cung cấp ít nhất email hoặc số điện thoại. Không gửi mật khẩu, OTP hoặc thông tin thanh toán." : "Nhập mã yêu cầu và mã truy cập đã được cấp khi gửi phiếu."}
      open={mode !== null}
      onClose={() => { if (!busy) onClose(); }}
      width="md"
      actions={<><Button variant="secondary" disabled={busy} onClick={onClose}>Đóng</Button><Button
        type="submit" form={formId} loading={busy}
        disabled={busy || (createMode && (categoriesLoading || !categories.length || Boolean(supportStatus?.captchaRequiredForIntake && !proof)))}
      >{createMode ? "Gửi yêu cầu" : "Tra cứu"}</Button></>}
    >
      {error ? <p role="alert" className="tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-red-700">{error.message}</p> : null}
      {createMode ? <>
        {categoriesLoading ? <p role="status">Đang tải danh mục hỗ trợ...</p> : null}
        {categoriesError ? <div role="alert"><p>{categoriesError.message}</p><Button variant="secondary" onClick={() => setCategoriesAttempt((value) => value + 1)}>Tải lại danh mục</Button></div> : null}
        <form id={formId} className="tw-grid tw-gap-3" onSubmit={submitRequest}>
          <label className="tw-grid tw-gap-1 tw-font-bold">Danh mục<SelectMenu ariaLabel="Danh mục"
                  triggerClassName={fieldClass}
                  value={form.categoryId}
                  onChange={(selectedValue) => updateField("categoryId", selectedValue)}
                  disabled={busy}
                  options={[{ label: "Chọn danh mục", value: "", disabled: true }, ...categories.map((category) => ({ label: category.name, value: category.categoryId }))]} portal clearValue="" /></label>
          <label className="tw-grid tw-gap-1 tw-font-bold">Tiêu đề<input className={fieldClass} maxLength={150} required disabled={busy} value={form.title} onChange={(event) => updateField("title", event.target.value)} /></label>
          <label className="tw-grid tw-gap-1 tw-font-bold">Nội dung<textarea className={`${fieldClass} tw-min-h-28`} maxLength={4000} required disabled={busy} value={form.content} onChange={(event) => updateField("content", event.target.value)} /></label>
          <div className="tw-grid tw-gap-3 sm:tw-grid-cols-2">
            <label className="tw-grid tw-gap-1 tw-font-bold">Email<input className={fieldClass} type="email" maxLength={254} disabled={busy} value={form.email} onChange={(event) => updateField("email", event.target.value)} /></label>
            <label className="tw-grid tw-gap-1 tw-font-bold">Điện thoại<input className={fieldClass} inputMode="tel" maxLength={20} disabled={busy} value={form.phone} onChange={(event) => updateField("phone", event.target.value)} /></label>
          </div>
          {supportStatus?.captchaEnabled && supportStatus.captchaRequiredForIntake ? <CaptchaChallenge purpose="INTAKE" resetKey={captchaReset} sessionId={sessionId} siteKey={supportStatus.captchaSiteKey ?? ""} onProof={setProof} /> : null}
        </form>
        {createdRequest ? <div role="status" className="tw-mt-4 tw-rounded-lg tw-bg-emerald-50 tw-p-3 tw-text-emerald-900">
          <strong>Đã tiếp nhận: {createdRequest.referenceCode}</strong>
          <p>Hãy lưu mã yêu cầu và mã truy cập để tra cứu sau. Mã truy cập được giữ tạm trong tab này.</p>
          {createdRequest.capabilityToken ? <details><summary>Xem mã truy cập để lưu</summary><code className="tw-block tw-break-all tw-py-2">{createdRequest.capabilityToken}</code></details> : null}
        </div> : null}
      </> : <>
        <form id={formId} className="tw-grid tw-gap-3" onSubmit={lookupRequest}>
          <label className="tw-grid tw-gap-1 tw-font-bold">Mã yêu cầu<input className={fieldClass} placeholder="GS-..." maxLength={32} required disabled={busy} value={referenceCode} onChange={(event) => { setReferenceCode(event.target.value); setLookupResult(null); }} /></label>
          <label className="tw-grid tw-gap-1 tw-font-bold">Mã truy cập<input className={fieldClass} type="password" required disabled={busy} value={capabilityToken} onChange={(event) => { setCapabilityToken(event.target.value); setLookupResult(null); }} /></label>
        </form>
        {lookupResult ? <div className="tw-mt-4 tw-rounded-lg tw-bg-slate-50 tw-p-3">
          <strong>{lookupResult.referenceCode} · {statusLabels[lookupResult.status]}</strong>
          <p className="tw-mt-2 tw-font-bold">{lookupResult.title}</p>
          <p className="tw-whitespace-pre-wrap tw-break-words">{lookupResult.content}</p>
          <p>Liên hệ: {lookupResult.maskedContact}</p>
          <small>Mã truy cập hết hạn: {new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short", timeZone: getApplicationTimeZone() }).format(new Date(lookupResult.capabilityExpiresAt))}</small>
        </div> : null}
      </>}
    </Modal>
  );
}
