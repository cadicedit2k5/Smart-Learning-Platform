from uuid import uuid4

import pytest
from langchain_core.documents import Document
from sqlalchemy import func, select

from app.models import ChunkEmbedding, DocumentChunk
from app.repositories.chunk_repository import ChunkRepository
from app.services.knowledge_ingestion_service import KnowledgeIngestionService, KnowledgeSource, KnowledgeSourceType


@pytest.mark.parametrize("source_type", list(KnowledgeSourceType))
async def test_ingest_and_reindex_source(db_session, embeddings, settings, source_type):
    source_id, version_id = uuid4(), uuid4()
    source = KnowledgeSource(course_id=uuid4(), source_type=source_type, source_id=source_id,
                             source_version_id=version_id if source_type == KnowledgeSourceType.DOCUMENT else None,
                             document_id=source_id if source_type == KnowledgeSourceType.DOCUMENT else None,
                             document_version_id=version_id if source_type == KnowledgeSourceType.DOCUMENT else None,
                             topic_id=source_id if source_type == KnowledgeSourceType.TOPIC else None)
    repository = ChunkRepository(db_session)
    service = KnowledgeIngestionService(session=db_session, embeddings=embeddings,
                                        repository=repository, settings=settings)
    result = await service.ingest(source=source, documents=[
        Document(page_content="First chunk"), Document(page_content=" "), Document(page_content="Second chunk")])
    assert result.chunk_count == 2
    assert result.model_key == "google:test-embedding:3:"
    async with db_session.begin():
        chunks = (await db_session.scalars(select(DocumentChunk).where(
            DocumentChunk.course_id == source.course_id).order_by(DocumentChunk.chunk_index))).all()
        assert [chunk.content for chunk in chunks] == ["First chunk", "Second chunk"]
        assert all(chunk.source_type == source_type.value and chunk.source_id == source_id for chunk in chunks)
        old_ids = [chunk.id for chunk in chunks]

    result = await service.ingest(source=source, documents=[Document(page_content="Replacement")])
    assert result.chunk_count == 1
    async with db_session.begin():
        chunks = (await db_session.scalars(select(DocumentChunk).where(
            DocumentChunk.course_id == source.course_id))).all()
        assert [chunk.content for chunk in chunks] == ["Replacement"]
        assert await db_session.scalar(select(func.count()).select_from(ChunkEmbedding).where(
            ChunkEmbedding.chunk_id.in_(old_ids))) == 0
        assert await db_session.scalar(select(func.count()).select_from(ChunkEmbedding).where(
            ChunkEmbedding.chunk_id == chunks[0].id)) == 1

    await service.ingest(source=source, documents=[])
    async with db_session.begin():
        assert await db_session.scalar(select(func.count()).select_from(DocumentChunk).where(
            DocumentChunk.course_id == source.course_id)) == 0
