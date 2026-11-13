import { axiosInstance } from "../lib/axios";

import type {
  AuthUser,
  SignUpData,
  LoginData,
  UpdateProfileData,
} from "../types/auth";

export const authApi = {
  AuthCheck: async () =>
    axiosInstance.get<AuthUser>("/auth/check"),

  signup: async (data: SignUpData) =>
    axiosInstance.post<AuthUser>("/auth/signup", data),

  login: async (data: LoginData) =>
    axiosInstance.post<AuthUser>("/auth/login", data),

  logout: async () =>
    axiosInstance.post("/auth/logout"),

  updateProfile: async (data: UpdateProfileData) =>
    axiosInstance.put<AuthUser>("/auth/update-profile", data),
};