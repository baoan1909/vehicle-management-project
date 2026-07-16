import { appConfig } from "@/config/env";

const ABSOLUTE_URL_PATTERN = /^[a-z][a-z\d+\-.]*:/i;
const LOCAL_APP_PATH_PREFIXES = ["/assets/", "/favicon", "/logo"];

function encodePathSegments(path: string) {
  return path
    .split("/")
    .map((segment) => encodeURIComponent(segment))
    .join("/");
}

function getPublicFileBase() {
  return appConfig.publicFileBaseUrl.replace(/\/+$/, "");
}

function getPublicBucketName() {
  try {
    const parsedBase = new URL(getPublicFileBase());
    return parsedBase.pathname.split("/").filter(Boolean).at(-1);
  } catch {
    return getPublicFileBase().split("/").filter(Boolean).at(-1);
  }
}

function buildPublicFileUrl(objectKey: string) {
  return `${getPublicFileBase()}/${encodePathSegments(objectKey.replace(/^\/+/, ""))}`;
}

function normalizeStoragePath(path: string) {
  const publicBucketName = getPublicBucketName();
  const normalizedPath = path.replace(/^\/+/, "");

  if (publicBucketName && normalizedPath.startsWith(`${publicBucketName}/`)) {
    return normalizedPath.slice(publicBucketName.length + 1);
  }

  return normalizedPath;
}

export function resolvePublicMediaUrl(value?: string | null) {
  const mediaPath = value?.trim();
  if (!mediaPath) return undefined;

  if (mediaPath.startsWith("blob:") || mediaPath.startsWith("data:")) {
    return mediaPath;
  }

  if (ABSOLUTE_URL_PATTERN.test(mediaPath)) {
    try {
      const parsedUrl = new URL(mediaPath);
      const publicBucketName = getPublicBucketName();
      const bucketPathPrefix = publicBucketName ? `/${publicBucketName}/` : "";

      if (parsedUrl.hostname === "minio" && bucketPathPrefix && parsedUrl.pathname.startsWith(bucketPathPrefix)) {
        return buildPublicFileUrl(normalizeStoragePath(parsedUrl.pathname));
      }
    } catch {
      return mediaPath;
    }

    return mediaPath;
  }

  if (mediaPath.startsWith("/") && LOCAL_APP_PATH_PREFIXES.some((prefix) => mediaPath.startsWith(prefix))) {
    return mediaPath;
  }

  return buildPublicFileUrl(normalizeStoragePath(mediaPath));
}
