import { expect, test, type Page } from "@playwright/test";

const envelope = (data: unknown) => ({ success: true, data, message: "Thành công", timestamp: "2026-11-25T09:00:00+07:00" });
const paged = (content: unknown[]) => ({ content, first: true, last: true, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 });
const source = { sourceId: "source-public", title: "Hướng dẫn khách hàng", status: "ACTIVE", accessScope: "PUBLIC", tenantId: null, description: "Hướng dẫn sử dụng", createdAt: "2026-11-25T02:00:00Z", updatedAt: null };
const knowledgeDocument = {
  documentId: "doc-monthly", documentKey: "monthly", sourceId: source.sourceId, title: "Điều kiện đăng ký vé tháng", status: "REVIEW", accessScope: "PUBLIC",
  documentVersion: 1, originalFilename: "ve-thang.md", fileExtension: "md", fileSizeBytes: 800, mimeType: "text/markdown", checksumSha256: "fixture",
  tenantId: null, effectiveFrom: null, effectiveTo: null, reviewedAt: null, archivedAt: null, failureCode: null, createdAt: source.createdAt, updatedAt: null,
};
const version = {
  indexVersionId: "index-qwen", versionCode: "LOCAL-2026-11", status: "READY", provider: "LM_STUDIO", modelId: "qwen3-embedding-0.6b",
  modelConfigurationId: "embedding-local", dimension: 768, chunkerVersion: "chunks-v1", embeddingPromptVersion: "qwen-qa-v1", distanceMetric: "COSINE", normalization: "L2",
  embeddedChunkCount: 16, expectedChunkCount: 16, failedChunkCount: 0, failureCode: null, contentChecksum: "fixture", createdAt: source.createdAt, updatedAt: source.createdAt,
  readyAt: source.createdAt, activatedAt: null as string | null, retiredAt: null,
};

async function fixture(page: Page, options: { empty?: boolean; readonly?: boolean; failedPreview?: boolean; dashboardError?: boolean; archivedOnly?: boolean } = {}) {
  const requests: Array<{ path: string; method: string }> = [];
  const unknown: string[] = [];
  let documentStatus = options.archivedOnly ? "ARCHIVED" : "REVIEW";
  let versionStatus = "READY";
  let qualityFails = !!options.dashboardError;
  await page.route("**/ai/**", async (route) => {
    const request = route.request();
    if (!["fetch", "xhr"].includes(request.resourceType())) return route.continue();
    const url = new URL(request.url());
    const path = url.pathname.slice(url.pathname.indexOf("/ai/"));
    requests.push({ path, method: request.method() });
    if (path === "/ai/knowledge/quality-dashboard") {
      if (qualityFails) { qualityFails = false; return route.fulfill({ status: 503, json: { success: false, message: "Không thể tải tổng quan" } }); }
      return route.fulfill({ json: envelope({
        sourcesByStatus: options.empty ? {} : { ACTIVE: 1 }, documentsByStatus: options.empty ? {} : { [documentStatus]: 1 },
        jobsByStatus: { FAILED: 3 }, indexVersionsByStatus: options.empty ? {} : { [versionStatus]: 1 }, activeIndexCount: versionStatus === "ACTIVE" ? 1 : 0,
        totalIndexVersionCount: options.empty ? 0 : 1, warnings: [],
      }) });
    }
    if (path === "/ai/knowledge/sources") return route.fulfill({ json: envelope(paged(options.empty ? [] : [source])) });
    if (path === "/ai/knowledge/documents") return route.fulfill({ json: envelope(paged(options.empty ? [] : [{ ...knowledgeDocument, status: documentStatus }])) });
    if (path === "/ai/knowledge/ingestion-jobs") return route.fulfill({ json: envelope(paged([])) });
    if (path === "/ai/knowledge/documents/doc-monthly/blocks") {
      if (options.failedPreview) return route.fulfill({ status: 500, json: { success: false, message: "Không đọc được nội dung" } });
      return route.fulfill({ json: envelope([{ blockId: "block-1", blockIndex: 0, content: "Khách hàng cần có phương tiện hợp lệ và chọn bãi xe còn chỗ.", kind: "TEXT", sourcePage: 1, sourceSection: null, headingPath: "Điều kiện", createdAt: source.createdAt }]) });
    }
    if (path === "/ai/knowledge/documents/doc-monthly/chunks") return route.fulfill({ json: envelope([]) });
    if (path === "/ai/knowledge/documents/doc-monthly/publish" && request.method() === "POST") {
      documentStatus = "READY";
      return route.fulfill({ json: envelope({ ...knowledgeDocument, status: documentStatus }) });
    }
    if (path === "/ai/index-versions") return route.fulfill({ json: envelope([{ ...version, status: versionStatus }]) });
    if (path === "/ai/models/configurations") return route.fulfill({ json: envelope([]) });
    if (path === "/ai/index-versions/index-qwen/activate" && request.method() === "POST") {
      versionStatus = "ACTIVE";
      return route.fulfill({ json: envelope({ ...version, status: versionStatus, activatedAt: source.createdAt }) });
    }
    if (path === "/ai/knowledge/retrieval-tests") return route.fulfill({ json: envelope({
      retrievalMethod: "COMMON_FAQ", diagnosticCode: null, activeIndexVersionId: null,
      results: [{ chunkId: "faq-1", documentId: knowledgeDocument.documentId, title: knowledgeDocument.title, content: "Chọn bãi xe còn chỗ và phương tiện đủ điều kiện.", sourcePage: null, sourceSection: "Điều kiện", score: 0.98 }],
    }) });
    unknown.push(`${request.method()} ${path}`);
    return route.fulfill({ status: 400, json: { success: false, message: "Unexpected test request" } });
  });
  return { requests, unknown };
}

test("empty warehouse starts at source setup; navigation keeps URL context and browser back", async ({ page }) => {
  const state = await fixture(page, { empty: true });
  await page.goto("/tests/ai-knowledge-workflow-harness.html?keep=team");
  await expect(page.getByRole("heading", { name: "Bắt đầu bằng một nguồn tài liệu" })).toBeVisible();
  await page.getByRole("button", { name: "Đến bước 1 · Nguồn & quyền xem" }).click();
  await expect(page).toHaveURL(/keep=team.*tab=sources/);
  await expect(page.getByRole("button", { name: "Tạo nguồn mới" })).toBeVisible();
  await page.getByRole("button", { name: "Bước tiếp theo" }).click();
  await page.getByRole("button", { name: "Tải lên tài liệu", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Tải lên tài liệu" });
  await expect(dialog.getByText(/Chưa có nguồn đang hoạt động/)).toBeVisible();
  await expect(dialog.getByRole("button", { name: "Tải lên", exact: true })).toBeDisabled();
  await expect(dialog.getByLabel("Mã chống gửi trùng")).not.toBeVisible();
  await dialog.getByRole("button", { name: "Đến nguồn & quyền xem" }).click();
  await expect(page).toHaveURL(/tab=sources/);
  await page.goBack();
  await expect(page).toHaveURL(/tab=documents/);
  expect(state.requests.filter((request) => request.method !== "GET")).toEqual([]);
  expect(state.unknown).toEqual([]);
});

test("review opens actual extracted content and scope before approval; confirmation makes exactly one write", async ({ page }) => {
  const state = await fixture(page);
  await page.goto("/tests/ai-knowledge-workflow-harness.html");
  await expect(page.getByRole("heading", { name: "1 tài liệu đang chờ bạn kiểm tra" })).toBeVisible();
  await page.getByRole("button", { name: "Mở tài liệu chờ duyệt" }).click();
  await expect(page.getByRole("button", { name: "Duyệt nội dung", exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "Xem & duyệt" }).click();
  const preview = page.getByRole("dialog", { name: knowledgeDocument.title });
  await expect(preview.getByText("Khách hàng cần có phương tiện hợp lệ và chọn bãi xe còn chỗ.")).toBeVisible();
  await expect(preview.getByText(/Quyền xem: Công khai/)).toBeVisible();
  expect(state.requests.filter((request) => request.method !== "GET")).toEqual([]);
  await preview.getByRole("button", { name: "Duyệt nội dung", exact: true }).click();
  const confirmation = page.getByRole("dialog", { name: "Duyệt nội dung tài liệu" });
  await expect(confirmation.getByText(/phần tìm kiếm tài liệu cần phiên bản phù hợp/)).toBeVisible();
  await confirmation.getByRole("button", { name: "Duyệt nội dung", exact: true }).click();
  await expect(page.getByRole("cell", { name: "Đã duyệt nội dung", exact: true })).toBeVisible();
  expect(state.requests.filter((request) => request.method === "POST")).toEqual([{ path: "/ai/knowledge/documents/doc-monthly/publish", method: "POST" }]);
  expect(state.unknown).toEqual([]);
});

test("read-only account can inspect but has no upload, approval, model or activation controls", async ({ page }) => {
  const state = await fixture(page, { readonly: true });
  await page.goto("/tests/ai-knowledge-workflow-harness.html?readonly");
  await expect(page.getByText("Bạn có thể xem; cần người có quyền thực hiện bước này.")).toBeVisible();
  await expect(page.getByRole("link", { name: "Model & kết nối" })).toHaveCount(0);
  await page.getByRole("button", { name: "Mở tài liệu chờ duyệt" }).click();
  await expect(page.getByRole("button", { name: "Tải lên tài liệu" })).toHaveCount(0);
  await page.getByRole("button", { name: "Xem nội dung", exact: true }).click();
  await expect(page.getByRole("dialog").getByRole("button", { name: /Duyệt nội dung|Từ chối/ })).toHaveCount(0);
  await page.getByRole("dialog").getByRole("button", { name: "Đóng", exact: true }).filter({ hasText: /^Đóng$/ }).click();
  await page.getByRole("navigation", { name: "Các bước quản lý tri thức" }).getByRole("button", { name: "3 Bật tìm kiếm" }).click();
  await expect(page.getByRole("button", { name: /Bật sử dụng|Chuẩn bị bản mới/ })).toHaveCount(0);
  expect(state.requests.filter((request) => request.method !== "GET")).toEqual([]);
  expect(state.requests.some((request) => request.path === "/ai/models/configurations")).toBe(false);
  expect(state.unknown).toEqual([]);
});

test("failed preview cannot be approved", async ({ page }) => {
  const state = await fixture(page, { failedPreview: true });
  await page.goto("/tests/ai-knowledge-workflow-harness.html?tab=documents");
  await page.getByRole("button", { name: "Xem & duyệt" }).click();
  const preview = page.getByRole("dialog", { name: knowledgeDocument.title });
  await expect(preview.getByRole("alert")).toBeVisible();
  await expect(preview.getByRole("button", { name: "Duyệt nội dung", exact: true })).toBeDisabled();
  expect(state.requests.filter((request) => request.method !== "GET")).toEqual([]);
});

test("old index deep link works; technical fields are collapsed and activation requires confirmation", async ({ page }) => {
  const state = await fixture(page);
  await page.goto("/tests/ai-knowledge-workflow-harness.html?tab=index-versions");
  await expect(page.getByRole("cell", { name: "Chờ bật sử dụng" })).toBeVisible();
  await expect(page.getByText("COSINE / L2")).not.toBeVisible();
  await page.getByText("Thông số kỹ thuật", { exact: true }).click();
  await expect(page.getByText("COSINE / L2")).toBeVisible();
  await page.getByRole("button", { name: "Bật sử dụng", exact: true }).click();
  expect(state.requests.filter((request) => request.method !== "GET")).toEqual([]);
  await page.getByRole("dialog", { name: "Bật phiên bản tìm kiếm" }).getByRole("button", { name: "Bật sử dụng", exact: true }).click();
  await expect(page.getByRole("cell", { name: "Đang dùng", exact: true })).toBeVisible();
  expect(state.requests.filter((request) => request.method === "POST")).toEqual([{ path: "/ai/index-versions/index-qwen/activate", method: "POST" }]);
  expect(state.unknown).toEqual([]);
});

test("source test accepts unaccented questions without claiming answer accuracy or auto-calling on example selection", async ({ page }) => {
  const state = await fixture(page);
  await page.goto("/tests/ai-knowledge-workflow-harness.html?tab=tests");
  await expect(page.getByText(/chưa phải câu trả lời hoàn chỉnh/)).toBeVisible();
  await page.getByRole("button", { name: "dang ky ve thang can gi", exact: true }).click();
  expect(state.requests.filter((request) => request.path.endsWith("retrieval-tests"))).toHaveLength(0);
  await page.getByRole("button", { name: "Tìm nguồn thử", exact: true }).click();
  await expect(page.getByRole("heading", { name: "1 nguồn tìm được cho “dang ky ve thang can gi”" })).toBeVisible();
  await expect(page.getByText("Chọn bãi xe còn chỗ và phương tiện đủ điều kiện.")).toBeVisible();
  await expect(page.getByText("98.0%", { exact: true })).toHaveCount(0);
  expect(state.requests.filter((request) => request.path.endsWith("retrieval-tests"))).toHaveLength(1);
  expect(state.unknown).toEqual([]);
});

test("dashboard failure does not invent empty counts or a successful state; retry recovers", async ({ page }) => {
  await fixture(page, { dashboardError: true });
  await page.goto("/tests/ai-knowledge-workflow-harness.html");
  await expect(page.getByRole("heading", { name: "Chưa tải được tình trạng kho tài liệu" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Nguồn đang dùng/ })).toHaveCount(0);
  await page.getByRole("button", { name: "Thử tải lại", exact: true }).click();
  await expect(page.getByRole("heading", { name: "1 tài liệu đang chờ bạn kiểm tra" })).toBeVisible();
});

test("archived-only warehouse guides back to upload; mobile overview has no page overflow", async ({ page }, testInfo) => {
  await fixture(page, { archivedOnly: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/tests/ai-knowledge-workflow-harness.html");
  await expect(page.getByRole("heading", { name: "Nguồn đã có, hãy thêm tài liệu" })).toBeVisible();
  expect(await page.evaluate(() => window.document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: testInfo.outputPath("knowledge-mobile.png"), fullPage: true });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({ path: testInfo.outputPath("knowledge-desktop.png"), fullPage: true });
});
