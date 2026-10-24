export type AccessStatus = "idle" | "loading" | "ready" | "refreshing" | "stale" | "error";

export type AccessErrorType =
  | "unauthorized"
  | "forbidden"
  | "offline"
  | "timeout"
  | "server"
  | "invalid-response";

export type AccessError = {
  message: string;
  retryable: boolean;
  status?: number;
  type: AccessErrorType;
};

const ERROR_MESSAGES: Record<AccessErrorType, string> = {
  unauthorized: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
  forbidden: "Bạn không có quyền truy cập trạng thái hiện tại của tài khoản.",
  offline: "Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng và thử lại.",
  timeout: "Máy chủ phản hồi quá lâu. Vui lòng thử lại.",
  server: "Hệ thống đang gặp sự cố khi tải quyền truy cập. Vui lòng thử lại.",
  "invalid-response": "Dữ liệu quyền truy cập không hợp lệ. Vui lòng thử lại.",
};

type ErrorLike = {
  kind?: unknown;
  message?: unknown;
  name?: unknown;
  status?: unknown;
};

function asErrorLike(error: unknown): ErrorLike | null {
  return error && typeof error === "object" ? error as ErrorLike : null;
}

function getErrorStatus(error: unknown): number | undefined {
  const status = asErrorLike(error)?.status;
  return typeof status === "number" ? status : undefined;
}

function getErrorKind(error: unknown): string | undefined {
  const kind = asErrorLike(error)?.kind;
  return typeof kind === "string" ? kind : undefined;
}

export function isAbortAccessError(error: unknown) {
  if (getErrorKind(error) === "abort") return true;
  return asErrorLike(error)?.name === "AbortError";
}

export function isUnauthorizedAccessError(error: unknown) {
  return getErrorStatus(error) === 401;
}

export function isForbiddenAccessError(error: unknown) {
  return getErrorStatus(error) === 403;
}

export function getAccessErrorMessage(errorType: AccessErrorType) {
  return ERROR_MESSAGES[errorType];
}

export function classifyAccessError(error: unknown): AccessError | null {
  if (isAbortAccessError(error)) return null;

  const status = getErrorStatus(error);
  const kind = getErrorKind(error);
  const rawMessage = asErrorLike(error)?.message;
  const fallbackMessage = typeof rawMessage === "string" && rawMessage.trim()
    ? rawMessage.trim()
    : undefined;

  if (kind === "timeout") {
    return {
      type: "timeout",
      retryable: true,
      message: fallbackMessage ?? ERROR_MESSAGES.timeout,
    };
  }

  if (kind === "network" || error instanceof TypeError) {
    return {
      type: "offline",
      retryable: true,
      message: fallbackMessage ?? ERROR_MESSAGES.offline,
    };
  }

  if (kind === "invalid-response") {
    return {
      type: "invalid-response",
      retryable: true,
      status,
      message: fallbackMessage ?? ERROR_MESSAGES["invalid-response"],
    };
  }

  if (status === 401) {
    return {
      type: "unauthorized",
      retryable: false,
      status,
      message: fallbackMessage ?? ERROR_MESSAGES.unauthorized,
    };
  }

  if (status === 403) {
    return {
      type: "forbidden",
      retryable: false,
      status,
      message: fallbackMessage ?? ERROR_MESSAGES.forbidden,
    };
  }

  return {
    type: "server",
    retryable: status === undefined || status === 429 || status >= 500,
    status,
    message: fallbackMessage ?? ERROR_MESSAGES.server,
  };
}

export function isResolvedAccessStatus(status: AccessStatus) {
  return status === "ready"
    || status === "refreshing"
    || status === "stale";
}

export function isAccessLoadingStatus(status: AccessStatus) {
  return status === "idle" || status === "loading";
}

export function shouldReplaceAccessSnapshot(currentRevision?: string, nextRevision?: string) {
  if (!nextRevision?.trim()) return true;
  return currentRevision !== nextRevision;
}
