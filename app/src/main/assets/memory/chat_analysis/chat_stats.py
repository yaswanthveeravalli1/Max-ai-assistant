#!/usr/bin/env python3
"""
WhatsApp Chat Stats Analyzer
Parses WhatsApp exported .txt files and generates detailed statistics.
Built for Yaswanth (Yash) — Personal Memory Project
"""

import re
import sys
import os
from collections import Counter, defaultdict
from datetime import datetime, timedelta

# ─── WhatsApp Message Parser ───────────────────────────────────────────

# Pattern: DD/MM/YY, H:MM am/pm - ContactName: Message
MSG_PATTERN = re.compile(
    r'^(\d{1,2}/\d{2}/\d{2}),\s(\d{1,2}:\d{2}\s[ap]m)\s-\s([^:]+):\s(.+)',
    re.IGNORECASE
)

# Emojis (basic + extended ranges)
EMOJI_PATTERN = re.compile(
    "["
    "\U0001F600-\U0001F64F"  # emoticons
    "\U0001F300-\U0001F5FF"  # symbols & pictographs
    "\U0001F680-\U0001F6FF"  # transport & map
    "\U0001F1E0-\U0001F1FF"  # flags
    "\U00002702-\U000027B0"  # dingbats
    "\U0000FE00-\U0000FE0F"  # variation selectors
    "\U0001F900-\U0001F9FF"  # supplemental
    "\U0001FA00-\U0001FA6F"  # chess symbols
    "\U0001FA70-\U0001FAFF"  # symbols extended-A
    "\U00002600-\U000026FF"  # misc symbols
    "\U0000200D"             # ZWJ
    "\U00002764"             # heart
    "\U0000FE0F"             # variation selector
    "]+",
    flags=re.UNICODE,
)


def parse_chat(filepath):
    """Parse a WhatsApp exported chat file into structured messages."""
    messages = []
    current_msg = None

    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            match = MSG_PATTERN.match(line)
            if match:
                if current_msg:
                    messages.append(current_msg)
                date_str, time_str, sender, text = match.groups()
                try:
                    dt = datetime.strptime(f"{date_str}, {time_str}", "%d/%m/%y, %I:%M %p")
                except ValueError:
                    continue
                current_msg = {
                    'datetime': dt,
                    'date': dt.date(),
                    'hour': dt.hour,
                    'sender': sender.strip(),
                    'text': text.strip(),
                    'is_media': text.strip() == '<Media omitted>',
                }
            elif current_msg:
                # Continuation line (multi-line message)
                current_msg['text'] += '\n' + line

    if current_msg:
        messages.append(current_msg)

    return messages


def extract_emojis(text):
    """Extract all emojis from text."""
    return EMOJI_PATTERN.findall(text)


def format_duration(seconds):
    """Format seconds into human-readable duration."""
    if seconds < 60:
        return f"{seconds:.0f}s"
    elif seconds < 3600:
        return f"{seconds/60:.1f} min"
    else:
        return f"{seconds/3600:.1f} hrs"


# ─── Statistics Calculator ────────────────────────────────────────────

def calculate_stats(messages, filepath):
    """Calculate comprehensive statistics from parsed messages."""
    if not messages:
        return None

    # Identify participants
    senders = set(msg['sender'] for msg in messages)
    total = len(messages)

    # ── Basic Counts ──
    msgs_per_sender = Counter(msg['sender'] for msg in messages)
    media_per_sender = Counter(msg['sender'] for msg in messages if msg['is_media'])
    text_msgs = [msg for msg in messages if not msg['is_media'] and msg['text'] != 'null']

    # ── Date Range ──
    first_date = messages[0]['date']
    last_date = messages[-1]['date']
    total_days = (last_date - first_date).days + 1
    active_days = len(set(msg['date'] for msg in messages))

    # ── Message Length ──
    avg_length = {}
    for sender in senders:
        sender_texts = [msg['text'] for msg in text_msgs if msg['sender'] == sender]
        if sender_texts:
            avg_length[sender] = sum(len(t) for t in sender_texts) / len(sender_texts)
        else:
            avg_length[sender] = 0

    # ── Messages per Month ──
    msgs_per_month = defaultdict(lambda: Counter())
    for msg in messages:
        month_key = msg['datetime'].strftime('%Y-%m')
        msgs_per_month[month_key][msg['sender']] += 1

    # ── Messages per Hour (Activity Heatmap) ──
    msgs_per_hour = defaultdict(lambda: Counter())
    for msg in messages:
        msgs_per_hour[msg['hour']][msg['sender']] += 1

    # ── Most Active Hours ──
    hour_totals = Counter()
    for msg in messages:
        hour_totals[msg['hour']] += 1

    # ── Emoji Stats ──
    emoji_per_sender = defaultdict(list)
    for msg in text_msgs:
        emojis = extract_emojis(msg['text'])
        emoji_per_sender[msg['sender']].extend(emojis)

    top_emojis = {}
    for sender in senders:
        all_emojis = emoji_per_sender[sender]
        # Flatten individual characters from emoji sequences
        flat = []
        for e in all_emojis:
            flat.append(e)  # Keep sequences together
        top_emojis[sender] = Counter(flat).most_common(10)

    # ── Conversation Starters ──
    starters = Counter()
    prev_date = None
    prev_sender = None
    for msg in messages:
        # New conversation = gap > 4 hours or new day
        if prev_date is None or (msg['datetime'] - prev_date).total_seconds() > 4 * 3600:
            starters[msg['sender']] += 1
        prev_date = msg['datetime']
        prev_sender = msg['sender']

    # ── Reply Time Analysis ──
    reply_times = defaultdict(list)
    for i in range(1, len(messages)):
        curr = messages[i]
        prev = messages[i - 1]
        if curr['sender'] != prev['sender']:
            gap = (curr['datetime'] - prev['datetime']).total_seconds()
            if 0 < gap < 24 * 3600:  # Ignore gaps > 24h
                reply_times[curr['sender']].append(gap)

    avg_reply = {}
    median_reply = {}
    for sender in senders:
        times = sorted(reply_times[sender])
        if times:
            avg_reply[sender] = sum(times) / len(times)
            median_reply[sender] = times[len(times) // 2]
        else:
            avg_reply[sender] = 0
            median_reply[sender] = 0

    # ── Longest Streak (consecutive days with messages) ──
    msg_dates = sorted(set(msg['date'] for msg in messages))
    longest_streak = 1
    current_streak = 1
    streak_start = msg_dates[0]
    best_streak_start = msg_dates[0]
    for i in range(1, len(msg_dates)):
        if (msg_dates[i] - msg_dates[i - 1]).days == 1:
            current_streak += 1
            if current_streak > longest_streak:
                longest_streak = current_streak
                best_streak_start = streak_start
        else:
            current_streak = 1
            streak_start = msg_dates[i]

    # ── Busiest Day ──
    msgs_per_day = Counter(msg['date'] for msg in messages)
    busiest_day = msgs_per_day.most_common(1)[0]

    # ── Late Night Chat Stats (12 AM - 5 AM) ──
    late_night = Counter()
    for msg in messages:
        if 0 <= msg['hour'] < 5:
            late_night[msg['sender']] += 1

    # ── Word Frequency (top words) ──
    word_freq = defaultdict(Counter)
    stop_words = {'na', 'ni', 'ki', 'lo', 'ga', 'le', 'ha', 'haa', 'hmm', 'ok',
                  'the', 'a', 'an', 'is', 'it', 'to', 'and', 'of', 'in', 'for',
                  'null', 'media', 'omitted', 'this', 'message', 'was', 'edited',
                  'deleted'}
    for msg in text_msgs:
        words = re.findall(r'[a-zA-Z\u0C00-\u0C7F]{3,}', msg['text'].lower())
        for word in words:
            if word not in stop_words:
                word_freq[msg['sender']][word] += 1

    # ── "Deleted" message count ──
    deleted = Counter()
    for msg in messages:
        if 'deleted' in msg['text'].lower() or msg['text'] == 'null':
            deleted[msg['sender']] += 1

    return {
        'filepath': filepath,
        'senders': senders,
        'total': total,
        'msgs_per_sender': msgs_per_sender,
        'media_per_sender': media_per_sender,
        'first_date': first_date,
        'last_date': last_date,
        'total_days': total_days,
        'active_days': active_days,
        'avg_length': avg_length,
        'msgs_per_month': dict(msgs_per_month),
        'msgs_per_hour': dict(msgs_per_hour),
        'hour_totals': hour_totals,
        'top_emojis': top_emojis,
        'starters': starters,
        'avg_reply': avg_reply,
        'median_reply': median_reply,
        'longest_streak': longest_streak,
        'best_streak_start': best_streak_start,
        'busiest_day': busiest_day,
        'late_night': late_night,
        'word_freq': word_freq,
        'deleted': deleted,
        'msgs_per_day': msgs_per_day,
    }


# ─── Report Generator ─────────────────────────────────────────────────

def generate_report(stats):
    """Generate a formatted markdown report from statistics."""
    lines = []
    w = lines.append  # shorthand

    filename = os.path.basename(stats['filepath'])
    senders = sorted(stats['senders'])

    w(f"# 📊 WhatsApp Chat Statistics Report")
    w(f"")
    w(f"**File:** `{filename}`")
    w(f"**Period:** {stats['first_date'].strftime('%d %b %Y')} → {stats['last_date'].strftime('%d %b %Y')}")
    w(f"**Duration:** {stats['total_days']} days ({stats['active_days']} active)")
    w(f"**Total Messages:** {stats['total']:,}")
    w(f"")

    # ── Message Distribution ──
    w(f"---")
    w(f"## 📨 Message Distribution")
    w(f"")
    w(f"| Person | Messages | % | Media | Deleted/Null |")
    w(f"|--------|----------|---|-------|-------------|")
    for sender in senders:
        count = stats['msgs_per_sender'][sender]
        pct = (count / stats['total']) * 100
        media = stats['media_per_sender'].get(sender, 0)
        deleted = stats['deleted'].get(sender, 0)
        w(f"| {sender} | {count:,} | {pct:.1f}% | {media} | {deleted} |")
    w(f"")

    # ── Average Message Length ──
    w(f"## 📏 Average Message Length (characters)")
    w(f"")
    for sender in senders:
        length = stats['avg_length'][sender]
        bar = '█' * int(length / 5)
        w(f"- **{sender}:** {length:.0f} chars {bar}")
    w(f"")

    # ── Activity Stats ──
    w(f"---")
    w(f"## 📅 Activity Stats")
    w(f"")
    w(f"- **Messages per day (avg):** {stats['total'] / stats['total_days']:.1f}")
    w(f"- **Messages per active day:** {stats['total'] / stats['active_days']:.1f}")
    w(f"- **Busiest day:** {stats['busiest_day'][0].strftime('%d %b %Y')} ({stats['busiest_day'][1]} msgs)")
    w(f"- **Longest streak:** {stats['longest_streak']} consecutive days (from {stats['best_streak_start'].strftime('%d %b %Y')})")
    w(f"")

    # ── Monthly Breakdown ──
    w(f"## 📆 Monthly Breakdown")
    w(f"")
    w(f"| Month | " + " | ".join(senders) + " | Total |")
    w(f"|-------|" + "|".join(["-----"] * len(senders)) + "|-------|")
    for month in sorted(stats['msgs_per_month'].keys()):
        month_data = stats['msgs_per_month'][month]
        cols = [str(month_data.get(s, 0)) for s in senders]
        total = sum(month_data.values())
        bar = '▓' * (total // 50)
        w(f"| {month} | " + " | ".join(cols) + f" | {total} {bar} |")
    w(f"")

    # ── Hourly Activity Heatmap ──
    w(f"---")
    w(f"## 🕐 Hourly Activity Heatmap")
    w(f"")
    w(f"| Hour | Total | Bar |")
    w(f"|------|-------|-----|")
    max_hour_val = max(stats['hour_totals'].values()) if stats['hour_totals'] else 1
    for hour in range(24):
        count = stats['hour_totals'].get(hour, 0)
        bar_len = int((count / max_hour_val) * 30) if max_hour_val > 0 else 0
        bar = '█' * bar_len
        period = "AM" if hour < 12 else "PM"
        display_hour = hour if hour <= 12 else hour - 12
        if display_hour == 0:
            display_hour = 12
        w(f"| {display_hour:2d} {period} | {count:>5} | {bar} |")
    w(f"")

    # ── Top 3 Most Active Hours ──
    top3_hours = stats['hour_totals'].most_common(3)
    w(f"**Top 3 active hours:** " + ", ".join(
        f"{h}:00 ({c} msgs)" for h, c in top3_hours
    ))
    w(f"")

    # ── Late Night Stats ──
    w(f"## 🌙 Late Night Chat (12 AM - 5 AM)")
    w(f"")
    for sender in senders:
        count = stats['late_night'].get(sender, 0)
        w(f"- **{sender}:** {count} messages")
    w(f"")

    # ── Conversation Starters ──
    w(f"---")
    w(f"## 🗣️ Who Starts Conversations?")
    w(f"")
    w(f"*(Conversation = first message after 4+ hour gap)*")
    w(f"")
    for sender in senders:
        count = stats['starters'].get(sender, 0)
        w(f"- **{sender}:** {count} times")
    w(f"")

    # ── Reply Time ──
    w(f"## ⏱️ Reply Time Analysis")
    w(f"")
    w(f"| Person | Avg Reply | Median Reply |")
    w(f"|--------|-----------|-------------|")
    for sender in senders:
        avg = format_duration(stats['avg_reply'].get(sender, 0))
        med = format_duration(stats['median_reply'].get(sender, 0))
        w(f"| {sender} | {avg} | {med} |")
    w(f"")

    # ── Emoji Stats ──
    w(f"---")
    w(f"## 😍 Emoji Usage")
    w(f"")
    for sender in senders:
        total_emoji = sum(c for _, c in stats['top_emojis'].get(sender, []))
        w(f"### {sender} ({total_emoji} total emojis)")
        top = stats['top_emojis'].get(sender, [])
        if top:
            w(f"| Emoji | Count |")
            w(f"|-------|-------|")
            for emoji, count in top[:10]:
                w(f"| {emoji} | {count} |")
        else:
            w(f"*No emojis found*")
        w(f"")

    # ── Top Words ──
    w(f"---")
    w(f"## 💬 Top Words (3+ letters, excluding common words)")
    w(f"")
    for sender in senders:
        top_words = stats['word_freq'][sender].most_common(15)
        if top_words:
            w(f"### {sender}")
            w(f"| Word | Count |")
            w(f"|------|-------|")
            for word, count in top_words:
                w(f"| {word} | {count} |")
        w(f"")

    # ── Summary ──
    w(f"---")
    w(f"## 🏆 Quick Summary")
    w(f"")
    most_active = stats['msgs_per_sender'].most_common(1)[0]
    most_starter = stats['starters'].most_common(1)[0]
    w(f"- **Most talkative:** {most_active[0]} ({most_active[1]:,} messages)")
    w(f"- **Conversation initiator:** {most_starter[0]} ({most_starter[1]} times)")
    w(f"- **Chat span:** {stats['total_days']} days")
    w(f"- **Active days:** {stats['active_days']} / {stats['total_days']} ({stats['active_days']/stats['total_days']*100:.0f}%)")
    w(f"- **Busiest day:** {stats['busiest_day'][0].strftime('%d %b %Y')} with {stats['busiest_day'][1]} messages")
    w(f"- **Longest daily streak:** {stats['longest_streak']} days")
    w(f"")
    w(f"---")
    w(f"*Generated by WhatsApp Chat Stats Analyzer — {datetime.now().strftime('%Y-%m-%d %H:%M')}*")

    return '\n'.join(lines)


# ─── Main ──────────────────────────────────────────────────────────────

def main():
    # Fix Windows console encoding
    if sys.platform == 'win32':
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')

    # Default: analyze all .txt files in raw_chats/
    script_dir = os.path.dirname(os.path.abspath(__file__))
    raw_dir = os.path.join(script_dir, 'raw_chats')

    if len(sys.argv) > 1:
        files = sys.argv[1:]
    elif os.path.isdir(raw_dir):
        files = [
            os.path.join(raw_dir, f)
            for f in os.listdir(raw_dir)
            if f.endswith('.txt')
        ]
    else:
        print("Usage: python chat_stats.py [chat_file.txt ...]")
        print("   Or place .txt files in raw_chats/ directory")
        sys.exit(1)

    if not files:
        print("No chat files found!")
        sys.exit(1)

    reports_dir = os.path.join(script_dir, 'reports')
    os.makedirs(reports_dir, exist_ok=True)

    for filepath in files:
        print(f"\n{'='*60}")
        print(f"📂 Analyzing: {os.path.basename(filepath)}")
        print(f"{'='*60}")

        messages = parse_chat(filepath)
        if not messages:
            print("  ⚠️  No messages found!")
            continue

        print(f"  ✅ Parsed {len(messages):,} messages")
        stats = calculate_stats(messages, filepath)

        report = generate_report(stats)

        # Print to terminal
        print(report)

        # Save report
        base = os.path.splitext(os.path.basename(filepath))[0]
        report_path = os.path.join(reports_dir, f"stats_{base}.md")
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report)
        print(f"\n  💾 Report saved to: {report_path}")


if __name__ == '__main__':
    main()
