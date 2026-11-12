export class ApiError extends Error {
    code: string;
    status: number;
    detail: any;

    constructor({
        message = "Something went wrong",
        code = "UNKNOWN_ERROR",
        status = 500,
        detail = null,
    }: {
        message?: string;
        code?: string;
        status?: number;
        detail?: any;
    } = {}) {
        super(message);

        this.name = "ApiError";
        this.code = code;
        this.status = status;
        this.detail = detail;

        Object.setPrototypeOf(this, ApiError.prototype);
    }
}