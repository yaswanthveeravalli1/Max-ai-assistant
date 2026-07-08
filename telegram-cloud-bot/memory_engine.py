import os
import glob
import httpx
from google import genai
from supabase import create_client, Client


class MemoryEngine:
    def __init__(self):
        self.supabase: Client = None
        self.is_ready = False
        self.last_error = "None"
        self.provider = None
        self.gemini_client = None

        # Supabase config
        self.supabase_url = os.getenv("SUPABASE_URL")
        self.supabase_key = os.getenv("SUPABASE_KEY")

        # Detect available AI providers for response generation (priority order)
        self.groq_key = os.getenv("GROQ_API_KEY")
        self.openrouter_key = os.getenv("OPENROUTER_API_KEY")
        self.gemini_key = os.getenv("GEMINI_API_KEY")
        self.cohere_key = os.getenv("COHERE_API_KEY")

        if self.groq_key:
            self.provider = "groq"
        elif self.openrouter_key:
            self.provider = "openrouter"
        elif self.gemini_key:
            self.provider = "gemini"
        elif self.cohere_key:
            self.provider = "cohere"
        else:
            self.last_error = "No AI API key set. Add GROQ_API_KEY, OPENROUTER_API_KEY, GEMINI_API_KEY, or COHERE_API_KEY."
            print(f"WARNING: {self.last_error}")

        if self.provider:
            print(f"Using AI provider: {self.provider.upper()}")

        # Gemini client — required for embeddings (regardless of response provider)
        if self.gemini_key:
            self.gemini_client = genai.Client(api_key=self.gemini_key)
        else:
            print("WARNING: GEMINI_API_KEY is required for embedding generation.")

    # ── Embedding helpers ──────────────────────────────────────────────

    def _get_embedding(self, text: str) -> list:
        """Generate embedding for a single text using Gemini text-embedding-004."""
        result = self.gemini_client.models.embed_content(
            model='text-embedding-004',
            contents=text
        )
        return result.embeddings[0].values

    def _get_embeddings_batch(self, texts: list) -> list:
        """Generate embeddings for multiple texts in batches of 20."""
        all_embeddings = []
        batch_size = 20
        total_batches = (len(texts) - 1) // batch_size + 1
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            batch_num = i // batch_size + 1
            print(f"  Embedding batch {batch_num}/{total_batches} ({len(batch)} texts)...")
            result = self.gemini_client.models.embed_content(
                model='text-embedding-004',
                contents=batch
            )
            for emb in result.embeddings:
                all_embeddings.append(emb.values)
        return all_embeddings

    # ── Chunking ───────────────────────────────────────────────────────

    def chunk_text(self, text, chunk_size=500, overlap=50):
        """Split text into overlapping chunks."""
        chunks = []
        start = 0
        text_len = len(text)
        while start < text_len:
            end = min(start + chunk_size, text_len)
            chunks.append(text[start:end])
            start += chunk_size - overlap
        return chunks

    # ── Index management ───────────────────────────────────────────────

    def build_index(self):
        """Connect to Supabase. Seed memory only on first run (table empty).

        Unlike ChromaDB, this does NOT download models or build indexes locally.
        It connects to the remote database. If data already exists, startup is instant.
        """
        if not self.supabase_url or not self.supabase_key:
            self.last_error = "SUPABASE_URL and SUPABASE_KEY environment variables must be set."
            print(f"ERROR: {self.last_error}")
            return

        if not self.gemini_client:
            self.last_error = "GEMINI_API_KEY is required for embedding generation."
            print(f"ERROR: {self.last_error}")
            return

        if not self.provider:
            return

        try:
            print("Connecting to Supabase...")
            self.supabase = create_client(self.supabase_url, self.supabase_key)

            # Check if memory chunks already exist (instant query, no heavy work)
            result = self.supabase.table("memory_chunks").select("id", count="exact").limit(1).execute()
            existing_count = result.count if result.count is not None else 0

            if existing_count > 0:
                print(f"Memory already indexed ({existing_count} chunks in Supabase). Ready!")
                self.is_ready = True
                self.last_error = "Success"
                return

            # First run only: seed from markdown files
            print("First run — seeding memory into Supabase...")
            self._seed_memory()

        except Exception as e:
            self.last_error = f"Supabase setup failed: {str(e)}"
            print(f"ERROR: {self.last_error}")

    def _seed_memory(self):
        """One-time operation: read .md files, embed via Gemini API, store in Supabase.

        This only runs when the memory_chunks table is empty (first deployment).
        On subsequent restarts, build_index() detects existing data and skips this entirely.
        """
        memory_files = glob.glob('memory/**/*.md', recursive=True)
        if not memory_files:
            self.last_error = f"No memory files found. CWD: {os.getcwd()}"
            print(self.last_error)
            return

        all_chunks = []
        all_sources = []
        all_indices = []

        for file_path in memory_files:
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                chunks = self.chunk_text(content)
                for i, chunk in enumerate(chunks):
                    all_chunks.append(chunk)
                    all_sources.append(file_path)
                    all_indices.append(i)
            except Exception as e:
                print(f"Error reading {file_path}: {e}")

        if not all_chunks:
            self.last_error = "No documents could be parsed."
            print(self.last_error)
            return

        # Generate embeddings via Gemini API (no local ONNX model needed)
        print(f"Generating embeddings for {len(all_chunks)} chunks via Gemini API...")
        embeddings = self._get_embeddings_batch(all_chunks)

        # Store in Supabase (persistent — survives restarts)
        print("Storing chunks in Supabase...")
        rows = []
        for i in range(len(all_chunks)):
            rows.append({
                "content": all_chunks[i],
                "source": all_sources[i],
                "chunk_index": all_indices[i],
                "embedding": str(embeddings[i])
            })

        # Batch insert
        batch_size = 50
        for i in range(0, len(rows), batch_size):
            batch = rows[i:i + batch_size]
            self.supabase.table("memory_chunks").insert(batch).execute()

        self.is_ready = True
        self.last_error = "Success"
        print(f"Memory seeded! {len(all_chunks)} chunks stored in Supabase.")

    # ── Prompt construction ────────────────────────────────────────────

    def _build_prompt(self, context: str, question: str) -> str:
        return f"""You are MAX, a highly capable, intelligent personal AI assistant. 
Currently, the user's phone is offline, so you are responding in Cloud Mode based on the user's external memory.
Use the following context to answer the user's question accurately and naturally.
If the answer is not in the context, say you don't have that info in your current memory.
Keep responses concise and helpful.

Context from memory:
{context}

User Question:
{question}"""

    # ── AI provider methods ────────────────────────────────────────────

    def _ask_groq(self, prompt: str) -> str:
        response = httpx.post(
            "https://api.groq.com/openai/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {self.groq_key}",
                "Content-Type": "application/json"
            },
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.7,
                "max_tokens": 1024
            },
            timeout=30.0
        )
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]

    def _ask_openrouter(self, prompt: str) -> str:
        response = httpx.post(
            "https://openrouter.ai/api/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {self.openrouter_key}",
                "Content-Type": "application/json"
            },
            json={
                "model": "meta-llama/llama-3.3-70b-instruct:free",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.7,
                "max_tokens": 1024
            },
            timeout=30.0
        )
        response.raise_for_status()
        return response.json()["choices"][0]["message"]["content"]

    def _ask_gemini(self, prompt: str) -> str:
        response = self.gemini_client.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt
        )
        return response.text

    def _ask_cohere(self, prompt: str) -> str:
        response = httpx.post(
            "https://api.cohere.com/v2/chat",
            headers={
                "Authorization": f"Bearer {self.cohere_key}",
                "Content-Type": "application/json"
            },
            json={
                "model": "command-r",
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0.7,
                "max_tokens": 1024
            },
            timeout=30.0
        )
        response.raise_for_status()
        return response.json()["message"]["content"][0]["text"]

    def _generate_response(self, prompt: str) -> str:
        """Try the configured provider, fallback to others if it fails."""
        providers = {
            "groq": self._ask_groq,
            "openrouter": self._ask_openrouter,
            "gemini": self._ask_gemini,
            "cohere": self._ask_cohere
        }

        # Try primary provider first
        try:
            return providers[self.provider](prompt)
        except Exception as e:
            print(f"Primary provider ({self.provider}) failed: {e}")

        # Fallback to other available providers
        fallback_order = ["groq", "openrouter", "gemini", "cohere"]
        keys = {
            "groq": self.groq_key,
            "openrouter": self.openrouter_key,
            "gemini": self.gemini_key,
            "cohere": self.cohere_key
        }

        for fallback in fallback_order:
            if fallback != self.provider and keys.get(fallback):
                try:
                    print(f"Trying fallback: {fallback.upper()}")
                    return providers[fallback](prompt)
                except Exception as e2:
                    print(f"Fallback {fallback} also failed: {e2}")

        raise Exception("All AI providers failed.")

    # ── Main query method ──────────────────────────────────────────────

    def answer_question(self, question: str) -> str:
        if not self.is_ready:
            return f"[Cloud Mode ERROR] Memory index not ready. Reason: {self.last_error}"

        try:
            # 1. Embed the question via Gemini API (lightweight API call, no local model)
            query_embedding = self._get_embedding(question)

            # 2. Vector similarity search in Supabase (server-side, no local compute)
            results = self.supabase.rpc("match_memory", {
                "query_embedding": str(query_embedding),
                "match_count": 5
            }).execute()

            if not results.data:
                return "[Cloud Mode] No relevant memory found for your question."

            context = "\n\n".join([r["content"] for r in results.data])

            # 3. Build prompt and ask AI
            prompt = self._build_prompt(context, question)
            answer = self._generate_response(prompt)

            return f"[Cloud Mode - {self.provider.upper()}] {answer}"

        except Exception as e:
            print(f"Error generating answer: {e}")
            return f"[Cloud Mode ERROR] {e}"
