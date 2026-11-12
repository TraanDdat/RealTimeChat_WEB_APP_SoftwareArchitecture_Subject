import { normalizeApiError } from "../errors/HandlerError";
import { ApiError } from "../errors/ApiError";

export async function apiRequest<T = any>(
    url: string,
    options: RequestInit = {}
): Promise<T> {
    let response: Response;

    try {
        response = await fetch(`${url}`, {
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...options.headers,
            },
        });
    } catch (networkError: unknown) {
        throw new ApiError({
            message: "Cannot connect to server",
            code: "NETWORK_ERROR",
            status: 0,
            detail: networkError,
        });
    }

    let data: any = null;

    try {
        const contentType = response.headers.get("content-type");

        if (contentType?.includes("application/json")) {
            data = await response.json();
        } else {
            const text = await response.text();
            data = text ? { message: text } : null;
        }
    } catch {
        throw new ApiError({
            message: "Invalid server response",
            code: "INVALID_RESPONSE",
            status: response.status,
        });
    }

    if (!response.ok) {
        throw normalizeApiError(data, response.status);
    }

    return data as T;
}