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
  getCheckInParkingSpaceId,
  getSuggestedParkingSpaceId,
  isCheckInPreviewAdmissible,
  isSelectedVisitorSpaceEligible,
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
assert.equal(getCheckInParkingSpaceId(capacityVisitor, "space-a"), undefined);
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
assert.equal(isCheckInPreviewAdmissible(spaceVisitor), false);
assert.equal(isSelectedVisitorSpaceEligible(spaceVisitor, "space-a"), true);
assert.equal(isCheckInPreviewAdmissible(spaceVisitor, "space-a"), true);
assert.equal(getCheckInParkingSpaceId(spaceVisitor, "space-a"), "space-a");
assert.equal(isSelectedVisitorSpaceEligible(spaceVisitor, "removed-space"), false);
assert.equal(
  getSuggestedParkingSpaceId({
    ...spaceVisitor,
    eligibleSpaces: [spaceVisitor.eligibleSpaces[0]],
    selectionRequired: false,
  }),
  "space-a",
);

const spaceSubscription = {
  ...spaceVisitor,
  customerType: "SUBSCRIPTION",
  eligibleSpaces: [{ code: "S-01", parkingSpaceId: "space-s", status: "RESERVED" }],
  recommendedParkingSpaceId: "space-s",
  selectionRequired: false,
};
assert.equal(isCheckInPreviewAdmissible(spaceSubscription), true);
assert.equal(getCheckInParkingSpaceId(spaceSubscription, "space-s"), undefined);
assert.equal(isCheckInPreviewAdmissible({ ...spaceSubscription, eligibleSpaces: [] }), false);

const api = read("../src/features/parking/api/parkingSessionApi.ts");
const hook = read("../src/features/parking/hooks/useCheckInPreview.ts");
const operationPage = read("../src/features/parking/pages/SwipeListPage.tsx");
const operationForm = read("../src/features/parking/components/ParkingOperationForm.tsx");
const drawer = read("../src/features/parking/components/ParkingSpaceSelectionDrawer.tsx");
const summary = read("../src/features/parking/components/ParkingSessionSummary.tsx");
const routes = read("../src/app/routes.tsx");

assert.match(api, /\/check-in\/preview/);
assert.match(api, /parkingSpaceId\?: string/);
assert.match(hook, /AbortController/);
assert.match(hook, /requestSequenceRef/);
assert.match(operationPage, /ocrStatus === "recognizing"/);
assert.match(operationPage, /normalizedPlate !== committedPlate/);
assert.match(operationPage, /event\.parkingLotId !== parkingLotId/);
assert.match(operationPage, /event\.zoneId !== checkInPreview\.zoneId/);
assert.match(operationPage, /error\.status === 409/);
assert.match(operationPage, /invoice\?\.status === "PAID"/);
const pendingStorageType = operationPage.match(/type PendingVnpayCheckOut = \{[\s\S]*?\n\};/)?.[0] ?? "";
assert.match(pendingStorageType, /invoiceId: string/);
assert.match(pendingStorageType, /parkingSessionId: string/);
assert.doesNotMatch(pendingStorageType, /cardUid|licensePlate|result:/);
assert.match(operationForm, /ValidationChipState = "idle" \| "loading" \| "valid" \| "invalid" \| "stale"/);
assert.match(drawer, /space\.status === "AVAILABLE"/);
assert.match(summary, /resolvedTrackingMode !== "CAPACITY"/);
assert.match(routes, /to="\/admin\/swipe\?mode=check-in"/);
assert.match(routes, /to="\/admin\/swipe\?mode=check-out"/);

console.log("Parking check-in preview, space selection, realtime, payment and legacy-route safeguards passed.");
