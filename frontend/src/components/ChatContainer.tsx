import { useEffect, useRef } from "react";
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
    messages,
    isMessagesLoading,
    subscribeToMessages,
    unsubscribeFromMessages,
  } = useChatStore();
  const { authUser } = useAuthStore();
  const messageEndRef = useRef<HTMLDivElement>(null);
  const userId = selectedUser?._id;
  const groupId = selectedGroup?.id;

  useEffect(() => {
    if (!userId && !groupId) return;
    if (groupId) getGroupMessages(groupId);
    else if (userId) getMessagesByUserId(userId);
    subscribeToMessages();

    return () => unsubscribeFromMessages();
  }, [userId, groupId, getMessagesByUserId, getGroupMessages, subscribeToMessages, unsubscribeFromMessages]);

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
                    {msg.text && <p className="mt-2">{msg.text}</p>}
                    <p className="text-xs mt-1 opacity-75 flex items-center gap-1">
                      {new Date(msg.createdAt).toLocaleTimeString(undefined, {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </p>
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