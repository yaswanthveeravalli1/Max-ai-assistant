---
name: session-continuity-system
description: On session start, always read current_context.md then diary.md before responding
type: reference
---

# Session Continuity Protocol

Every new session:
1. Read profile/current_context.md first (summarized active context)
2. Read diary/diary.md (last 7-30 days)
3. Continue conversations naturally without asking user to repeat details
4. Remember ongoing stories, people, emotions, and plans

Memory priority: current_context.md → latest diary entries → older diary memories

## File structure
- C:/Users/yaswa/.claude/memory/diary/diary.md — permanent chronological diary entries
- C:/Users/yaswa/.claude/memory/profile/current_context.md — active context (people, goals, emotional state, things to follow up)
- C:/Users/yaswa/.claude/memory/profile/user_profile.md — user identity record
- C:/Users/yaswa/.claude/memory/relationships/ — relationship records
- C:/Users/yaswa/.claude/memory/chat_analysis/ — WhatsApp analysis framework, scripts & reports
- C:/Users/yaswa/.claude/memory/system/ — session protocols & preferences

## Update behavior
- Update profile/current_context.md at end of each session
- Move older context into diary/diary.md permanently
- Keep only active/recent context in current_context.md

## Key points about the user
- Uses diary-only memory system (no technical categorization)
- Wants concise but emotionally meaningful entries
- Entries must have timestamps
- Preserve old memories unless explicitly deleted
- No sharing of diary content without permission