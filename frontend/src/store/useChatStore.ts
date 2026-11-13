import axios from "axios";
import { create } from "zustand";
import toast from "react-hot-toast";
import { useAuthStore } from "./useAuthStore";
import type { ActiveTab, ChatUser, ChatMessage, SendMessageData, ApiErrorBody } from "../types/chat.ts"
import { chatApi } from "../api/chat.ts";

interface ChatStore {
  allContacts: ChatUser[];
  chats: ChatUser[];
  messages: ChatMessage[];
  activeTab: ActiveTab;
  selectedUser: ChatUser | null;
  isUsersLoading: boolean;
  isMessagesLoading: boolean;
  isSoundEnabled: boolean;
  toggleSound: () => void;
  setActiveTab: (tab: ActiveTab) => void;
  setSelectedUser: (selectedUser: ChatUser | null) => void;
  getAllContacts: () => Promise<void>;
  getMyChatPartners: () => Promise<void>;
  getMessagesByUserId: (userId: number) => Promise<void>;
  sendMessage: (messageData: SendMessageData) => Promise<void>;
  subscribeToMessages: () => void;
  unsubscribeFromMessages: () => void;
}

const getErrorMessage = (error: unknown) => {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return error.response?.data?.message ?? "Something went wrong";
  }
  return "Something went wrong";
};

let messageListener: ((message: ChatMessage) => void) | null = null;

export const useChatStore = create<ChatStore>()((set, get) => ({
  allContacts: [],
  chats: [],
  messages: [],
  activeTab: "chats",
  selectedUser: null,
  isUsersLoading: false,
  isMessagesLoading: false,
  isSoundEnabled: JSON.parse(localStorage.getItem("isSoundEnabled") ?? "false") === true,

  toggleSound: () => {
    const enabled = !get().isSoundEnabled;
    localStorage.setItem("isSoundEnabled", String(enabled));
    set({ isSoundEnabled: enabled });
  },

  setActiveTab: (tab) => set({ activeTab: tab }),

  setSelectedUser: (selectedUser) =>
    set({
    selectedUser,
    messages: [],
    isMessagesLoading: selectedUser !== null,
  }),

  getAllContacts: async () => {
    set({ isUsersLoading: true });
    try {
      const res = await chatApi.getContacts();
      set({ allContacts: res.data });
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      set({ isUsersLoading: false });
    }
  },

  getMyChatPartners: async () => {
    set({ isUsersLoading: true });
    try {
      const res = await chatApi.getChatPartners();
      set({ chats: res.data });
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      set({ isUsersLoading: false });
    }
  },

  getMessagesByUserId: async (userId) => {
    set({ isMessagesLoading: true });
    try {
      const res = await chatApi.getMessages(userId);
      if (get().selectedUser?._id !== userId) return;
      set({ messages: res.data });
    } catch (error) {
      if (get().selectedUser?._id === userId) {
        toast.error(getErrorMessage(error));
      }
    } finally {
        if (get().selectedUser?._id === userId) {
        set({ isMessagesLoading: false });
      }
    }
  },

  sendMessage: async (messageData) => {
    const { selectedUser } = get();
    const { authUser } = useAuthStore.getState();
    if (!selectedUser || !authUser) return;
    const receiverId = selectedUser._id;

    const tempId = `temp-${Date.now()}`;
    const optimisticMessage: ChatMessage = {
      _id: tempId,
      senderId: authUser._id,
      receiverId: selectedUser._id,
      text: messageData.text,
      image: messageData.image,
      createdAt: new Date().toISOString(),
      isOptimistic: true,
    };
    set((state) => ({
      messages: [...state.messages, optimisticMessage],
    }));

    try {
      const res = await chatApi.sendMessage(receiverId, messageData);
      if (get().selectedUser?._id !== receiverId) return;
      set((state) => ({
        messages: [
          ...state.messages.filter(
            (message) =>
              message._id !== tempId &&
              message._id !== res.data._id
          ), res.data, ],
      }));
    } catch (error) {
        if (get().selectedUser?._id === receiverId) {
          set((state) => ({
            messages: state.messages.filter(
              (message) => message._id !== tempId
            ),
          }));
        }
        toast.error(getErrorMessage(error));
    }
  },

  subscribeToMessages: () => {
    const { selectedUser } = get();
    if (!selectedUser) return;

    const socket = useAuthStore.getState().socket;
    if (!socket) return;

    // remove existing listener to listen the new one
    if (messageListener) {
      socket.off("newMessage", messageListener);
    }

  messageListener = (newMessage: ChatMessage) => {
    const { selectedUser, isSoundEnabled, messages } = get();

    if (!selectedUser) return;

    if (newMessage.senderId !== selectedUser._id) return;

    if (messages.some((message) => message._id === newMessage._id)) {
      return;
    }

    set((state) => ({
      messages: [...state.messages, newMessage],
    }));
    if (isSoundEnabled) {
      const notificationSound = new Audio("/sounds/notification.mp3");
      notificationSound.currentTime = 0;

      notificationSound.play().catch((error) => {
        console.log("Audio play failed:", error);
      });
    }
  };

  socket.on("newMessage", messageListener);
},

  unsubscribeFromMessages: () => {
    const socket = useAuthStore.getState().socket;
    if (socket && messageListener) {
      socket.off("newMessage", messageListener);
    }
    messageListener = null;
  },
}));