import { useEffect, useState } from "react";
import { friendApi } from "../api/addfriend";
import type { SentFriendRequestItem, SentFriendRequestStatus } from "../types/friend";

const STATUS_STYLES: Record<SentFriendRequestStatus, { label: string; className: string }> = {
  pending: { label: "Pending", className: "bg-slate-700 text-slate-300" },
  accepted: { label: "Accepted", className: "bg-emerald-600/20 text-emerald-400" },
  rejected: { label: "Declined", className: "bg-red-600/20 text-red-400" },
};

interface SentRequestsPanelProps {
  /** Change this value to make the panel reload (e.g. after sending a request). */
  refreshKey?: number;
}

function SentRequestsPanel({ refreshKey = 0 }: SentRequestsPanelProps) {
  const [requests, setRequests] = useState<SentFriendRequestItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [error, setError] = useState(false);

  // Loaded over REST whenever the Contacts tab mounts or a new request is sent.
  useEffect(() => {
    let active = true;
    friendApi.getSentRequests()
      .then((response) => {
        if (!active) return;
        setRequests(response.data.data);
        setError(false);
      })
      .catch(() => {
        if (active) setError(true);
      });
    return () => {
      active = false;
    };
  }, [refreshKey]);

  const answeredCount = requests.filter((request) => request.status !== "pending").length;

  return (
    <section className="mb-4 border-b border-slate-700/70 pb-3">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        className="flex w-full items-center justify-between px-1 py-2 text-left text-sm text-slate-300"
      >
        <span>Sent requests</span>
        <span className="rounded-full bg-slate-700 px-2 py-0.5 text-xs" title={`${answeredCount} answered`}>
          {requests.length}
        </span>
      </button>
      {isOpen && (
        <div className="space-y-2">
          {error ? (
            <p className="px-1 text-xs text-red-400">Could not load sent requests</p>
          ) : requests.length === 0 ? (
            <p className="px-1 text-xs text-slate-500">You haven't sent any requests</p>
          ) : requests.map((request) => {
            const status = STATUS_STYLES[request.status] ?? STATUS_STYLES.pending;
            return (
              <div key={request.id} className="flex items-center gap-2 rounded-lg bg-slate-900/70 p-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-slate-200">{request.username}</p>
                  <p className="truncate text-xs text-slate-400">{request.email}</p>
                </div>
                <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs ${status.className}`}>
                  {status.label}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

export default SentRequestsPanel;
