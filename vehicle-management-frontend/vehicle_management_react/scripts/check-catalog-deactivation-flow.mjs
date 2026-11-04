import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const pagePath = new URL("../src/features/catalog/pages/CatalogListPages.tsx", import.meta.url);
const apiPath = new URL("../src/features/catalog/api/vehicleTypesApi.ts", import.meta.url);
const messagesPath = new URL("../src/core/api/apiMessage.ts", import.meta.url);
const lotAvailabilityApiPath = new URL("../src/features/catalog/api/parkingLotCatalogAvailabilityApi.ts", import.meta.url);
const lotControlsPath = new URL("../src/features/catalog/components/ParkingLotCatalogControls.tsx", import.meta.url);

const [page, api, messages, lotAvailabilityApi, lotControls] = await Promise.all([
  readFile(pagePath, "utf8"),
  readFile(apiPath, "utf8"),
  readFile(messagesPath, "utf8"),
  readFile(lotAvailabilityApiPath, "utf8"),
  readFile(lotControlsPath, "utf8"),
]);

assert.match(api, /getVehicleTypeDeactivationImpact/);
assert.match(api, /deactivation-impact/);
assert.match(api, /cascadePriceRules/);
assert.match(page, /Đang kiểm tra dữ liệu liên quan/);
assert.match(page, /Xác nhận ngừng dùng/);
assert.match(page, /Phạm vi áp dụng/);
assert.match(page, /Tất cả bãi thuộc Partner/);
assert.match(page, /Chọn bãi áp dụng/);
assert.match(page, /Manager chỉ được áp dụng tại bãi đang được phân công/);
assert.match(page, /Manager phải chọn một trong các bãi đang được phân công/);
assert.match(page, /deactivationImpact\.activeSubscriptionCount/);
assert.match(page, /Không có bản ghi nào bị xóa/);
assert.match(page, /khu vực không còn loại xe nào sẽ chuyển sang bảo trì/);
assert.match(page, /updateVehicleAvailabilityAtLot/);
assert.match(page, /updateVehicleAvailabilityForAllLots/);
assert.match(page, /excludedVehicleTypeIds\.includes\(row\.id\)/);
assert.match(page, /Đã kích hoạt lại/);
assert.match(page, /toast\.success/);
assert.doesNotMatch(page, />Ngừng dùng toàn Partner</);
assert.match(page, /Dùng thao tác Kích hoạt lại hoặc Ngừng dùng bên ngoài biểu mẫu/);
assert.match(messages, /Vehicle type still has open parking sessions/);
assert.match(messages, /Vehicle type still has active or paid subscriptions/);
assert.match(page, /VEHICLE_TYPE_UPDATE_ALL/);
assert.match(lotAvailabilityApi, /drainingVehicleTypeIds/);
assert.match(lotAvailabilityApi, /setParkingLotVehicleTypeEnabledForLots/);
assert.match(lotControls, /Đang ngừng dần/);
assert.match(lotControls, /Tất cả bãi thuộc Partner/);
assert.match(lotControls, /Bãi được phân công đang chọn/);
assert.match(lotControls, /Áp dụng một phần/);
assert.match(lotControls, /vé còn hiệu lực và phiên đang gửi/);

console.log("Scoped vehicle-service deactivation UI and impact safeguards passed.");
