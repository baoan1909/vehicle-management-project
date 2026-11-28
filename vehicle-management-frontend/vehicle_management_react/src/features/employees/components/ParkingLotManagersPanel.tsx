import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Badge, Card } from "@/components/ui";
import { getParkingLotManagementHistory, type ParkingLotManagementAssignmentResponse } from "@/features/iam/api/organizationsApi";
import type { EmployeeApiResponse } from "@/features/employees/api/employeesApi";
import type { ParkingLotApiResponse } from "@/features/employees/api/shiftsApi";
import { getApplicationTimeZone } from "@/shared/time/applicationTime";

type DisplayStatus = ParkingLotManagementAssignmentResponse["status"] | "SCHEDULED";
const statusLabels: Record<DisplayStatus, string> = {
  ACTIVE: "Đang quản lý", SCHEDULED: "Sắp hiệu lực", PENDING_ACCEPTANCE: "Chờ tiếp nhận",
  ENDED: "Đã kết thúc", CANCELLED: "Đã hủy",
};
function effectiveStatus(item: ParkingLotManagementAssignmentResponse): DisplayStatus {
  if (item.status !== "ACTIVE" && item.status !== "PENDING_ACCEPTANCE") return item.status;
  const now = Date.now();
  if (item.effectiveTo && Date.parse(item.effectiveTo) <= now) return "ENDED";
  if (item.status === "ACTIVE" && Date.parse(item.effectiveFrom) > now) return "SCHEDULED";
  return item.status;
}
function formatTime(value: string | null) {
  return value ? new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short", timeStyle: "short", timeZone: getApplicationTimeZone(),
  }).format(new Date(value)) : "Không giới hạn";
}

export function ParkingLotManagersPanel({ parkingLots, employees, selectedLot, keyword, statusFilter, canManage, loading: parentLoading }: {
  parkingLots: ParkingLotApiResponse[];
  employees: EmployeeApiResponse[];
  selectedLot: string;
  keyword: string;
  statusFilter: string;
  canManage: boolean;
  loading: boolean;
}) {
  const [assignments, setAssignments] = useState<ParkingLotManagementAssignmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let current = true;
    setAssignments([]);
    setError("");
    setLoading(true);
    const lots = parkingLots.filter(lot => selectedLot === "all" || lot.parkingLotId === selectedLot);
    void Promise.all(lots.map(async lot => {
      const response = await getParkingLotManagementHistory(lot.organizationId, lot.parkingLotId);
      return (response.data ?? []).filter(item => item.parkingLotId === lot.parkingLotId && item.organizationId === lot.organizationId);
    })).then(results => {
      if (current) setAssignments(results.flat());
    }).catch(reason => {
      if (current) setError(reason instanceof Error ? reason.message : "Không thể tải phân công quản lý bãi.");
    }).finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [parkingLots, selectedLot, retry]);

  const managerMap = new Map(employees.filter(employee => employee.roleCode === "PARKING_MANAGER")
    .map(employee => [employee.accountId, employee]));
  const lotMap = new Map(parkingLots.map(lot => [lot.parkingLotId, lot]));
  const managerName = (id: string) => {
    const manager = managerMap.get(id);
    return manager?.userProfile?.fullName || manager?.accountUsername || `Quản lý (${id.slice(0, 8)})`;
  };
  const normalizedKeyword = keyword.trim().toLocaleLowerCase("vi");
  const visibleAssignments = assignments.filter(item => {
    if (selectedLot !== "all" && item.parkingLotId !== selectedLot) return false;
    const status = effectiveStatus(item);
    if (statusFilter === "CURRENT" && !["ACTIVE", "SCHEDULED", "PENDING_ACCEPTANCE"].includes(status)) return false;
    if (statusFilter !== "all" && statusFilter !== "CURRENT" && status !== statusFilter) return false;
    const lot = lotMap.get(item.parkingLotId);
    const manager = managerMap.get(item.managerAccountId);
    return !normalizedKeyword || [managerName(item.managerAccountId), manager?.employeeCode, manager?.accountEmail, lot?.code, lot?.name]
      .some(value => value?.toLocaleLowerCase("vi").includes(normalizedKeyword));
  });
  const managementLink = (lotId: string) => `/admin/parking-lots?lotId=${encodeURIComponent(lotId)}&action=manage-assignments`;
  const selectedParkingLot = lotMap.get(selectedLot);
  const linkClass = "tw-font-bold tw-text-vm-primary hover:tw-underline";

  return <Card className="tw-overflow-hidden tw-p-0">
    <div className="tw-flex tw-flex-wrap tw-items-center tw-justify-between tw-gap-3 tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-px-5 tw-py-4">
      <div>
        <h2 className="tw-m-0 tw-text-[1rem] tw-font-extrabold tw-text-vm-slate-900">Quản lý bãi</h2>
        <p className="tw-m-0 tw-mt-1 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">Phân công phụ trách bãi của Parking Manager, thời gian hiệu lực và lịch sử bàn giao.</p>
      </div>
      {canManage && selectedParkingLot && selectedParkingLot.status !== "CLOSED" ? <Link className={linkClass} to={managementLink(selectedLot)}>Quản lý phân công tại bãi này</Link> : null}
    </div>
    {parentLoading || loading ? <p className="tw-p-5 tw-text-sm">Đang tải phân công quản lý...</p>
      : error ? <div role="alert" className="tw-p-5 tw-text-sm tw-text-red-600">
        Không tải được phân công quản lý: {error}
        <button type="button" className="tw-ml-3 tw-font-bold tw-underline" onClick={() => setRetry(value => value + 1)}>Thử lại</button>
      </div>
      : visibleAssignments.length === 0 ? <p className="tw-p-5 tw-text-sm tw-text-vm-slate-500">Không có phân công quản lý phù hợp với bộ lọc.</p>
      : <div className="tw-overflow-x-auto"><table className="tw-w-full tw-text-left tw-text-sm">
        <thead className="tw-bg-vm-slate-25"><tr>{["Quản lý", "Bãi phụ trách", "Vai trò quản lý", "Hiệu lực", "Trạng thái", "Thao tác"].map(label => <th key={label} className="tw-px-5 tw-py-3">{label}</th>)}</tr></thead>
        <tbody>{visibleAssignments.map(item => {
          const lot = lotMap.get(item.parkingLotId);
          const status = effectiveStatus(item);
          return <tr key={item.assignmentId} className="tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100">
            <td className="tw-px-5 tw-py-4"><strong>{managerName(item.managerAccountId)}</strong><div className="tw-text-xs tw-text-vm-slate-500">{managerMap.get(item.managerAccountId)?.employeeCode}</div></td>
            <td className="tw-px-5 tw-py-4">{lot?.name}<div className="tw-text-xs tw-text-vm-slate-500">{lot?.code}</div></td>
            <td className="tw-px-5 tw-py-4">{item.managementRole === "PRIMARY" ? "Quản lý chính" : "Quản lý hỗ trợ"}</td>
            <td className="tw-px-5 tw-py-4"><div>Từ {formatTime(item.effectiveFrom)}</div><div className="tw-mt-1 tw-text-xs">Đến {formatTime(item.effectiveTo)}</div></td>
            <td className="tw-px-5 tw-py-4"><Badge tone={status === "ACTIVE" ? "success" : status === "ENDED" || status === "CANCELLED" ? "neutral" : "warning"}>{statusLabels[status]}</Badge></td>
            <td className="tw-px-5 tw-py-4">{canManage && lot && lot.status !== "CLOSED" ? <Link className={linkClass} to={managementLink(item.parkingLotId)}>Quản lý phân công</Link> : "—"}</td>
          </tr>;
        })}</tbody>
      </table></div>}
  </Card>;
}
