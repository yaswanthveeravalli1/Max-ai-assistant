import os
import glob
import chromadb
import google.generativeai as genai
from chromadb.api.types import Documents, EmbeddingFunction, Embeddings

class GeminiEmbeddingFunction(EmbeddingFunction):
    def __call__(self, input: Documents) -> Embeddings:
        model = 'models/text-embedding-004'
        title = "Memory Embedding"
        return [genai.embed_content(model=model,
                                    content=doc,
                                    task_type="retrieval_document",
                                    title=title)["embedding"] for doc in input]

class MemoryEngine:
    def __init__(self):
        self.api_key = os.getenv("GEMINI_API_KEY")
        self.client = None
        self.collection = None
        self.is_ready = False
        
        if self.api_key:
            genai.configure(api_key=self.api_key)
            self.model = genai.GenerativeModel('gemini-2.0-flash')
        else:
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
        self.client = chromadb.Client()
        self.collection = self.client.create_collection(
            name="user_memory",
            embedding_function=GeminiEmbeddingFunction()
        )

        memory_files = glob.glob('memory/**/*.md', recursive=True)
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
            # Batch add to avoid hitting API rate limits if there are many chunks.
            # We'll just add them all at once for now, assuming the dataset is small.
            try:
                print(f"Adding {len(documents)} chunks to vector database...")
                self.collection.add(
                    documents=documents,
                    metadatas=metadatas,
                    ids=ids
                )
                self.is_ready = True
                print("Memory index built successfully!")
            except Exception as e:
                print(f"Failed to build index: {e}")

    def answer_question(self, question: str) -> str:
        if not self.is_ready:
            return "[Cloud Mode] I'm sorry, my memory index is not ready or the API key is missing."

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
            response = self.model.generate_content(prompt)
            return f"[Cloud Mode] {response.text}"
            
        except Exception as e:
            print(f"Error generating answer: {e}")
            return f"[Cloud Mode] An error occurred while accessing my memory: {e}"
