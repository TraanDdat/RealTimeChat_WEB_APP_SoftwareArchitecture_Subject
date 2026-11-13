import { axiosInstance } from "../lib/axios";
import type { ChatUser, ChatMessage, SendMessageData } from "../types/chat";

export const chatApi = {
  getContacts: () =>
    axiosInstance.get<ChatUser[]>("/messages/contacts"),

  getChatPartners: () =>
    axiosInstance.get<ChatUser[]>("/messages/chats"),

  getMessages: (userId: number) =>
    axiosInstance.get<ChatMessage[]>(`/messages/${userId}`),

  sendMessage: (receiverId: number, data: SendMessageData) =>
    axiosInstance.post<ChatMessage>(
      `/messages/send/${receiverId}`,
      data
    ),
};