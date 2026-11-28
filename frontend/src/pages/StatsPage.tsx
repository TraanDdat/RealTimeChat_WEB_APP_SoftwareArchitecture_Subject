import { useEffect, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeftIcon,
  ImageIcon,
  MessageCircleIcon,
  MessagesSquareIcon,
  RefreshCwIcon,
  SendIcon,
  CalendarCheckIcon,
  TableIcon,
  BarChart3Icon,
  UserIcon,
  UsersIcon,
} from "lucide-react";
import { isAxiosError } from "axios";

import { statsApi } from "../api/stats";
import type { MessageType, UserStats } from "../types/stats";
import { useAuthStore } from "../store/useAuthStore";

// Chart colors: series 1 = app blue, series 2 = orange (validated pair: CVD ΔE 32, normal ΔE 42).
// Orange is < 3:1 on white, so the daily chart also ships a table view.
const SENT_COLOR = "#0068ff";
const RECEIVED_COLOR = "#f47230";
const GRID_COLOR = "#e3ebf5";

const RANGES = [7, 30] as const;

const TYPE_LABELS: Record<MessageType, string> = {
  text: "Text",
  emotion: "Emoji / emotion",
  image: "Image",
  video: "Video",
  file: "Document / file",
};

function formatDay(isoDate: string, long = false) {
  const [y, m, d] = isoDate.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, long
    ? { weekday: "short", day: "numeric", month: "short" }
    : { day: "numeric", month: "numeric" });
}

function formatDateTime(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString(undefined, {
    day: "numeric", month: "short", hour: "2-digit", minute: "2-digit",
  });
}

/** Round an axis max up to a "nice" number so gridlines land on clean values. */
function niceMax(value: number) {
  if (value <= 4) return 4;
  const magnitude = 10 ** Math.floor(Math.log10(value));
  const step = [1, 2, 2.5, 5, 10].find((s) => s * magnitude >= value / 4)! * magnitude;
  return Math.ceil(value / step) * step;
}

// ------------------------------------------------------------------ building blocks

function Card({ title, subtitle, action, children }: {
  title: string; subtitle?: string; action?: ReactNode; children: ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-700/50 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div>
          <h2 className="text-slate-200 font-semibold">{title}</h2>
          {subtitle && <p className="text-slate-500 text-xs mt-0.5">{subtitle}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

function SectionTitle({ children }: { children: ReactNode }) {
  return <h2 className="text-slate-400 text-xs font-semibold uppercase tracking-wide">{children}</h2>;
}

function TableToggle({ showTable, onToggle }: { showTable: boolean; onToggle: () => void }) {
  return (
    <button
      onClick={onToggle}
      className="flex items-center gap-1 rounded-md border border-slate-700/50 px-2 py-1 text-xs text-slate-400 hover:text-slate-200"
    >
      {showTable ? <BarChart3Icon className="size-3.5" /> : <TableIcon className="size-3.5" />}
      {showTable ? "Chart" : "Table"}
    </button>
  );
}

function StatTile({ icon, label, value, hint }: {
  icon: ReactNode; label: string; value: string | number; hint?: string;
}) {
  return (
    <div className="rounded-2xl border border-slate-700/50 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-2 text-slate-400 text-xs font-medium">
        <span className="text-cyan-500">{icon}</span>
        {label}
      </div>
      <div className="text-slate-200 text-2xl font-semibold mt-2 tabular-nums">{value}</div>
      {hint && <div className="text-slate-500 text-xs mt-1">{hint}</div>}
    </div>
  );
}

function LegendItem({ color, label }: { color: string; label: string }) {
  return (
    <span className="flex items-center gap-1.5 text-slate-400 text-xs">
      <span className="inline-block size-2.5 rounded-sm" style={{ background: color }} />
      {label}
    </span>
  );
}

function Tooltip({ children }: { children: ReactNode }) {
  return (
    <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 -translate-x-1/2 whitespace-nowrap rounded-lg border border-slate-700/50 bg-white px-3 py-2 text-xs shadow-lg">
      {children}
    </div>
  );
}

// ------------------------------------------------------------------ charts

/** Vertical bars on a shared y-axis. One or two stacked series. */
function BarChart({ data, series, formatValue = String, height = 180 }: {
  data: { label: string; longLabel: string; values: number[] }[];
  series: { name: string; color: string }[];
  formatValue?: typeof String;
  height?: number;
}) {
  const [hovered, setHovered] = useState<number | null>(null);
  const max = niceMax(Math.max(0, ...data.map((d) => d.values.reduce((a, b) => a + b, 0))));
  const ticks = [0, 0.25, 0.5, 0.75, 1].map((t) => t * max);
  const labelEvery = data.length > 14 ? Math.ceil(data.length / 10) : 1;

  return (
    <div className="flex gap-2">
      {/* y axis */}
      <div className="relative w-10 shrink-0 text-right text-[11px] text-slate-500 tabular-nums" style={{ height }}>
        {ticks.map((tick) => (
          <span key={tick} className="absolute right-0 -translate-y-1/2" style={{ bottom: `${(tick / max) * 100}%` }}>
            {formatValue(Math.round(tick))}
          </span>
        ))}
      </div>

      <div className="flex-1 min-w-0">
        <div className="relative" style={{ height }}>
          {/* recessive gridlines */}
          {ticks.map((tick) => (
            <div key={tick} className="absolute inset-x-0" style={{ bottom: `${(tick / max) * 100}%`, borderTop: `1px solid ${GRID_COLOR}` }} />
          ))}

          <div className="absolute inset-0 flex items-end gap-[2px]">
            {data.map((d, index) => {
              const total = d.values.reduce((a, b) => a + b, 0);
              return (
                <div
                  key={d.longLabel}
                  className="relative flex h-full flex-1 cursor-default flex-col justify-end"
                  onMouseEnter={() => setHovered(index)}
                  onMouseLeave={() => setHovered(null)}
                >
                  {hovered === index && (
                    <Tooltip>
                      <div className="text-slate-200 font-medium mb-1">{d.longLabel}</div>
                      {series.map((s, i) => (
                        <div key={s.name} className="flex items-center justify-between gap-4 text-slate-400">
                          <LegendItem color={s.color} label={s.name} />
                          <span className="text-slate-200 tabular-nums">{formatValue(d.values[i])}</span>
                        </div>
                      ))}
                    </Tooltip>
                  )}
                  {/* stacked segments: first series sits on the baseline, 2px gap between fills */}
                  <div
                    className={`mx-auto flex w-full max-w-[28px] flex-col-reverse gap-[2px] transition-opacity ${hovered !== null && hovered !== index ? "opacity-50" : ""}`}
                    style={{ height: `${(total / max) * 100}%` }}
                  >
                    {d.values.map((value, i) => {
                      if (value === 0) return null;
                      const isTop = d.values.slice(i + 1).every((v) => v === 0);
                      return (
                        <div
                          key={series[i].name}
                          style={{
                            flexGrow: value,
                            flexBasis: 0,
                            minHeight: 2,
                            background: series[i].color,
                            borderRadius: isTop ? "4px 4px 0 0" : 0,
                          }}
                        />
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* x axis */}
        <div className="mt-1.5 flex gap-[2px] text-[11px] text-slate-500">
          {data.map((d, index) => (
            <span key={d.longLabel} className="flex-1 text-center tabular-nums">
              {index % labelEvery === 0 ? d.label : ""}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

/** 24 cells, one hue light→dark by message count. */
function HourHeatStrip({ hourly }: { hourly: number[] }) {
  const [hovered, setHovered] = useState<number | null>(null);
  const max = Math.max(1, ...hourly);
  const peak = hourly.indexOf(Math.max(...hourly));

  return (
    <div>
      <div className="flex gap-[2px]">
        {hourly.map((count, hour) => (
          <div
            key={hour}
            className="relative h-10 flex-1 rounded"
            style={{
              background: count === 0
                ? "#eef3fa"
                : `color-mix(in oklab, ${SENT_COLOR} ${18 + (count / max) * 82}%, #eef3fa)`,
            }}
            onMouseEnter={() => setHovered(hour)}
            onMouseLeave={() => setHovered(null)}
          >
            {hovered === hour && (
              <Tooltip>
                <span className="text-slate-200 font-medium">{hour}:00–{hour}:59</span>
                <span className="text-slate-400"> · {count} message{count === 1 ? "" : "s"}</span>
              </Tooltip>
            )}
          </div>
        ))}
      </div>
      <div className="mt-1.5 flex text-[11px] text-slate-500">
        {[0, 6, 12, 18].map((h) => (
          <span key={h} className="flex-1">{h}:00</span>
        ))}
        <span>23:59</span>
      </div>
      <p className="text-slate-400 text-xs mt-3">
        {hourly.some((c) => c > 0)
          ? <>Most active around <span className="text-slate-200 font-medium">{peak}:00–{peak + 1}:00</span></>
          : "No messages sent in this period."}
      </p>
    </div>
  );
}

function HorizontalBars({ rows }: { rows: { label: string; value: number }[] }) {
  const max = Math.max(1, ...rows.map((r) => r.value));
  const total = rows.reduce((a, r) => a + r.value, 0);
  return (
    <div className="space-y-3">
      {rows.map((row) => (
        <div key={row.label}>
          <div className="flex justify-between text-xs mb-1">
            <span className="text-slate-300">{row.label}</span>
            <span className="text-slate-200 tabular-nums font-medium">
              {row.value}
              <span className="text-slate-500 font-normal">
                {" "}({total ? Math.round((row.value / total) * 100) : 0}%)
              </span>
            </span>
          </div>
          <div className="h-2 rounded bg-[#eef3fa]">
            <div
              className="h-full rounded"
              style={{ width: `${(row.value / max) * 100}%`, background: SENT_COLOR, minWidth: row.value ? 4 : 0 }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

// ------------------------------------------------------------------ page

function StatsPage() {
  const navigate = useNavigate();
  const { authUser } = useAuthStore();
  const [days, setDays] = useState<(typeof RANGES)[number]>(7);
  const [reloadToken, setReloadToken] = useState(0);
  const [showTable, setShowTable] = useState(false);
  const [showSystemTable, setShowSystemTable] = useState(false);
  // result of the latest finished request; loading = it doesn't match the current request yet
  const [result, setResult] = useState<{ key: string; stats: UserStats | null; error: string | null }>({
    key: "", stats: null, error: null,
  });
  const requestKey = `${days}:${reloadToken}`;
  const isLoading = result.key !== requestKey;
  const { stats, error } = result;

  useEffect(() => {
    let cancelled = false;
    const key = `${days}:${reloadToken}`;
    statsApi.getMyStats(days)
      .then((response) => {
        if (!cancelled) setResult({ key, stats: response.data, error: null });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        const message = isAxiosError(err)
          ? err.response?.data?.message ?? err.message
          : "Could not load statistics";
        setResult((prev) => ({ key, stats: prev.stats, error: String(message) }));
      });
    return () => {
      cancelled = true;
    };
  }, [days, reloadToken]);

  const daily = stats?.daily ?? [];

  return (
    <div className="w-full max-w-6xl h-[800px] overflow-y-auto rounded-2xl border border-slate-700/50 bg-slate-800/50 p-6">
      {/* header + filters in one row */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div className="flex items-center gap-3">
          <button
            className="rounded-full p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-700 transition-colors"
            onClick={() => navigate("/chat")}
            title="Back to chat"
          >
            <ArrowLeftIcon className="size-5" />
          </button>
          <div>
            <h1 className="text-slate-200 text-xl font-semibold">Activity statistics</h1>
            <p className="text-slate-500 text-xs">
              {authUser?.username} · last {days} days{stats ? ` · ${stats.timezone}` : ""}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex rounded-lg border border-slate-700/50 bg-white p-0.5">
            {RANGES.map((range) => (
              <button
                key={range}
                onClick={() => setDays(range)}
                className={`rounded-md px-3 py-1 text-sm transition-colors ${
                  days === range ? "bg-cyan-500 text-white" : "text-slate-400 hover:text-slate-200"
                }`}
              >
                {range} days
              </button>
            ))}
          </div>
          <button
            onClick={() => setReloadToken((token) => token + 1)}
            className="rounded-lg border border-slate-700/50 bg-white p-2 text-slate-400 hover:text-slate-200 transition-colors"
            title="Refresh"
          >
            <RefreshCwIcon className={`size-4 ${isLoading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </div>

      {error && (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 mb-6">
          {error}
        </div>
      )}

      {!stats && isLoading && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-24 rounded-2xl bg-slate-700 animate-pulse" />
          ))}
        </div>
      )}

      {/* {stats && ( */}
        
          {/* ---------------- system-wide statistics ---------------- */}
          {stats && authUser && (
            <div className={`space-y-6 transition-opacity ${isLoading ? "opacity-60" : ""}`}>

              {/* Admin: System-wide statistics */}
              {authUser.email?.endsWith("@admin.vn") && (
                <div className="space-y-6">
                  <SectionTitle>System overview</SectionTitle>
                  <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <StatTile icon={<UserIcon className="size-4" />} label="Registered users" value={stats.system.totalUsers} />
                    <StatTile icon={<UsersIcon className="size-4" />} label="Groups" value={stats.system.totalGroups} />
                    <StatTile
                      icon={<span className="inline-block size-2 rounded-full bg-green-500" />}
                      label="Users online now"
                      value={stats.system.onlineUsers}
                      hint={`${stats.system.totalUsers ? Math.round((stats.system.onlineUsers / stats.system.totalUsers) * 100) : 0}% of all users`}
                    />
                    <StatTile
                      icon={<MessagesSquareIcon className="size-4" />}
                      label="Total messages"
                      value={stats.system.totalMessages}
                      hint={`${stats.system.privateMessages} private · ${stats.system.groupMessages} group · ${stats.system.messagesToday} today`}
                    />
                  </div>

                  <Card
                    title="Messages in the system per day"
                    subtitle={`${stats.system.messagesInRange} messages in the last ${stats.rangeDays} days, all users`}
                    action={
                      <div className="flex items-center gap-4">
                        <LegendItem color={SENT_COLOR} label="Private" />
                        <LegendItem color={RECEIVED_COLOR} label="Group" />
                        <TableToggle showTable={showSystemTable} onToggle={() => setShowSystemTable((v) => !v)} />
                      </div>
                    }
                  >
                    {showSystemTable ? (
                      <div className="max-h-64 overflow-y-auto">
                        <table className="w-full text-sm">
                          <thead className="text-slate-500 text-xs text-left">
                            <tr className="border-b border-slate-700/50">
                              <th className="py-2 font-medium">Date</th>
                              <th className="py-2 font-medium text-right">Private</th>
                              <th className="py-2 font-medium text-right">Group</th>
                              <th className="py-2 font-medium text-right">Total</th>
                            </tr>
                          </thead>
                          <tbody className="text-slate-300 tabular-nums">
                            {[...stats.system.daily].reverse().map((d) => (
                              <tr key={d.date} className="border-b border-slate-700/50 last:border-0">
                                <td className="py-1.5">{formatDay(d.date, true)}</td>
                                <td className="py-1.5 text-right">{d.private}</td>
                                <td className="py-1.5 text-right">{d.group}</td>
                                <td className="py-1.5 text-right">{d.private + d.group}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <BarChart
                        series={[{ name: "Private", color: SENT_COLOR }, { name: "Group", color: RECEIVED_COLOR }]}
                        data={stats.system.daily.map((d) => ({
                          label: formatDay(d.date),
                          longLabel: formatDay(d.date, true),
                          values: [d.private, d.group],
                        }))}
                      />
                    )}
                  </Card>
                </div>
              )}
          
          {/* User: Personal activity */}
          {!authUser.email?.endsWith("@admin.vn") && (
            <div className="space-y-6">
              <div className="pt-2">
                <SectionTitle>Your activity</SectionTitle>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
                <StatTile icon={<SendIcon className="size-4" />} label="Messages sent" value={stats.summary.messagesSent}
                  hint={`${stats.summary.groupMessagesSent} in groups`} />
                <StatTile icon={<MessageCircleIcon className="size-4" />} label="Messages received" value={stats.summary.messagesReceived}
                  hint="private chats" />
                <StatTile icon={<CalendarCheckIcon className="size-4" />} label="Active days" value={`${stats.summary.activeDays}/${stats.rangeDays}`}
                  hint={`last message: ${formatDateTime(stats.lastActiveAt)}`} />
                <StatTile icon={<ImageIcon className="size-4" />} label="Media shared" value={stats.summary.mediaShared}
                  hint={`${stats.summary.recalledMessages} recalled`} />
                <StatTile icon={<UsersIcon className="size-4" />} label="Friends · groups"
                  value={`${stats.summary.friends} · ${stats.summary.groups}`} hint="friends · groups joined" />
              </div>
              <Card
                title="Your messages per day"
                subtitle="Sent (private + group) and received (private)"
                action={
                  <div className="flex items-center gap-4">
                    <LegendItem color={SENT_COLOR} label="Sent" />
                    <LegendItem color={RECEIVED_COLOR} label="Received" />
                    <TableToggle showTable={showTable} onToggle={() => setShowTable((v) => !v)} />
                  </div>
                }
              >
                {showTable ? (
                  <div className="max-h-64 overflow-y-auto">
                    <table className="w-full text-sm">
                      <thead className="text-slate-500 text-xs text-left">
                        <tr className="border-b border-slate-700/50">
                          <th className="py-2 font-medium">Date</th>
                          <th className="py-2 font-medium text-right">Sent</th>
                          <th className="py-2 font-medium text-right">Received</th>
                        </tr>
                      </thead>
                      <tbody className="text-slate-300 tabular-nums">
                        {[...daily].reverse().map((d) => (
                          <tr key={d.date} className="border-b border-slate-700/50 last:border-0">
                            <td className="py-1.5">{formatDay(d.date, true)}</td>
                            <td className="py-1.5 text-right">{d.sent}</td>
                            <td className="py-1.5 text-right">{d.received}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <BarChart
                    series={[{ name: "Sent", color: SENT_COLOR }, { name: "Received", color: RECEIVED_COLOR }]}
                    data={daily.map((d) => ({
                      label: formatDay(d.date),
                      longLabel: formatDay(d.date, true),
                      values: [d.sent, d.received],
                    }))}
                  />
                )}
              </Card>

              <Card title="Most active hours" subtitle="Messages you sent, by hour of day">
                <HourHeatStrip hourly={stats.hourly} />
              </Card>

              <div className="grid gap-6 lg:grid-cols-2">
                <Card title="Message types" subtitle="What you sent in this period">
                  <HorizontalBars
                    rows={(Object.keys(TYPE_LABELS) as MessageType[]).map((type) => ({
                      label: TYPE_LABELS[type],
                      value: stats.messageTypes[type] ?? 0,
                    }))}
                  />
                </Card>

                <Card title="Top contacts" subtitle="Private messages exchanged">
                  {stats.topContacts.length === 0 ? (
                    <p className="text-slate-500 text-sm">No private conversations in this period.</p>
                  ) : (
                    <ul className="space-y-3">
                      {stats.topContacts.map((contact) => (
                        <li key={contact.userId} className="flex items-center gap-3">
                          <img
                            src={contact.profilePic || "/avatar.png"}
                            alt={contact.username}
                            className="size-8 rounded-full object-cover"
                          />
                          <div className="flex-1 min-w-0">
                            <div className="flex justify-between text-sm">
                              <span className="text-slate-200 truncate">{contact.username}</span>
                              <span className="text-slate-200 tabular-nums font-medium">{contact.messages}</span>
                            </div>
                            <div className="mt-1 h-1.5 rounded bg-[#eef3fa]">
                              <div
                                className="h-full rounded"
                                style={{
                                  width: `${(contact.messages / stats.topContacts[0].messages) * 100}%`,
                                  background: SENT_COLOR,
                                }}
                              />
                            </div>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </Card>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default StatsPage;
