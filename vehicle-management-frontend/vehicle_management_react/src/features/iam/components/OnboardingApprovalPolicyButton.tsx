import { useState } from "react";

import { Button, InfoBanner, Modal, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  getOnboardingApprovalPolicies,
  updateOnboardingApprovalPolicies,
  type OnboardingApprovalPolicies,
} from "@/features/iam/api/onboardingApprovalPolicyApi";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

const emptyPolicies: OnboardingApprovalPolicies = {
  customerAutoApproveEnabled: false,
  partnerAutoApproveEnabled: false,
  avatarAutoApproveEnabled: false,
};

function PolicySwitch({
  checked,
  description,
  disabled,
  label,
  onChange,
}: {
  checked: boolean;
  description: string;
  disabled: boolean;
  label: string;
  onChange: (checked: boolean) => void;
}) {
  return (
    <div className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
      <div>
        <p className="tw-m-0 tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">{label}</p>
        <p className="tw-mb-0 tw-mt-1 tw-text-[0.78rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-500">{description}</p>
      </div>
      <button
        aria-checked={checked}
        aria-label={label}
        className={cn(
          "tw-relative tw-mt-0.5 tw-h-7 tw-w-12 tw-flex-shrink-0 tw-rounded-full tw-border-0 tw-transition focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus disabled:tw-cursor-not-allowed disabled:tw-opacity-60",
          checked ? "tw-bg-vm-primary" : "tw-bg-vm-slate-200",
        )}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        role="switch"
        type="button"
      >
        <span className={cn(
          "tw-absolute tw-top-1 tw-h-5 tw-w-5 tw-rounded-full tw-bg-white tw-shadow tw-transition-all",
          checked ? "tw-left-6" : "tw-left-1",
        )} />
      </button>
    </div>
  );
}

export function OnboardingApprovalPolicyButton() {
  const { user } = useAuth();
  const toast = useToast();
  const canManagePolicies = hasAnyPermission(user, ["ORGANIZATION_CREATE_ALL"])
    || hasAnyPermission(user, ["ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL"]);
  const canManageCustomerPolicy = hasAnyPermission(user, ["ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL"]);
  const canManagePartnerPolicy = hasAnyPermission(user, ["ORGANIZATION_CREATE_ALL"]);
  const canManageAvatarPolicy = canManageCustomerPolicy && canManagePartnerPolicy;
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [policies, setPolicies] = useState<OnboardingApprovalPolicies>(emptyPolicies);

  if (!canManagePolicies) return null;

  async function openModal() {
    setOpen(true);
    setLoading(true);
    try {
      setPolicies(await getOnboardingApprovalPolicies());
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không tải được cấu hình tự động duyệt.", "Tải cấu hình thất bại");
      setOpen(false);
    } finally {
      setLoading(false);
    }
  }

  async function savePolicies() {
    setSaving(true);
    try {
      setPolicies(await updateOnboardingApprovalPolicies({
        customerAutoApproveEnabled: policies.customerAutoApproveEnabled,
        partnerAutoApproveEnabled: policies.partnerAutoApproveEnabled,
        avatarAutoApproveEnabled: policies.avatarAutoApproveEnabled,
      }));
      toast.success("Đã cập nhật chính sách tự động duyệt cho hồ sơ đăng ký mới.", "Cập nhật thành công");
      setOpen(false);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Không cập nhật được cấu hình tự động duyệt.", "Cập nhật thất bại");
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      <Button variant="secondary" onClick={() => void openModal()}>
        <i className="fas fa-magic" />
        Tự động duyệt
      </Button>
      <Modal
        actions={(
          <div className="tw-flex tw-justify-end tw-gap-3">
            <Button disabled={saving} variant="secondary" onClick={() => setOpen(false)}>Hủy</Button>
            <Button disabled={loading} loading={saving} onClick={() => void savePolicies()}>Lưu cấu hình</Button>
          </div>
        )}
        description="Cấu hình áp dụng cho hồ sơ được tạo sau thời điểm bật. Hồ sơ đang chờ vẫn cần xét duyệt thủ công."
        onClose={saving ? () => undefined : () => setOpen(false)}
        open={open}
        title="Chính sách tự động duyệt"
      >
        {loading ? (
          <InfoBanner tone="info" title="Đang tải cấu hình" description="Vui lòng chờ trong giây lát." icon={<i className="fas fa-spinner fa-spin" />} />
        ) : (
          <div className="tw-grid tw-gap-3">
            <PolicySwitch
              checked={policies.customerAutoApproveEnabled}
              description="Tự động kích hoạt Customer sau khi hoàn tất hồ sơ và email đã được xác minh."
              disabled={saving || !canManageCustomerPolicy}
              label="Tự động duyệt khách hàng mới"
              onChange={(checked) => setPolicies((current) => ({ ...current, customerAutoApproveEnabled: checked }))}
            />
            <PolicySwitch
              checked={policies.avatarAutoApproveEnabled}
              description="Tự động duyệt ảnh đại diện mới sau kiểm tra định dạng. Luồng này độc lập với onboarding và trạng thái tài khoản."
              disabled={saving || !canManageAvatarPolicy}
              label="Tự động duyệt ảnh đại diện"
              onChange={(checked) => setPolicies((current) => ({ ...current, avatarAutoApproveEnabled: checked }))}
            />
            <PolicySwitch
              checked={policies.partnerAutoApproveEnabled}
              description="Tự động tạo đơn vị, membership và kích hoạt Partner sau khi email đã được xác minh."
              disabled={saving || !canManagePartnerPolicy}
              label="Tự động duyệt đối tác mới"
              onChange={(checked) => setPolicies((current) => ({ ...current, partnerAutoApproveEnabled: checked }))}
            />
            <InfoBanner
              tone="warning"
              title="Không bỏ qua bước xác minh email"
              description="Tài khoản đối tác chỉ được tự động duyệt sau khi Keycloak xác nhận emailVerified = true."
              icon={<i className="fas fa-shield-alt" />}
            />
          </div>
        )}
      </Modal>
    </>
  );
}
