from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest
from langchain_core.documents import Document

from app.messaging.events.document import DocumentDeletionRequestedEvent, DocumentIngestionRequestedEvent
from app.messaging.events.topic import TopicKnowledgeIndexRequestedEvent
from app.services import document_deletion_handler, document_ingestion_handler, topic_knowledge_handler
from app.services.knowledge_ingestion_service import IngestionResult, KnowledgeSourceType


@pytest.fixture
def session_factory():
    session = MagicMock()
    session.begin.return_value = AsyncMock()
    factory = MagicMock()
    factory.return_value.__aenter__ = AsyncMock(return_value=session)
    factory.return_value.__aexit__ = AsyncMock(return_value=False)
    return factory, session


def event_metadata():
    return {"event_id": uuid4(), "schema_version": 1, "occurred_at": datetime.now(timezone.utc)}


@pytest.mark.parametrize("operation", ["UPSERT", "DELETE"])
async def test_topic_event_routes_to_ingestion_or_deletion(monkeypatch, settings, session_factory, operation):
    factory, session = session_factory
    repository = MagicMock(delete_source_chunks=AsyncMock())
    repository_class = MagicMock(return_value=repository)
    ingestion = MagicMock(ingest=AsyncMock(return_value=IngestionResult(1, "model")))
    service_class = MagicMock(return_value=ingestion)
    monkeypatch.setattr(topic_knowledge_handler, "ChunkRepository", repository_class)
    monkeypatch.setattr(topic_knowledge_handler, "KnowledgeIngestionService", service_class)
    documents = [Document(page_content="Topic content")]
    loader = MagicMock()
    loader.load.return_value = documents
    embeddings = MagicMock()
    event = TopicKnowledgeIndexRequestedEvent(**event_metadata(), course_id=uuid4(), chapter_id=uuid4(),
        topic_id=uuid4(), title="DI", description="Injection", content={"type": "doc"}, operation=operation)
    handler = topic_knowledge_handler.TopicKnowledgeHandler(
        session_factory=factory, embeddings=embeddings, loader=loader, settings=settings)
    result = await handler.handle(event)
    repository_class.assert_called_once_with(session)
    if operation == "DELETE":
        assert result is None
        repository.delete_source_chunks.assert_awaited_once_with(
            course_id=event.course_id, source_type="TOPIC", source_id=event.topic_id)
        session.begin.assert_called_once_with()
        loader.load.assert_not_called()
        service_class.assert_not_called()
    else:
        assert result == IngestionResult(1, "model")
        loader.load.assert_called_once_with(topic_id=event.topic_id, chapter_id=event.chapter_id,
            title=event.title, description=event.description, content=event.content)
        service_class.assert_called_once_with(session=session, embeddings=embeddings,
                                               repository=repository, settings=settings)
        ingestion.ingest.assert_awaited_once()
        kwargs = ingestion.ingest.call_args.kwargs
        assert kwargs["documents"] == documents
        source = kwargs["source"]
        assert source.source_type == KnowledgeSourceType.TOPIC
        assert source.source_id == source.topic_id == event.topic_id
        assert source.course_id == event.course_id
        assert source.chapter_id == event.chapter_id
        assert source.document_id is None
        repository.delete_source_chunks.assert_not_awaited()


async def test_document_deletion_uses_generalized_source(monkeypatch, session_factory):
    factory, session = session_factory
    repository = MagicMock(delete_source_chunks=AsyncMock())
    monkeypatch.setattr(document_deletion_handler, "ChunkRepository", MagicMock(return_value=repository))
    event = DocumentDeletionRequestedEvent(**event_metadata(), course_id=uuid4(), document_id=uuid4())
    await document_deletion_handler.DocumentDeletionHandler(session_factory=factory).handle(event)
    repository.delete_source_chunks.assert_awaited_once_with(
        course_id=event.course_id, source_type="DOCUMENT", source_id=event.document_id)
    session.begin.assert_called_once_with()


@pytest.mark.parametrize("empty", [False, True])
async def test_document_download_load_and_ingest(monkeypatch, settings, session_factory, empty):
    factory, session = session_factory
    repository = MagicMock()
    monkeypatch.setattr(document_ingestion_handler, "ChunkRepository", MagicMock(return_value=repository))
    ingestion = MagicMock(ingest=AsyncMock(return_value=IngestionResult(1, "model")))
    service_class = MagicMock(return_value=ingestion)
    monkeypatch.setattr(document_ingestion_handler, "KnowledgeIngestionService", service_class)
    storage = MagicMock(download_to_file=AsyncMock())
    documents = [Document(page_content=" " if empty else "Document content")]
    loader = MagicMock(load=AsyncMock(return_value=documents))
    embeddings = MagicMock()
    event = DocumentIngestionRequestedEvent(**event_metadata(), processing_job_id=uuid4(),
        course_id=uuid4(), document_id=uuid4(), document_version_id=uuid4(),
        storage_bucket="documents", storage_key="course/file.pdf", file_name="Lesson.PDF", mime_type="application/pdf")
    handler = document_ingestion_handler.DocumentIngestionHandler(
        session_factory=factory, storage=storage, loader=loader, embeddings=embeddings, settings=settings)
    if empty:
        with pytest.raises(ValueError, match="chunk"):
            await handler.handle(event)
        factory.assert_not_called()
        ingestion.ingest.assert_not_awaited()
    else:
        assert await handler.handle(event) == IngestionResult(1, "model")
        service_class.assert_called_once_with(session=session, embeddings=embeddings,
                                               repository=repository, settings=settings)
        ingestion.ingest.assert_awaited_once()
        kwargs = ingestion.ingest.call_args.kwargs
        assert kwargs["documents"] == documents
        source = kwargs["source"]
        assert source.source_type == KnowledgeSourceType.DOCUMENT
        assert source.source_id == source.document_id == event.document_id
        assert source.source_version_id == source.document_version_id == event.document_version_id
        assert source.course_id == event.course_id
        assert source.topic_id is None
    destination = storage.download_to_file.call_args.kwargs["destination"]
    storage.download_to_file.assert_awaited_once_with(
        bucket=event.storage_bucket, object_name=event.storage_key, destination=destination)
    assert destination.suffix == ".pdf"
    loader.load.assert_awaited_once_with(destination)
    assert not destination.parent.exists()
