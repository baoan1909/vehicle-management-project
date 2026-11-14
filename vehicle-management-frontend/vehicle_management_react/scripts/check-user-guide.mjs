import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const apiPath = new URL("../src/features/user-guide/api/userGuideApi.ts", import.meta.url);
const hostPath = new URL("../src/features/user-guide/components/UserGuideHost.tsx", import.meta.url);
const cardPath = new URL("../src/features/user-guide/components/UserGuideCard.tsx", import.meta.url);
const backendHostPath = new URL("../src/features/user-guide/components/UserGuideBackendHost.tsx", import.meta.url);
const runtimePath = new URL("../src/features/user-guide/hooks/useUserGuideRuntime.ts", import.meta.url);
const adminPath = new URL("../src/features/user-guide/pages/UserGuideManagementPage.tsx", import.meta.url);
const panelPath = new URL("../src/features/user-guide/components/UserGuideVersionPanel.tsx", import.meta.url);
const publicPath = new URL("../src/features/user-guide/pages/UserGuidePublicPage.tsx", import.meta.url);
const contextPath = new URL("../src/features/user-guide/utils/guideContext.ts", import.meta.url);
const endpointsPath = new URL("../src/core/api/apiEndpoints.ts", import.meta.url);
const routesPath = new URL("../src/app/routes.tsx", import.meta.url);
const routerPath = new URL("../src/app/router/index.tsx", import.meta.url);
const permissionsPath = new URL("../src/app/routePermissions.ts", import.meta.url);
const navigationPath = new URL("../src/config/navigation.ts", import.meta.url);
const adminLayoutPath = new URL("../src/shared/components/layout/AdminLayout.tsx", import.meta.url);
const clientLayoutPath = new URL("../src/shared/components/layout/ClientLayout.tsx", import.meta.url);
const backendContextPath = new URL("../src/features/user-guide/context/UserGuideBackendContext.tsx", import.meta.url);
const activationHookPath = new URL("../src/features/user-guide/hooks/useUserGuideActivation.ts", import.meta.url);
const manualLauncherPath = new URL("../src/features/user-guide/hooks/useUserGuideManualLauncher.ts", import.meta.url);
const anchorHookPath = new URL("../src/features/user-guide/hooks/useUserGuideAnchor.ts", import.meta.url);
const swipePath = new URL("../src/features/parking/pages/SwipeListPage.tsx", import.meta.url);
const lostCardPath = new URL("../src/features/cards/pages/LostCardWorkflowPages.tsx", import.meta.url);
const subscriptionPath = new URL("../src/features/catalog/pages/SubscriptionApprovalPage.tsx", import.meta.url);
const pricingPath = new URL("../src/features/pricing/pages/PricingManagePages.tsx", import.meta.url);
const catalogPagePath = new URL("../src/features/catalog/pages/CatalogListPages.tsx", import.meta.url);
const catalogComponentsPath = new URL("../src/features/catalog/components/CatalogManageComponents.tsx", import.meta.url);
const drawerPath = new URL("../src/shared/components/ui/Drawer.tsx", import.meta.url);
const modalPath = new URL("../src/shared/components/ui/Modal.tsx", import.meta.url);
const bodyScrollLockPath = new URL("../src/shared/utils/bodyScrollLock.ts", import.meta.url);
const helpButtonPath = new URL("../src/features/user-guide/components/UserGuideHelpButton.tsx", import.meta.url);
const parkingWorkspaceHeaderPath = new URL("../src/features/parking/components/ParkingLotWorkspaceHeader.tsx", import.meta.url);
const parkingWizardPath = new URL("../src/features/parking/components/ParkingLotCreateWizard.tsx", import.meta.url);
const parkingInfoFormPath = new URL("../src/features/parking/components/ParkingLotInfoForm.tsx", import.meta.url);
const parkingOperationFormPath = new URL("../src/features/parking/components/ParkingOperationForm.tsx", import.meta.url);
const parkingSessionSummaryPath = new URL("../src/features/parking/components/ParkingSessionSummary.tsx", import.meta.url);
const parkingLayoutPath = new URL("../src/features/parking/pages/ParkingLayoutEditorPage.tsx", import.meta.url);
const parkingOverviewPath = new URL("../src/features/parking/pages/ParkingLotOverviewPage.tsx", import.meta.url);
const parkingStructurePath = new URL("../src/features/parking/pages/ParkingLotStructurePage.tsx", import.meta.url);
const businessGuideSeedPath = new URL("../../../vehicle-management-backend/vehicle-management/src/main/resources/db/migration/V20261113120000__seed_business_workflow_user_guide_drafts.sql", import.meta.url);

const [
  api, host, card, backendHost, runtime, admin, panel, publicPage, context, endpoints, routes, router,
  permissions, navigation, adminLayout, clientLayout, backendContext, activationHook, manualLauncher, anchorHook,
  swipe, lostCard, subscription, pricing, catalogPage, catalogComponents, drawer, modal, bodyScrollLock,
  helpButton, parkingWorkspaceHeader, parkingWizard,
  parkingInfoForm, parkingOperationForm, parkingSessionSummary, parkingLayout, parkingOverview, parkingStructure,
  businessGuideSeed,
] = await Promise.all(
  [
    apiPath, hostPath, cardPath, backendHostPath, runtimePath, adminPath, panelPath, publicPath, contextPath,
    endpointsPath, routesPath, routerPath, permissionsPath, navigationPath, adminLayoutPath, clientLayoutPath,
    backendContextPath, activationHookPath, manualLauncherPath, anchorHookPath, swipePath, lostCardPath, subscriptionPath,
    pricingPath, catalogPagePath, catalogComponentsPath, drawerPath, modalPath, bodyScrollLockPath,
    helpButtonPath, parkingWorkspaceHeaderPath, parkingWizardPath,
    parkingInfoFormPath, parkingOperationFormPath, parkingSessionSummaryPath, parkingLayoutPath, parkingOverviewPath,
    parkingStructurePath, businessGuideSeedPath,
  ].map((url) => readFile(url, "utf8")),
);

// Runtime must never trust client-provided identity: no accountId in any payload.
assert.doesNotMatch(api, /accountId/);
assert.match(api, /resolveUserGuides\(featureCode/);
assert.match(api, /getUserGuideStatusCounts/);
assert.match(api, /recordUserGuideEvent\(\s*guideId:\s*string/);
// Runtime user-facing types must carry only signed URLs, never raw object keys.
const stepUserBlock = api.match(/export type UserGuideStepUserResponse = \{[^}]*\}/s)?.[0] ?? "";
assert.match(stepUserBlock, /mediaUrl/);
assert.doesNotMatch(stepUserBlock, /mediaObjectKey/);

// State events must not crash the UI on transient failures.
assert.match(runtime, /catch \{\s*\/\/ Runtime event failures must never crash/);
assert.match(publicPage, /\.catch\(\(\) => undefined\)/);
assert.match(runtime, /requestIdRef/);

// Missing anchor must fall back to a non-anchored surface instead of crashing.
assert.match(host, /isAnchored/);
assert.match(host, /vm-user-guide--modal/);
assert.match(host, /BOTTOM_SHEET/);
assert.match(host, /window\.visualViewport/);
assert.match(host, /collapseMs/);
assert.match(host, /documentHidden/);
assert.match(host, /interactionPaused/);
assert.match(host, /calculateFloatingLayout/);
assert.match(host, /MIN_ANCHORED_CARD_HEIGHT/);
assert.match(host, /ResizeObserver/);
assert.match(host, /CENTER is unsafe for an anchored guide/);
assert.match(host, /\["BOTTOM", "RIGHT", "LEFT", "TOP"\]/);
assert.match(anchorHook, /scrollIntoView/);
assert.match(anchorHook, /MIN_VISIBLE_ANCHOR_SIZE/);
assert.match(anchorHook, /Re-arm automatic positioning/);
assert.match(anchorHook, /positionAnchorForGuide/);
assert.match(anchorHook, /behavior: "auto"/);
assert.match(anchorHook, /ANCHOR_TOP_VIEWPORT_RATIO = 0\.34/);
assert.match(anchorHook, /initialStepPositioned/);
assert.match(anchorHook, /setSnapshot\(null\)/);
assert.match(anchorHook, /querySelectorAll/);
assert.match(anchorHook, /rect\.width > 0/);
assert.match(anchorHook, /attributeFilter: \["class", "hidden", "open", "style"\]/);
assert.match(anchorHook, /ResizeObserver/);

// Every anchor shipped by the business seed must have a concrete FE target.
// This prevents a newly added Next/Previous step from silently falling back to
// an unanchored popup because its data anchor was renamed or removed.
const businessGuideFrontend = [
  swipe, lostCard, subscription, pricing, catalogPage, catalogComponents,
  parkingWizard, parkingInfoForm, parkingOperationForm, parkingSessionSummary,
  parkingLayout, parkingOverview, parkingStructure,
].join("\n");
const seededAnchorKeys = [...businessGuideSeed.matchAll(/"anchor":"([^"]+)"/g)]
  .map((match) => match[1]);
assert.ok(seededAnchorKeys.length > 0, "Business guide seed must define at least one anchor.");
for (const anchorKey of new Set(seededAnchorKeys)) {
  assert.ok(
    businessGuideFrontend.includes(anchorKey),
    `Seeded user-guide anchor is missing from the frontend: ${anchorKey}`,
  );
}

// Nested drawer/modal workflows must not leave document scrolling locked.
assert.match(drawer, /acquireBodyScrollLock/);
assert.match(drawer, /onCloseRef/);
assert.match(modal, /acquireBodyScrollLock/);
assert.match(bodyScrollLock, /activeBodyScrollLocks \+= 1/);
assert.match(bodyScrollLock, /activeBodyScrollLocks !== 0/);

// Accessibility: focus trap + Escape handling + dialog semantics.
assert.match(host, /event\.key !== "Tab"/);
assert.match(host, /Escape/);
assert.match(card, /role="dialog"/);
assert.match(card, /aria-modal/);

// Admin must gate actions by permission; backend remains the enforcer.
assert.match(admin, /USER_GUIDE_CREATE_ALL/);
assert.match(admin, /USER_GUIDE_UPDATE_ALL/);
assert.match(admin, /USER_GUIDE_PUBLISH_ALL/);
assert.match(admin, /USER_GUIDE_ARCHIVE_ALL/);
assert.match(admin, /hasAnyPermission\(user,/);
assert.match(panel, /Bản này chỉ đọc/);
assert.match(admin, /Thay đổi chưa được lưu/);
assert.match(admin, /Xác nhận phát hành/);
assert.match(admin, /counts=\{guideCounts\}/);
assert.match(admin, /variant="tabs"/);
assert.match(admin, /tw-grid-cols-\[minmax\(280px,1\.35fr\)_minmax\(220px,1fr\)_minmax\(220px,1fr\)\]/);

// Published versions stay immutable in the UI: draft-only editing.
assert.match(panel, /versionNumber > currentVersion/);

// Wiring: endpoints, routes, permissions, navigation, shell hosts.
assert.match(endpoints, /notifications\/user-guides/);
assert.match(endpoints, /notifications\/user-guides\/counts/);
assert.match(endpoints, /notifications\/user-guide-features/);
assert.match(endpoints, /user-guides\/resolve/);
assert.match(routes, /\/admin\/user-guides/);
assert.match(routes, /\/guide\/:code/);
assert.match(routes, /UserGuideManagementPage/);
assert.match(routes, /UserGuidePublicPage/);
assert.match(permissions, /\/admin\/user-guides/);
assert.match(permissions, /USER_GUIDE_READ_ALL/);
assert.match(navigation, /\/admin\/user-guides/);
assert.match(adminLayout, /UserGuideBackendHost/);
assert.match(clientLayout, /UserGuideBackendHost/);
assert.match(adminLayout, /key=\{location\.pathname\}/);
assert.match(clientLayout, /key=\{location\.pathname\}/);
assert.match(router, /function FullscreenShell\(\)[\s\S]*deriveGuideContext\(location\.pathname\)[\s\S]*<UserGuideBackendHost[\s\S]*\{routeContent\}/);

// State-aware pages register one eligible guide instead of blindly opening the first result.
assert.match(backendContext, /registerActivation/);
assert.match(backendContext, /new Map<string, UserGuideActivation/);
assert.match(backendContext, /hasRegistrations/);
assert.match(activationHook, /useId/);
assert.match(manualLauncher, /getPublishedUserGuide/);
assert.match(manualLauncher, /guide\.startGuide\(presentation\)/);
assert.match(helpButton, /useUserGuideManualLauncher/);
assert.match(helpButton, /Hướng dẫn & Trợ giúp/);
assert.match(helpButton, /await launchGuide\(guideCode\)/);
assert.doesNotMatch(helpButton, /useLoading/);
assert.doesNotMatch(helpButton, /Đang mở hướng dẫn\.\.\./);
assert.match(backendHost, /BackendStateAwareStarter/);
assert.match(backendHost, /runtime\.state\.guides\.find\(\(candidate\) => candidate\.code === activation\.code\)/);
assert.match(context, /\/admin\/subscription-approvals/);
assert.match(context, /\/admin\/ticket/);
assert.match(context, /\/admin\/vehicle/);
assert.match(context, /STATE_AWARE_GUIDE_ROUTES/);

// Business-state selection and permission gates remain explicit and do not invoke business APIs.
assert.match(swipe, /PARKING_CHECK_IN_FIRST_RUN/);
assert.match(swipe, /PARKING_CHECK_OUT_FIRST_RUN/);
assert.match(swipe, /PARKING_SESSION_CHECK_IN_ALL/);
assert.match(swipe, /PARKING_SESSION_CHECK_OUT_ALL/);
assert.match(swipe, /UserGuideHelpButton/);
assert.match(helpButton, /Hướng dẫn & Trợ giúp/);
assert.match(lostCard, /LOST_CARD_PAYMENT/);
assert.match(lostCard, /LOST_CARD_REPLACEMENT/);
assert.match(lostCard, /!isPaid \|\| needsNewCard/);
assert.match(subscription, /SUBSCRIPTION_REVIEW_PENDING/);
assert.match(subscription, /SUBSCRIPTION_PAYMENT_COUNTER/);
assert.match(subscription, /SUBSCRIPTION_CARD_FULFILLMENT/);
assert.match(pricing, /PRICE_PLAN_CREATE_ALL/);
assert.match(pricing, /PRICE_RULE_CREATE_ALL/);
assert.match(pricing, /PRICING\.PLAN\.STATUS/);
assert.match(pricing, /PRICING\.RULE\.VALIDATION/);
assert.match(pricing, /userGuide\.activeGuide\?\.code !== "PRICE_PLAN_SETUP"/);
assert.match(pricing, /userGuide\.currentStep\?\.anchorKey === "PRICING\.PLAN\.STATUS"/);
assert.match(pricing, /userGuide\.activeGuide\?\.code !== "PRICE_RULE_SETUP"/);
assert.match(pricing, /setIsCreatePanelOpen\(true\)/);
assert.match(pricing, /Khung giờ chỉ áp dụng cho kế hoạch khách vãng lai/);
assert.match(parkingSessionSummary, /Thao tác tiền mặt hoặc VNPay sẽ xuất hiện tại đây/);
assert.match(lostCard, /keepCancelGuideTargetVisible/);
assert.match(subscription, /Không có quyền từ chối/);
assert.match(catalogPage, /guideAnchorKey="CATALOG_TICKET\.LIST"/);
assert.match(catalogPage, /guideAnchorKey="CATALOG_TICKET\.DETAIL"/);
assert.match(catalogComponents, /row\.id === guideAnchorRowId/);
assert.match(catalogComponents, /data-user-guide-anchor/);

// Every seeded business guide has an explicit replay entry point.
for (const guideCode of ["LOST_CARD_REPORT_CREATE", "LOST_CARD_PAYMENT", "LOST_CARD_REPLACEMENT"]) {
  assert.match(lostCard, new RegExp(guideCode));
}
for (const guideCode of ["SUBSCRIPTION_REVIEW_PENDING", "SUBSCRIPTION_PAYMENT_COUNTER", "SUBSCRIPTION_CARD_FULFILLMENT"]) {
  assert.match(subscription, new RegExp(guideCode));
}
for (const guideCode of ["PRICE_PLAN_SETUP", "PRICE_RULE_SETUP"]) {
  assert.match(pricing, new RegExp(guideCode));
}
for (const guideCode of ["CATALOG_TICKET_TYPE_SETUP", "CATALOG_VEHICLE_TYPE_SETUP"]) {
  assert.match(catalogPage, new RegExp(guideCode));
}
assert.match(catalogComponents, /UserGuideHelpButton/);
assert.match(parkingWorkspaceHeader, /UserGuideHelpButton/);
assert.match(parkingWizard, /guideCode="PARKING_LOT_CREATE"/);
assert.match(lostCard, /UserGuideHelpButton/);
assert.match(subscription, /UserGuideHelpButton/);
assert.match(pricing, /UserGuideHelpButton/);

// Context mapping must degrade to no-fetch on unknown routes.
assert.match(context, /featureCode: string \| null/);
assert.match(context, /return \{\s*contextKey:.*featureCode: match \? match\.featureCode : null/s);

console.log("User guide API, runtime, accessibility, permission gating, and routing checks passed.");
