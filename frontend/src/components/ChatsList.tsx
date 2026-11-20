import { useEffect } from "react";
import { useChatStore } from "../store/useChatStore";
import UsersLoadingSkeleton from "./UsersLoadingSkeleton";
import NoChatsFound from "./NoChatsFound";
import { useAuthStore } from "../store/useAuthStore";

function ChatsList() {
  const { getMyChatPartners, getUnreadCounts, unreadCounts, chats, isUsersLoading, setSelectedUser } = useChatStore();
  const { onlineUsers } = useAuthStore();

  useEffect(() => {
    getMyChatPartners();
    getUnreadCounts();
  }, [getMyChatPartners, getUnreadCounts]);

  if (isUsersLoading) return <UsersLoadingSkeleton />;
  if (chats.length === 0) return <NoChatsFound />;

  return (
    <>
      {chats.map((chat) => (
        <button
          type="button"
          key={chat._id}
          className="w-full bg-cyan-500/10 p-4 rounded-lg cursor-pointer hover:bg-cyan-500/20 transition-colors text-left"
          onClick={() => setSelectedUser(chat)}
        >
          <div className="flex items-center gap-3">
            <div className={`avatar ${onlineUsers.includes(chat._id) ? "online" : "offline"}`}>
              <div className="size-12 rounded-full">
                <img src={chat.profilePic || "/avatar.png"} alt={chat.username} />
              </div>
            </div>
            <h4 className={`min-w-0 flex-1 truncate text-slate-200 ${unreadCounts[`user:${chat._id}`] ? "font-bold" : "font-medium"}`}>
              {chat.username}
            </h4>
            {!!unreadCounts[`user:${chat._id}`] && (
              <span
                aria-label={`${unreadCounts[`user:${chat._id}`]} unread messages`}
                className="flex size-6 shrink-0 items-center justify-center rounded-full bg-cyan-500 text-xs font-bold text-slate-950"
              >
                {unreadCounts[`user:${chat._id}`] > 99 ? "99+" : unreadCounts[`user:${chat._id}`]}
              </span>
            )}
          </div>
        </button>
      ))}
    </>
  );
}
export default ChatsList;