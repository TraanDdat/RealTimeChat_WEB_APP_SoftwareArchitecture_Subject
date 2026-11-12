export interface LoginResponse {
    id: number;
    email: string;
    username: string;
    role: string;
    access_token: string;
}

export interface LoginRequest {
    email: String;
    password: String;
    role: String
}