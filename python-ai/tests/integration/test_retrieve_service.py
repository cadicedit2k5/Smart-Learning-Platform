from app.configs.config import get_settings
from app.configs.database import AsyncSessionLocal
from app.infrastructure.ai.embeddings import create_embeddings
from app.repositories.chunk_repository import ChunkRepository
from app.services.retrieval_service import RetrievalService


async def test_real_retrieval_without_course_id():
    settings = get_settings()

    async with AsyncSessionLocal() as session:

        repository = ChunkRepository(
            session
        )

        embeddings = create_embeddings(
            settings
        )

        service = RetrievalService(
            embeddings=embeddings,
            repository=repository,
            settings=settings,
        )

        results = await service.retrieve(
            question=(
                "Dependency Injection là gì?"
            ),
        )

        assert results
        assert all(result.content.strip() for result in results)

        for result in results:
            print()
            print(
                "distance:",
                result.distance,
            )
            print(
                "document:",
                result.document_id,
            )
            print(
                "locator:",
                result.source_locator,
            )
            print(
                result.content[:500],
            )