import uuid
from dataclasses import dataclass

from langchain_core.embeddings import Embeddings

from app.configs.config import Settings
from app.infrastructure.ai.embeddings import get_embedding_model_key
from app.repositories.chunk_repository import ChunkRepository


@dataclass(frozen=True, slots=True)
class RetrievedChunk:
    chunk_id: uuid.UUID
    document_id: uuid.UUID
    document_version_id: uuid.UUID | None

    content: str
    source_locator: dict | None

    distance: float

class RetrievalService:

    def __init__(self, *, embeddings: Embeddings, repository: ChunkRepository, settings: Settings):
        self._embeddings = embeddings
        self._repository = repository
        self._settings = settings

    async def retrieve(self, *, course_id: uuid.UUID, question: str,
                       top_k: int | None = None) -> list[RetrievedChunk]:
        question = question.strip()

        if not question:
            raise ValueError("Không có câu hỏi nào!")

        effective_top_k = (top_k if top_k is not None else self._settings.retrieve_top_k)

        if effective_top_k < 0:
            raise ValueError("top_k must be greater than 0!")

        query_vector = await self._embeddings.aembed_query(question)

        if len(query_vector) != self._settings.embedding_dimensions:
            raise RuntimeError("Embedding provider returned an expected vector count")

        model_key = get_embedding_model_key(self._settings)

        results = await self._repository.similarity_search(
            course_id=course_id,
            model_key=model_key,
            query_vector=query_vector,
            top_k=effective_top_k,
        )

        return [
            RetrievedChunk(
                chunk_id=result.chunk.id,
                document_id=result.chunk.document_id,
                document_version_id=(
                    result.chunk.document_version_id
                ),
                content=result.chunk.content,
                source_locator=(
                    result.chunk.source_locator
                ),
                distance=result.distance,
            )
            for result in results
        ]

