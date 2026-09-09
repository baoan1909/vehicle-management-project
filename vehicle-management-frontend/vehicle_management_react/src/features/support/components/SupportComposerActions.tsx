import { useEffect, useRef, useState, type ChangeEvent } from "react";

type SupportComposerActionsProps = {
  canAttach: boolean;
  disabled?: boolean;
  onCreateTicket?: () => void;
  onFilesSelected: (files: File[]) => void;
  onOpenHistory: () => void;
};

export function SupportComposerActions({
  canAttach,
  disabled = false,
  onCreateTicket,
  onFilesSelected,
  onOpenHistory,
}: SupportComposerActionsProps) {
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (!open) return undefined;
    const close = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    window.addEventListener("pointerdown", close, true);
    window.addEventListener("keydown", closeOnEscape);
    return () => {
      window.removeEventListener("pointerdown", close, true);
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  function openImagePicker() {
    if (!canAttach || disabled) return;
    fileInputRef.current?.click();
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const images = Array.from(event.target.files ?? []).filter((file) => file.type.startsWith("image/"));
    if (images.length) onFilesSelected(images);
    event.target.value = "";
  }

  const actionClass =
    "tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-border-0 tw-bg-white tw-text-base tw-text-slate-900 hover:tw-bg-slate-50 disabled:tw-cursor-not-allowed disabled:tw-text-slate-300 disabled:hover:tw-bg-white";

  return (
    <div className="tw-relative tw-flex tw-items-center tw-gap-5 tw-border-0 tw-border-b tw-border-solid tw-border-slate-100 tw-px-4 tw-py-2">
      <input ref={fileInputRef} className="tw-hidden" type="file" accept="image/*" multiple onChange={handleFileChange} />
      <button
        type="button"
        className={actionClass}
        aria-label="Đính kèm ảnh"
        disabled={!canAttach || disabled}
        title={canAttach ? "Đính kèm ảnh" : "Bạn chưa có quyền đính kèm ảnh"}
        onClick={openImagePicker}
      >
        <i className="fas fa-paperclip" />
      </button>
      <button type="button" className={actionClass} aria-label="Chèn emoji" disabled title="Emoji sẽ được hỗ trợ ở phiên bản sau">
        <i className="far fa-smile" />
      </button>
      <button
        type="button"
        className={actionClass}
        aria-label="Chọn ảnh"
        disabled={!canAttach || disabled}
        title={canAttach ? "Chọn ảnh" : "Bạn chưa có quyền gửi ảnh"}
        onClick={openImagePicker}
      >
        <i className="far fa-image" />
      </button>
      <button type="button" className={actionClass} aria-label="Đính kèm tài liệu" disabled title="Tài liệu sẽ được hỗ trợ ở phiên bản sau">
        <i className="far fa-file-alt" />
      </button>
      <div className="tw-relative tw-inline-flex" ref={menuRef}>
        <button
          type="button"
          aria-label="Mở thêm tác vụ"
          aria-expanded={open}
          className={actionClass}
          disabled={disabled}
          title="Thêm tác vụ"
          onClick={() => setOpen((current) => !current)}
        >
          <i className="fas fa-ellipsis-h" />
        </button>

        {open ? (
          <div className="tw-absolute tw-bottom-[calc(100%+6px)] tw-right-0 tw-z-[1100] tw-w-52 tw-rounded-lg tw-border tw-border-solid tw-border-slate-200 tw-bg-white tw-p-1.5 tw-shadow-xl">
          {onCreateTicket ? (
            <button
              type="button"
              className="tw-flex tw-w-full tw-items-center tw-gap-2 tw-rounded-md tw-border-0 tw-bg-white tw-px-3 tw-py-2.5 tw-text-left tw-text-sm tw-font-bold tw-text-slate-700 hover:tw-bg-sky-50 hover:tw-text-sky-700"
              onClick={() => {
                setOpen(false);
                onCreateTicket();
              }}
            >
              <i className="far fa-life-ring" />Tạo phiếu hỗ trợ
            </button>
          ) : null}
          <button
            type="button"
            className="tw-flex tw-w-full tw-items-center tw-gap-2 tw-rounded-md tw-border-0 tw-bg-white tw-px-3 tw-py-2.5 tw-text-left tw-text-sm tw-font-bold tw-text-slate-700 hover:tw-bg-sky-50 hover:tw-text-sky-700"
            onClick={() => {
              setOpen(false);
              onOpenHistory();
            }}
          >
            <i className="far fa-list-alt" />Phiếu của tôi
          </button>
          </div>
        ) : null}
      </div>
    </div>
  );
}
