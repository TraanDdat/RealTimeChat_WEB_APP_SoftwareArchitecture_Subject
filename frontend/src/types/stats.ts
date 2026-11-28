export type MessageType = "text" | "emotion" | "image" | "video" | "file";

export interface DailyActivity {
  date: string; // YYYY-MM-DD (user's local time zone)
  sent: number;
  received: number;
  onlineMinutes: number;
}

export interface SystemDailyMessages {
  date: string;
  private: number;
  group: number;
}

export interface SystemStats {
  totalUsers: number;
  totalGroups: number;
  onlineUsers: number;
  totalMessages: number;
  privateMessages: number;
  groupMessages: number;
  messagesInRange: number;
  messagesToday: number;
  daily: SystemDailyMessages[];
}

export interface TopContact {
  userId: number;
  username: string;
  profilePic: string | null;
  messages: number;
}

export interface UserStats {
  rangeDays: number;
  timezone: string;
  generatedAt: string;
  system: SystemStats;
  summary: {
    messagesSent: number;
    messagesReceived: number;
    groupMessagesSent: number;
    mediaShared: number;
    recalledMessages: number;
    onlineMinutes: number;
    activeDays: number;
    friends: number;
    groups: number;
  };
  daily: DailyActivity[];
  messageTypes: Record<MessageType, number>;
  hourly: number[]; // 24 buckets, local hour of day
  topContacts: TopContact[];
  lastActiveAt: string | null;
}
