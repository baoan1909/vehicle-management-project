import { useEffect, useState } from "react";

import { Button, Modal, useToast } from "@/components/ui";
import { usePlatformFeeConfiguration } from "@/features/billing/components/PlatformFeeConfigurationContext";
import {
  getErrorMessage,
  parsePercentInput,
  platformFeeInputClassName,
  versionFormRateToFraction,
  versionFractionToFormRate,
} from "@/features/billing/components/platformFeeMeta";

type PlatformFeeConfigurationDialogProps = {
  onClose: () => void;
  open: boolean;
};

export function PlatformFeeConfigurationDialog({ onClose, open }: PlatformFeeConfigurationDialogProps) {
  const toast = useToast();
  const { configuration, updateConfiguration } = usePlatformFeeConfiguration();
  const [percent, setPercent] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open && configuration) {
      setPercent(versionFractionToFormRate(configuration.maxPartnerTotalRate));
    }
  }, [configuration, open]);

  const parsedPercent = parsePercentInput(percent);
  const valid = parsedPercent !== null && parsedPercent >= 0 && parsedPercent <= 100;

  async function handleSave() {
    if (!valid || parsedPercent === null) {
      toast.error("Mức trần phải nằm trong khoảng từ 0% đến 100%.", "Dữ liệu không hợp lệ");
      return;
    }
    setSaving(true);
    try {
      await updateConfiguration(versionFormRateToFraction(parsedPercent));
      toast.success(`Đã cập nhật mức trần phí sàn thành ${parsedPercent}%.`, "Cập nhật thành công");
      onClose();
    } catch (error) {
      toast.error(getErrorMessage(error, "Không thể cập nhật mức trần phí sàn."), "Cập nhật thất bại");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2.5">
          <Button disabled={saving} onClick={onClose} variant="secondary">Hủy</Button>
          <Button disabled={!configuration || !valid} loading={saving} onClick={() => void handleSave()}>
            Lưu mức trần
          </Button>
        </div>
      )}
      description="Mức trần được áp dụng khi kiểm tra tổng tỷ lệ các khoản phí đối tác phải chịu trên mọi đơn mua vé tháng."
      onClose={onClose}
      open={open}
      title="Cấu hình giới hạn phí sàn"
      width="sm"
    >
      <div className="tw-grid tw-gap-4">
        <label className="tw-grid tw-gap-1.5">
          <span className="tw-text-[0.86rem] tw-font-black tw-text-vm-slate-700">Tổng tỷ lệ tối đa</span>
          <span className="tw-relative tw-block">
            <input
              aria-describedby="platform-fee-limit-help"
              className={`${platformFeeInputClassName} tw-pr-11`}
              inputMode="decimal"
              max="100"
              min="0"
              placeholder="Ví dụ: 30"
              step="0.0001"
              type="number"
              value={percent}
              onChange={(event) => setPercent(event.target.value)}
            />
            <span className="tw-pointer-events-none tw-absolute tw-inset-y-0 tw-right-3 tw-flex tw-items-center tw-font-black tw-text-vm-slate-500">%</span>
          </span>
          <span id="platform-fee-limit-help" className="tw-text-xs tw-font-semibold tw-leading-5 tw-text-vm-slate-500">
            Cho phép nhập từ 0% đến 100%, tối đa 4 chữ số thập phân.
          </span>
        </label>

        <div className="tw-flex tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-amber-200 tw-bg-amber-50 tw-p-3 tw-text-[0.8rem] tw-font-semibold tw-leading-5 tw-text-amber-800">
          <i className="fas fa-exclamation-triangle tw-mt-0.5 tw-flex-none" aria-hidden="true" />
          <span>Không thể hạ mức trần thấp hơn tổng tỷ lệ của các phí đang hoặc sắp có hiệu lực.</span>
        </div>

        {configuration ? (
          <p className="tw-m-0 tw-text-xs tw-font-semibold tw-text-vm-slate-400">
            Phiên bản cấu hình hiện tại: {configuration.version}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}
