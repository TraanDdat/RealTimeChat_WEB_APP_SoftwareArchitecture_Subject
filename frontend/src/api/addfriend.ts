<<<<<<< HEAD
import { API_BASE_URL, API_USER_ADDFRIEND_ADD, API_USER_ADDFRIEND_REQUEST } from "../../config";
import { apiRequest } from "./api_client";
import type { AddFriendRequest, AddFriendResponse, FriendListRequest, FriendListResponse } from "../types/friend"
=======
import { axiosInstance } from "../lib/axios";
import type { ChatUser } from "../types/chat";
import type { AddFriendRequest, AddFriendResponse, FriendRequestItem } from "../types/friend";
>>>>>>> feature/trieu-move-db-to-aws

interface DataEnvelope<T> {
    data: T;
}

export const friendApi = {
    sendRequest: (data: AddFriendRequest) =>
        axiosInstance.post<DataEnvelope<AddFriendResponse>>("/user/addfriend/add", data),

    getRequests: () =>
        axiosInstance.get<DataEnvelope<FriendRequestItem[]>>("/user/addfriend/requests"),

    acceptRequest: (requestId: number) =>
        axiosInstance.post(`/user/addfriend/requests/${requestId}/accept`),

    rejectRequest: (requestId: number) =>
        axiosInstance.post(`/user/addfriend/requests/${requestId}/reject`),

    getFriends: () => axiosInstance.get<ChatUser[]>("/user/addfriend/friends"),
};