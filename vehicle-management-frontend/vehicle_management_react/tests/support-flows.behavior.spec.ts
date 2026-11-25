import { expect, test, type Page } from "@playwright/test";

test.setTimeout(60_000);
test.beforeEach(({ page }) => { page.on("pageerror", (error) => console.error("Support flow page error:", error.message)); });
const actor = "00000000-0000-0000-0000-000000000001";
const counterpart = "00000000-0000-0000-0000-000000000002";
const conversationId = "6090c0ea-f5b9-4f40-89b1-db5f9b61b043";
const envelope = (data: unknown) => ({ data, success: true, message: "ok", timestamp: new Date().toISOString() });
const category = (internal: boolean) => ({
  categoryId: internal ? "c1910000-0000-4000-8000-000000000017" : "c1910000-0000-4000-8000-000000000004",
  code: internal ? "INTERNAL_LOST_CARD_WORKFLOW" : "CUSTOMER_CARD_QR", name: internal ? "Lỗi xử lý báo mất thẻ" : "Thẻ và mã QR",
  description: "Không thể hoàn tất chức năng.", priority: "HIGH", displayOrder: 10, featureCode: null,
});
const createdTicket = { supportTicketId: "00000000-0000-0000-0000-000000000099", categoryName: "Support category", title: "Lỗi chức năng", content: "Nội dung lỗi", status: "OPEN", priority: "HIGH", assignedTo: null, createdAt: null };

async function openChat(page: Page, query = "") {
  await page.goto(`/tests/support-flows-harness.html?mode=chat${query}`);
  await page.locator(".vm-chat-conversation-item").first().click();
}

async function mockApi(page: Page, { internal = false, type = "CUSTOMER_DIRECT" } = {}) {
  const posts: string[] = [];
  const ticketRequests: string[] = [];
  const sentMessages: Array<{ content: string; contextTicketId: string | null }> = [];
  const categories = [category(internal)];
  const conversation = { conversationId, conversationType: type, status: "ACTIVE", ownerAccountId: type === "CUSTOMER_DIRECT" && !internal ? counterpart : actor,
    assignedTo: null, customerId: "customer-1", title: "Chat kiểm thử", participants: [
      { accountId: actor, memberRole: internal ? "OWNER" : "CUSTOMER", fullName: "Support Tester", username: "tester" },
      { accountId: counterpart, memberRole: internal ? "CUSTOMER" : "OWNER", fullName: "Người trao đổi", username: "other" },
    ], lastMessageAt: null, lastMessageId: null, supportTicketId: null, relatedId: null, relatedSchema: null, relatedTable: null };
  const conversations = [conversation];
  await page.route("**/vehicle-management/api/**", async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path.includes("/operations/support-tickets")) ticketRequests.push(path);
    if (request.method() === "POST" && path.endsWith(`/chat/conversations/${conversationId}/messages`)) {
      const payload = request.postDataJSON();
      sentMessages.push(payload);
      await route.fulfill({ status: 201, json: envelope({ messageId: "00000000-0000-0000-0000-000000000088", conversationId,
        senderAccountId: actor, messageType: "TEXT", content: payload.content, contextTicketId: payload.contextTicketId,
        createdAt: new Date().toISOString(), deleted: false, attachments: [] }) });
      return;
    }
    if (request.method() === "POST" && /support-tickets($|\/from-conversations\/)/.test(path)) {
      posts.push(path);
      await route.fulfill({ status: path.includes("/from-conversations/") && type === "CUSTOMER_DIRECT" ? 400 : 201,
        json: path.includes("/from-conversations/") && type === "CUSTOMER_DIRECT"
          ? { success: false, message: "Conversation must be an active requester support conversation" } : envelope(createdTicket) });
      return;
    }
    const data = path.endsWith("/support-ticket-categories/available") ? categories
      : path.endsWith("/chat/conversations") ? conversations.map((item) => ({ conversation: item, lastMessage: null, unreadCount: 0 }))
      : conversations.some((item) => path.endsWith(`/chat/conversations/${item.conversationId}`)) ? conversations.find((item) => path.endsWith(`/chat/conversations/${item.conversationId}`))
      : path.includes("/accounts/me/profile") ? { fullName: "Support Tester", customer: {} }
      : [];
    await route.fulfill({ json: envelope(data) });
  });
  return { posts, categories, ticketRequests, sentMessages, conversations };
}

for (const mode of ["customer", "internal"]) test(`${mode} dropdown refreshes by aligned icon and creates a ticket`, async ({ page }) => {
  const state = await mockApi(page, { internal: mode === "internal" });
  await page.goto(`/tests/support-flows-harness.html?mode=${mode}`);
  await page.getByRole("button", { name: "Tạo yêu cầu mới" }).click();
  const dialog = page.getByRole("dialog", { name: "Tạo yêu cầu hỗ trợ" });
  const refresh = dialog.getByRole("button", { name: "Làm mới danh mục hỗ trợ" });
  await expect(refresh).toBeEnabled();
  const heading = await dialog.getByText("Danh mục hỗ trợ", { exact: true }).boundingBox();
  const icon = await refresh.boundingBox();
  expect(Math.abs(heading!.y + heading!.height / 2 - (icon!.y + icon!.height / 2))).toBeLessThan(3);
  state.categories.push({ ...category(mode === "internal"), categoryId: "new-category", code: "NEW_CUSTOM", name: "Danh mục vừa thêm" });
  await refresh.click();
  await dialog.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await page.getByRole("button", { name: "Danh mục vừa thêm", exact: true }).click();
  await dialog.getByLabel("Tiêu đề", { exact: true }).fill("Lỗi chức năng");
  await dialog.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect(dialog).toContainText("Vui lòng nhập nội dung chi tiết về chức năng gặp lỗi.");
  expect(state.posts).toHaveLength(0);
  await dialog.getByLabel("Nội dung", { exact: true }).fill("Chức năng không cập nhật trạng thái.");
  await dialog.getByRole("button", { name: "Gửi yêu cầu", exact: true }).click();
  await expect(dialog).toBeHidden();
  expect(state.posts).toEqual(["/vehicle-management/api/operations/support-tickets"]);
});

for (const internal of [false, true]) test(`CUSTOMER_DIRECT never offers support requests even with ticket permissions (${internal ? "partner" : "customer"})`, async ({ page }) => {
  const state = await mockApi(page, { internal });
  await openChat(page, "&orders=" + (internal ? "all" : "own"));
  await expect(page.getByPlaceholder("Nhập nội dung trả lời...")).toBeEnabled();
  await expect(page.getByRole("button", { name: "Mở tác vụ khác" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Lịch sử yêu cầu", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Gắn phiếu", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Mở danh sách đơn hàng" })).toBeVisible();
  await page.getByPlaceholder("Nhập nội dung trả lời...").fill("Trao đổi đơn hàng thông thường");
  await page.getByRole("button", { name: "Gửi", exact: true }).click();
  await expect.poll(() => state.sentMessages.length).toBe(1);
  expect(state.sentMessages[0]).toMatchObject({ content: "Trao đổi đơn hàng thông thường", contextTicketId: null });
  expect(state.ticketRequests).toEqual([]);
});
for (const orders of ["none", "own", "all"]) test(`chat hides unpermitted ticket item and order icon (${orders})`, async ({ page }) => {
  await mockApi(page);
  await openChat(page, `&ticket=no&orders=${orders}`);
  await expect(page.getByRole("button", { name: "Mở tác vụ khác" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Chưa thể tạo phiếu hỗ trợ" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Mở danh sách đơn hàng" })).toHaveCount(orders === "own" ? 1 : 0);
});

test("partner order icon requires ALL rather than OWN subscription permission", async ({ page }) => {
  await mockApi(page, { internal: true });
  await openChat(page, "&orders=own");
  await expect(page.getByPlaceholder("Nhập nội dung trả lời...")).toBeVisible();
  await expect(page.getByRole("button", { name: "Mở danh sách đơn hàng" })).toHaveCount(0);
  await openChat(page, "&orders=all");
  await expect(page.getByRole("button", { name: "Mở danh sách đơn hàng" })).toBeVisible();
});

for (const internal of [false, true]) for (const type of ["ASSISTANT_SUPPORT", "SUPPORT_REQUESTER_DIRECT"]) test(`${type} preserves system support creation (${internal ? "partner" : "customer"})`, async ({ page }) => {
  const state = await mockApi(page, { internal, type });
  await openChat(page);
  await page.getByRole("button", { name: "Mở tác vụ khác" }).click();
  await page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Tạo phiếu hỗ trợ" });
  await dialog.getByRole("button", { name: "Danh mục hỗ trợ", exact: true }).click();
  await page.getByRole("button", { name: category(internal).name, exact: true }).click();
  await dialog.getByLabel("Tiêu đề", { exact: true }).fill("Lỗi chức năng");
  await dialog.getByLabel("Nội dung chi tiết").fill("Chức năng không cập nhật trạng thái.");
  await dialog.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true }).click();
  await expect(dialog).toBeHidden();
  expect(state.posts).toEqual([`/vehicle-management/api/operations/support-tickets/from-conversations/${conversationId}`]);
  await expect(page.getByText("Đã gửi phiếu hỗ trợ trong hội thoại.")).toBeVisible();
});

for (const type of ["ASSISTANT_SUPPORT", "SUPPORT_REQUESTER_DIRECT"]) test(`${type} still hides ticket creation without permission`, async ({ page }) => {
  await mockApi(page, { type });
  await openChat(page, "&ticket=no");
  await page.getByRole("button", { name: "Mở tác vụ khác" }).click();
  await expect(page.getByRole("button", { name: "Tạo phiếu hỗ trợ", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Lịch sử yêu cầu", exact: true })).toBeVisible();
});

test("assistant stays first and pale blue when another chat is selected and inbox refreshes", async ({ page }) => {
  const state = await mockApi(page);
  const assistantId = "00000000-0000-0000-0000-000000000077";
  state.conversations.push({ ...state.conversations[0], conversationId: assistantId,
    conversationType: "ASSISTANT_SUPPORT", ownerAccountId: actor, title: "Trợ lý hỗ trợ CoParking" });
  await page.goto("/tests/support-flows-harness.html?mode=chat");
  const rows = page.locator(".vm-chat-conversation-item");
  const assistant = rows.filter({ hasText: "Trợ lý hỗ trợ CoParking" });
  await expect(rows.first()).toContainText("Trợ lý hỗ trợ CoParking");
  await expect(assistant).toHaveCSS("background-color", "rgb(239, 246, 255)");
  await rows.nth(1).click();
  await expect(assistant).not.toHaveClass(/tw-border-vm-primary/);
  await expect(assistant).toHaveCSS("background-color", "rgb(239, 246, 255)");
  state.conversations.unshift({ ...state.conversations[0], conversationId: "00000000-0000-0000-0000-000000000066", title: "Chat vừa có tin mới" });
  await Promise.all([
    page.waitForResponse((response) => response.url().endsWith("/chat/conversations")),
    page.getByRole("button", { name: "Tải lại hội thoại" }).click(),
  ]);
  await expect(rows).toHaveCount(3);
  await expect(rows.first()).toContainText("Trợ lý hỗ trợ CoParking");
  await expect(assistant).not.toHaveClass(/tw-border-vm-primary/);
  await expect(assistant).toHaveCSS("background-color", "rgb(239, 246, 255)");
});
