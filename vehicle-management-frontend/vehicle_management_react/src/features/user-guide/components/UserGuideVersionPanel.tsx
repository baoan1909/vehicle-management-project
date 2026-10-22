import { useEffect, useMemo, useState } from "react";

import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { SelectMenu } from "@/components/ui/SelectMenu";
import { UserGuideAudienceEditor } from "@/features/user-guide/components/UserGuideAudienceEditor";
import { UserGuideStepBuilder } from "@/features/user-guide/components/UserGuideStepBuilder";
import type {
  UpdateUserGuideVersionRequest,
  UserGuideAudienceMode,
  UserGuideAudiencePayload,
  UserGuideStepPayload,
  UserGuideVersionAdminResponse,
} from "@/features/user-guide/api/userGuideApi";

const fieldClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25";

const modeOptions = [
  { label: "Công khai (PUBLIC)", value: "PUBLIC" },
  { label: "Mọi tài khoản đăng nhập", value: "ALL_AUTHENTICATED" },
  { label: "Theo vai trò (ROLE)", value: "ROLE" },
  { label: "Có ít nhất một quyền", value: "PERMISSION_ANY" },
  { label: "Có mọi quyền", value: "PERMISSION_ALL" },
];

type UserGuideVersionPanelProps = {
  canPublish: boolean;
  canUpdate: boolean;
  currentVersion: number;
  lockVersion: number;
  onClone: (versionNumber: number) => void;
  onPublish: (versionNumber: number) => void;
  onSaveDraft: (versionNumber: number, payload: UpdateUserGuideVersionRequest) => void;
  saving: boolean;
  versions: UserGuideVersionAdminResponse[];
};

function toStepPayloads(version: UserGuideVersionAdminResponse): UserGuideStepPayload[] {
  return [...version.steps]
    .sort((left, right) => left.displayOrder - right.displayOrder)
    .map((step) => ({
      anchorKey: step.anchorKey,
      body: step.body,
      collapseMs: step.collapseMs,
      displayOrder: step.displayOrder,
      mediaObjectKey: step.mediaObjectKey,
      placement: step.placement,
      title: step.title,
    }));
}

function toAudiencePayloads(version: UserGuideVersionAdminResponse): UserGuideAudiencePayload[] {
  return version.audiences.map((audience) => ({
    permissionId: audience.permissionId,
    roleId: audience.roleId,
    targetType: audience.targetType,
  }));
}

export function UserGuideVersionPanel({
  canPublish,
  canUpdate,
  currentVersion,
  lockVersion,
  onClone,
  onPublish,
  onSaveDraft,
  saving,
  versions,
}: UserGuideVersionPanelProps) {
  const sorted = useMemo(
    () => [...versions].sort((left, right) => right.versionNumber - left.versionNumber),
    [versions],
  );
  const [selected, setSelected] = useState<number>(sorted[0]?.versionNumber ?? 0);
  const active = sorted.find((version) => version.versionNumber === selected) ?? sorted[0] ?? null;
  const isDraft = active ? active.versionNumber > currentVersion : false;

  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [deepLink, setDeepLink] = useState("");
  const [mode, setMode] = useState<UserGuideAudienceMode>("ALL_AUTHENTICATED");
  const [steps, setSteps] = useState<UserGuideStepPayload[]>([]);
  const [audiences, setAudiences] = useState<UserGuideAudiencePayload[]>([]);

  useEffect(() => {
    if (!active) return;
    setSelected(active.versionNumber);
    setTitle(active.title);
    setSummary(active.summary ?? "");
    setDeepLink(active.deepLink ?? "");
    setMode(active.audienceMode);
    setSteps(toStepPayloads(active));
    setAudiences(toAudiencePayloads(active));
  }, [active?.guideVersionId]);

  if (!active) {
    return (
      <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
        Chưa có phiên bản nào.
      </p>
    );
  }

  const publishedBadge = (versionNumber: number) => {
    if (versionNumber === currentVersion && currentVersion > 0) {
      return <Badge tone="success">Đang phát hành</Badge>;
    }
    if (versionNumber > currentVersion) {
      return <Badge tone="warning">Nháp</Badge>;
    }
    return <Badge tone="neutral">Bản cũ</Badge>;
  };

  return (
    <div className="tw-flex tw-flex-col tw-gap-3">
      <div className="tw-flex tw-flex-wrap tw-gap-2">
        {sorted.map((version) => (
          <Button
            key={version.guideVersionId}
            onClick={() => setSelected(version.versionNumber)}
            size="sm"
            variant={version.versionNumber === (active?.versionNumber ?? -1) ? "primary" : "secondary"}
          >
            v{version.versionNumber}
          </Button>
        ))}
      </div>

      <div className="tw-flex tw-items-center tw-justify-between">
        <div className="tw-flex tw-items-center tw-gap-2">
          <strong className="tw-text-[0.95rem] tw-text-vm-slate-900">Phiên bản {active.versionNumber}</strong>
          {publishedBadge(active.versionNumber)}
        </div>
        <div className="tw-flex tw-gap-2">
          {canUpdate ? (
            <Button onClick={() => onClone(active.versionNumber)} size="sm" variant="secondary">
              <i className="fas fa-copy" aria-hidden="true" /> Tạo nháp từ bản này
            </Button>
          ) : null}
          {canPublish && isDraft ? (
            <Button onClick={() => onPublish(active.versionNumber)} size="sm">
              <i className="fas fa-paper-plane" aria-hidden="true" /> Phát hành
            </Button>
          ) : null}
        </div>
      </div>

      {isDraft ? (
        <p className="tw-m-0 tw-rounded-vm-md tw-bg-amber-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-amber-700">
          Bản nháp có thể chỉnh sửa. Bản đã phát hành chỉ đọc — hãy tạo nháp mới để thay đổi.
        </p>
      ) : (
        <p className="tw-m-0 tw-rounded-vm-md tw-bg-vm-slate-25 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-vm-slate-500">
          Bản này chỉ đọc (đã phát hành hoặc bản cũ). Mọi chỉnh sửa phải thực hiện trên bản nháp mới.
        </p>
      )}

      <div className="tw-flex tw-flex-col tw-gap-2">
        <Input
          aria-label="Tiêu đề phiên bản"
          className={fieldClassName}
          disabled={!isDraft || !canUpdate}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Tiêu đề phiên bản"
          value={title}
        />
        <textarea
          aria-label="Tóm tắt phiên bản"
          className="tw-min-h-16 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-py-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25"
          disabled={!isDraft || !canUpdate}
          onChange={(event) => setSummary(event.target.value)}
          placeholder="Tóm tắt"
          value={summary}
        />
        <Input
          aria-label="Deep link"
          className={fieldClassName}
          disabled={!isDraft || !canUpdate}
          onChange={(event) => setDeepLink(event.target.value)}
          placeholder="Deep link nội bộ (vd: /admin/swipe)"
          value={deepLink}
        />
        {isDraft && canUpdate ? (
          <SelectMenu
            ariaLabel="Chế độ đối tượng"
            onChange={(value) => setMode(value as UserGuideAudienceMode)}
            options={modeOptions}
            value={mode}
          />
        ) : null}
      </div>

      {isDraft && canUpdate ? (
        <>
          <UserGuideStepBuilder onChange={setSteps} steps={steps} />
          <UserGuideAudienceEditor
            audiences={audiences}
            mode={mode}
            onChange={(nextMode, nextAudiences) => {
              setMode(nextMode);
              setAudiences(nextAudiences);
            }}
          />
          <div className="tw-flex tw-justify-end">
            <Button
              loading={saving}
              onClick={() => onSaveDraft(active.versionNumber, {
                audiences,
                deepLink: deepLink || null,
                lockVersion,
                steps,
                summary: summary || null,
                title,
              })}
            >
              Lưu bản nháp
            </Button>
          </div>
        </>
      ) : (
        <div className="tw-flex tw-flex-col tw-gap-2">
          <p className="tw-m-0 tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-700">
            {active.steps.length} bước · {active.audiences.length} đối tượng · Chế độ {active.audienceMode}
          </p>
          {active.steps.map((step) => (
            <div key={step.stepId} className="tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-px-3 tw-py-2">
              <strong className="tw-text-[0.85rem] tw-text-vm-slate-900">
                #{step.displayOrder + 1} {step.title}
              </strong>
              <p className="tw-m-0 tw-text-[0.82rem] tw-text-vm-slate-600">{step.body}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
