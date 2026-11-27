import { useCallback, useEffect, useRef, useState } from "react";
import { ForwardIcon, Trash2Icon } from "lucide-react";
import type { ChatMessage } from "../types/chat";
import ForwardMessageModal from "./ForwardMessageModal";
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
    isOtherUserTyping,
  } = useChatStore();
  const { authUser } = useAuthStore();
  const messageEndRef = useRef<HTMLDivElement>(null);
  const [forwardingMessage, setForwardingMessage] = useState<ChatMessage | null>(null);
  const closeForwardModal = useCallback(() => setForwardingMessage(null), []);
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
                      msg.isRecalled
                        ? "border border-slate-600 bg-transparent text-slate-400"
                        : isOwnMessage
                          ? "bg-cyan-600 text-white"
                          : "bg-slate-800 text-slate-200"
                    }`}
                  >
                    {selectedGroup && !isOwnMessage && (
                      <p className="mb-1 text-xs font-semibold opacity-80">{msg.senderUsername}</p>
                    )}
                    {msg.isForwarded && !msg.isRecalled && (
                      <p className="mb-1 flex items-center gap-1 text-xs italic opacity-70">
                        <ForwardIcon className="size-3" />
                        Đã chuyển tiếp
                      </p>
                    )}
                    {msg.isRecalled && (
                      <p className="italic opacity-70">Tin nhắn đã được thu hồi</p>
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
                      {!msg.isOptimistic && !msg.isRecalled && (
                        <span className="flex items-center gap-1">
                          <button
                            type="button"
                            title="Chuyển tiếp"
                            aria-label="Chuyển tiếp tin nhắn"
                            onClick={() => setForwardingMessage(msg)}
                            className="rounded p-1 text-current/70 transition-colors hover:bg-white/15 hover:text-current"
                          >
                            <ForwardIcon className="size-3.5" />
                          </button>
                          {isOwnMessage && (
                            <button
                              type="button"
                              title="Thu hồi tin nhắn"
                              aria-label="Thu hồi tin nhắn"
                              onClick={() => {
                                if (window.confirm("Thu hồi tin nhắn này với mọi người?")) {
                                  void deleteMessage(msg._id);
                                }
                              }}
                              className="rounded p-1 text-current/70 transition-colors hover:bg-white/15 hover:text-current"
                            >
                              <Trash2Icon className="size-3.5" />
                            </button>
                          )}
                        </span>
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
      {!selectedGroup && isOtherUserTyping && (
        <div className="px-6 py-2 text-sm italic text-slate-400">
          {selectedUser?.username} đang nhập...
        </div>
      )}
      <MessageInput />
      {forwardingMessage && (
        <ForwardMessageModal message={forwardingMessage} onClose={closeForwardModal} />
      )}
    </>
  );
}

export default ChatContainer;