import { API_BASE_URL, API_AUTH_REGISTER } from "../../config";
import { apiRequest } from "./api_client";

interface RegisterRequest {
    email: string;
    username: string;
    password: string;
    role?: string;
}

interface RegisterResponse {
    email: string;
    role: string;
}

export async function react_register_request({
    email,
    username,
    password,
    role
}: RegisterRequest) {

    const loginUrl = `${API_BASE_URL}${API_AUTH_REGISTER}`;
    const body = {
        email,
        username,
        password,
        role
    };

    const package_res = await apiRequest(loginUrl, {
        method: "POST",
        body: JSON.stringify(body)
    });

    const data_res: RegisterResponse = {
      email: package_res.data.email,
      role: package_res.data.role
    };
    
    return data_res;
}