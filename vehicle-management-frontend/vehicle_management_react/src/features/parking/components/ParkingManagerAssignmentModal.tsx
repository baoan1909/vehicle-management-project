import { useCallback, useEffect, useState } from "react";

import { Button, Modal } from "@/components/ui";
import { assignParkingManagerToParkingLot } from "@/features/iam/api/organizationsApi";
import { getProvisionedAccounts, type ProvisionedAccountResponse } from "@/features/iam/api/provisionedAccountApi";
import type { ParkingLotApiResponse } from "@/features/parking/api/parkingLotsApi";
import { SelectMenu } from "@/shared/components/ui/SelectMenu";

export function ParkingManagerAssignmentModal({ lot, open, onClose, onCompleted }: {
  lot: ParkingLotApiResponse | null;
  open: boolean;
  onClose: () => void;
  onCompleted: (manager: ProvisionedAccountResponse) => void;
}) {
  const [managers, setManagers] = useState<ProvisionedAccountResponse[]>([]);
  const [managerId, setManagerId] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadManagers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await getProvisionedAccounts({ accountStatus: "ACTIVE", roleCode: "PARKING_MANAGER" });
      const list = response.data ?? [];
      setManagers(list);
      setManagerId((current) => list.some((item) => item.account.accountId === current) ? current : list[0]?.account.accountId ?? "");
    } catch (requestError) {
      setManagers([]);
      setManagerId("");
      setError(requestError instanceof Error ? requestError.message : "Không thể tải danh sách Parking Manager.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!open) return;
    setSaving(false);
    setError(null);
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
      onCompleted(manager);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Không thể phân công Parking Manager.");
    } finally {
      setSaving(false);
    }
  }

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
            options={[{ label: "Chọn Parking Manager", value: "" }, ...managers.map((manager) => ({
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
        <p className="tw-m-0 tw-text-[0.8rem] tw-font-medium tw-text-slate-500">
          Manager chỉ đọc và cấu hình các bãi đã được phân công theo phạm vi tổ chức.
        </p>
      </div>
    </Modal>
  );
}
