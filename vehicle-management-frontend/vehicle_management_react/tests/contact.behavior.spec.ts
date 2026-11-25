import { expect, test } from "@playwright/test";

test("contact shows approved company details and cannot submit an unavailable form", async ({ page }) => {
  const submitted: string[] = [];
  for (const [path, data] of [
    ["public/application-time", { timeZone: "Asia/Ho_Chi_Minh" }],
    ["notifications/broadcast-announcements/active", []],
  ] as const) {
    await page.route(`**/vehicle-management/api/${path}`, (route) => route.fulfill({
      contentType: "application/json", body: JSON.stringify({ success: true, data }),
    }));
  }
  await page.route("https://www.google.com/maps**", (route) => route.fulfill({ contentType: "text/html", body: "Map fixture" }));
  page.on("request", (request) => {
    if (request.method() === "POST") submitted.push(request.url());
  });
  await page.goto("/contact");
  await expect(page.getByRole("heading", { name: "Thông tin liên hệ" })).toBeVisible();
  const company = page.getByRole("article").filter({ has: page.getByRole("heading", { name: "Thông tin liên hệ" }) });
  await expect(company.getByText("0912345678", { exact: true })).toBeVisible();
  await expect(company.getByText("10/5 Đường Tỉnh Lộ 19, Phường An Phú Đông, Thành phố Hồ Chí Minh", { exact: true })).toBeVisible();
  await expect(page.getByText("Biểu mẫu liên hệ hiện chưa nhận yêu cầu.", { exact: false })).toBeVisible();
  await expect(page.getByRole("button", { name: "Chưa nhận yêu cầu qua biểu mẫu" })).toBeDisabled();
  await expect(page.getByRole("textbox").first()).toBeDisabled();
  const source = await page.locator('iframe[title="Bản đồ trụ sở CoParking"]').getAttribute("src");
  expect(decodeURIComponent(source ?? "")).toContain("10/5 Đường Tỉnh Lộ 19");
  expect(submitted).toEqual([]);
  await expect(page.getByRole("link", { name: "Liên hệ qua email" }).first()).toHaveAttribute("href", "mailto:support@coparking.vn");
});
