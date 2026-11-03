import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const root = new URL("../", import.meta.url);
const [api, embeddedSection, adminPage, pricingPage, historyPage, routes, permissions, navigation, endpoints] = await Promise.all([
  readFile(new URL("src/features/parking-reviews/api/parkingLotReviewApi.ts", root), "utf8"),
  readFile(new URL("src/features/parking-reviews/components/ParkingLotReviewsSection.tsx", root), "utf8"),
  readFile(new URL("src/features/parking-reviews/pages/ParkingLotReviewManagementPage.tsx", root), "utf8"),
  readFile(new URL("src/features/customer-portal/pages/PricingPage.tsx", root), "utf8"),
  readFile(new URL("src/features/customer-portal/pages/CustomerHistoryPage.tsx", root), "utf8"),
  readFile(new URL("src/app/routes.tsx", root), "utf8"),
  readFile(new URL("src/app/routePermissions.ts", root), "utf8"),
  readFile(new URL("src/config/navigation.ts", root), "utf8"),
  readFile(new URL("src/core/api/apiEndpoints.ts", root), "utf8"),
]);

assert.match(endpoints, /parkingLotReviews: \(parkingLotId: string\)/);
assert.match(endpoints, /myParkingLotReview: \(parkingLotId: string\)/);
assert.match(api, /skipAuth: true/);
assert.match(api, /method: "PUT"/);
assert.match(embeddedSection, /id="parking-reviews"/);
assert.match(embeddedSection, /getPublicParkingLotReviews/);
assert.match(embeddedSection, /getMyParkingLotReview/);
assert.match(embeddedSection, /upsertMyParkingLotReview/);
assert.match(embeddedSection, /Phân bố số sao/);
assert.match(embeddedSection, /NO_ELIGIBLE_TRANSACTION/);
assert.match(embeddedSection, /maxLength=\{2000\}/);
assert.match(embeddedSection, /<Modal/);
assert.match(embeddedSection, /Sửa đánh giá/);
assert.match(embeddedSection, /Viết đánh giá/);
assert.match(adminPage, /getManagedParkingLotReviews/);
assert.match(adminPage, /<SelectMenu/);
assert.match(adminPage, /<DateRangeInput/);
assert.match(adminPage, /<PaginationFooter/);
assert.match(adminPage, /Hành động đề xuất/);
assert.match(adminPage, /Cơ cấu phản hồi/);
assert.match(adminPage, /createAndOpenCustomerSupportConversation/);
assert.match(adminPage, /Chat với/);
assert.doesNotMatch(adminPage, /<select/);
assert.match(pricingPage, /lot\.averageRating\?\.toFixed\(1\)/);
assert.match(pricingPage, /lot\.reviewCount\} đánh giá/);
assert.match(pricingPage, /href="#parking-reviews"/);
assert.match(pricingPage, /scrollIntoView\(\{ behavior: "smooth", block: "start" \}\)/);
assert.match(pricingPage, /<ParkingLotReviewsSection/);
assert.match(historyPage, /selectedSession\.status === "CLOSED"/);
assert.match(historyPage, /selectedSession\.parkingLotId\}#write-review/);
assert.doesNotMatch(historyPage, /selectedSession\.parkingLotId\}\/reviews/);
assert.doesNotMatch(routes, /path: "\/pricing\/:parkingLotId\/reviews"/);
assert.match(routes, /path: "\/admin\/parking-reviews"/);
assert.match(permissions, /"\/admin\/parking-reviews": \["PARKING_LOT_REVIEW_READ_ALL"\]/);
assert.match(navigation, /Đánh giá bãi xe/);

console.log("Parking-lot review API, embedded modal flow, scoped admin route, pricing card, and history CTA checks passed.");
