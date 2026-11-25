import { axiosInstance } from "../lib/axios";
import type {
    AddFriendRequest,
    AddFriendResponse,
    FriendRequestItem,
    SentFriendRequestItem,
} from "../types/friend";

interface DataEnvelope<T> {
    data: T;
}

export const friendApi = {
    sendRequest: (data: AddFriendRequest) =>
        axiosInstance.post<DataEnvelope<AddFriendResponse>>("/user/addfriend/add", data),

    getRequests: () =>
        axiosInstance.get<DataEnvelope<FriendRequestItem[]>>("/user/addfriend/requests"),

    getSentRequests: () =>
        axiosInstance.get<DataEnvelope<SentFriendRequestItem[]>>("/user/addfriend/requests/sent"),

    acceptRequest: (requestId: number) =>
        axiosInstance.post(`/user/addfriend/requests/${requestId}/accept`),

    rejectRequest: (requestId: number) =>
        axiosInstance.post(`/user/addfriend/requests/${requestId}/reject`),

    deleteFriend: (userId: number) =>
        axiosInstance.post(
            "/user/addfriend/requests/deleted",
            null,
            { params: { other_user_id: userId } },
        ),

    // getFriends: () => axiosInstance.get<ChatUser[]>("/user/addfriend/friends"),
};