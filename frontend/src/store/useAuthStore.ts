import axios from "axios";
import { create } from "zustand";
import type { Socket } from "socket.io-client";
import toast from "react-hot-toast";
import { createSocket } from "../socket/socketClient";
import { authApi } from "../api/auth";
import type { AuthUser, SignUpData, LoginData, UpdateProfileData, ApiErrorBody } from "../types/auth";

interface AuthStore {
  authUser: AuthUser | null;
  isCheckingAuth: boolean;
  isSigningUp: boolean;
  isLoggingIn: boolean;
  socket: Socket | null;
  onlineUsers: number[];
  checkAuth: () => Promise<void>;
  signup: (data: SignUpData) => Promise<void>;
  login: (data: LoginData) => Promise<void>;
  logout: () => Promise<void>;
  updateProfile: (data: UpdateProfileData) => Promise<void>;
  connectSocket: () => void;
  disconnectSocket: () => void;
}

const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
};

export const useAuthStore = create<AuthStore>()((set, get) => ({
  authUser: null,
  isCheckingAuth: true,
  isSigningUp: false,
  isLoggingIn: false,
  socket: null,
  onlineUsers: [],

  checkAuth: async () => {
    try {
      const res = await authApi.checkAuth();
      set({ authUser: res.data });
      get().connectSocket();
    } catch (error) {
      console.log("Error in authCheck:", error);
      set({ authUser: null });
    } finally {
      set({ isCheckingAuth: false });
    }
  },

  signup: async (data) => {
    set({ isSigningUp: true });
    try {
      const res = await authApi.signup(data);
      set({ authUser: res.data });

      toast.success("Account created successfully!");
      get().connectSocket();
    } catch (error) {
      toast.error(getErrorMessage(error, "Unable to create account"));
    } finally {
      set({ isSigningUp: false });
    }
  },

  login: async (data) => {
    set({ isLoggingIn: true });
    try {
      await authApi.login(data);
      const res = await authApi.checkAuth();
      set({ authUser: res.data });

      toast.success("Logged in successfully");
      get().connectSocket();
    } catch (error) {
      toast.error(getErrorMessage(error, "Unable to log in"));
    } finally {
      set({ isLoggingIn: false });
    }
  },

  logout: async () => {
    try {
      await authApi.logout();
      set({ authUser: null });
      toast.success("Logged out successfully");
      get().disconnectSocket();
    } catch (error) {
      toast.error("Error logging out");
      console.log("Logout error:", error);
    }
  },

  updateProfile: async (data) => {
    try {
      const res = await authApi.updateProfile(data);
      set({ authUser: res.data });
      toast.success("Profile updated successfully");
    } catch (error) {
      console.log("Error in update profile:", error);
      toast.error(getErrorMessage(error, "Unable to update profile"));
    }
  },

  connectSocket: () => {
      const { authUser, socket } = get();
      if (!authUser || get().socket?.connected) return;
      if (socket)
        {
          socket.connect();
          return;
        };
      const newSocket = createSocket();
      newSocket.on("connect", () => {
          console.log("Socket connected:", newSocket.id);
      });
      newSocket.on("connect_error", (error) => {
          console.error("Socket connection error:", error.message);
      });
      newSocket.on("getOnlineUsers", (userIds: number[]) => {
          set({ onlineUsers: userIds });
      });
      set({ socket: newSocket });
  },

  disconnectSocket: () => {
    const socket = get().socket;
    if (socket) {
      socket.removeAllListeners();
      socket.disconnect();
      set({
        socket: null,
        onlineUsers: [],
      });
    }
  },
}));