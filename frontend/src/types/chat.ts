export type ActiveTab = "chats" | "contacts";

export interface ChatUser {
  _id: number;
  username: string;
  email: string;
  profilePic: string | null;
}

export interface ChatMessage {
  _id: string;
  senderId: number;
  receiverId: number;
  text: string | null;
  image: string | null;
  createdAt: string;
  updatedAt?: string;
  isOptimistic?: boolean;
}

export interface SendMessageData {
  text: string;
  image: string | null;
}

export interface ApiErrorBody {
  message?: string;
}