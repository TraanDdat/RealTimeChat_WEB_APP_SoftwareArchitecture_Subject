import { useEffect, useState } from "react";
import { UsersIcon, XIcon } from "lucide-react";
import { useAuthStore } from "../store/useAuthStore";
import { useChatStore } from "../store/useChatStore";

function GroupChatHeader() {
  const {
    selectedGroup,
    allContacts,
    getAllContacts,
    addGroupMember,
    removeGroupMember,
    leaveGroupMember,
    // updateGroupMemberRole,
    setSelectedGroup,
  } = useChatStore();
  const { authUser } = useAuthStore();
  const [isManaging, setIsManaging] = useState(false);
  const [newMemberId, setNewMemberId] = useState("");
  const [newMemberRole, setNewMemberRole] = useState<"member">("member");

  const groupId = selectedGroup?.id;

  useEffect(() => {
    if (groupId) getAllContacts();
  }, [groupId, getAllContacts]);

  if (!selectedGroup || !authUser) return null;

  const myRole = selectedGroup.members.find((member) => member.user_id === authUser._id)?.role;
  const isAdmin = myRole === "admin";
  const availableContacts = allContacts.filter(
    (contact) => !selectedGroup.members.some((member) => member.user_id === contact._id),
  );

  const handleAddMember = async () => {
    if (!newMemberId) return;
    await addGroupMember(selectedGroup.id, Number(newMemberId), newMemberRole);
    setNewMemberId("");
  };

  const handleRemoveMember = async (userId: number) => {
    try {
      await removeGroupMember(selectedGroup.id, Number(userId), "member");
    } catch (error) {
      console.error("Failed to remove member:", error);
    }
  };

  const handleLeaveGroup = async () => {
    try {
      await leaveGroupMember(selectedGroup.id, Number(authUser._id), "member");
    } catch (error) {
      console.error("Failed to leave group:", error);
    }
  };

  return (
    <div className="relative flex min-h-[84px] items-center justify-between border-b border-slate-700/50 bg-slate-800/50 px-6">
      <div>
        <h3 className="font-medium text-slate-100">{selectedGroup.name}</h3>
        <p className="text-sm text-slate-400">
          {selectedGroup.members.length} members · {myRole ?? "Member"}
        </p>
      </div>
      <div className="flex items-center gap-3">
        <button
          type="button"
          title="Manage group members"
          aria-label="Manage group members"
          onClick={() => setIsManaging((open) => !open)}
          className="rounded p-2 text-slate-300 hover:bg-slate-700"
        >
          <UsersIcon className="size-5" />
        </button>
        <button
          type="button"
          title="Close conversation"
          aria-label="Close conversation"
          onClick={() => setSelectedGroup(null)}
          className="rounded p-2 text-slate-400 hover:bg-slate-700 hover:text-slate-100"
        >
          <XIcon className="size-5" />
        </button>
      </div>

      {isManaging && (
        <div className="absolute right-4 top-[72px] z-20 max-h-[65vh] w-[min(360px,calc(100vw-2rem))] overflow-y-auto rounded-lg border border-slate-700 bg-slate-900 p-4 shadow-xl">
          <h4 className="mb-3 text-sm font-semibold text-slate-100">Members</h4>
          <div className="space-y-2">
            {selectedGroup.members.map((member) => (
              <div key={member.user_id} className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm text-slate-200">{member.username}</p>
                  <p className="truncate text-xs text-slate-500">{member.email}</p>
                </div>
                {member.role === "admin" ? (
                  <span className="shrink-0 rounded-full bg-blue-500/20 px-2 py-1 text-xs font-semibold text-blue-400">
                    Admin
                  </span>
                ) : member.user_id === authUser._id ? (
                  <button
                    type="button"
                    onClick={handleLeaveGroup}
                    className="shrink-0 rounded-md bg-red-500/10 px-3 py-1.5 text-xs font-semibold text-red-400 transition hover:bg-red-500/20"
                  >
                    Leave
                  </button>
                ) : isAdmin ? (
                  <button
                    type="button"
                    onClick={() => handleRemoveMember(member.user_id)}
                    className="shrink-0 rounded-md bg-red-500/10 px-3 py-1.5 text-xs font-semibold text-red-400 transition hover:bg-red-500/20"
                  >
                    Remove
                  </button>
                ) : null}
                {/* {isAdmin ? (
                  <select
                    aria-label={`Role for ${member.username}`}
                    value={member.role}
                    onChange={(event) => updateGroupMemberRole(
                      selectedGroup.id,
                      member.user_id,
                      event.target.value as "admin" | "member",
                    )}
                    className="rounded border border-slate-700 bg-slate-800 px-2 py-1 text-xs text-slate-200"
                  >
                    <option value="member">Member</option>
                    <option value="admin">Admin</option>
                  </select>
                ) : (
                  <span className="text-xs text-slate-400">{member.role}</span>
                )} */}
              </div>
            ))}
          </div>

          {isAdmin && (
            <div className="mt-4 border-t border-slate-700 pt-4">
              <h4 className="mb-2 text-sm font-semibold text-slate-100">Add a friend</h4>
              {availableContacts.length === 0 ? (
                <p className="text-xs text-slate-500">All your friends are in this group.</p>
              ) : (
                <>
                  <select
                    aria-label="Friend to add"
                    value={newMemberId}
                    onChange={(event) => setNewMemberId(event.target.value)}
                    className="mb-2 w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-200"
                  >
                    <option value="">Select a friend</option>
                    {availableContacts.map((contact) => (
                      <option key={contact._id} value={contact._id}>{contact.username}</option>
                    ))}
                  </select>
                  <div className="flex gap-2">
                    <select
                      aria-label="Role for new member"
                      value={newMemberRole}
                      onChange={(event) => setNewMemberRole(event.target.value as "member")}
                      className="min-w-0 flex-1 rounded border border-slate-700 bg-slate-800 px-2 py-2 text-sm text-slate-200"
                    >
                      <option value="member">Member</option>
                    </select>
                    <button
                      type="button"
                      onClick={handleAddMember}
                      disabled={!newMemberId}
                      className="rounded bg-cyan-600 px-3 py-2 text-sm text-white hover:bg-cyan-500 disabled:opacity-50"
                    >
                      Add
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default GroupChatHeader;