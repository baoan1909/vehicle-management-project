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
const permissionsPath = new URL("../src/app/routePermissions.ts", import.meta.url);
const navigationPath = new URL("../src/config/navigation.ts", import.meta.url);
const adminLayoutPath = new URL("../src/shared/components/layout/AdminLayout.tsx", import.meta.url);
const clientLayoutPath = new URL("../src/shared/components/layout/ClientLayout.tsx", import.meta.url);

const [
  api, host, card, backendHost, runtime, admin, panel, publicPage, context, endpoints, routes,
  permissions, navigation, adminLayout, clientLayout,
] = await Promise.all(
  [
    apiPath, hostPath, cardPath, backendHostPath, runtimePath, adminPath, panelPath, publicPath, contextPath,
    endpointsPath, routesPath, permissionsPath, navigationPath, adminLayoutPath, clientLayoutPath,
  ].map((url) => readFile(url, "utf8")),
);

// Runtime must never trust client-provided identity: no accountId in any payload.
assert.doesNotMatch(api, /accountId/);
assert.match(api, /resolveUserGuides\(featureCode/);
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

// Published versions stay immutable in the UI: draft-only editing.
assert.match(panel, /versionNumber > currentVersion/);

// Wiring: endpoints, routes, permissions, navigation, shell hosts.
assert.match(endpoints, /notifications\/user-guides/);
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

// Context mapping must degrade to no-fetch on unknown routes.
assert.match(context, /featureCode: string \| null/);
assert.match(context, /return \{\s*contextKey:.*featureCode: match \? match\.featureCode : null/s);

console.log("User guide API, runtime, accessibility, permission gating, and routing checks passed.");
