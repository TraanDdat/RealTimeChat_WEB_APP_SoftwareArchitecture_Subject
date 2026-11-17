export interface AuthUser {
  _id: number;
  username: string;
  email: string;
  profilePic: string | null;
  access_token: string | null;
}

export interface SignUpData {
  username: string;
  email: string;
  password: string;
}

export interface LoginData {
  email: string;
  password: string;
}

export interface UpdateProfileData {
  profilePic: string;
}

export interface ApiErrorBody {
  message?: string;
}