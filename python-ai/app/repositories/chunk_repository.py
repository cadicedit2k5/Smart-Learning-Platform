import uuid
from collections.abc import Sequence
from dataclasses import dataclass

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import DocumentChunk, ChunkEmbedding


@dataclass(frozen=True, slots=True)
class ChunkSearchResult:
    chunk: DocumentChunk
    distance: float


class ChunkRepository:

    def __init__(self, session: AsyncSession):
        self._session = session

    async def replace_document_chunks(self, *,
                                      course_id: uuid.UUID,
                                      document_id: uuid.UUID,
                                      document_version_id: uuid.UUID | None,
                                      chunks: Sequence[DocumentChunk]) -> None:
        statement = delete(DocumentChunk).where(DocumentChunk.course_id == course_id,
                                                DocumentChunk.document_id == document_id)

        if document_version_id is None:
            statement = statement.where(DocumentChunk.document_version_id.is_(None))
        else:
            statement = statement.where(DocumentChunk.document_version_id == document_version_id)

        await self._session.execute(statement)

        self._session.add_all(list(chunks))

    async def delete_document_chunks(self, *, course_id: uuid.UUID, document_id: uuid.UUID) -> None:

        statement = delete(DocumentChunk).where(
            DocumentChunk.course_id == course_id,
            DocumentChunk.document_id == document_id
        )

        await self._session.execute(statement)

    async def similarity_search(self, *, course_id: uuid.UUID,
                         model_key: str,
                         query_vector: list[float],
                         top_k: int) -> list[ChunkSearchResult]:
        if top_k <= 0:
            raise ValueError("top_k phải lớn hơn 0.")

        distance = ChunkEmbedding.embedding.cosine_distance(query_vector).label("distance")
        statement = (select(DocumentChunk, distance)
                     .join(ChunkEmbedding, ChunkEmbedding.chunk_id==DocumentChunk.id)
                     .where(DocumentChunk.course_id == course_id,
                            ChunkEmbedding.model_key == model_key)
                     .order_by(distance.asc())
                     .limit(top_k))

        res = await self._session.execute(statement)

        return [ ChunkSearchResult(chunk=row[0], distance=float(row[1])) for row in res.all()]