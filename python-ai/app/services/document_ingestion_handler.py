from pathlib import Path
from tempfile import TemporaryDirectory

from langchain_core.embeddings import Embeddings
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.configs.config import Settings
from app.infrastructure.documents.loader import DocumentLoader
from app.infrastructure.storage.minio_storage import MinioStorage
from app.messaging.events.document import DocumentIngestionRequestedEvent
from app.repositories.chunk_repository import ChunkRepository
from app.services.knowledge_ingestion_service import IngestionResult, KnowledgeIngestionService, KnowledgeSource, \
    KnowledgeSourceType


class DocumentIngestionHandler:

    def __init__(self, *, session_factory: async_sessionmaker[AsyncSession], storage: MinioStorage, loader: DocumentLoader, embeddings: Embeddings, settings: Settings) -> None:
        self._session_factory = session_factory
        self._storage = storage
        self._loader = loader
        self._embeddings = embeddings
        self._settings = settings

    async def handle(self, event: DocumentIngestionRequestedEvent) -> IngestionResult:
        suffix = Path(event.file_name).suffix.lower()

        with TemporaryDirectory(prefix="document-ingestion-") as temp_dir:
            file_path = Path(temp_dir) / f"document{suffix}"

            await self._storage.download_to_file(
                bucket=event.storage_bucket,
                object_name=event.storage_key,
                destination=file_path,
            )

            documents = await self._loader.load(file_path)

            if not any(document.page_content.strip() for document in documents):
                raise ValueError("Document không thể sử dụng để chunk")

            async with self._session_factory() as session:
                repository = ChunkRepository(session)

                ingestion_service = KnowledgeIngestionService(
                    session=session,
                    embeddings=self._embeddings,
                    repository=repository,
                    settings=self._settings,
                )

                source = KnowledgeSource(
                    course_id=event.course_id,
                    source_type=KnowledgeSourceType.DOCUMENT,
                    source_id=event.document_id,
                    source_version_id=event.document_version_id,
                    document_id=event.document_id,
                    document_version_id=event.document_version_id,
                )

                return await ingestion_service.ingest(source=source, documents=documents)