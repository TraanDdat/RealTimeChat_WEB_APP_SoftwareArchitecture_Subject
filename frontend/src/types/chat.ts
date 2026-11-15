export type ActiveTab = "chats" | "contacts" | "groups";

export interface ChatUser {
  _id: number;
  username: string;
  email: string;
  profilePic: string | null;
}

export interface ChatMessage {
  _id: string;
  senderId: number;
  receiverId?: number;
  groupId?: number;
  senderUsername?: string;
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

export interface GroupMember {
  user_id: number;
  username: string;
  email: string;
  role: "admin" | "member";
}

export interface ChatGroup {
  id: number;
  name: string;
  created_by: number;
  created_at: string;
  members: GroupMember[];
}

export interface CreateGroupData {
  name: string;
  member_ids: number[];
}

export interface GroupMessageData extends ChatMessage {
  groupId: number;
  senderUsername: string;
}