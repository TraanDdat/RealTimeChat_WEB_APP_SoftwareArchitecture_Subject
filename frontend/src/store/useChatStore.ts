import axios from "axios";
import { create } from "zustand";
import toast from "react-hot-toast";
import { useAuthStore } from "./useAuthStore";
import type {
  ActiveTab,
  ChatGroup,
  ChatUser,
  ChatMessage,
  SendMessageDataRequest,
  ApiErrorBody,
  UnreadCountsResponse,
} from "../types/chat.ts";
import { chatApi } from "../api/chat.ts";
import { groupApi } from "../api/groups.ts";

interface ChatStore {
  allContacts: ChatUser[];
  chats: ChatUser[];
  messages: ChatMessage[];
  unreadCounts: Record<string, number>;
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
  getUnreadCounts: () => Promise<void>;
  markUserMessagesRead: (userId: number) => Promise<void>;
  markGroupMessagesRead: (groupId: number) => Promise<void>;
  getGroups: () => Promise<void>;
  createGroup: (name: string, memberIds: number[]) => Promise<boolean>;
  addGroupMember: (groupId: number, userId: number, role: "admin" | "member") => Promise<void>;
  updateGroupMemberRole: (groupId: number, userId: number, role: "admin" | "member") => Promise<void>;
  refreshGroup: (groupId: number) => Promise<void>;
  getMessagesByUserId: (userId: number) => Promise<void>;
  getGroupMessages: (groupId: number) => Promise<void>;
  sendMessage: (messageData: SendMessageDataRequest) => Promise<void>;
  deleteMessage: (messageId: string) => Promise<void>;
  subscribeToMessages: () => void;
  unsubscribeFromMessages: () => void;
}

const getErrorMessage = (error: unknown) => {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return error.response?.data?.message ?? "Something went wrong";
  }
  return "Something went wrong";
};

const toUnreadMap = (counts: UnreadCountsResponse) => ({
  ...Object.fromEntries(Object.entries(counts.users).map(([id, count]) => [`user:${id}`, count])),
  ...Object.fromEntries(Object.entries(counts.groups).map(([id, count]) => [`group:${id}`, count])),
});

let messageListener: ((message: ChatMessage) => void) | null = null;
let groupMessageListener: ((message: ChatMessage) => void) | null = null;
let messageDeletedListener: ((payload: { messageId: string }) => void) | null = null;

export const useChatStore = create<ChatStore>()((set, get) => ({
  allContacts: [],
  chats: [],
  messages: [],
  unreadCounts: {},
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

  getUnreadCounts: async () => {
    try {
      const res = await chatApi.getUnreadCounts();
      set({ unreadCounts: toUnreadMap(res.data) });
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  },

  markUserMessagesRead: async (userId) => {
    try {
      await chatApi.markRead(userId);
      set((state) => ({
        unreadCounts: { ...state.unreadCounts, [`user:${userId}`]: 0 },
      }));
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  },

  markGroupMessagesRead: async (groupId) => {
    try {
      await groupApi.markRead(groupId);
      set((state) => ({
        unreadCounts: { ...state.unreadCounts, [`group:${groupId}`]: 0 },
      }));
    } catch (error) {
      toast.error(getErrorMessage(error));
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
      attachment: messageData.attachment,
      attachmentName: messageData.attachmentName,
      attachmentMimeType: messageData.attachmentMimeType,
      createdAt: new Date().toISOString(),
      isOptimistic: true,
      emotion: messageData.emotion ?? null,
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

  deleteMessage: async (messageId) => {
    const { selectedUser, selectedGroup } = get();
    if (!selectedUser && !selectedGroup) return;
    try {
      if (selectedGroup) await groupApi.deleteMessage(selectedGroup.id, messageId);
      else await chatApi.deleteMessage(messageId);
      set((state) => ({
        messages: state.messages.filter((message) => message._id !== messageId),
      }));
      toast.success("Message deleted");
    } catch (error) {
      toast.error(getErrorMessage(error));
    }
  },

  subscribeToMessages: () => {
    const socket = useAuthStore.getState().socket;
    if (!socket) return;

    // remove existing listener to listen the new one
    if (messageListener) {
      socket.off("newMessage", messageListener);
    }
    if (groupMessageListener) socket.off("newGroupMessage", groupMessageListener);
    if (messageDeletedListener) socket.off("messageDeleted", messageDeletedListener);

  messageListener = (newMessage: ChatMessage) => {
    const { selectedUser, isSoundEnabled, messages } = get();
    const isOpenConversation = Number(newMessage.senderId) === Number(selectedUser?._id);

    if (!isOpenConversation) {
      void get().getUnreadCounts();
      if (!get().chats.some((chat) => Number(chat._id) === Number(newMessage.senderId))) {
        void get().getMyChatPartners();
      }
      return;
    }

    if (messages.some((message) => message._id === newMessage._id)) return;
    set((state) => ({ messages: [...state.messages, newMessage] }));
    void get().markUserMessagesRead(newMessage.senderId);
    if (isSoundEnabled) {
      const notificationSound = new Audio("/sounds/notification.mp3");
      notificationSound.currentTime = 0;
      notificationSound.play().catch((error) => {
        console.log("Audio play failed:", error);
      });
    }
  };

  groupMessageListener = (newMessage: ChatMessage) => {
    const { selectedGroup, messages } = get();
    const currentUserId = useAuthStore.getState().authUser?._id;
    if (Number(newMessage.senderId) === Number(currentUserId)) return;
    const isOpenGroup = Number(newMessage.groupId) === Number(selectedGroup?.id);
    if (!isOpenGroup) {
      void get().getUnreadCounts();
      return;
    }

    if (messages.some((message) => message._id === newMessage._id)) return;
    set((state) => ({
      messages: [...state.messages, newMessage],
    }));
    void get().markGroupMessagesRead(newMessage.groupId!);
  };

  messageDeletedListener = ({ messageId }) => {
    set((state) => ({
      messages: state.messages.filter((message) => message._id !== messageId),
    }));
    void get().getUnreadCounts();
  };

  socket.on("newMessage", messageListener);
  socket.on("newGroupMessage", groupMessageListener);
  socket.on("messageDeleted", messageDeletedListener);
},

  unsubscribeFromMessages: () => {
    const socket = useAuthStore.getState().socket;
    if (socket && messageListener) {
      socket.off("newMessage", messageListener);
    }
    if (socket && groupMessageListener) socket.off("newGroupMessage", groupMessageListener);
    if (socket && messageDeletedListener) socket.off("messageDeleted", messageDeletedListener);
    messageListener = null;
    groupMessageListener = null;
    messageDeletedListener = null;
  },
}));