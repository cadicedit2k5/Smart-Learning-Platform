import pytest

from app.repositories.chunk_repository import ChunkRepository
from app.services.retrieval_service import RetrievalService


@pytest.mark.parametrize("scoped", [True, False])
async def test_retrieval_of_seeded_document(db_session, embeddings, settings, indexed_source, scoped):
    service = RetrievalService(embeddings=embeddings, repository=ChunkRepository(db_session), settings=settings)
    results = await service.retrieve(course_id=indexed_source.course_id if scoped else None,
                                     question="Dependency Injection?")
    assert len(results) == 1
    assert results[0].source_type == "DOCUMENT"
    assert results[0].source_id == indexed_source.source_id
    assert results[0].document_id == indexed_source.document_id
    assert results[0].content == "Dependency Injection reduces coupling."
