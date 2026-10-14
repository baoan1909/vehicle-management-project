import { useEffect, useRef, useState } from "react";

import { Button, ConfirmDialog, Modal, SelectMenu, useToast } from "@/components/ui";
import { useAuth } from "@/core/auth/useAuth";
import { hasAnyPermission } from "@/shared/auth/permissions";
import { ParkingLotInfoForm, type ParkingLotInfoValue } from "@/features/parking/components/ParkingLotInfoForm";
import {
  confirmParkingLotLocation,
  createParkingLot,
  geocodeParkingLot,
  type ParkingLotApiResponse,
} from "@/features/parking/api/parkingLotsApi";
import { createParkingLevel } from "@/features/parking/api/parkingLayoutApi";
import { getProvisionedAccounts, type ProvisionedAccountResponse } from "@/features/iam/api/provisionedAccountApi";
import { assignParkingManagerToParkingLot } from "@/features/iam/api/organizationsApi";

type LotKind = "GROUND" | "MULTI";

const KIND_OPTIONS = [
  { label: "Bãi mặt đất (1 tầng GROUND)", value: "GROUND" },
  { label: "Bãi nhiều tầng (GROUND + tầng nổi)", value: "MULTI" },
];

const LEVEL_CANVAS = { canvasHeight: 700, canvasWidth: 1000, floorHeight: 3.5 };

function StepDots({ step }: { step: number }) {
  return (
    <div className="tw-flex tw-items-center tw-gap-2" aria-label={`Bước ${step} trên 3`}>
      {[1, 2, 3].map((item) => (
        <span
          key={item}
          className={
            item === step
              ? "tw-h-2 tw-w-6 tw-rounded-full tw-bg-[#2563eb]"
              : item < step
                ? "tw-h-2 tw-w-2 tw-rounded-full tw-bg-[#2563eb]"
                : "tw-h-2 tw-w-2 tw-rounded-full tw-bg-[#cbd5e1]"
          }
        />
      ))}
      <span className="tw-ml-1 tw-text-[0.78rem] tw-font-bold tw-text-[#64748b]">Bước {step}/3</span>
    </div>
  );
}

/**
 * Modal wizard "Thêm bãi": B1 thông tin bãi → B2 loại hình + tầng → B3 phân công Manager.
 * Không cho quay lại B1 sau khi bãi đã tạo để tránh tạo trùng bãi.
 */
export function ParkingLotCreateWizard({ open, onClose, onCompleted }: {
  open: boolean;
  onClose: () => void;
  onCompleted: (lot: ParkingLotApiResponse) => void;
}) {
  const { user } = useAuth();
  const toast = useToast();

  const [step, setStep] = useState(1);
  const [infoBusy, setInfoBusy] = useState(false);
  const [infoError, setInfoError] = useState<string | null>(null);
  const [createdLot, setCreatedLot] = useState<ParkingLotApiResponse | null>(null);
  const completedRef = useRef(false);

  const [kind, setKind] = useState<LotKind>("GROUND");
  const [floorCount, setFloorCount] = useState("1");
  const [levelsBusy, setLevelsBusy] = useState(false);
  const [levelsError, setLevelsError] = useState<string | null>(null);
  const [levelsDone, setLevelsDone] = useState<string[]>([]);
  const [retryFrom, setRetryFrom] = useState<number | null>(null);

  const [managers, setManagers] = useState<ProvisionedAccountResponse[]>([]);
  const [managersLoading, setManagersLoading] = useState(false);
  const [managersError, setManagersError] = useState<string | null>(null);
  const [managerId, setManagerId] = useState("");
  const [assignBusy, setAssignBusy] = useState(false);
  const [assignError, setAssignError] = useState<string | null>(null);
  const [managerReloadKey, setManagerReloadKey] = useState(0);
  const [closeConfirmOpen, setCloseConfirmOpen] = useState(false);

  const canAssignManager = hasAnyPermission(user, ["ORGANIZATION_MEMBERSHIP_MANAGE_ALL"]);

  useEffect(() => {
    if (!open) return;
    setStep(1);
    setInfoBusy(false);
    setInfoError(null);
    setCreatedLot(null);
    completedRef.current = false;
    setKind("GROUND");
    setFloorCount("1");
    setLevelsBusy(false);
    setLevelsError(null);
    setLevelsDone([]);
    setRetryFrom(null);
    setManagers([]);
    setManagersLoading(false);
    setManagersError(null);
    setManagerId("");
    setAssignBusy(false);
    setAssignError(null);
    setManagerReloadKey(0);
    setCloseConfirmOpen(false);
  }, [open ]);

  useEffect(() => {
    if (!open || step !== 3 || !canAssignManager) return;
    let alive = true;
    (async () => {
      setManagersLoading(true);
      setManagersError(null);
      try {
        const response = await getProvisionedAccounts({ accountStatus: "ACTIVE", roleCode: "PARKING_MANAGER" });
        if (!alive) return;
        const list = response.data ?? [];
        setManagers(list);
        setManagerId(list[0]?.account.accountId ?? "");
      } catch (requestError) {
        if (!alive) return;
        setManagers([]);
        setManagersError(requestError instanceof Error ? requestError.message : "Không thể tải danh sách Parking Manager.");
      } finally {
        if (alive) setManagersLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [open, step, canAssignManager, managerReloadKey]);

  function finish(lot: ParkingLotApiResponse) {
    if (completedRef.current) return;
    completedRef.current = true;
    onCompleted(lot);
    onClose();
  }

  function handleClose() {
    if (infoBusy || levelsBusy || assignBusy) return;
    if (createdLot) {
      setCloseConfirmOpen(true);
      return;
    }
    onClose();
  }

  async function handleInfoSubmit(info: ParkingLotInfoValue) {
    setInfoBusy(true);
    setInfoError(null);
    try {
      const created = await createParkingLot({
        addressDisplay: info.addressDisplay,
        addressInputScheme: info.addressInputScheme,
        code: info.code,
        currentWardCode: info.currentWardCode,
        latitude: info.latitude,
        legacyWardCode: info.legacyWardCode,
        longitude: info.longitude,
        name: info.name,
        totalCapacity: info.totalCapacity,
      });
      const lot = created.data;
      if (info.latitude !== null && info.longitude !== null) {
        try {
          if (info.locationResolution === "MANUAL") {
            await confirmParkingLotLocation(lot.parkingLotId, info.latitude, info.longitude);
          } else if (info.locationResolution === "GEOCODED") {
            await geocodeParkingLot(lot.parkingLotId);
          }
        } catch (locationError) {
          toast.warning(locationError instanceof Error ? locationError.message : "Đã tạo bãi nhưng chưa xác nhận được vị trí; có thể bổ sung sau.");
        }
      }
      setCreatedLot(lot);
      toast.success("Đã tạo bãi xe.");
      setStep(2);
    } catch (requestError) {
      setInfoError(requestError instanceof Error ? requestError.message : "Không thể tạo bãi xe.");
    } finally {
      setInfoBusy(false);
    }
  }

  function levelDefs(total: number) {
    const defs = [{
      code: "GROUND",
      displayOrder: 0,
      elevation: 0,
      name: "Tầng trệt",
      ...LEVEL_CANVAS,
    }];
    for (let index = 1; index <= total; index += 1) {
      defs.push({
        code: `L${index}`,
        displayOrder: index,
        elevation: index * 3.5,
        name: `Tầng ${index}`,
        ...LEVEL_CANVAS,
      });
    }
    return defs;
  }

  async function handleCreateLevels(fromIndex: number) {
    if (!createdLot || levelsBusy) return;
    const requestedFloorCount = Number(floorCount);
    if (kind === "MULTI" && (!Number.isInteger(requestedFloorCount) || requestedFloorCount < 1 || requestedFloorCount > 50)) {
      setLevelsError("Số tầng nổi phải là số nguyên từ 1 đến 50.");
      return;
    }
    const extraFloors = kind === "MULTI" ? requestedFloorCount : 0;
    const defs = levelDefs(extraFloors);
    setLevelsBusy(true);
    setLevelsError(null);
    try {
      const done: string[] = fromIndex === 0 ? [] : [...levelsDone];
      for (let index = fromIndex; index < defs.length; index += 1) {
        try {
          // eslint-disable-next-line no-await-in-loop
          await createParkingLevel(createdLot.parkingLotId, defs[index]);
          done.push(defs[index].code);
          setLevelsDone([...done]);
          toast.success(`Đã tạo tầng ${defs[index].code}.`);
        } catch (requestError) {
          setRetryFrom(index);
          throw new Error(`Không tạo được tầng ${defs[index].code}: ${requestError instanceof Error ? requestError.message : "lỗi không xác định"}`);
        }
      }
      setRetryFrom(null);
      toast.success(`Đã tạo ${done.length} tầng cho bãi ${createdLot.code}.`);
      setStep(3);
    } catch (requestError) {
      setLevelsError(requestError instanceof Error ? requestError.message : "Không thể tạo tầng.");
    } finally {
      setLevelsBusy(false);
    }
  }

  async function handleAssignAndFinish() {
    if (!createdLot || !managerId || assignBusy) return;
    setAssignBusy(true);
    setAssignError(null);
    try {
      await assignParkingManagerToParkingLot(createdLot.organizationId, createdLot.parkingLotId, managerId);
      toast.success("Đã phân công Parking Manager cho bãi xe.");
      finish(createdLot);
    } catch (requestError) {
      setAssignError(requestError instanceof Error ? requestError.message : "Không thể phân công Parking Manager.");
    } finally {
      setAssignBusy(false);
    }
  }

  const stepTitles = ["", "Thêm bãi xe", "Loại hình bãi", "Phân công quản lý"];
  const stepDescriptions = [
    "",
    "Nhập thông tin và địa chỉ của bãi mới.",
    createdLot ? `Tạo tầng khởi đầu cho bãi ${createdLot.code} — ${createdLot.name}.` : "Tạo tầng khởi đầu cho bãi mới.",
    "Giao bãi cho Parking Manager vận hành (tùy chọn).",
  ];

  return (
    <>
      <Modal
      open={open}
      width="lg"
      title={stepTitles[step]}
      description={stepDescriptions[step]}
      onClose={handleClose}
      actions={step === 1 ? undefined : (
        <div className="tw-flex tw-justify-end tw-gap-2">
          {step === 2 ? (
            <>
              {retryFrom !== null && levelsDone.length > 0 ? (
                <Button variant="secondary" disabled={levelsBusy} onClick={() => setStep(3)}>Tiếp tục với {levelsDone.length} tầng đã tạo</Button>
              ) : null}
              <Button loading={levelsBusy} onClick={() => void handleCreateLevels(retryFrom ?? 0)}>
                <i className="fas fa-layer-group" aria-hidden="true" />
                {retryFrom !== null ? `Thử lại từ tầng ${retryFrom + 1}` : "Tạo tầng và tiếp tục"}
              </Button>
            </>
          ) : (
            <>
              <Button variant="secondary" disabled={assignBusy} onClick={() => createdLot && finish(createdLot)}>Hoàn tất</Button>
              {canAssignManager ? (
                <Button loading={assignBusy} disabled={!managerId} onClick={() => void handleAssignAndFinish()}>
                  <i className="fas fa-user-check" aria-hidden="true" />
                  Phân công và hoàn tất
                </Button>
              ) : null}
            </>
          )}
        </div>
      )}
    >
      <div className="tw-grid tw-gap-4">
        <StepDots step={step} />

        {step === 1 ? (
          <ParkingLotInfoForm
            formKey="wizard-new-lot"
            initial={null}
            submitLabel="Tạo bãi và tiếp tục"
            submitting={infoBusy}
            serverError={infoError}
            onSubmit={(info) => void handleInfoSubmit(info)}
            onClose={handleClose}
          />
        ) : null}

        {step === 2 ? (
          <div className="tw-grid tw-gap-3.5">
            {levelsError ? (
              <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
                {levelsError}
              </p>
            ) : null}
            <label className="tw-block">
              <span className="tw-mb-1 tw-block tw-text-[0.84rem] tw-font-bold tw-text-[#0f172a]">Loại hình bãi <span className="tw-text-red-500">*</span></span>
              <SelectMenu
                ariaLabel="Loại hình bãi"
                value={kind}
                onChange={(value) => setKind(value === "MULTI" ? "MULTI" : "GROUND")}
                options={KIND_OPTIONS}
              />
            </label>
            {kind === "MULTI" ? (
              <label className="tw-block">
                <span className="tw-mb-1 tw-block tw-text-[0.84rem] tw-font-bold tw-text-[#0f172a]">Số tầng nổi (ngoài tầng trệt) <span className="tw-text-red-500">*</span></span>
                <input
                  type="number"
                  min={1}
                  max={50}
                  step={1}
                  className="tw-h-10 tw-w-full tw-rounded-[8px] tw-border tw-border-solid tw-border-[#dbe3f0] tw-bg-white tw-px-3 tw-text-[0.9rem] tw-font-medium tw-text-[#0f172a] tw-outline-none focus:tw-border-[#2563eb]"
                  value={floorCount}
                  disabled={levelsBusy}
                  onChange={(event) => setFloorCount(event.target.value)}
                />
                <span className="tw-mt-1 tw-block tw-text-[0.78rem] tw-font-medium tw-text-[#64748b]">
                  Hệ thống sẽ tạo tầng GROUND (Tầng trệt) rồi tới L1…LN. Tầng hầm thêm thủ công sau.
                </span>
              </label>
            ) : (
              <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#dbeafe] tw-bg-[#eff6ff] tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-medium tw-text-[#1d4ed8]">
                Bãi mặt đất dùng chung 1 tầng GROUND; mỗi cổng ra/vào riêng được cấu hình ở bước Cấu trúc sau.
              </p>
            )}
            {levelsDone.length > 0 ? (
              <p className="tw-m-0 tw-text-[0.84rem] tw-font-semibold tw-text-[#15803d]">
                Đã tạo: {levelsDone.join(", ")}
              </p>
            ) : null}
            <p className="tw-m-0 tw-text-[0.82rem] tw-font-medium tw-text-[#64748b]">
              Bãi nhiều tầng chung một lối ra vào: mỗi tầng tạo 1 cổng cùng mã ở bước Cấu trúc; bãi mặt đất: mỗi bãi cổng riêng.
            </p>
          </div>
        ) : null}

        {step === 3 ? (
          <div className="tw-grid tw-gap-3.5">
            {createdLot ? (
              <div className="tw-rounded-[8px] tw-border tw-border-solid tw-border-[#dbeafe] tw-bg-[#eff6ff] tw-px-3 tw-py-2">
                <p className="tw-m-0 tw-text-[0.9rem] tw-font-extrabold tw-text-[#0f172a]">{createdLot.code} — {createdLot.name}</p>
                <p className="tw-m-0 tw-mt-0.5 tw-text-[0.82rem] tw-font-medium tw-text-[#475569]">{createdLot.addressDisplay ?? "Chưa có địa chỉ"}</p>
              </div>
            ) : null}
            {assignError ? (
              <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-red-200 tw-bg-red-50 tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-red-700">
                {assignError}
              </p>
            ) : null}
            {!canAssignManager ? (
              <p className="tw-m-0 tw-rounded-[8px] tw-border tw-border-solid tw-border-[#fde68a] tw-bg-[#fffbeb] tw-px-3 tw-py-2 tw-text-[0.84rem] tw-font-semibold tw-text-[#92400e]">
                Bạn không có quyền phân công Manager. Hãy bấm Hoàn tất; Partner Admin có thể gán sau.
              </p>
            ) : managersLoading ? (
              <p className="tw-m-0 tw-text-[0.86rem] tw-font-semibold tw-text-[#64748b]">
                <i className="fas fa-sync-alt fa-spin tw-mr-2" aria-hidden="true" /> Đang tải danh sách Parking Manager…
              </p>
            ) : (
              <>
                <label className="tw-block">
                  <span className="tw-mb-1 tw-block tw-text-[0.84rem] tw-font-bold tw-text-[#0f172a]">Parking Manager</span>
                  <SelectMenu
                    ariaLabel="Parking Manager"
                    value={managerId}
                    onChange={setManagerId}
                    clearValue=""
                    options={[{ label: "Chọn Parking Manager", value: "" }, ...managers.map((manager) => ({
                      label: `${manager.account.username} · ${manager.account.email}`,
                      value: manager.account.accountId,
                    }))]}
                  />
                </label>
                {managersError ? (
                  <div className="tw-flex tw-items-center tw-justify-between tw-gap-3 tw-rounded-[8px] tw-bg-red-50 tw-px-3 tw-py-2">
                    <p className="tw-m-0 tw-text-[0.82rem] tw-font-semibold tw-text-red-600">{managersError}</p>
                    <Button size="sm" variant="secondary" onClick={() => setManagerReloadKey((value) => value + 1)}>Thử lại</Button>
                  </div>
                ) : null}
                {managers.length === 0 && !managersError ? (
                  <p className="tw-m-0 tw-rounded-[8px] tw-bg-amber-50 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-semibold tw-text-amber-800">
                    Chưa có tài khoản Parking Manager đang hoạt động. Hãy tạo hoặc duyệt tài khoản Manager trước khi phân công, hoặc bấm Hoàn tất để gán sau.
                  </p>
                ) : null}
                <p className="tw-m-0 tw-rounded-[8px] tw-bg-vm-slate-25 tw-px-3 tw-py-2 tw-text-[0.82rem] tw-font-medium tw-text-vm-slate-600">
                  Sau khi được giao, Manager sẽ thiết lập topology, loại xe và loại vé. Khi hoàn tất, Manager gửi yêu cầu để Partner Admin kiểm tra và kích hoạt bãi.
                </p>
              </>
            )}
          </div>
        ) : null}
      </div>
      </Modal>
      <ConfirmDialog
        cancelLabel="Tiếp tục thiết lập"
        confirmLabel="Đóng và giữ bãi"
        message="Bãi xe đã được tạo. Nếu đóng wizard lúc này, dữ liệu đã tạo vẫn được giữ và bạn có thể tiếp tục thêm tầng hoặc gán Manager trong tab Cấu trúc."
        onCancel={() => setCloseConfirmOpen(false)}
        onConfirm={() => {
          setCloseConfirmOpen(false);
          if (createdLot) finish(createdLot);
        }}
        open={closeConfirmOpen}
        title="Dừng thiết lập bãi xe?"
      />
    </>
  );
}
