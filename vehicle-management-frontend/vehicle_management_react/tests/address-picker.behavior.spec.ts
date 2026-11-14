import { expect, test } from "@playwright/test";

for (const compact of [false, true]) {
  test(`address mode buttons preserve detail, reset division codes and respect disabled (${compact ? "compact mobile" : "standard"})`, async ({ page }, testInfo) => {
    const province = { code: "79", name: "Hồ Chí Minh", fullName: "Thành phố Hồ Chí Minh" };
    const district = { code: "760", name: "Quận 1", fullName: "Quận 1" };
    const ward = { code: "26734", name: "Bến Nghé", fullName: "Phường Bến Nghé" };
    await page.route("**/public/administrative-divisions/**", (route) => {
      const path = new URL(route.request().url()).pathname;
      return route.fulfill({ json: { success: true, data: path.endsWith("/provinces") ? [province] : path.endsWith("/districts") ? [district] : [ward] } });
    });
    await page.setViewportSize(compact ? { width: 390, height: 844 } : { width: 1280, height: 900 });
    await page.goto(`/tests/address-picker-harness.html${compact ? "?compact" : ""}`);
    const group = page.getByRole("group", { name: "Chọn kiểu địa chỉ" });
    const current = group.getByRole("button", { name: "Địa chỉ hiện hành", exact: true });
    const legacy = group.getByRole("button", { name: "Địa chỉ cũ (có huyện)", exact: true });
    const output = page.getByLabel("Dữ liệu địa chỉ");
    const initial = { provinceCode: "79", districtCode: "760", wardCode: "26734", addressDetail: "12 Lê Duẩn" };
    await expect(legacy).toHaveAttribute("aria-pressed", "true");
    await legacy.click();
    await expect(output).toHaveText(JSON.stringify(initial));
    await current.click();
    await expect(current).toHaveAttribute("aria-pressed", "true");
    await expect(output).toHaveText(JSON.stringify({ ...initial, provinceCode: "", districtCode: null, wardCode: "" }));
    await expect(page.getByRole("button", { name: "Quận hoặc huyện", exact: true })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Phường hoặc xã", exact: true })).toBeDisabled();
    await expect(page.getByRole("textbox", { name: "Địa chỉ cụ thể", exact: true })).toHaveValue("12 Lê Duẩn");
    await page.getByRole("button", { name: "Tỉnh hoặc thành phố", exact: true }).click();
    await page.getByRole("button", { name: "Thành phố Hồ Chí Minh", exact: true }).click();
    await page.getByRole("button", { name: "Phường hoặc xã", exact: true }).click();
    await page.getByRole("button", { name: "Phường Bến Nghé", exact: true }).click();
    await legacy.click();
    await expect(legacy).toHaveAttribute("aria-pressed", "true");
    await expect(output).toHaveText(JSON.stringify({ ...initial, provinceCode: "", districtCode: null, wardCode: "" }));
    await expect(page.getByRole("button", { name: "Quận hoặc huyện", exact: true })).toBeDisabled();
    const bounds = (await group.boundingBox())!;
    expect(bounds.x + bounds.width).toBeLessThanOrEqual(compact ? 390 : 1280);
    await page.screenshot({ path: testInfo.outputPath("address-mode-buttons.png"), animations: "disabled" });
    await page.getByRole("button", { name: "Khóa địa chỉ", exact: true }).click();
    await expect(current).toBeDisabled();
    await expect(legacy).toBeDisabled();
  });
}
