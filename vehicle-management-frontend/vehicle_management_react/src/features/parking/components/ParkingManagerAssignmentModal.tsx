import { useCallback, useEffect, useState } from "react";

import { Button, ConfirmDialog, Modal } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  assignParkingManagerToParkingLot,
  getAssignedParkingManagers,
  unassignParkingManagerFromParkingLot,
} from "@/features/iam/api/organizationsApi";
import { getProvisionedAccounts, type ProvisionedAccountResponse } from "@/features/iam/api/provisionedAccountApi";
import type { ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

export function ParkingManagerAssignmentModal({ lot, open, onClose, onChanged }: {
  lot: ParkingLotApiResponse | null;
  open: boolean;
  onClose: () => void;
  onChanged: (message: string) => void;
}) {
  const { invalidateAndRefreshAccess } = useAuth();
  const [managers, setManagers] = useState<ProvisionedAccountResponse[]>([]);
  const [managerId, setManagerId] = useState("");
  const [assignedManagerIds, setAssignedManagerIds] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [unassignTarget, setUnassignTarget] = useState<ProvisionedAccountResponse | null>(null);

  const loadManagers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (!lot) return;
      const [managerResponse, assignmentResponse] = await Promise.all([
        getProvisionedAccounts({ accountStatus: "ACTIVE", roleCode: "PARKING_MANAGER" }),
        getAssignedParkingManagers(lot.organizationId, lot.parkingLotId),
      ]);
      const list = managerResponse.data ?? [];
      const assignedIds = assignmentResponse.data?.parkingManagerAccountIds ?? [];
      const availableManagers = list.filter((item) => !assignedIds.includes(item.account.accountId));
      setManagers(list);
      setAssignedManagerIds(assignedIds);
      setManagerId((current) => availableManagers.some((item) => item.account.accountId === current)
        ? current
        : availableManagers[0]?.account.accountId ?? "");
    } catch (requestError) {
      setManagers([]);
      setAssignedManagerIds([]);
      setManagerId("");
      setError(requestError instanceof Error ? requestError.message : "Không thể tải danh sách Parking Manager.");
    } finally {
      setLoading(false);
    }
  }, [lot]);

  useEffect(() => {
    if (!open) return;
    setSaving(false);
    setError(null);
    setUnassignTarget(null);
    void loadManagers();
  }, [loadManagers, open, lot?.parkingLotId]);

  async function handleAssign() {
    if (!lot || !managerId || saving) return;
    const manager = managers.find((item) => item.account.accountId === managerId);
    if (!manager) return;
    setSaving(true);
    setError(null);
    try {
      await assignParkingManagerToParkingLot(lot.organizationId, lot.parkingLotId, managerId);
      await invalidateAndRefreshAccess("parking-lot-assignment-changed");
      onChanged(`Đã phân công ${manager.account.username} cho bãi ${lot.code}.`);
      await loadManagers();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể phân công Parking Manager.");
    } finally {
      setSaving(false);
    }
  }

  async function handleUnassign(manager: ProvisionedAccountResponse) {
    if (!lot || saving) return;
    setSaving(true);
    setError(null);
    try {
      await unassignParkingManagerFromParkingLot(
        lot.organizationId,
        lot.parkingLotId,
        manager.account.accountId,
      );
      await invalidateAndRefreshAccess("parking-lot-assignment-changed");
      setUnassignTarget(null);
      onChanged(`Đã gỡ ${manager.account.username} khỏi bãi ${lot.code}.`);
      await loadManagers();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể gỡ phân công Parking Manager.");
    } finally {
      setSaving(false);
    }
  }

  const availableManagers = managers.filter((manager) => !assignedManagerIds.includes(manager.account.accountId));
  const assignedManagers = managers.filter((manager) => assignedManagerIds.includes(manager.account.accountId));

  return (
    <Modal
      open={open && Boolean(lot)}
      title="Phân công Parking Manager"
      description={lot ? `Gán người quản lý cho ${lot.code} — ${lot.name}.` : undefined}
      onClose={() => { if (!saving) onClose(); }}
      actions={(
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={saving} onClick={onClose}>Hủy</Button>
          <Button loading={saving} disabled={!managerId || loading} onClick={() => void handleAssign()}>
            <i className="fas fa-user-check" aria-hidden="true" /> Phân công
          </Button>
        </div>
      )}
    >
      <div className="tw-grid tw-gap-3.5">
        {error ? (
          <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-red-700" role="alert">
            <span>{error}</span>
            <Button size="sm" variant="secondary" disabled={loading || saving} onClick={() => void loadManagers()}>Thử lại</Button>
          </div>
        ) : null}
        {loading ? (
          <p className="tw-m-0 tw-text-sm tw-font-semibold tw-text-slate-500"><i className="fas fa-spinner fa-spin tw-mr-2" />Đang tải danh sách Manager…</p>
        ) : (
          <SelectMenu
            ariaLabel="Parking Manager"
            value={managerId}
            onChange={setManagerId}
            clearValue=""
            options={[{ label: "Chọn Parking Manager chưa được phân công", value: "" }, ...availableManagers.map((manager) => ({
              label: `${manager.account.username} · ${manager.account.email}`,
              value: manager.account.accountId,
            }))]}
            portal
          />
        )}
        {!loading && !error && managers.length === 0 ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-bg-amber-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-amber-800">
            Chưa có Parking Manager đang hoạt động. Hãy tạo hoặc duyệt tài khoản Manager trước khi phân công.
          </p>
        ) : null}
        {!loading && managers.length > 0 && availableManagers.length === 0 ? (
          <p className="tw-m-0 tw-rounded-[8px] tw-bg-blue-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-blue-800">
            Tất cả Parking Manager khả dụng đã được phân công vào bãi này.
          </p>
        ) : null}
        {!loading && assignedManagers.length > 0 ? (
          <div className="tw-grid tw-gap-2">
            <strong className="tw-text-[0.82rem] tw-text-slate-700">Đang phụ trách bãi này</strong>
            {assignedManagers.map((manager) => (
              <div key={manager.account.accountId} className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-[8px] tw-border tw-border-solid tw-border-slate-200 tw-px-3 tw-py-2">
                <span className="tw-min-w-0 tw-text-[0.8rem] tw-font-semibold tw-text-slate-700">
                  <strong className="tw-block tw-truncate tw-text-slate-900">{manager.account.username}</strong>
                  <span className="tw-block tw-truncate">{manager.account.email}</span>
                </span>
                <Button size="sm" variant="danger" disabled={saving} onClick={() => setUnassignTarget(manager)}>
                  <i className="fas fa-user-minus" aria-hidden="true" /> Gỡ khỏi bãi
                </Button>
              </div>
            ))}
          </div>
        ) : null}
        <p className="tw-m-0 tw-text-[0.8rem] tw-font-medium tw-text-slate-500">
          Partner Admin mặc định trực tiếp phụ trách toàn bộ bãi thuộc đối tác. Phân công tại đây chỉ dành cho tài khoản Parking Manager chuyên trách và không làm thay đổi role của Partner Admin.
        </p>
      </div>
      <ConfirmDialog
        open={Boolean(unassignTarget)}
        title="Gỡ phân công bãi xe"
        description="Thao tác này chỉ gỡ quyền truy cập vào bãi hiện tại, không khóa tài khoản Manager."
        message={unassignTarget && lot
          ? `Gỡ ${unassignTarget.account.username} khỏi bãi ${lot.code}? Manager vẫn có thể truy cập các bãi khác đã được phân công.`
          : "Xác nhận gỡ phân công."}
        cancelLabel="Giữ phân công"
        confirmLabel="Gỡ khỏi bãi"
        tone="danger"
        loading={saving}
        onCancel={() => { if (!saving) setUnassignTarget(null); }}
        onConfirm={() => { if (unassignTarget) void handleUnassign(unassignTarget); }}
      />
    </Modal>
  );
}
