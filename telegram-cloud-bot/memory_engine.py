import os
import glob
import chromadb
import httpx
from google import genai
from chromadb.utils.embedding_functions import DefaultEmbeddingFunction

class MemoryEngine:
    def __init__(self):
        self.client = None
        self.collection = None
        self.is_ready = False
        self.last_error = "None"
        self.provider = None
        
        # Detect available providers (priority order)
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

    def chunk_text(self, text, chunk_size=500, overlap=50):
        chunks = []
        start = 0
        text_len = len(text)
        while start < text_len:
            end = min(start + chunk_size, text_len)
            chunks.append(text[start:end])
            start += chunk_size - overlap
        return chunks

    def build_index(self):
        if not self.provider:
            return

        print("Building memory index in ChromaDB (local embeddings, no API needed)...")
        try:
            self.client = chromadb.Client()
            # Use ChromaDB's default local embedding function (runs on CPU, no API key needed)
            self.collection = self.client.create_collection(
                name="user_memory",
                embedding_function=DefaultEmbeddingFunction()
            )

            memory_files = glob.glob('memory/**/*.md', recursive=True)
            if not memory_files:
                self.last_error = f"No memory files found. CWD: {os.getcwd()}"
                print(self.last_error)
                return

            documents = []
            metadatas = []
            ids = []
            
            doc_id = 0
            for file_path in memory_files:
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                        
                    chunks = self.chunk_text(content)
                    for i, chunk in enumerate(chunks):
                        documents.append(chunk)
                        metadatas.append({"source": file_path, "chunk": i})
                        ids.append(f"doc_{doc_id}")
                        doc_id += 1
                except Exception as e:
                    print(f"Error reading {file_path}: {e}")

            if documents:
                print(f"Embedding {len(documents)} chunks locally...")
                self.collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                self.is_ready = True
                self.last_error = "Success"
                print(f"Memory index built successfully! Provider: {self.provider.upper()}")
            else:
                self.last_error = "No documents could be parsed."
        except Exception as e:
            self.last_error = f"Index build failed: {str(e)}"
            print(self.last_error)

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
        client = genai.Client(api_key=self.gemini_key)
        response = client.models.generate_content(
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

    def answer_question(self, question: str) -> str:
        if not self.is_ready:
            return f"[Cloud Mode ERROR] Memory index not ready. Reason: {self.last_error}"

        try:
            # 1. Retrieve relevant chunks from vector DB
            results = self.collection.query(
                query_texts=[question],
                n_results=5
            )
            
            context = "\n\n".join(results['documents'][0])

            # 2. Build prompt and ask AI
            prompt = self._build_prompt(context, question)
            answer = self._generate_response(prompt)
            
            return f"[Cloud Mode - {self.provider.upper()}] {answer}"
            
        except Exception as e:
            print(f"Error generating answer: {e}")
            return f"[Cloud Mode ERROR] {e}"
