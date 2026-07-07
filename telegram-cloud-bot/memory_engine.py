import os
import glob
import chromadb
from google import genai
from chromadb.api.types import Documents, EmbeddingFunction, Embeddings

# Initialize the Genai client globally
_genai_client = None

def get_genai_client():
    global _genai_client
    if _genai_client is None:
        api_key = os.getenv("GEMINI_API_KEY")
        if api_key:
            _genai_client = genai.Client(api_key=api_key)
    return _genai_client

class GeminiEmbeddingFunction(EmbeddingFunction):
    def __call__(self, input: Documents) -> Embeddings:
        client = get_genai_client()
        results = []
        for doc in input:
            response = client.models.embed_content(
                model='gemini-embedding-001',
                contents=doc
            )
            results.append(response.embeddings[0].values)
        return results

class MemoryEngine:
    def __init__(self):
        self.api_key = os.getenv("GEMINI_API_KEY")
        self.client = None
        self.collection = None
        self.is_ready = False
        self.last_error = "None"
        self.genai_client = None
        
        if self.api_key:
            self.genai_client = genai.Client(api_key=self.api_key)
        else:
            self.last_error = "GEMINI_API_KEY is not set in environment."
            print("WARNING: GEMINI_API_KEY is not set. Cloud offline mode will not work.")

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
        if not self.api_key:
            return

        print("Building memory index in ChromaDB...")
        try:
            self.client = chromadb.Client()
            self.collection = self.client.create_collection(
                name="user_memory",
                embedding_function=GeminiEmbeddingFunction()
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
                print(f"Adding {len(documents)} chunks to vector database...")
                self.collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                self.is_ready = True
                self.last_error = "Success"
                print("Memory index built successfully!")
            else:
                self.last_error = "No documents could be parsed."
        except Exception as e:
            self.last_error = f"Index build failed: {str(e)}"
            print(self.last_error)

    def answer_question(self, question: str) -> str:
        if not self.is_ready:
            return f"[Cloud Mode ERROR] Memory index not ready. Reason: {self.last_error}"

        try:
            # 1. Retrieve relevant chunks
            results = self.collection.query(
                query_texts=[question],
                n_results=5
            )
            
            context = ""
            for doc in results['documents'][0]:
                context += doc + "\n\n"

            # 2. Ask Gemini
            prompt = f"""You are MAX, a highly capable, intelligent personal AI assistant. 
Currently, the user's phone is offline, so you are responding in Cloud Mode based on the user's external memory.
Use the following context to answer the user's question accurately. If the answer is not in the context, say you don't know based on current memory.

Context:
{context}

User Question:
{question}
"""
            response = self.genai_client.models.generate_content(
                model='gemini-2.0-flash',
                contents=prompt
            )
            return f"[Cloud Mode] {response.text}"
            
        except Exception as e:
            print(f"Error generating answer: {e}")
            return f"[Cloud Mode ERROR] An error occurred: {e}"
