import type { ReactNode } from "react";

import { Button } from "./Button";
import { Modal } from "./Modal";

type ConfirmDialogProps = {
  cancelLabel?: string;
  confirmLabel?: string;
  description?: string;
  loading?: boolean;
  message: ReactNode;
  onCancel: () => void;
  onConfirm: () => void;
  open: boolean;
  title: string;
  tone?: "default" | "danger";
};

export function ConfirmDialog({
  cancelLabel = "Không",
  confirmLabel = "Có",
  description,
  loading = false,
  message,
  onCancel,
  onConfirm,
  open,
  title,
  tone = "default",
}: ConfirmDialogProps) {
  return (
    <Modal
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button disabled={loading} onClick={onCancel} variant="secondary">
            {cancelLabel}
          </Button>
          <Button loading={loading} onClick={onConfirm} variant={tone === "danger" ? "danger" : "primary"}>
            {confirmLabel}
          </Button>
        </div>
      )}
      description={description}
      onClose={() => {
        if (!loading) onCancel();
      }}
      open={open}
      title={title}
      width="sm"
    >
      <div className="tw-flex tw-items-start tw-gap-3">
        <span
          aria-hidden="true"
          className={tone === "danger"
            ? "tw-grid tw-h-10 tw-w-10 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-red-50 tw-text-red-600"
            : "tw-grid tw-h-10 tw-w-10 tw-flex-none tw-place-items-center tw-rounded-full tw-bg-blue-50 tw-text-vm-primary"}
        >
          <i className={tone === "danger" ? "fas fa-exclamation-triangle" : "fas fa-question-circle"} />
        </span>
        <div className="tw-min-w-0 tw-pt-1 tw-text-[0.9rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
          {message}
        </div>
      </div>
    </Modal>
  );
}
