import { useEffect, useState } from "react";
import { Button, Modal } from "@/components/ui";
import {
  previewPublishLayout,
  type PublishPreviewApi,
} from "@/features/parking/api/parkingLayoutApi";

export function PublishDialog({
  onClose,
  onConfirm,
  open,
  publishing,
  expectedDraftRevision,
  zoneId,
  zoneName,
}: {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  publishing: boolean;
  expectedDraftRevision: number | null;
  zoneId: string | null;
  zoneName: string;
}) {
  const [preview, setPreview] = useState<PublishPreviewApi | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!open || !zoneId || expectedDraftRevision == null) return;
    setLoading(true);
    setError("");
    setPreview(null);
    previewPublishLayout(zoneId, expectedDraftRevision)
      .then((response) => setPreview(response.data))
      .catch((requestError: unknown) =>
        setError(requestError instanceof Error ? requestError.message : "Không tải được xem trước."),
      )
      .finally(() => setLoading(false));
  }, [expectedDraftRevision, open, zoneId]);

  return (
    <Modal open={open} onClose={onClose} title={`Xuất bản sơ đồ — ${zoneName}`}>
      {loading ? (
        <p className="tw-m-0 tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-500">Đang tính thay đổi…</p>
      ) : error ? (
        <p className="tw-m-0 tw-text-[0.85rem] tw-font-semibold tw-text-red-600">{error}</p>
      ) : preview ? (
        <div className="tw-grid tw-gap-2">
          <p className="tw-m-0 tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-700">
            Xuất bản sẽ áp dụng draft (phiên bản {preview.draftVersion}, revision {preview.draftRevision}) thành sơ đồ live.
          </p>
          <ul className="tw-m-0 tw-grid tw-gap-1 tw-pl-5 tw-text-[0.85rem] tw-font-semibold tw-text-vm-slate-700">
            <li>Ô mới: <strong>{preview.newSpaces}</strong></li>
            <li>Ô cập nhật: <strong>{preview.updatedSpaces}</strong></li>
            <li>Ô sẽ lưu trữ: <strong>{preview.archivedSpaces}</strong></li>
            <li>Phần tử sơ đồ: <strong>{preview.elementCount}</strong></li>
            <li>Sức chứa sau xuất bản: <strong>{preview.capacityAfterPublish}</strong></li>
          </ul>
          {preview.archivedSpaces > 0 && (
            <p className="tw-m-0 tw-text-[0.8rem] tw-font-bold tw-text-amber-600">
              Lưu ý: ô bị loại khỏi sơ đồ sẽ lưu trữ. Ô đang có phiên mở hoặc allocation hiệu lực sẽ chặn xuất bản.
            </p>
          )}
        </div>
      ) : null}
      <div className="tw-mt-4 tw-flex tw-justify-end tw-gap-2">
        <Button variant="secondary" onClick={onClose} disabled={publishing}>
          Hủy
        </Button>
        <Button onClick={onConfirm} loading={publishing} disabled={loading || !preview}>
          Xác nhận xuất bản
        </Button>
      </div>
    </Modal>
  );
}
