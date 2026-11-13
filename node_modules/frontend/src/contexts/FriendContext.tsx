export interface AddFriendRequest {
    current_email: string
    target_email: string
}

export interface AddFriendResponse {
    id: number
    email: string
    username: string
    status: string
}

export interface FriendListRequest {
    email: string;
}

export interface FriendListResponse {
    id: number
    email: string
    username: string
    status: string
}