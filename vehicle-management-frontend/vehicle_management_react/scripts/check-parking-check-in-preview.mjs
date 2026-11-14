import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import vm from "node:vm";
import ts from "typescript";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const helperSource = read("../src/features/parking/utils/parkingCheckInState.ts");
const transpiledHelper = ts.transpileModule(helperSource, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText;
const helperModule = { exports: {} };
vm.runInNewContext(transpiledHelper, { exports: helperModule.exports, module: helperModule });

const {
  isCheckInPreviewAdmissible,
} = helperModule.exports;

const capacityVisitor = {
  customerType: "VISITOR",
  eligibleSpaces: [],
  parkingLotId: "lot",
  physicalAvailable: 2,
  recommendedParkingSpaceId: null,
  selectionRequired: false,
  trackingMode: "CAPACITY",
  visitorAvailable: 1,
  zoneId: "zone",
};
assert.equal(isCheckInPreviewAdmissible(capacityVisitor), true);
assert.equal(isCheckInPreviewAdmissible({ ...capacityVisitor, visitorAvailable: 0 }), false);
assert.equal(
  isCheckInPreviewAdmissible({ ...capacityVisitor, customerType: "SUBSCRIPTION", physicalAvailable: 0 }),
  false,
);

const spaceVisitor = {
  ...capacityVisitor,
  eligibleSpaces: [
    { code: "A-08", parkingSpaceId: "space-a", status: "AVAILABLE" },
    { code: "A-09", parkingSpaceId: "space-b", status: "AVAILABLE" },
  ],
  recommendedParkingSpaceId: null,
  selectionRequired: true,
  trackingMode: "SPACE",
};
assert.equal(isCheckInPreviewAdmissible(spaceVisitor), true);
assert.equal(isCheckInPreviewAdmissible({ ...spaceVisitor, eligibleSpaces: [] }), false);

const spaceSubscription = {
  ...spaceVisitor,
  customerType: "SUBSCRIPTION",
  eligibleSpaces: [{ code: "S-01", parkingSpaceId: "space-s", status: "RESERVED" }],
  recommendedParkingSpaceId: "space-s",
  selectionRequired: false,
};
assert.equal(isCheckInPreviewAdmissible(spaceSubscription), true);
assert.equal(isCheckInPreviewAdmissible({ ...spaceSubscription, eligibleSpaces: [] }), false);

const api = read("../src/features/parking/api/parkingSessionApi.ts");
const hook = read("../src/features/parking/hooks/useCheckInPreview.ts");
const holdHook = read("../src/features/parking/hooks/useCheckInSpaceHold.ts");
const operationPage = read("../src/features/parking/pages/SwipeListPage.tsx");
const operationForm = read("../src/features/parking/components/ParkingOperationForm.tsx");
const locationPanel = read("../src/features/parking/components/ParkingCheckInLocationPanel.tsx");
const summary = read("../src/features/parking/components/ParkingSessionSummary.tsx");
const cameraPanel = read("../src/features/parking/components/ParkingCameraPanel.tsx");
const selectMenu = read("../src/shared/components/ui/SelectMenu.tsx");
const toastProvider = read("../src/shared/components/ui/ToastProvider.tsx");
const apiMessages = read("../src/core/api/apiMessage.ts");
const routes = read("../src/app/routes.tsx");

assert.match(api, /\/check-in\/preview/);
assert.match(api, /\/check-in\/space-holds/);
assert.match(api, /spaceHoldId\?: string/);
assert.match(api, /formData\.append\("vehicleImage", vehicleImage/);
assert.doesNotMatch(api, /VehicleMatchStatus|vehicleMatchStatus/);
assert.doesNotMatch(api.match(/export type CheckInParkingSessionRequest = \{[\s\S]*?\n\};/)?.[0] ?? "", /parkingSpaceId/);
assert.match(hook, /AbortController/);
assert.match(hook, /requestSequenceRef/);
assert.match(holdHook, /crypto\.randomUUID/);
assert.match(holdHook, /CHECK_IN_CONTEXT_CLEARED/);
assert.match(holdHook, /remainingSeconds/);
assert.match(holdHook, /serverClockOffsetMs/);
assert.match(holdHook, /expiredRequestKeyRef\.current === requestKey/);
assert.match(operationPage, /ocrStatus === "recognizing"/);
assert.match(operationPage, /normalizedPlate !== committedPlate/);
assert.match(operationPage, /event\.parkingLotId !== parkingLotId/);
assert.match(operationPage, /event\.zoneId !== checkInPreview\.zoneId/);
assert.match(operationPage, /error\.status === 409/);
assert.match(operationPage, /spaceHoldId: checkInSpaceHold\?\.holdId/);
assert.doesNotMatch(operationPage, /ParkingSpaceSelectionDrawer|selectedParkingSpaceId|onChooseSpace/);
assert.match(operationPage, /Vui lòng quẹt hoặc quét lại thẻ/);
assert.match(operationPage, /invoice\?\.status === "PAID"/);
assert.match(operationPage, /label: vehicleType\.name\?\.trim\(\)/);
assert.match(operationPage, /DEFAULT_CHECK_IN_VEHICLE_TYPE_KEY/);
assert.match(operationPage, /localStorage\.setItem\(DEFAULT_CHECK_IN_VEHICLE_TYPE_KEY, vehicleTypeId\)/);
assert.match(operationPage, /const checkInVehicleTypeDisabled = isQrOperation \|\| selectedIsRegisteredCard/);
assert.match(operationPage, /setVehicleTypeId\(resolvedDefaultCheckInVehicleTypeId\)/);
assert.match(operationPage, /<CheckInVehicleTypeSelect[\s\S]*?onSetDefault=\{handleSetDefaultCheckInVehicleType\}/);
assert.match(operationPage, /getOptionAction=\{\(option\) =>/);
assert.match(operationPage, /<span className="tw-whitespace-nowrap">Mặc định<\/span>/);
assert.doesNotMatch(operationPage, /aria-pressed=\{isDefault\}/);
const checkInVehicleTypeSelect = operationPage.match(/function CheckInVehicleTypeSelect[\s\S]*?export function SwipeListPage/)?.[0] ?? "";
assert.match(checkInVehicleTypeSelect, />\s*Loại xe\s*<\/span>[\s\S]*?<SelectMenu/);
assert.doesNotMatch(checkInVehicleTypeSelect, /fas fa-car/);
assert.match(operationPage, /showVehicleTypeField=\{!isQrOperation && mode === "check-out"\}/);
assert.match(operationPage, /durationMs: 9000/);
assert.doesNotMatch(operationPage, /VehicleMatchStatus|vehicleMatchStatus/);
assert.match(operationPage, /setVehicleImage\(null\)/);
assert.match(operationPage, /<header className=/);
assert.match(operationPage, /onClick=\{\(\) => void handleOpenUserGuide\(\)\}[\s\S]*?Hướng dẫn & Trợ giúp/);
assert.doesNotMatch(operationPage, /error=\{submitError \|\| parkingLotError/);
const pendingStorageType = operationPage.match(/type PendingVnpayCheckOut = \{[\s\S]*?\n\};/)?.[0] ?? "";
assert.match(pendingStorageType, /invoiceId: string/);
assert.match(pendingStorageType, /parkingSessionId: string/);
assert.doesNotMatch(pendingStorageType, /cardUid|licensePlate|result:/);
assert.match(operationForm, /ValidationChipState = "idle" \| "loading" \| "valid" \| "invalid" \| "stale"/);
assert.match(operationForm, />Làn xe<\/span>/);
assert.match(operationForm, /!isCheckIn \? \([\s\S]*?aria-label="Loại xe check-out"[\s\S]*?readOnly/);
assert.match(operationForm, /const selectedVehicleTypeLabel = vehicleTypeId/);
assert.match(locationPanel, /Ô được hệ thống tạm giữ/);
assert.doesNotMatch(operationPage, /CheckInAssignmentResult/);
assert.match(operationPage, /"Check-in thành công"/);
assert.match(operationPage, /Xe \$\{checkedInPlate\} đã được gán vào ô \$\{response\.data\.parkingSpaceCode\}/);
assert.match(summary, /resolvedTrackingMode !== "CAPACITY"/);
assert.doesNotMatch(summary, /label="Tầng"|label="Chế độ theo dõi"/);
assert.match(summary, /function LicensePlateHighlight/);
assert.match(summary, /tw-border-brand-200 tw-bg-brand-50[\s\S]*?Biển số/);
assert.match(summary, /Ảnh biển số khi vào/);
assert.match(summary, /Xe \/ phương tiện/);
assert.match(summary, /Hình ảnh khi vào/);
assert.match(summary, /đối chiếu với hình ảnh camera hiện tại/);
assert.doesNotMatch(summary, /Khi ra|Kết quả đối chiếu ảnh xe|Ảnh xe khi ra/);
assert.doesNotMatch(summary, /Chưa có dữ liệu/);
assert.match(cameraPanel, /capture\.url[\s\S]*?tw-object-contain tw-object-center/);
assert.match(cameraPanel, /visibleRestoredImageUrl[\s\S]*?tw-object-contain tw-object-center/);
assert.match(cameraPanel, /Ảnh xe \/ phương tiện/);
assert.match(cameraPanel, /scene="vehicle"/);
assert.match(cameraPanel, /vehicleCameraRef\.current\?\.startCamera\(\)/);
assert.match(cameraPanel, /vehicleCameraRef\.current\?\.clear\(true\)/);
assert.match(cameraPanel, /mode === "check-in"[\s\S]*?tw-grid-cols-2 tw-gap-3/);
assert.match(cameraPanel, /mode === "check-in"[\s\S]*?tw-grid tw-grid-cols-3/);
assert.match(operationPage, /tw-max-w-\[440px\][\s\S]*?<ParkingSessionSummary/);
assert.match(selectMenu, /getOptionAction\?: \(option: SelectMenuOption\)/);
assert.match(toastProvider, /durationMs\?: number/);
assert.match(toastProvider, /input\.durationMs \?\? 4200/);
assert.match(apiMessages, /Detected license plate does not match check-in license plate/);
assert.match(apiMessages, /Biển số nhận diện khi xe ra không khớp với biển số đã ghi nhận lúc check-in/);
const transpiledApiMessages = ts.transpileModule(apiMessages, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText;
const apiMessageModule = { exports: {} };
vm.runInNewContext(transpiledApiMessages, { exports: apiMessageModule.exports, module: apiMessageModule });
assert.equal(
  apiMessageModule.exports.localizeApiMessage(
    "Detected license plate does not match check-in license plate",
    409,
  ),
  "Biển số nhận diện khi xe ra không khớp với biển số đã ghi nhận lúc check-in. Vui lòng kiểm tra lại.",
);
assert.match(routes, /to="\/admin\/swipe\?mode=check-in"/);
assert.match(routes, /to="\/admin\/swipe\?mode=check-out"/);

console.log("Parking check-in preview, automatic space hold, realtime, payment and legacy-route safeguards passed.");
