import { ApiError } from "./ApiError";

export function normalizeApiError(data: any, status: any) {
    let message = "Request failed";
    let code = "UNKNOWN_ERROR";
    let detail = null;

    if (Array.isArray(data?.detail)) {
        // FastAPI validation error (422)
        message = data.detail
            .map((e: { msg?: string }) => e?.msg)
            .filter(Boolean)
            .join(", ") || message;

        code = "VAL_INVALID_REQUEST_BODY";
        detail = data.detail;

    } else if (
        typeof data?.detail === "object" &&
        data.detail !== null
    ) {
        // Custom FastAPI error
        message = data.detail.message || message;
        code = data.detail.code || code;
        detail = data.detail;

    } else if (typeof data?.detail === "string") {
        message = data.detail;

    } else if (typeof data?.message === "string") {
        message = data.message;
        code = data.code || code;
    }

    return new ApiError({
        message,
        code,
        status,
        detail,
    });
}