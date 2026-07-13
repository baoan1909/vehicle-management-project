import { useMemo, useState } from "react";
import type { FormEvent, InputHTMLAttributes, ReactNode } from "react";

import { Badge, Button, Card, SelectMenu, useToast } from "@/components/ui";
import type { EmployeeApiResponse } from "@/features/employees/api/employeesApi";
import {
  activateEmployeeRosterRule,
  activateShiftTemplate,
  createEmployeeRosterRule,
  createShiftTemplate,
  deleteEmployeeRosterRule,
  deleteShiftTemplate,
  updateEmployeeRosterRule,
  updateShiftTemplate,
  type AssignmentModeApi,
  type EmployeeRosterRuleApiResponse,
  type GateApiResponse,
  type ParkingLotApiResponse,
  type ShiftTemplateApiResponse,
  type ShiftTypeApi,
  type WeekdayApi,
} from "@/features/employees/api/shiftsApi";
import { cn } from "@/lib/cn";

type ConfigurationMode = "templates" | "rules";

type ShiftConfigurationPanelProps = {
  employees: EmployeeApiResponse[];
  gates: GateApiResponse[];
  mode: ConfigurationMode;
  onChanged: () => Promise<void> | void;
  parkingLots: ParkingLotApiResponse[];
  rosterRules: EmployeeRosterRuleApiResponse[];
  selectedLot: string;
  shiftTemplates: ShiftTemplateApiResponse[];
};

type TemplateFormState = {
  endLocalTime: string;
  name: string;
  parkingLotId: string;
  shiftType: ShiftTypeApi;
  startLocalTime: string;
};

type RosterRuleFormState = {
  assignmentMode: AssignmentModeApi;
  effectiveFrom: string;
  effectiveTo: string;
  employeeId: string;
  parkingLotId: string;
  preferredGateId: string;
  preferredShiftType: ShiftTypeApi;
  weeklyDayOff: WeekdayApi;
};

const shiftTypeOptions: Array<{ label: string; value: ShiftTypeApi }> = [
  { label: "Ca sáng", value: "MORNING" },
  { label: "Ca chiều", value: "AFTERNOON" },
  { label: "Ca đêm", value: "NIGHT" },
];

const assignmentModeOptions: Array<{ label: string; value: AssignmentModeApi }> = [
  { label: "Cố định", value: "FIXED" },
  { label: "Dự phòng", value: "RELIEF" },
];

const weekdayOptions: Array<{ label: string; value: WeekdayApi }> = [
  { label: "Thứ hai", value: "MONDAY" },
  { label: "Thứ ba", value: "TUESDAY" },
  { label: "Thứ tư", value: "WEDNESDAY" },
  { label: "Thứ năm", value: "THURSDAY" },
  { label: "Thứ sáu", value: "FRIDAY" },
  { label: "Thứ bảy", value: "SATURDAY" },
  { label: "Chủ nhật", value: "SUNDAY" },
];

const shiftTypeLabels: Record<ShiftTypeApi, string> = {
  AFTERNOON: "Ca chiều",
  MORNING: "Ca sáng",
  NIGHT: "Ca đêm",
};

const assignmentModeLabels: Record<AssignmentModeApi, string> = {
  FIXED: "Cố định",
  RELIEF: "Dự phòng",
};

const weekdayLabels: Record<WeekdayApi, string> = {
  FRIDAY: "Thứ sáu",
  MONDAY: "Thứ hai",
  SATURDAY: "Thứ bảy",
  SUNDAY: "Chủ nhật",
  THURSDAY: "Thứ năm",
  TUESDAY: "Thứ ba",
  WEDNESDAY: "Thứ tư",
};

function todayIsoDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

function getEmployeeName(employee?: EmployeeApiResponse) {
  return employee?.userProfile?.fullName ?? employee?.employeeCode ?? "Chưa rõ nhân viên";
}

function getLotName(lots: ParkingLotApiResponse[], lotId?: string | null) {
  const lot = lots.find((item) => item.parkingLotId === lotId);
  return lot ? `${lot.code} - ${lot.name}` : shortId("Bãi", lotId);
}

function getGateName(gates: GateApiResponse[], gateId?: string | null) {
  const gate = gates.find((item) => item.gateId === gateId);
  return gate ? `${gate.code} - ${gate.name}` : shortId("Cổng", gateId);
}

function shortId(prefix: string, id?: string | null) {
  return id ? `${prefix}-${id.slice(0, 8).toUpperCase()}` : "Chưa có";
}

function toTemplateForm(template: ShiftTemplateApiResponse | null, selectedLot: string): TemplateFormState {
  return {
    endLocalTime: template?.endLocalTime?.slice(0, 5) ?? "14:00",
    name: template?.name ?? "",
    parkingLotId: template?.parkingLotId ?? (selectedLot === "all" ? "" : selectedLot),
    shiftType: template?.shiftType ?? "MORNING",
    startLocalTime: template?.startLocalTime?.slice(0, 5) ?? "06:00",
  };
}

function toRosterRuleForm(rule: EmployeeRosterRuleApiResponse | null, selectedLot: string): RosterRuleFormState {
  return {
    assignmentMode: rule?.assignmentMode ?? "FIXED",
    effectiveFrom: rule?.effectiveFrom ?? todayIsoDate(),
    effectiveTo: rule?.effectiveTo ?? "",
    employeeId: rule?.employeeId ?? "",
    parkingLotId: rule?.parkingLotId ?? (selectedLot === "all" ? "" : selectedLot),
    preferredGateId: rule?.preferredGateId ?? "",
    preferredShiftType: rule?.preferredShiftType ?? "MORNING",
    weeklyDayOff: rule?.weeklyDayOff ?? "SUNDAY",
  };
}

function Field({ children, label }: { children: ReactNode; label: string }) {
  return (
    <label className="tw-m-0 tw-grid tw-gap-2">
      <span className="tw-text-[0.76rem] tw-font-extrabold tw-text-vm-slate-600">{label}</span>
      {children}
    </label>
  );
}

function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={cn(
        "tw-h-[42px] tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)]",
        props.className,
      )}
    />
  );
}

export function ShiftConfigurationPanel({
  employees,
  gates,
  mode,
  onChanged,
  parkingLots,
  rosterRules,
  selectedLot,
  shiftTemplates,
}: ShiftConfigurationPanelProps) {
  const toast = useToast();
  const lotOptions = useMemo(
    () => parkingLots.map((lot) => ({ label: `${lot.code} - ${lot.name}`, value: lot.parkingLotId })),
    [parkingLots],
  );
  const lotSelectOptions = useMemo(
    () => [{ label: "Chọn bãi xe", value: "" }, ...lotOptions],
    [lotOptions],
  );
  const employeeOptions = useMemo(
    () => employees.map((employee) => ({ label: getEmployeeName(employee), value: employee.employeeId })),
    [employees],
  );
  const employeeSelectOptions = useMemo(
    () => [{ label: "Chọn nhân viên", value: "" }, ...employeeOptions],
    [employeeOptions],
  );
  const gateOptions = useMemo(
    () => gates.map((gate) => ({ label: `${gate.code} - ${gate.name}`, value: gate.gateId })),
    [gates],
  );
  const gateSelectOptions = useMemo(
    () => [{ label: "Không chọn cổng", value: "" }, ...gateOptions],
    [gateOptions],
  );

  const [templateForm, setTemplateForm] = useState(() => toTemplateForm(null, selectedLot));
  const [editingTemplateId, setEditingTemplateId] = useState<string | null>(null);
  const [ruleForm, setRuleForm] = useState(() => toRosterRuleForm(null, selectedLot));
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  function resetTemplateForm() {
    setEditingTemplateId(null);
    setTemplateForm(toTemplateForm(null, selectedLot));
    setFormError("");
  }

  function resetRuleForm() {
    setEditingRuleId(null);
    setRuleForm(toRosterRuleForm(null, selectedLot));
    setFormError("");
  }

  async function handleSubmitTemplate(event: FormEvent) {
    event.preventDefault();
    setFormError("");

    if (!templateForm.parkingLotId || !templateForm.name.trim() || !templateForm.startLocalTime || !templateForm.endLocalTime) {
      setFormError("Vui lòng nhập đủ bãi xe, tên mẫu ca và khung giờ.");
      return;
    }

    setSaving(true);
    try {
      if (editingTemplateId) {
        await updateShiftTemplate(editingTemplateId, {
          endLocalTime: templateForm.endLocalTime,
          name: templateForm.name.trim(),
          startLocalTime: templateForm.startLocalTime,
        });
        toast.success("Đã cập nhật mẫu ca.");
      } else {
        await createShiftTemplate({
          endLocalTime: templateForm.endLocalTime,
          name: templateForm.name.trim(),
          parkingLotId: templateForm.parkingLotId,
          shiftType: templateForm.shiftType,
          startLocalTime: templateForm.startLocalTime,
        });
        toast.success("Đã tạo mẫu ca.");
      }
      resetTemplateForm();
      await onChanged();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể lưu mẫu ca.");
    } finally {
      setSaving(false);
    }
  }

  async function handleSubmitRule(event: FormEvent) {
    event.preventDefault();
    setFormError("");

    if (!ruleForm.parkingLotId || !ruleForm.employeeId || !ruleForm.effectiveFrom) {
      setFormError("Vui lòng chọn bãi xe, nhân viên và ngày hiệu lực.");
      return;
    }

    setSaving(true);
    try {
      const payload = {
        assignmentMode: ruleForm.assignmentMode,
        effectiveFrom: ruleForm.effectiveFrom,
        effectiveTo: ruleForm.effectiveTo || null,
        employeeId: ruleForm.employeeId,
        parkingLotId: ruleForm.parkingLotId,
        preferredGateId: ruleForm.preferredGateId || null,
        preferredShiftType: ruleForm.preferredShiftType,
        weeklyDayOff: ruleForm.weeklyDayOff,
      };

      if (editingRuleId) {
        await updateEmployeeRosterRule(editingRuleId, payload);
        toast.success("Đã cập nhật quy tắc phân công.");
      } else {
        await createEmployeeRosterRule(payload);
        toast.success("Đã tạo quy tắc phân công.");
      }
      resetRuleForm();
      await onChanged();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể lưu quy tắc phân công.");
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleTemplate(template: ShiftTemplateApiResponse) {
    setSaving(true);
    setFormError("");
    try {
      if (template.status === "ACTIVE") {
        await deleteShiftTemplate(template.shiftTemplateId);
        toast.success("Đã vô hiệu hóa mẫu ca.");
      } else {
        await activateShiftTemplate(template.shiftTemplateId);
        toast.success("Đã kích hoạt mẫu ca.");
      }
      await onChanged();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể đổi trạng thái mẫu ca.");
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleRule(rule: EmployeeRosterRuleApiResponse) {
    setSaving(true);
    setFormError("");
    try {
      if (rule.status === "ACTIVE") {
        await deleteEmployeeRosterRule(rule.rosterRuleId);
        toast.success("Đã vô hiệu hóa quy tắc.");
      } else {
        await activateEmployeeRosterRule(rule.rosterRuleId);
        toast.success("Đã kích hoạt quy tắc.");
      }
      await onChanged();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Không thể đổi trạng thái quy tắc.");
    } finally {
      setSaving(false);
    }
  }

  if (mode === "templates") {
    return (
      <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_420px] tw-gap-4 max-[1180px]:tw-grid-cols-1">
        <Card className="tw-order-2 tw-p-4">
          <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
            <div>
              <h2 className="tw-m-0 tw-text-[1.08rem] tw-font-extrabold tw-text-vm-slate-900">{editingTemplateId ? "Sửa mẫu ca" : "Tạo mẫu ca"}</h2>
              <p className="tw-m-0 tw-mt-1 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">Mẫu ca quy định bãi xe có ca nào và khung giờ nào.</p>
            </div>
            {editingTemplateId ? <Button size="sm" variant="ghost" onClick={resetTemplateForm}>Bỏ chọn</Button> : null}
          </div>

          <form className="tw-mt-4 tw-grid tw-gap-3" onSubmit={(event) => void handleSubmitTemplate(event)}>
            <Field label="Bãi xe">
              <SelectMenu
                ariaLabel="Bãi xe mẫu ca"
                disabled={Boolean(editingTemplateId)}
                options={lotSelectOptions}
                value={templateForm.parkingLotId}
                onChange={(value) => setTemplateForm((current) => ({ ...current, parkingLotId: value }))}
              />
            </Field>
            <Field label="Loại ca">
              <SelectMenu
                ariaLabel="Loại ca mẫu ca"
                disabled={Boolean(editingTemplateId)}
                options={shiftTypeOptions}
                value={templateForm.shiftType}
                onChange={(value) => setTemplateForm((current) => ({ ...current, shiftType: value as ShiftTypeApi }))}
              />
            </Field>
            <Field label="Tên mẫu ca">
              <TextInput value={templateForm.name} onChange={(event) => setTemplateForm((current) => ({ ...current, name: event.target.value }))} />
            </Field>
            <div className="tw-grid tw-grid-cols-2 tw-gap-3">
              <Field label="Bắt đầu">
                <TextInput type="time" value={templateForm.startLocalTime} onChange={(event) => setTemplateForm((current) => ({ ...current, startLocalTime: event.target.value }))} />
              </Field>
              <Field label="Kết thúc">
                <TextInput type="time" value={templateForm.endLocalTime} onChange={(event) => setTemplateForm((current) => ({ ...current, endLocalTime: event.target.value }))} />
              </Field>
            </div>
            {formError ? <div className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{formError}</div> : null}
            <Button disabled={saving} type="submit">{editingTemplateId ? "Cập nhật mẫu ca" : "Tạo mẫu ca"}</Button>
          </form>
        </Card>

        <Card className="tw-order-1 tw-overflow-hidden">
          <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
            <h2 className="tw-m-0 tw-text-[1.08rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách mẫu ca</h2>
          </div>
          <div className="tw-overflow-x-auto">
            <table className="table tw-m-0 tw-min-w-[760px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.78rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
              <thead>
                <tr>
                  <th>Bãi xe</th>
                  <th>Loại ca</th>
                  <th>Tên mẫu</th>
                  <th>Khung giờ</th>
                  <th>Trạng thái</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {shiftTemplates.length ? shiftTemplates.map((template) => (
                  <tr className="tw-cursor-pointer hover:tw-bg-brand-50/30" key={template.shiftTemplateId} onClick={() => {
                    setEditingTemplateId(template.shiftTemplateId);
                    setTemplateForm(toTemplateForm(template, selectedLot));
                    setFormError("");
                  }}>
                    <td className="tw-font-bold tw-text-vm-slate-900">{getLotName(parkingLots, template.parkingLotId)}</td>
                    <td>{shiftTypeLabels[template.shiftType]}</td>
                    <td>{template.name}</td>
                    <td>{template.startLocalTime?.slice(0, 5)} - {template.endLocalTime?.slice(0, 5)}</td>
                    <td><Badge tone={template.status === "ACTIVE" ? "success" : "neutral"}>{template.status === "ACTIVE" ? "Đang hoạt động" : "Ngưng"}</Badge></td>
                    <td>
                      <Button size="sm" variant={template.status === "ACTIVE" ? "danger" : "secondary"} disabled={saving} onClick={(event) => {
                        event.stopPropagation();
                        void handleToggleTemplate(template);
                      }}>
                        {template.status === "ACTIVE" ? "Vô hiệu hóa" : "Kích hoạt"}
                      </Button>
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan={6} className="tw-text-center tw-font-bold tw-text-vm-slate-500">Chưa có mẫu ca.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="tw-mt-4 tw-grid tw-grid-cols-[minmax(0,1fr)_460px] tw-gap-4 max-[1180px]:tw-grid-cols-1">
      <Card className="tw-order-2 tw-p-4">
        <div className="tw-flex tw-items-start tw-justify-between tw-gap-3">
          <div>
            <h2 className="tw-m-0 tw-text-[1.08rem] tw-font-extrabold tw-text-vm-slate-900">{editingRuleId ? "Sửa quy tắc" : "Tạo quy tắc"}</h2>
            <p className="tw-m-0 tw-mt-1 tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-500">Quy tắc này được dùng khi sinh lịch tuần tự động.</p>
          </div>
          {editingRuleId ? <Button size="sm" variant="ghost" onClick={resetRuleForm}>Bỏ chọn</Button> : null}
        </div>

        <form className="tw-mt-4 tw-grid tw-gap-3" onSubmit={(event) => void handleSubmitRule(event)}>
          <Field label="Bãi xe">
            <SelectMenu
              ariaLabel="Bãi xe quy tắc"
              options={lotSelectOptions}
              value={ruleForm.parkingLotId}
              onChange={(value) => setRuleForm((current) => ({ ...current, parkingLotId: value }))}
            />
          </Field>
          <Field label="Nhân viên">
            <SelectMenu
              ariaLabel="Nhân viên quy tắc"
              options={employeeSelectOptions}
              value={ruleForm.employeeId}
              onChange={(value) => setRuleForm((current) => ({ ...current, employeeId: value }))}
            />
          </Field>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <Field label="Loại ca ưu tiên">
              <SelectMenu ariaLabel="Loại ca ưu tiên" options={shiftTypeOptions} value={ruleForm.preferredShiftType} onChange={(value) => setRuleForm((current) => ({ ...current, preferredShiftType: value as ShiftTypeApi }))} />
            </Field>
            <Field label="Chế độ">
              <SelectMenu ariaLabel="Chế độ phân công" options={assignmentModeOptions} value={ruleForm.assignmentMode} onChange={(value) => setRuleForm((current) => ({ ...current, assignmentMode: value as AssignmentModeApi }))} />
            </Field>
          </div>
          <Field label="Cổng ưu tiên">
            <SelectMenu
              ariaLabel="Cổng ưu tiên"
              clearValue=""
              options={gateSelectOptions}
              value={ruleForm.preferredGateId}
              onChange={(value) => setRuleForm((current) => ({ ...current, preferredGateId: value }))}
            />
          </Field>
          <Field label="Ngày nghỉ hàng tuần">
            <SelectMenu ariaLabel="Ngày nghỉ hàng tuần" options={weekdayOptions} value={ruleForm.weeklyDayOff} onChange={(value) => setRuleForm((current) => ({ ...current, weeklyDayOff: value as WeekdayApi }))} />
          </Field>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3">
            <Field label="Hiệu lực từ">
              <TextInput type="date" value={ruleForm.effectiveFrom} onChange={(event) => setRuleForm((current) => ({ ...current, effectiveFrom: event.target.value }))} />
            </Field>
            <Field label="Hiệu lực đến">
              <TextInput type="date" value={ruleForm.effectiveTo} onChange={(event) => setRuleForm((current) => ({ ...current, effectiveTo: event.target.value }))} />
            </Field>
          </div>
          {formError ? <div className="tw-rounded-vm-md tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">{formError}</div> : null}
          <Button disabled={saving} type="submit">{editingRuleId ? "Cập nhật quy tắc" : "Tạo quy tắc"}</Button>
        </form>
      </Card>

      <Card className="tw-order-1 tw-overflow-hidden">
        <div className="tw-border-0 tw-border-b tw-border-solid tw-border-vm-slate-100 tw-p-4">
          <h2 className="tw-m-0 tw-text-[1.08rem] tw-font-extrabold tw-text-vm-slate-900">Danh sách quy tắc</h2>
        </div>
        <div className="tw-overflow-x-auto">
          <table className="table tw-m-0 tw-min-w-[980px] [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.78rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
            <thead>
              <tr>
                <th>Nhân viên</th>
                <th>Bãi xe</th>
                <th>Loại ca</th>
                <th>Cổng</th>
                <th>Ngày nghỉ</th>
                <th>Chế độ</th>
                <th>Hiệu lực</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rosterRules.length ? rosterRules.map((rule) => (
                <tr className="tw-cursor-pointer hover:tw-bg-brand-50/30" key={rule.rosterRuleId} onClick={() => {
                  setEditingRuleId(rule.rosterRuleId);
                  setRuleForm(toRosterRuleForm(rule, selectedLot));
                  setFormError("");
                }}>
                  <td className="tw-font-bold tw-text-vm-slate-900">{getEmployeeName(employees.find((employee) => employee.employeeId === rule.employeeId))}</td>
                  <td>{getLotName(parkingLots, rule.parkingLotId)}</td>
                  <td>{shiftTypeLabels[rule.preferredShiftType]}</td>
                  <td>{getGateName(gates, rule.preferredGateId)}</td>
                  <td>{weekdayLabels[rule.weeklyDayOff]}</td>
                  <td>{assignmentModeLabels[rule.assignmentMode]}</td>
                  <td>{rule.effectiveFrom} - {rule.effectiveTo || "Không giới hạn"}</td>
                  <td><Badge tone={rule.status === "ACTIVE" ? "success" : "neutral"}>{rule.status === "ACTIVE" ? "Đang hoạt động" : "Ngưng"}</Badge></td>
                  <td>
                    <Button size="sm" variant={rule.status === "ACTIVE" ? "danger" : "secondary"} disabled={saving} onClick={(event) => {
                      event.stopPropagation();
                      void handleToggleRule(rule);
                    }}>
                      {rule.status === "ACTIVE" ? "Vô hiệu hóa" : "Kích hoạt"}
                    </Button>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={9} className="tw-text-center tw-font-bold tw-text-vm-slate-500">Chưa có quy tắc phân công.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}
