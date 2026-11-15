import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { friendApi } from "../api/addfriend";
import type { FriendRequestItem } from "../types/friend";
import { useChatStore } from "../store/useChatStore";
import { useAuthStore } from "../store/useAuthStore";

function FriendRequestsPanel() {
  const [requests, setRequests] = useState<FriendRequestItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const { getAllContacts } = useChatStore();
  const socket = useAuthStore((state) => state.socket);

  useEffect(() => {
    let active = true;
    friendApi.getRequests()
      .then((response) => {
        if (active) setRequests(response.data.data);
      })
      .catch(() => {
        if (active) toast.error("Could not load friend requests");
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!socket) return;
    const handleFriendRequest = () => {
      friendApi.getRequests()
        .then((response) => setRequests(response.data.data))
        .catch(() => toast.error("Could not refresh friend requests"));
    };
    socket.on("friendRequest", handleFriendRequest);
    return () => {
      socket.off("friendRequest", handleFriendRequest);
    };
  }, [socket]);

  const respond = async (requestId: number, accept: boolean) => {
    try {
      if (accept) await friendApi.acceptRequest(requestId);
      else await friendApi.rejectRequest(requestId);
      setRequests((current) => current.filter((request) => request.id !== requestId));
      if (accept) await getAllContacts();
      toast.success(accept ? "Friend request accepted" : "Friend request declined");
    } catch {
      toast.error("Could not update friend request");
    }
  };

  return (
    <section className="mb-4 border-b border-slate-700/70 pb-3">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        className="flex w-full items-center justify-between px-1 py-2 text-left text-sm text-slate-300"
      >
        <span>Friend requests</span>
        <span className="rounded-full bg-slate-700 px-2 py-0.5 text-xs">{requests.length}</span>
      </button>
      {isOpen && (
        <div className="space-y-2">
          {requests.length === 0 ? (
            <p className="px-1 text-xs text-slate-500">No pending requests</p>
          ) : requests.map((request) => (
            <div key={request.id} className="rounded-lg bg-slate-900/70 p-3">
              <p className="truncate text-sm text-slate-200">{request.username}</p>
              <p className="truncate text-xs text-slate-400">{request.email}</p>
              <div className="mt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => respond(request.id, true)}
                  className="rounded bg-cyan-600 px-2 py-1 text-xs text-white hover:bg-cyan-500"
                >
                  Accept
                </button>
                <button
                  type="button"
                  onClick={() => respond(request.id, false)}
                  className="rounded border border-slate-600 px-2 py-1 text-xs text-slate-300 hover:bg-slate-800"
                >
                  Decline
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

export default FriendRequestsPanel;