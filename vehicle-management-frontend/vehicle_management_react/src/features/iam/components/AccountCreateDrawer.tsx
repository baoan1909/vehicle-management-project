import { useEffect, useMemo, useState } from "react";

import { Badge, Button, InfoBanner, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { cn } from "@/lib/cn";
import {
  createProvisionedAccount,
  type ProvisionedAccountResponse,
  type ProvisionedAccountRoleCode,
} from "@/features/iam/api/provisionedAccountApi";
import { hasAnyPermission } from "@/shared/auth/permissions";
import type { CurrentUser } from "@/shared/types/common";

type DrawerPhase = "opening" | "open" | "closing";

type AccountCreateDrawerProps = {
  isOpen: boolean;
  onClose: () => void;
  onCreated?: (account: ProvisionedAccountResponse) => void;
};

const DRAWER_ANIMATION_MS = 280;

const roleOptions: Array<{ code: ProvisionedAccountRoleCode; description: string; label: string }> = [
  { code: "SYSTEM_ADMIN", description: "Toàn quyền hệ thống", label: "Quản trị" },
  { code: "PARKING_MANAGER", description: "Quản lý vận hành bãi xe", label: "Quản lý" },
  { code: "EMPLOYEE", description: "Vận hành, thu ngân, hỗ trợ", label: "Nhân viên" },
  { code: "CUSTOMER", description: "Tài khoản cổng khách hàng", label: "Khách hàng" },
];

const initialForm = {
  email: "",
  fullName: "",
  username: "",
};

function getManageableRoleOptions(currentRole: CurrentUser["role"]) {
  if (currentRole === "SYSTEM_ADMIN") {
    return roleOptions.filter((role) => role.code === "SYSTEM_ADMIN" || role.code === "PARKING_MANAGER");
  }

  if (currentRole === "PARKING_MANAGER") {
    return roleOptions.filter((role) => role.code === "EMPLOYEE" || role.code === "CUSTOMER");
  }

  return [];
}

function getDefaultRole(availableRoles: typeof roleOptions): ProvisionedAccountRoleCode {
  return availableRoles[0]?.code ?? "EMPLOYEE";
}

export function AccountCreateDrawer({ isOpen, onClose, onCreated }: AccountCreateDrawerProps) {
  const { user } = useAuth();
  const toast = useToast();
  const canCreateProvisionedAccount = hasAnyPermission(user, ["ACCOUNT_CREATE_ALL"]);
  const manageableRoleOptions = useMemo(() => getManageableRoleOptions(user?.role ?? "UNKNOWN"), [user?.role]);
  const [isRendered, setIsRendered] = useState(isOpen);
  const [phase, setPhase] = useState<DrawerPhase>(isOpen ? "open" : "closing");
  const [selectedRole, setSelectedRole] = useState<ProvisionedAccountRoleCode>("EMPLOYEE");
  const [form, setForm] = useState(initialForm);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (isOpen) {
      setIsRendered(true);
      setPhase("opening");

      const openTimer = window.setTimeout(() => setPhase("open"), DRAWER_ANIMATION_MS);
      return () => window.clearTimeout(openTimer);
    }

    if (!isRendered) return undefined;

    setPhase("closing");
    const closeTimer = window.setTimeout(() => setIsRendered(false), DRAWER_ANIMATION_MS);
    return () => window.clearTimeout(closeTimer);
  }, [isOpen, isRendered]);

  useEffect(() => {
    if (!isRendered) return undefined;

    const previousBodyOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousBodyOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isRendered, onClose]);

  useEffect(() => {
    if (!isOpen) return;
    if (manageableRoleOptions.some((role) => role.code === selectedRole)) return;
    setSelectedRole(getDefaultRole(manageableRoleOptions));
  }, [isOpen, manageableRoleOptions, selectedRole]);

  if (!isRendered) return null;

  function updateField(field: keyof typeof initialForm, value: string) {
    setForm((currentValue) => ({ ...currentValue, [field]: value }));
  }

  async function handleSubmit() {
    if (!canCreateProvisionedAccount || manageableRoleOptions.length === 0) return;

    setErrorMessage("");
    setIsSubmitting(true);

    try {
      const response = await createProvisionedAccount({
        email: form.email,
        fullName: form.fullName,
        roleCode: selectedRole,
        username: form.username,
      });
      onCreated?.(response.data);
      toast.success(response.message || "Đã tạo tài khoản.", "Tạo tài khoản thành công");
      setForm(initialForm);
      setSelectedRole(getDefaultRole(manageableRoleOptions));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Không thể tạo tài khoản cấp sẵn.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="tw-fixed tw-inset-0 tw-z-[2200] tw-isolate tw-flex tw-justify-end" role="dialog" aria-modal="true" aria-labelledby="account-create-drawer-title">
      <button
        type="button"
        aria-label="Đóng drawer tạo tài khoản"
        className={cn(
          "tw-absolute tw-inset-0 tw-border-0 tw-bg-slate-900/30 tw-p-0 tw-backdrop-blur-[3px] tw-will-change-opacity",
          phase === "opening" ? "tw-animate-vm-drawer-backdrop-in" : "",
          phase === "closing" ? "tw-animate-vm-drawer-backdrop-out" : "",
        )}
        onClick={onClose}
      />

      <aside
        className={cn(
          "tw-relative tw-z-[1] tw-flex tw-h-full tw-w-[min(100%,460px)] tw-transform-gpu tw-flex-col tw-bg-white tw-shadow-vm-drawer tw-will-change-transform [backface-visibility:hidden] max-[768px]:tw-w-full",
          phase === "opening" ? "tw-animate-vm-drawer-panel-in" : "",
          phase === "closing" ? "tw-animate-vm-drawer-panel-out" : "",
        )}
      >
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
          <div className="tw-min-w-0">
            <h2 id="account-create-drawer-title" className="tw-m-0 tw-text-[1.18rem] tw-font-extrabold tw-text-vm-slate-900">
              Tạo tài khoản
            </h2>
            <p className="tw-m-0 tw-mt-1.5 tw-text-[0.84rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-500">
              Tạo tài khoản và gán vai trò ban đầu theo phạm vi được phân quyền.
            </p>
          </div>
          <button
            className="tw-inline-flex tw-h-[36px] tw-w-[36px] tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-slate-600 tw-transition hover:tw-bg-vm-slate-100 hover:tw-text-vm-slate-900 focus-visible:tw-outline-none focus-visible:tw-shadow-vm-focus"
            type="button"
            aria-label="Đóng"
            onClick={onClose}
          >
            <i className="fas fa-times" />
          </button>
        </header>

        <div className="tw-grid tw-min-h-0 tw-flex-1 tw-gap-4 tw-overflow-y-auto tw-px-5 tw-py-4 tw-[scrollbar-width:none] tw-[-ms-overflow-style:none] [&::-webkit-scrollbar]:tw-hidden">
          <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            <h3 className="tw-m-0 tw-text-[0.96rem] tw-font-extrabold tw-text-vm-slate-900">Thông tin tài khoản</h3>
            <div className="tw-mt-4 tw-grid tw-gap-3">
              {[
                { field: "username", label: "Username", placeholder: "vd: binh.tran" },
                { field: "email", label: "Email", placeholder: "binh.tran@coparking.vn" },
                { field: "fullName", label: "Họ và tên", placeholder: "Trần Thị Bình" },
              ].map((field) => (
                <label className="tw-grid tw-gap-1.5" key={field.label}>
                  <span className="tw-text-[0.78rem] tw-font-extrabold tw-text-vm-slate-700">{field.label}</span>
                  <input
                    className="tw-h-10 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-500 focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_4px_rgba(37,99,235,0.08)]"
                    placeholder={field.placeholder}
                    value={form[field.field as keyof typeof initialForm]}
                    onChange={(event) => updateField(field.field as keyof typeof initialForm, event.target.value)}
                  />
                </label>
              ))}
            </div>
          </section>

          <section className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-4">
            <h3 className="tw-m-0 tw-text-[0.96rem] tw-font-extrabold tw-text-vm-slate-900">Vai trò</h3>
            <div className="tw-mt-4 tw-grid tw-gap-2.5">
              {manageableRoleOptions.length === 0 ? (
                <div className="tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">
                  {canCreateProvisionedAccount
                    ? "Backend provisioned account hien chi ho tro scope role he thong; custom role can API scope rieng o phase sau."
                    : "Tai khoan hien tai chua co ACCOUNT_CREATE_ALL de tao tai khoan cap san."}
                </div>
              ) : null}
              {manageableRoleOptions.map((role) => {
                const selected = selectedRole === role.code;

                return (
                  <button
                    className={cn(
                      "tw-flex tw-items-start tw-gap-3 tw-rounded-vm-lg tw-border tw-border-solid tw-bg-white tw-p-3 tw-text-left tw-transition",
                      selected ? "tw-border-vm-primary tw-bg-brand-50 tw-shadow-[inset_3px_0_0_#2563EB]" : "tw-border-vm-slate-100 hover:tw-border-brand-100 hover:tw-bg-vm-slate-25",
                    )}
                    key={role.code}
                    type="button"
                    onClick={() => setSelectedRole(role.code)}
                  >
                    <span
                      className={cn(
                        "tw-mt-0.5 tw-inline-flex tw-h-5 tw-w-5 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-border-2 tw-border-solid",
                        selected ? "tw-border-vm-primary tw-bg-vm-primary tw-text-white" : "tw-border-vm-slate-200 tw-bg-white tw-text-transparent",
                      )}
                    >
                      <i className="fas fa-check tw-text-[0.62rem]" />
                    </span>
                    <span className="tw-min-w-0 tw-flex-1">
                      <span className="tw-flex tw-items-center tw-justify-between tw-gap-3">
                        <strong className="tw-text-[0.9rem] tw-font-extrabold tw-text-vm-slate-900">{role.label}</strong>
                        <Badge tone={selected ? "primary" : "neutral"} className="tw-rounded-full tw-px-2.5">{role.code}</Badge>
                      </span>
                      <small className="tw-mt-1 tw-block tw-text-[0.76rem] tw-font-semibold tw-text-vm-slate-500">{role.description}</small>
                    </span>
                  </button>
                );
              })}
            </div>
          </section>

          {errorMessage ? (
            <InfoBanner tone="warning" title="Không thể tạo tài khoản" description={errorMessage} icon={<i className="fas fa-exclamation-circle" />} />
          ) : null}
        </div>

        <footer className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-5 tw-py-4">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button variant="primary" onClick={handleSubmit} disabled={!canCreateProvisionedAccount || manageableRoleOptions.length === 0} loading={isSubmitting}>
            {!isSubmitting ? <i className="fas fa-plus" /> : null}
            {isSubmitting ? "Đang tạo..." : "Tạo tài khoản"}
          </Button>
        </footer>
      </aside>
    </div>
  );
}
