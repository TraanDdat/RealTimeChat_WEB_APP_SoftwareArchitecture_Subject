import { axiosInstance } from "../lib/axios";
import type { ChatGroup, CreateGroupData, GroupMessageData, SendMessageDataRequest } from "../types/chat";

export const groupApi = {
  getGroups: () => axiosInstance.get<ChatGroup[]>("/groups"),

  createGroup: (data: CreateGroupData) =>
    axiosInstance.post<ChatGroup>("/groups", data),

  addMember: (groupId: number, userId: number, role: "admin" | "member") =>
    axiosInstance.post<ChatGroup>(`/groups/${groupId}/add/members`, { user_id: userId, role }),

  removeMember: (groupId: number, userId: number, role: "member") =>
    axiosInstance.post<ChatGroup>(`/groups/${groupId}/remove/members`, { user_id: userId, role }),

  leaveMember: (groupId: number, userId: number, role: "member") =>
    axiosInstance.post<ChatGroup>(`/groups/${groupId}/leave/members`, { user_id: userId, role }),

  // updateMemberRole: (groupId: number, userId: number, role: "admin" | "member") =>
  //   axiosInstance.patch<ChatGroup>(`/groups/${groupId}/members/${userId}/role`, { role }),

  getMessages: (groupId: number) =>
    axiosInstance.get<GroupMessageData[]>(`/groups/${groupId}/messages`),

  deleteMessage: (groupId: number, messageId: string) =>
    axiosInstance.delete(`/groups/${groupId}/messages/${messageId}`),

  markRead: (groupId: number) =>
    axiosInstance.post<void>(`/groups/${groupId}/read`),

  sendMessage: (groupId: number, data: SendMessageDataRequest) =>
    axiosInstance.post<GroupMessageData>(`/groups/${groupId}/messages`, data),
};