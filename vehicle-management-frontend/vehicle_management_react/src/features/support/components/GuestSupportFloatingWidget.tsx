import { useEffect, useRef, useState, type FormEvent } from "react";

import { getGuestAssistantStatus, type PublicSupportStatus } from "@/features/support/api/guestSupportApi";
import { CaptchaChallenge } from "@/features/support/components/CaptchaChallenge";
import { GuestSupportRequests } from "@/features/support/components/GuestSupportRequests";
import { SupportComposerActions } from "@/features/support/components/SupportComposerActions";
import { SupportSparkleIcon } from "@/features/support/components/SupportSparkleIcon";
import { useGuestSupportConversation } from "@/features/support/context/GuestSupportConversationProvider";
import { OPEN_SUPPORT_WIDGET_EVENT } from "@/features/support/supportWidgetEvents";

export function GuestSupportFloatingWidget() {
  const { sessionId, messages, draft, setDraft, busy, error, send } = useGuestSupportConversation();
  const [open, setOpen] = useState(false);
  const [supportStatus, setSupportStatus] = useState<PublicSupportStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(false);
  const [statusError, setStatusError] = useState(false);
  const [statusAttempt, setStatusAttempt] = useState(0);
  const [proof, setProof] = useState<string | null>(null);
  const [captchaReset, setCaptchaReset] = useState(0);
  const [requestMode, setRequestMode] = useState<"create" | "lookup" | null>(null);
  const [suggestedContent, setSuggestedContent] = useState("");
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const endRef = useRef<HTMLDivElement | null>(null);
  const buttonRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    const handleOpen = () => setOpen(true);
    window.addEventListener(OPEN_SUPPORT_WIDGET_EVENT, handleOpen);
    return () => window.removeEventListener(OPEN_SUPPORT_WIDGET_EVENT, handleOpen);
  }, []);

  useEffect(() => {
    if (!open || supportStatus) return;
    let active = true;
    setStatusLoading(true);
    setStatusError(false);
    void getGuestAssistantStatus()
      .then((response) => { if (active) { setSupportStatus(response.data); setStatusLoading(false); } })
      .catch(() => { if (active) { setStatusError(true); setStatusLoading(false); } });
    return () => { active = false; };
  }, [open, supportStatus, statusAttempt]);

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ block: "nearest" });
  }, [open, messages, busy, error]);

  useEffect(() => {
    if (!open || requestMode) return;
    inputRef.current?.focus();
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") { setOpen(false); buttonRef.current?.focus(); }
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [open, requestMode]);

  function createRequest(content = "") {
    setSuggestedContent(content);
    setRequestMode("create");
  }

  async function submitQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft.trim() || busy || !supportStatus?.enabled || (supportStatus.captchaRequiredForAssistant && !proof)) return;
    await send(draft, proof ?? undefined);
    if (supportStatus.captchaRequiredForAssistant) {
      setProof(null);
      setCaptchaReset((value) => value + 1);
    }
  }

  const canSend = Boolean(supportStatus?.enabled) && !busy && !(supportStatus?.captchaRequiredForAssistant && !proof);
  return (
    <>
      {open ? <section
        data-support-floating-panel role="dialog" aria-label="Trợ lý hỗ trợ CoParking"
        className="vm-support-floating-panel tw-fixed tw-bottom-28 tw-right-6 tw-z-[1079] tw-flex tw-h-[min(620px,calc(100dvh-9rem))] tw-w-[min(410px,calc(100vw-2rem))] tw-flex-col tw-overflow-hidden tw-rounded-2xl tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-shadow-[0_24px_70px_rgba(15,23,42,0.28)] max-[480px]:tw-bottom-24 max-[480px]:tw-left-2 max-[480px]:tw-right-2 max-[480px]:tw-w-auto"
      >
        <header className="tw-flex tw-shrink-0 tw-items-center tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-p-4">
          <span className="tw-flex tw-h-10 tw-w-10 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-cyan-50 tw-p-2"><SupportSparkleIcon /></span>
          <div className="tw-min-w-0 tw-flex-1"><strong className="tw-text-sm tw-text-slate-900">Trợ lý hỗ trợ CoParking</strong><p className="tw-m-0 tw-text-xs tw-text-slate-500">Hỗ trợ thông tin công khai</p></div>
          <button type="button" aria-label="Đóng trợ lý" className="tw-h-10 tw-w-10 tw-shrink-0 tw-rounded-full tw-border-0 tw-bg-slate-100 tw-text-slate-600" onClick={() => { setOpen(false); buttonRef.current?.focus(); }}><i className="fas fa-times" /></button>
        </header>
        <div className="tw-min-h-0 tw-flex-1 tw-overflow-y-auto tw-bg-slate-50 tw-p-3">
          <p className="tw-rounded-xl tw-bg-white tw-p-3 tw-text-sm tw-text-slate-600">Trợ lý chỉ sử dụng thông tin công khai và không thể xem hay thay đổi dữ liệu tài khoản. Bạn có thể hỏi hoặc tạo phiếu hỗ trợ tại đây.</p>
          <div role="log" aria-label="Hội thoại hỗ trợ công khai" aria-live="polite" className="tw-grid tw-gap-3">
            {messages.map((message, index) => <article key={message.id} className={`tw-max-w-[95%] tw-break-words tw-rounded-xl tw-p-3 tw-text-sm ${message.role === "user" ? "tw-justify-self-end tw-bg-sky-700 tw-text-white" : "tw-justify-self-start tw-bg-white tw-text-slate-700 tw-shadow-sm"}`}>
              <p className="tw-m-0 tw-whitespace-pre-wrap">{message.content.replace(/gửi yêu cầu hỗ trợ bên dưới/gi, "chọn Tạo phiếu hỗ trợ")}</p>
              {message.answer?.citations.length ? <div className="tw-mt-2 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-p-2">
                <strong className="tw-text-[11px] tw-uppercase tw-text-slate-500">Nguồn tham khảo</strong>
                <ul className="tw-mb-0 tw-list-none tw-pl-0 tw-text-xs">{message.answer.citations.map((citation) => <li key={`${citation.label}-${citation.title}`}>
                  [{citation.label}] {citation.title}{citation.page ? ` · trang ${citation.page}` : ""}{citation.section ? ` · ${citation.section}` : ""}
                </li>)}</ul>
              </div> : null}
              {message.answer?.handoffRecommended ? <button type="button" className="tw-mt-3 tw-rounded-lg tw-border-0 tw-bg-sky-700 tw-px-3 tw-py-2 tw-font-bold tw-text-white" onClick={() => createRequest(messages[index - 1]?.content ?? "")}>Tạo phiếu hỗ trợ</button> : null}
            </article>)}
          </div>
          {busy ? <p role="status" className="tw-mt-3 tw-text-sm tw-text-slate-600">Trợ lý đang xử lý...</p> : null}
          {error ? <p role="alert" className="tw-mt-3 tw-rounded-lg tw-bg-red-50 tw-p-3 tw-text-sm tw-text-red-700">{error.message}</p> : null}
          <div ref={endRef} />
        </div>
        <footer className="tw-shrink-0 tw-border-0 tw-border-t tw-border-solid tw-border-slate-200 tw-bg-white">
          <SupportComposerActions canAttach={false} showAttachments={false} historyLabel="Tra cứu phiếu" onFilesSelected={() => undefined} onCreateTicket={() => createRequest()} onOpenHistory={() => setRequestMode("lookup")} />
          {statusLoading ? <p role="status" className="tw-m-0 tw-px-3 tw-pt-2 tw-text-xs">Đang kiểm tra trạng thái trợ lý...</p> : null}
          {supportStatus?.enabled === false ? <p className="tw-m-0 tw-px-3 tw-pt-2 tw-text-xs tw-text-amber-800">Trợ lý đang tạm ngưng. Bạn vẫn có thể tạo và tra cứu phiếu hỗ trợ.</p> : null}
          {statusError ? <div role="alert" className="tw-px-3 tw-pt-2 tw-text-xs tw-text-red-700">Không thể kiểm tra dịch vụ hỗ trợ. <button type="button" className="tw-border-0 tw-bg-transparent tw-font-bold tw-underline" onClick={() => setStatusAttempt((value) => value + 1)}>Thử lại</button></div> : null}
          {supportStatus?.captchaEnabled && supportStatus.captchaRequiredForAssistant ? <div className="tw-max-h-32 tw-overflow-y-auto tw-px-3 tw-pt-2"><CaptchaChallenge purpose="ASSISTANT" resetKey={captchaReset} sessionId={sessionId} siteKey={supportStatus.captchaSiteKey ?? ""} onProof={setProof} /></div> : null}
          <form className="tw-flex tw-items-end tw-gap-2 tw-p-3" onSubmit={submitQuestion}>
            <textarea ref={inputRef} aria-label="Nhập tin nhắn hỗ trợ" className="tw-max-h-24 tw-min-h-10 tw-min-w-0 tw-flex-1 tw-resize-none tw-rounded-xl tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-px-3 tw-py-2 tw-text-sm tw-text-slate-800" maxLength={1000} rows={1} disabled={busy} placeholder="Nhập nội dung cần hỗ trợ..." value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); } }} />
            <button type="submit" aria-label="Gửi tin nhắn" disabled={!canSend || !draft.trim()} className="tw-flex tw-h-10 tw-w-10 tw-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-sky-700 tw-text-white disabled:tw-bg-slate-300"><i className={busy ? "fas fa-spinner fa-spin" : "fas fa-paper-plane"} /></button>
          </form>
        </footer>
      </section> : null}
      <GuestSupportRequests mode={requestMode} onClose={() => setRequestMode(null)} sessionId={sessionId} supportStatus={supportStatus} suggestedContent={suggestedContent} />
      <button ref={buttonRef} data-support-floating-button type="button" aria-expanded={open} aria-label={open ? "Đóng Trợ lý hỗ trợ CoParking" : "Mở Trợ lý hỗ trợ CoParking"} title="Trợ lý hỗ trợ CoParking" onClick={() => setOpen((value) => !value)} className="vm-support-floating-button tw-fixed tw-bottom-10 tw-right-6 tw-z-[1280] tw-inline-flex tw-h-14 tw-w-14 tw-items-center tw-justify-center tw-rounded-full tw-border tw-border-solid tw-border-slate-100 tw-bg-white tw-p-3 tw-text-slate-700 tw-shadow-[0_4px_12px_rgba(15,23,42,0.15)] max-[768px]:tw-bottom-4">
        {open ? <i className="fas fa-times tw-text-lg" /> : <SupportSparkleIcon />}
      </button>
    </>
  );
}
