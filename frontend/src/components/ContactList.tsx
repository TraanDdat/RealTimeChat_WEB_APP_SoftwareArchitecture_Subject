import { useEffect, useState, type FormEvent } from "react";
import { UserMinusIcon } from "lucide-react";
import toast from "react-hot-toast";
import { useChatStore } from "../store/useChatStore";
import UsersLoadingSkeleton from "./UsersLoadingSkeleton";
import { useAuthStore } from "../store/useAuthStore";
import { friendApi } from "../api/addfriend";
import FriendRequestsPanel from "./FriendRequestsPanel";
import SentRequestsPanel from "./SentRequestsPanel";

function ContactList() {
  const { getAllContacts, allContacts, setSelectedUser, isUsersLoading } = useChatStore();
  const { onlineUsers } = useAuthStore();
  const [targetEmail, setTargetEmail] = useState("");
  const [deletingFriendId, setDeletingFriendId] = useState<number | null>(null);
  const [sentRequestsVersion, setSentRequestsVersion] = useState(0);

  useEffect(() => {
    // will be modify to only show list friend
    getAllContacts();
  }, [getAllContacts]);

  const sendRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const email = targetEmail.trim();
    if (!email) return;
    try {
      await friendApi.sendRequest({ target_email: email });
      toast.success("Friend request sent");
      setTargetEmail("");
      setSentRequestsVersion((version) => version + 1);
    } catch (error) {
      const message = (error as { response?: { data?: { detail?: string } } })
        .response?.data?.detail;
      toast.error(message ?? "Could not send friend request");
    }
  };

  const deleteFriend = async (event: React.MouseEvent<HTMLButtonElement>, userId: number, username: string) => {
    event.stopPropagation();
    if (!window.confirm(`Remove ${username} from your friends? Your chat history will remain.`)) return;

    setDeletingFriendId(userId);
    try {
      await friendApi.deleteFriend(userId);
      await getAllContacts();
      toast.success(`${username} removed from friends`);
    } catch (error) {
      const message = (error as { response?: { data?: { detail?: string } } })
        .response?.data?.detail;
      toast.error(message ?? "Could not remove friend");
    } finally {
      setDeletingFriendId(null);
    }
  };

  if (isUsersLoading) return <UsersLoadingSkeleton />;

  return (
    <>
      <form onSubmit={sendRequest} className="mb-4 flex gap-2">
        <input
          type="email"
          required
          value={targetEmail}
          onChange={(event) => setTargetEmail(event.target.value)}
          placeholder="Email to add"
          aria-label="Email to add as a friend"
          className="min-w-0 flex-1 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200"
        />
        <button type="submit" className="rounded-lg bg-cyan-600 px-3 text-sm text-white hover:bg-cyan-500">
          Invite
        </button>
      </form>
      <FriendRequestsPanel />
      <SentRequestsPanel refreshKey={sentRequestsVersion} />
      <h3 className="px-1 pb-1 text-xs font-semibold uppercase text-slate-400">Friends</h3>
      {allContacts.map((contact) => (
        <div
          key={contact._id}
          className="bg-cyan-500/10 p-4 rounded-lg cursor-pointer hover:bg-cyan-500/20 transition-colors"
          onClick={() => setSelectedUser(contact)}
        >
          <div className="flex items-center gap-3">
            <div className={`avatar ${onlineUsers.includes(contact._id) ? "online" : "offline"}`}>
              <div className="size-12 rounded-full">
                <img src={contact.profilePic || "/avatar.png"} alt={contact.username} />
              </div>
            </div>
            <h4 className="min-w-0 flex-1 truncate text-slate-200 font-medium">{contact.username}</h4>
            <button
              type="button"
              title={`Remove ${contact.username} from friends`}
              aria-label={`Remove ${contact.username} from friends`}
              disabled={deletingFriendId === contact._id}
              onClick={(event) => deleteFriend(event, contact._id, contact.username)}
              className="flex size-9 shrink-0 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
            >
              <UserMinusIcon className="size-4" />
            </button>
          </div>
        </div>
      ))}
    </>
  );
}
export default ContactList;