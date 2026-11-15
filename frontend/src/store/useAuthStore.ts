import axios from "axios";
import { create } from "zustand";
import type { Socket } from "socket.io-client";
import toast from "react-hot-toast";
import { socketClient } from "../socket/socketClient";
import { socketService } from "../socket/socketServices";
import { authApi } from "../api/authApi";
import type { AuthUser, SignUpData, LoginData, UpdateProfileData, ApiErrorBody } from "../types/auth";

interface AuthStore {
  authUser: AuthUser | null;
  isCheckingAuth: boolean;
  isSigningUp: boolean;
  isLoggingIn: boolean;
  socket: Socket | null;
  onlineUsers: number[];
  AuthCheck: () => Promise<void>;
  signup: (data: SignUpData) => Promise<void>;
  login: (data: LoginData) => Promise<AuthUser | undefined>;
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

  AuthCheck: async () => {
    try {
      const res = await authApi.AuthCheck();
      set({ authUser: res.data });
      get().connectSocket();
    } catch (error) {
      console.log("Error in AuthCheck:", error);
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
      const res = await authApi.AuthCheck();
      set({ authUser: res.data });
      toast.success("Logged in successfully");
      get().connectSocket();
      return res.data;
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
      if (!authUser || socket?.connected) return;
      const newSocket = socketClient.create();
      // Listen to socket events before connecting
      socketService.listenEvents({
        onOnlineUsers: (user_id: number[]) => {
          set({ onlineUsers: user_id });
        } 
      });
      // Connect the socket if not already connected
      newSocket.on("connect", () => {
        console.log("Socket connected:", newSocket.id);
      });
      // Handle socket connection errors
      newSocket.on("connect_error", (error) => {
        console.error("Socket connection error:", error.message);
      });
      // Attempt to connect the socket if not already connected
      if(!newSocket.connected) {
        newSocket.connect();
      }
      set({ socket: newSocket });
  },

  disconnectSocket: () => {
    socketService.removeListeners();
    socketClient.disconnect();
    set({
      socket: null,
      onlineUsers: [],
    });
  }
}));