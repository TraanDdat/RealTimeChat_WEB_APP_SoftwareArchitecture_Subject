"""User activity tracking and statistics.

Collected data (MongoDB):
- online_sessions: one document per Socket.IO connection {userId, sid, startedAt, endedAt}

Statistics are computed from that collection plus the existing
`messages` / `group_messages` collections and PostgreSQL (users, friends, groups).
"""
import logging
from datetime import date, datetime, timedelta, timezone
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pymongo import DESCENDING
from pymongo.errors import PyMongoError

from zalo_backend.database.l_mongodb import collection, group_messages, online_sessions
from zalo_backend.database.l_postgres import SessionLocal
from zalo_backend.models.postgres.group_chat import GroupChat, GroupMember
from zalo_backend.models.postgres.user import User
from zalo_backend.user_service.addfr_service import get_friends
from zalo_backend.user_service.client import get_user

logger = logging.getLogger(__name__)

MESSAGE_TYPES = ("text", "emotion", "image", "video", "file")


# ---------------------------------------------------------------- collecting

def start_session(user_id: int, sid: str) -> None:
    """Best effort: activity tracking must never break the socket connection."""
    try:
        online_sessions.insert_one({
            "userId": user_id,
            "sid": sid,
            "startedAt": datetime.now(timezone.utc),
            "endedAt": None,
        })
    except PyMongoError:
        logger.exception("Failed to start online session: user=%s sid=%s", user_id, sid)


def end_session(sid: str) -> None:
    try:
        online_sessions.update_one(
            {"sid": sid, "endedAt": None},
            {"$set": {"endedAt": datetime.now(timezone.utc)}},
        )
    except PyMongoError:
        logger.exception("Failed to end online session: sid=%s", sid)


# ---------------------------------------------------------------- statistics

def _resolve_tz(tz_name: str | None) -> tuple[ZoneInfo | timezone, str]:
    if tz_name:
        try:
            return ZoneInfo(tz_name), tz_name
        except (ZoneInfoNotFoundError, ValueError):
            pass
    return timezone.utc, "UTC"


def _type_expression() -> dict:
    """Classify a message document into one of MESSAGE_TYPES (or 'recalled')."""
    return {"$switch": {
        "branches": [
            {"case": {"$eq": ["$isRecalled", True]}, "then": "recalled"},
            {"case": {"$gt": ["$image", None]}, "then": "image"},
            {"case": {"$and": [
                {"$gt": ["$attachment", None]},
                {"$regexMatch": {
                    "input": {"$ifNull": ["$attachmentMimeType", ""]},
                    "regex": "^video/",
                }},
            ]}, "then": "video"},
            {"case": {"$gt": ["$attachment", None]}, "then": "file"},
            {"case": {"$gt": ["$text", None]}, "then": "text"},
            {"case": {"$gt": ["$emotion", None]}, "then": "emotion"},
        ],
        "default": "text",
    }}


def _count_by_day(coll, match: dict, tz_name: str) -> dict[str, int]:
    rows = coll.aggregate([
        {"$match": match},
        {"$group": {
            "_id": {"$dateToString": {"format": "%Y-%m-%d", "date": "$createdAt", "timezone": tz_name}},
            "count": {"$sum": 1},
        }},
    ])
    return {row["_id"]: row["count"] for row in rows}


def _online_minutes_by_day(
    user_id: int, start: datetime, now: datetime, tz, days: list[date],
) -> dict[str, float]:
    from zalo_backend.socket.event import connected_users  # avoid import cycle

    minutes = {day.isoformat(): 0.0 for day in days}
    sessions = online_sessions.find({
        "userId": user_id,
        "startedAt": {"$lt": now},
        "$or": [{"endedAt": None}, {"endedAt": {"$gte": start}}],
    })
    for session in sessions:
        begin = max(session["startedAt"], start)
        end = session.get("endedAt")
        if end is None:
            # still open: only count it if the socket is really connected right now
            # (sessions left open by a server restart are ignored)
            if session.get("sid") not in connected_users:
                continue
            end = now
        end = min(end, now)
        # split the session across local-day boundaries
        cursor = begin
        while cursor < end:
            local_day = cursor.astimezone(tz).date()
            next_midnight = datetime.combine(local_day + timedelta(days=1), datetime.min.time(), tz)
            chunk_end = min(end, next_midnight.astimezone(timezone.utc))
            key = local_day.isoformat()
            if key in minutes:
                minutes[key] += (chunk_end - cursor).total_seconds() / 60
            cursor = chunk_end
    return minutes


def _system_stats(day_list: list[date], in_range: dict, tz_name: str) -> dict:
    """System-wide numbers: users, groups, online users, messages."""
    from zalo_backend.socket.event import online_users  # avoid import cycle

    with SessionLocal() as db:
        total_users = db.query(User).count()
        total_groups = db.query(GroupChat).count()

    private_total = collection.estimated_document_count()
    group_total = group_messages.estimated_document_count()
    private_daily = _count_by_day(collection, {"createdAt": in_range}, tz_name)
    group_daily = _count_by_day(group_messages, {"createdAt": in_range}, tz_name)

    daily = [
        {
            "date": day.isoformat(),
            "private": private_daily.get(day.isoformat(), 0),
            "group": group_daily.get(day.isoformat(), 0),
        }
        for day in day_list
    ]
    return {
        "totalUsers": total_users,
        "totalGroups": total_groups,
        "onlineUsers": len(online_users),
        "totalMessages": private_total + group_total,
        "privateMessages": private_total,
        "groupMessages": group_total,
        "messagesInRange": sum(d["private"] + d["group"] for d in daily),
        "messagesToday": daily[-1]["private"] + daily[-1]["group"],
        "daily": daily,
    }


def get_user_stats(user_id: int, days: int = 7, tz_name: str | None = None) -> dict:
    tz, tz_name = _resolve_tz(tz_name)
    now = datetime.now(timezone.utc)
    today = now.astimezone(tz).date()
    day_list = [today - timedelta(days=offset) for offset in range(days - 1, -1, -1)]
    start = datetime.combine(day_list[0], datetime.min.time(), tz).astimezone(timezone.utc)
    in_range = {"$gte": start}

    sent_match = {"senderId": user_id, "createdAt": in_range}
    received_match = {"receiverId": user_id, "createdAt": in_range}

    # ---- daily message counts + online time of this user
    private_sent = _count_by_day(collection, sent_match, tz_name)
    group_sent = _count_by_day(group_messages, sent_match, tz_name)
    received = _count_by_day(collection, received_match, tz_name)
    online = _online_minutes_by_day(user_id, start, now, tz, day_list)

    daily = []
    for day in day_list:
        key = day.isoformat()
        daily.append({
            "date": key,
            "sent": private_sent.get(key, 0) + group_sent.get(key, 0),
            "received": received.get(key, 0),
            "onlineMinutes": round(online.get(key, 0.0)),
        })

    # ---- message types + most active hours (messages the user sent)
    message_types = {name: 0 for name in MESSAGE_TYPES}
    recalled = 0
    hourly = [0] * 24
    for coll in (collection, group_messages):
        for row in coll.aggregate([
            {"$match": sent_match},
            {"$group": {"_id": _type_expression(), "count": {"$sum": 1}}},
        ]):
            if row["_id"] == "recalled":
                recalled += row["count"]
            else:
                message_types[row["_id"]] = message_types.get(row["_id"], 0) + row["count"]
        for row in coll.aggregate([
            {"$match": sent_match},
            {"$group": {"_id": {"$hour": {"date": "$createdAt", "timezone": tz_name}}, "count": {"$sum": 1}}},
        ]):
            hourly[row["_id"]] += row["count"]

    # ---- top private-chat partners
    top_rows = list(collection.aggregate([
        {"$match": {"$or": [sent_match, received_match]}},
        {"$group": {
            "_id": {"$cond": [{"$eq": ["$senderId", user_id]}, "$receiverId", "$senderId"]},
            "count": {"$sum": 1},
        }},
        {"$sort": {"count": -1}},
        {"$limit": 5},
    ]))
    top_contacts = []
    for row in top_rows:
        partner = get_user(row["_id"])
        if partner is None:
            continue
        top_contacts.append({
            "userId": partner.id,
            "username": partner.username,
            "profilePic": partner.profilePic,
            "messages": row["count"],
        })

    # ---- last activity
    last_message_times = [
        doc["createdAt"]
        for coll in (collection, group_messages)
        for doc in coll.find({"senderId": user_id}, {"createdAt": 1}).sort("createdAt", DESCENDING).limit(1)
    ]
    last_active = max(last_message_times, default=None)

    # ---- PostgreSQL: friends, groups of this user
    with SessionLocal() as db:
        friends = len(get_friends(db, user_id))
        groups = db.query(GroupMember).filter(GroupMember.user_id == user_id).count()

    return {
        "rangeDays": days,
        "timezone": tz_name,
        "generatedAt": now.isoformat(),
        "system": _system_stats(day_list, in_range, tz_name),
        "summary": {
            "messagesSent": sum(day["sent"] for day in daily),
            "messagesReceived": sum(day["received"] for day in daily),
            "groupMessagesSent": sum(group_sent.values()),
            "mediaShared": message_types["image"] + message_types["video"] + message_types["file"],
            "recalledMessages": recalled,
            "onlineMinutes": sum(day["onlineMinutes"] for day in daily),
            "activeDays": sum(1 for day in daily if day["sent"] or day["onlineMinutes"]),
            "friends": friends,
            "groups": groups,
        },
        "daily": daily,
        "messageTypes": message_types,
        "hourly": hourly,
        "topContacts": top_contacts,
        "lastActiveAt": last_active.isoformat() if last_active else None,
    }
