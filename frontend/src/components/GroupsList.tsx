import { useEffect, useState, type FormEvent } from "react";
import { PlusIcon, UsersIcon } from "lucide-react";
import { useChatStore } from "../store/useChatStore";

function GroupsList() {
  const {
    groups,
    allContacts,
    getGroups,
    getAllContacts,
    createGroup,
    selectedGroup,
    setSelectedGroup,
    isUsersLoading,
  } = useChatStore();
  const [isCreating, setIsCreating] = useState(false);
  const [name, setName] = useState("");
  const [memberIds, setMemberIds] = useState<number[]>([]);

  useEffect(() => {
    getGroups();
    getAllContacts();
  }, [getGroups, getAllContacts]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!name.trim()) return;
    const created = await createGroup(name.trim(), memberIds);
    if (!created) return;
    setName("");
    setMemberIds([]);
    setIsCreating(false);
  };

  const toggleMember = (userId: number) => {
    setMemberIds((selected) => selected.includes(userId)
      ? selected.filter((id) => id !== userId)
      : [...selected, userId]);
  };

  return (
    <section className="space-y-3">
      <button
        type="button"
        onClick={() => setIsCreating((visible) => !visible)}
        aria-expanded={isCreating}
        className="flex w-full items-center justify-center gap-2 rounded-lg bg-cyan-600 px-3 py-2 text-sm font-medium text-white hover:bg-cyan-500"
      >
        <PlusIcon className="size-4" />
        Create group
      </button>

      {isCreating && (
        <form onSubmit={submit} className="space-y-3 rounded-lg border border-slate-700 bg-slate-900/80 p-3">
          <input
            required
            maxLength={100}
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Group name"
            aria-label="Group name"
            className="w-full rounded border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-100"
          />
          <div className="max-h-40 space-y-2 overflow-y-auto">
            {allContacts.length === 0 ? (
              <p className="text-xs text-slate-500">Add friends before creating a group.</p>
            ) : allContacts.map((contact) => (
              <label key={contact._id} className="flex items-center gap-2 text-sm text-slate-300">
                <input
                  type="checkbox"
                  checked={memberIds.includes(contact._id)}
                  onChange={() => toggleMember(contact._id)}
                  className="checkbox checkbox-sm checkbox-info"
                />
                <span className="truncate">{contact.username}</span>
              </label>
            ))}
          </div>
          <button type="submit" className="w-full rounded bg-cyan-600 px-3 py-2 text-sm text-white hover:bg-cyan-500">
            Create
          </button>
        </form>
      )}

      {isUsersLoading ? <p className="text-sm text-slate-500">Loading groups...</p> : groups.length === 0 ? (
        <p className="px-2 py-4 text-center text-sm text-slate-500">No groups yet</p>
      ) : groups.map((group) => (
        <button
          type="button"
          key={group.id}
          onClick={() => setSelectedGroup(group)}
          className={`flex w-full items-center gap-3 rounded-lg p-3 text-left transition-colors ${
            selectedGroup?.id === group.id ? "bg-cyan-500/20" : "bg-slate-800/60 hover:bg-slate-700/70"
          }`}
        >
          <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-cyan-900/70 text-cyan-300">
            <UsersIcon className="size-5" />
          </span>
          <span className="min-w-0">
            <span className="block truncate text-sm font-medium text-slate-100">{group.name}</span>
            <span className="block text-xs text-slate-400">{group.members.length} members</span>
          </span>
        </button>
      ))}
    </section>
  );
}

export default GroupsList;