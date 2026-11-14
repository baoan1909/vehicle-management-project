import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const routes = await readFile(new URL("../src/app/routes.tsx", import.meta.url), "utf8");
const router = await readFile(new URL("../src/app/router/index.tsx", import.meta.url), "utf8");
const indexHtml = await readFile(new URL("../index.html", import.meta.url), "utf8");

const routeTitles = [...routes.matchAll(/\{ path: "([^"]+)", title: "([^"]+)"/g)]
  .map(([, path, title]) => ({ path, title }));

assert.ok(routeTitles.length >= 70, "Every application route must keep a browser-tab title.");

const legacyUnaccentedTitles = new Set([
  "Bang gia dich vu do xe",
  "Chi tiet bang gia bai xe",
  "Chi tiet phieu bao mat the",
  "Danh muc ho tro va quy trinh xu ly yeu cau",
  "Duyet dang ky ve thang va gan the",
  "Duyet onboarding",
  "Gioi thieu CoParking",
  "Ho so ca nhan",
  "Ho tro",
  "Huong dan",
  "Ke hoach gia",
  "Ket qua thanh toan",
  "Lich su gui xe",
  "Lien he",
  "Phan quyen vai tro",
  "Phi dang ky",
  "Phi vang lai",
  "Phien gui xe",
  "Quan ly phuong tien",
  "Quan ly the",
  "Quan ly tri thuc AI",
  "Quan ly ve",
  "Quan ly voucher",
  "Quan tri model AI",
  "Quy tac gia",
  "Tao phieu bao mat the",
  "The bi mat",
  "Thong tin khach hang",
  "Thong tin nhan vien",
  "Thong tin phuong tien",
  "Thong tin tai khoan",
  "Thong tin tai khoan ca nhan",
  "Thong tin the",
  "Thong tin vai tro",
  "Thong tin ve",
  "Tong quan",
  "Tong quan khach hang",
  "Trung tam ho tro van hanh",
  "Ve thang",
  "Xe cua toi",
  "Yeu cau ho tro",
]);

for (const route of routeTitles) {
  assert.ok(
    !legacyUnaccentedTitles.has(route.title),
    `Browser title for ${route.path} is still missing Vietnamese diacritics: ${route.title}`,
  );
}

assert.match(routes, /path: "\/customer\/support", title: "Hỗ trợ"/);
assert.match(router, /document\.title = activeHandle\?\.title \?\? "Quản lý bãi xe CoParking"/);
assert.match(indexHtml, /<title>Quản lý bãi xe CoParking<\/title>/);

console.log(`Vietnamese browser titles passed for ${routeTitles.length} routes.`);
