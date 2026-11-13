import { API_BASE_URL, API_AUTH_LOGIN } from "../../config";
import { apiRequest } from "./api_client";
import type { LoginRequest, LoginResponse } from "../types/login"

export async function react_login_request({
    email,
    password
}: LoginRequest) {

    const loginUrl = `${API_BASE_URL}${API_AUTH_LOGIN}`;
    const body = {
        email,
        password
    };

    const package_res = await apiRequest(loginUrl, {
        method: "POST",
        body: JSON.stringify(body)
    });

    const data_res: LoginResponse = {
      id: package_res.data.id,
      email: package_res.data.email,
      username: package_res.data.username,
      access_token: package_res.data.access_token
    };
    
    return data_res;
}