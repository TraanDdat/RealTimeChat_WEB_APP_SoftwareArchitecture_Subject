import axios from "axios";
import { create } from "zustand";
import toast from "react-hot-toast";
import { useAuthStore } from "./useAuthStore";
import type {
  ActiveTab,
  ChatGroup,
  ChatUser,
  ChatMessage,
  SendMessageData,
  ApiErrorBody,
} from "../types/chat.ts";
import { chatApi } from "../api/chat.ts";
import { groupApi } from "../api/groups.ts";

interface ChatStore {
  allContacts: ChatUser[];
  chats: ChatUser[];
  messages: ChatMessage[];
  activeTab: ActiveTab;
  selectedUser: ChatUser | null;
  groups: ChatGroup[];
  selectedGroup: ChatGroup | null;
  isUsersLoading: boolean;
  isMessagesLoading: boolean;
  isSoundEnabled: boolean;
  toggleSound: () => void;
  setActiveTab: (tab: ActiveTab) => void;
  setSelectedUser: (selectedUser: ChatUser | null) => void;
  setSelectedGroup: (selectedGroup: ChatGroup | null) => void;
  getAllContacts: () => Promise<void>;
  getMyChatPartners: () => Promise<void>;
  getGroups: () => Promise<void>;
  createGroup: (name: string, memberIds: number[]) => Promise<boolean>;
  addGroupMember: (groupId: number, userId: number, role: "admin" | "member") => Promise<void>;
  updateGroupMemberRole: (groupId: number, userId: number, role: "admin" | "member") => Promise<void>;
  refreshGroup: (groupId: number) => Promise<void>;
  getMessagesByUserId: (userId: number) => Promise<void>;
  getGroupMessages: (groupId: number) => Promise<void>;
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
let groupMessageListener: ((message: ChatMessage) => void) | null = null;

export const useChatStore = create<ChatStore>()((set, get) => ({
  allContacts: [],
  chats: [],
  messages: [],
  activeTab: "chats",
  selectedUser: null,
  groups: [],
  selectedGroup: null,
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
    selectedGroup: null,
    messages: [],
    isMessagesLoading: selectedUser !== null,
  }),

  setSelectedGroup: (selectedGroup) =>
    set({
      selectedGroup,
      selectedUser: null,
      messages: [],
      isMessagesLoading: selectedGroup !== null,
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

  getGroups: async () => {
    set({ isUsersLoading: true });
    try {
      const res = await groupApi.getGroups();
      set({ groups: res.data });
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      set({ isUsersLoading: false });
    }
  },

  createGroup: async (name, memberIds) => {
    try {
      const res = await groupApi.createGroup({ name, member_ids: memberIds });
      set((state) => ({ groups: [res.data, ...state.groups] }));
      get().setSelectedGroup(res.data);
      toast.success("Group created");
      return true;
    } catch (error) {
      toast.error(getErrorMessage(error));
      return false;
    }
  },

  addGroupMember: async (groupId, userId, role) => {
    try {
      const res = await groupApi.addMember(groupId, userId, role);
      set((state) => ({
        groups: state.groups.map((group) => group.id === groupId ? res.data : group),
        selectedGroup: state.selectedGroup?.id === groupId ? res.data : state.selectedGroup,
      }));
      toast.success("Member added");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  },

  updateGroupMemberRole: async (groupId, userId, role) => {
    try {
      const res = await groupApi.updateMemberRole(groupId, userId, role);
      set((state) => ({
        groups: state.groups.map((group) => group.id === groupId ? res.data : group),
        selectedGroup: state.selectedGroup?.id === groupId ? res.data : state.selectedGroup,
      }));
      toast.success("Member role updated");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  },

  refreshGroup: async (groupId) => {
    try {
      const res = await groupApi.getGroups();
      const group = res.data.find((item) => item.id === groupId);
      if (!group) return;
      set((state) => ({
        groups: state.groups.map((item) => item.id === groupId ? group : item),
        selectedGroup: state.selectedGroup?.id === groupId ? group : state.selectedGroup,
      }));
    } catch (error) {
      toast.error(getErrorMessage(error));
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

  getGroupMessages: async (groupId) => {
    set({ isMessagesLoading: true });
    try {
      const res = await groupApi.getMessages(groupId);
      if (get().selectedGroup?.id !== groupId) return;
      set({ messages: res.data });
    } catch (error) {
      if (get().selectedGroup?.id === groupId) toast.error(getErrorMessage(error));
    } finally {
      if (get().selectedGroup?.id === groupId) set({ isMessagesLoading: false });
    }
  },

  sendMessage: async (messageData) => {
    const { selectedUser, selectedGroup } = get();
    const { authUser } = useAuthStore.getState();
    if ((!selectedUser && !selectedGroup) || !authUser) return;
    const conversationId = selectedGroup?.id ?? selectedUser!._id;

    const tempId = `temp-${Date.now()}`;
    const optimisticMessage: ChatMessage = {
      _id: tempId,
      senderId: authUser._id,
      ...(selectedGroup ? { groupId: selectedGroup.id, senderUsername: authUser.username } : { receiverId: selectedUser!._id }),
      text: messageData.text,
      image: messageData.image,
      createdAt: new Date().toISOString(),
      isOptimistic: true,
    };
    set((state) => ({
      messages: [...state.messages, optimisticMessage],
    }));

    try {
      const res = selectedGroup
        ? await groupApi.sendMessage(selectedGroup.id, messageData)
        : await chatApi.sendMessage(selectedUser!._id, messageData);
      if ((get().selectedGroup?.id ?? get().selectedUser?._id) !== conversationId) return;
      set((state) => ({
        messages: [
          ...state.messages.filter(
            (message) =>
              message._id !== tempId &&
              message._id !== res.data._id
          ), res.data, ],
      }));
    } catch (error) {
        if ((get().selectedGroup?.id ?? get().selectedUser?._id) === conversationId) {
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
    const { selectedUser, selectedGroup } = get();
    if (!selectedUser && !selectedGroup) return;

    const socket = useAuthStore.getState().socket;
    if (!socket) return;

    // remove existing listener to listen the new one
    if (messageListener) {
      socket.off("newMessage", messageListener);
    }
    if (groupMessageListener) socket.off("newGroupMessage", groupMessageListener);

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

  groupMessageListener = (newMessage: ChatMessage) => {
    const { selectedGroup, isSoundEnabled, messages } = get();
    if (!selectedGroup || newMessage.groupId !== selectedGroup.id) return;
    if (messages.some((message) => message._id === newMessage._id)) return;
    set((state) => ({ messages: [...state.messages, newMessage] }));
    if (isSoundEnabled) {
      const notificationSound = new Audio("/sounds/notification.mp3");
      notificationSound.play().catch(() => undefined);
    }
  };

  socket.on("newMessage", messageListener);
  socket.on("newGroupMessage", groupMessageListener);
},

  unsubscribeFromMessages: () => {
    const socket = useAuthStore.getState().socket;
    if (socket && messageListener) {
      socket.off("newMessage", messageListener);
    }
    if (socket && groupMessageListener) socket.off("newGroupMessage", groupMessageListener);
    messageListener = null;
    groupMessageListener = null;
  },
}));