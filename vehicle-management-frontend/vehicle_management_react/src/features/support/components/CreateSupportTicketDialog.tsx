import { useEffect, useRef, useState } from "react";
import { Button, Modal } from "@/components/ui";
import {
  type SaveSupportTicketRequest,
  type SupportTicketResponse,
} from "@/features/support/api/supportApi";
import { SupportCategoryPicker } from "@/features/support/components/SupportCategoryPicker";
import { isSupportCategoryError, useAvailableSupportCategories } from "@/features/support/hooks/useAvailableSupportCategories";
import { validateSupportTicketForm } from "@/features/support/utils/validateSupportTicketForm";

const priorityLabels = { LOW: "Thấp", NORMAL: "Bình thường", HIGH: "Cao", URGENT: "Khẩn cấp" } as const;
const emptyForm: SaveSupportTicketRequest = { categoryId: "", content: "", title: "" };

function getErrorMessage(caught: unknown, fallback: string) {
  if (caught instanceof TypeError && caught.message.toLowerCase().includes("failed to fetch")) {
    return "Không thể kết nối đến hệ thống. Vui lòng kiểm tra dịch vụ backend và thử lại.";
  }
  return caught instanceof Error ? caught.message : fallback;
}

export function CreateSupportTicketDialog({
  createTicket,
  onClose,
  onCreated,
  open,
}: {
  createTicket: (payload: SaveSupportTicketRequest, idempotencyKey: string) => Promise<SupportTicketResponse>;
  onClose: () => void;
  onCreated: (ticket: SupportTicketResponse) => void;
  open: boolean;
}) {
  const categoryState = useAvailableSupportCategories(open);
  const { categories, loading } = categoryState;
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const idempotencyKeyRef = useRef("");

  useEffect(() => {
    if (!open) return;
    setForm(emptyForm);
    setError("");
    idempotencyKeyRef.current = globalThis.crypto.randomUUID();
  }, [open]);

  const selectedCategory = categories.find((category) => category.categoryId === form.categoryId);

  async function submit() {
    const payload = { categoryId: form.categoryId, content: form.content.trim(), title: form.title.trim() };
    const validationError = validateSupportTicketForm(payload);
    if (validationError) {
      setError(validationError);
      return;
    }

    setSaving(true);
    setError("");
    try {
      const ticket = await createTicket(payload, idempotencyKeyRef.current);
      onCreated(ticket);
      onClose();
    } catch (caught) {
      setError(getErrorMessage(caught, "Không thể tạo phiếu hỗ trợ."));
      if (isSupportCategoryError(caught)) {
        setForm((current) => ({ ...current, categoryId: "" }));
        await categoryState.reload();
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      actions={<><Button variant="secondary" disabled={saving} onClick={onClose}>Hủy</Button><Button disabled={loading || saving || !selectedCategory || Boolean(categoryState.error)} loading={saving} onClick={() => void submit()}>Tạo phiếu hỗ trợ</Button></>}
      description="Chỉ báo lỗi chức năng hệ thống. Dùng module nghiệp vụ tương ứng để xử lý các yêu cầu vận hành."
      onClose={() => !saving && onClose()}
      open={open}
      title="Tạo phiếu hỗ trợ"
      width="lg"
    >
      <div className="tw-grid tw-gap-4">
        {error ? <div role="alert" className="tw-rounded-lg tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-p-3 tw-text-sm tw-font-bold tw-text-red-700">{error}</div> : null}
        <SupportCategoryPicker state={categoryState} disabled={saving} value={form.categoryId}
            onChange={(categoryId) => setForm((current) => ({ ...current, categoryId }))} />
        <label className="tw-grid tw-gap-1.5 tw-text-sm tw-font-bold tw-text-slate-700">Mức ưu tiên
          <input readOnly className="tw-h-11 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-slate-50 tw-px-3" value={selectedCategory ? priorityLabels[selectedCategory.priority] : "Tự động theo danh mục"} />
        </label>
        <label className="tw-grid tw-gap-1.5 tw-text-sm tw-font-bold tw-text-slate-700">Tiêu đề
          <input maxLength={150} disabled={saving} className="tw-h-11 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-px-3" value={form.title} onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))} />
        </label>
        <label className="tw-grid tw-gap-1.5 tw-text-sm tw-font-bold tw-text-slate-700">Nội dung chi tiết
          <textarea maxLength={4000} disabled={saving} placeholder="Nêu rõ chức năng gặp lỗi, bước thực hiện và lỗi thực tế." className="tw-min-h-28 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-p-3" value={form.content} onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))} />
        </label>
      </div>
    </Modal>
  );
}
