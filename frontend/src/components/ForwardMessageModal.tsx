import { useEffect, useMemo, useState } from "react";
import { FileTextIcon, ImageIcon, SearchIcon, UsersIcon, VideoIcon, XIcon } from "lucide-react";
import { chatApi } from "../api/chat";
import { groupApi } from "../api/groups";
import { useChatStore } from "../store/useChatStore";
import type { ChatGroup, ChatMessage, ChatUser } from "../types/chat";

interface ForwardMessageModalProps {
  message: ChatMessage;
  onClose: () => void;
}

function messagePreview(message: ChatMessage) {
  if (message.image) return { icon: <ImageIcon className="size-4" />, label: "Hình ảnh" };
  if (message.attachment && message.attachmentMimeType?.startsWith("video/")) {
    return { icon: <VideoIcon className="size-4" />, label: message.attachmentName ?? "Video" };
  }
  if (message.attachment) {
    return { icon: <FileTextIcon className="size-4" />, label: message.attachmentName ?? "Tài liệu" };
  }
  return { icon: null, label: message.emotion ?? message.text ?? "" };
}

function ForwardMessageModal({ message, onClose }: ForwardMessageModalProps) {
  const forwardMessage = useChatStore((state) => state.forwardMessage);
  const [contacts, setContacts] = useState<ChatUser[]>([]);
  const [groups, setGroups] = useState<ChatGroup[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [query, setQuery] = useState("");
  const [receiverIds, setReceiverIds] = useState<number[]>([]);
  const [groupIds, setGroupIds] = useState<number[]>([]);
  const [isSending, setIsSending] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.allSettled([chatApi.getContacts(), groupApi.getGroups()]).then(([contactsRes, groupsRes]) => {
      if (cancelled) return;
      if (contactsRes.status === "fulfilled") setContacts(contactsRes.value.data);
      if (groupsRes.status === "fulfilled") setGroups(groupsRes.value.data);
      setIsLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onClose]);

  const normalizedQuery = query.trim().toLowerCase();
  const visibleContacts = useMemo(
    () => contacts.filter((contact) => contact.username.toLowerCase().includes(normalizedQuery)),
    [contacts, normalizedQuery]
  );
  const visibleGroups = useMemo(
    () => groups.filter((group) => group.name.toLowerCase().includes(normalizedQuery)),
    [groups, normalizedQuery]
  );

  const toggle = (setter: typeof setReceiverIds, id: number) =>
    setter((selected) => (selected.includes(id) ? selected.filter((item) => item !== id) : [...selected, id]));

  const selectedCount = receiverIds.length + groupIds.length;
  const preview = messagePreview(message);

  const submit = async () => {
    if (selectedCount === 0 || isSending) return;
    setIsSending(true);
    const ok = await forwardMessage(message, receiverIds, groupIds);
    setIsSending(false);
    if (ok) onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="forward-title"
        onClick={(event) => event.stopPropagation()}
        className="flex max-h-[85vh] w-full max-w-md flex-col rounded-xl border border-slate-700 bg-slate-900 text-slate-200 shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-slate-700 px-4 py-3">
          <h2 id="forward-title" className="font-semibold text-slate-100">Chuyển tiếp tin nhắn</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="rounded p-1 text-slate-400 hover:bg-slate-800 hover:text-slate-100"
          >
            <XIcon className="size-5" />
          </button>
        </header>

        <div className="space-y-3 px-4 py-3">
          <div className="flex items-center gap-2 rounded-lg bg-slate-800 px-3 py-2 text-sm text-slate-300">
            {preview.icon}
            <span className="truncate">{preview.label}</span>
          </div>
          <label className="flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2">
            <SearchIcon className="size-4 text-slate-500" />
            <input
              autoFocus
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Tìm bạn bè hoặc nhóm"
              aria-label="Tìm bạn bè hoặc nhóm"
              className="w-full bg-transparent text-sm text-slate-100 outline-none placeholder:text-slate-500"
            />
          </label>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto px-4 pb-3">
          {isLoading ? (
            <p className="py-6 text-center text-sm text-slate-500">Đang tải...</p>
          ) : (
            <>
              <section>
                <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Bạn bè</h3>
                {visibleContacts.length === 0 ? (
                  <p className="text-sm text-slate-500">Không có bạn bè phù hợp</p>
                ) : (
                  visibleContacts.map((contact) => (
                    <label
                      key={contact._id}
                      className="flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2 hover:bg-slate-800"
                    >
                      <input
                        type="checkbox"
                        checked={receiverIds.includes(contact._id)}
                        onChange={() => toggle(setReceiverIds, contact._id)}
                        className="checkbox checkbox-sm checkbox-info"
                      />
                      <img
                        src={contact.profilePic || "/avatar.png"}
                        alt=""
                        className="size-8 rounded-full object-cover"
                      />
                      <span className="truncate text-sm">{contact.username}</span>
                    </label>
                  ))
                )}
              </section>
              <section>
                <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Nhóm</h3>
                {visibleGroups.length === 0 ? (
                  <p className="text-sm text-slate-500">Không có nhóm phù hợp</p>
                ) : (
                  visibleGroups.map((group) => (
                    <label
                      key={group.id}
                      className="flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2 hover:bg-slate-800"
                    >
                      <input
                        type="checkbox"
                        checked={groupIds.includes(group.id)}
                        onChange={() => toggle(setGroupIds, group.id)}
                        className="checkbox checkbox-sm checkbox-info"
                      />
                      <span className="flex size-8 items-center justify-center rounded-full bg-cyan-900/70 text-cyan-300">
                        <UsersIcon className="size-4" />
                      </span>
                      <span className="truncate text-sm">{group.name}</span>
                    </label>
                  ))
                )}
              </section>
            </>
          )}
        </div>

        <footer className="flex items-center justify-end gap-2 border-t border-slate-700 px-4 py-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg px-4 py-2 text-sm text-slate-300 hover:bg-slate-800"
          >
            Huỷ
          </button>
          <button
            type="button"
            onClick={submit}
            disabled={selectedCount === 0 || isSending}
            className="rounded-lg bg-cyan-600 px-4 py-2 text-sm font-medium text-white hover:bg-cyan-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSending ? "Đang gửi..." : `Chuyển tiếp${selectedCount ? ` (${selectedCount})` : ""}`}
          </button>
        </footer>
      </div>
    </div>
  );
}

export default ForwardMessageModal;
