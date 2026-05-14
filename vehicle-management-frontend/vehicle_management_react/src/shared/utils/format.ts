export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("vi-VN").format(value) + "đ";
}

export function normalizeHashPath(hash: string): string {
  const path = hash.replace(/^#/, "") || "/admin/dashboard";
  return path.startsWith("/") ? path : `/${path}`;
}

export function isActivePath(currentPath: string, matches: string[]): boolean {
  return matches.some((match) => currentPath === match || currentPath.startsWith(`${match}/`));
}
