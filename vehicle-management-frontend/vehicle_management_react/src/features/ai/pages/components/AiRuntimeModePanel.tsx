import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui";
import { changeAiRuntimeMode, getAiRuntimeMode, type AiRuntimeMode, type AiRuntimeState } from "@/features/ai/api/aiModelAdminApi";

const labels: Record<AiRuntimeMode, string> = { GOOGLE_API: "Google API", QWEN_LOCAL: "Qwen local" };
const rank: Record<AiRuntimeState["status"], number> = { SWITCHING: 0, PREPARING: 1, READY: 2, ERROR: 3 };

export function AiRuntimeModePanel({ canManage }: { canManage: boolean }) {
  const [state, setState] = useState<AiRuntimeState | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const alive = useRef(true);
  const changing = useRef(false);
  function accept(next: AiRuntimeState) {
    if (!alive.current) return;
    setState(current => !current || next.epoch > current.epoch ||
      (next.epoch === current.epoch && rank[next.status] >= rank[current.status]) ? next : current);
  }
  useEffect(() => {
    alive.current = true;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;
    async function poll() {
      try {
        const response = await getAiRuntimeMode();
        if (cancelled || !alive.current) return;
        accept(response.data);
        if (!changing.current) setError("");
        timer = setTimeout(() => void poll(), response.data.status === "SWITCHING" || response.data.status === "PREPARING" ? 1500 : 15000);
      } catch (caught) {
        if (cancelled || !alive.current) return;
        setError(caught instanceof Error ? caught.message : "Không thể đọc chế độ AI.");
        timer = setTimeout(() => void poll(), 15000);
      }
    }
    void poll();
    return () => { cancelled = true; alive.current = false; clearTimeout(timer); };
  }, [state?.status]);

  async function choose(mode: AiRuntimeMode) {
    if (!state || changing.current || state.status === "SWITCHING" || !canManage || mode === state.mode) return;
    changing.current = true;
    setSaving(true); setError("");
    try { accept((await changeAiRuntimeMode(mode, state.epoch)).data); }
    catch (caught) {
      if (alive.current) setError(caught instanceof Error ? caught.message : "Không thể chuyển chế độ AI.");
      try { accept((await getAiRuntimeMode()).data); } catch { /* Retain last confirmed server state. */ }
    } finally {
      changing.current = false;
      if (alive.current) setSaving(false);
    }
  }
  const selected = state?.status === "SWITCHING" ? state.targetMode : state?.mode;
  return <section aria-label="Chế độ chatbot AI" className="tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-4">
    <h2 className="tw-m-0 tw-text-base tw-font-bold">Chế độ chatbot AI</h2>
    <p className="tw-mt-1 tw-text-sm tw-text-slate-600">Chọn một chế độ cho toàn hệ thống. Trợ lý không tự chuyển sang bên còn lại khi có lỗi.</p>
    <div className="tw-flex tw-flex-wrap tw-gap-2">
      {(["QWEN_LOCAL", "GOOGLE_API"] as const).map(mode => <Button key={mode}
        aria-pressed={selected === mode} variant={selected === mode ? "primary" : "secondary"}
        disabled={!canManage || !state || saving || state.status === "SWITCHING" || state.mode === mode}
        onClick={() => void choose(mode)}>{labels[mode]}</Button>)}
    </div>
    <p role="status" aria-live="polite" className="tw-mb-0 tw-mt-3 tw-text-sm">
      {!state ? "Đang đọc cấu hình…" : state.status === "SWITCHING"
        ? `Đang chuyển sang ${labels[state.targetMode]}. Đang chờ tác vụ cũ kết thúc.`
        : state.status === "PREPARING" ? `Đã chọn ${labels[state.mode]}. Chỉ mục tìm tài liệu bằng AI chưa sẵn sàng. FAQ đã duyệt vẫn có thể trả lời mà không gọi model.`
        : state.status === "ERROR" ? "Chưa thể áp dụng chế độ. Kiểm tra cấu hình và cảnh báo vận hành."
        : `Đã áp dụng: ${labels[state.mode]}.`}
    </p>
    {!canManage ? <p className="tw-mb-0 tw-text-sm">Bạn không có quyền đổi chế độ AI.</p> : null}
    {error ? <p role="alert" className="tw-mb-0 tw-text-sm tw-text-red-700">{error}</p> : null}
  </section>;
}
