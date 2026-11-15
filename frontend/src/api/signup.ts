import { API_BASE_URL, API_AUTH_SIGNUP } from "../../config";
import { apiRequest } from "./api_client";
import type { RegisterRequest, RegisterResponse } from "../types/signup"

export async function react_register_request({
    email,
    username,
    password
}: RegisterRequest) {

    const loginUrl = `${API_BASE_URL}${API_AUTH_SIGNUP}`;
    const body = {
        email,
        username,
        password
    };

    const package_res = await apiRequest(loginUrl, {
        method: "POST",
        body: JSON.stringify(body)
    });

    const data_res: RegisterResponse = {
      email: package_res.data.email
    };
    
    return data_res;
}