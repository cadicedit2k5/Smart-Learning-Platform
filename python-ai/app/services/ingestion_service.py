import uuid
from dataclasses import dataclass
from pathlib import Path

from langchain_core.embeddings import Embeddings
from sqlalchemy.ext.asyncio import AsyncSession

from app.configs.config import Settings
from app.infrastructure.ai.embeddings import get_embedding_model_key
from app.infrastructure.documents.loader import DocumentLoader
from app.infrastructure.documents.source_locator import extract_source_locator
from app.models import DocumentChunk, ChunkEmbedding
from app.repositories.chunk_repository import ChunkRepository


@dataclass(frozen=True)
class IngestionResult:
    chunk_count: int
    model_key: str

class IngestionService:
    def __init__(self, *, session: AsyncSession, loader: DocumentLoader,
            embeddings: Embeddings, repository: ChunkRepository, settings: Settings):
        self._session = session
        self._loader = loader
        self._embeddings = embeddings
        self._repository = repository
        self._settings = settings

    async def ingest(self, file_path: Path, course_id: uuid.UUID, document_id: uuid.UUID,
                     document_version_id: uuid.UUID,
                     chapter_id: uuid.UUID | None = None, topic_id: uuid.UUID | None = None) -> IngestionResult:
        documents = await self._loader.load(file_path)

        documents = [document for document in documents if document.page_content.strip()]

        if not documents:
            raise ValueError("Document không thể sử dụng để chunk")

        texts = [document.page_content.strip() for document in documents]

        vectors = await self._embeddings.aembed_documents(texts)
        if len(vectors) != len(documents):
            raise RuntimeError("Embedding provider returned an expected vector count")

        model_key = get_embedding_model_key(self._settings)
        chunks = []

        for idx, (document, vector) in enumerate(zip(documents, vectors, strict=True)):
            chunk = DocumentChunk(
                course_id=course_id,
                document_id=document_id,
                document_version_id=document_version_id,
                chapter_id=chapter_id,
                topic_id=topic_id,
                chunk_index=idx,
                content=document.page_content.strip(),
                source_locator=extract_source_locator(document=document),
            )

            chunk.embeddings.append(ChunkEmbedding(model_key=model_key, embedding=vector))

            chunks.append(chunk)
        async with self._session.begin() as session:
            await self._repository.replace_document_chunks(
                course_id=course_id,
                document_id=document_id,
                document_version_id=document_version_id,
                chunks=chunks,
            )

        return IngestionResult(chunk_count=len(chunks), model_key=model_key)