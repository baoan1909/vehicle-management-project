import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const editorPath = new URL("../src/features/parking/pages/ParkingLayoutEditorPage.tsx", import.meta.url);
const livePath = new URL("../src/features/parking/pages/ParkingLiveMapPage.tsx", import.meta.url);
const apiPath = new URL("../src/features/parking/api/parkingLayoutApi.ts", import.meta.url);
const canvasPath = new URL("../src/features/parking/components/LayoutCanvas.tsx", import.meta.url);
const liveMapPath = new URL("../src/features/parking/components/LiveOpsMap.tsx", import.meta.url);
const validationPath = new URL("../src/features/parking/utils/layoutValidation.ts", import.meta.url);
const realtimePath = new URL("../src/features/parking/api/parkingRealtime.ts", import.meta.url);
const routesPath = new URL("../src/app/routes.tsx", import.meta.url);
const permissionsPath = new URL("../src/app/routePermissions.ts", import.meta.url);
const navigationPath = new URL("../src/config/navigation.ts", import.meta.url);
const propertyPanelPath = new URL("../src/features/parking/components/LayoutPropertyPanel.tsx", import.meta.url);
const defaultLayoutPath = new URL("../src/features/parking/utils/defaultParkingLayout.ts", import.meta.url);

const [editor, live, api, canvas, liveMap, validation, realtime, routes, permissions, navigation, propertyPanel, defaultLayout] = await Promise.all([
  readFile(editorPath, "utf8"),
  readFile(livePath, "utf8"),
  readFile(apiPath, "utf8"),
  readFile(canvasPath, "utf8"),
  readFile(liveMapPath, "utf8"),
  readFile(validationPath, "utf8"),
  readFile(realtimePath, "utf8"),
  readFile(routesPath, "utf8"),
  readFile(permissionsPath, "utf8"),
  readFile(navigationPath, "utf8"),
  readFile(propertyPanelPath, "utf8"),
  readFile(defaultLayoutPath, "utf8"),
]);

// Draft workflow uses optimistic locking and never auto-overwrites.
assert.match(editor, /expectedDraftRevision/);
assert.match(editor, /Tải bản mới/);
assert.match(editor, /Sao chép dữ liệu local/);
assert.match(api, /publish-preview/);
assert.match(api, /expectedDraftRevision=/);
assert.match(editor, /dirty \? await handleSave\(\) : draft/);
assert.match(editor, /publishDraftRevision/);
assert.match(api, /elements\?: .*\| null/);
assert.doesNotMatch(canvas, /react-konva|fabric|konva/);

// Live map is read-only and refetches snapshots on realtime events.
assert.match(live, /useParkingRealtime/);
assert.match(live, /onReconnect/);
assert.doesNotMatch(live, /window\.prompt|window\.confirm/);
assert.match(live, /title="Gán chỗ đăng ký"/);
assert.match(live, /PARKING_SPACE_ASSIGN_ALL/);
assert.match(liveMap, /<LayoutCanvas/);
assert.match(liveMap, /readOnly/);
assert.match(live, /getPublishedLayout/);
assert.match(live, /parkingSpaceId === space\.parkingSpaceId/);
assert.match(live, /layout\.elements\.forEach/);
assert.match(live, /levelMap\.level\.canvasWidth/);
assert.match(live, /levelMap\.level\.canvasHeight/);
assert.match(live, /event\.eventType === "PARKING_LAYOUT_PUBLISHED"/);
assert.match(live, /space\.parkingSpaceId === event\.parkingSpaceId/);
assert.doesNotMatch(live, /rowTop|rowMiddle|rowBottom|SpotButton|ColumnBlock|Hatch/);
assert.match(canvas, /spaceVisuals/);
assert.match(canvas, /ALLOCATED_SPACE_VISUAL/);
assert.match(realtime, /\/topic\/parking-lots\//);
assert.match(realtime, /reconnectDelay/);

// Validation runs on the client but the backend stays authoritative.
assert.match(validation, /rectanglesOverlap/);
assert.match(validation, /isSafeImageSource/);
assert.match(validation, /javascript:/);
assert.match(editor, /Còn .* vấn đề/);

// Status is never color-only and draft data never leaks to public views.
assert.match(liveMap, /glyph/);
assert.match(liveMap, /aria-label/);
assert.match(api, /space-availability/);
assert.match(api, /skipAuth: true/);

// Routes and permissions keep System Admin read-only.
assert.match(routes, /\/admin\/parking-layouts/);
assert.match(routes, /\/admin\/parking-live/);
assert.match(permissions, /\/admin\/parking-layouts/);
assert.match(permissions, /PARKING_SPACE_READ_ALL/);
assert.match(editor, /PARKING_SPACE_CONFIGURE_ALL/);
assert.match(editor, /PARKING_LAYOUT_PUBLISH_ALL/);
assert.match(canvas, /readOnly\?: boolean/);
assert.match(editor, /readOnly=\{!canConfigure\}/);
assert.match(editor, /setDraft\(null\)/);
assert.match(editor, /getPublishedLayout/);

// The API base already ends in /api; feature paths must never create /api/api.
assert.doesNotMatch(api, /[`"]\/api\//);
assert.match(api, /createParkingLevel/);
assert.match(editor, /Tạo GROUND mặc định/);
assert.match(editor, /ParkingLotWorkspaceHeader[\s\S]*?activeTab="layout"/);
assert.match(editor, /Quản lý tầng/);
assert.match(propertyPanel, /Thuộc tính ô đỗ/);
assert.match(editor, /Không có lỗi bố cục/);
assert.match(api, /updateParkingLevel/);
assert.match(canvas, /touchAction: "none"/);
assert.match(canvas, /tw-min-h-\[590px\] tw-flex-1/);
assert.doesNotMatch(canvas, /height: 590/);
assert.match(editor, /tw-h-\[650px\] tw-grid-cols/);
assert.match(editor, /tw-overflow-y-auto/);
assert.match(canvas, /fa-magnet/);
assert.match(canvas, /createPortal/);
assert.match(canvas, /toolbarTarget/);
assert.match(canvas, /resize-element/);
assert.match(canvas, /rotate-element/);
for (const handle of ["nw", "n", "ne", "e", "se", "s", "sw", "w"]) {
  assert.match(canvas, new RegExp(`handle: "${handle}"`));
}
assert.match(canvas, /beginSpaceResize/);
assert.match(canvas, /beginElementResize/);
assert.match(canvas, /elementResizeHandles\.map/);
assert.match(canvas, /handle: ResizeHandle/);
assert.match(canvas, /SPACE_MIN_WIDTH/);
assert.match(canvas, /SPACE_MIN_HEIGHT/);
assert.match(propertyPanel, /Xoay \(°\)/);
assert.match(propertyPanel, /Đóng thuộc tính/);
assert.match(editor, /propertyDrawerOpen/);
assert.match(editor, /translateX\(100%\)/);
assert.match(editor, /visibility: propertyDrawerOpen/);
assert.match(editor, /toolbarTarget=\{toolbarTarget\}/);
assert.match(canvas, /openProperties: true/);
assert.match(editor, /options\?\.openProperties/);
assert.match(canvas, /event\.ctrlKey \|\| !event\.altKey/);
assert.match(canvas, /passive: false/);
assert.match(canvas, /event\.preventDefault\(\)/);
assert.match(canvas, /event\.stopPropagation\(\)/);
assert.match(canvas, /spacePressedRef/);
assert.match(canvas, /application\/x-parking-layout-tool/);
assert.match(canvas, /toCanvas/);
assert.match(canvas, /setZoom\(1\)/);
assert.match(canvas, /constrainPan/);
assert.match(canvas, /const maxX = Math\.max\(0, visibleWidth - canvasWidth\)/);
assert.match(canvas, /Math\.min\(maxX, Math\.max\(minX/);
assert.match(canvas, /viewportRef\.current\?\.clientWidth/);
assert.match(canvas, /setPan\(\{ x: 0, y: 0 \}\)/);
assert.doesNotMatch(canvas, /useEffect\(\(\) => \{\s*fitToScreen\(\)/);
assert.match(editor, /onPaletteDrop/);
assert.match(editor, /createDefaultParkingLayout/);
assert.match(editor, /applyDefaultTemplate/);
assert.match(editor, /Sơ đồ mặc định/);
assert.match(editor, /Mẫu gốc luôn được giữ nguyên/);
assert.match(editor, /defaultSpaceCount/);
assert.match(editor, /defaultVehicleTypeId/);
assert.match(editor, /defaultTemplateOpen/);
assert.match(editor, /Tạo sơ đồ từ mẫu mặc định/);
assert.match(editor, /SelectMenu/);
assert.doesNotMatch(editor, /<select/);
assert.doesNotMatch(propertyPanel, /<select/);
assert.match(editor, /max=\{28\}/);
assert.match(defaultLayout, /Math\.min\(28/);
assert.match(defaultLayout, /x: 40, y: 190, width: 920, height: 180/);
assert.match(defaultLayout, /x, y: 170, width: 20, height: 20/);
assert.match(defaultLayout, /x, y: 370, width: 20, height: 20/);
assert.match(defaultLayout, /width: 60, height: 28, rotation: 0/);
assert.doesNotMatch(defaultLayout, /y: 172, width: 920, height: 10/);
assert.doesNotMatch(defaultLayout, /y: 445, width: (390|420), height: 10/);
assert.match(canvas, /isMotorcycle/);
assert.match(canvas, /LANE_ARROW/);
assert.match(canvas, /LANE_CROSSWALK/);
assert.match(canvas, /variant === "ARROW"/);
assert.match(canvas, /variant === "CROSSWALK"/);
assert.match(canvas, /Ctrl\+Z/);
assert.match(canvas, /Ctrl\+Shift\+Z/);
assert.match(canvas, /centerDiagram/);
assert.match(canvas, /resetViewportScroll/);
assert.match(canvas, /scrollTo\(\{ left: 0, top: 0, behavior: "auto" \}\)/);
assert.match(canvas, /const targetZoom = Math\.min/);
assert.match(canvas, /contentWidth/);
assert.match(editor, /undoStackRef/);
assert.match(editor, /redoStackRef/);
assert.match(defaultLayout, /row\(layoutVersionId, 1, 14/);
assert.match(defaultLayout, /row\(layoutVersionId, 15, 7/);
assert.match(defaultLayout, /row\(layoutVersionId, 22, 7/);
for (const elementType of ["BACKGROUND", "DRIVE_LANE", "WALL", "COLUMN", "ENTRY", "EXIT"]) {
  assert.match(defaultLayout, new RegExp(`element\\("${elementType}"`));
}
assert.match(canvas, /LỐI VÀO/);
assert.match(canvas, /LỐI RA/);
assert.match(canvas, /fontWeight=\{700\}/);
assert.match(canvas, /stroke="none"/);
assert.doesNotMatch(canvas, /fontWeight=\{900\}/);
assert.match(canvas, /Inter, ui-sans-serif, system-ui, sans-serif/);
assert.match(liveMap, /STATUS_META/);
assert.match(navigation, /Thiết kế sơ đồ/);
assert.match(navigation, /Sơ đồ trực tiếp/);

console.log("parking layout checks passed");
