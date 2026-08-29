from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.messaging.events.document import DocumentDeletionRequestedEvent
from app.repositories.chunk_repository import ChunkRepository


class DocumentDeletionHandler:

    def __init__(self,*,
        session_factory: async_sessionmaker[AsyncSession],
    ) -> None:
        self._session_factory = session_factory

    async def handle(self, event: DocumentDeletionRequestedEvent) -> None:
        async with self._session_factory() as session:
            repository = ChunkRepository(session)

            async with session.begin():
                await repository.delete_document_chunks(
                    course_id=event.course_id,
                    document_id=event.document_id)