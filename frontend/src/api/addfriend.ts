import { API_BASE_URL, API_USER_ADDFRIEND_ADD, API_USER_ADDFRIEND_REQUEST } from "../../config";
import { apiRequest } from "./api_client";
import type { AddFriendRequest, AddFriendResponse, FriendListRequest, FriendListResponse } from "../contexts/friend"

export async function react_addfriend_request({
    current_email,
    target_email
}: AddFriendRequest) {

    const loginUrl = `${API_BASE_URL}${API_USER_ADDFRIEND_ADD}`;
    const body = {
        current_email, target_email
    };

    const package_res = await apiRequest(loginUrl, {
        method: "POST",
        body: JSON.stringify(body)
    });

    const data_res: AddFriendResponse = {
      id: package_res.data.id,
      email: package_res.data.email,
      username: package_res.data.username,
      status: package_res.data.status
    };
    
    return data_res;
}

export async function react_getaddfriend_request({email}: FriendListRequest) {

    const url = `${API_BASE_URL}${API_USER_ADDFRIEND_REQUEST}?email=${encodeURIComponent(email)}`;

    const package_res = await apiRequest(url, {
        method: "GET"
    });

    const data_res: FriendListResponse[] = package_res.data;

    return data_res;
}