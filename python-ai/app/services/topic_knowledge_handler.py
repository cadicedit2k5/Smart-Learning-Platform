from langchain_core.embeddings import Embeddings
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.configs.config import Settings
from app.messaging.events.topic import TopicKnowledgeIndexRequestedEvent
from app.repositories.chunk_repository import ChunkRepository
from app.services.knowledge_ingestion_service import KnowledgeIngestionService, IngestionResult
from app.services.knowledge_source import KnowledgeSource, KnowledgeSourceType
from app.services.topic_knowledge_extractor import TopicKnowledgeExtractor


class TopicKnowledgeHandler:

    def __init__(self, *, session_factory: async_sessionmaker[AsyncSession], embeddings: Embeddings, extractor: TopicKnowledgeExtractor, settings: Settings):
        self._session_factory = session_factory
        self._embeddings = embeddings
        self._extractor = extractor
        self._settings = settings

    async def handle(self, event: TopicKnowledgeIndexRequestedEvent) -> IngestionResult | None:
        source = KnowledgeSource(
            course_id=event.course_id,
            source_type=KnowledgeSourceType.TOPIC,
            source_id=event.topic_id,
            chapter_id=event.chapter_id,
            topic_id=event.topic_id,
        )

        async with self._session_factory() as session:
            repository = ChunkRepository(session)

            if event.operation == "DELETE":
                async with session.begin():
                    await repository.delete_source_chunks(
                        course_id=source.course_id,
                        source_type=source.source_type.value,
                        source_id=source.source_id,
                    )

                return None

            documents = self._extractor.extract(event)

            ingestion_service = KnowledgeIngestionService(
                session=session,
                embeddings=self._embeddings,
                repository=repository,
                settings=self._settings,
            )

            return await ingestion_service.ingest(source=source, documents=documents)