import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");
const structure = read("../src/features/parking/pages/ParkingLotStructurePage.tsx");
const layoutApi = read("../src/features/parking/api/parkingLayoutApi.ts");
const topologyApi = read("../src/features/parking/api/parkingTopologyApi.ts");
const lotWizard = read("../src/features/parking/components/ParkingLotCreateWizard.tsx");
const lotInfoForm = read("../src/features/parking/components/ParkingLotInfoForm.tsx");
const lotEditModal = read("../src/features/parking/components/ParkingLotEditModal.tsx");
const managerAssignmentModal = read("../src/features/parking/components/ParkingManagerAssignmentModal.tsx");
const workspaceHeader = read("../src/features/parking/components/ParkingLotWorkspaceHeader.tsx");

assert.match(layoutApi, /export function deleteParkingLevel\(levelId: string\)/);
assert.match(layoutApi, /`\/parking-levels\/\$\{levelId\}`[\s\S]*method: "DELETE"/);
assert.ok(structure.includes("deleteParkingLot"), "Thiếu API xóa bãi");
assert.ok(structure.includes("deleteParkingLevel"), "Thiếu API xóa tầng");
assert.ok(structure.includes("Xóa bãi"), "Thiếu nút xóa bãi");
assert.ok(structure.includes("Xóa tầng"), "Thiếu nút xóa tầng");
assert.ok(structure.includes("Không thể xóa: đang có ${lvZones.length} khu vực"), "Thiếu guard tầng còn khu vực");
assert.ok(structure.includes('icon="fas fa-door-open"'), "Icon KPI Cổng không hợp lệ");
assert.ok(!structure.includes('icon="far fa-door-closed"'), "Vẫn còn icon Cổng không tồn tại");
assert.ok(structure.includes("activeZoneCount > 0"), "Thiếu guard bãi còn khu vực hoạt động");

// Phase 2: không còn mock, KPI thật, data flow sạch.
for (const mock of ["MOCK_LEVELS", "MOCK_ZONES", "MOCK_GATES", "MOCK_LANES"]) {
  assert.ok(!structure.includes(mock), `Vẫn còn dữ liệu mock ${mock} trên tab Cấu trúc`);
}
assert.ok(!structure.includes("Math.max(zones.length"), "KPI khu vực vẫn dùng số liệu giả");
assert.ok(!structure.includes("Math.max(gates.length"), "KPI cổng vẫn dùng số liệu giả");
assert.ok(!structure.includes("Math.max(lanes.length"), "KPI làn vẫn dùng số liệu giả");
assert.ok(!structure.includes(".catch(() => null)"), "Data flow vẫn nuốt lỗi bằng .catch(() => null)");
assert.ok(structure.includes("reloadTopology"), "Thiếu hàm tải lại topology rõ ràng");
assert.ok(structure.includes("Thử lại"), "Thiếu nút retry khi tải topology lỗi");
assert.ok(structure.includes("chưa có tầng"), "Thiếu empty state khi chưa có tầng");
assert.ok(structure.includes("chưa có khu vực"), "Thiếu empty state tầng chưa có khu");
assert.ok(structure.includes("chưa có cổng"), "Thiếu empty state khu chưa có cổng");
assert.ok(structure.includes("chưa có làn"), "Thiếu empty state cổng chưa có làn");

// Phase 3: CRUD thật qua modal dùng chung, panel động, không điều hướng giả.
for (const fn of ["createZone", "updateZone", "deleteZone", "getZoneById"]) {
  assert.ok(topologyApi.includes(`export function ${fn}`), `Thiếu frontend API ${fn}`);
  assert.ok(structure.includes(fn), `Tab Cấu trúc chưa gọi ${fn}`);
}
for (const fn of ["createGate", "updateGate", "deleteGate", "getGateById"]) {
  assert.ok(topologyApi.includes(`export function ${fn}`), `Thiếu frontend API ${fn}`);
  assert.ok(structure.includes(fn), `Tab Cấu trúc chưa gọi ${fn}`);
}
for (const fn of ["createLane", "updateLane", "deleteLane", "getLaneById"]) {
  assert.ok(topologyApi.includes(`export function ${fn}`), `Thiếu frontend API ${fn}`);
  assert.ok(structure.includes(fn), `Tab Cấu trúc chưa gọi ${fn}`);
}
for (const fn of ["activateZone", "markZoneMaintenance", "closeZone", "activateGate", "markGateMaintenance", "closeGate", "activateLane", "markLaneMaintenance", "closeLane"]) {
  assert.ok(structure.includes(fn), `Tab Cấu trúc chưa tái dùng hàm trạng thái ${fn}`);
}
assert.ok(structure.includes("ConfirmDialog"), "Thiếu ConfirmDialog cho thao tác đóng");
assert.ok(!structure.includes("window.confirm"), "Vẫn dùng window.confirm");
assert.ok(!structure.includes("window.prompt"), "Vẫn dùng window.prompt");
assert.ok(!/[^.]alert\(/.test(structure.replace("fa-exclamation-triangle", "")), "Vẫn dùng alert()");
assert.ok(structure.includes("Thêm khu vực") || structure.includes("Thêm khu"), "Thiếu modal thêm khu");
assert.ok(structure.includes("Thêm cổng"), "Thiếu modal thêm cổng");
assert.ok(structure.includes("Thêm làn"), "Thiếu modal thêm làn");
assert.ok(structure.includes("Chi tiết tầng"), "Panel chi tiết thiếu loại tầng");
assert.ok(structure.includes("Chi tiết khu vực"), "Panel chi tiết thiếu loại khu vực");
assert.ok(structure.includes("Chi tiết cổng"), "Panel chi tiết thiếu loại cổng");
assert.ok(structure.includes("Chi tiết làn"), "Panel chi tiết thiếu loại làn");
assert.ok(structure.includes("PARKING_SPACE_CONFIGURE_ALL"), "Thiếu phân quyền mutation theo PARKING_SPACE_CONFIGURE_ALL");
assert.ok(structure.includes("RowMenu"), "Nút ba chấm chưa có menu thao tác thật");

console.log("Parking structure delete actions and icons passed.");
console.log("Parking structure CRUD, mock removal and detail panels passed.");

// Thêm bãi: wizard 3 bước tái dùng form địa chỉ/bản đồ + phân công Manager.
assert.ok(structure.includes("Thêm bãi"), "Thiếu nút Thêm bãi ở tab Cấu trúc");
assert.ok(structure.includes("setLotWizardOpen(true)"), "Nút Thêm bãi chưa mở wizard");
assert.ok(structure.includes("ParkingLotCreateWizard"), "Tab Cấu trúc chưa gắn wizard tạo bãi");
assert.ok(structure.includes("?lotId=${lot.parkingLotId}"), "Tạo bãi xong chưa điều hướng sang bãi mới");
assert.ok(lotWizard.includes("Bước {step}/3"), "Wizard thiếu chỉ báo 3 bước");
assert.ok(lotWizard.includes("createParkingLot"), "Wizard chưa gọi createParkingLot");
assert.ok(lotWizard.includes("createParkingLevel"), "Wizard chưa tự tạo tầng GROUND/L");
assert.ok(lotWizard.includes("assignParkingManagerToParkingLot"), "Wizard thiếu bước phân công Manager");
assert.ok(lotWizard.includes("getProvisionedAccounts"), "Wizard chưa tải danh sách Manager");
assert.ok(lotWizard.includes("Tầng trệt"), "Wizard thiếu mặc định tầng GROUND");
assert.ok(!lotWizard.includes("<select"), "Wizard vẫn dùng select HTML thuần thay vì SelectMenu");
assert.ok(lotWizard.includes("PARKING_LOT_CREATE_ALL") || structure.includes("PARKING_LOT_CREATE_ALL"), "Thiếu phân quyền tạo bãi");
assert.ok(lotWizard.includes("ORGANIZATION_MEMBERSHIP_MANAGE_ALL"), "Thiếu phân quyền phân công Manager");
assert.ok(lotInfoForm.includes("ParkingAddressPicker"), "Form bãi dùng chung thiếu chọn địa chỉ");
assert.ok(lotInfoForm.includes("ParkingLocationMap"), "Form bãi dùng chung thiếu bản đồ");
assert.ok(lotInfoForm.includes("searchParkingLocations"), "Form bãi dùng chung thiếu tìm tọa độ");
assert.ok(lotInfoForm.includes("reverseParkingLocation"), "Form bãi dùng chung thiếu reverse geocode");
assert.ok(lotInfoForm.includes("Xác nhận vị trí thủ công"), "Form bãi dùng chung thiếu xác nhận thủ công");
assert.ok(structure.includes("Thêm tầng mới (cùng bãi)"), "Panel tầng thiếu CTA thêm tầng cho bãi hiện có");
assert.ok(structure.includes("Thêm tầng đầu tiên"), "Empty state thiếu CTA thêm tầng đầu tiên");
assert.ok(structure.includes("createParkingLevel"), "Tab Cấu trúc chưa gọi createParkingLevel");
assert.ok(structure.includes("ParkingLotWorkspaceHeader"), "Tab Cấu trúc chưa dùng header bãi xe thống nhất");
assert.ok(structure.includes("encodeURIComponent(parkingLotId)"), "Chuyển bãi chưa giữ parkingLotId đang chọn");
assert.ok(structure.includes('searchParams.get("action")'), "Tab Cấu trúc chưa xử lý action từ Tổng quan");
assert.ok(structure.includes("ParkingLotEditModal"), "Tab Cấu trúc chưa gắn modal sửa bãi mới");
assert.ok(structure.includes("ParkingManagerAssignmentModal"), "Tab Cấu trúc chưa gắn modal gán Manager mới");
assert.ok(lotEditModal.includes("updateParkingLot"), "Modal sửa bãi chưa gọi API cập nhật");
assert.ok(managerAssignmentModal.includes("assignParkingManagerToParkingLot"), "Modal Manager chưa gọi API phân công");
assert.ok(managerAssignmentModal.includes("getAssignedParkingManagers"), "Modal Manager chưa đọc phân công hiện tại");
assert.ok(managerAssignmentModal.includes("unassignParkingManagerFromParkingLot"), "Modal Manager thiếu thao tác gỡ khỏi bãi");
assert.ok(managerAssignmentModal.includes("Gỡ khỏi bãi"), "Modal Manager thiếu nút gỡ phân công");
assert.ok(managerAssignmentModal.includes("Thử lại"), "Modal Manager thiếu retry tải danh sách");
assert.ok(workspaceHeader.includes('ariaLabel="Chọn bãi xe"'), "Header chung thiếu dropdown bãi");
assert.ok(!lotWizard.includes('>Để sau</Button>'), "Wizard vẫn cho bỏ qua toàn bộ bước tạo tầng");
assert.ok(lotWizard.includes("Number.isInteger"), "Wizard thiếu validation số tầng nguyên 1-50");

console.log("Parking lot creation wizard and level CTAs passed.");
