import { axiosInstance } from "../lib/axios";
import type { ChatGroup, CreateGroupData, GroupMessageData, SendMessageData } from "../types/chat";

export const groupApi = {
  getGroups: () => axiosInstance.get<ChatGroup[]>("/groups"),

  createGroup: (data: CreateGroupData) =>
    axiosInstance.post<ChatGroup>("/groups", data),

  addMember: (groupId: number, userId: number, role: "admin" | "member") =>
    axiosInstance.post<ChatGroup>(`/groups/${groupId}/members`, { user_id: userId, role }),

  updateMemberRole: (groupId: number, userId: number, role: "admin" | "member") =>
    axiosInstance.patch<ChatGroup>(`/groups/${groupId}/members/${userId}/role`, { role }),

  getMessages: (groupId: number) =>
    axiosInstance.get<GroupMessageData[]>(`/groups/${groupId}/messages`),

  sendMessage: (groupId: number, data: SendMessageData) =>
    axiosInstance.post<GroupMessageData>(`/groups/${groupId}/messages`, data),
};