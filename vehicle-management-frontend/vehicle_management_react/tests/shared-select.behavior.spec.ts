import { expect, test } from "@playwright/test";

test("migrated form and filter dropdowns retain submitted values and reset behavior", async ({ page }) => {
  await page.goto("/tests/shared-select-harness.html");
  const form = page.getByRole("region", { name: "Biểu mẫu", exact: true }).locator("form");
  await expect(form.getByRole("button", { name: "Phân loại", exact: true })).toContainText("B");
  await form.getByRole("button", { name: "Phân loại", exact: true }).click();
  await page.getByRole("button", { name: "A", exact: true }).click();
  expect(await form.evaluate((element) => Object.fromEntries(new FormData(element as HTMLFormElement)))).toEqual({ kind: "A" });
  const filters = page.getByRole("form", { name: "Bộ lọc dữ liệu" });
  await filters.getByRole("button", { name: "Trạng thái", exact: true }).click();
  await page.getByRole("button", { name: "Hoạt động", exact: true }).click();
  expect(await filters.evaluate((element) => Object.fromEntries(new FormData(element as HTMLFormElement)))).toEqual({ status: "ACTIVE" });
  await filters.getByRole("button", { name: "Đặt lại", exact: true }).click();
  expect(await filters.evaluate((element) => Object.fromEntries(new FormData(element as HTMLFormElement)))).toEqual({ status: "" });
});

test("shared page-size dropdown resets pagination and unavailable options stay disabled", async ({ page }, testInfo) => {
  await page.goto("/tests/shared-select-harness.html");
  const table = page.getByRole("region", { name: "Bảng dữ liệu", exact: true });
  await table.getByRole("button", { name: "2", exact: true }).click();
  await expect(table.getByRole("cell", { name: "Dòng 11", exact: true })).toBeVisible();
  await table.getByRole("button", { name: "Số dòng mỗi trang", exact: true }).click();
  await page.getByRole("button", { name: "5", exact: true }).click();
  await expect(table.getByRole("cell", { name: "Dòng 1", exact: true })).toBeVisible();
  await expect(table.getByRole("row")).toHaveCount(6);
  await page.getByRole("button", { name: "Lựa chọn khả dụng", exact: true }).click();
  await expect(page.getByRole("button", { name: "Không khả dụng", exact: true })).toBeDisabled();
  await page.screenshot({ path: testInfo.outputPath("shared-dropdown.png"), animations: "disabled" });
  await page.getByLabel("Khóa lựa chọn", { exact: true }).check();
  await expect(page.getByRole("button", { name: "Lựa chọn khả dụng", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "Không khả dụng", exact: true })).toHaveCount(0);
});
