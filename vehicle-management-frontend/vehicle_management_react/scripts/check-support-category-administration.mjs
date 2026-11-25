import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const [navigation, permissions, page, api, createDialog, categoryHook, picker] = await Promise.all([
  readFile(new URL("../src/config/navigation.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/app/routePermissions.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/support/pages/SupportCategoryWorkflowPage.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/support/api/supportApi.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/support/components/CreateSupportTicketDialog.tsx", import.meta.url), "utf8"),
  readFile(new URL("../src/features/support/hooks/useAvailableSupportCategories.ts", import.meta.url), "utf8"),
  readFile(new URL("../src/features/support/components/SupportCategoryPicker.tsx", import.meta.url), "utf8"),
]);

assert.match(navigation, /Danh mục hỗ trợ.*\/admin\/support-categories/);
assert.match(permissions, /"\/admin\/support-categories": \["SUPPORT_TICKET_CATEGORY_READ_ALL"\]/);
assert.match(page, /SUPPORT_TICKET_CATEGORY_CREATE_ALL/);
assert.match(page, /SUPPORT_TICKET_CATEGORY_UPDATE_ALL/);
assert.match(page, /SUPPORT_TICKET_CATEGORY_ACTIVATE_ALL/);
assert.match(page, /Đối tác & nhân sự/);
assert.match(page, /getIamPermissions/);
assert.doesNotMatch(page, /label: "Tất cả", value: "ALL"/);
assert.doesNotMatch(page, /Nhập các permission code/);
assert.match(api, /supportTicketCategories}\s*\/available/);
assert.match(createDialog, /useAvailableSupportCategories/);
assert.match(categoryHook, /getAvailableSupportTicketCategories/);
assert.match(picker, /searchable/);
assert.match(picker, /selected\?\.description/);
assert.doesNotMatch(createDialog, /getPublicSupportCategories/);

console.log("Support category administration and audience safeguards passed.");
