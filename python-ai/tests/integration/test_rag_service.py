from unittest.mock import AsyncMock, MagicMock

from app.repositories.chunk_repository import ChunkRepository
from app.services.rag_service import RagService
from app.services.retrieval_service import RetrievalService


async def test_rag_with_indexed_document(db_session, embeddings, settings, indexed_source):
    retrieval = RetrievalService(embeddings=embeddings, repository=ChunkRepository(db_session), settings=settings)
    model = MagicMock()
    model.with_structured_output.return_value.ainvoke = AsyncMock(return_value={
        "answer": "Dependency Injection reduces coupling.", "citation_labels": ["S1"]})
    service = RagService(retrieval_service=retrieval, chat_model=model)
    result = await service.answer(course_id=indexed_source.course_id, question="Dependency Injection?", history=[])
    assert result.answer == "Dependency Injection reduces coupling."
    assert len(result.citations) == 1
    assert result.citations[0].document_id == indexed_source.document_id
