export type ApiErrorKind = "http" | "network" | "timeout" | "abort" | "invalid-response";

type ApiErrorOptions = {
  cause?: unknown;
  code?: string;
  kind: ApiErrorKind;
  status?: number;
};

export class ApiError extends Error {
  readonly code?: string;
  readonly kind: ApiErrorKind;
  readonly status?: number;

  constructor(message: string, options: ApiErrorOptions) {
    super(message, { cause: options.cause });
    this.name = "ApiError";
    this.kind = options.kind;
    this.status = options.status;
    this.code = options.code;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function isAbortLikeError(error: unknown) {
  if (isApiError(error)) return error.kind === "abort";
  return Boolean(
    error
    && typeof error === "object"
    && "name" in error
    && (error as { name?: unknown }).name === "AbortError",
  );
}
