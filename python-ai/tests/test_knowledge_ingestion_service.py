from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest
from langchain_core.documents import Document

from app.services.knowledge_ingestion_service import KnowledgeIngestionService, KnowledgeSource, KnowledgeSourceType


@pytest.fixture
def dependencies(settings):
    session = MagicMock()
    session.begin.return_value = AsyncMock()
    session.flush = AsyncMock()
    embeddings = MagicMock(aembed_documents=AsyncMock(return_value=[[1., 0., 0.], [0., 1., 0.]]))
    repository = MagicMock(replace_source_chunks=AsyncMock())
    service = KnowledgeIngestionService(session=session, embeddings=embeddings,
                                        repository=repository, settings=settings)
    return service, session, embeddings, repository


@pytest.mark.parametrize("source_type", list(KnowledgeSourceType))
async def test_ingest_generalized_source(dependencies, source_type):
    service, session, embeddings, repository = dependencies
    source_id, version_id = uuid4(), uuid4()
    is_document = source_type == KnowledgeSourceType.DOCUMENT
    source = KnowledgeSource(course_id=uuid4(), source_type=source_type, source_id=source_id,
                             source_version_id=version_id if is_document else None,
                             document_id=source_id if is_document else None,
                             document_version_id=version_id if is_document else None,
                             topic_id=None if is_document else source_id,
                             chapter_id=None if is_document else uuid4())
    locator = {"type": "topic", "topicId": str(source_id)}
    result = await service.ingest(source=source, documents=[
        Document(page_content="  First chunk  ", metadata={"source_locator": locator}),
        Document(page_content=" \n "),
        Document(page_content="Second chunk", metadata={"source_locator": "invalid", "dl_meta": {"headings": ["Section"]}}),
    ])
    assert result.chunk_count == 2
    assert result.model_key == "google:test-embedding:3:"
    embeddings.aembed_documents.assert_awaited_once_with(["First chunk", "Second chunk"])
    repository.replace_source_chunks.assert_awaited_once()
    kwargs = repository.replace_source_chunks.call_args.kwargs
    assert {key: kwargs[key] for key in ("course_id", "source_type", "source_id")} == {
        "course_id": source.course_id, "source_type": source_type.value, "source_id": source_id}
    chunks = kwargs["chunks"]
    assert [chunk.chunk_index for chunk in chunks] == [0, 1]
    assert [chunk.content for chunk in chunks] == ["First chunk", "Second chunk"]
    assert [chunk.source_locator for chunk in chunks] == [locator, {"heading": "Section"}]
    for index, chunk in enumerate(chunks):
        for field in ("course_id", "source_id", "source_version_id", "document_id", "document_version_id", "topic_id", "chapter_id"):
            assert getattr(chunk, field) == getattr(source, field)
        assert chunk.source_type == source_type.value
        assert len(chunk.embeddings) == 1
        assert chunk.embeddings[0].model_key == result.model_key
        assert chunk.embeddings[0].embedding == embeddings.aembed_documents.return_value[index]
    session.begin.assert_called_once_with()
    session.flush.assert_awaited_once_with()


@pytest.mark.parametrize("documents", [[], [Document(page_content=" \n ")]])
async def test_empty_source_clears_previous_chunks(dependencies, documents):
    service, session, embeddings, repository = dependencies
    source = KnowledgeSource(course_id=uuid4(), source_type=KnowledgeSourceType.TOPIC, source_id=uuid4())
    result = await service.ingest(source=source, documents=documents)
    assert result.chunk_count == 0
    assert result.model_key == "google:test-embedding:3:"
    embeddings.aembed_documents.assert_not_awaited()
    repository.replace_source_chunks.assert_awaited_once_with(
        course_id=source.course_id, source_type="TOPIC", source_id=source.source_id, chunks=[])
    session.begin.assert_called_once_with()


@pytest.mark.parametrize("vectors", [[], [[1., 0., 0.], [0., 1., 0.]]])
async def test_invalid_embedding_count_does_not_replace_existing_chunks(dependencies, vectors):
    service, session, embeddings, repository = dependencies
    embeddings.aembed_documents.return_value = vectors
    source = KnowledgeSource(course_id=uuid4(), source_type=KnowledgeSourceType.DOCUMENT, source_id=uuid4())
    with pytest.raises(RuntimeError, match="unexpected vector count"):
        await service.ingest(source=source, documents=[Document(page_content="content")])
    repository.replace_source_chunks.assert_not_awaited()
    session.begin.assert_not_called()
