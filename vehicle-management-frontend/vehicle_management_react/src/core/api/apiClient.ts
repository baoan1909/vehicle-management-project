import { appConfig } from "@/config/env";
import { ApiError, isAbortLikeError, isApiError } from "@/core/api/apiError";
import { localizeApiMessage, localizeApiResponseBody } from "@/core/api/apiMessage";
import { getValidAccessToken, renewToken } from "@/core/auth/oidcSession";

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  skipAuth?: boolean;
  timeoutMs?: number;
};

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { skipAuth, timeoutMs, signal: externalSignal, ...requestOptions } = options;
  const controller = new AbortController();
  let timedOut = false;
  let timeoutId: number | undefined;

  const abortFromExternalSignal = () => controller.abort(externalSignal?.reason);
  if (externalSignal?.aborted) {
    controller.abort(externalSignal.reason);
  } else {
    externalSignal?.addEventListener("abort", abortFromExternalSignal, { once: true });
  }

  if (timeoutMs && timeoutMs > 0) {
    timeoutId = window.setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, timeoutMs);
  }

  try {
    const accessToken = skipAuth ? null : await getValidAccessToken();
    let response = await sendJsonRequest(path, { ...requestOptions, signal: controller.signal }, accessToken);

    if (!skipAuth && response.status === 401 && accessToken) {
      let refreshedToken: string | null;
      try {
        refreshedToken = (await renewToken())?.access_token ?? null;
      } catch (refreshError) {
        throw mapRefreshFailure(refreshError);
      }

      if (refreshedToken) {
        response = await sendJsonRequest(path, { ...requestOptions, signal: controller.signal }, refreshedToken);
      }
    }

    const contentType = response.headers.get("content-type") ?? "";
    let responseBody: unknown = null;
    let bodyParseError: unknown;

    if (contentType.includes("application/json")) {
      try {
        responseBody = await response.json();
      } catch (error) {
        bodyParseError = error;
      }
    }

    if (!response.ok) {
      const rawMessage =
        responseBody
        && typeof responseBody === "object"
        && "message" in responseBody
        && typeof responseBody.message === "string"
          ? responseBody.message
          : null;

      const errorCode =
        responseBody
        && typeof responseBody === "object"
        && "errorCode" in responseBody
        && typeof responseBody.errorCode === "string"
          ? responseBody.errorCode
          : undefined;

      throw new ApiError(localizeApiMessage(rawMessage, response.status), {
        code: errorCode,
        kind: "http",
        status: response.status,
      });
    }

    if (bodyParseError) {
      throw new ApiError("Máy chủ trả về dữ liệu không đúng định dạng.", {
        cause: bodyParseError,
        kind: "invalid-response",
        status: response.status,
      });
    }

    if (responseBody === null) {
      throw new ApiError("Máy chủ trả về dữ liệu không đúng định dạng.", {
        kind: "invalid-response",
        status: response.status,
      });
    }

    return localizeApiResponseBody(responseBody, response.status) as T;
  } catch (error) {
    if (isApiError(error)) throw error;

    if (isAbortLikeError(error)) {
      if (timedOut) {
        throw new ApiError("Yêu cầu tải dữ liệu đã quá thời gian chờ.", {
          cause: error,
          kind: "timeout",
        });
      }

      throw new ApiError("Yêu cầu đã bị hủy.", {
        cause: error,
        kind: "abort",
      });
    }

    if (error instanceof TypeError) {
      throw new ApiError("Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối mạng.", {
        cause: error,
        kind: "network",
      });
    }

    throw error;
  } finally {
    if (timeoutId !== undefined) {
      window.clearTimeout(timeoutId);
    }
    externalSignal?.removeEventListener("abort", abortFromExternalSignal);
  }
}

function mapRefreshFailure(error: unknown): ApiError {
  if (isApiError(error)) {
    if (
      error.kind === "network"
      || error.kind === "timeout"
      || error.kind === "abort"
      || error.kind === "invalid-response"
      || (error.kind === "http" && (error.status ?? 0) >= 500)
    ) {
      return error;
    }
  }

  if (isAbortLikeError(error)) {
    return new ApiError("Yêu cầu làm mới phiên đăng nhập đã bị hủy.", {
      cause: error,
      kind: "abort",
    });
  }

  const message = error instanceof Error && error.message
    ? error.message
    : "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";

  return new ApiError(message, {
    cause: error,
    kind: "http",
    status: 401,
  });
}

function sendJsonRequest(path: string, options: Omit<RequestOptions, "skipAuth" | "timeoutMs">, accessToken: string | null) {
  const isMultipart = options.body instanceof FormData;
  const requestBody = options.body instanceof FormData
    ? options.body
    : options.body
      ? JSON.stringify(options.body)
      : undefined;
  return fetch(`${appConfig.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      ...(!isMultipart ? { "Content-Type": "application/json" } : {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
    body: requestBody,
  });
}
