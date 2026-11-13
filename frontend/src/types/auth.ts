export interface AuthUser {
  _id: number;
  username: string;
  email: string;
  profilePic: string | null;
}

export interface SignUpData {
  username: string;
  email: string;
  password: string;
}

export interface LoginData {
  email: string;
  password: string;
  role: string;
}

export interface UpdateProfileData {
  profilePic: string;
}

export interface ApiErrorBody {
  message?: string;
}