import uuid

from app.configs.config import get_settings
from app.configs.database import AsyncSessionLocal
from app.infrastructure.ai.chat_model import create_chat_model
from app.infrastructure.ai.embeddings import create_embeddings
from app.repositories.chunk_repository import ChunkRepository
from app.services.rag_service import RagService
from app.services.retrieval_service import RetrievalService


async def test_real_rag():

    settings = get_settings()

    course_id = uuid.UUID("965c545d-dcd8-48a6-88fe-d684b60e8ddc")

    async with AsyncSessionLocal() as session:

        repository = ChunkRepository(session)

        embeddings = create_embeddings(settings)

        retrieval_service = (RetrievalService(
                embeddings=embeddings,
                repository=repository,
                settings=settings,
            )
        )

        chat_model = create_chat_model(settings)

        rag_service = RagService(
            retrieval_service=retrieval_service,
            chat_model=chat_model,
        )

        result = await rag_service.answer(
            course_id=course_id,
            question=(
                "Dependency Injection là gì?"
            ),
        )

        print(result.model_dump_json(indent=2))

        assert result.answer.strip()
        assert result.citations
        assert all(citation.document_id is not None for citation in result.citations)