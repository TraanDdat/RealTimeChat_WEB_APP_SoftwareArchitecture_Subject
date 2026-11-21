import { useEffect, useRef } from "react";
import { Trash2Icon } from "lucide-react";
import { useAuthStore } from "../store/useAuthStore";
import { useChatStore } from "../store/useChatStore";
import ChatHeader from "./ChatHeader";
import GroupChatHeader from "./GroupChatHeader";
import NoChatHistoryPlaceholder from "./NoChatHistoryPlaceholder";
import MessageInput from "./MessageInput";
import MessagesLoadingSkeleton from "./MessagesLoadingSkeleton";

function ChatContainer() {
  const {
    selectedUser,
    selectedGroup,
    getMessagesByUserId,
    getGroupMessages,
    markUserMessagesRead,
    markGroupMessagesRead,
    deleteMessage,
    messages,
    isMessagesLoading,
  } = useChatStore();
  const { authUser } = useAuthStore();
  const messageEndRef = useRef<HTMLDivElement>(null);
  const userId = selectedUser?._id;
  const groupId = selectedGroup?.id;

  useEffect(() => {
    if (!userId && !groupId) return;
    if (groupId) {
      getGroupMessages(groupId);
      void markGroupMessagesRead(groupId);
    } else if (userId) {
      getMessagesByUserId(userId);
      void markUserMessagesRead(userId);
    }
  }, [userId, groupId, getMessagesByUserId, getGroupMessages, markUserMessagesRead, markGroupMessagesRead]);

  useEffect(() => {
    if (messageEndRef.current) {
      messageEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  if ((!selectedUser && !selectedGroup) || !authUser) return null;
  const currentUserId = String(authUser._id);

  return (
    <>
      {selectedGroup ? <GroupChatHeader /> : <ChatHeader />}
      <div className="flex-1 px-6 overflow-y-auto py-8">
        {messages.length > 0 && !isMessagesLoading ? (
          <div className="max-w-3xl mx-auto space-y-6">
            {messages.map((msg) => {
              const isOwnMessage = String(msg.senderId) === currentUserId;
              return (
                <div
                  key={msg._id}
                  className={`flex w-full ${isOwnMessage ? "justify-end" : "justify-start"}`}
                >
                  <div
                    className={`relative min-w-0 max-w-[75%] break-words rounded-lg px-4 py-3 ${
                      isOwnMessage ? "bg-cyan-600 text-white" : "bg-slate-800 text-slate-200"
                    }`}
                  >
                    {selectedGroup && !isOwnMessage && (
                      <p className="mb-1 text-xs font-semibold opacity-80">{msg.senderUsername}</p>
                    )}
                    {msg.image && (
                      <img src={msg.image} alt="Shared" className="rounded-lg h-48 object-cover" />
                    )}
                    {msg.attachment && msg.attachmentMimeType?.startsWith("video/") && (
                      <video
                        src={msg.attachment}
                        controls
                        preload="metadata"
                        className="max-h-64 max-w-full rounded-lg"
                      />
                    )}
                    {msg.attachment && !msg.attachmentMimeType?.startsWith("video/") && (
                      <a
                        href={msg.attachment}
                        target="_blank"
                        rel="noreferrer"
                        className="block max-w-64 truncate underline underline-offset-2"
                        download={msg.attachmentName ?? undefined}
                      >
                        {msg.attachmentName ?? "Download attachment"}
                      </a>
                    )}
                    {msg.emotion ? (
                        <div className="text-5xl leading-tight">
                            {msg.emotion}
                        </div>
                    ) : (
                        msg.text && <p className="mt-2">{msg.text}</p>
                    )}
                    <div className="mt-1 flex items-center justify-between gap-4 text-xs opacity-75">
                      {new Date(msg.createdAt).toLocaleTimeString(undefined, {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                      {isOwnMessage && !msg.isOptimistic && (
                        <button
                          type="button"
                          title="Delete message for everyone"
                          aria-label="Delete message for everyone"
                          onClick={() => {
                            if (window.confirm("Delete this message for everyone?")) {
                              void deleteMessage(msg._id);
                            }
                          }}
                          className="rounded p-1 text-current/70 transition-colors hover:bg-white/15 hover:text-current"
                        >
                          <Trash2Icon className="size-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
            <div ref={messageEndRef} />
          </div>
        ) : isMessagesLoading ? (
          <MessagesLoadingSkeleton />
        ) : (
          <NoChatHistoryPlaceholder name={selectedGroup?.name ?? selectedUser?.username ?? ""} />
        )}
      </div>

      <MessageInput />
    </>
  );
}

export default ChatContainer;