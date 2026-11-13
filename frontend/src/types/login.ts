export interface LoginResponse {
    id: number;
    email: string;
    username: string;
    access_token: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}