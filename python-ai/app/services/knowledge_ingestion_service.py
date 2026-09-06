import uuid
from dataclasses import dataclass
from enum import StrEnum

from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from sqlalchemy.ext.asyncio import AsyncSession

from app.configs.config import Settings
from app.infrastructure.ai.embeddings import get_embedding_model_key
from app.infrastructure.documents.source_locator import extract_source_locator
from app.models import DocumentChunk, ChunkEmbedding
from app.repositories.chunk_repository import ChunkRepository

import logging

logger = logging.getLogger(__name__)

@dataclass(frozen=True)
class IngestionResult:
    chunk_count: int
    model_key: str

class KnowledgeSourceType(StrEnum):
    DOCUMENT = "DOCUMENT"
    TOPIC = "TOPIC"


@dataclass(frozen=True, slots=True)
class KnowledgeSource:
    course_id: uuid.UUID
    source_type: KnowledgeSourceType
    source_id: uuid.UUID
    source_version_id: uuid.UUID | None = None

    document_id: uuid.UUID | None = None
    document_version_id: uuid.UUID | None = None

    chapter_id: uuid.UUID | None = None
    topic_id: uuid.UUID | None = None


class KnowledgeIngestionService:

    def __init__(self, *, session: AsyncSession, embeddings: Embeddings, repository: ChunkRepository, settings: Settings):
        self._session = session
        self._embeddings = embeddings
        self._repository = repository
        self._settings = settings

    async def ingest(self, *, source: KnowledgeSource, documents: list[Document]) -> IngestionResult:
        documents = [document for document in documents if document.page_content.strip()]
        model_key = get_embedding_model_key(self._settings)

        if not documents:
            async with self._session.begin():
                await self._repository.replace_source_chunks(
                    course_id=source.course_id,
                    source_type=source.source_type.value,
                    source_id=source.source_id,
                    chunks=[],
                )

            return IngestionResult(chunk_count=0, model_key=model_key)

        texts = [document.page_content.strip() for document in documents]
        logger.info("Texts prepared: count=%d lengths=%s", len(texts), [len(text) for text in texts])
        vectors = await self._embeddings.aembed_documents(texts)
        logger.info(
            "Embedding completed: vectors=%d dimensions=%s",
            len(vectors),
            [len(vector) for vector in vectors],
        )

        if len(vectors) != len(documents):
            raise RuntimeError("Embedding provider returned an unexpected vector count")

        chunks = []

        for index, (document, vector) in enumerate(zip(documents, vectors, strict=True)):
            locator = document.metadata.get("source_locator")

            if not isinstance(locator, dict):
                locator = extract_source_locator(document=document)

            chunk = DocumentChunk(
                course_id=source.course_id,
                source_type=source.source_type.value,
                source_id=source.source_id,
                source_version_id=source.source_version_id,
                document_id=source.document_id,
                document_version_id=source.document_version_id,
                chapter_id=source.chapter_id,
                topic_id=source.topic_id,
                chunk_index=index,
                content=document.page_content.strip(),
                source_locator=locator,
            )

            chunk.embeddings.append(ChunkEmbedding(model_key=model_key, embedding=vector))
            chunks.append(chunk)

        async with self._session.begin():
            await self._repository.replace_source_chunks(
                course_id=source.course_id,
                source_type=source.source_type.value,
                source_id=source.source_id,
                chunks=chunks,
            )

            await self._session.flush()

        logger.info(
            "Knowledge chunks persisted: type=%s id=%s chunks=%d",
            source.source_type.value,
            source.source_id,
            len(chunks),
        )

        return IngestionResult(chunk_count=len(chunks), model_key=model_key)