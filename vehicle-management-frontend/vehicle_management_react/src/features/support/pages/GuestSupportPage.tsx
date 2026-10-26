import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { isApiError } from "@/core/api/apiError";
import {
  accessGuestSupportRequest,
  askGuestAssistant,
  createGuestSupportRequest,
  getGuestAssistantStatus,
  getPublicSupportCategories,
  type GuestAssistantAnswer,
  type GuestSupportRequest,
  type PublicSupportCategory,
} from "@/features/support/api/guestSupportApi";
import { ClientPage } from "@/shared/components/layout/ClientPage";

const SESSION_KEY = "coparking.guest-support.session";
const CAPABILITY_KEY = "coparking.guest-support.capability";

type ErrorState = { kind: "offline" | "rate-limited" | "expired" | "generic"; message: string } | null;

function randomKey() {
  return crypto.randomUUID();
}

function getSessionId() {
  const current = sessionStorage.getItem(SESSION_KEY);
  if (current) return current;
  const created = randomKey();
  sessionStorage.setItem(SESSION_KEY, created);
  return created;
}

function classifyError(error: unknown): NonNullable<ErrorState> {
  if (isApiError(error)) {
    if (error.status === 429) return { kind: "rate-limited", message: error.message };
    if (error.status === 404) return { kind: "expired", message: "Mã truy cập không đúng hoặc đã hết hạn." };
    if (error.kind === "network" || error.kind === "timeout") {
      return { kind: "offline", message: "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng và thử lại." };
    }
  }
  return { kind: "generic", message: error instanceof Error ? error.message : "Đã có lỗi xảy ra." };
}

export function GuestSupportPage() {
  const sessionId = useMemo(getSessionId, []);
  const [categories, setCategories] = useState<PublicSupportCategory[]>([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [assistantEnabled, setAssistantEnabled] = useState<boolean | null>(null);
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState<GuestAssistantAnswer | null>(null);
  const [assistantBusy, setAssistantBusy] = useState(false);
  const [assistantError, setAssistantError] = useState<ErrorState>(null);
  const [requestBusy, setRequestBusy] = useState(false);
  const [requestError, setRequestError] = useState<ErrorState>(null);
  const [createdRequest, setCreatedRequest] = useState<GuestSupportRequest | null>(null);
  const [referenceCode, setReferenceCode] = useState("");
  const [capabilityToken, setCapabilityToken] = useState("");
  const [lookupResult, setLookupResult] = useState<GuestSupportRequest | null>(null);

  useEffect(() => {
    void getPublicSupportCategories()
      .then((response) => setCategories(response.data))
      .catch((error) => {
        setRequestError(classifyError(error));
      })
      .finally(() => setCategoriesLoading(false));
    void getGuestAssistantStatus()
      .then((response) => setAssistantEnabled(response.data.enabled))
      .catch(() => setAssistantEnabled(false));
    const saved = sessionStorage.getItem(CAPABILITY_KEY);
    if (saved) {
      try {
        const credential = JSON.parse(saved) as { referenceCode: string; capabilityToken: string };
        setReferenceCode(credential.referenceCode);
        setCapabilityToken(credential.capabilityToken);
      } catch {
        sessionStorage.removeItem(CAPABILITY_KEY);
      }
    }
  }, []);

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question.trim() || assistantBusy) return;
    setAssistantBusy(true);
    setAssistantError(null);
    try {
      const response = await askGuestAssistant(question.trim(), sessionId);
      setAnswer(response.data);
    } catch (error) {
      setAssistantError(classifyError(error));
    } finally {
      setAssistantBusy(false);
    }
  }

  async function submitRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (requestBusy) return;
    const form = new FormData(event.currentTarget);
    setRequestBusy(true);
    setRequestError(null);
    try {
      const response = await createGuestSupportRequest({
        categoryId: String(form.get("categoryId") ?? ""),
        title: String(form.get("title") ?? ""),
        content: String(form.get("content") ?? ""),
        email: String(form.get("email") ?? ""),
        phone: String(form.get("phone") ?? ""),
        sessionId,
      }, randomKey());
      setCreatedRequest(response.data);
      if (response.data.capabilityToken) {
        const credential = { referenceCode: response.data.referenceCode, capabilityToken: response.data.capabilityToken };
        sessionStorage.setItem(CAPABILITY_KEY, JSON.stringify(credential));
        setReferenceCode(credential.referenceCode);
        setCapabilityToken(credential.capabilityToken);
      }
      event.currentTarget.reset();
    } catch (error) {
      setRequestError(classifyError(error));
    } finally {
      setRequestBusy(false);
    }
  }

  async function lookupRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRequestError(null);
    setLookupResult(null);
    try {
      const response = await accessGuestSupportRequest(referenceCode.trim(), capabilityToken.trim());
      setLookupResult(response.data);
    } catch (error) {
      const classified = classifyError(error);
      setRequestError(classified);
      if (classified.kind === "expired") sessionStorage.removeItem(CAPABILITY_KEY);
    }
  }

  return (
    <ClientPage>
      <main className="tw-mx-auto tw-grid tw-w-full tw-max-w-6xl tw-gap-6 tw-px-4 tw-py-10">
        <header className="tw-rounded-vm-lg tw-bg-slate-950 tw-p-7 tw-text-white">
          <p className="tw-m-0 tw-text-sm tw-font-bold tw-uppercase tw-tracking-widest tw-text-blue-200">Trợ giúp công khai</p>
          <h1 className="tw-mb-2 tw-mt-2 tw-text-3xl tw-font-black">Bạn cần hỗ trợ trước khi đăng nhập?</h1>
          <p className="tw-m-0 tw-max-w-3xl tw-text-slate-200">Hỏi về đăng ký, xác minh email, hoàn thiện hồ sơ và quy trình chờ phê duyệt. Đăng nhập nếu bạn cần xem lịch sử gắn với tài khoản.</p>
          <div className="tw-mt-4 tw-flex tw-gap-3"><Link className="tw-rounded-vm-md tw-bg-white tw-px-4 tw-py-2 tw-font-bold tw-text-slate-950" to="/guide">Xem hướng dẫn</Link><Link className="tw-rounded-vm-md tw-border tw-border-white tw-px-4 tw-py-2 tw-font-bold tw-text-white" to="/login">Đăng nhập</Link></div>
        </header>

        <section className="tw-grid tw-gap-6 lg:tw-grid-cols-2">
          <article className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-6">
            <h2 className="tw-text-xl tw-font-black">Hỏi trợ lý công khai</h2>
            <p className="tw-text-sm tw-text-slate-600">Trợ lý chỉ sử dụng tri thức PUBLIC và không thể xem hay thay đổi dữ liệu tài khoản.</p>
            {assistantEnabled === null ? <p>Đang kiểm tra trạng thái trợ lý...</p> : null}
            {assistantEnabled === false ? <p className="tw-rounded-vm-md tw-bg-amber-50 tw-p-3 tw-font-semibold tw-text-amber-800">Trợ lý đang tạm ngưng. Bạn vẫn có thể gửi yêu cầu hỗ trợ.</p> : null}
            <form className="tw-grid tw-gap-3" onSubmit={submitQuestion}>
              <label className="tw-grid tw-gap-1 tw-font-bold">Câu hỏi<textarea className="tw-min-h-28 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-3" maxLength={1000} required value={question} onChange={(event) => setQuestion(event.target.value)} /></label>
              <button className="tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-py-2.5 tw-font-black tw-text-white disabled:tw-opacity-60" disabled={!assistantEnabled || assistantBusy} type="submit">{assistantBusy ? "Đang tìm câu trả lời..." : "Hỏi trợ lý"}</button>
            </form>
            {assistantError ? <p className="tw-mt-3 tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-red-700" role="alert">{assistantError.message}</p> : null}
            {answer ? <div className="tw-mt-4 tw-rounded-vm-md tw-bg-brand-50 tw-p-4"><p className="tw-whitespace-pre-wrap tw-text-slate-800">{answer.answer}</p>{answer.citations.length ? <ul className="tw-mb-0 tw-pl-5 tw-text-xs tw-text-slate-600">{answer.citations.map((citation) => <li key={`${citation.label}-${citation.title}`}>{citation.label}: {citation.title}</li>)}</ul> : null}{answer.handoffRecommended ? <p className="tw-mb-0 tw-font-bold">Bạn nên gửi yêu cầu hỗ trợ để được kiểm tra thêm.</p> : null}</div> : null}
          </article>

          <article className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-6">
            <h2 className="tw-text-xl tw-font-black">Gửi yêu cầu hỗ trợ</h2>
            <p className="tw-text-sm tw-text-slate-600">Cung cấp ít nhất email hoặc số điện thoại. Không gửi mật khẩu, OTP hoặc thông tin thanh toán.</p>
            {categoriesLoading ? <p role="status">Đang tải danh mục hỗ trợ...</p> : null}
            <form className="tw-grid tw-gap-3" onSubmit={submitRequest}>
              <label className="tw-grid tw-gap-1 tw-font-bold">Danh mục<select className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-2.5" name="categoryId" required defaultValue=""><option value="" disabled>Chọn danh mục</option>{categories.map((category) => <option key={category.categoryId} value={category.categoryId}>{category.name}</option>)}</select></label>
              <label className="tw-grid tw-gap-1 tw-font-bold">Tiêu đề<input className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-2.5" name="title" maxLength={150} required /></label>
              <label className="tw-grid tw-gap-1 tw-font-bold">Nội dung<textarea className="tw-min-h-28 tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-3" name="content" maxLength={4000} required /></label>
              <div className="tw-grid tw-gap-3 sm:tw-grid-cols-2"><label className="tw-grid tw-gap-1 tw-font-bold">Email<input className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-2.5" name="email" type="email" maxLength={254} /></label><label className="tw-grid tw-gap-1 tw-font-bold">Điện thoại<input className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-2.5" name="phone" inputMode="tel" /></label></div>
              <button className="tw-rounded-vm-md tw-border-0 tw-bg-vm-primary tw-px-4 tw-py-2.5 tw-font-black tw-text-white disabled:tw-opacity-60" disabled={requestBusy || categoriesLoading || categories.length === 0} type="submit">{requestBusy ? "Đang gửi..." : "Gửi yêu cầu"}</button>
            </form>
            {requestError ? <p className="tw-mt-3 tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-red-700" role="alert">{requestError.message}</p> : null}
            {createdRequest ? <div className="tw-mt-4 tw-rounded-vm-md tw-bg-emerald-50 tw-p-4 tw-text-emerald-900"><strong>Đã tiếp nhận: {createdRequest.referenceCode}</strong><p className="tw-mb-0 tw-mt-1">Mã truy cập chỉ được giữ trong tab này đến khi bạn đóng trình duyệt. Hãy lưu lại nếu cần tra cứu sau.</p></div> : null}
          </article>
        </section>

        <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-6">
          <h2 className="tw-text-xl tw-font-black">Tra cứu yêu cầu Guest</h2>
          <p className="tw-text-sm tw-text-slate-600">Không dùng mã này cho ticket trong tài khoản. Mã truy cập sẽ hết hạn theo thời điểm hiển thị khi tạo yêu cầu.</p>
          <form className="tw-grid tw-gap-3 md:tw-grid-cols-[1fr_2fr_auto]" onSubmit={lookupRequest}><input aria-label="Mã yêu cầu" className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-2.5" placeholder="GS-..." required value={referenceCode} onChange={(event) => setReferenceCode(event.target.value)} /><input aria-label="Mã truy cập" className="tw-rounded-vm-md tw-border tw-border-solid tw-border-slate-300 tw-p-2.5" placeholder="Mã truy cập bảo mật" required type="password" value={capabilityToken} onChange={(event) => setCapabilityToken(event.target.value)} /><button className="tw-rounded-vm-md tw-border-0 tw-bg-slate-950 tw-px-5 tw-py-2.5 tw-font-black tw-text-white" type="submit">Tra cứu</button></form>
          {lookupResult ? <div className="tw-mt-4 tw-rounded-vm-md tw-bg-slate-50 tw-p-4"><strong>{lookupResult.referenceCode} · {lookupResult.status}</strong><p className="tw-mb-1 tw-mt-2">{lookupResult.title}</p><small>Liên hệ: {lookupResult.maskedContact}</small></div> : null}
        </section>
      </main>
    </ClientPage>
  );
}
