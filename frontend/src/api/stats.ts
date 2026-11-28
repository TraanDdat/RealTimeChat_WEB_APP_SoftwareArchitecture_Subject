import { axiosInstance } from "../lib/axios";
import type { UserStats } from "../types/stats";

export const statsApi = {
  getMyStats: (days: number) =>
    axiosInstance.get<UserStats>("/stats/me", {
      params: {
        days,
        tz: Intl.DateTimeFormat().resolvedOptions().timeZone,
      },
    }),
};
