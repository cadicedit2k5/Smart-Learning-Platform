from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest

from app.models import DocumentChunk
from app.repositories.chunk_repository import ChunkSearchResult
from app.services.retrieval_service import RetrievalService


@pytest.fixture
def dependencies(settings):
    embeddings = MagicMock(aembed_query=AsyncMock(return_value=[1., 0., 0.]))
    repository = MagicMock(similarity_search=AsyncMock(return_value=[]))
    return RetrievalService(embeddings=embeddings, repository=repository, settings=settings), embeddings, repository


@pytest.mark.parametrize("scoped", [True, False])
@pytest.mark.parametrize("top_k", [None, 2])
async def test_retrieve_maps_document_and_topic_sources(dependencies, scoped, top_k):
    service, embeddings, repository = dependencies
    course_id = uuid4() if scoped else None
    chunks = [
        DocumentChunk(id=uuid4(), source_type="DOCUMENT", source_id=uuid4(), document_id=uuid4(),
                      document_version_id=uuid4(), content="Document", source_locator={"pages": [1]}),
        DocumentChunk(id=uuid4(), source_type="TOPIC", source_id=uuid4(), topic_id=uuid4(),
                      content="Topic", source_locator={"type": "topic"}),
    ]
    repository.similarity_search.return_value = [
        ChunkSearchResult(chunk=chunk, distance=index / 10) for index, chunk in enumerate(chunks)]
    results = await service.retrieve(course_id=course_id, question="  Explain DI  ", top_k=top_k)
    embeddings.aembed_query.assert_awaited_once_with("Explain DI")
    repository.similarity_search.assert_awaited_once_with(
        course_id=course_id, model_key="google:test-embedding:3:", query_vector=[1., 0., 0.],
        top_k=5 if top_k is None else top_k)
    assert len(results) == 2
    for index, (result, chunk) in enumerate(zip(results, chunks, strict=True)):
        assert result.chunk_id == chunk.id
        for field in ("source_type", "source_id", "document_id", "document_version_id", "topic_id", "content", "source_locator"):
            assert getattr(result, field) == getattr(chunk, field)
        assert result.distance == index / 10


@pytest.mark.parametrize("question,top_k", [(" ", None), ("DI", 0), ("DI", -1)])
async def test_invalid_retrieval_input_has_no_external_calls(dependencies, question, top_k):
    service, embeddings, repository = dependencies
    with pytest.raises(ValueError):
        await service.retrieve(question=question, top_k=top_k)
    embeddings.aembed_query.assert_not_awaited()
    repository.similarity_search.assert_not_awaited()


async def test_wrong_query_dimensions(dependencies):
    service, embeddings, repository = dependencies
    embeddings.aembed_query.return_value = [1., 0.]
    with pytest.raises(RuntimeError):
        await service.retrieve(question="DI")
    repository.similarity_search.assert_not_awaited()


async def test_no_matching_chunks(dependencies):
    service, _, _ = dependencies
    assert await service.retrieve(question="DI") == []
