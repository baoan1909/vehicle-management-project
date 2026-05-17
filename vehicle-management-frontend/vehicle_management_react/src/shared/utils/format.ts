export function formatCurrency(value: number): string {
  return `${new Intl.NumberFormat("vi-VN").format(value)}đ`;
}

export function isActivePath(currentPath: string, matches: string[]): boolean {
  return matches.some((match) => currentPath === match || currentPath.startsWith(`${match}/`));
}
