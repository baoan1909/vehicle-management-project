import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = process.cwd();
const transitionLoader = readFileSync(
  resolve(root, "src/shared/components/ui/PageTransitionLoader.tsx"),
  "utf8",
);
const loadingHook = readFileSync(resolve(root, "src/hooks/use-loading.ts"), "utf8");
const loaderService = readFileSync(resolve(root, "src/services/loader-service.tsx"), "utf8");

for (const forbiddenPattern of [/window\.history\.pushState\s*=/, /window\.history\.replaceState\s*=/, /addEventListener\("popstate"/]) {
  if (forbiddenPattern.test(transitionLoader)) {
    throw new Error(`Page transition loader must not patch browser history: ${forbiddenPattern}`);
  }
}

for (const requiredPattern of [/startLoading\(\);\s*finishLoading\(\);/, /visibleRef\.current = false/, /finally\s*{\s*LoaderService\.hide\(\)/, /pendingCount/]) {
  if (!requiredPattern.test(`${transitionLoader}\n${loadingHook}\n${loaderService}`)) {
    throw new Error(`Missing loading lifecycle safeguard: ${requiredPattern}`);
  }
}

console.log("Loading lifecycle safeguards are in place.");
