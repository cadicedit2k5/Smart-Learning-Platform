from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest
from langchain_core.documents import Document
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine

from app.configs.config import get_settings
from app.repositories.chunk_repository import ChunkRepository
from app.services.knowledge_ingestion_service import KnowledgeIngestionService, KnowledgeSource, KnowledgeSourceType


@pytest.fixture
async def db_session():
    engine = create_async_engine(get_settings().database_url)
    try:
        async with engine.connect() as connection:
            transaction = await connection.begin()
            try:
                async with AsyncSession(bind=connection, expire_on_commit=False,
                                        join_transaction_mode="create_savepoint") as session:
                    yield session
            finally:
                await transaction.rollback()
    finally:
        await engine.dispose()


@pytest.fixture
def embeddings():
    return MagicMock(
        aembed_documents=AsyncMock(side_effect=lambda texts: [[1., 0., 0.] for _ in texts]),
        aembed_query=AsyncMock(return_value=[1., 0., 0.]),
    )


@pytest.fixture
async def indexed_source(db_session, embeddings, settings):
    source_id, version_id = uuid4(), uuid4()
    settings.embedding_model = f"test-{uuid4()}"
    source = KnowledgeSource(course_id=uuid4(), source_type=KnowledgeSourceType.DOCUMENT,
                             source_id=source_id, source_version_id=version_id, document_id=source_id,
                             document_version_id=version_id)
    service = KnowledgeIngestionService(session=db_session, embeddings=embeddings,
                                        repository=ChunkRepository(db_session), settings=settings)
    await service.ingest(source=source, documents=[Document(page_content="Dependency Injection reduces coupling.")])
    return source
