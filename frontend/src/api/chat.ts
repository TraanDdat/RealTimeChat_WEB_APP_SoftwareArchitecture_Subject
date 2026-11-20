import { axiosInstance } from "../lib/axios";
import type { ChatUser, ChatMessage, SendMessageDataRequest, UnreadCountsResponse } from "../types/chat";

export const chatApi = {
  getContacts: () =>
    axiosInstance.get<ChatUser[]>("/messages/contacts"),

  getChatPartners: () =>
    axiosInstance.get<ChatUser[]>("/messages/chats"),

  getUnreadCounts: () =>
    axiosInstance.get<UnreadCountsResponse>("/messages/unread-counts"),

  markRead: (userId: number) =>
    axiosInstance.post<void>(`/messages/${userId}/read`),

  getMessages: (userId: number) =>
    axiosInstance.get<ChatMessage[]>(`/messages/${userId}`),

  sendMessage: (receiverId: number, data: SendMessageDataRequest) =>
    axiosInstance.post<ChatMessage>(
      `/messages/send/${receiverId}`,
      data
    ),
};