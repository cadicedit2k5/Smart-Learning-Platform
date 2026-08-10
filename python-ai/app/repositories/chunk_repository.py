import uuid
from collections.abc import Sequence

from sqlalchemy import delete
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import DocumentChunk


class ChunkRepository:

    def __init__(self, session: AsyncSession):
        self.session = session

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

        await self.session.execute(statement)

        self.session.add_all(list(chunks))