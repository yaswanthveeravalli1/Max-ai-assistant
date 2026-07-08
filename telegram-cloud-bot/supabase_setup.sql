-- ============================================
-- Supabase Setup for MAX Cloud Bot Memory
-- Run this in Supabase SQL Editor
-- ============================================

-- 1. Enable pgvector extension
create extension if not exists vector;

-- 2. Create memory chunks table (Long Term Memory)
create table if not exists memory_chunks (
  id bigserial primary key,
  content text not null,
  source text not null,
  chunk_index int not null,
  embedding vector(768),
  created_at timestamptz default now()
);

-- 3. Create index for fast vector search
create index if not exists memory_chunks_embedding_idx
  on memory_chunks
  using ivfflat (embedding vector_cosine_ops)
  with (lists = 10);

-- 4. Create similarity search function
create or replace function match_memory(
  query_embedding text,
  match_count int default 5
)
returns table (
  id bigint,
  content text,
  source text,
  similarity float
)
language plpgsql
as $$
begin
  return query
  select
    mc.id,
    mc.content,
    mc.source,
    1 - (mc.embedding <=> query_embedding::vector) as similarity
  from memory_chunks mc
  order by mc.embedding <=> query_embedding::vector
  limit match_count;
end;
$$;

-- 5. Create chat history table (Short Term Memory)
create table if not exists chat_history (
  id bigserial primary key,
  user_id text not null,
  role text not null, -- 'user' or 'assistant'
  content text not null,
  created_at timestamptz default now()
);

-- 6. Create user summaries table (Rolling context)
create table if not exists user_summaries (
  user_id text primary key,
  summary text not null,
  last_updated timestamptz default now()
);
