import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";

import {
  Badge,
  Button,
  Card,
  Drawer,
  Modal,
  PaginationFooter,
  SelectMenu,
  useToast,
} from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import {
  activateDevice,
  createDevice,
  getDevices,
  markDeviceMaintenance,
  markDeviceOffline,
  retireDevice,
  updateDevice,
  type DeviceApiResponse,
  type DeviceStatusApi,
  type DeviceTypeApi,
  type SaveDeviceRequest,
} from "@/features/hardware/api/deviceApi";
import {
  getGates,
  getLanes,
  getZones,
  type LaneApiResponse,
} from "@/features/parking/api/parkingTopologyApi";
import {
  getParkingLots,
  type ParkingLotApiResponse,
} from "@/features/parking/api/parkingLotsApi";
import { cn } from "@/lib/cn";
import { hasAnyPermission } from "@/shared/auth/permissions";

type TopologyLane = LaneApiResponse & {
  gateName: string;
  parkingLotId: string;
  zoneName: string;
};

type DeviceFormState = {
  configText: string;
  deviceCode: string;
  deviceType: DeviceTypeApi | "";
  ipAddress: string;
  laneId: string;
  name: string;
  parkingLotId: string;
};

const deviceTypeOptions = [
  { label: "Loại thiết bị: Tất cả", value: "all" },
  { label: "Camera", value: "CAMERA" },
  { label: "Máy tính / Kiosk", value: "KIOSK" },
  { label: "Đầu đọc thẻ", value: "CARD_READER" },
  { label: "Thanh chắn", value: "BARRIER" },
];

const deviceStatusOptions = [
  { label: "Trạng thái: Tất cả", value: "all" },
  { label: "Đang hoạt động", value: "ACTIVE" },
  { label: "Ngoại tuyến", value: "OFFLINE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
  { label: "Ngưng sử dụng", value: "RETIRED" },
];

const inputClassName =
  "tw-h-10 tw-w-full tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none tw-transition placeholder:tw-text-vm-slate-400 focus:tw-border-brand-200 focus:tw-shadow-[0_0_0_3px_rgba(37,99,235,0.08)] disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25 disabled:tw-text-vm-slate-500";

function getDeviceTypeLabel(type: DeviceTypeApi) {
  if (type === "CAMERA") return "Camera";
  if (type === "KIOSK") return "Máy tính / Kiosk";
  if (type === "CARD_READER") return "Đầu đọc thẻ";
  return "Thanh chắn";
}

function getDeviceTypeIcon(type: DeviceTypeApi) {
  if (type === "CAMERA") return "fas fa-video";
  if (type === "KIOSK") return "fas fa-desktop";
  if (type === "CARD_READER") return "fas fa-id-card";
  return "fas fa-road";
}

function getDeviceStatusLabel(status: DeviceStatusApi) {
  if (status === "ACTIVE") return "Đang hoạt động";
  if (status === "OFFLINE") return "Ngoại tuyến";
  if (status === "MAINTENANCE") return "Bảo trì";
  return "Ngưng sử dụng";
}

function getDeviceStatusTone(status: DeviceStatusApi) {
  if (status === "ACTIVE") return "success";
  if (status === "OFFLINE") return "neutral";
  if (status === "MAINTENANCE") return "warning";
  return "danger";
}

function getDeviceStatusDot(status: DeviceStatusApi) {
  if (status === "ACTIVE") return "tw-bg-emerald-500";
  if (status === "OFFLINE") return "tw-bg-slate-400";
  if (status === "MAINTENANCE") return "tw-bg-amber-500";
  return "tw-bg-red-500";
}

function formatDateTime(value: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

function getInitialForm(device: DeviceApiResponse | null): DeviceFormState {
  return {
    configText: JSON.stringify(device?.config ?? {}, null, 2),
    deviceCode: device?.deviceCode ?? "",
    deviceType: device?.deviceType ?? "",
    ipAddress: device?.ipAddress ?? "",
    laneId: device?.laneId ?? "",
    name: device?.name ?? "",
    parkingLotId: device?.parkingLotId ?? "",
  };
}

async function loadTopology(parkingLots: ParkingLotApiResponse[]) {
  const zoneGroups = await Promise.all(
    parkingLots.map(async (parkingLot) => ({
      parkingLotId: parkingLot.parkingLotId,
      zones: (await getZones({ parkingLotId: parkingLot.parkingLotId })).data ?? [],
    })),
  );

  const zones = zoneGroups.flatMap(({ parkingLotId, zones: items }) =>
    items.map((zone) => ({ ...zone, parkingLotId })),
  );
  const gateGroups = await Promise.all(
    zones.map(async (zone) => ({
      zone,
      gates: (await getGates({ zoneId: zone.zoneId })).data ?? [],
    })),
  );
  const gates = gateGroups.flatMap(({ zone, gates: items }) =>
    items.map((gate) => ({ gate, zone })),
  );
  const laneGroups = await Promise.all(
    gates.map(async ({ gate, zone }) => ({
      gate,
      lanes: (await getLanes({ gateId: gate.gateId })).data ?? [],
      zone,
    })),
  );

  return laneGroups.flatMap(({ gate, lanes, zone }) =>
    lanes.map<TopologyLane>((lane) => ({
      ...lane,
      gateName: gate.name,
      parkingLotId: zone.parkingLotId,
      zoneName: zone.name,
    })),
  );
}

function DeviceMetric({
  icon,
  iconClassName,
  label,
  meta,
  value,
}: {
  icon: string;
  iconClassName: string;
  label: string;
  meta: string;
  value: number;
}) {
  return (
    <Card className="tw-flex tw-min-h-[104px] tw-items-center tw-gap-4 tw-p-4">
      <span
        className={cn(
          "tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-text-[1.18rem]",
          iconClassName,
        )}
      >
        <i className={icon} />
      </span>
      <span className="tw-min-w-0">
        <span className="tw-block tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-600">
          {label}
        </span>
        <strong className="tw-mt-1 tw-block tw-text-[1.7rem] tw-font-black tw-leading-none tw-text-vm-slate-900">
          {value.toLocaleString("vi-VN")}
        </strong>
        <span className="tw-mt-2 tw-block tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">
          {meta}
        </span>
      </span>
    </Card>
  );
}

function DeviceEditorDrawer({
  canSave,
  device,
  lanes,
  onClose,
  onSubmit,
  open,
  parkingLots,
  saving,
}: {
  canSave: boolean;
  device: DeviceApiResponse | null;
  lanes: TopologyLane[];
  onClose: () => void;
  onSubmit: (payload: SaveDeviceRequest) => Promise<void> | void;
  open: boolean;
  parkingLots: ParkingLotApiResponse[];
  saving: boolean;
}) {
  const [form, setForm] = useState<DeviceFormState>(() => getInitialForm(device));
  const [formError, setFormError] = useState("");
  const readOnly = !canSave || device?.status === "RETIRED";

  useEffect(() => {
    setForm(getInitialForm(device));
    setFormError("");
  }, [device, open]);

  const availableLanes = useMemo(
    () => lanes.filter((lane) => lane.parkingLotId === form.parkingLotId),
    [form.parkingLotId, lanes],
  );

  const parkingLotOptions = [
    { label: "Chọn bãi xe", value: "" },
    ...parkingLots.map((parkingLot) => ({
      label: `${parkingLot.code} - ${parkingLot.name}`,
      value: parkingLot.parkingLotId,
    })),
  ];
  const laneOptions = [
    { label: "Không liên kết làn", value: "" },
    ...availableLanes.map((lane) => ({
      label: `${lane.code} - ${lane.name} · ${lane.zoneName}`,
      value: lane.laneId,
    })),
  ];
  const formTypeOptions = [
    { label: "Chọn loại thiết bị", value: "" },
    ...deviceTypeOptions.slice(1),
  ];

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (readOnly) return;
    setFormError("");

    const normalizedCode = form.deviceCode.trim().toUpperCase();
    if (!form.parkingLotId || !form.deviceType || !normalizedCode || !form.name.trim()) {
      setFormError("Vui lòng chọn bãi xe, loại thiết bị và nhập đầy đủ mã, tên thiết bị.");
      return;
    }
    if (!/^[A-Z0-9_-]+$/.test(normalizedCode)) {
      setFormError("Mã thiết bị chỉ gồm chữ in hoa, số, dấu gạch dưới hoặc gạch ngang.");
      return;
    }

    let config: Record<string, unknown> | null = null;
    try {
      const parsed = form.configText.trim() ? JSON.parse(form.configText) : {};
      if (parsed === null || Array.isArray(parsed) || typeof parsed !== "object") {
        throw new Error("invalid");
      }
      config = parsed as Record<string, unknown>;
    } catch {
      setFormError("Cấu hình phải là một JSON object hợp lệ, ví dụ {\"streamUrl\":\"...\"}.");
      return;
    }

    await onSubmit({
      config,
      deviceCode: normalizedCode,
      deviceType: form.deviceType,
      ipAddress: form.ipAddress.trim() || null,
      laneId: form.laneId || null,
      name: form.name.trim(),
      parkingLotId: form.parkingLotId,
    });
  }

  return (
    <Drawer
      actions={
        <div className={cn("tw-grid tw-gap-2", readOnly ? "tw-grid-cols-1" : "tw-grid-cols-2")}>
          <Button variant="secondary" onClick={onClose}>
            {readOnly ? "Đóng" : "Hủy"}
          </Button>
          {!readOnly ? (
            <Button form="device-editor-form" loading={saving} type="submit">
              <i className="far fa-save" />
              {device ? "Lưu thay đổi" : "Tạo thiết bị"}
            </Button>
          ) : null}
        </div>
      }
      description={
        readOnly
          ? "Thông tin thiết bị được lấy trực tiếp từ hệ thống."
          : "Liên kết thiết bị với bãi xe và làn vận hành phù hợp."
      }
      onClose={onClose}
      open={open}
      title={device ? (readOnly ? "Chi tiết thiết bị" : "Cập nhật thiết bị") : "Thêm thiết bị"}
      width="lg"
    >
      <form
        className="tw-grid tw-gap-5"
        id="device-editor-form"
        onSubmit={(event) => void handleSubmit(event)}
      >
        {device ? (
          <div className="tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
            <span className="tw-inline-flex tw-h-11 tw-w-11 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
              <i className={getDeviceTypeIcon(device.deviceType)} />
            </span>
            <div className="tw-min-w-0 tw-flex-1">
              <strong className="tw-block tw-truncate tw-text-[0.9rem] tw-font-black tw-text-vm-slate-900">
                {device.deviceCode}
              </strong>
              <span className="tw-mt-1 tw-block tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
                {getDeviceTypeLabel(device.deviceType)}
              </span>
            </div>
            <Badge tone={getDeviceStatusTone(device.status)}>
              {getDeviceStatusLabel(device.status)}
            </Badge>
          </div>
        ) : null}

        <section className="tw-grid tw-gap-4">
          <div>
            <h4 className="tw-m-0 tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">
              Thông tin định danh
            </h4>
            <p className="tw-mb-0 tw-mt-1 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
              Mã thiết bị là duy nhất trong toàn hệ thống.
            </p>
          </div>
          <div className="tw-grid tw-grid-cols-2 tw-gap-3 max-[560px]:tw-grid-cols-1">
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
                Mã thiết bị <span className="tw-text-red-500">*</span>
              </span>
              <input
                className={inputClassName}
                disabled={readOnly}
                maxLength={50}
                placeholder="VD: CAM_GATE_IN_01"
                value={form.deviceCode}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    deviceCode: event.target.value.toUpperCase(),
                  }))
                }
              />
            </label>
            <label className="tw-grid tw-gap-2">
              <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
                Loại thiết bị <span className="tw-text-red-500">*</span>
              </span>
              <SelectMenu
                ariaLabel="Loại thiết bị"
                disabled={readOnly}
                options={formTypeOptions}
                portal
                value={form.deviceType}
                onChange={(value) =>
                  setForm((current) => ({
                    ...current,
                    deviceType: value as DeviceTypeApi | "",
                  }))
                }
              />
            </label>
          </div>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
              Tên thiết bị <span className="tw-text-red-500">*</span>
            </span>
            <input
              className={inputClassName}
              disabled={readOnly}
              maxLength={150}
              placeholder="Tên dễ nhận biết trong vận hành"
              value={form.name}
              onChange={(event) =>
                setForm((current) => ({ ...current, name: event.target.value }))
              }
            />
          </label>
        </section>

        <section className="tw-grid tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5">
          <div>
            <h4 className="tw-m-0 tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">
              Vị trí lắp đặt
            </h4>
            <p className="tw-mb-0 tw-mt-1 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
              Làn xe là tùy chọn; bãi xe là thông tin bắt buộc.
            </p>
          </div>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
              Bãi xe <span className="tw-text-red-500">*</span>
            </span>
            <SelectMenu
              ariaLabel="Bãi xe"
              disabled={readOnly}
              menuClassName="tw-max-h-52"
              options={parkingLotOptions}
              portal
              value={form.parkingLotId}
              onChange={(value) =>
                setForm((current) => ({
                  ...current,
                  laneId: "",
                  parkingLotId: value,
                }))
              }
            />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
              Làn liên kết
            </span>
            <SelectMenu
              ariaLabel="Làn liên kết"
              disabled={readOnly || !form.parkingLotId}
              menuClassName="tw-max-h-52"
              options={laneOptions}
              portal
              value={form.laneId}
              onChange={(value) =>
                setForm((current) => ({ ...current, laneId: value }))
              }
            />
          </label>
        </section>

        <section className="tw-grid tw-gap-4 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5">
          <div>
            <h4 className="tw-m-0 tw-text-[0.92rem] tw-font-black tw-text-vm-slate-900">
              Kết nối và cấu hình
            </h4>
            <p className="tw-mb-0 tw-mt-1 tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
              Cấu hình mở rộng được lưu dưới dạng JSON theo thiết bị thực tế.
            </p>
          </div>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
              Địa chỉ IP
            </span>
            <input
              className={inputClassName}
              disabled={readOnly}
              maxLength={50}
              placeholder="VD: 192.168.1.20"
              value={form.ipAddress}
              onChange={(event) =>
                setForm((current) => ({ ...current, ipAddress: event.target.value }))
              }
            />
          </label>
          <label className="tw-grid tw-gap-2">
            <span className="tw-text-[0.76rem] tw-font-black tw-text-vm-slate-700">
              Cấu hình JSON
            </span>
            <textarea
              className="tw-min-h-[150px] tw-w-full tw-resize-y tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-3 tw-font-mono tw-text-[0.78rem] tw-leading-5 tw-text-vm-slate-800 tw-outline-none focus:tw-border-brand-200 disabled:tw-cursor-not-allowed disabled:tw-bg-vm-slate-25"
              disabled={readOnly}
              spellCheck={false}
              value={form.configText}
              onChange={(event) =>
                setForm((current) => ({ ...current, configText: event.target.value }))
              }
            />
          </label>
        </section>

        {device ? (
          <section className="tw-grid tw-grid-cols-2 tw-gap-3 tw-border-0 tw-border-t tw-border-solid tw-border-vm-slate-100 tw-pt-5 max-[560px]:tw-grid-cols-1">
            <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
              <span className="tw-block tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">
                Ngày tạo
              </span>
              <strong className="tw-mt-1 tw-block tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-800">
                {formatDateTime(device.createdAt)}
              </strong>
            </div>
            <div className="tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
              <span className="tw-block tw-text-[0.7rem] tw-font-bold tw-text-vm-slate-500">
                Cập nhật gần nhất
              </span>
              <strong className="tw-mt-1 tw-block tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-800">
                {formatDateTime(device.updatedAt)}
              </strong>
            </div>
          </section>
        ) : null}

        {formError ? (
          <div className="tw-rounded-vm-md tw-border tw-border-solid tw-border-red-100 tw-bg-red-50 tw-p-3 tw-text-[0.8rem] tw-font-bold tw-text-red-600">
            {formError}
          </div>
        ) : null}
      </form>
    </Drawer>
  );
}

function DeviceStatusModal({
  device,
  onChange,
  onClose,
  saving,
}: {
  device: DeviceApiResponse;
  onChange: (status: Exclude<DeviceStatusApi, "RETIRED">) => Promise<void> | void;
  onClose: () => void;
  saving: boolean;
}) {
  const options: Array<{
    description: string;
    icon: string;
    label: string;
    status: Exclude<DeviceStatusApi, "RETIRED">;
    style: string;
  }> = [
    {
      description: "Cho phép thiết bị tiếp tục tham gia vận hành.",
      icon: "fas fa-check",
      label: "Kích hoạt",
      status: "ACTIVE",
      style: "tw-border-emerald-100 tw-bg-emerald-50 tw-text-emerald-700",
    },
    {
      description: "Ghi nhận thiết bị mất kết nối hoặc tạm thời không phản hồi.",
      icon: "fas fa-plug",
      label: "Ngoại tuyến",
      status: "OFFLINE",
      style: "tw-border-slate-200 tw-bg-slate-50 tw-text-slate-700",
    },
    {
      description: "Tạm ngưng thiết bị để kiểm tra hoặc sửa chữa.",
      icon: "fas fa-tools",
      label: "Bảo trì",
      status: "MAINTENANCE",
      style: "tw-border-amber-100 tw-bg-amber-50 tw-text-amber-700",
    },
  ];

  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end">
          <Button variant="secondary" disabled={saving} onClick={onClose}>
            Đóng
          </Button>
        </div>
      }
      description="Chọn trạng thái phản ánh đúng tình trạng vận hành hiện tại."
      onClose={onClose}
      open
      title="Cập nhật trạng thái thiết bị"
      width="md"
    >
      <div className="tw-mb-4 tw-flex tw-items-center tw-gap-3 tw-rounded-vm-md tw-bg-vm-slate-25 tw-p-3">
        <span className="tw-inline-flex tw-h-10 tw-w-10 tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
          <i className={getDeviceTypeIcon(device.deviceType)} />
        </span>
        <div className="tw-min-w-0">
          <strong className="tw-block tw-truncate tw-text-[0.88rem] tw-font-black tw-text-vm-slate-900">
            {device.deviceCode}
          </strong>
          <span className="tw-text-[0.75rem] tw-font-semibold tw-text-vm-slate-500">
            Hiện tại: {getDeviceStatusLabel(device.status)}
          </span>
        </div>
      </div>
      <div className="tw-grid tw-gap-2">
        {options.map((option) => {
          const selected = option.status === device.status;
          return (
            <button
              className={cn(
                "tw-grid tw-grid-cols-[42px_minmax(0,1fr)_24px] tw-items-center tw-gap-3 tw-rounded-vm-md tw-border tw-border-solid tw-p-3 tw-text-left tw-transition hover:tw-translate-y-[-1px] disabled:tw-cursor-not-allowed disabled:tw-opacity-60",
                option.style,
              )}
              disabled={saving || selected}
              key={option.status}
              type="button"
              onClick={() => void onChange(option.status)}
            >
              <span className="tw-inline-flex tw-h-9 tw-w-9 tw-items-center tw-justify-center tw-rounded-full tw-bg-white/80">
                <i className={option.icon} />
              </span>
              <span>
                <strong className="tw-block tw-text-[0.84rem] tw-font-black">{option.label}</strong>
                <small className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-semibold tw-leading-5">
                  {option.description}
                </small>
              </span>
              {selected ? <i className="fas fa-check-circle" /> : <i className="fas fa-chevron-right" />}
            </button>
          );
        })}
      </div>
    </Modal>
  );
}

function RetireDeviceModal({
  device,
  onClose,
  onConfirm,
  saving,
}: {
  device: DeviceApiResponse;
  onClose: () => void;
  onConfirm: () => Promise<void> | void;
  saving: boolean;
}) {
  return (
    <Modal
      actions={
        <div className="tw-flex tw-justify-end tw-gap-2">
          <Button variant="secondary" disabled={saving} onClick={onClose}>
            Hủy
          </Button>
          <Button variant="danger" loading={saving} onClick={() => void onConfirm()}>
            <i className="fas fa-ban" />
            Ngưng sử dụng
          </Button>
        </div>
      }
      description="Thiết bị sẽ được chuyển sang trạng thái ngưng sử dụng và không còn tham gia vận hành."
      onClose={onClose}
      open
      title="Ngưng sử dụng thiết bị"
      width="sm"
    >
      <div className="tw-flex tw-gap-4">
        <span className="tw-inline-flex tw-h-12 tw-w-12 tw-flex-shrink-0 tw-items-center tw-justify-center tw-rounded-full tw-bg-red-50 tw-text-[1.2rem] tw-text-red-500">
          <i className="fas fa-exclamation-triangle" />
        </span>
        <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-leading-6 tw-text-vm-slate-700">
          Xác nhận ngưng sử dụng thiết bị <strong>{device.deviceCode}</strong> – {device.name}?
        </p>
      </div>
    </Modal>
  );
}

export function DeviceManagementPage() {
  const toast = useToast();
  const { user } = useAuth();
  const [devices, setDevices] = useState<DeviceApiResponse[]>([]);
  const [parkingLots, setParkingLots] = useState<ParkingLotApiResponse[]>([]);
  const [lanes, setLanes] = useState<TopologyLane[]>([]);
  const [keyword, setKeyword] = useState("");
  const [parkingLotFilter, setParkingLotFilter] = useState("all");
  const [laneFilter, setLaneFilter] = useState("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingDevice, setEditingDevice] = useState<DeviceApiResponse | null>(null);
  const [statusDevice, setStatusDevice] = useState<DeviceApiResponse | null>(null);
  const [retiringDevice, setRetiringDevice] = useState<DeviceApiResponse | null>(null);

  const canCreate = hasAnyPermission(user, ["DEVICE_CREATE_ALL"]);
  const canUpdate = hasAnyPermission(user, ["DEVICE_UPDATE_ALL"]);
  const canUpdateStatus = hasAnyPermission(user, ["DEVICE_STATUS_UPDATE_ALL"]);
  const canRetire = hasAnyPermission(user, ["DEVICE_DELETE_ALL"]);

  const loadDevices = useCallback(async () => {
    const response = await getDevices();
    setDevices(response.data ?? []);
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [deviceResponse, parkingLotResponse] = await Promise.all([
        getDevices(),
        getParkingLots(),
      ]);
      const nextParkingLots = parkingLotResponse.data ?? [];
      setDevices(deviceResponse.data ?? []);
      setParkingLots(nextParkingLots);

      try {
        setLanes(await loadTopology(nextParkingLots));
      } catch (topologyError) {
        setLanes([]);
        toast.warning(
          topologyError instanceof Error
            ? topologyError.message
            : "Không thể tải thông tin làn xe.",
          "Thiếu dữ liệu vị trí",
        );
      }
    } catch (error) {
      setDevices([]);
      setParkingLots([]);
      setLanes([]);
      toast.error(
        error instanceof Error ? error.message : "Không thể tải danh sách thiết bị.",
        "Tải dữ liệu thất bại",
      );
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const parkingLotMap = useMemo(
    () => new Map(parkingLots.map((parkingLot) => [parkingLot.parkingLotId, parkingLot])),
    [parkingLots],
  );
  const laneMap = useMemo(
    () => new Map(lanes.map((lane) => [lane.laneId, lane])),
    [lanes],
  );
  const filterLanes = useMemo(
    () =>
      parkingLotFilter === "all"
        ? lanes
        : lanes.filter((lane) => lane.parkingLotId === parkingLotFilter),
    [lanes, parkingLotFilter],
  );

  const visibleDevices = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return devices.filter((device) => {
      const parkingLot = parkingLotMap.get(device.parkingLotId);
      const lane = device.laneId ? laneMap.get(device.laneId) : null;
      const matchKeyword =
        !normalizedKeyword ||
        device.deviceCode.toLowerCase().includes(normalizedKeyword) ||
        device.name.toLowerCase().includes(normalizedKeyword) ||
        (device.ipAddress ?? "").toLowerCase().includes(normalizedKeyword) ||
        (parkingLot?.name ?? "").toLowerCase().includes(normalizedKeyword) ||
        (lane?.name ?? "").toLowerCase().includes(normalizedKeyword);

      return (
        matchKeyword &&
        (parkingLotFilter === "all" || device.parkingLotId === parkingLotFilter) &&
        (laneFilter === "all" || device.laneId === laneFilter) &&
        (typeFilter === "all" || device.deviceType === typeFilter) &&
        (statusFilter === "all" || device.status === statusFilter)
      );
    });
  }, [
    devices,
    keyword,
    laneFilter,
    laneMap,
    parkingLotFilter,
    parkingLotMap,
    statusFilter,
    typeFilter,
  ]);

  const totalPages = Math.max(1, Math.ceil(visibleDevices.length / pageSize));

  useEffect(() => {
    setCurrentPage(1);
  }, [keyword, laneFilter, pageSize, parkingLotFilter, statusFilter, typeFilter]);

  useEffect(() => {
    setCurrentPage((page) => Math.min(page, totalPages));
  }, [totalPages]);

  const startOffset = (currentPage - 1) * pageSize;
  const paginatedDevices = visibleDevices.slice(startOffset, startOffset + pageSize);
  const activeCount = devices.filter((device) => device.status === "ACTIVE").length;
  const maintenanceCount = devices.filter((device) => device.status === "MAINTENANCE").length;
  const offlineCount = devices.filter((device) => device.status === "OFFLINE").length;
  const retiredCount = devices.filter((device) => device.status === "RETIRED").length;

  const handleOpenCreate = () => {
    setEditingDevice(null);
    setDrawerOpen(true);
  };

  const handleOpenDetail = (device: DeviceApiResponse) => {
    setEditingDevice(device);
    setDrawerOpen(true);
  };

  const handleSave = async (payload: SaveDeviceRequest) => {
    setSaving(true);
    try {
      if (editingDevice) {
        await updateDevice(editingDevice.deviceId, payload);
        toast.success("Thông tin thiết bị đã được cập nhật.", "Cập nhật thành công");
      } else {
        await createDevice(payload);
        toast.success("Thiết bị mới đã được thêm vào hệ thống.", "Tạo thiết bị thành công");
      }
      setDrawerOpen(false);
      setEditingDevice(null);
      await loadDevices();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Không thể lưu thông tin thiết bị.",
        "Thao tác thất bại",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (status: Exclude<DeviceStatusApi, "RETIRED">) => {
    if (!statusDevice) return;
    setSaving(true);
    try {
      if (status === "ACTIVE") await activateDevice(statusDevice.deviceId);
      if (status === "OFFLINE") await markDeviceOffline(statusDevice.deviceId);
      if (status === "MAINTENANCE") await markDeviceMaintenance(statusDevice.deviceId);
      toast.success(
        `Thiết bị đã chuyển sang trạng thái ${getDeviceStatusLabel(status).toLowerCase()}.`,
        "Cập nhật trạng thái thành công",
      );
      setStatusDevice(null);
      await loadDevices();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Không thể cập nhật trạng thái thiết bị.",
        "Cập nhật thất bại",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleRetire = async () => {
    if (!retiringDevice) return;
    setSaving(true);
    try {
      await retireDevice(retiringDevice.deviceId);
      toast.success("Thiết bị đã được chuyển sang trạng thái ngưng sử dụng.", "Cập nhật thành công");
      setRetiringDevice(null);
      await loadDevices();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Không thể ngưng sử dụng thiết bị.",
        "Thao tác thất bại",
      );
    } finally {
      setSaving(false);
    }
  };

  const clearFilters = () => {
    setKeyword("");
    setParkingLotFilter("all");
    setLaneFilter("all");
    setTypeFilter("all");
    setStatusFilter("all");
  };

  const parkingLotOptions = [
    { label: "Bãi xe: Tất cả", value: "all" },
    ...parkingLots.map((parkingLot) => ({
      label: `${parkingLot.code} - ${parkingLot.name}`,
      value: parkingLot.parkingLotId,
    })),
  ];
  const laneOptions = [
    { label: "Làn xe: Tất cả", value: "all" },
    ...filterLanes.map((lane) => ({
      label: `${lane.code} - ${lane.name}`,
      value: lane.laneId,
    })),
  ];

  return (
    <div className="tw-px-4 tw-py-4 lg:tw-px-5">
      <section className="tw-mx-auto tw-min-h-[calc(100vh-104px)] tw-w-[min(100%,1560px)] tw-rounded-vm-lg tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-p-5 tw-shadow-vm-card">
        <header className="tw-flex tw-items-start tw-justify-between tw-gap-4 max-[720px]:tw-flex-col">
          <div>
            <h1 className="tw-m-0 tw-text-vm-page-title tw-text-vm-slate-900">
              Quản lý thiết bị
            </h1>
            <p className="tw-mb-0 tw-mt-2 tw-text-[0.86rem] tw-font-semibold tw-text-vm-slate-500">
              Quản lý camera, kiosk, đầu đọc thẻ và thanh chắn theo dữ liệu vận hành thực tế
            </p>
          </div>
          <div className="tw-flex tw-gap-2 max-[720px]:tw-w-full">
            <Button
              className="max-[720px]:tw-flex-1"
              variant="secondary"
              disabled={loading}
              onClick={() => void loadData()}
            >
              <i className="fas fa-sync-alt" />
              Làm mới
            </Button>
            {canCreate ? (
              <Button className="max-[720px]:tw-flex-1" onClick={handleOpenCreate}>
                <i className="fas fa-plus" />
                Thêm thiết bị
              </Button>
            ) : null}
          </div>
        </header>

        <div className="tw-mt-6 tw-grid tw-grid-cols-4 tw-gap-3 max-[1180px]:tw-grid-cols-2 max-[620px]:tw-grid-cols-1">
          <DeviceMetric
            icon="fas fa-microchip"
            iconClassName="tw-bg-brand-50 tw-text-vm-primary"
            label="Tổng thiết bị"
            meta={`${retiredCount.toLocaleString("vi-VN")} đã ngưng sử dụng`}
            value={devices.length}
          />
          <DeviceMetric
            icon="fas fa-check-circle"
            iconClassName="tw-bg-emerald-50 tw-text-emerald-600"
            label="Đang hoạt động"
            meta="Sẵn sàng vận hành"
            value={activeCount}
          />
          <DeviceMetric
            icon="fas fa-tools"
            iconClassName="tw-bg-amber-50 tw-text-amber-600"
            label="Đang bảo trì"
            meta="Cần theo dõi kỹ thuật"
            value={maintenanceCount}
          />
          <DeviceMetric
            icon="fas fa-plug"
            iconClassName="tw-bg-slate-100 tw-text-slate-600"
            label="Ngoại tuyến"
            meta="Chưa kết nối hệ thống"
            value={offlineCount}
          />
        </div>

        <div className="tw-mt-5 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-vm-slate-25 tw-p-3">
          <div className="tw-flex tw-w-full tw-items-center tw-gap-3 tw-overflow-x-auto tw-pb-1">
            <label className="tw-flex tw-h-[42px] tw-min-w-[260px] tw-flex-1 tw-items-center tw-gap-2 tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100 tw-bg-white tw-px-3">
              <i className="fas fa-search tw-text-vm-slate-500" />
              <input
                className="tw-min-w-0 tw-flex-1 tw-border-0 tw-bg-transparent tw-text-[0.84rem] tw-font-semibold tw-text-vm-slate-900 tw-outline-none placeholder:tw-text-vm-slate-400"
                placeholder="Tìm mã, tên, IP, bãi hoặc làn..."
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
              />
            </label>
            <div className="tw-w-[190px] tw-flex-none">
              <SelectMenu
                ariaLabel="Bãi xe"
                menuClassName="tw-max-h-60"
                options={parkingLotOptions}
                portal
                value={parkingLotFilter}
                onChange={(value) => {
                  setParkingLotFilter(value);
                  setLaneFilter("all");
                }}
              />
            </div>
            <div className="tw-w-[180px] tw-flex-none">
              <SelectMenu
                ariaLabel="Làn xe"
                disabled={filterLanes.length === 0}
                menuClassName="tw-max-h-60"
                options={laneOptions}
                portal
                value={laneFilter}
                onChange={setLaneFilter}
              />
            </div>
            <div className="tw-w-[170px] tw-flex-none">
              <SelectMenu
                ariaLabel="Loại thiết bị"
                options={deviceTypeOptions}
                portal
                value={typeFilter}
                onChange={setTypeFilter}
              />
            </div>
            <div className="tw-w-[170px] tw-flex-none">
              <SelectMenu
                ariaLabel="Trạng thái thiết bị"
                options={deviceStatusOptions}
                portal
                value={statusFilter}
                onChange={setStatusFilter}
              />
            </div>
            <Button className="tw-flex-none tw-whitespace-nowrap" variant="secondary" onClick={clearFilters}>
              <i className="fas fa-sync-alt" />
              Xóa lọc
            </Button>
          </div>
        </div>

        <div className="tw-mt-5 tw-overflow-hidden tw-rounded-vm-md tw-border tw-border-solid tw-border-vm-slate-100">
          <div className="tw-overflow-x-auto">
            <table className="table tw-m-0 tw-w-full tw-min-w-[1190px] tw-table-fixed [&_td]:tw-border-0 [&_td]:tw-border-t [&_td]:tw-border-solid [&_td]:tw-border-vm-slate-100 [&_td]:tw-px-4 [&_td]:tw-py-3 [&_td]:tw-align-middle [&_thead_th]:tw-border-0 [&_thead_th]:tw-bg-vm-slate-25 [&_thead_th]:tw-px-4 [&_thead_th]:tw-py-3.5 [&_thead_th]:tw-text-left [&_thead_th]:tw-text-[0.75rem] [&_thead_th]:tw-font-black [&_thead_th]:tw-text-vm-slate-700">
              <colgroup>
                <col className="tw-w-[260px]" />
                <col className="tw-w-[140px]" />
                <col className="tw-w-[230px]" />
                <col className="tw-w-[150px]" />
                <col className="tw-w-[160px]" />
                <col className="tw-w-[130px]" />
                <col className="tw-w-[120px]" />
              </colgroup>
              <thead>
                <tr>
                  <th>Thiết bị</th>
                  <th>Loại</th>
                  <th>Vị trí lắp đặt</th>
                  <th>Kết nối</th>
                  <th>Trạng thái</th>
                  <th>Cập nhật</th>
                  <th className="tw-text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {paginatedDevices.map((device) => {
                  const parkingLot = parkingLotMap.get(device.parkingLotId);
                  const lane = device.laneId ? laneMap.get(device.laneId) : null;
                  return (
                    <tr
                      aria-label={`Mở thông tin thiết bị ${device.deviceCode}`}
                      className="tw-cursor-pointer tw-transition hover:tw-bg-vm-slate-25 focus-visible:tw-bg-brand-50 focus-visible:tw-outline-none"
                      key={device.deviceId}
                      role="button"
                      tabIndex={0}
                      onClick={() => handleOpenDetail(device)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          handleOpenDetail(device);
                        }
                      }}
                    >
                      <td>
                        <button
                          className="tw-flex tw-w-full tw-min-w-0 tw-items-center tw-gap-3 tw-border-0 tw-bg-transparent tw-p-0 tw-text-left"
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleOpenDetail(device);
                          }}
                        >
                          <span className="tw-inline-flex tw-h-10 tw-w-10 tw-flex-none tw-items-center tw-justify-center tw-rounded-vm-md tw-bg-brand-50 tw-text-vm-primary">
                            <i className={getDeviceTypeIcon(device.deviceType)} />
                          </span>
                          <span className="tw-min-w-0 tw-flex-1">
                            <strong className="tw-block tw-truncate tw-text-[0.82rem] tw-font-black tw-text-vm-slate-900">
                              {device.deviceCode}
                            </strong>
                            <small className="tw-mt-1 tw-block tw-truncate tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                              {device.name}
                            </small>
                          </span>
                        </button>
                      </td>
                      <td className="tw-text-[0.8rem] tw-font-semibold tw-text-vm-slate-700">
                        {getDeviceTypeLabel(device.deviceType)}
                      </td>
                      <td>
                        <strong className="tw-block tw-truncate tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-800">
                          {parkingLot?.name ?? "Không xác định"}
                        </strong>
                        <small className="tw-mt-1 tw-block tw-truncate tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                          {lane ? `${lane.code} · ${lane.name}` : "Không liên kết làn"}
                        </small>
                      </td>
                      <td>
                        <strong className="tw-block tw-truncate tw-text-[0.8rem] tw-font-extrabold tw-text-vm-slate-800">
                          {device.ipAddress || "--"}
                        </strong>
                        <small className="tw-mt-1 tw-block tw-text-[0.72rem] tw-font-semibold tw-text-vm-slate-500">
                          {device.ipAddress ? "Đã cấu hình IP" : "Chưa cấu hình IP"}
                        </small>
                      </td>
                      <td>
                        <Badge
                          className="tw-w-fit tw-gap-1.5 tw-whitespace-nowrap tw-px-2.5"
                          tone={getDeviceStatusTone(device.status)}
                        >
                          <span
                            className={cn(
                              "tw-h-1.5 tw-w-1.5 tw-flex-none tw-rounded-full",
                              getDeviceStatusDot(device.status),
                            )}
                          />
                          {getDeviceStatusLabel(device.status)}
                        </Badge>
                      </td>
                      <td className="tw-text-[0.72rem] tw-font-semibold tw-leading-5 tw-text-vm-slate-600">
                        {formatDateTime(device.updatedAt ?? device.createdAt)}
                      </td>
                      <td>
                        <div className="tw-flex tw-justify-end tw-gap-1">
                          <button
                            aria-label={`Xem chi tiết thiết bị ${device.deviceCode}`}
                            className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-vm-primary hover:tw-bg-brand-50"
                            title={canUpdate && device.status !== "RETIRED" ? "Chỉnh sửa thiết bị" : "Xem chi tiết"}
                            type="button"
                            onClick={(event) => {
                              event.stopPropagation();
                              handleOpenDetail(device);
                            }}
                          >
                            <i className={canUpdate && device.status !== "RETIRED" ? "far fa-edit" : "far fa-eye"} />
                          </button>
                          {canUpdateStatus && device.status !== "RETIRED" ? (
                            <button
                              aria-label={
                                device.status === "MAINTENANCE"
                                  ? `Kích hoạt lại thiết bị ${device.deviceCode}`
                                  : `Đánh dấu bảo trì thiết bị ${device.deviceCode}`
                              }
                              className={cn(
                                "tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent",
                                device.status === "MAINTENANCE"
                                  ? "tw-text-emerald-600 hover:tw-bg-emerald-50"
                                  : "tw-text-amber-600 hover:tw-bg-amber-50",
                              )}
                              title={device.status === "MAINTENANCE" ? "Kích hoạt lại" : "Đánh dấu bảo trì"}
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                setStatusDevice(device);
                              }}
                            >
                              <i
                                className={
                                  device.status === "MAINTENANCE"
                                    ? "fas fa-check-circle"
                                    : "fas fa-tools"
                                }
                              />
                            </button>
                          ) : null}
                          {canRetire && device.status !== "RETIRED" ? (
                            <button
                              aria-label={`Ngưng sử dụng thiết bị ${device.deviceCode}`}
                              className="tw-inline-flex tw-h-8 tw-w-8 tw-items-center tw-justify-center tw-rounded-vm-md tw-border-0 tw-bg-transparent tw-text-red-500 hover:tw-bg-red-50"
                              title="Ngưng sử dụng"
                              type="button"
                              onClick={(event) => {
                                event.stopPropagation();
                                setRetiringDevice(device);
                              }}
                            >
                              <i className="fas fa-ban" />
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {paginatedDevices.length === 0 ? (
                  <tr>
                    <td className="tw-py-12 tw-text-center" colSpan={7}>
                      <span className="tw-inline-flex tw-h-12 tw-w-12 tw-items-center tw-justify-center tw-rounded-full tw-bg-vm-slate-50 tw-text-vm-slate-400">
                        <i className={loading ? "fas fa-spinner fa-spin" : "fas fa-microchip"} />
                      </span>
                      <p className="tw-mb-0 tw-mt-3 tw-text-[0.84rem] tw-font-bold tw-text-vm-slate-500">
                        {loading
                          ? "Đang tải danh sách thiết bị..."
                          : "Chưa có thiết bị phù hợp với bộ lọc."}
                      </p>
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>

          <PaginationFooter
            ariaLabel="Phân trang thiết bị"
            currentPage={currentPage}
            endIndex={visibleDevices.length === 0 ? 0 : startOffset + paginatedDevices.length}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
            pageSize={pageSize}
            pageSizeOptions={[5, 10, 20]}
            startIndex={visibleDevices.length === 0 ? 0 : startOffset + 1}
            totalPages={totalPages}
            totalRecords={visibleDevices.length}
          />
        </div>
      </section>

      <DeviceEditorDrawer
        canSave={editingDevice ? canUpdate : canCreate}
        device={editingDevice}
        lanes={lanes}
        onClose={() => {
          if (saving) return;
          setDrawerOpen(false);
          setEditingDevice(null);
        }}
        onSubmit={handleSave}
        open={drawerOpen}
        parkingLots={parkingLots}
        saving={saving}
      />
      {statusDevice ? (
        <DeviceStatusModal
          device={statusDevice}
          onChange={handleStatusChange}
          onClose={() => {
            if (!saving) setStatusDevice(null);
          }}
          saving={saving}
        />
      ) : null}
      {retiringDevice ? (
        <RetireDeviceModal
          device={retiringDevice}
          onClose={() => {
            if (!saving) setRetiringDevice(null);
          }}
          onConfirm={handleRetire}
          saving={saving}
        />
      ) : null}
    </div>
  );
}
