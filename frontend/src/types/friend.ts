export interface AddFriendRequest {
    target_email: string;
}

export interface AddFriendResponse {
    id: number;
    email: string;
    username: string;
    status: string;
}

export interface FriendRequestItem {
    id: number;
    email: string;
    username: string;
    status: string;
}

export type SentFriendRequestStatus = "pending" | "accepted" | "rejected";

export interface SentFriendRequestItem extends FriendRequestItem {
    status: SentFriendRequestStatus;
}