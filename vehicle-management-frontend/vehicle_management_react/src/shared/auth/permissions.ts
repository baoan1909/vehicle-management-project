import type { CurrentUser } from "@/shared/types/common";

export type PermissionCode = string;

export function normalizePermissionCode(permissionCode: string) {
  return permissionCode.trim().replace(/^SCOPE_/, "").toUpperCase();
}

export function normalizePermissionCodes(permissionCodes?: readonly string[] | null) {
  return Array.from(
    new Set(
      (permissionCodes ?? [])
        .filter((permissionCode): permissionCode is string => typeof permissionCode === "string")
        .map(normalizePermissionCode)
        .filter(Boolean),
    ),
  );
}

export function getUserPermissionSet(user?: CurrentUser | null) {
  return new Set(normalizePermissionCodes(user?.permissionCodes));
}

export function hasResolvedPermissions(user: CurrentUser | null | undefined) {
  return Array.isArray(user?.permissionCodes);
}

export function hasAnyPermission(user: CurrentUser | null | undefined, requiredPermissions?: readonly PermissionCode[]) {
  if (!requiredPermissions || requiredPermissions.length === 0) {
    return true;
  }

  const permissionSet = getUserPermissionSet(user);
  return requiredPermissions.some((permissionCode) => permissionSet.has(normalizePermissionCode(permissionCode)));
}
