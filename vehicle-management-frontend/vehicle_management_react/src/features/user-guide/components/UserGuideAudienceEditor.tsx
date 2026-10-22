import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { SelectMenu } from "@/components/ui/SelectMenu";
import type {
  UserGuideAudienceMode,
  UserGuideAudiencePayload,
  UserGuideAudienceTargetType,
} from "@/features/user-guide/api/userGuideApi";

const modeOptions = [
  { label: "Công khai (PUBLIC)", value: "PUBLIC" },
  { label: "Mọi tài khoản đăng nhập", value: "ALL_AUTHENTICATED" },
  { label: "Theo vai trò (ROLE)", value: "ROLE" },
  { label: "Có ít nhất một quyền", value: "PERMISSION_ANY" },
  { label: "Có mọi quyền", value: "PERMISSION_ALL" },
];

const targetOptions = [
  { label: "Vai trò", value: "ROLE" },
  { label: "Quyền", value: "PERMISSION" },
];

const fieldClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25";

type UserGuideAudienceEditorProps = {
  audiences: UserGuideAudiencePayload[];
  mode: UserGuideAudienceMode;
  onChange: (mode: UserGuideAudienceMode, audiences: UserGuideAudiencePayload[]) => void;
  readOnly?: boolean;
};

export function UserGuideAudienceEditor({ audiences, mode, onChange, readOnly = false }: UserGuideAudienceEditorProps) {
  const needsTargets = mode === "ROLE" || mode === "PERMISSION_ANY" || mode === "PERMISSION_ALL";

  const updateRow = (index: number, patch: Partial<UserGuideAudiencePayload>) => {
    onChange(mode, audiences.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)));
  };

  const addRow = () => {
    const defaultTarget: UserGuideAudienceTargetType = mode === "ROLE" ? "ROLE" : "PERMISSION";
    onChange(mode, [...audiences, { targetType: defaultTarget }]);
  };

  const removeRow = (index: number) => {
    onChange(mode, audiences.filter((_, rowIndex) => rowIndex !== index));
  };

  return (
    <div className="tw-flex tw-flex-col tw-gap-3">
      <div>
        <p className="tw-m-0 tw-mb-1 tw-text-[0.82rem] tw-font-bold tw-text-vm-slate-700">Đối tượng hiển thị</p>
        <SelectMenu
          ariaLabel="Chế độ đối tượng"
          disabled={readOnly}
          onChange={(value) => onChange(value as UserGuideAudienceMode, needsTargets ? audiences : [])}
          options={modeOptions}
          value={mode}
        />
      </div>

      {needsTargets ? (
        <div className="tw-flex tw-flex-col tw-gap-2">
          {audiences.length === 0 ? (
            <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-3 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">
              Chế độ này yêu cầu ít nhất một dòng đối tượng (roleId cho ROLE, permissionId cho PERMISSION).
            </p>
          ) : null}
          {audiences.map((row, index) => (
            <div key={`audience-${index}`} className="tw-grid tw-grid-cols-[130px_1fr_auto] tw-items-center tw-gap-2">
              <SelectMenu
                ariaLabel={`Loại đối tượng ${index + 1}`}
                disabled={readOnly}
                onChange={(value) => updateRow(index, {
                  permissionId: null,
                  roleId: null,
                  targetType: value as UserGuideAudienceTargetType,
                })}
                options={targetOptions}
                value={row.targetType}
              />
              {row.targetType === "ROLE" ? (
                <Input
                  aria-label={`Role ID ${index + 1}`}
                  className={fieldClassName}
                  disabled={readOnly}
                  onChange={(event) => updateRow(index, { roleId: event.target.value || null })}
                  placeholder="Role ID (UUID)"
                  value={row.roleId ?? ""}
                />
              ) : (
                <Input
                  aria-label={`Permission ID ${index + 1}`}
                  className={fieldClassName}
                  disabled={readOnly}
                  onChange={(event) => updateRow(index, { permissionId: event.target.value || null })}
                  placeholder="Permission ID (UUID)"
                  value={row.permissionId ?? ""}
                />
              )}
              {!readOnly ? (
                <Button aria-label={`Xóa đối tượng ${index + 1}`} onClick={() => removeRow(index)} size="sm" variant="ghost">
                  <i className="fas fa-trash" aria-hidden="true" />
                </Button>
              ) : null}
            </div>
          ))}
          {!readOnly ? (
            <Button onClick={addRow} size="sm" variant="secondary">
              <i className="fas fa-plus" aria-hidden="true" /> Thêm đối tượng
            </Button>
          ) : null}
        </div>
      ) : (
        <p className="tw-m-0 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">
          Chế độ này không cần cấu hình dòng đối tượng.
        </p>
      )}
    </div>
  );
}
